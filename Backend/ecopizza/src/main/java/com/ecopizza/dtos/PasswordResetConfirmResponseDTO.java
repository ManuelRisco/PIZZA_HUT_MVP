package com.ecopizza.dtos;

public record PasswordResetConfirmResponseDTO(
        boolean reset,
        String email
) {}
