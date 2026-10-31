package com.example.controllers;

import com.example.dtos.ApiResponse;
import com.example.dtos.ProductLossCreateDTO;
import com.example.dtos.ProductLossDTO;
import com.example.security.SecurityUtils;
import com.example.services.ProductLossService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/losses")
@CrossOrigin(origins = "http://localhost:4200")
@PreAuthorize("hasRole('ADMIN')")
public class ProductLossController {

    private final ProductLossService productLossService;
    private final SecurityUtils securityUtils;

    public ProductLossController(ProductLossService productLossService, SecurityUtils securityUtils) {
        this.productLossService = productLossService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductLossDTO>>> listLosses(
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "itemType", required = false) String itemType) {
        return ResponseEntity.ok(ApiResponse.success(
                productLossService.listLosses(startDate, endDate, category, itemType)));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSummary(
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.success(
                productLossService.getLossesSummary(startDate, endDate)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductLossDTO>> createLoss(@RequestBody ProductLossCreateDTO req) {
        Integer userId = securityUtils.getCurrentUserId();
        ProductLossDTO created = productLossService.createLoss(req, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Pérdida de producto registrada correctamente"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductLossDTO>> updateLoss(
            @PathVariable("id") Integer id,
            @RequestBody ProductLossCreateDTO req) {
        ProductLossDTO updated = productLossService.updateLoss(id, req);
        return ResponseEntity.ok(ApiResponse.success(updated, "Pérdida actualizada correctamente"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteLoss(
            @PathVariable("id") Integer id,
            @RequestParam(value = "restoreStock", defaultValue = "true") boolean restoreStock) {
        Integer userId = securityUtils.getCurrentUserId();
        productLossService.deleteLoss(id, restoreStock, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Registro de pérdida eliminado correctamente"));
    }
}

