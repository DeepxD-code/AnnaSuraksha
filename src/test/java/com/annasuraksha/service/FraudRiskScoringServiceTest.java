package com.annasuraksha.service;

import com.annasuraksha.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ============================================================================
 * STEP 3 — THE PAYOFF: ONE TEST PER FRAUD SIGNAL
 * ============================================================================
 *
 * This is the file the whole learning ladder (Steps 1 and 2) was building
 * toward. extractFeatures() computes 16 independent fraud signals from a
 * single Beneficiary. The technique used throughout this file is always
 * the same one:
 *
 *   1. Construct a Beneficiary object that is engineered to trigger EXACTLY
 *      ONE signal (or a small, deliberate combination when testing an
 *      interaction), while leaving every other field in its "boring, no
 *      fraud here" default state.
 *   2. Call extractFeatures() directly (no scoring, no weights yet — just
 *      the raw signal extraction).
 *   3. Assert on the ONE field of FraudFeatureVector that signal writes to.
 *
 * Reading these 16 tests, in order, top to bottom, is functionally
 * equivalent to reading a spec for what "fraud" means in this system —
 * which is exactly the ownership the council was pointing you toward.
 *
 * For each test, the comment states: (a) which weight this signal carries
 * in the real scoring formula (W_* constants in FraudRiskScoringService),
 * and (b) WHY that weight makes sense given what the signal detects. If you
 * can explain (b) in an interview without looking at this file, you own
 * that piece of the project.
 */
@ExtendWith(MockitoExtension.class)
class FraudRiskScoringServiceTest {

    @Mock private BeneficiaryRepository beneRepo;
    @Mock private FpsDeliveryRepository fpsRepo;

    private FraudRiskScoringService scoringService;
    private final ImpossibleTravelDetector realTravelDetector = new ImpossibleTravelDetector();

    // Empty/neutral lookup maps used by every test unless a specific test
    // needs to populate one to trigger its signal (duplicate counts, dealer
    // flag ratios, etc. all come from these maps rather than the DB directly
    // — extractFeatures() takes them as parameters, so we control them exactly).
    private Map<String, Long> emptyStringLongMap;
    private Map<String, DoubleSummaryStatistics> emptyStatsMap;
    private Map<Long, Long> emptyVelocityMap;
    private Map<Long, long[]> emptyDealerMap;

    @BeforeEach
    void setUp() {
        scoringService = new FraudRiskScoringService(beneRepo, fpsRepo, realTravelDetector);
        emptyStringLongMap = Map.of();
        emptyStatsMap = Map.of();
        emptyVelocityMap = Map.of();
        emptyDealerMap = Map.of();
    }

    /** A "boring" beneficiary with no fraud signals — the baseline every test starts from. */
    private Beneficiary.BeneficiaryBuilder cleanBeneficiary() {
        return Beneficiary.builder()
            .id(1L).fullName("Ordinary Citizen").stateCode("MH").category("BPL")
            .familySize(4).riceKg(20).claimCount(2).phone("9000000001")
            .aadhaarHash("UNIQUE_HASH_1").migrant(false);
    }

    private FraudFeatureVector extract(Beneficiary b, Map<String, Long> aadhaarCount,
                                        Map<String, Long> phoneCount, Map<Long, long[]> dealerFlags) {
        return scoringService.extractFeatures(b, aadhaarCount, phoneCount, emptyStatsMap, emptyVelocityMap, dealerFlags);
    }

