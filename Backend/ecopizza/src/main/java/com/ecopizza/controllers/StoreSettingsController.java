package com.ecopizza.controllers;

import com.ecopizza.dtos.DeliveryFeeConfigDTO;
import com.ecopizza.dtos.UpdateDeliveryFeeRequestDTO;
import com.ecopizza.services.StoreSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/settings/delivery-fee")
public class StoreSettingsController {

    private final StoreSettingsService storeSettingsService;

    public StoreSettingsController(StoreSettingsService storeSettingsService) {
        this.storeSettingsService = storeSettingsService;
    }

    @GetMapping
    public ResponseEntity<DeliveryFeeConfigDTO> getDeliveryFee() {
        return ResponseEntity.ok(storeSettingsService.getDeliveryFeeConfig());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeliveryFeeConfigDTO> updateDeliveryFee(@RequestBody UpdateDeliveryFeeRequestDTO request) {
        BigDecimal fee = request != null ? request.deliveryFee() : null;
        DeliveryFeeConfigDTO cfg = storeSettingsService.updateDeliveryFee(fee);
        return ResponseEntity.ok(cfg.withMessage("Costo de delivery actualizado correctamente"));
    }
}
