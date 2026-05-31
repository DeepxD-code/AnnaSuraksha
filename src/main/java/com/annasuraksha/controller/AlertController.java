package com.annasuraksha.controller;

import com.annasuraksha.model.alert.AlertEvent;
import com.annasuraksha.model.api.ApiResponse;
import com.annasuraksha.service.alert.AlertService;
import com.annasuraksha.service.alert.SseAlertEmitter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService    alertService;
    private final SseAlertEmitter sseEmitter;

    /** SSE real-time stream — connect to receive alerts as they happen. */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @RequestParam(required = false) String stateCode,
            HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        return sseEmitter.subscribe(stateCode, userId != null ? userId : "anonymous");
    }

    @GetMapping("/active")
    public ApiResponse<Map<String, Object>> active(
            @RequestParam(required = false) String stateCode,
            @RequestParam(defaultValue = "50") int limit) {

        List<AlertEvent> alerts = alertService.getActiveAlerts(stateCode)
            .stream().limit(limit).toList();
        return ApiResponse.success(Map.of(
            "results",     alerts,
            "totalActive", alerts.size(),
            "sseClients",  sseEmitter.activeConnections()
        ), "Active alerts retrieved.");
    }

    @GetMapping("/history")
    public ApiResponse<Map<String, Object>> history() {
        List<AlertEvent> all = alertService.getAllAlerts();
        return ApiResponse.success(Map.of(
            "results", all,
            "total",   all.size()
        ), "Alert history retrieved.");
    }

    @PostMapping("/{alertId}/acknowledge")
    public ApiResponse<Map<String, Object>> acknowledge(
            @PathVariable String alertId,
            HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        boolean ok = alertService.acknowledgeAlert(alertId, userId != null ? userId : "system");
        if (!ok) return ApiResponse.error("NOT_FOUND", "Alert " + alertId + " not found.");
        return ApiResponse.success(Map.of("alertId", alertId, "acknowledgedBy", userId), "Alert acknowledged.");
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        List<AlertEvent> all = alertService.getAllAlerts();
        long critical = all.stream().filter(a -> "CRITICAL".equals(a.getSeverity())).count();
        long warning  = all.stream().filter(a -> "WARNING".equals(a.getSeverity())).count();
        long fraud    = all.stream().filter(a -> a.getAlertType() != null && a.getAlertType().startsWith("FRAUD")).count();
        long supply   = all.stream().filter(a -> a.getAlertType() != null && a.getAlertType().startsWith("SUPPLY")).count();
        return ApiResponse.success(Map.of(
            "totalActive",       all.size(),
            "criticalCount",     critical,
            "warningCount",      warning,
            "fraudAlerts",       fraud,
            "supplyAlerts",      supply,
            "sseConnections",    sseEmitter.activeConnections()
        ), "Alert statistics retrieved.");
    }
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AlertController.class);
    public AlertController(AlertService alertService, SseAlertEmitter sseEmitter) {
        this.alertService = alertService;
        this.sseEmitter = sseEmitter;
    }
}
