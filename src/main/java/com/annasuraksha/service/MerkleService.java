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
            SnapshotProof sp = new SnapshotProof(s.getId(), benId, toJson(proof));
            proofRepo.save(sp);
        }

        return s;
    }

    public SnapshotProof getProof(Long snapshotId, Long beneficiaryId) {
        return proofRepo.findBySnapshotIdAndBeneficiaryId(snapshotId, beneficiaryId);
    }

    private List<String> buildProof(List<List<String>> tree, int leafIndex) {
        List<String> proof = new ArrayList<>();
        int idx = leafIndex;
        for (int level = 0; level < tree.size() - 1; level++) {
            List<String> levelNodes = tree.get(level);
            int sibling = (idx % 2 == 0) ? idx + 1 : idx - 1;
            if (sibling >= 0 && sibling < levelNodes.size()) proof.add(levelNodes.get(sibling));
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
