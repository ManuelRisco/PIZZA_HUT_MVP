package com.ecopizza.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record NotaCreditoEmisionDTO(
        Integer orderId,
        String serie,
        Integer numero,
        String comprobante,
        String boletaModificadaComprobante,
        Integer tipoNota,
        String motivo,
        String clienteDni,
        String clienteNombre,
        String clienteDireccion,
        BigDecimal totalGravada,
        BigDecimal totalIgv,
        BigDecimal total,
        String pdfUrl,
        String xmlUrl,
        String cdrUrl,
        String enlace,
        String hash,
        String qr,
        String estado,
        LocalDateTime emitidaAt
) {}
