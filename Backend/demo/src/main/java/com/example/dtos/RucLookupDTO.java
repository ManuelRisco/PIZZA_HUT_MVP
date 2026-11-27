package com.example.dtos;

public record RucLookupDTO(
        String ruc,
        String razonSocial,
        String direccion,
        String estado,
        String condicion,
        String distrito,
        String provincia,
        String departamento,
        String source
) {}
