package com.annasuraksha.model;

import java.time.LocalDateTime;
import java.util.List;

public record FraudRiskScore(
    Long              subjectId,
    String            subjectName,
    String            subjectType,
    String            stateCode,
    double            riskScore,
    RiskLevel         riskLevel,
    List<String>      topFactors,
    FraudFeatureVector features,
    LocalDateTime     computedAt
) {
    public enum RiskLevel {
        LOW, MEDIUM, HIGH;

        public static RiskLevel from(double score) {
            if (score >= 0.70) return HIGH;
            if (score >= 0.40) return MEDIUM;
            return LOW;
        }

        public String badge() {
            return switch (this) {
                case HIGH   -> "🔴 HIGH";
                case MEDIUM -> "🟡 MEDIUM";
                case LOW    -> "🟢 LOW";
            };
        }
    }
}
