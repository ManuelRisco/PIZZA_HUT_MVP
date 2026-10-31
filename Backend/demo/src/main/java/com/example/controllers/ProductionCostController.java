package com.example.controllers;

import com.example.dtos.ApiResponse;
import com.example.dtos.ProductionCatalogCostDTO;
import com.example.dtos.ProductionEstimateRequestDTO;
import com.example.dtos.ProductionEstimateResponseDTO;
import com.example.services.ProductionCostService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/production")
@CrossOrigin(origins = "http://localhost:4200")
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

