package com.annasuraksha.model.event;

import com.annasuraksha.model.FraudRiskScore;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FraudEvent(
    String       eventId,
    String       eventType,
    Long         beneficiaryId,
    String       subjectName,
    String       stateCode,
    double       riskScore,
    String       riskLevel,
    List<String> topFactors,
    String       detectionLayer,
    Instant      occurredAt,
    String       correlationId
) {
    public static FraudEvent from(FraudRiskScore score, String correlationId) {
        return new FraudEvent(
            UUID.randomUUID().toString(), "FRAUD_DETECTED",
            score.subjectId(), score.subjectName(), score.stateCode(),
            score.riskScore(), score.riskLevel().name(), score.topFactors(),
            "LAYER_5_RISK_SCORE", Instant.now(), correlationId
        );
    }
}
