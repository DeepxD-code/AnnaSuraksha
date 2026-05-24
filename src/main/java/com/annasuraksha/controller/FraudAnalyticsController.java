package com.annasuraksha.controller;

import com.annasuraksha.model.*;
import com.annasuraksha.model.api.ApiResponse;
import com.annasuraksha.model.event.FraudEvent;
import com.annasuraksha.service.*;
import com.annasuraksha.service.event.FraudEventProducer;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/fraud")
public class FraudAnalyticsController {

    private final BeneficiaryRepository    beneRepo;
    private final FpsDeliveryRepository    fpsRepo;
    private final FraudRiskScoringService  scoringSvc;
    private final FraudExplanationService  explanationSvc;
    private final ImpossibleTravelDetector travelDetector;
    private final GhostDetectionService    ghostSvc;
    private final FraudEventProducer       eventProducer;

    // ── 1. Score single beneficiary ────────────────────────────────────────
    @GetMapping("/score/{id}")
    public ApiResponse<Map<String, Object>> scoreById(
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean explain,
            HttpServletRequest request) {

        Optional<FraudRiskScore> scoreOpt = scoringSvc.scoreById(id);
        if (scoreOpt.isEmpty())
            return ApiResponse.error("NOT_FOUND", "Beneficiary #" + id + " not found.");

        FraudRiskScore score = scoreOpt.get();

        // Publish event
        String correlationId = getCorrelationId(request);
        eventProducer.publishFraudDetected(FraudEvent.from(score, correlationId));

        Map<String, Object> result = buildScoreMap(score, explain);
        return ApiResponse.success(result, "Fraud score computed for beneficiary #" + id, correlationId);
    }

    // ── 2. Score all beneficiaries ─────────────────────────────────────────
    @PostMapping("/score-all")
    public ApiResponse<Map<String, Object>> scoreAll() {
        List<FraudRiskScore> scores = scoringSvc.scoreAll();

        long high   = scores.stream().filter(s -> s.riskLevel() == FraudRiskScore.RiskLevel.HIGH).count();
        long medium = scores.stream().filter(s -> s.riskLevel() == FraudRiskScore.RiskLevel.MEDIUM).count();
        long low    = scores.stream().filter(s -> s.riskLevel() == FraudRiskScore.RiskLevel.LOW).count();

        // Publish HIGH risk events
        scores.stream()
            .filter(s -> s.riskLevel() == FraudRiskScore.RiskLevel.HIGH)
            .limit(50)
            .forEach(s -> eventProducer.publishFraudDetected(FraudEvent.from(s, "batch")));

        return ApiResponse.success(Map.<String, Object>of(
            "totalScored", scores.size(),
            "highRisk",    high,
            "mediumRisk",  medium,
            "lowRisk",     low,
            "scoredAt",    LocalDateTime.now()
        ), "Batch scoring complete.");
    }

    // ── 3. High-risk beneficiaries ─────────────────────────────────────────
    @GetMapping("/high-risk-beneficiaries")
    public ApiResponse<Map<String, Object>> highRisk(
            @RequestParam(defaultValue = "20")    int     limit,
            @RequestParam(defaultValue = "false") boolean withExplanation) {

        List<FraudRiskScore> scores = scoringSvc.getHighRisk().stream().limit(limit).toList();
        List<Map<String, Object>> results = scores.stream()
            .map(s -> buildScoreMap(s, withExplanation))
            .toList();

        return ApiResponse.success(Map.<String, Object>of(
            "results",        results,
            "totalHighRisk",  results.size()
        ), "High-risk beneficiaries retrieved.");
    }

