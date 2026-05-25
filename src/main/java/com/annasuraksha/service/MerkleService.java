package com.annasuraksha.service;

import com.annasuraksha.model.*;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MerkleService {

    private final BeneficiaryRepository beneRepo;
    private final SnapshotRepository snapshotRepo;
    private final SnapshotProofRepository proofRepo;

    public MerkleService(BeneficiaryRepository beneRepo, SnapshotRepository snapshotRepo, SnapshotProofRepository proofRepo) {
        this.beneRepo = beneRepo;
        this.snapshotRepo = snapshotRepo;
        this.proofRepo = proofRepo;
    }

    public Snapshot createSnapshot() {
        List<Beneficiary> all = beneRepo.findAll();
        // Build leaf hashes deterministic by beneficiary id
        List<Map.Entry<Long, String>> leaves = all.stream()
            .sorted(Comparator.comparingLong(Beneficiary::getId))
            .map(b -> Map.entry(b.getId(), sha256(b.getBlockHash() == null ? (b.getAadhaarHash() + b.getId()) : b.getBlockHash())))
            .collect(Collectors.toList());

        List<String> nodes = leaves.stream().map(Map.Entry::getValue).collect(Collectors.toList());
        List<List<String>> tree = new ArrayList<>();
        tree.add(nodes);
        while (nodes.size() > 1) {
            List<String> next = new ArrayList<>();
            for (int i = 0; i < nodes.size(); i += 2) {
                if (i + 1 < nodes.size()) next.add(sha256(nodes.get(i) + nodes.get(i+1)));
                else next.add(nodes.get(i));
            }
            tree.add(next);
            nodes = next;
        }

        String root = nodes.isEmpty() ? "" : nodes.get(0);
        Snapshot s = snapshotRepo.save(new Snapshot(root));

        // Generate proofs for each leaf
        for (int idx = 0; idx < leaves.size(); idx++) {
            Long benId = leaves.get(idx).getKey();
            List<String> proof = buildProof(tree, idx);
            SnapshotProof sp = new SnapshotProof(s.getId(), benId, idx, toJson(proof));
            proofRepo.save(sp);
        }

        // Attempt to anchor snapshot on-chain via FabricGatewayService if available (best-effort).
        try {
            // Resolve bean lazily to avoid hard dependency in unit tests/environments without Fabric
            var ctx = com.annasuraksha.config.ApplicationContextProvider.getApplicationContext();
            if (ctx != null && ctx.containsBean("fabricGatewayService")) {
                var fabric = (com.annasuraksha.service.fabric.FabricGatewayService) ctx.getBean("fabricGatewayService");
                var meta = fabric.anchorSnapshot(String.valueOf(s.getId()), root);
                if (meta != null) {
                    s.setAnchorTxHash(meta.get("txId"));
                    s.setChainName(meta.get("chain"));
                    s.setAnchoredAt(java.time.LocalDateTime.now());
                    snapshotRepo.save(s);
                }
            }
        } catch (Exception e) {
            // Anchor failures should not block snapshot creation
            java.util.logging.Logger.getLogger(MerkleService.class.getName()).warning("Fabric anchor failed: " + e.getMessage());
        }

        return s;
    }

    public SnapshotProof getProof(Long snapshotId, Long beneficiaryId) {
        return proofRepo.findBySnapshotIdAndBeneficiaryId(snapshotId, beneficiaryId);
    }

    public Snapshot getSnapshot(Long id) { return snapshotRepo.findById(id).orElse(null); }
    public List<Snapshot> getAllSnapshots() { return snapshotRepo.findAll(); }

    /**
     * Admin operation: attempt to anchor an existing snapshot on-chain and persist anchor metadata.
     * Returns the anchor metadata map when successful, or null if no gateway is available.
     */
    public java.util.Map<String,String> anchorSnapshotOnChain(Long snapshotId) throws Exception {
        Snapshot s = getSnapshot(snapshotId);
        if (s == null) throw new IllegalArgumentException("Snapshot not found: " + snapshotId);
        if (s.getRoot() == null || s.getRoot().isBlank()) throw new IllegalArgumentException("Snapshot has empty root");

        var ctx = com.annasuraksha.config.ApplicationContextProvider.getApplicationContext();
        if (ctx == null || !ctx.containsBean("fabricGatewayService")) return null;
        var fabric = (com.annasuraksha.service.fabric.FabricGatewayService) ctx.getBean("fabricGatewayService");
        var meta = fabric.anchorSnapshot(String.valueOf(snapshotId), s.getRoot());
        if (meta != null) {
            s.setAnchorTxHash(meta.get("txId"));
            s.setChainName(meta.get("chain"));
            s.setAnchoredAt(java.time.LocalDateTime.now());
            snapshotRepo.save(s);
        }
        return meta;
    }

    public boolean verifyProof(Long snapshotId, Long beneficiaryId) {
        SnapshotProof sp = getProof(snapshotId, beneficiaryId);
        if (sp == null) return false;
        Snapshot s = snapshotRepo.findById(snapshotId).orElse(null);
        if (s == null) return false;

        // Recompute leaf hash from beneficiary record
        var benOpt = beneRepo.findById(beneficiaryId);
        if (benOpt.isEmpty()) return false;
        Beneficiary b = benOpt.get();
        String leaf = sha256(b.getBlockHash() == null ? (b.getAadhaarHash() + b.getId()) : b.getBlockHash());

        // Apply proof
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.List<String> proof = om.readValue(sp.getProofJson(), java.util.List.class);
            String computed = leaf;
            int idx = sp.getLeafIndex() != null ? sp.getLeafIndex() : 0;
            for (String sibling : proof) {
                if (!sibling.isEmpty()) {
                    if (idx % 2 == 0) computed = sha256(computed + sibling);
                    else computed = sha256(sibling + computed);
                }
                idx = idx / 2;
            }
            return computed.equals(s.getRoot());
        } catch (Exception e) {
            return false;
        }
    }

    private List<String> buildProof(List<List<String>> tree, int leafIndex) {
        List<String> proof = new ArrayList<>();
        int idx = leafIndex;
        for (int level = 0; level < tree.size() - 1; level++) {
            List<String> levelNodes = tree.get(level);
            int sibling = (idx % 2 == 0) ? idx + 1 : idx - 1;
            if (sibling >= 0 && sibling < levelNodes.size()) proof.add(levelNodes.get(sibling));
            else proof.add(""); // orphan: no sibling at this level, add empty marker
            idx = idx / 2;
        }
        return proof;
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private String toJson(Object o) { return new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(o).toString(); }
}
