package com.annasuraksha.controller;

import com.annasuraksha.model.*;
import com.annasuraksha.model.api.ApiResponse;
import com.annasuraksha.service.DistributionService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/distribution")
public class DistributionController {

    private final DistributionService distributionService;

    @PostMapping
    public ApiResponse<Map<String, Object>> record(@RequestBody Map<String, Object> body) {
        try {
            Long beneficiaryId = Long.parseLong(body.get("beneficiary_id").toString());
            String fpsId       = body.get("fps_shop_id").toString();
            int rice           = Integer.parseInt(body.get("rice_kg").toString());
            int wheat          = Integer.parseInt(body.get("wheat_kg").toString());
            int sugar          = Integer.parseInt(body.get("sugar_kg").toString());

            DistributionEntry entry = distributionService.recordDistribution(beneficiaryId, fpsId, rice, wheat, sugar);

            Map<String, Object> res = new LinkedHashMap<>();
            res.put("distributionId", entry.getId());
            res.put("beneficiary",    entry.getBeneficiaryName());
            res.put("violation",      entry.getRuleViolation());
            if (entry.getRuleViolation()) {
                res.put("alert", "⚠️ ENTITLEMENT BREACH: " + entry.getViolationReason());
            }
            res.put("blockHash",      entry.getEntryHash());
            
            return ApiResponse.success(res, "Distribution recorded on blockchain.");
        } catch (Exception e) {
            return ApiResponse.error("ERROR", e.getMessage());
        }
    }

    @GetMapping
    public ApiResponse<List<DistributionEntry>> list() {
        return ApiResponse.success(distributionService.getAll(), "Distributions retrieved.");
    }

    public DistributionController(DistributionService distributionService) {
        this.distributionService = distributionService;
    }
}