    // ── 4. Suspicious dealers ──────────────────────────────────────────────
    @GetMapping("/suspicious-dealers")
    public ApiResponse<Map<String, Object>> suspiciousDealers(
            @RequestParam(defaultValue = "20") int limit) {

        List<FpsDelivery> flagged = fpsRepo.findByFlaggedTrue();
        Map<String, List<FpsDelivery>> byShop = flagged.stream()
            .filter(d -> d.getFpsShopId() != null)
            .collect(Collectors.groupingBy(FpsDelivery::getFpsShopId));

        Map<String, Long> totalPerShop = fpsRepo.findAll().stream()
            .filter(d -> d.getFpsShopId() != null)
            .collect(Collectors.groupingBy(FpsDelivery::getFpsShopId, Collectors.counting()));

        List<Map<String, Object>> dealers = new ArrayList<>();
        for (var e : byShop.entrySet()) {
            String shopId    = e.getKey();
            var    shopFlags = e.getValue();
            long   total     = totalPerShop.getOrDefault(shopId, 1L);
            double rate      = shopFlags.size() / (double) total;
            FpsDelivery sample = shopFlags.get(0);
            long riceLost = shopFlags.stream()
                .mapToLong(d -> Math.max(0,
                    (d.getDealerRiceKg() != null ? d.getDealerRiceKg() : 0) -
                    (d.getConfirmedRiceKg() != null ? d.getConfirmedRiceKg() : 0)))
                .sum();

            Map<String, Object> dealer = new LinkedHashMap<>();
            dealer.put("fpsShopId",        shopId);
            dealer.put("state",            sample.getStateCode());
            dealer.put("operator",         sample.getFpsOperatorName());
            dealer.put("flaggedDeliveries",shopFlags.size());
            dealer.put("totalDeliveries",  total);
            dealer.put("diversionRatePct", round(rate * 100));
            dealer.put("riceLostKg",       riceLost);
            dealer.put("estimatedLossRs",  riceLost * 30L);
            dealer.put("riskLevel",        rate > 0.5 ? "HIGH" : rate > 0.2 ? "MEDIUM" : "LOW");
            dealer.put("sampleReason",     sample.getFlagReason());
            dealers.add(dealer);
        }
        dealers.sort((a, b) -> Double.compare(
            (Double) b.get("diversionRatePct"), (Double) a.get("diversionRatePct")));

        List<Map<String, Object>> top = dealers.stream().limit(limit).toList();
        return ApiResponse.success(Map.<String, Object>of("results", top), "Suspicious dealers retrieved.");
    }

    // ── 5. Duplicate identities ────────────────────────────────────────────
    @GetMapping("/duplicate-identities")
    public ApiResponse<Map<String, Object>> duplicateIdentities(
            @RequestParam(defaultValue = "50") int limit) {

        List<Beneficiary> all = beneRepo.findAll();
        Map<String, List<Beneficiary>> byAadhaar = all.stream()
            .filter(b -> b.getAadhaarHash() != null)
            .collect(Collectors.groupingBy(Beneficiary::getAadhaarHash));

        List<Map<String, Object>> groups = new ArrayList<>();
        for (var e : byAadhaar.entrySet()) {
            if (e.getValue().size() <= 1) continue;
            Set<String> states = e.getValue().stream()
                .map(Beneficiary::getStateCode).collect(Collectors.toSet());
            List<Map<String, Object>> members = e.getValue().stream().map(b -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id",           b.getId());
                m.put("name",         b.getFullName());
                m.put("state",        b.getStateCode());
                m.put("category",     b.getCategory());
                m.put("status",       b.getStatus());
                m.put("registeredAt", b.getRegisteredAt());
                return m;
            }).toList();
            Map<String, Object> group = new LinkedHashMap<>();
            group.put("aadhaarHashPrefix", e.getKey().substring(0, 8) + "…");
            group.put("duplicateCount",    e.getValue().size());
            group.put("statesFoundIn",     states);
            group.put("crossState",        states.size() > 1);
            group.put("members",           members);
            groups.add(group);
        }
        groups.sort((a, b) -> Integer.compare(
            (Integer) b.get("duplicateCount"), (Integer) a.get("duplicateCount")));

