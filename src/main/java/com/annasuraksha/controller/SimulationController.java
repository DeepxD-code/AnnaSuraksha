package com.annasuraksha.controller;

import com.annasuraksha.model.api.ApiResponse;
import com.annasuraksha.service.FraudMetricsService;
import com.annasuraksha.service.demo.SimulationService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/simulate")
public class SimulationController {

    private final SimulationService simulationSvc;
    private final FraudMetricsService metricsSvc;

    @GetMapping("/scenarios")
    public ApiResponse<List<Map<String, Object>>> listScenarios() {
        return ApiResponse.success(simulationSvc.listScenarios(), "Available simulation scenarios.");
    }

    @PostMapping("/fraud-case")
    public ApiResponse<SimulationService.SimulationResult> runScenario(@RequestBody Map<String, String> body) {
        String scenarioStr = body.get("scenario");
        if (scenarioStr == null || scenarioStr.isBlank())
            return ApiResponse.error("VALIDATION_ERROR", "scenario field is required. " +
                "Options: " + Arrays.toString(SimulationService.ScenarioType.values()));

        try {
            SimulationService.ScenarioType type =
                SimulationService.ScenarioType.valueOf(scenarioStr.toUpperCase());
            SimulationService.SimulationResult result = simulationSvc.runScenario(type);
            return ApiResponse.success(result, "Simulation scenario '" + scenarioStr + "' executed successfully. " +
                "Detection verified: " + result.detectionVerified());
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
        long passed = results.stream().filter(SimulationService.SimulationResult::detectionVerified).count();
        return ApiResponse.success(Map.of(
            "scenariosRun",     results.size(),
            "scenariosPassed",  passed,
            "scenariosFailed",  results.size() - passed,
            "detectionRate",    results.size() > 0 ? (double) passed / results.size() * 100 : 0,
            "results",          results
        ), passed + "/" + results.size() + " scenarios passed detection verification.");
    }

    @GetMapping("/accuracy")
    public ApiResponse<Map<String, Object>> getAccuracy() {
        Map<String, Object> acc = new LinkedHashMap<>();
        acc.put("overallAccuracy", simulationSvc.getAccuracy());
        acc.put("totalSimulationsRun", simulationSvc.getTotalRuns());
        acc.put("simulationsPassed", simulationSvc.getPassedRuns());
        acc.put("simulationsFailed", simulationSvc.getFailedRuns());
        acc.put("scenarioResults", simulationSvc.getScenarioResults());
        acc.put("modelVersion", "v5.1.0");
        acc.put("computedAt", new Date());
        return ApiResponse.success(acc, "Detection accuracy metrics retrieved.");
    }

    @DeleteMapping("/cleanup")
    public ApiResponse<Map<String, Object>> cleanup() {
        int deleted = simulationSvc.cleanup();
        return ApiResponse.success(Map.of("deletedRecords", deleted),
            deleted + " simulation records removed. Accuracy counters reset.");
    }
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SimulationController.class);
    public SimulationController(SimulationService simulationSvc, FraudMetricsService metricsSvc) {
        this.simulationSvc = simulationSvc;
        this.metricsSvc = metricsSvc;
    }
}
