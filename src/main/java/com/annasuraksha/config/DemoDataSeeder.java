package com.annasuraksha.config;

import com.annasuraksha.model.*;
import com.annasuraksha.model.auth.User;
import com.annasuraksha.model.auth.UserRepository;
import com.annasuraksha.service.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Component
public class DemoDataSeeder implements CommandLineRunner {

    private final BeneficiaryRepository beneRepo;
    private final FpsDeliveryRepository fpsRepo;
    private final UserRepository        userRepo;
    private final BlockchainService     blockchainSvc;
    private final GhostDetectionService ghostSvc;
    private final SupplyChainService    supplyChainSvc;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${annasuraksha.seed.demo-data:true}")
    private boolean seedEnabled;

    private static final String[][] BENEFICIARIES = {
        // name, state, category, familySize, riceKg, migrant, claimState
        {"Ramesh Kumar",    "BR", "BPL", "4", "28",  "false", ""},
        {"Sunita Devi",     "UP", "PHH", "3", "15",  "false", ""},
        {"Arjun Singh",     "RJ", "AAY", "6", "14",  "false", ""},
        {"Priya Sharma",    "MH", "BPL", "5", "35",  "false", ""},
        {"Lakshmi Bai",     "TN", "PHH", "2", "10",  "false", ""},
        {"Mohammad Ali",    "GJ", "APL", "4", "12",  "true",  "MH"},  // ONORC migrant
        {"Geeta Pandey",    "MP", "BPL", "3", "15",  "false", ""},
        {"Vijay Yadav",     "JH", "AAY", "5", "14",  "false", ""},
        {"Anita Gupta",     "WB", "PHH", "4", "20",  "false", ""},
        {"Ravi Shankar",    "KA", "BPL", "3", "21",  "false", ""},
        // Fraud cases
        {"Ghost User A",    "BR", "BPL", "1", "5",   "false", ""},    // ghost - same aadhaar as Ramesh
        {"Duplicate TN",    "TN", "BPL", "2", "10",  "false", ""},    // ghost - cross state
        {"AAY Fraud MP",    "MP", "AAY", "1", "14",  "false", ""},    // category mismatch
        {"Travel Fraud RJ", "RJ", "PHH", "3", "15",  "true",  "KL"},  // impossible travel
        {"Bulk Claimer WB", "WB", "BPL", "2", "10",  "false", ""},    // high claim count
    };

    private final Random rng = new Random(42);

    @Override
    public void run(String... args) {
        if (!seedEnabled || beneRepo.count() > 0) return;
        log.info("Seeding demo data...");

        seedUsers();
        seedBeneficiariesAndDeliveries();
        seedSupplyChain();
        runGhostDetection();

        log.info("Demo data seeded — 15 beneficiaries, 45 FPS deliveries, supply chain discrepancies");
    }

    private void seedUsers() {
        if (userRepo.count() > 0) return;
        userRepo.saveAll(List.of(
            buildUser("admin@annasuraksha.gov.in",   "Admin@123",   List.of("ROLE_ADMIN"),        null,   "System Admin"),
            buildUser("officer@up.gov.in",           "Officer@123", List.of("ROLE_GOVT_OFFICER"),  "UP",   "UP District Officer"),
            buildUser("auditor@cag.gov.in",          "Audit@123",   List.of("ROLE_AUDITOR"),        null,   "CAG Auditor"),
            buildUser("fps@mh.gov.in",               "Fps@123",     List.of("ROLE_FPS_OPERATOR"),   "MH",   "MH FPS Operator")
        ));
        log.info("Demo users created — login with admin@annasuraksha.gov.in / Admin@123");
    }

