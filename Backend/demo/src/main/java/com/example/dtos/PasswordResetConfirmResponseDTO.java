package com.example.dtos;

public record PasswordResetConfirmResponseDTO(
        boolean reset,
        String email
) {}