        long totalDuplicates = groups.stream()
            .mapToLong(g -> ((Integer) g.get("duplicateCount")) - 1L).sum();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("results", groups.stream().limit(limit).toList());
        response.put("totalGroups", groups.size());
        response.put("totalGhostEntries", totalDuplicates);
        return ApiResponse.success(response, "Duplicate identities retrieved.");
    }

    // ── 6. Impossible travel ───────────────────────────────────────────────
    @GetMapping("/impossible-travel")
    public ApiResponse<Map<String, Object>> impossibleTravel(
            @RequestParam(defaultValue = "50") int limit) {

        List<Beneficiary> all = beneRepo.findAll();
        List<Map<String, Object>> events = new ArrayList<>();

        for (Beneficiary b : all) {
            if (b.getClaimState() == null || b.getStateCode() == null
                    || b.getClaimState().equals(b.getStateCode())
                    || b.getRegisteredAt() == null || b.getLastClaimAt() == null) continue;

            double hours = Math.abs(ChronoUnit.HOURS.between(b.getRegisteredAt(), b.getLastClaimAt()));
            var ta = travelDetector.analyse(b.getStateCode(), b.getClaimState(), hours);
            if (!ta.isImpossible()) continue;

            Map<String, Object> event = new LinkedHashMap<>();
            event.put("beneficiaryId",   b.getId());
            event.put("name",            b.getFullName());
            event.put("homeState",       b.getStateCode());
            event.put("claimState",      b.getClaimState());
            event.put("distanceKm",      round(ta.distanceKm()));
            event.put("minTravelHours",  round(ta.minTravelHours()));
            event.put("actualGapHours",  round(ta.actualHours()));
            event.put("isOnorcMigrant",  b.getMigrant());
            event.put("explanation",     ta.explanation());
            event.put("fraudLayer",      "LAYER_3_VELOCITY");
            events.add(event);
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("results", events.stream().limit(limit).toList());
        map.put("totalImpossibleEvents", events.size());
        return ApiResponse.success(map, "Impossible travel events retrieved.");
    }

    // ── 7. Ghost flags ─────────────────────────────────────────────────────
    @GetMapping("/ghost-flags")
    public ApiResponse<Map<String, Object>> ghostFlags() {
        List<GhostDetectionService.GhostFlag> flags = ghostSvc.runAllLayers();
        int applied = ghostSvc.applyFlags(flags);

        List<Map<String, Object>> results = flags.stream().map(f -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("beneficiaryId", f.beneficiaryId());
            map.put("layer", f.layer());
            map.put("reason", f.reason());
            map.put("monthlyLossRs", f.estimatedMonthlyLossRs());
            return map;
        }).toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("results", results);
        response.put("totalFlags", flags.size());
        response.put("newlyApplied", applied);
        return ApiResponse.success(response, "Ghost detection complete.");
    }

    // ── 8. Fraud summary ───────────────────────────────────────────────────
    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> summary() {
        List<FraudRiskScore> all = scoringSvc.scoreAll();

        long high     = all.stream().filter(s -> s.riskLevel() == FraudRiskScore.RiskLevel.HIGH).count();
        long medium   = all.stream().filter(s -> s.riskLevel() == FraudRiskScore.RiskLevel.MEDIUM).count();
        long low      = all.stream().filter(s -> s.riskLevel() == FraudRiskScore.RiskLevel.LOW).count();
        long ghost    = beneRepo.countByStatus("GHOST");
        long fps      = fpsRepo.findByFlaggedTrue().size();
        long loss     = beneRepo.findByStatus("GHOST").stream()
            .mapToLong(b -> ghostSvc.estimateMonthlyLoss(b.getCategory())).sum();
        double avgScore = all.stream().mapToDouble(FraudRiskScore::riskScore).average().orElse(0);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("scoredAt",           LocalDateTime.now());
        data.put("totalScored",        all.size());
        data.put("highRiskCount",      high);
        data.put("mediumRiskCount",    medium);
        data.put("lowRiskCount",       low);
        data.put("ghostBeneficiaries", ghost);
        data.put("flaggedFpsShops",    fps);
        data.put("monthlyLossRs",      loss);
        data.put("annualLossRs",       loss * 12);
        data.put("avgRiskScore",       round(avgScore));
        data.put("detectionLayers",    List.of(
            "LAYER_1_DUPLICATE_AADHAAR", "LAYER_2_STATISTICAL_ANOMALY",
            "LAYER_3_VELOCITY_CROSS_STATE", "LAYER_4_AI_GROQ_REASONING",
            "LAYER_5_FRAUD_RISK_SCORE"));
        data.put("modelVersion",       "v5.0.0");

        return ApiResponse.success(data, "Fraud summary generated.");
    }

    // ── Helpers ────────────────────────────────────────────────────────────
    private Map<String, Object> buildScoreMap(FraudRiskScore score, boolean explain) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("beneficiaryId", score.subjectId());
        result.put("fullName",      score.subjectName());
        result.put("stateCode",     score.stateCode());
        result.put("fraudScore",    round(score.riskScore()));
        result.put("riskLevel",     score.riskLevel());
        result.put("fraudFlag",     (score.topFactors() != null && !score.topFactors().isEmpty()) ? score.topFactors().get(0) : "N/A");
        result.put("primaryFlag",   (score.topFactors() != null && !score.topFactors().isEmpty()) ? score.topFactors().get(0) : "N/A");
        result.put("flags",         score.topFactors());
        result.put("computedAt",    score.computedAt());

        if (explain && score.riskLevel() != FraudRiskScore.RiskLevel.LOW) {
            result.put("llmExplanation", explanationSvc.explain(score));
        }
        return result;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String getCorrelationId(HttpServletRequest request) {
        String cid = request.getHeader("X-Correlation-ID");
        return cid != null ? cid : UUID.randomUUID().toString();
    }

    public FraudAnalyticsController(BeneficiaryRepository beneRepo, FpsDeliveryRepository fpsRepo, FraudRiskScoringService scoringSvc, FraudExplanationService explanationSvc, ImpossibleTravelDetector travelDetector, GhostDetectionService ghostSvc, FraudEventProducer eventProducer) {
        this.beneRepo = beneRepo;
        this.fpsRepo = fpsRepo;
        this.scoringSvc = scoringSvc;
        this.explanationSvc = explanationSvc;
        this.travelDetector = travelDetector;
        this.ghostSvc = ghostSvc;
        this.eventProducer = eventProducer;
    }
}
