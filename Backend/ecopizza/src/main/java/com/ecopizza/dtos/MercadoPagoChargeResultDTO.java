package com.ecopizza.dtos;

public record MercadoPagoChargeResultDTO(
        boolean processedViaMercadoPago,
        String paymentId,
        String status,
        String statusDetail,
        String paymentMethodId,
        String transactionId,
        String message
) {}