    // ======================================================================
    // SIGNAL 1 — Duplicate Aadhaar (weight 0.20, highest of all 16 — this
    // is the single most heavily-weighted signal because a duplicate
    // Aadhaar hash is close to unambiguous proof of a ghost registration:
    // two different beneficiary records claiming the same national ID.)
    // ======================================================================
    @Test
    void signal1_duplicateAadhaar_firesWhenSameHashUsedTwice() {
        Beneficiary b = cleanBeneficiary().aadhaarHash("SHARED_HASH").build();
        // aadhaarCount map says "SHARED_HASH" appears 2 times across all beneficiaries
        Map<String, Long> aadhaarCount = Map.of("SHARED_HASH", 2L);

        FraudFeatureVector fv = extract(b, aadhaarCount, emptyStringLongMap, emptyDealerMap);

        assertEquals(1.0, fv.duplicateAadhaarSignal(),
            "Aadhaar hash shared by >1 beneficiary must fire this signal at full strength (1.0).");
    }

    @Test
    void signal1_duplicateAadhaar_doesNotFireWhenHashIsUnique() {
        Beneficiary b = cleanBeneficiary().aadhaarHash("ONLY_MINE").build();
        Map<String, Long> aadhaarCount = Map.of("ONLY_MINE", 1L);

        FraudFeatureVector fv = extract(b, aadhaarCount, emptyStringLongMap, emptyDealerMap);

        assertEquals(0.0, fv.duplicateAadhaarSignal(),
            "A uniquely-held Aadhaar hash must never trigger the duplicate signal.");
    }

    // ======================================================================
    // SIGNAL 2 — Duplicate Voter ID (weight 0.04 — much lower than Aadhaar,
    // because this system doesn't independently verify voter ID duplication;
    // it piggybacks on whatever GhostDetectionService already flagged via
    // ghostLayer == "LAYER_1_DUPLICATE", so it's treated as weaker/secondary
    // evidence rather than a primary signal.)
    // ======================================================================
    @Test
    void signal2_duplicateVoterId_firesAtHalfStrengthWhenGhostLayerMatches() {
        Beneficiary b = cleanBeneficiary().ghostLayer("LAYER_1_DUPLICATE").build();

        FraudFeatureVector fv = extract(b, emptyStringLongMap, emptyStringLongMap, emptyDealerMap);

        assertEquals(0.5, fv.duplicateVoterIdSignal(),
            "This signal is capped at 0.5 (not 1.0) — it's corroborating evidence from another layer, not independently verified.");
    }

    // ======================================================================
    // SIGNAL 3 — Duplicate Phone (weight 0.04). Two thresholds exist:
    // >3 shared registrations = full strength (organized fraud ring);
    // 2-3 shared = half strength (could be a legitimate shared family phone).
    // This tiered design is worth understanding: a phone shared by exactly
    // 2 people is common and often innocent (spouses); a phone shared by 5+
    // people is a strong organized-fraud tell.
    // ======================================================================
    @Test
    void signal3_duplicatePhone_firesFullStrengthAboveThreeShares() {
        Beneficiary b = cleanBeneficiary().phone("9999999999").build();
        Map<String, Long> phoneCount = Map.of("9999999999", 5L); // shared by 5 beneficiaries

        FraudFeatureVector fv = extract(b, emptyStringLongMap, phoneCount, emptyDealerMap);

        assertEquals(1.0, fv.duplicatePhoneSignal(),
            "A phone number shared by more than 3 beneficiaries indicates an organized ghost-family pattern (see GHOST_FAMILY_NETWORK scenario in the seeder).");
    }

    @Test
    void signal3_duplicatePhone_firesHalfStrengthForTwoOrThreeShares() {
        Beneficiary b = cleanBeneficiary().phone("8888888888").build();
        Map<String, Long> phoneCount = Map.of("8888888888", 2L);

        FraudFeatureVector fv = extract(b, emptyStringLongMap, phoneCount, emptyDealerMap);

        assertEquals(0.5, fv.duplicatePhoneSignal(),
            "2-3 shared registrations is treated as weaker evidence — could be a legitimately shared household phone.");
    }

