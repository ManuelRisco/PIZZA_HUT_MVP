package com.example.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UpdateDeliveryFeeRequestDTO(
        BigDecimal deliveryFee
) {}
