package com.annasuraksha.controller;

import com.annasuraksha.model.*;
import com.annasuraksha.model.api.ApiResponse;
import com.annasuraksha.service.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/beneficiary")
public class BeneficiaryController {

    private final BeneficiaryRepository  beneRepo;
    private final BlockchainService      blockchainSvc;
    private final AuditLogService        auditLogSvc;
    private final FraudRiskScoringService scoringSvc;

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String state) {
        List<Beneficiary> all = beneRepo.findAll();
        if (state != null && !state.isBlank())
            all = all.stream().filter(b -> state.equalsIgnoreCase(b.getStateCode())).toList();
        int total = all.size();
        int from  = Math.min(page * size, total);
        int to    = Math.min(from + size, total);
        List<Map<String, Object>> content = all.subList(from, to).stream().map(b -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",           b.getId());
            m.put("fullName",     b.getFullName());
            m.put("stateCode",    b.getStateCode());
            m.put("stateName",    b.getStateName());
            m.put("category",     b.getCategory());
            m.put("status",       b.getStatus());
            m.put("familySize",   b.getFamilySize());
            m.put("migrant",      b.getMigrant());
            m.put("fpsShopId",    b.getFpsShopId());
            m.put("riceKg",       b.getRiceKg());
            m.put("claimCount",   b.getClaimCount());
            return m;
        }).toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content",       content);
        result.put("totalElements", total);
        result.put("page",          page);
        result.put("size",          size);
        return ApiResponse.success(result, total + " beneficiaries.");
    }

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@RequestBody Map<String, Object> body) {
        try {
            String name     = str(body, "fullName");
            String state    = str(body, "stateCode");
            String category = str(body, "category");
            int    family   = num(body, "familySize");
            String aadhaar  = str(body, "aadhaarRaw");

            if (name.isBlank() || state.isBlank() || category.isBlank() || aadhaar.isBlank())
                return ApiResponse.error("VALIDATION_ERROR", "fullName, stateCode, category, aadhaarRaw are required.");

            String aadhaarHash = blockchainSvc.hashAadhaar(aadhaar);
            String prevHash    = blockchainSvc.getLatestHash();
            long   height      = blockchainSvc.getNextBlockHeight();
            String blockHash   = blockchainSvc.computeBlockHash(prevHash, aadhaarHash, name, state, category);

            int rice, wheat;
            if ("AAY".equalsIgnoreCase(category)) {
                rice = 14; wheat = 21; // Total 35kg/family
            } else if ("BPL".equalsIgnoreCase(category)) {
                rice = Math.max(25, family * 5); wheat = Math.max(10, family * 2);
            } else if ("PHH".equalsIgnoreCase(category) || "Priority Household".equalsIgnoreCase(category)) {
                rice = family * 5; wheat = family * 3;
            } else if ("APL".equalsIgnoreCase(category)) {
                rice = family * 2; wheat = family * 1;
            } else {
                rice = family * 3; wheat = family * 2;
            }

            Beneficiary b = Beneficiary.builder()
                .fullName(name).stateCode(state).category(category).familySize(family)
                .aadhaarHash(aadhaarHash)
                .maskedAadhaar("XXXX-XXXX-" + aadhaar.replaceAll("[^0-9]","").substring(
                    Math.max(0, aadhaar.replaceAll("[^0-9]","").length() - 4)))
                .riceKg(rice).wheatKg(wheat).sugarKg(1)
                .fpsShopId(str(body, "fpsShopId"))
                .migrant(Boolean.parseBoolean(str(body, "migrant")))
                .blockHash(blockHash).prevBlockHash(prevHash).blockHeight(height)
                .build();

            Beneficiary saved = beneRepo.save(b);
            auditLogSvc.logApiAccess("system", null, "POST", "/api/beneficiary/register", 200, 0L);

            return ApiResponse.success(Map.<String, Object>of(
                "beneficiaryId", saved.getId(),
                "maskedAadhaar", saved.getMaskedAadhaar(),
                "blockHeight",   saved.getBlockHeight(),
                "blockHash",     blockchainSvc.shortHash(saved.getBlockHash()),
                "riceKg",        saved.getRiceKg(),
                "wheatKg",       saved.getWheatKg(),
                "status",        saved.getStatus()
            ), "Beneficiary registered and added to blockchain ledger.");
        } catch (Exception e) {
            return ApiResponse.error("REGISTRATION_ERROR", e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> getById(@PathVariable Long id) {
        return beneRepo.findById(id).map(b -> {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id",           b.getId());
            data.put("fullName",     b.getFullName());
            data.put("stateCode",    b.getStateCode());
            data.put("category",     b.getCategory());
            data.put("familySize",   b.getFamilySize());
            data.put("maskedAadhaar",b.getMaskedAadhaar());
            data.put("riceKg",       b.getRiceKg());
            data.put("wheatKg",      b.getWheatKg());
            data.put("status",       b.getStatus());
            data.put("migrant",      b.getMigrant());
            data.put("claimState",   b.getClaimState());
            data.put("claimCount",   b.getClaimCount());
            data.put("blockHeight",  b.getBlockHeight());
            data.put("shortHash",    blockchainSvc.shortHash(b.getBlockHash()));
            data.put("registeredAt", b.getRegisteredAt());
            data.put("ghostReason",  b.getGhostReason());
            return ApiResponse.success(data, "Beneficiary retrieved.");
        }).orElse(ApiResponse.error("NOT_FOUND", "Beneficiary #" + id + " not found."));
    }

    @GetMapping("/search")
    public ApiResponse<List<Map<String, Object>>> search(
            @RequestParam String q,
            @RequestParam(required = false) String state) {
        List<Beneficiary> found = beneRepo.search(q);
        if (state != null && !state.isBlank())
            found = found.stream().filter(b -> state.equalsIgnoreCase(b.getStateCode())).toList();

        List<Map<String, Object>> results = found.stream().limit(50).map(b -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",           b.getId());
            m.put("fullName",     b.getFullName());
            m.put("stateCode",    b.getStateCode());
            m.put("category",     b.getCategory());
            m.put("status",       b.getStatus());
            m.put("maskedAadhaar",b.getMaskedAadhaar());
            return m;
        }).toList();

        return ApiResponse.success(results, results.size() + " results for '" + q + "'.");
    }

    private String str(Map<String, Object> m, String key) {
        Object v = m.get(key); return v != null ? v.toString() : "";
    }
    private int num(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return 0; }
    }
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BeneficiaryController.class);
    public BeneficiaryController(BeneficiaryRepository beneRepo, BlockchainService blockchainSvc, AuditLogService auditLogSvc, FraudRiskScoringService scoringSvc) {
        this.beneRepo = beneRepo;
        this.blockchainSvc = blockchainSvc;
        this.auditLogSvc = auditLogSvc;
        this.scoringSvc = scoringSvc;
    }
}
