package com.example.dtos;

public record EmitirNotaCreditoRequestDTO(
        String motivo,
        Boolean enviarCorreo
) {}
