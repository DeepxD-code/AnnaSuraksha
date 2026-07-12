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
import java.util.*;
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

    private static final String DEFAULT_ADMIN_PW   = "Admin@123";
    private static final String DEFAULT_OFFICER_PW = "Officer@123";
    private static final String DEFAULT_AUDIT_PW    = "Audit@123";
    private static final String DEFAULT_FPS_PW      = "Fps@123";

    @Value("${annasuraksha.seed.demo-data:true}")
    private boolean seedEnabled;

    private static final String[] ALL_STATES = {
        "AP","AR","AS","BR","CG","DL","GA","GJ","HR","HP",
        "JH","KA","KL","MP","MH","MN","ME","MZ","NL","OD",
        "PB","RJ","SK","TN","TG","TR","UP","UK","WB"
    };

    private static final String[] CATEGORIES = {"BPL","APL","AAY","PHH"};

    private static final String[][] FRAUD_CASES = {
        {"Ghost User A",       "BR", "BPL", "1", "5",   "false", ""},
        {"Duplicate TN",       "TN", "BPL", "2", "10",  "false", ""},
        {"AAY Fraud MP",       "MP", "AAY", "1", "14",  "false", ""},
        {"Travel Fraud RJ",    "RJ", "PHH", "3", "15",  "true",  "KL"},
        {"Bulk Claimer WB",    "WB", "BPL", "2", "10",  "false", ""},
        {"FPS Clone A",        "BR", "BPL", "2", "10",  "false", ""},
        {"FPS Clone B",        "BR", "BPL", "1", "5",   "false", ""},
        {"Phone Share KA-1",   "KA", "PHH", "3", "15",  "false", ""},
        {"Phone Share KA-2",   "KA", "BPL", "4", "20",  "false", ""},
        {"Oversized Family UK","UK", "BPL", "14","35",  "false", ""},
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

        log.info("Demo data seeded — beneficiaries seeded, ghost detection complete.");
    }

    private void seedUsers() {
        if (userRepo.count() > 0) return;

        String adminPw   = System.getenv().getOrDefault("DEMO_ADMIN_PW", DEFAULT_ADMIN_PW);
        String officerPw = System.getenv().getOrDefault("DEMO_OFFICER_PW", DEFAULT_OFFICER_PW);
        String auditPw   = System.getenv().getOrDefault("DEMO_AUDIT_PW", DEFAULT_AUDIT_PW);
        String fpsPw     = System.getenv().getOrDefault("DEMO_FPS_PW", DEFAULT_FPS_PW);

        userRepo.saveAll(List.of(
            buildUser("admin@annasuraksha.gov.in",   adminPw,   List.of("ROLE_ADMIN"),        null,   "System Admin"),
            buildUser("officer@up.gov.in",           officerPw, List.of("ROLE_GOVT_OFFICER"),  "UP",   "UP District Officer"),
            buildUser("auditor@cag.gov.in",          auditPw,   List.of("ROLE_AUDITOR"),        null,   "CAG Auditor"),
            buildUser("fps@mh.gov.in",               fpsPw,     List.of("ROLE_FPS_OPERATOR"),   "MH",   "MH FPS Operator")
        ));

        log.info("Demo users created. Using fixed defaults unless overridden by DEMO_ADMIN_PW / DEMO_OFFICER_PW / DEMO_AUDIT_PW / DEMO_FPS_PW.");
    }

    private void seedBeneficiariesAndDeliveries() {
        String ghostAadhaarHash = blockchainSvc.hashAadhaar("999900001111");
        String phoneShareHash = "9876543210";

        List<Beneficiary> allBenes = new ArrayList<>();
        int idx = 0;

        // Generate 40 legitimate beneficiaries across 29 states
        for (int i = 0; i < 40; i++) {
            String state  = ALL_STATES[i % ALL_STATES.length];
            String cat    = CATEGORIES[i % CATEGORIES.length];
            int family    = 1 + rng.nextInt(6);
            int rice      = cat.equals("AAY") ? 14 : Math.max(5, family * 3 + rng.nextInt(5));
            boolean migrant = rng.nextDouble() < 0.1;
            String aadhaarRaw = "100" + (10000000 + i);
            String aadhaarHash = blockchainSvc.hashAadhaar(aadhaarRaw);
            String prevHash  = blockchainSvc.getLatestHash();
            long   height    = blockchainSvc.getNextBlockHeight();
            String blockHash = blockchainSvc.computeBlockHash(prevHash, aadhaarHash, "Legit User " + (i+1), state, cat);

            Beneficiary b = Beneficiary.builder()
                .fullName("Citizen " + (i+1) + " (" + state + ")")
                .stateCode(state).stateName(state)
                .district(cat)
                .category(cat).familySize(family)
                .aadhaarHash(aadhaarHash)
                .phone("91" + (700000000L + i * 10000 + rng.nextInt(9999)))
                .maskedAadhaar("XXXX-XXXX-" + (1000 + rng.nextInt(8999)))
                .riceKg(rice).wheatKg(cat.equals("AAY") ? 21 : family * 3).sugarKg(1)
                .fpsShopId(state + "-FPS-" + (100 + rng.nextInt(900)))
                .status("ACTIVE").migrant(migrant)
                .claimCount(1 + rng.nextInt(8))
                .blockHash(blockHash).prevBlockHash(prevHash).blockHeight(height)
                .registeredAt(LocalDateTime.now().minusMonths(6 + rng.nextInt(18)))
                .lastClaimAt(LocalDateTime.now().minusDays(rng.nextInt(45)))
                .build();
            allBenes.add(b);
            idx++;
        }

        // Add 10 fraud cases with known patterns
        for (String[] row : FRAUD_CASES) {
            String name     = row[0];
            String state    = row[1];
            String cat      = row[2];
            int    family   = Integer.parseInt(row[3]);
            int    rice     = Integer.parseInt(row[4]);
            boolean migrant = Boolean.parseBoolean(row[5]);
            String claimSt  = row[6].isBlank() ? null : row[6];

            String aadhaarRaw = (name.startsWith("Ghost") || name.startsWith("Duplicate"))
                ? "999900001111" : "200" + (10000000 + idx);
            String aadhaarHash = blockchainSvc.hashAadhaar(aadhaarRaw);

            // Phone sharing for the Phone Share cases
            String phone = name.startsWith("Phone Share") ? phoneShareHash : "91" + (800000000L + idx);

            String prevHash  = blockchainSvc.getLatestHash();
            long   height    = blockchainSvc.getNextBlockHeight();
            String blockHash = blockchainSvc.computeBlockHash(prevHash, aadhaarHash, name, state, cat);

            LocalDateTime regAt   = LocalDateTime.now().minusMonths(6 + rng.nextInt(18));
            LocalDateTime claimAt = claimSt != null
                ? regAt.plusHours(rng.nextInt(3))
                : LocalDateTime.now().minusDays(rng.nextInt(30));

            Beneficiary b = Beneficiary.builder()
                .fullName(name).stateCode(state).stateName(state)
                .category(cat).familySize(family)
                .aadhaarHash(aadhaarHash)
                .phone(phone)
                .maskedAadhaar("XXXX-XXXX-" + (1000 + rng.nextInt(8999)))
                .riceKg(rice).wheatKg(cat.equals("AAY") ? 21 : family * 3).sugarKg(1)
                .fpsShopId(state + "-FPS-" + (100 + rng.nextInt(900)))
                .status("ACTIVE").migrant(migrant).claimState(claimSt)
                .claimCount(name.contains("Bulk") ? 42 : 1 + rng.nextInt(12))
                .blockHash(blockHash).prevBlockHash(prevHash).blockHeight(height)
                .registeredAt(regAt).lastClaimAt(claimAt)
                .build();
            // Override specific fields for fraud patterns
            if (name.equals("FPS Clone A") || name.equals("FPS Clone B")) {
                b.setFpsShopId("FPS-BR-CLONE");
            }
            if (name.startsWith("Phone Share")) {
                b.setPhone(phoneShareHash);
            }
            allBenes.add(b);
            idx++;
        }

        beneRepo.saveAll(allBenes);
        log.info("Seeded {} beneficiaries ({} legitimate + {} fraud cases)",
            allBenes.size(), allBenes.size() - FRAUD_CASES.length, FRAUD_CASES.length);

        // Seed FPS deliveries for ALL beneficiaries
        int deliveryCount = 0;
        int flagCount = 0;
        for (Beneficiary b : allBenes) {
            for (int d = 0; d < 3; d++) {
                boolean flagged = rng.nextDouble() < 0.15;
                if (flagged) flagCount++;
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
                deliveryCount++;
            }
        }
        log.info("Seeded {} FPS deliveries ({} flagged)", deliveryCount, flagCount);
    }

    private void seedSupplyChain() {
        log.info("Seeding supply chain data...");
        try {
            SupplyChainEntry e1 = supplyChainSvc.warehouseLoad("WH-MAIN-001", "FPS-SHOP-123", "BR", "Patna", "OFF-99", "TRANS-PAT-X", 5000, 3000, 500);
            supplyChainSvc.fpsReceive(e1.getShipmentId(), "OP-77", 4200, 2400, 500);
            SupplyChainEntry e2 = supplyChainSvc.warehouseLoad("WH-EAST-042", "FPS-SHOP-999", "UP", "Lucknow", "OFF-44", "TRANS-UP-01", 2000, 2000, 200);
            supplyChainSvc.fpsReceive(e2.getShipmentId(), "OP-88", 1950, 1980, 200);
            supplyChainSvc.warehouseLoad("WH-WEST-007", "FPS-SHOP-MH-77", "MH", "Mumbai", "OFF-22", "TRANS-MH-01", 10000, 5000, 1000);
        } catch (Exception e) {
            log.error("Failed to seed supply chain: {}", e.getMessage());
        }
    }

    private void runGhostDetection() {
        var flags = ghostSvc.runAllLayers();
        int applied = ghostSvc.applyFlags(flags);
        log.info("Ghost detection on seed data — {} flags across {} layers, {} applied",
            flags.size(), 6, applied);
        // Log layer breakdown
        Map<String, Long> byLayer = flags.stream()
            .collect(Collectors.groupingBy(GhostDetectionService.GhostFlag::layer, Collectors.counting()));
        byLayer.forEach((layer, count) -> log.info("  {} → {} flags", layer, count));
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
