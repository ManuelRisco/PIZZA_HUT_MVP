package com.ecopizza.dtos;

public record GoogleAuthResponseDTO(
        String token,
        String refreshToken,
        UsuarioDTO usuario,
        boolean newlyCreated,
        boolean requiresPasswordSetup
) {}
