package com.example.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ConfigurarDniRequestDTO(
        String dni,
        Boolean ageVerified,
        String name
) {}