    // ======================================================================
    // SIGNAL 4 — Ration Usage Anomaly (weight 0.09). NFSA caps entitlement
    // at roughly 7kg/family-member/month for non-AAY categories. This
    // signal ramps up linearly once claimed rice exceeds 90% of that legal
    // maximum — it's deliberately NOT a hard cliff at 100%, because legit
    // administrative rounding can push a claim slightly over without fraud.
    // ======================================================================
    @Test
    void signal4_rationAnomaly_firesWhenRiceExceedsNfsaCapForNonAay() {
        // familySize=4 -> maxLegal = 4*7 = 28kg. Claiming 40kg is a severe overshoot.
        Beneficiary b = cleanBeneficiary().category("BPL").familySize(4).riceKg(40).build();

        FraudFeatureVector fv = extract(b, emptyStringLongMap, emptyStringLongMap, emptyDealerMap);

        assertTrue(fv.rationUsageAnomalyScore() > 0.5,
            "Claiming 40kg rice for a family of 4 (legal max 28kg) is a severe NFSA violation and should score high.");
    }

    @Test
    void signal4_rationAnomaly_doesNotFireForAayCategory() {
        // AAY households have a flat, different entitlement structure and are
        // explicitly excluded from this ratio check in the source (!"AAY".equals(category)).
        Beneficiary b = cleanBeneficiary().category("AAY").familySize(1).riceKg(40).build();

        FraudFeatureVector fv = extract(b, emptyStringLongMap, emptyStringLongMap, emptyDealerMap);

        assertEquals(0.0, fv.rationUsageAnomalyScore(),
            "AAY category is explicitly exempt from this signal — it has its own separate entitlement rules, checked by signal 5 instead.");
    }

    // ======================================================================
    // SIGNAL 5 — Category Mismatch (weight 0.06). AAY ("poorest of the
    // poor", multi-generational) status assigned to a 1-person household is
    // a classic fraud pattern — this is the exact scenario the seeder's
    // CATEGORY_FRAUD simulation is built to demonstrate.
    // ======================================================================
    @Test
    void signal5_categoryMismatch_firesFullStrengthForSinglePersonAay() {
        Beneficiary b = cleanBeneficiary().category("AAY").familySize(1).build();

        FraudFeatureVector fv = extract(b, emptyStringLongMap, emptyStringLongMap, emptyDealerMap);

        assertEquals(1.0, fv.categoryMismatchSignal(),
            "AAY is reserved for destitute multi-person households — a lone individual with AAY status is a documented fraud pattern.");
    }

    @Test
    void signal5_categoryMismatch_doesNotFireForLegitimateAayFamily() {
        Beneficiary b = cleanBeneficiary().category("AAY").familySize(5).build();

        FraudFeatureVector fv = extract(b, emptyStringLongMap, emptyStringLongMap, emptyDealerMap);

        assertEquals(0.0, fv.categoryMismatchSignal(),
            "A 5-person AAY household is a legitimate, expected use of the category.");
    }

    // ======================================================================
    // SIGNAL 6 — Family Size Anomaly (weight 0.03). Two independent
    // over-large thresholds (>8 = 0.5, >12 = 1.0), with an extra floor for
    // BPL households specifically (>10 members forces at least 0.8) — BPL
    // has a lower "this is suspicious" bar than the general population
    // because BPL entitlement scales directly with family size, making it
    // the more attractive category to inflate.
    // ======================================================================
    @Test
    void signal6_familyAnomaly_firesAtFullStrengthAboveTwelveMembers() {
        Beneficiary b = cleanBeneficiary().category("APL").familySize(15).build();

        FraudFeatureVector fv = extract(b, emptyStringLongMap, emptyStringLongMap, emptyDealerMap);

        assertEquals(1.0, fv.familySizeAnomalySignal(),
            "Family sizes above 12 are extremely rare and score at full anomaly strength.");
    }

