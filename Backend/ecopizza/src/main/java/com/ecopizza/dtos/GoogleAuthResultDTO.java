package com.ecopizza.dtos;

import com.ecopizza.models.Usuario;

public record GoogleAuthResultDTO(
        Usuario usuario,
        boolean newlyCreated
) {}
