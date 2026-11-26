package com.example.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PasswordResetConfirmRequestDTO(
        String email,
        String code,
        String newPassword
) {}