    @Test
    void signal6_familyAnomaly_bplFloorOverridesLowerGenericScore() {
        // familySize=11 alone would only trigger the ">8" tier (0.5), but the
        // BPL-specific floor (>10 members -> at least 0.8) must win via Math.max.
        Beneficiary b = cleanBeneficiary().category("BPL").familySize(11).build();

        FraudFeatureVector fv = extract(b, emptyStringLongMap, emptyStringLongMap, emptyDealerMap);

        assertEquals(0.8, fv.familySizeAnomalySignal(),
            "BPL households over 10 members are floored at 0.8 regardless of the generic >8/>12 tiers — BPL scales entitlement with family size, making inflation more attractive.");
    }

    // ======================================================================
    // SIGNAL 7 — Dealer Diversion (weight 0.06). Ratio of this
    // beneficiary's FLAGGED FPS deliveries to their TOTAL deliveries. This
    // is passed in pre-computed via the dealerFlags map (long[]{flaggedCount,
    // totalCount}) rather than queried live inside extractFeatures() — an
    // important architectural detail: extractFeatures() is otherwise a pure
    // function, and this is the one place state is injected as data instead.
    // ======================================================================
    @Test
    void signal7_dealerDiversion_computesRatioFromInjectedFlagCounts() {
        Beneficiary b = cleanBeneficiary().id(42L).build();
        // 3 out of 4 deliveries to this beneficiary were flagged by the dealer.
        Map<Long, long[]> dealerFlags = Map.of(42L, new long[]{3, 4});

        FraudFeatureVector fv = extract(b, emptyStringLongMap, emptyStringLongMap, dealerFlags);

        assertEquals(0.75, fv.dealerDiversionRate(), 0.0001,
            "3/4 flagged deliveries must compute to exactly 0.75, not be rounded or clamped.");
    }

    @Test
    void signal7_dealerDiversion_isZeroWhenNoDeliveriesRecorded() {
        Beneficiary b = cleanBeneficiary().id(99L).build();
        // No entry in dealerFlags at all for id=99 -> must default safely, not throw.

        FraudFeatureVector fv = extract(b, emptyStringLongMap, emptyStringLongMap, emptyDealerMap);

        assertEquals(0.0, fv.dealerDiversionRate(),
            "With zero total deliveries, the ratio must safely default to 0.0 (guard against divide-by-zero), never NaN or an exception.");
    }

    // ======================================================================
    // SIGNAL 8 — Multi-Shop Claim (weight 0.02). Only fires above 24 claims
    // total, ramping to full strength by 36 claims. Normal entitlement is
    // ~1 claim/month, so 24+ claims implies either multiple years of
    // accumulated legitimate claims OR a much faster illegitimate pattern —
    // this signal alone is weak evidence, which is why its weight is
    // among the lowest of all 16 (it needs corroboration from other signals).
    // ======================================================================
    @Test
    void signal8_multiShop_doesNotFireAtOrBelowTwentyFourClaims() {
        Beneficiary b = cleanBeneficiary().claimCount(24).build();

        FraudFeatureVector fv = extract(b, emptyStringLongMap, emptyStringLongMap, emptyDealerMap);

        assertEquals(0.0, fv.multiShopClaimSignal(),
            "The threshold is strictly greater-than 24, not greater-or-equal — exactly 24 claims must not fire.");
    }

    @Test
    void signal8_multiShop_ratchetsUpBetweenTwentyFiveAndThirtySix() {
        Beneficiary b = cleanBeneficiary().claimCount(30).build();

        FraudFeatureVector fv = extract(b, emptyStringLongMap, emptyStringLongMap, emptyDealerMap);

        // (30 - 24) / 12 = 0.5
        assertEquals(0.5, fv.multiShopClaimSignal(), 0.0001,
            "At 30 claims, signal should be exactly (30-24)/12 = 0.5 — a linear ramp, not a step function.");
    }

