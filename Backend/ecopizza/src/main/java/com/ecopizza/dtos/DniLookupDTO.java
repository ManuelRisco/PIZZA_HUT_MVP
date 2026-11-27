package com.ecopizza.dtos;

public record DniLookupDTO(
        String dni,
        String nombres,
        String apellidoPaterno,
        String apellidoMaterno,
        String nombreCompleto,
        String source
) {}
