package com.annasuraksha.service.alert;

import com.annasuraksha.model.alert.AlertEvent;
import com.annasuraksha.model.alert.AlertEventRepository;
import com.annasuraksha.model.FraudRiskScore;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AlertService {

    private final AlertEventRepository alertRepo;
    private final SseAlertEmitter      sseEmitter;

    public void triggerFraudAlert(FraudRiskScore score) {
        AlertEvent alert = AlertEvent.builder()
            .alertId(UUID.randomUUID().toString())
            .alertType("FRAUD_DETECTED")
            .severity("CRITICAL")
            .title("High Fraud Risk: " + score.subjectName())
            .description(String.format("Beneficiary #%d (%s, %s) scored %.3f. Factors: %s",
                score.subjectId(), score.subjectName(), score.stateCode(),
                score.riskScore(), String.join(", ", score.topFactors())))
            .entityId(String.valueOf(score.subjectId()))
            .entityType("BENEFICIARY")
            .stateCode(score.stateCode())
            .actionUrl("/api/fraud/score/" + score.subjectId() + "?explain=true")
            .build();
        save(alert);
    }

    public void triggerFraudAlert(com.annasuraksha.model.event.FraudEvent event) {
        AlertEvent alert = AlertEvent.builder()
            .alertId(UUID.randomUUID().toString())
            .alertType("FRAUD_DETECTED")
            .severity("CRITICAL")
            .title("High Fraud Risk: " + event.subjectName())
            .description(String.format("Beneficiary #%d (%s, %s) scored %.3f. Factors: %s",
                event.beneficiaryId(), event.subjectName(), event.stateCode(),
                event.riskScore(), String.join(", ", event.topFactors())))
            .entityId(String.valueOf(event.beneficiaryId()))
            .entityType("BENEFICIARY")
            .stateCode(event.stateCode())
            .actionUrl("/api/fraud/score/" + event.beneficiaryId())
            .build();
        save(alert);
    }

    public void triggerSupplyAlert(String alertType, String shipmentId, String description) {
        String severity = switch (alertType) {
            case "OFF_ROUTE", "MISSING_INVENTORY" -> "CRITICAL";
            case "DELAY", "DISCREPANCY"           -> "WARNING";
            default                               -> "INFO";
        };
        AlertEvent alert = AlertEvent.builder()
            .alertId(UUID.randomUUID().toString())
            .alertType("SUPPLY_" + alertType)
            .severity(severity)
            .title("Supply Chain: " + alertType.replace("_", " "))
            .description(description)
            .entityId(shipmentId)
            .entityType("SHIPMENT")
            .actionUrl("/api/supply-chain/shipment/" + shipmentId)
            .build();
        save(alert);
    }

    public void triggerBlockchainAlert(String message) {
        AlertEvent alert = AlertEvent.builder()
            .alertId(UUID.randomUUID().toString())
            .alertType("BLOCKCHAIN_INTEGRITY")
            .severity("CRITICAL")
            .title("Blockchain Integrity Failure")
            .description(message)
            .entityType("LEDGER")
            .actionUrl("/api/ledger/verify")
            .build();
        save(alert);
        log.error("BLOCKCHAIN INTEGRITY ALERT: {}", message);
    }

    public List<AlertEvent> getActiveAlerts(String stateCode) {
        return stateCode != null
            ? alertRepo.findByStateCodeAndAcknowledgedFalseOrderByCreatedAtDesc(stateCode)
            : alertRepo.findByAcknowledgedFalseOrderByCreatedAtDesc();
    }

    public List<AlertEvent> getAllAlerts() {
        return alertRepo.findByAcknowledgedFalseOrderByCreatedAtDesc();
    }

    public boolean acknowledgeAlert(String alertId, String acknowledgedBy) {
        var opt = alertRepo.findByAlertId(alertId);
        if (opt.isEmpty()) return false;
        AlertEvent a = opt.get();
        a.setAcknowledged(true);
        a.setAcknowledgedBy(acknowledgedBy);
        a.setAcknowledgedAt(LocalDateTime.now());
        alertRepo.save(a);
        return true;
    }

    private void save(AlertEvent alert) {
        alertRepo.save(alert);
        sseEmitter.broadcast(alert);
        log.warn("ALERT [{}]: {}", alert.getSeverity(), alert.getTitle());
    }
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AlertService.class);
    public AlertService(AlertEventRepository alertRepo, SseAlertEmitter sseEmitter) {
        this.alertRepo = alertRepo;
        this.sseEmitter = sseEmitter;
    }
}
