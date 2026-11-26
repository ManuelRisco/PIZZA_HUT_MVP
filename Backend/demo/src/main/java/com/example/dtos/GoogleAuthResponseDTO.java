package com.example.dtos;

public record GoogleAuthResponseDTO(
        String token,
        String refreshToken,
        UsuarioDTO usuario,
        boolean newlyCreated,
        boolean requiresPasswordSetup
) {}
