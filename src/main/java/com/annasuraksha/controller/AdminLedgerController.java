package com.annasuraksha.controller;

import com.annasuraksha.model.Snapshot;
import com.annasuraksha.model.api.ApiResponse;
import com.annasuraksha.service.MerkleService;
import com.annasuraksha.service.AuditLogService;
import com.annasuraksha.service.fabric.FabricQueryService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/ledger")
public class AdminLedgerController {

    private final MerkleService merkleService;
    private final FabricQueryService fabricQueryService;
    private final AuditLogService auditLogService;

    public AdminLedgerController(MerkleService merkleService, FabricQueryService fabricQueryService, AuditLogService auditLogService) {
        this.merkleService = merkleService;
        this.fabricQueryService = fabricQueryService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/verify-all")
    public ApiResponse<List<Map<String, Object>>> verifyAll(HttpServletRequest req, Authentication auth) {
        List<Map<String, Object>> result = new ArrayList<>();
        List<Snapshot> snaps = merkleService.getAllSnapshots();
        for (Snapshot s : snaps) {
            Map<String, Object> item = new HashMap<>();
            item.put("snapshotId", s.getId());
            item.put("root", s.getRoot());
            item.put("anchoredAt", s.getAnchoredAt());
            item.put("anchorTxHash", s.getAnchorTxHash());
            try {
                var onChain = fabricQueryService.queryAnchor(String.valueOf(s.getId()));
                item.put("onChain", onChain);
                boolean matches = false;
                if (onChain != null && onChain.get("merkleRoot") != null) {
                    matches = s.getRoot() != null && s.getRoot().equals(onChain.get("merkleRoot"));
                }
                item.put("matchesOnChain", matches);
            } catch (Exception e) {
                item.put("onChainError", e.getMessage());
            }
            result.add(item);
        }

        // Audit the admin action
        String clientIp = req.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isBlank()) clientIp = req.getRemoteAddr();
        String userId = auth != null ? auth.getName() : "anonymous";
        auditLogService.logAuthEvent(true, clientIp, userId, "/api/admin/ledger/verify-all");

        return ApiResponse.success(result, "Verification completed.");
    }

    @PostMapping("/anchor/{id}")
    public ApiResponse<Map<String, String>> anchorSnapshot(@PathVariable Long id, HttpServletRequest req, Authentication auth) {
        try {
            var meta = merkleService.anchorSnapshotOnChain(id);
            if (meta == null) return ApiResponse.error("NO_GATEWAY", "No Fabric gateway configured.");

            String clientIp = req.getHeader("X-Forwarded-For");
            if (clientIp == null || clientIp.isBlank()) clientIp = req.getRemoteAddr();
            String userId = auth != null ? auth.getName() : "anonymous";
            auditLogService.logAuthEvent(true, clientIp, userId, "/api/admin/ledger/anchor/" + id);

            return ApiResponse.success(meta, "Snapshot anchored on-chain.");
        } catch (Exception e) {
            String clientIp = req.getHeader("X-Forwarded-For");
            if (clientIp == null || clientIp.isBlank()) clientIp = req.getRemoteAddr();
            String userId = auth != null ? auth.getName() : "anonymous";
            auditLogService.logAuthEvent(false, clientIp, userId, "/api/admin/ledger/anchor/" + id);
            return ApiResponse.error("ANCHOR_FAILED", e.getMessage());
        }
    }
}
