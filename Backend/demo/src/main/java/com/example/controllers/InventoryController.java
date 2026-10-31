package com.example.controllers;

import com.example.dtos.ApiResponse;
import com.example.dtos.InventorySummaryDTO;
import com.example.dtos.StockMovementDTO;
import com.example.dtos.StockOperationRequestDTO;
import com.example.security.SecurityUtils;
import com.example.services.InventoryService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "http://localhost:4200")
@PreAuthorize("hasRole('ADMIN')")
public class InventoryController {

    private final InventoryService inventoryService;
    private final SecurityUtils securityUtils;

    public InventoryController(InventoryService inventoryService, SecurityUtils securityUtils) {
        this.inventoryService = inventoryService;
        this.securityUtils = securityUtils;
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
}

