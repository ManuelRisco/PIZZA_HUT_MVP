package com.ecopizza.controllers;

import com.ecopizza.dtos.ApiResponse;
import com.ecopizza.dtos.ProductionCatalogCostDTO;
import com.ecopizza.dtos.ProductionEstimateRequestDTO;
import com.ecopizza.dtos.ProductionEstimateResponseDTO;
import com.ecopizza.services.ProductionCostService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/production")
@PreAuthorize("hasRole('ADMIN')")
public class ProductionCostController {

    private final ProductionCostService productionCostService;

    public ProductionCostController(ProductionCostService productionCostService) {
        this.productionCostService = productionCostService;
    }

    @GetMapping("/catalog-costs")
    public ResponseEntity<ApiResponse<ProductionCatalogCostDTO>> getCatalogCosts() {
        return ResponseEntity.ok(ApiResponse.success(productionCostService.getCatalogCosts()));
    }

    @PostMapping("/estimate")
    public ResponseEntity<ApiResponse<ProductionEstimateResponseDTO>> estimateBatch(
            @RequestBody ProductionEstimateRequestDTO req) {
        return ResponseEntity.ok(ApiResponse.success(productionCostService.estimateBatch(req)));
    }
}