    // ======================================================================
    // SIGNAL 9 — Impossible Travel (weight 0.18, second-highest weight).
    // This is the signal that delegates to ImpossibleTravelDetector — the
    // exact class from Step 1 of this learning ladder. This test is proof
    // that the two services correctly compose: extractFeatures() calls
    // travelDetector.analyse() internally and turns a boolean into 0.0/1.0.
    // ======================================================================
    @Test
    void signal9_impossibleTravel_firesWhenClaimGapIsPhysicallyImpossible() {
        // RJ -> KL is ~2,200km apart. A 1.5 hour gap is the exact scenario
        // used in the SimulationService.simulateImpossibleTravel() seed case.
        LocalDateTime registeredAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime claimedAt    = LocalDateTime.of(2026, 1, 1, 11, 30); // 1.5h later
        Beneficiary b = cleanBeneficiary().stateCode("RJ").claimState("KL")
            .registeredAt(registeredAt).lastClaimAt(claimedAt).build();

        FraudFeatureVector fv = extract(b, emptyStringLongMap, emptyStringLongMap, emptyDealerMap);

        assertEquals(1.0, fv.impossibleTravelSignal(),
            "RJ to KL in 1.5 hours is ~2200km — far beyond any real travel mode's minimum time, so this must fire at full strength.");
    }

    @Test
    void signal9_impossibleTravel_doesNotFireForPlausibleGap() {
        // Same RJ -> KL route, but now with a 30-day gap — trivially feasible.
        LocalDateTime registeredAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime claimedAt    = LocalDateTime.of(2026, 1, 31, 10, 0);
        Beneficiary b = cleanBeneficiary().stateCode("RJ").claimState("KL")
            .registeredAt(registeredAt).lastClaimAt(claimedAt).build();

        FraudFeatureVector fv = extract(b, emptyStringLongMap, emptyStringLongMap, emptyDealerMap);

        assertEquals(0.0, fv.impossibleTravelSignal(),
            "A 30-day gap between RJ and KL is completely feasible by any transport mode — must not fire.");
    }

    // ======================================================================
    // SIGNAL 10 — Cross-State Fraud (weight 0.12). Distinct from Signal 9:
    // this doesn't care about TIME, only about WHETHER a non-ONORC migrant
    // is claiming outside their home state at all (NFSA 2013 S.7 violation).
    // Migrants get a discount (0.3 instead of 1.0) because ONORC
    // (One Nation One Ration Card) legally permits registered migrants to
    // claim across state lines — the discount reflects legal ambiguity,
    // not innocence.
    // ======================================================================
    @Test
    void signal10_crossState_firesAtFullStrengthForNonMigrant() {
        Beneficiary b = cleanBeneficiary().stateCode("GJ").claimState("MH").migrant(false).build();

        FraudFeatureVector fv = extract(b, emptyStringLongMap, emptyStringLongMap, emptyDealerMap);

        assertEquals(1.0, fv.crossStateFraudSignal(),
            "A non-ONORC beneficiary claiming outside their home state is a direct NFSA 2013 S.7 violation.");
    }

    @Test
    void signal10_crossState_isDiscountedForRegisteredMigrant() {
        Beneficiary b = cleanBeneficiary().stateCode("GJ").claimState("MH").migrant(true).build();

        FraudFeatureVector fv = extract(b, emptyStringLongMap, emptyStringLongMap, emptyDealerMap);

        assertEquals(0.3, fv.crossStateFraudSignal(),
            "ONORC migrants are legally permitted to claim cross-state, so this scores lower — not zero, since velocity (Signal 9) still applies separately.");
    }

