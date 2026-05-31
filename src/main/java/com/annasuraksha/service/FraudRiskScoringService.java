package com.annasuraksha.service;

import com.annasuraksha.model.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class FraudRiskScoringService {

    private final BeneficiaryRepository    beneRepo;
    private final FpsDeliveryRepository    fpsRepo;
    private final ImpossibleTravelDetector travelDetector;

    // ── Weights (must sum to 1.0) ──────────────────────────────────────────
    private static final double W_DUP_AADHAAR      = 0.20;
    private static final double W_DUP_VOTER_ID     = 0.04;
    private static final double W_DUP_PHONE        = 0.04;
    private static final double W_TRAVEL           = 0.18;
    private static final double W_CROSS_STATE      = 0.12;
    private static final double W_RATION_ANOMALY   = 0.09;
    private static final double W_CAT_MISMATCH     = 0.06;
    private static final double W_DEALER_DIVERSION = 0.06;
    private static final double W_PHONE_DUPLICATE  = 0.02;
    private static final double W_FAMILY_ANOMALY   = 0.03;
    private static final double W_CLAIM_FREQ       = 0.03;
    private static final double W_MULTI_SHOP       = 0.02;
    private static final double W_NIGHT_CLAIM      = 0.04;
    private static final double W_BULK_BURST       = 0.03;
    private static final double W_NEW_FPS          = 0.01;
    private static final double W_DISTRICT_RISK    = 0.01;
    private static final double W_REG_VELOCITY     = 0.02;
    // Total: 1.00

    private final Map<Long, CachedScore> scoreCache = new ConcurrentHashMap<>();
    private record CachedScore(FraudRiskScore score, long expiresAtMs) {}

    private long ttlMs(FraudRiskScore.RiskLevel level) {
        return switch (level) {
            case CRITICAL ->  2 * 60_000L;
            case HIGH     ->  5 * 60_000L;
            case MEDIUM   -> 15 * 60_000L;
            case LOW      -> 60 * 60_000L;
        };
    }

    // ── Public API ─────────────────────────────────────────────────────────

    public List<FraudRiskScore> scoreAll() {
        List<Beneficiary> all = beneRepo.findAll();

        Map<String, Long> aadhaarCount = all.stream()
            .filter(b -> b.getAadhaarHash() != null)
            .collect(Collectors.groupingBy(Beneficiary::getAadhaarHash, Collectors.counting()));

        Map<String, Long> phoneCount = all.stream()
            .filter(b -> b.getPhone() != null && !b.getPhone().isBlank())
            .collect(Collectors.groupingBy(Beneficiary::getPhone, Collectors.counting()));

        Map<String, DoubleSummaryStatistics> stateStats = all.stream()
            .filter(b -> b.getClaimCount() != null && b.getStateCode() != null)
            .collect(Collectors.groupingBy(
                Beneficiary::getStateCode,
                Collectors.summarizingDouble(b -> b.getClaimCount())));

        Map<Long, Long> registrationVelocity = computeRegistrationVelocity(all);
        Map<Long, long[]> dealerFlags = buildDealerFlagIndex();

        List<FraudRiskScore> scores = new ArrayList<>();
        for (Beneficiary b : all) {
            try {
                FraudFeatureVector fv = extractFeatures(b, aadhaarCount, phoneCount, stateStats, registrationVelocity, dealerFlags);
                FraudRiskScore score  = computeScore(b, fv);
                cacheScore(b.getId(), score);
                scores.add(score);
            } catch (Exception e) {
                log.warn("Scoring failed for beneficiary {}: {}", b.getId(), e.getMessage());
            }
        }

        scores.sort(Comparator.comparingDouble(FraudRiskScore::riskScore).reversed());
        log.info("Scoring complete — {} scored | CRITICAL:{} HIGH:{} MED:{} LOW:{}",
            scores.size(),
            scores.stream().filter(s -> s.riskLevel() == FraudRiskScore.RiskLevel.CRITICAL).count(),
            scores.stream().filter(s -> s.riskLevel() == FraudRiskScore.RiskLevel.HIGH).count(),
            scores.stream().filter(s -> s.riskLevel() == FraudRiskScore.RiskLevel.MEDIUM).count(),
            scores.stream().filter(s -> s.riskLevel() == FraudRiskScore.RiskLevel.LOW).count());
        return scores;
    }

    public Optional<FraudRiskScore> scoreById(Long id) {
        CachedScore cached = scoreCache.get(id);
        if (cached != null && System.currentTimeMillis() < cached.expiresAtMs()) {
            return Optional.of(cached.score());
        }

        return beneRepo.findById(id).map(b -> {
            long aadhaarCnt = b.getAadhaarHash() != null
                ? beneRepo.countByAadhaarHash(b.getAadhaarHash()) : 0L;
            Map<String, Long> aadhaarMap = b.getAadhaarHash() != null
                ? Map.of(b.getAadhaarHash(), aadhaarCnt) : Map.of();

            long phoneCnt = b.getPhone() != null && !b.getPhone().isBlank()
                ? beneRepo.countByPhone(b.getPhone()) : 0L;
            Map<String, Long> phoneMap = b.getPhone() != null && !b.getPhone().isBlank()
                ? Map.of(b.getPhone(), phoneCnt) : Map.of();

            DoubleSummaryStatistics singleStat = new DoubleSummaryStatistics();
            if (b.getClaimCount() != null) singleStat.accept(b.getClaimCount());
            Map<String, DoubleSummaryStatistics> statsMap = b.getStateCode() != null
                ? Map.of(b.getStateCode(), singleStat) : Map.of();

            Map<Long, Long> velocityMap = Map.of();
            Map<Long, long[]> dealerFlags = buildDealerFlagForOne(id);

            FraudFeatureVector fv = extractFeatures(b, aadhaarMap, phoneMap, statsMap, velocityMap, dealerFlags);
            FraudRiskScore score  = computeScore(b, fv);
            cacheScore(id, score);
            return score;
        });
    }

    public List<FraudRiskScore> getHighRisk() {
        return scoreAll().stream()
            .filter(s -> s.riskLevel() == FraudRiskScore.RiskLevel.HIGH
                      || s.riskLevel() == FraudRiskScore.RiskLevel.CRITICAL)
            .toList();
    }

    public List<FraudRiskScore> getTopRisk(int limit) {
        return scoreAll().stream().limit(limit).toList();
    }

    public void evictCache(Long beneficiaryId) {
        scoreCache.remove(beneficiaryId);
    }

    // ── Feature extraction ─────────────────────────────────────────────────

    FraudFeatureVector extractFeatures(Beneficiary b,
                                        Map<String, Long> aadhaarCount,
                                        Map<String, Long> phoneCount,
                                        Map<String, DoubleSummaryStatistics> stateStats,
                                        Map<Long, Long> registrationVelocity,
                                        Map<Long, long[]> dealerFlags) {

        // 1. Duplicate Aadhaar
        double dupAadhaar = (b.getAadhaarHash() != null
            && aadhaarCount.getOrDefault(b.getAadhaarHash(), 0L) > 1) ? 1.0 : 0.0;

        // 2. Duplicate voter ID
        double dupVoterId = "LAYER_1_DUPLICATE".equals(b.getGhostLayer()) ? 0.5 : 0.0;

        // 3. Duplicate phone number
        double dupPhone = 0.0;
        if (b.getPhone() != null && !b.getPhone().isBlank()) {
            long count = phoneCount.getOrDefault(b.getPhone(), 0L);
            if (count > 3) dupPhone = 1.0;
            else if (count > 1) dupPhone = 0.5;
        }

        // 4. Ration usage anomaly
        double rationAnomaly = 0.0;
        if (b.getRiceKg() != null && b.getFamilySize() != null
                && b.getFamilySize() > 0 && !"AAY".equals(b.getCategory())) {
            double maxLegal = b.getFamilySize() * 7.0;
            double ratio    = b.getRiceKg() / maxLegal;
            rationAnomaly   = Math.min(1.0, Math.max(0.0, (ratio - 0.9) * 10.0));
        }

        // 5. Category mismatch
        double catMismatch = 0.0;
        if ("AAY".equals(b.getCategory()) && b.getFamilySize() != null) {
            if      (b.getFamilySize() < 2) catMismatch = 1.0;
            else if (b.getFamilySize() < 3) catMismatch = 0.6;
        }

        // 6. Family size anomaly (single large family irregular)
        double familyAnomaly = 0.0;
        if (b.getFamilySize() != null) {
            if (b.getFamilySize() > 12) familyAnomaly = 1.0;
            else if (b.getFamilySize() > 8) familyAnomaly = 0.5;
            if ("BPL".equals(b.getCategory()) && b.getFamilySize() > 10) familyAnomaly = Math.max(familyAnomaly, 0.8);
        }

        // 7. Dealer diversion
        long[] flagInfo = dealerFlags.getOrDefault(b.getId(), new long[]{0, 0});
        double divRate  = flagInfo[1] == 0 ? 0.0 : (double) flagInfo[0] / flagInfo[1];

        // 8. Multi-shop claim
        int    claims    = b.getClaimCount() != null ? b.getClaimCount() : 0;
        double multiShop = claims > 24 ? Math.min(1.0, (claims - 24) / 12.0) : 0.0;

        // 9. Impossible travel
        double travelSignal = 0.0;
        if (b.getClaimState() != null && b.getStateCode() != null
                && !b.getClaimState().equals(b.getStateCode())
                && b.getRegisteredAt() != null && b.getLastClaimAt() != null) {
            double hours = Math.abs(ChronoUnit.HOURS.between(b.getRegisteredAt(), b.getLastClaimAt()));
            ImpossibleTravelDetector.TravelAnalysis ta =
                travelDetector.analyse(b.getStateCode(), b.getClaimState(), hours);
            travelSignal = ta.isImpossible() ? 1.0 : 0.0;
        }

        // 10. Cross-state fraud
        double crossState = 0.0;
        if (b.getClaimState() != null && !b.getClaimState().isBlank()
                && !b.getClaimState().equals(b.getStateCode())) {
            crossState = Boolean.TRUE.equals(b.getMigrant()) ? 0.3 : 1.0;
        }

        // 11. Claim frequency anomaly (z-score)
        double claimFreq = 0.0;
        DoubleSummaryStatistics stats = stateStats.get(b.getStateCode());
        if (stats != null && stats.getCount() > 1 && stats.getAverage() > 0) {
            double mean   = stats.getAverage();
            double stddev = Math.sqrt(mean);
            double z      = (claims - mean) / Math.max(stddev, 1.0);
            claimFreq     = Math.min(1.0, Math.max(0.0, (z - 2.0) / 3.0));
        }

        // 12. Night-time claim
        double nightClaim = 0.0;
        if (b.getLastClaimAt() != null) {
            LocalTime t = b.getLastClaimAt().toLocalTime();
            nightClaim = (t.isAfter(LocalTime.of(22, 0)) || t.isBefore(LocalTime.of(5, 0))) ? 1.0 : 0.0;
        }

        // 13. Bulk claim burst
        double bulkBurst = claims > 36 ? Math.min(1.0, (claims - 36) / 24.0) : 0.0;

        // 14. Registration velocity (many registrations from same FPS recently)
        double regVelocity = registrationVelocity.getOrDefault(b.getId(), 0L) / 10.0;
        regVelocity = Math.min(1.0, regVelocity);

        // 15-16. Placeholder signals
        double newFpsSignal = 0.0;
        double districtRisk = 0.0;

        // Confidence: based on number of active signals + strength of strongest signals
        double dupStrength = Math.max(dupAadhaar, Math.max(dupVoterId, dupPhone));
        double travelStrength = travelSignal;
        double compoundBonus = (dupStrength + travelStrength) > 1.5 ? 1.0 : 0.0;

        int evidenceCount = 0;
        double signalSum = 0.0;
        for (double val : new double[]{dupAadhaar, dupVoterId, dupPhone, rationAnomaly, catMismatch,
                familyAnomaly, divRate, multiShop, travelSignal, crossState,
                claimFreq, nightClaim, bulkBurst, regVelocity}) {
            if (val > 0) { evidenceCount++; signalSum += val; }
        }
        double avgSignal = evidenceCount > 0 ? signalSum / evidenceCount : 0.0;
        double confidence = Math.min(1.0, (evidenceCount / 6.0) + (avgSignal * 0.5) + (compoundBonus * 0.2));

        return new FraudFeatureVector(
            dupAadhaar, dupVoterId, dupPhone, rationAnomaly, catMismatch, familyAnomaly,
            divRate, multiShop, travelSignal, crossState, claimFreq,
            nightClaim, bulkBurst, regVelocity, newFpsSignal, districtRisk,
            confidence, evidenceCount,
            b.getId(), b.getFullName(), b.getStateCode(), b.getCategory()
        );
    }

    // ── Score computation ──────────────────────────────────────────────────

    FraudRiskScore computeScore(Beneficiary b, FraudFeatureVector f) {
        double raw =
            f.duplicateAadhaarSignal()    * W_DUP_AADHAAR
          + f.duplicateVoterIdSignal()     * W_DUP_VOTER_ID
          + f.duplicatePhoneSignal()       * W_DUP_PHONE
          + f.impossibleTravelSignal()     * W_TRAVEL
          + f.crossStateFraudSignal()      * W_CROSS_STATE
          + f.rationUsageAnomalyScore()    * W_RATION_ANOMALY
          + f.categoryMismatchSignal()     * W_CAT_MISMATCH
          + f.familySizeAnomalySignal()    * W_FAMILY_ANOMALY
          + f.dealerDiversionRate()        * W_DEALER_DIVERSION
          + f.claimFrequencyAnomaly()      * W_CLAIM_FREQ
          + f.multiShopClaimSignal()       * W_MULTI_SHOP
          + f.nightTimeClaimSignal()       * W_NIGHT_CLAIM
          + f.bulkClaimBurstScore()        * W_BULK_BURST
          + f.registrationVelocitySignal() * W_REG_VELOCITY
          + f.newFpsShopSignal()           * W_NEW_FPS
          + f.districtBaseRiskScore()      * W_DISTRICT_RISK;

        // Non-linear HIGH floor for critical signals
        if (f.duplicateAadhaarSignal() == 1.0) raw = Math.max(raw, 0.70);
        if (f.impossibleTravelSignal() == 1.0) raw = Math.max(raw, 0.75);
        if (f.bulkClaimBurstScore() > 0.5 && f.nightTimeClaimSignal() == 1.0) raw = Math.max(raw, 0.80);
        if (f.duplicateAadhaarSignal() == 1.0 && f.impossibleTravelSignal() == 1.0) raw = Math.max(raw, 0.90);
        if (f.duplicatePhoneSignal() > 0.5 && f.duplicateAadhaarSignal() == 1.0) raw = Math.max(raw, 0.85);

        double score = Math.min(1.0, Math.max(0.0, raw));

        String summary = buildDetectionSummary(f);

        return new FraudRiskScore(
            b.getId(), b.getFullName(), "BENEFICIARY", b.getStateCode(),
            score, f.confidenceScore(), f.evidenceCount(),
            FraudRiskScore.RiskLevel.from(score), summary,
            buildTopFactors(f), f, LocalDateTime.now()
        );
    }

    private String buildDetectionSummary(FraudFeatureVector f) {
        List<String> parts = new ArrayList<>();
        if (f.duplicateAadhaarSignal() > 0) parts.add("Aadhaar cloned across " + (int)(f.duplicateAadhaarSignal() * 10 + 1) + " registrations");
        if (f.impossibleTravelSignal() > 0) parts.add("physically impossible inter-state travel");
        if (f.crossStateFraudSignal() > 0) parts.add("illegal cross-state PDS claim");
        if (f.categoryMismatchSignal() > 0) parts.add("AAY category fraud");
        if (f.dealerDiversionRate() > 0.2) parts.add((int)(f.dealerDiversionRate()*100) + "% FPS diversion rate");
        if (f.bulkClaimBurstScore() > 0) parts.add("bulk claim anomaly (" + (int)(f.bulkClaimBurstScore()*100) + "% burst)");
        if (f.nightTimeClaimSignal() > 0) parts.add("off-hours claim (22:00-05:00)");
        if (f.registrationVelocitySignal() > 0) parts.add("suspicious registration velocity");
        if (f.familySizeAnomalySignal() > 0) parts.add("family size anomaly");
        if (parts.isEmpty()) parts.add("low-risk profile");
        return String.join("; ", parts);
    }

    private List<String> buildTopFactors(FraudFeatureVector f) {
        List<String> facs = new ArrayList<>();
        if (f.duplicateAadhaarSignal()  > 0) facs.add("DUPLICATE_AADHAAR("     + fmt(f.duplicateAadhaarSignal())  + ")");
        if (f.impossibleTravelSignal()  > 0) facs.add("IMPOSSIBLE_TRAVEL("     + fmt(f.impossibleTravelSignal())  + ")");
        if (f.crossStateFraudSignal()   > 0) facs.add("CROSS_STATE_FRAUD("     + fmt(f.crossStateFraudSignal())   + ")");
        if (f.rationUsageAnomalyScore() > 0) facs.add("RATION_ANOMALY("        + fmt(f.rationUsageAnomalyScore()) + ")");
        if (f.categoryMismatchSignal()  > 0) facs.add("CATEGORY_MISMATCH("     + fmt(f.categoryMismatchSignal())  + ")");
        if (f.dealerDiversionRate()     > 0) facs.add("DEALER_DIVERSION("      + fmt(f.dealerDiversionRate())     + ")");
        if (f.nightTimeClaimSignal()    > 0) facs.add("NIGHT_CLAIM("           + fmt(f.nightTimeClaimSignal())    + ")");
        if (f.bulkClaimBurstScore()     > 0) facs.add("BULK_BURST("            + fmt(f.bulkClaimBurstScore())     + ")");
        if (f.duplicatePhoneSignal()    > 0) facs.add("DUPLICATE_PHONE("       + fmt(f.duplicatePhoneSignal())    + ")");
        if (f.familySizeAnomalySignal() > 0) facs.add("FAMILY_ANOMALY("        + fmt(f.familySizeAnomalySignal()) + ")");
        if (f.registrationVelocitySignal() > 0) facs.add("HIGH_REG_VELOCITY("  + fmt(f.registrationVelocitySignal()) + ")");
        facs.sort(Comparator.reverseOrder());
        return facs.stream().limit(4).toList();
    }

    private String fmt(double v) { return String.format("%.2f", v); }

    private void cacheScore(Long id, FraudRiskScore score) {
        scoreCache.put(id, new CachedScore(score, System.currentTimeMillis() + ttlMs(score.riskLevel())));
    }

    private Map<Long, Long> computeRegistrationVelocity(List<Beneficiary> all) {
        Map<String, List<Beneficiary>> byFps = all.stream()
            .filter(b -> b.getFpsShopId() != null)
            .collect(Collectors.groupingBy(Beneficiary::getFpsShopId));
        Map<Long, Long> velocity = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        for (var entry : byFps.entrySet()) {
            List<Beneficiary> fpsBenes = entry.getValue();
            long recentCount = fpsBenes.stream()
                .filter(b -> b.getRegisteredAt() != null
                    && ChronoUnit.DAYS.between(b.getRegisteredAt(), now) < 90)
                .count();
            if (recentCount > 5) {
                for (Beneficiary b : fpsBenes) {
                    velocity.put(b.getId(), recentCount);
                }
            }
        }
        return velocity;
    }

    private Map<Long, long[]> buildDealerFlagIndex() {
        Map<Long, long[]> idx = new HashMap<>();
        try {
            for (FpsDelivery d : fpsRepo.findAll()) {
                if (d.getBeneficiaryId() == null) continue;
                idx.computeIfAbsent(d.getBeneficiaryId(), k -> new long[]{0, 0});
                idx.get(d.getBeneficiaryId())[1]++;
                if (Boolean.TRUE.equals(d.getFlagged())) idx.get(d.getBeneficiaryId())[0]++;
            }
        } catch (Exception e) {
            log.warn("Could not build dealer flag index: {}", e.getMessage());
        }
        return idx;
    }

    private Map<Long, long[]> buildDealerFlagForOne(Long beneficiaryId) {
        Map<Long, long[]> idx = new HashMap<>();
        try {
            List<FpsDelivery> deliveries = fpsRepo.findByBeneficiaryId(beneficiaryId);
            long flagged = deliveries.stream().filter(d -> Boolean.TRUE.equals(d.getFlagged())).count();
            idx.put(beneficiaryId, new long[]{flagged, deliveries.size()});
        } catch (Exception e) {
            log.warn("Could not build dealer flags for {}: {}", beneficiaryId, e.getMessage());
        }
        return idx;
    }
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FraudRiskScoringService.class);
    public FraudRiskScoringService(BeneficiaryRepository beneRepo, FpsDeliveryRepository fpsRepo, ImpossibleTravelDetector travelDetector) {
        this.beneRepo = beneRepo;
        this.fpsRepo = fpsRepo;
        this.travelDetector = travelDetector;
    }
}
