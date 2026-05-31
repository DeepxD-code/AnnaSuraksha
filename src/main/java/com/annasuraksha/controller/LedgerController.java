package com.annasuraksha.controller;

import com.annasuraksha.model.Snapshot;
import com.annasuraksha.model.SnapshotProof;
import com.annasuraksha.model.api.ApiResponse;
import com.annasuraksha.service.MerkleService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/ledger")
public class LedgerController {

    private final MerkleService merkleService;
    private final com.annasuraksha.service.fabric.FabricQueryService fabricQueryService;

    public LedgerController(MerkleService merkleService, com.annasuraksha.service.fabric.FabricQueryService fabricQueryService) {
        this.merkleService = merkleService; this.fabricQueryService = fabricQueryService;
    }

    @PostMapping("/snapshot")
    public ApiResponse<Snapshot> createSnapshot() {
        Snapshot s = merkleService.createSnapshot();
        return ApiResponse.success(s, "Snapshot created.");
    }

    @GetMapping("/snapshot/{id}/proof/{beneficiaryId}")
    public ApiResponse<SnapshotProof> getProof(@PathVariable Long id, @PathVariable Long beneficiaryId) {
        SnapshotProof p = merkleService.getProof(id, beneficiaryId);
        if (p == null) return ApiResponse.error("NOT_FOUND", "Proof not found.");
        return ApiResponse.success(p, "Proof retrieved.");
    }

    @GetMapping("/snapshot/{id}/verify/{beneficiaryId}")
    public ApiResponse<Map<String, Object>> verifyProof(@PathVariable Long id, @PathVariable Long beneficiaryId) {
        boolean ok = merkleService.verifyProof(id, beneficiaryId);
        return ok ? ApiResponse.success(Map.of("verified", true), "Proof verified.")
                  : ApiResponse.error("VERIFY_FAILED", "Proof mismatch or snapshot not found.");
    }

    @GetMapping("/snapshot/{id}/anchor")
    public ApiResponse<Map<String, Object>> getAnchor(@PathVariable Long id) {
        Snapshot s = merkleService.getSnapshot(id);
        if (s == null) return ApiResponse.error("NOT_FOUND", "Snapshot not found.");

        Map<String, Object> out = new java.util.HashMap<>();
        out.put("snapshotId", s.getId());
        out.put("root", s.getRoot());
        out.put("createdAt", s.getCreatedAt());
        out.put("anchoredAt", s.getAnchoredAt());
        out.put("anchorTxHash", s.getAnchorTxHash());

        try {
            var onChain = fabricQueryService.queryAnchor(String.valueOf(id));
            out.put("onChain", onChain);
        } catch (Exception e) {
            out.put("onChainError", e.getMessage());
        }

        return ApiResponse.success(out, "Anchor metadata.");
    }
}
