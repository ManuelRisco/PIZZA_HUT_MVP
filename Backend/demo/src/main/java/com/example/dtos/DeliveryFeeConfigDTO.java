package com.example.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DeliveryFeeConfigDTO(
        BigDecimal deliveryFee,
        String currency,
        LocalDateTime updatedAt,
        @JsonInclude(JsonInclude.Include.NON_NULL) String message
) {
    public DeliveryFeeConfigDTO(BigDecimal deliveryFee, String currency, LocalDateTime updatedAt) {
        this(deliveryFee, currency, updatedAt, null);
    }

    public DeliveryFeeConfigDTO withMessage(String message) {
        return new DeliveryFeeConfigDTO(deliveryFee, currency, updatedAt, message);
    }
}
