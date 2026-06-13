package com.example.application.payments;

import com.example.infrastructure.izipay.IzipayClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PaymentApplicationService {

    private final IzipayClient izipayClient;

    public PaymentApplicationService(IzipayClient izipayClient) {
        this.izipayClient = izipayClient;
    }

    public String generatePaymentToken(double amount) {
        // En una app real, el orderId vendria de la base de datos tras crear la orden.
        // Aquí generamos uno aleatorio para demostración
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String customerEmail = "cliente@ejemplo.com"; // Simulado
        String currency = "PEN"; // Soles peruanos

        return izipayClient.createPaymentToken(amount, currency, orderId, customerEmail);
    }
}
