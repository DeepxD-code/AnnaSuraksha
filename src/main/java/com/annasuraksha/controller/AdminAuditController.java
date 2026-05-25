package com.annasuraksha.controller;

import com.annasuraksha.model.AuditLog;
import com.annasuraksha.model.AuditLogRepository;
import com.annasuraksha.model.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/audit")
public class AdminAuditController {

    private final AuditLogRepository auditRepo;

    public AdminAuditController(AuditLogRepository auditRepo) {
        this.auditRepo = auditRepo;
    }

    @GetMapping("/recent")
    public ApiResponse<List<AuditLog>> getRecent(@RequestParam(defaultValue = "/api/admin/ledger") String pathPrefix) {
        List<AuditLog> logs = auditRepo.findByPathStartingWithOrderByCreatedAtDesc(pathPrefix);
        return ApiResponse.success(logs, "Audit logs fetched.");
    }
}