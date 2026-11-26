package com.example.dtos;

import java.util.List;

public record MercadoPagoConfigDTO(
        boolean configured,
        String publicKey,
        boolean sandboxMode,
        String currency,
        String locale,
        List<String> supportedGateways
) {}
