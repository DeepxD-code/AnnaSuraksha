package com.annasuraksha.service.demo;

import com.annasuraksha.model.*;
import com.annasuraksha.service.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class SimulationService {

    private final BeneficiaryRepository   beneRepo;
    private final FraudRiskScoringService  scoringSvc;
    private final FraudExplanationService  explanationSvc;
    private final BlockchainService        blockchainSvc;
    private final GhostDetectionService    ghostSvc;

    public enum ScenarioType {
        GHOST_BENEFICIARY, IMPOSSIBLE_TRAVEL, DEALER_DIVERSION,
        CATEGORY_FRAUD, BULK_CLAIM_BURST, SUPPLY_CHAIN_LEAK,
        CROSS_STATE_NON_ONORC, BIOMETRIC_DUPLICATE
    }

    public record SimulationResult(
        String            scenarioType,
        String            scenarioDescription,
        String            fraudNarrative,
        Object            fraudCase,
        Object            detectionResult,
        String            aiExplanation,
        String            estimatedMonthlyLoss,
        boolean           simulationMode,
        LocalDateTime     simulatedAt,
        Map<String,Object> metrics
    ) {}

    public SimulationResult runScenario(ScenarioType type) {
        log.info("[SIMULATION] Running scenario: {}", type);
        return switch (type) {
            case GHOST_BENEFICIARY     -> simulateGhost();
            case IMPOSSIBLE_TRAVEL     -> simulateImpossibleTravel();
            case DEALER_DIVERSION      -> simulateDealerDiversion();
            case CATEGORY_FRAUD        -> simulateCategoryFraud();
            case BULK_CLAIM_BURST      -> simulateBulkBurst();
            case SUPPLY_CHAIN_LEAK     -> simulateSupplyLeak();
            case CROSS_STATE_NON_ONORC -> simulateCrossState();
            case BIOMETRIC_DUPLICATE   -> simulateBiometric();
        };
    }

    public List<SimulationResult> runAllScenarios() {
        List<SimulationResult> results = new ArrayList<>();
        for (ScenarioType type : ScenarioType.values()) {
            try { results.add(runScenario(type)); }
            catch (Exception e) { log.error("Scenario {} failed: {}", type, e.getMessage()); }
        }
        return results;
    }

    public List<Map<String,Object>> listScenarios() {
        return List.of(
            scenario("GHOST_BENEFICIARY",     "Duplicate Aadhaar across Bihar + UP"),
            scenario("IMPOSSIBLE_TRAVEL",     "Claim in RJ and KL within 1.5 hours (2,200 km apart)"),
            scenario("DEALER_DIVERSION",      "FPS dealer flagged 80% of deliveries — grain to black market"),
            scenario("CATEGORY_FRAUD",        "AAY category for 1-person household"),
            scenario("BULK_CLAIM_BURST",      "5 ration claims in 72 hours across 3 shops"),
            scenario("SUPPLY_CHAIN_LEAK",     "40% grain missing between warehouse and FPS shop"),
            scenario("CROSS_STATE_NON_ONORC", "Non-ONORC beneficiary claiming in different state"),
            scenario("BIOMETRIC_DUPLICATE",   "Same Aadhaar hash in TN, KA, and KL simultaneously")
        );
    }

    public int cleanup() {
        List<Beneficiary> simRecords = beneRepo.findSimulationRecords();
        beneRepo.deleteAll(simRecords);
        log.info("Cleaned up {} simulation records", simRecords.size());
        return simRecords.size();
    }

    // ── Scenarios ──────────────────────────────────────────────────────────

    private SimulationResult simulateGhost() {
        String hash = blockchainSvc.hashAadhaar("999900001111");

        Beneficiary legit = saveSim(build("Rajesh Kumar",        "BR", "BPL", 4, hash, 28, 14,
            false, null, LocalDateTime.now().minusMonths(3), null));
        Beneficiary ghost = saveSim(build("Rajesh Kumar (Ghost)", "UP", "BPL", 1, hash, 5,  14,
            false, null, LocalDateTime.now().minusMonths(1), null));

        List<GhostDetectionService.GhostFlag> flags = ghostSvc.runAllLayers();
        ghostSvc.applyFlags(flags);

        FraudRiskScore score = scoringSvc.scoreById(ghost.getId()).orElseThrow();
        String explanation   = explanationSvc.explain(score);

        return new SimulationResult("GHOST_BENEFICIARY",
            "Two registrations share the same Aadhaar — Bihar (legitimate) and UP (ghost)",
            "Rajesh Kumar (ID:" + legit.getId() + ", Bihar) is legitimate. " +
            "A ghost registration (ID:" + ghost.getId() + ") was created in UP using the same Aadhaar, " +
            "collecting ₹96/month fraudulently. Detected by Layer 1 Aadhaar deduplication.",
            Map.of("legitimate_id", legit.getId(), "ghost_id", ghost.getId()),
            Map.of("flags", flags.size(), "layer", "LAYER_1_DUPLICATE_AADHAAR"),
            explanation, "₹96/month", true, LocalDateTime.now(),
            Map.of("risk_score", score.riskScore(), "risk_level", score.riskLevel().name()));
    }

    private SimulationResult simulateImpossibleTravel() {
        String hash = blockchainSvc.hashAadhaar("888700006661");
        Beneficiary b = saveSim(build("Sunita Devi", "RJ", "PHH", 3, hash, 15, 0,
            true, "KL",
            LocalDateTime.now().minusHours(2),
            LocalDateTime.now().minusMinutes(30)));

        FraudRiskScore score = scoringSvc.scoreById(b.getId()).orElseThrow();
        return new SimulationResult("IMPOSSIBLE_TRAVEL",
            "Beneficiary claimed in RJ and KL within 1.5 hours (2,200 km apart)",
            "Sunita Devi (ID:" + b.getId() + ") has a home PDS registration in Rajasthan. " +
            "A ration claim was recorded in Kerala 1.5 hours later — " +
            "a physically impossible journey of ~2,200 km (min travel ~6.7h by fastest flight).",
            Map.of("beneficiary_id", b.getId(), "home_state", "RJ", "claim_state", "KL"),
            Map.of("detection", "IMPOSSIBLE_TRAVEL", "gap_hours", 1.5, "distance_km", 2200),
            explanationSvc.explain(score), "₹75/month", true, LocalDateTime.now(),
            Map.of("risk_score", score.riskScore(), "distance_km", 2200));
    }

    private SimulationResult simulateDealerDiversion() {
        return new SimulationResult("DEALER_DIVERSION",
            "FPS shop MH-FPS-007: 80% flagged deliveries — grain diverted to black market",
            "FPS Dealer at shop MH-FPS-007 (Mumbai) has 12/15 deliveries flagged. " +
            "Beneficiaries confirmed receiving only 3–4 kg rice instead of 14 kg. " +
            "Shop throughput is 40% above licensed quota. Estimated monthly loss: ₹18,400.",
            Map.of("fps_shop_id", "MH-FPS-007", "flagged", 12, "total", 15),
            Map.of("diversion_rate", "80%", "detection", "DEALER_DIVERSION_RATE > 0.3"),
            "Dealer Santosh Patil flagged HIGH risk. Recommend license suspension under PDS Control Order 2001.",
            "₹18,400/month (23 families)", true, LocalDateTime.now(),
            Map.of("diversion_rate", 0.80, "families_affected", 23));
    }

    private SimulationResult simulateCategoryFraud() {
        String hash  = blockchainSvc.hashAadhaar("111222333444");
        Beneficiary b = saveSim(build("Mohan Sharma", "MP", "AAY", 1, hash, 14, 21,
            false, null, LocalDateTime.now().minusMonths(6), null));
        FraudRiskScore score = scoringSvc.scoreById(b.getId()).orElseThrow();
        return new SimulationResult("CATEGORY_FRAUD",
            "AAY category (for destitute families ≥3 members) assigned to 1-person household",
            "Mohan Sharma (ID:" + b.getId() + ", MP) is a 1-person household with AAY status. " +
            "AAY is reserved for 'poorest of the poor' multi-generational families. " +
            "Single-person AAY entitlement (₹350/month) indicates fraudulent category assignment.",
            Map.of("beneficiary_id", b.getId(), "category", "AAY", "family_size", 1),
            Map.of("detection", "CATEGORY_MISMATCH", "layer", "LAYER_2_PATTERN"),
            explanationSvc.explain(score), "₹350/month", true, LocalDateTime.now(),
            Map.of("risk_score", score.riskScore(), "monthly_loss_rs", 350));
    }

    private SimulationResult simulateBulkBurst() {
        return new SimulationResult("BULK_CLAIM_BURST",
            "5 ration claims in 72 hours across 3 FPS shops — normal is 1/month",
            "A WB beneficiary had 5 ration claims across 72h at 3 different FPS shops. " +
            "Normal entitlement is exactly 1 claim per month. This pattern indicates " +
            "a cloned ration card in circulation or FPS operators inflating fake claims.",
            Map.of("claims_in_72h", 5, "shops_used", 3, "expected", 1),
            Map.of("detection", "BULK_CLAIM_BURST + MULTI_SHOP", "anomaly_score", 0.85),
            "5 claims in 72 hours is 5x normal monthly entitlement. Freeze all 3 FPS shops.",
            "₹480/month (5× entitlement)", true, LocalDateTime.now(),
            Map.of("anomaly", "5x monthly claims", "estimated_loss_rs", 480));
    }

    private SimulationResult simulateSupplyLeak() {
        return new SimulationResult("SUPPLY_CHAIN_LEAK",
            "SHIP-OD-001: 500kg rice dispatched; FPS received only 300kg (40% missing)",
            "Shipment SHIP-OD-001 (Odisha) left warehouse with 500kg rice. " +
            "GPS showed a 35km detour near Cuttack wholesale market. " +
            "FPS OD-FPS-112 received only 300kg — 200kg unaccounted (₹6,000 loss).",
            Map.of("shipment_id", "SHIP-OD-001", "dispatched_kg", 500, "received_kg", 300),
            Map.of("discrepancy_flagged", true, "missing_pct", 40.0, "deviation_km", 35),
            "Transporter TRK-OD-2024 deviated 35km. File FIR under Essential Commodities Act.",
            "₹6,000 per shipment", true, LocalDateTime.now(),
            Map.of("missing_rice_kg", 200, "loss_rs", 6000));
    }

    private SimulationResult simulateCrossState() {
        String hash  = blockchainSvc.hashAadhaar("555666777888");
        Beneficiary b = saveSim(build("Priya Verma", "GJ", "BPL", 3, hash, 15, 0,
            false, "MH",
            LocalDateTime.now().minusMonths(4),
            LocalDateTime.now().minusDays(3)));
        FraudRiskScore score = scoringSvc.scoreById(b.getId()).orElseThrow();
        return new SimulationResult("CROSS_STATE_NON_ONORC",
            "Non-ONORC beneficiary (Gujarat) claimed in Maharashtra — illegal under NFSA 2013 S.7",
            "Priya Verma (ID:" + b.getId() + ") is registered in Gujarat, NOT enrolled in ONORC. " +
            "A claim was recorded in Maharashtra 3 days ago. This is a direct violation of NFSA 2013 S.7.",
            Map.of("beneficiary_id", b.getId(), "home_state", "GJ", "claim_state", "MH"),
            Map.of("detection", "CROSS_STATE_FRAUD", "is_onorc", false, "legal_violation", "NFSA_2013_S7"),
            explanationSvc.explain(score), "₹96/month", true, LocalDateTime.now(),
            Map.of("risk_score", score.riskScore(), "violation", "NFSA_2013_S7"));
    }

    private SimulationResult simulateBiometric() {
        String hash = blockchainSvc.hashAadhaar("777888999000");
        Beneficiary tn = saveSim(build("Suresh M",       "TN", "BPL", 3, hash, 15, 0, false, null,
            LocalDateTime.now().minusMonths(8), null));
        Beneficiary ka = saveSim(build("Suresh Murugan", "KA", "BPL", 3, hash, 15, 0, false, null,
            LocalDateTime.now().minusMonths(5), null));
        Beneficiary kl = saveSim(build("S. Murugan",     "KL", "BPL", 3, hash, 15, 0, false, null,
            LocalDateTime.now().minusMonths(2), null));

        FraudRiskScore score = scoringSvc.scoreById(kl.getId()).orElseThrow();
        return new SimulationResult("BIOMETRIC_DUPLICATE",
            "Same Aadhaar hash in TN, KA, KL under 3 different names",
            "One Aadhaar is registered as 'Suresh M' (TN/ID:" + tn.getId() + "), " +
            "'Suresh Murugan' (KA/ID:" + ka.getId() + "), and 'S. Murugan' (KL/ID:" + kl.getId() + "). " +
            "All share hash " + hash.substring(0, 12) + "... Annual loss: ₹3,456.",
            Map.of("states", List.of("TN", "KA", "KL"), "ids", List.of(tn.getId(), ka.getId(), kl.getId())),
            Map.of("detection", "LAYER_1_DUPLICATE_AADHAAR", "duplicate_count", 3, "cross_state", true),
            explanationSvc.explain(score), "₹288/month (3× BPL)", true, LocalDateTime.now(),
            Map.of("registrations", 3, "annual_loss_rs", 3456));
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private Beneficiary build(String name, String state, String category, int familySize,
                               String aadhaarHash, int riceKg, int wheatKg,
                               boolean migrant, String claimState,
                               LocalDateTime registeredAt, LocalDateTime lastClaimAt) {
        String prevHash  = blockchainSvc.getLatestHash();
        long   height    = blockchainSvc.getNextBlockHeight();
        String blockHash = blockchainSvc.computeBlockHash(prevHash, aadhaarHash, name, state, category);
        return Beneficiary.builder()
            .fullName(name).stateCode(state).stateName(state)
            .category(category).familySize(familySize)
            .aadhaarHash(aadhaarHash)
            .maskedAadhaar("XXXX-XXXX-" + (1000 + new Random().nextInt(8999)))
            .riceKg(riceKg).wheatKg(wheatKg)
            .status("ACTIVE").migrant(migrant).claimState(claimState)
            .claimCount(migrant ? 2 : 1)
            .blockHash(blockHash).prevBlockHash(prevHash).blockHeight(height)
            .registeredAt(registeredAt).lastClaimAt(lastClaimAt)
            .simulationMode(true)
            .build();
    }

    private Beneficiary saveSim(Beneficiary b) { return beneRepo.save(b); }

    private Map<String, Object> scenario(String type, String desc) {
        return Map.of("scenarioType", type, "description", desc);
    }
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SimulationService.class);
    public SimulationService(BeneficiaryRepository beneRepo, FraudRiskScoringService scoringSvc, FraudExplanationService explanationSvc, BlockchainService blockchainSvc, GhostDetectionService ghostSvc) {
        this.beneRepo = beneRepo;
        this.scoringSvc = scoringSvc;
        this.explanationSvc = explanationSvc;
        this.blockchainSvc = blockchainSvc;
        this.ghostSvc = ghostSvc;
    }
}
