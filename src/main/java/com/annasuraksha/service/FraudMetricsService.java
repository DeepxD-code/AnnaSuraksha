package com.annasuraksha.service;

import com.annasuraksha.model.Beneficiary;
import com.annasuraksha.model.BeneficiaryRepository;
import com.annasuraksha.model.FraudRiskScore;
import com.annasuraksha.model.api.ApiResponse;
import com.annasuraksha.service.demo.SimulationService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FraudMetricsService {

    private final FraudRiskScoringService scoringSvc;
    private final GhostDetectionService   ghostSvc;
    private final SimulationService       simulationSvc;
    private final BeneficiaryRepository   beneRepo;

    private final List<AccuracyRecord> accuracyHistory = new ArrayList<>();

    public record AccuracyRecord(
        LocalDateTime timestamp,
        int totalScenarios,
        int scenariosPassed,
        double accuracyPct,
        Map<String, Boolean> scenarioResults,
        String version
    ) {}

    public record AccuracyReport(
        double overallAccuracy,
        int totalSimulations,
        int passed,
        int failed,
        double avgRiskScore,
        double highRiskPct,
        Map<String, Boolean> scenarioDetails,
        Map<String, Long> riskDistribution,
        double monthlyLossRs,
        double annualLossRs,
        int ghostCount,
        int activeSignalCount,
        String modelVersion,
        LocalDateTime computedAt
    ) {}

    public AccuracyReport generateReport() {
        List<FraudRiskScore> scores = scoringSvc.scoreAll();

        if (scores.isEmpty()) {
            return new AccuracyReport(0, 0, 0, 0, 0, 0,
                Map.of(), Map.of(), 0, 0, 0, 0, "v5.1.0", LocalDateTime.now());
        }

        long high = scores.stream().filter(s -> s.riskLevel() == FraudRiskScore.RiskLevel.HIGH
                                             || s.riskLevel() == FraudRiskScore.RiskLevel.CRITICAL).count();
        long medium = scores.stream().filter(s -> s.riskLevel() == FraudRiskScore.RiskLevel.MEDIUM).count();
        long low = scores.stream().filter(s -> s.riskLevel() == FraudRiskScore.RiskLevel.LOW).count();

        double avgScore = scores.stream().mapToDouble(FraudRiskScore::riskScore).average().orElse(0);

        long ghostCount = beneRepo.countByStatus("GHOST");
        long monthlyLoss = beneRepo.findByStatus("GHOST").stream()
            .mapToLong(b -> ghostSvc.estimateMonthlyLoss(b.getCategory())).sum();

        int totalSignals = scores.stream()
            .filter(s -> s.features() != null)
            .mapToInt(s -> s.features().activeSignalCount())
            .sum();

        return new AccuracyReport(
            computeSimulationAccuracy(),
            simulationSvc.getTotalRuns(),
            simulationSvc.getPassedRuns(),
            simulationSvc.getFailedRuns(),
            avgScore,
            scores.isEmpty() ? 0 : (double) high / scores.size() * 100,
            simulationSvc.getScenarioResults(),
            Map.of("CRITICAL", scores.stream().filter(s -> s.riskLevel() == FraudRiskScore.RiskLevel.CRITICAL).count() + 0L,
                   "HIGH", high,
                   "MEDIUM", medium,
                   "LOW", low),
            monthlyLoss,
            monthlyLoss * 12,
            ghostCount,
            totalSignals,
            "v5.1.0",
            LocalDateTime.now()
        );
    }

    public double computeSimulationAccuracy() {
        return simulationSvc.getAccuracy();
    }

    public void recordAccuracyRun(int total, int passed, Map<String, Boolean> results) {
        accuracyHistory.add(new AccuracyRecord(
            LocalDateTime.now(), total, passed,
            total > 0 ? (double) passed / total * 100 : 0,
            results, "v5.1.0"
        ));
    }

    public List<AccuracyRecord> getAccuracyHistory() {
        return List.copyOf(accuracyHistory);
    }

    public Map<String, Object> getDetailedMetrics() {
        AccuracyReport report = generateReport();
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("overallAccuracy", report.overallAccuracy());
        metrics.put("totalSimulationsRun", report.totalSimulations());
        metrics.put("simulationsPassed", report.passed());
        metrics.put("simulationsFailed", report.failed());
        metrics.put("avgRiskScore", Math.round(report.avgRiskScore() * 1000.0) / 1000.0);
        metrics.put("highRiskPercentage", Math.round(report.highRiskPct() * 100.0) / 100.0);
        metrics.put("riskDistribution", report.riskDistribution());
        metrics.put("scenarioDetails", report.scenarioDetails());
        metrics.put("ghostBeneficiariesDetected", report.ghostCount());
        metrics.put("monthlyLossEstimatedRs", report.monthlyLossRs());
        metrics.put("annualLossEstimatedRs", report.annualLossRs());
        metrics.put("totalActiveSignals", report.activeSignalCount());
        metrics.put("modelVersion", report.modelVersion());
        metrics.put("computedAt", report.computedAt());
        metrics.put("detectionLayers", List.of(
            "LAYER_1_DUPLICATE_AADHAAR", "LAYER_1_PHONE_DUPLICATE",
            "LAYER_2_STATISTICAL_ANOMALY",
            "LAYER_3_VELOCITY_CROSS_STATE",
            "LAYER_4_FAMILY_COMPOSITION",
            "LAYER_5_REGISTRATION_VELOCITY",
            "LAYER_6_FRAUD_RISK_SCORE_16_FEATURES"));
        return metrics;
    }

    public FraudMetricsService(FraudRiskScoringService scoringSvc, GhostDetectionService ghostSvc,
                                SimulationService simulationSvc, BeneficiaryRepository beneRepo) {
        this.scoringSvc = scoringSvc;
        this.ghostSvc = ghostSvc;
        this.simulationSvc = simulationSvc;
        this.beneRepo = beneRepo;
    }
}
