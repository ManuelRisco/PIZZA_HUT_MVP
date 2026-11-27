package com.ecopizza.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ConfigurarRucRequestDTO(
        String ruc,
        Boolean rucVerified,
        String razonSocial,
        String direccionFiscal
) {}
