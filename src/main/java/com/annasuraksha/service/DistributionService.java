package com.annasuraksha.service;

import com.annasuraksha.model.*;
import com.annasuraksha.service.alert.AlertService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.Optional;

@Service
public class DistributionService {

    private final DistributionRepository distRepo;
    private final BeneficiaryRepository beneRepo;
    private final BlockchainService     blockchainSvc;
    private final AlertService          alertService;

    public DistributionEntry recordDistribution(Long beneficiaryId, String fpsId, int rice, int wheat, int sugar) {
        log.info("Recording distribution for beneficiary #{}", beneficiaryId);
        Beneficiary bene = beneRepo.findById(beneficiaryId)
            .orElseThrow(() -> new RuntimeException("Beneficiary #" + beneficiaryId + " not found."));
        log.info("Found beneficiary: {} rice={} wheat={}", bene.getFullName(), bene.getRiceKg(), bene.getWheatKg());

        List<String> violations = new ArrayList<>();
        if (rice > bene.getRiceKg())  violations.add("Rice: requested " + rice + "kg exceeds " + bene.getRiceKg() + "kg");
        if (wheat > bene.getWheatKg()) violations.add("Wheat: requested " + wheat + "kg exceeds " + bene.getWheatKg() + "kg");

        boolean isViolation = !violations.isEmpty();
        String reason = isViolation ? String.join("; ", violations) : null;

        if (isViolation) {
            log.warn("Entitlement violation for #{}: {}", beneficiaryId, reason);
            alertService.triggerSupplyAlert("DISCREPANCY", "DIST-" + beneficiaryId, 
                String.format("Illegal distribution to %s (ID #%d) at FPS %s. %s", 
                    bene.getFullName(), beneficiaryId, fpsId, reason));
        }

        // Blockchain integration
        String prevHash = getLatestHash();
        long   height   = getNextHeight();
        String entryHash = blockchainSvc.sha256(prevHash + beneficiaryId + rice + wheat + System.currentTimeMillis());
        log.info("Blockchain entry — height={} hash={}", height, entryHash.substring(0, 8));

        DistributionEntry entry = DistributionEntry.builder()
            .beneficiaryId(beneficiaryId)
            .beneficiaryName(bene.getFullName())
            .fpsShopId(fpsId)
            .riceKg(rice).wheatKg(wheat).sugarKg(sugar)
            .ruleViolation(isViolation)
            .violationReason(reason)
            .entryHash(entryHash).prevEntryHash(prevHash).entryHeight(height)
            .build();

        DistributionEntry saved = distRepo.save(entry);
        log.info("Distribution saved ID={}", saved.getId());
        return saved;
    }

    public List<DistributionEntry> getAll() {
        return distRepo.findAll();
    }

    private String getLatestHash() {
        return distRepo.findFirstByOrderByEntryHeightDesc().map(DistributionEntry::getEntryHash).orElse("0".repeat(64));
    }

    private long getNextHeight() {
        return distRepo.findFirstByOrderByEntryHeightDesc().map(e -> e.getEntryHeight() + 1).orElse(1L);
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DistributionService.class);

    public DistributionService(DistributionRepository distRepo, BeneficiaryRepository beneRepo, BlockchainService blockchainSvc, AlertService alertService) {
        this.distRepo = distRepo;
        this.beneRepo = beneRepo;
        this.blockchainSvc = blockchainSvc;
        this.alertService = alertService;
    }
}
