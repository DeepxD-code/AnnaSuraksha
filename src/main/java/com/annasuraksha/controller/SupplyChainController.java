package com.annasuraksha.controller;

import com.annasuraksha.model.*;
import com.annasuraksha.model.api.ApiResponse;
import com.annasuraksha.service.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/supply-chain")
public class SupplyChainController {

    private final SupplyChainService    supplyChainSvc;
    private final AuditLogService       auditLogSvc;
    private final BlockchainService     blockchainSvc;
    private final BeneficiaryRepository beneRepo;

    @PostMapping("/warehouse-load")
    public ApiResponse<Map<String, Object>> warehouseLoad(@RequestBody Map<String, Object> body) {
        try {
            SupplyChainEntry entry = supplyChainSvc.warehouseLoad(
                str(body, "warehouse_id"), str(body, "fps_shop_id"),
                str(body, "state_code"),   str(body, "district"),
                str(body, "warehouse_officer_id"),
                str(body, "transporter_id"),
                num(body, "rice_kg"), num(body, "wheat_kg"), num(body, "sugar_kg"));
            
            auditLogSvc.logSupplyChainUpdate(entry.getShipmentId(), "AUTO_DISPATCHED", str(body, "warehouse_officer_id"));
            
            return ApiResponse.success(Map.of(
                "shipmentId",  entry.getShipmentId(),
                "stage",       entry.getStage(),
                "entryHash",   entry.getEntryHash(),
                "entryHeight", entry.getEntryHeight(),
                "riceKg",      entry.getDispatchedRiceKg(),
                "wheatKg",     entry.getDispatchedWheatKg(),
                "createdAt",   entry.getCreatedAt()
            ), "Shipment loaded and auto-dispatched.");
        } catch (Exception e) {
            return ApiResponse.error("PROCESSING_ERROR", e.getMessage());
        }
    }

    @PostMapping("/dispatch")
    public ApiResponse<Map<String, Object>> dispatch(@RequestBody Map<String, Object> body) {
        try {
            SupplyChainEntry entry = supplyChainSvc.dispatch(str(body, "shipment_id"), str(body, "transporter_id"));
            auditLogSvc.logSupplyChainUpdate(entry.getShipmentId(), "DISPATCHED", str(body, "transporter_id"));
            return ApiResponse.success(Map.of(
                "shipmentId",  entry.getShipmentId(),
                "stage",       entry.getStage(),
                "entryHash",   entry.getEntryHash(),
                "dispatchedAt",entry.getDispatchedAt()
            ), "Shipment dispatched.");
        } catch (Exception e) {
            return ApiResponse.error("PROCESSING_ERROR", e.getMessage());
        }
    }

    @PostMapping("/fps-receive")
    public ApiResponse<Map<String, Object>> fpsReceive(@RequestBody Map<String, Object> body) {
        try {
            SupplyChainEntry entry = supplyChainSvc.fpsReceive(
                str(body, "shipment_id"), str(body, "fps_operator_id"),
                num(body, "received_rice_kg"), num(body, "received_wheat_kg"), num(body, "received_sugar_kg"));
            auditLogSvc.logSupplyChainUpdate(entry.getShipmentId(), "FPS_RECEIVED", str(body, "fps_operator_id"));

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("shipmentId",         entry.getShipmentId());
            result.put("stage",              entry.getStage());
            result.put("entryHash",          entry.getEntryHash());
            result.put("receivedAt",         entry.getReceivedAt());
            result.put("discrepancyFlagged", entry.getDiscrepancyFlagged());
            if (Boolean.TRUE.equals(entry.getDiscrepancyFlagged())) {
                result.put("discrepancyReason",  entry.getDiscrepancyReason());
                result.put("riceDiscrepancyKg",  entry.getRiceDiscrepancyKg());
                result.put("wheatDiscrepancyKg", entry.getWheatDiscrepancyKg());
                result.put("alert", "⚠️ GRAIN SHORTAGE — shipment flagged for investigation.");
            }
            return ApiResponse.success(result, "FPS receipt recorded.");
        } catch (Exception e) {
            return ApiResponse.error("PROCESSING_ERROR", e.getMessage());
        }
    }

    @GetMapping("/shipment/{shipmentId}")
    public ApiResponse<Map<String, Object>> shipmentHistory(@PathVariable String shipmentId) {
        List<SupplyChainEntry> history = supplyChainSvc.getShipmentHistory(shipmentId);
        if (history.isEmpty())
            return ApiResponse.error("NOT_FOUND", "Shipment " + shipmentId + " not found.");

        List<Map<String, Object>> stages = history.stream().map(e -> {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("stage",            e.getStage());
            s.put("entryHeight",      e.getEntryHeight());
            s.put("entryHash",        e.getEntryHash());
            s.put("prevHash",         e.getPrevEntryHash());
            s.put("dispatchedRiceKg", e.getDispatchedRiceKg());
            s.put("receivedRiceKg",   e.getReceivedRiceKg());
            s.put("flagged",          e.getDiscrepancyFlagged());
            s.put("timestamp",        e.getCreatedAt());
            return s;
        }).toList();

        return ApiResponse.success(Map.of(
            "shipmentId", shipmentId,
            "fpsShop",    history.get(0).getFpsShopId(),
            "state",      history.get(0).getStateCode(),
            "stageCount", stages.size(),
            "stages",     stages
        ), "Shipment history retrieved.");
    }