    // ======================================================================
    // SIGNAL 11 — Claim Frequency Anomaly (z-score, weight 0.03). Needs a
    // populated stateStats map to compute mean/stddev, unlike most other
    // signals — so this test builds its own scoring service call with a
    // real DoubleSummaryStatistics instead of the shared empty map.
    // ======================================================================
    @Test
    void signal11_claimFrequency_firesForStatisticalOutlierWithinState() {
        // Simulate a state where most beneficiaries claim ~2x/period,
        // but this one has claimed 20 times — a clear outlier.
        DoubleSummaryStatistics stats = new DoubleSummaryStatistics();
        stats.accept(2); stats.accept(2); stats.accept(3); stats.accept(2);
        Map<String, DoubleSummaryStatistics> stateStats = Map.of("WB", stats);

        Beneficiary b = cleanBeneficiary().stateCode("WB").claimCount(20).build();
        FraudFeatureVector fv = scoringService.extractFeatures(
            b, emptyStringLongMap, emptyStringLongMap, stateStats, emptyVelocityMap, emptyDealerMap);

        assertTrue(fv.claimFrequencyAnomaly() > 0.0,
            "20 claims against a state average of ~2.25 is a significant statistical outlier and must register a nonzero anomaly score.");
    }

    // ======================================================================
    // SIGNAL 12 — Night-Time Claim (weight 0.04). Fixed window 22:00-05:00,
    // outside legal FPS operating hours. A hard boundary test at exactly
    // 22:00 and 21:59 is the meaningful edge case here.
    // ======================================================================
    @Test
    void signal12_nightClaim_firesAtExactlyTenPm() {
        Beneficiary b = cleanBeneficiary()
            .lastClaimAt(LocalDateTime.of(2026, 3, 1, 22, 0)).build();

        FraudFeatureVector fv = extract(b, emptyStringLongMap, emptyStringLongMap, emptyDealerMap);

        // Source uses isAfter(22:00), so exactly 22:00 is NOT "after" — this
        // is the kind of off-by-one boundary a test catches that reading
        // the code casually would miss.
        assertEquals(0.0, fv.nightTimeClaimSignal(),
            "The source checks isAfter(22:00) — exactly 22:00:00 is NOT strictly after 22:00, so this boundary tick does not yet fire. One minute later, it would.");
    }

    @Test
    void signal12_nightClaim_firesOneMinuteAfterTenPm() {
        Beneficiary b = cleanBeneficiary()
            .lastClaimAt(LocalDateTime.of(2026, 3, 1, 22, 1)).build();

        FraudFeatureVector fv = extract(b, emptyStringLongMap, emptyStringLongMap, emptyDealerMap);

        assertEquals(1.0, fv.nightTimeClaimSignal(),
            "22:01 is strictly after 22:00 and must fire — confirming the exact boundary found in the previous test.");
    }

    // ======================================================================
    // SIGNAL 13 — Bulk Claim Burst (weight 0.03). Similar ramp shape to
    // Signal 8 (Multi-Shop) but with a higher threshold (36 vs 24) and
    // wider ramp window (24 vs 12) — deliberately requires MORE evidence
    // before firing, since it's meant to catch a different, more extreme
    // pattern (5 claims in 72 hours in the BULK_CLAIM_BURST seed scenario).
    // ======================================================================
    @Test
    void signal13_bulkBurst_doesNotFireAtThirtySixClaims() {
        Beneficiary b = cleanBeneficiary().claimCount(36).build();

        FraudFeatureVector fv = extract(b, emptyStringLongMap, emptyStringLongMap, emptyDealerMap);

        assertEquals(0.0, fv.bulkClaimBurstScore(),
            "Threshold is strictly greater-than 36 — exactly 36 must not fire, mirroring the Signal 8 boundary pattern.");
    }

    // ======================================================================
    // SIGNAL 14 — Registration Velocity (weight 0.02). Passed in
    // pre-computed via registrationVelocity map (count of recent same-FPS
    // registrations), then divided by 10 and clamped to [0,1] — meaning a
    // raw velocity of 10+ registrations already saturates this signal.
    // ======================================================================
    @Test
    void signal14_registrationVelocity_clampsAtOneForHighRawVelocity() {
        Beneficiary b = cleanBeneficiary().id(7L).build();
        Map<Long, Long> velocityMap = Map.of(7L, 25L); // raw velocity way above the /10 clamp point

        FraudFeatureVector fv = scoringService.extractFeatures(
            b, emptyStringLongMap, emptyStringLongMap, emptyStatsMap, velocityMap, emptyDealerMap);

        assertEquals(1.0, fv.registrationVelocitySignal(),
            "25/10 = 2.5, which must be clamped to the maximum of 1.0 — the signal is a saturating function, not unbounded.");
    }

