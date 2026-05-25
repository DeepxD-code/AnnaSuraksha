package com.annasuraksha.controller;

import com.annasuraksha.model.Snapshot;
import com.annasuraksha.model.SnapshotProof;
import com.annasuraksha.model.api.ApiResponse;
import com.annasuraksha.service.MerkleService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ledger")
public class LedgerController {

    private final MerkleService merkleService;

    public LedgerController(MerkleService merkleService) { this.merkleService = merkleService; }

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
}
