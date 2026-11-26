package com.example.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EmitirBoletaRequestDTO(
        String dni,
        String nombre,
        Boolean enviarCorreo
) {}