    @Test
    void signal14_registrationVelocity_scalesLinearlyBelowClampPoint() {
        Beneficiary b = cleanBeneficiary().id(8L).build();
        Map<Long, Long> velocityMap = Map.of(8L, 6L);

        FraudFeatureVector fv = scoringService.extractFeatures(
            b, emptyStringLongMap, emptyStringLongMap, emptyStatsMap, velocityMap, emptyDealerMap);

        assertEquals(0.6, fv.registrationVelocitySignal(), 0.0001,
            "6/10 = 0.6, below the clamp point, should pass through unmodified.");
    }

    // ======================================================================
    // SIGNALS 15 & 16 — New FPS Shop / District Base Risk (weights 0.01
    // each, the two lowest-weighted signals in the entire system). The
    // source code hard-codes both to 0.0 — they are explicitly documented
    // placeholders for future signals not yet implemented. This test exists
    // specifically to PROVE that, and to make sure nobody accidentally
    // "fixes" this into silently returning garbage instead of a clear zero.
    // ======================================================================
    @Test
    void signals15and16_newFpsAndDistrictRisk_areDocumentedPlaceholdersReturningZero() {
        Beneficiary b = cleanBeneficiary().build();

        FraudFeatureVector fv = extract(b, emptyStringLongMap, emptyStringLongMap, emptyDealerMap);

        assertEquals(0.0, fv.newFpsShopSignal(),
            "Signal 15 is an unimplemented placeholder in the current codebase — must stay exactly 0.0, not null or NaN.");
        assertEquals(0.0, fv.districtBaseRiskScore(),
            "Signal 16 is likewise an unimplemented placeholder — same requirement.");
    }

    // ======================================================================
    // BONUS — evidenceCount and confidenceScore. These aren't separate
    // "signals" but are the meta-fields computed FROM the 16 signals, and
    // they're what the confidence-based caching and CRITICAL-tier compound
    // floors (in computeScore(), not tested in this file) actually rely on.
    // Worth a dedicated test because a bug here silently corrupts every
    // downstream risk tier without any single signal looking wrong.
    // ======================================================================
    @Test
    void evidenceCount_onlyCountsSignalsThatAreStrictlyPositive() {
        // Craft a beneficiary that fires EXACTLY 2 signals: duplicate Aadhaar
        // and category mismatch. Every other field is deliberately "clean."
        Beneficiary b = cleanBeneficiary()
            .aadhaarHash("DUP_HASH").category("AAY").familySize(1).build();
        Map<String, Long> aadhaarCount = Map.of("DUP_HASH", 2L);

        FraudFeatureVector fv = extract(b, aadhaarCount, emptyStringLongMap, emptyDealerMap);

        assertEquals(2, fv.evidenceCount(),
            "Exactly 2 signals were engineered to fire (duplicateAadhaar, categoryMismatch) — evidenceCount must match precisely, proving no other signal leaked a false positive from this input.");
    }

    @Test
    void confidenceScore_isZeroWhenNoSignalsFireAtAll() {
        Beneficiary b = cleanBeneficiary().build(); // deliberately clean, zero fraud signals

        FraudFeatureVector fv = extract(b, emptyStringLongMap, emptyStringLongMap, emptyDealerMap);

        assertEquals(0.0, fv.confidenceScore(),
            "With zero active signals, confidence must be exactly 0.0 — a clean record should never carry residual 'suspicion' from the confidence formula.");
    }
}