    @GetMapping("/recent")
    public ApiResponse<List<Map<String, Object>>> recent() {
        List<SupplyChainEntry> recent = supplyChainSvc.findRecentShipments();
        List<Map<String, Object>> results = recent.stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("shipmentId",  e.getShipmentId());
            m.put("warehouseId", e.getWarehouseId());
            m.put("fpsShop",    e.getFpsShopId());
            m.put("state",      e.getStateCode());
            m.put("riceKg",     e.getDispatchedRiceKg());
            m.put("wheatKg",    e.getDispatchedWheatKg());
            m.put("timestamp",  e.getCreatedAt());
            return m;
        }).toList();
        return ApiResponse.success(results, "Recent shipments retrieved.");
    }

    @GetMapping("/flagged")
    public ApiResponse<Map<String, Object>> flagged() {
        List<SupplyChainEntry> flagged = supplyChainSvc.getFlaggedDiscrepancies();
        List<Map<String, Object>> results = flagged.stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("shipmentId",        e.getShipmentId());
            m.put("fpsShop",           e.getFpsShopId());
            m.put("state",             e.getStateCode());
            m.put("discrepancyReason", e.getDiscrepancyReason());
            m.put("riceLostKg",        e.getRiceDiscrepancyKg());
            m.put("wheatLostKg",       e.getWheatDiscrepancyKg());
            m.put("estimatedLossRs",
                (e.getRiceDiscrepancyKg()  != null ? e.getRiceDiscrepancyKg()  * 30L : 0L) +
                (e.getWheatDiscrepancyKg() != null ? e.getWheatDiscrepancyKg() * 25L : 0L));
            m.put("flaggedAt",         e.getCreatedAt());
            return m;
        }).toList();

        return ApiResponse.success(Map.of(
            "flaggedCount", results.size(),
            "results",      results,
            "summary",      supplyChainSvc.getDiscrepancySummary()
        ), "Flagged shipments retrieved.");
    }

    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> summary() {
        Map<String, Object> data = supplyChainSvc.getDiscrepancySummary();
        data.put("generatedAt", LocalDateTime.now());
        return ApiResponse.success(data, "Supply chain summary generated.");
    }

    // ── Ledger endpoints ───────────────────────────────────────────────────

    @GetMapping("/ledger/verify")
    public ApiResponse<Map<String, Object>> verifyLedger() {
        boolean valid = blockchainSvc.validateChain();
        long    total = beneRepo.count();
        return ApiResponse.success(Map.of(
            "chainValid",      valid,
            "chainStatus",     valid ? "✅ INTACT — no tampering detected" : "❌ TAMPERED — chain broken",
            "blocksVerified",  total,
            "algorithm",       "SHA-256 hash chain",
            "generatedAt",     LocalDateTime.now()
        ), valid ? "Chain integrity verified." : "Chain integrity FAILED.");
    }

    @GetMapping("/ledger/audit")
    public ApiResponse<Map<String, Object>> auditLedger(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {

        List<Beneficiary> all   = beneRepo.findAllByOrderByBlockHeightAsc();
        int total = all.size();
        int from  = Math.min(page * size, total);
        int to    = Math.min(from + size, total);

        List<Map<String, Object>> blocks = all.subList(from, to).stream().map(b -> {
            Map<String, Object> block = new LinkedHashMap<>();
            block.put("blockHeight",   b.getBlockHeight());
            block.put("blockHash",     b.getBlockHash());
            block.put("prevHash",      b.getPrevBlockHash());
            block.put("shortHash",     blockchainSvc.shortHash(b.getBlockHash()));
            block.put("beneficiaryId", b.getId());
            block.put("name",          b.getFullName());
            block.put("state",         b.getStateCode());
            block.put("status",        b.getStatus());
            block.put("registeredAt",  b.getRegisteredAt());
            return block;
        }).toList();

        return ApiResponse.success(Map.of(
            "chainValid",  blockchainSvc.validateChain(),
            "totalBlocks", total,
            "page",        page,
            "pageSize",    size,
            "blocks",      blocks
        ), "Ledger audit retrieved.");
    }

    // ── Helpers ────────────────────────────────────────────────────────────
    private String str(Map<String, Object> m, String key) {
        Object v = m.get(key); return v != null ? v.toString() : "";
    }
    private int num(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return 0; }
    }
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SupplyChainController.class);
    public SupplyChainController(SupplyChainService supplyChainSvc, AuditLogService auditLogSvc, BlockchainService blockchainSvc, BeneficiaryRepository beneRepo) {
        this.supplyChainSvc = supplyChainSvc;
        this.auditLogSvc = auditLogSvc;
        this.blockchainSvc = blockchainSvc;
        this.beneRepo = beneRepo;
    }
}
