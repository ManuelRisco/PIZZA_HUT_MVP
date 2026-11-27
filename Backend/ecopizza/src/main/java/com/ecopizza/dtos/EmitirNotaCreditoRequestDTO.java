package com.ecopizza.dtos;

public record EmitirNotaCreditoRequestDTO(
        String motivo,
        Boolean enviarCorreo
) {}
