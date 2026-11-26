package com.example.controllers;

import com.example.dtos.ApiResponse;
import com.example.dtos.MercadoPagoConfigDTO;
import com.example.services.MercadoPagoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments/mercadopago")
public class MercadoPagoController {

    private final MercadoPagoService mercadoPagoService;

    public MercadoPagoController(MercadoPagoService mercadoPagoService) {
        this.mercadoPagoService = mercadoPagoService;
    }

    /**
     * Expone únicamente la configuración pública de Mercado Pago (Public Key, moneda PEN, estado).
     * El Access Token nunca se expone al frontend.
     */
    @GetMapping("/config")
    public ResponseEntity<ApiResponse<MercadoPagoConfigDTO>> obtenerConfiguracion() {
        return ResponseEntity.ok(ApiResponse.success(
                mercadoPagoService.obtenerConfiguracionPublica(),
                "Configuración de pasarela Mercado Pago obtenida"
        ));
    }
}
