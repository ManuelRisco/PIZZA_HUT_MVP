package com.example.controllers;

import com.example.dtos.ApiResponse;
import com.example.dtos.InventorySummaryDTO;
import com.example.dtos.StockAlertDTO;
import com.example.dtos.StockMovementDTO;
import com.example.dtos.StockOperationRequestDTO;
import com.example.security.SecurityUtils;
import com.example.services.InventoryService;
import com.example.services.StockAlertNotificationService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@PreAuthorize("hasRole('ADMIN')")
public class InventoryController {

    private final InventoryService inventoryService;
    private final SecurityUtils securityUtils;
    private final StockAlertNotificationService stockAlertNotificationService;

    public InventoryController(InventoryService inventoryService,
                               SecurityUtils securityUtils,
                               StockAlertNotificationService stockAlertNotificationService) {
        this.inventoryService = inventoryService;
        this.securityUtils = securityUtils;
        this.stockAlertNotificationService = stockAlertNotificationService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<InventorySummaryDTO>> getSummary() {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getSummary()));
    }

    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<List<InventorySummaryDTO.AlertItemDTO>>> getAlerts() {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getAlerts()));
    }

    @GetMapping("/movements")
    public ResponseEntity<ApiResponse<List<StockMovementDTO>>> getMovements(
            @RequestParam(value = "itemType", required = false) String itemType,
            @RequestParam(value = "movementType", required = false) String movementType,
            @RequestParam(value = "limit", required = false) Integer limit) {
        return ResponseEntity.ok(ApiResponse.success(
                inventoryService.getMovements(itemType, movementType, limit)));
    }

    @PostMapping("/restock")
    public ResponseEntity<ApiResponse<StockMovementDTO>> restock(@RequestBody StockOperationRequestDTO req) {
        Integer userId = securityUtils.getCurrentUserId();
        StockMovementDTO result = inventoryService.restock(req, userId);
        return ResponseEntity.ok(ApiResponse.success(result, "Stock reabastecido correctamente"));
    }

    @PostMapping("/adjust")
    public ResponseEntity<ApiResponse<StockMovementDTO>> adjustStock(@RequestBody StockOperationRequestDTO req) {
        Integer userId = securityUtils.getCurrentUserId();
        StockMovementDTO result = inventoryService.adjustStock(req, userId);
        return ResponseEntity.ok(ApiResponse.success(result, "Ajuste de inventario aplicado correctamente"));
    }

    @GetMapping("/trigger-alerts")
    public ResponseEntity<ApiResponse<List<StockAlertDTO>>> getTriggerAlerts(
            @RequestParam(value = "limit", required = false) Integer limit) {
        List<StockAlertDTO> alerts = stockAlertNotificationService != null
                ? stockAlertNotificationService.getRecentAlerts(limit)
                : List.of();
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    @GetMapping("/trigger-alerts/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTriggerAlertsBatchStatus() {
        Map<String, Object> status = stockAlertNotificationService != null
                ? stockAlertNotificationService.getBatchNotificationStatus()
                : Map.of("mailOperational", false, "pendingBatchCount", 0, "secondsRemaining", 0, "notifiedActiveCount", 0, "activeAlerts", List.of());
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    @GetMapping("/trigger-events")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTriggerEvents(
            @RequestParam(value = "limit", required = false) Integer limit) {
        List<Map<String, Object>> events = stockAlertNotificationService != null
                ? stockAlertNotificationService.getRecentTriggerEvents(limit)
                : List.of();
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    @PostMapping("/trigger-alerts/dispatch")
    public ResponseEntity<ApiResponse<Map<String, Object>>> dispatchPendingTriggerAlerts() {
        int dispatched = stockAlertNotificationService != null
                ? stockAlertNotificationService.processPendingAlerts(true)
                : 0;
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("dispatchedCount", dispatched),
                dispatched > 0
                        ? "Correo consolidado enviado exitosamente (" + dispatched + " producto(s) en alerta)"
                        : "No hay alertas pendientes de env\u00edo por correo"));
    }
}
