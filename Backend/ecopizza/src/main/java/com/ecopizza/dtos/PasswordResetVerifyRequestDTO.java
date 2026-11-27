package com.ecopizza.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PasswordResetVerifyRequestDTO(
        String email,
        String code
) {}