    private void seedBeneficiariesAndDeliveries() {
        String ghostAadhaarHash = blockchainSvc.hashAadhaar("999900001111");

        for (int i = 0; i < BENEFICIARIES.length; i++) {
            String[] row   = BENEFICIARIES[i];
            String name    = row[0];
            String state   = row[1];
            String cat     = row[2];
            int    family  = Integer.parseInt(row[3]);
            int    rice    = Integer.parseInt(row[4]);
            boolean migrant= Boolean.parseBoolean(row[5]);
            String claimSt = row[6].isBlank() ? null : row[6];

            // Ghost beneficiaries share the same Aadhaar as Ramesh Kumar
            String aadhaarRaw = (i == 10) ? "999900001111" : "000" + (100000 + i);
            String aadhaarHash= blockchainSvc.hashAadhaar(aadhaarRaw);

            String prevHash  = blockchainSvc.getLatestHash();
            long   height    = blockchainSvc.getNextBlockHeight();
            String blockHash = blockchainSvc.computeBlockHash(prevHash, aadhaarHash, name, state, cat);

            LocalDateTime regAt   = LocalDateTime.now().minusMonths(6 + rng.nextInt(18));
            LocalDateTime claimAt = claimSt != null
                ? regAt.plusHours(rng.nextInt(3))   // impossible travel window
                : LocalDateTime.now().minusDays(rng.nextInt(30));

            Beneficiary b = Beneficiary.builder()
                .fullName(name).stateCode(state).stateName(state)
                .category(cat).familySize(family)
                .aadhaarHash(aadhaarHash)
                .maskedAadhaar("XXXX-XXXX-" + (1000 + rng.nextInt(8999)))
                .riceKg(rice).wheatKg(cat.equals("AAY") ? 21 : family * 3).sugarKg(1)
                .fpsShopId(state + "-FPS-" + (100 + rng.nextInt(900)))
                .status("ACTIVE").migrant(migrant).claimState(claimSt)
                .claimCount(i == 14 ? 42 : 1 + rng.nextInt(12))  // bulk claimer gets 42
                .blockHash(blockHash).prevBlockHash(prevHash).blockHeight(height)
                .registeredAt(regAt).lastClaimAt(claimAt)
                .build();
            beneRepo.save(b);

            // Seed FPS deliveries for this beneficiary
            for (int d = 0; d < 3; d++) {
                boolean flagged = rng.nextDouble() < 0.25;
                fpsRepo.save(FpsDelivery.builder()
                    .beneficiaryId(b.getId())
                    .fpsShopId(b.getFpsShopId())
                    .fpsOperatorName("Operator-" + rng.nextInt(50))
                    .stateCode(b.getStateCode())
                    .dealerRiceKg(b.getRiceKg() != null ? b.getRiceKg() : 5)
                    .confirmedRiceKg(flagged ? (b.getRiceKg() != null ? b.getRiceKg() / 2 : 2) : b.getRiceKg())
                    .flagged(flagged)
                    .flagReason(flagged ? "Beneficiary reported short delivery" : null)
                    .deliveryDate(LocalDateTime.now().minusDays(d * 30L))
                    .build());
            }
        }
    }

    private void seedSupplyChain() {
        log.info("Seeding supply chain data...");
        try {
            SupplyChainEntry e1 = supplyChainSvc.warehouseLoad("WH-MAIN-001", "FPS-SHOP-123", "BR", "Patna", "OFF-99", "TRANS-PAT-X", 5000, 3000, 500);
            supplyChainSvc.fpsReceive(e1.getShipmentId(), "OP-77", 4200, 2400, 500); // Massive discrepancy

            SupplyChainEntry e2 = supplyChainSvc.warehouseLoad("WH-EAST-042", "FPS-SHOP-999", "UP", "Lucknow", "OFF-44", "TRANS-UP-01", 2000, 2000, 200);
            supplyChainSvc.fpsReceive(e2.getShipmentId(), "OP-88", 1950, 1980, 200); // Minor
        } catch (Exception e) {
            log.error("Failed to seed supply chain: {}", e.getMessage());
        }
    }

    private void runGhostDetection() {
        var flags = ghostSvc.runAllLayers();
        int applied = ghostSvc.applyFlags(flags);
        log.info("Ghost detection on seed data — {} flags, {} applied", flags.size(), applied);
    }

    private User buildUser(String email, String password, List<String> roles,
                            String stateCode, String fullName) {
        return User.builder()
            .email(email).passwordHash(passwordEncoder.encode(password))
            .roles(roles).stateCode(stateCode).fullName(fullName)
            .active(true).build();
    }
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DemoDataSeeder.class);
    public DemoDataSeeder(BeneficiaryRepository beneRepo, FpsDeliveryRepository fpsRepo, UserRepository userRepo,
                          BlockchainService blockchainSvc, GhostDetectionService ghostSvc,
                          SupplyChainService supplyChainSvc, BCryptPasswordEncoder passwordEncoder) {
        this.beneRepo = beneRepo;
        this.fpsRepo = fpsRepo;
        this.userRepo = userRepo;
        this.blockchainSvc = blockchainSvc;
        this.ghostSvc = ghostSvc;
        this.supplyChainSvc = supplyChainSvc;
        this.passwordEncoder = passwordEncoder;
    }
}
