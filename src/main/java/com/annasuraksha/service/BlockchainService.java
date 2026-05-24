package com.annasuraksha.service;

import com.annasuraksha.model.Beneficiary;
import com.annasuraksha.model.BeneficiaryRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Service
public class BlockchainService {

    private final BeneficiaryRepository repo;

    public String hashAadhaar(String aadhaarRaw) {
        return sha256(aadhaarRaw.replaceAll("[^0-9]", ""));
    }

    public String hashVoterId(String voterId) {
        return sha256(voterId.trim().toUpperCase());
    }

    public String computeBlockHash(String prevHash, String aadhaarHash,
                                    String name, String state, String category) {
        return sha256(prevHash + aadhaarHash + name + state + category);
    }

    public String getLatestHash() {
        List<Beneficiary> all = repo.findAllByOrderByBlockHeightAsc();
        if (all.isEmpty()) return "0".repeat(64);
        return all.get(all.size() - 1).getBlockHash();
    }

    public long getNextBlockHeight() {
        List<Beneficiary> all = repo.findAllByOrderByBlockHeightAsc();
        return all.isEmpty() ? 1L : all.get(all.size() - 1).getBlockHeight() + 1L;
    }

    public boolean validateChain() {
        List<Beneficiary> chain = repo.findAllByOrderByBlockHeightAsc();
        for (int i = 1; i < chain.size(); i++) {
            String expected = chain.get(i - 1).getBlockHash();
            String actual   = chain.get(i).getPrevBlockHash();
            if (expected != null && !expected.equals(actual)) {
                log.error("Chain broken at block {} — expected {} got {}",
                    chain.get(i).getBlockHeight(), expected, actual);
                return false;
            }
        }
        return true;
    }

    public String shortHash(String hash) {
        if (hash == null || hash.length() < 12) return hash;
        return "0x" + hash.substring(0, 4) + "…" + hash.substring(hash.length() - 4);
    }

    public String generateTxHash(long beneficiaryId, long epochMillis) {
        return "0x" + sha256(beneficiaryId + ":" + epochMillis).substring(0, 40);
    }

    public String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 unavailable", e);
        }
    }
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BlockchainService.class);
    public BlockchainService(BeneficiaryRepository repo) {
        this.repo = repo;
    }
}
