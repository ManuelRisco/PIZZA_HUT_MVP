package com.ecopizza.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EmitirBoletaRequestDTO(
        String dni,
        String nombre,
        Boolean enviarCorreo,
        Integer tipoComprobante,
        String ruc,
        String direccion,
        Boolean rucVerified
) {
    public EmitirBoletaRequestDTO(String dni, String nombre, Boolean enviarCorreo) {
        this(dni, nombre, enviarCorreo, null, null, null, null);
    }
}
