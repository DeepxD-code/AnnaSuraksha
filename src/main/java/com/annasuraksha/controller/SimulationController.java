package com.annasuraksha.controller;

import com.annasuraksha.model.api.ApiResponse;
import com.annasuraksha.service.demo.SimulationService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/simulate")
public class SimulationController {

    private final SimulationService simulationSvc;

    @GetMapping("/scenarios")
    public ApiResponse<List<Map<String, Object>>> listScenarios() {
        return ApiResponse.success(simulationSvc.listScenarios(), "Available simulation scenarios.");
    }

    @PostMapping("/fraud-case")
    public ApiResponse<SimulationService.SimulationResult> runScenario(@RequestBody Map<String, String> body) {
        String scenarioStr = body.get("scenario");
        if (scenarioStr == null || scenarioStr.isBlank())
            return ApiResponse.error("VALIDATION_ERROR", "scenario field is required. " +
                "Options: GHOST_BENEFICIARY, IMPOSSIBLE_TRAVEL, DEALER_DIVERSION, CATEGORY_FRAUD, " +
                "BULK_CLAIM_BURST, SUPPLY_CHAIN_LEAK, CROSS_STATE_NON_ONORC, BIOMETRIC_DUPLICATE");

        try {
            SimulationService.ScenarioType type =
                SimulationService.ScenarioType.valueOf(scenarioStr.toUpperCase());
            SimulationService.SimulationResult result = simulationSvc.runScenario(type);
            return ApiResponse.success(result, "Simulation scenario '" + scenarioStr + "' executed successfully.");
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("INVALID_SCENARIO",
                "Unknown scenario: " + scenarioStr + ". Valid options: " +
                Arrays.toString(SimulationService.ScenarioType.values()));
        } catch (Exception e) {
            log.error("Simulation failed for {}: {}", scenarioStr, e.getMessage(), e);
            return ApiResponse.error("SIMULATION_ERROR", e.getMessage());
        }
    }

    @PostMapping("/fraud-case/all")
    public ApiResponse<Map<String, Object>> runAllScenarios() {
        List<SimulationService.SimulationResult> results = simulationSvc.runAllScenarios();
        return ApiResponse.success(Map.of(
            "scenariosRun",     results.size(),
            "results",          results
        ), "All " + results.size() + " simulation scenarios executed.");
    }

    @DeleteMapping("/cleanup")
    public ApiResponse<Map<String, Object>> cleanup() {
        int deleted = simulationSvc.cleanup();
        return ApiResponse.success(Map.of("deletedRecords", deleted),
            deleted + " simulation records removed.");
    }
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SimulationController.class);
    public SimulationController(SimulationService simulationSvc) {
        this.simulationSvc = simulationSvc;
    }
}
