package com.example.controllers;

import com.example.services.StoreSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/settings/delivery-fee")
public class StoreSettingsController {

    private final StoreSettingsService storeSettingsService;

    public StoreSettingsController(StoreSettingsService storeSettingsService) {
        this.storeSettingsService = storeSettingsService;
    }

    public record UpdateDeliveryFeeRequest(BigDecimal deliveryFee) {}

    @GetMapping
    public ResponseEntity<Map<String, Object>> getDeliveryFee() {
        StoreSettingsService.DeliveryFeeConfig cfg = storeSettingsService.getDeliveryFeeConfig();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("deliveryFee", cfg.deliveryFee());
        body.put("currency", cfg.currency());
        body.put("updatedAt", cfg.updatedAt());
        return ResponseEntity.ok(body);
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateDeliveryFee(@RequestBody UpdateDeliveryFeeRequest request) {
        BigDecimal fee = request != null ? request.deliveryFee() : null;
        StoreSettingsService.DeliveryFeeConfig cfg = storeSettingsService.updateDeliveryFee(fee);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("deliveryFee", cfg.deliveryFee());
        body.put("currency", cfg.currency());
        body.put("updatedAt", cfg.updatedAt());
        body.put("message", "Costo de delivery actualizado correctamente");
        return ResponseEntity.ok(body);
    }
}
