package com.example.dtos;

import com.example.models.Usuario;

public record GoogleAuthResultDTO(
        Usuario usuario,
        boolean newlyCreated
) {}
