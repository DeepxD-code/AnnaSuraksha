package com.annasuraksha.model;

import java.time.LocalDateTime;
import java.util.List;

public record FraudRiskScore(
    Long              subjectId,
    String            subjectName,
    String            subjectType,
    String            stateCode,
    double            riskScore,
    double            confidenceScore,
    int               evidenceCount,
    RiskLevel         riskLevel,
    String            detectionSummary,
    List<String>      topFactors,
    FraudFeatureVector features,
    LocalDateTime     computedAt
) {
    public enum RiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL;

        public static RiskLevel from(double score) {
            if (score >= 0.85) return CRITICAL;
            if (score >= 0.70) return HIGH;
            if (score >= 0.40) return MEDIUM;
            return LOW;
        }

        public String badge() {
            return switch (this) {
                case CRITICAL -> "🔴 CRITICAL";
                case HIGH     -> "🔴 HIGH";
                case MEDIUM   -> "🟡 MEDIUM";
                case LOW      -> "🟢 LOW";
            };
        }
    }
}
