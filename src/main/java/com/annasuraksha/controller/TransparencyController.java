package com.annasuraksha.controller;

import com.annasuraksha.model.*;
import com.annasuraksha.model.api.ApiResponse;
import com.annasuraksha.service.BlockchainService;
import com.annasuraksha.service.FraudRiskScoringService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class TransparencyController {

    private final BeneficiaryRepository   beneRepo;
    private final FpsDeliveryRepository   fpsRepo;
    private final SupplyChainRepository   supplyRepo;
    private final BlockchainService       blockchainSvc;
    private final FraudRiskScoringService scoringSvc;

    @GetMapping("/api/stats")
    public ApiResponse<Map<String, Object>> stats() {
        long total   = beneRepo.count();
        long active  = beneRepo.countByStatus("ACTIVE");
        long ghost   = beneRepo.countByStatus("GHOST");
        long fps     = fpsRepo.findByFlaggedTrue().size();
        long supply  = supplyRepo.countByDiscrepancyFlaggedTrue();
        boolean chainValid = blockchainSvc.validateChain();

        return ApiResponse.success(Map.of(
            "totalBeneficiaries", total,
            "activeBeneficiaries",active,
            "ghostDetected",      ghost,
            "flaggedFpsShops",    fps,
            "supplyDiscrepancies",supply,
            "chainIntact",        chainValid,
            "generatedAt",        LocalDateTime.now(),
            "platform",           "AnnaSuraksha v5.0.0 — PDS Fraud Intelligence"
        ), "Platform statistics retrieved.");
    }

    @GetMapping("/api/transparency/state-summary")
    public ApiResponse<List<Map<String, Object>>> stateSummary() {
        List<Beneficiary> all = beneRepo.findAll();
        Map<String, List<Beneficiary>> byState = all.stream()
            .filter(b -> b.getStateCode() != null)
            .collect(Collectors.groupingBy(Beneficiary::getStateCode));

        List<Map<String, Object>> results = new ArrayList<>();
        byState.forEach((state, list) -> {
            long active = list.stream().filter(b -> "ACTIVE".equals(b.getStatus())).count();
            long ghost  = list.stream().filter(b -> "GHOST".equals(b.getStatus())).count();
            results.add(Map.<String, Object>of(
                "stateCode",   state,
                "total",       list.size(),
                "active",      active,
                "ghost",       ghost,
                "fraudRate",   list.isEmpty() ? 0.0 : Math.round((ghost * 1000.0) / list.size()) / 10.0
            ));
        });
        results.sort((a, b) -> Integer.compare((Integer) b.get("total"), (Integer) a.get("total")));
        return ApiResponse.success(results, "State-wise summary retrieved.");
    }

    @GetMapping("/api/transparency/public-ledger")
    public ApiResponse<Map<String, Object>> publicLedger(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        List<Beneficiary> all  = beneRepo.findAllByOrderByBlockHeightAsc();
        int total = all.size();
        int from  = Math.min(page * size, total);
        int to    = Math.min(from + size, total);

        List<Map<String, Object>> blocks = all.subList(from, to).stream().map(b -> Map.<String, Object>of(
            "blockHeight", b.getBlockHeight(),
            "shortHash",   blockchainSvc.shortHash(b.getBlockHash()),
            "state",       b.getStateCode(),
            "category",    b.getCategory(),
            "status",      b.getStatus(),
            "registeredAt",b.getRegisteredAt()
        )).toList();

        return ApiResponse.success(Map.<String, Object>of(
            "totalBlocks", total,
            "chainValid",  blockchainSvc.validateChain(),
            "page",        page,
            "blocks",      blocks
        ), "Public ledger retrieved.");
    }

    public TransparencyController(BeneficiaryRepository beneRepo, FpsDeliveryRepository fpsRepo, SupplyChainRepository supplyRepo, BlockchainService blockchainSvc, FraudRiskScoringService scoringSvc) {
        this.beneRepo = beneRepo;
        this.fpsRepo = fpsRepo;
        this.supplyRepo = supplyRepo;
        this.blockchainSvc = blockchainSvc;
        this.scoringSvc = scoringSvc;
    }
}
