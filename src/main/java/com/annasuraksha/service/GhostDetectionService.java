package com.annasuraksha.service;

import com.annasuraksha.model.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class GhostDetectionService {

    private final BeneficiaryRepository repo;

    public record GhostFlag(Long beneficiaryId, String layer, String reason, long estimatedMonthlyLossRs) {}

    public List<GhostFlag> runAllLayers() {
        List<Beneficiary> all = repo.findAll();
        List<GhostFlag> flags = new ArrayList<>();
        flags.addAll(layer1DuplicateAadhaar(all));
        flags.addAll(layer2AbnormalPattern(all));
        flags.addAll(layer3VelocityAndCrossState(all));
        log.info("Ghost detection complete — {} flags raised", flags.size());
        return flags;
    }

    private List<GhostFlag> layer1DuplicateAadhaar(List<Beneficiary> all) {
        Map<String, List<Beneficiary>> byAadhaar = new HashMap<>();
        for (Beneficiary b : all) {
            if (b.getAadhaarHash() == null) continue;
            byAadhaar.computeIfAbsent(b.getAadhaarHash(), k -> new ArrayList<>()).add(b);
        }
        List<GhostFlag> flags = new ArrayList<>();
        for (Map.Entry<String, List<Beneficiary>> e : byAadhaar.entrySet()) {
            if (e.getValue().size() <= 1) continue;
            List<Beneficiary> dups = e.getValue().stream()
                .sorted(Comparator.comparing(
                    (Beneficiary b) -> b.getRegisteredAt() != null ? b.getRegisteredAt() : LocalDateTime.MIN)
                    .thenComparingLong(b -> b.getId() != null ? b.getId() : Long.MAX_VALUE))
                .toList();
            for (int i = 1; i < dups.size(); i++) {
                Beneficiary ghost = dups.get(i);
                if ("GHOST".equals(ghost.getStatus())) continue;
                flags.add(new GhostFlag(ghost.getId(), "LAYER_1_DUPLICATE",
                    "Duplicate Aadhaar registered in " + dups.get(0).getStateCode() + " and " + ghost.getStateCode(),
                    estimateMonthlyLoss(ghost.getCategory())));
                log.warn("Layer 1: ghost — beneficiary {} ({})", ghost.getId(), ghost.getFullName());
            }
        }
        return flags;
    }

    private List<GhostFlag> layer2AbnormalPattern(List<Beneficiary> all) {
        List<GhostFlag> flags = new ArrayList<>();
        for (Beneficiary b : all) {
            if (!"ACTIVE".equals(b.getStatus())) continue;
            if (!"AAY".equals(b.getCategory())) {
                int maxRice = (b.getFamilySize() != null ? b.getFamilySize() : 1) * 7;
                if (b.getRiceKg() != null && b.getRiceKg() > maxRice) {
                    flags.add(new GhostFlag(b.getId(), "LAYER_2_PATTERN",
                        "Rice entitlement " + b.getRiceKg() + "kg exceeds NFSA max " + maxRice + "kg",
                        estimateMonthlyLoss(b.getCategory())));
                }
            }
            if ("AAY".equals(b.getCategory()) && b.getFamilySize() != null && b.getFamilySize() < 3) {
                flags.add(new GhostFlag(b.getId(), "LAYER_2_PATTERN",
                    "AAY category assigned to household of " + b.getFamilySize() + " — anomalous",
                    estimateMonthlyLoss("AAY")));
            }
        }
        return flags;
    }

    private List<GhostFlag> layer3VelocityAndCrossState(List<Beneficiary> all) {
        List<GhostFlag> flags = new ArrayList<>();
        for (Beneficiary b : all) {
            if (!"ACTIVE".equals(b.getStatus())) continue;
            boolean crossState = b.getClaimState() != null && !b.getClaimState().isBlank()
                && !b.getClaimState().equals(b.getStateCode());
            if (!crossState) continue;

            if (!Boolean.TRUE.equals(b.getMigrant())) {
                flags.add(new GhostFlag(b.getId(), "LAYER_3_VELOCITY",
                    "Non-ONORC beneficiary in " + b.getStateCode() + " claimed in " + b.getClaimState(),
                    estimateMonthlyLoss(b.getCategory())));
            } else if (b.getLastClaimAt() != null && b.getRegisteredAt() != null) {
                long hours = Math.abs(ChronoUnit.HOURS.between(b.getRegisteredAt(), b.getLastClaimAt()));
                if (hours < 24) {
                    flags.add(new GhostFlag(b.getId(), "LAYER_3_VELOCITY",
                        "ONORC migrant claimed in " + b.getClaimState() + " within " + hours + "h of " + b.getStateCode(),
                        estimateMonthlyLoss(b.getCategory())));
                }
            }
        }
        return flags;
    }

    public int applyFlags(List<GhostFlag> flags) {
        int count = 0;
        for (GhostFlag flag : flags) {
            repo.findById(flag.beneficiaryId()).ifPresent(b -> {
                if (!"GHOST".equals(b.getStatus())) {
                    b.setStatus("GHOST");
                    b.setGhostReason(flag.reason());
                    b.setGhostLayer(flag.layer());
                    b.setFlaggedAt(LocalDateTime.now());
                    repo.save(b);
                }
            });
            count++;
        }
        return count;
    }

    public long estimateMonthlyLoss(String category) {
        if (category == null) return 96L;
        return switch (category) {
            case "AAY" -> 350L;
            case "BPL" -> 96L;
            case "PHH" -> 75L;
            case "APL" -> 60L;
            default    -> 96L;
        };
    }
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GhostDetectionService.class);
    public GhostDetectionService(BeneficiaryRepository repo) {
        this.repo = repo;
    }
}
