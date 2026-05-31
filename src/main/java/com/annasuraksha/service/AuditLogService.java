package com.annasuraksha.service;

import com.annasuraksha.model.AuditLog;
import com.annasuraksha.model.AuditLogRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository repo;

    @Async
    public void logApiAccess(String clientIp, String keyHash, String method,
                              String path, int status, long durationMs) {
        save(AuditLog.builder()
            .eventType("API_ACCESS").clientIp(clientIp)
            .performedBy(keyHash).httpMethod(method)
            .path(path).httpStatus(status).durationMs(durationMs)
            .build());
    }

    @Async
    public void logFraudDetection(Long beneficiaryId, String riskLevel, String performedBy) {
        save(AuditLog.builder()
            .eventType("FRAUD_DETECTION").entityId(String.valueOf(beneficiaryId))
            .entityType("BENEFICIARY").details("riskLevel=" + riskLevel)
            .performedBy(performedBy).build());
    }

    @Async
    public void logSupplyChainUpdate(String shipmentId, String stage, String performedBy) {
        save(AuditLog.builder()
            .eventType("SUPPLY_CHAIN_UPDATE").entityId(shipmentId)
            .entityType("SHIPMENT").details("stage=" + stage)
            .performedBy(performedBy).build());
    }

    @Async
    public void logGhostFlagged(Long beneficiaryId, String reason) {
        save(AuditLog.builder()
            .eventType("GHOST_FLAGGED").entityId(String.valueOf(beneficiaryId))
            .entityType("BENEFICIARY").details(reason).build());
    }

    @Async
    public void logAuthEvent(boolean success, String clientIp, String userId, String path) {
        save(AuditLog.builder()
            .eventType(success ? "AUTH_SUCCESS" : "AUTH_FAILURE")
            .clientIp(clientIp).performedBy(userId).path(path).build());
    }

    @Async
    public void logRateLimit(String clientIp, String path) {
        save(AuditLog.builder()
            .eventType("RATE_LIMIT_HIT").clientIp(clientIp).path(path).build());
    }

    public List<AuditLog> getRecent() {
        return repo.findTop100ByOrderByCreatedAtDesc();
    }

    private void save(AuditLog log) {
        try { repo.save(log); }
        catch (Exception e) { this.log.warn("Audit log save failed: {}", e.getMessage()); }
    }
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuditLogService.class);
    public AuditLogService(AuditLogRepository repo) {
        this.repo = repo;
    }
}
