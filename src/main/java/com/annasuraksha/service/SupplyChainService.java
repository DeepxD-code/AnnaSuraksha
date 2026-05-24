package com.annasuraksha.service;

import com.annasuraksha.model.*;
import com.annasuraksha.service.alert.AlertService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class SupplyChainService {

    private final SupplyChainRepository supplyRepo;
    private final BlockchainService     blockchainSvc;
    private final AlertService          alertService;

    private static final double DISCREPANCY_THRESHOLD = 0.05;

    @Transactional
    public SupplyChainEntry warehouseLoad(String warehouseId, String fpsShopId,
            String stateCode, String district, String officerId, String transporterId,
            int riceKg, int wheatKg, int sugarKg) {

        String shipmentId = "SHIP-" + stateCode + "-" + System.currentTimeMillis();
        
        // 1. Create LOADED entry
        SupplyChainEntry loadedEntry = SupplyChainEntry.builder()
            .stage(SupplyChainEntry.Stage.WAREHOUSE_LOADED)
            .shipmentId(shipmentId)
            .warehouseId(warehouseId).fpsShopId(fpsShopId)
            .stateCode(stateCode).district(district)
            .warehouseOfficerId(officerId)
            .dispatchedRiceKg(riceKg).dispatchedWheatKg(wheatKg).dispatchedSugarKg(sugarKg)
            .dispatchedAt(LocalDateTime.now())
            .prevEntryHash(getLatestHash()).entryHeight(getNextHeight())
            .build();
        loadedEntry.setEntryHash(computeEntryHash(loadedEntry));
        supplyRepo.save(loadedEntry);

        // 2. Create DISPATCHED entry immediately (Auto-Dispatch)
        SupplyChainEntry dispatchEntry = SupplyChainEntry.builder()
            .stage(SupplyChainEntry.Stage.DISPATCHED)
            .shipmentId(shipmentId)
            .warehouseId(warehouseId).fpsShopId(fpsShopId)
            .stateCode(stateCode).district(district)
            .transporterId(transporterId)
            .dispatchedRiceKg(riceKg).dispatchedWheatKg(wheatKg).dispatchedSugarKg(sugarKg)
            .dispatchedAt(LocalDateTime.now())
            .prevEntryHash(loadedEntry.getEntryHash()).entryHeight(loadedEntry.getEntryHeight() + 1)
            .build();
        dispatchEntry.setEntryHash(computeEntryHash(dispatchEntry));
        SupplyChainEntry saved = supplyRepo.save(dispatchEntry);

        log.info("Auto-Dispatched — {} | warehouse={} transporter={} rice={}kg", 
                 shipmentId, warehouseId, transporterId, riceKg);
        return saved;
    }

    @Transactional
    public SupplyChainEntry dispatch(String shipmentId, String transporterId) {
        SupplyChainEntry loaded = getLatestByStage(shipmentId, SupplyChainEntry.Stage.WAREHOUSE_LOADED);
        SupplyChainEntry entry  = SupplyChainEntry.builder()
            .stage(SupplyChainEntry.Stage.DISPATCHED)
            .shipmentId(shipmentId)
            .warehouseId(loaded.getWarehouseId()).fpsShopId(loaded.getFpsShopId())
            .stateCode(loaded.getStateCode()).district(loaded.getDistrict())
            .transporterId(transporterId)
            .dispatchedRiceKg(loaded.getDispatchedRiceKg())
            .dispatchedWheatKg(loaded.getDispatchedWheatKg())
            .dispatchedSugarKg(loaded.getDispatchedSugarKg())
            .dispatchedAt(LocalDateTime.now())
            .prevEntryHash(getLatestHash()).entryHeight(getNextHeight())
            .build();
        entry.setEntryHash(computeEntryHash(entry));
        log.info("Manual Dispatch (Legacy) — {} by {}", shipmentId, transporterId);
        return supplyRepo.save(entry);
    }

    @Transactional
    public SupplyChainEntry fpsReceive(String shipmentId, String operatorId,
            int receivedRice, int receivedWheat, int receivedSugar) {

        SupplyChainEntry dispatched = getLatestByStage(shipmentId, SupplyChainEntry.Stage.DISPATCHED);
        int riceDiff  = dispatched.getDispatchedRiceKg()  - receivedRice;
        int wheatDiff = dispatched.getDispatchedWheatKg() - receivedWheat;
        int sugarDiff = dispatched.getDispatchedSugarKg() - receivedSugar;

        boolean flagged = isDiscrepancy(dispatched.getDispatchedRiceKg(),  receivedRice)
                       || isDiscrepancy(dispatched.getDispatchedWheatKg(), receivedWheat)
                       || isDiscrepancy(dispatched.getDispatchedSugarKg(), receivedSugar);

        String reason = null;
        if (flagged) {
            List<String> parts = new ArrayList<>();
            if (riceDiff  > 0) parts.add("Rice: " + riceDiff  + "kg missing");
            if (wheatDiff > 0) parts.add("Wheat: " + wheatDiff + "kg missing");
            if (sugarDiff > 0) parts.add("Sugar: " + sugarDiff + "kg missing");
            reason = String.join("; ", parts);
            log.warn("SUPPLY DISCREPANCY — {} | {}", shipmentId, reason);
            
            // Trigger system alert
            alertService.triggerSupplyAlert("DISCREPANCY", shipmentId, 
                String.format("Mismatch at FPS %s for shipment %s. %s", 
                    dispatched.getFpsShopId(), shipmentId, reason));
        }

        SupplyChainEntry entry = SupplyChainEntry.builder()
            .stage(SupplyChainEntry.Stage.FPS_RECEIVED)
            .shipmentId(shipmentId)
            .warehouseId(dispatched.getWarehouseId()).fpsShopId(dispatched.getFpsShopId())
            .stateCode(dispatched.getStateCode()).district(dispatched.getDistrict())
            .fpsOperatorId(operatorId)
            .dispatchedRiceKg(dispatched.getDispatchedRiceKg())
            .dispatchedWheatKg(dispatched.getDispatchedWheatKg())
            .dispatchedSugarKg(dispatched.getDispatchedSugarKg())
            .receivedRiceKg(receivedRice).receivedWheatKg(receivedWheat).receivedSugarKg(receivedSugar)
            .riceDiscrepancyKg(riceDiff).wheatDiscrepancyKg(wheatDiff).sugarDiscrepancyKg(sugarDiff)
            .discrepancyFlagged(flagged).discrepancyReason(reason)
            .receivedAt(LocalDateTime.now())
            .prevEntryHash(getLatestHash()).entryHeight(getNextHeight())
            .build();
        entry.setEntryHash(computeEntryHash(entry));
        return supplyRepo.save(entry);
    }

    public List<SupplyChainEntry> getShipmentHistory(String shipmentId) {
        return supplyRepo.findByShipmentIdOrderByEntryHeightAsc(shipmentId);
    }

    public List<SupplyChainEntry> findRecentShipments() {
        return supplyRepo.findByStageOrderByCreatedAtDesc(SupplyChainEntry.Stage.DISPATCHED);
    }

    public List<SupplyChainEntry> getFlaggedDiscrepancies() {
        return supplyRepo.findByDiscrepancyFlaggedTrueOrderByCreatedAtDesc();
    }

    public Map<String, Object> getDiscrepancySummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalFlagged",    supplyRepo.countByDiscrepancyFlaggedTrue());
        summary.put("totalRiceLostKg", safeSum(supplyRepo.sumRiceDiscrepancy()));
        summary.put("totalWheatLostKg",safeSum(supplyRepo.sumWheatDiscrepancy()));
        summary.put("estimatedLossRs", safeSum(supplyRepo.sumRiceDiscrepancy()) * 30L
                                     + safeSum(supplyRepo.sumWheatDiscrepancy()) * 25L);
        return summary;
    }

    private SupplyChainEntry getLatestByStage(String shipmentId, SupplyChainEntry.Stage stage) {
        return supplyRepo.findByShipmentIdOrderByEntryHeightAsc(shipmentId)
            .stream().filter(e -> e.getStage() == stage)
            .reduce((f, s) -> s)
            .orElseThrow(() -> new RuntimeException("No " + stage + " entry for shipment " + shipmentId));
    }

    private boolean isDiscrepancy(int dispatched, int received) {
        if (dispatched == 0) return false;
        return Math.abs(dispatched - received) / (double) dispatched > DISCREPANCY_THRESHOLD;
    }

    private String getLatestHash() {
        return supplyRepo.findLatestEntry()
            .map(SupplyChainEntry::getEntryHash)
            .orElse("0".repeat(64));
    }

    private long getNextHeight() {
        return supplyRepo.findLatestEntry().map(e -> e.getEntryHeight() + 1).orElse(1L);
    }

    private String computeEntryHash(SupplyChainEntry e) {
        return blockchainSvc.sha256(
            e.getPrevEntryHash() + e.getShipmentId() + e.getStage()
            + e.getDispatchedRiceKg() + e.getDispatchedWheatKg() + System.currentTimeMillis());
    }

    private long safeSum(Long v) { return v == null ? 0L : v; }
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SupplyChainService.class);
    public SupplyChainService(SupplyChainRepository supplyRepo, BlockchainService blockchainSvc, AlertService alertService) {
        this.supplyRepo = supplyRepo;
        this.blockchainSvc = blockchainSvc;
        this.alertService = alertService;
    }
}
