package com.example.infrastructure.controller;

import com.example.service.PaymentService;
import com.example.domain.dto.PaymentDTO;
import com.example.domain.model.Payment;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

import com.example.application.payments.PaymentApplicationService;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "http://localhost:4200")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentApplicationService paymentApplicationService;

    public PaymentController(PaymentService paymentService, PaymentApplicationService paymentApplicationService) {
        this.paymentService = paymentService;
        this.paymentApplicationService = paymentApplicationService;
    }

    @GetMapping
    public ResponseEntity<List<PaymentDTO>> listarPayments() {
        List<Payment> payments = paymentService.listarPayments();
        List<PaymentDTO> paymentsDTO = payments.stream()
            .map(PaymentDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(paymentsDTO);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPaymentPorId(@PathVariable("id") Integer id) {
        Optional<Payment> paymentOpt = paymentService.obtenerPorId(id);
        if (paymentOpt.isPresent()) {
            PaymentDTO paymentDTO = new PaymentDTO(paymentOpt.get());
            return ResponseEntity.ok(paymentDTO);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Payment no encontrado"));
        }
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<?> obtenerPaymentPorOrderId(@PathVariable("orderId") Integer orderId) {
        Optional<Payment> paymentOpt = paymentService.obtenerPorOrderId(orderId);
        if (paymentOpt.isPresent()) {
            PaymentDTO paymentDTO = new PaymentDTO(paymentOpt.get());
            return ResponseEntity.ok(paymentDTO);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Payment no encontrado para este pedido"));
        }
    }

    @PostMapping
    public ResponseEntity<?> crearPayment(@RequestBody PaymentDTO paymentDTO) {
        try {
            Payment payment = new Payment();
            payment.setOrderId(paymentDTO.getOrderId());
            payment.setAmount(paymentDTO.getAmount());
            payment.setPaymentMethodId(paymentDTO.getPaymentMethodId());
            payment.setStatus(Payment.PaymentStatus.valueOf(paymentDTO.getStatus()));
            payment.setTransactionId(paymentDTO.getTransactionId());
            
            Payment paymentCreado = paymentService.crearPayment(payment);
            return ResponseEntity.status(HttpStatus.CREATED).body(new PaymentDTO(paymentCreado));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarPayment(@PathVariable("id") Integer id, @RequestBody PaymentDTO paymentDTO) {
        try {
            Payment payment = new Payment();
            payment.setOrderId(paymentDTO.getOrderId());
            payment.setAmount(paymentDTO.getAmount());
            payment.setPaymentMethodId(paymentDTO.getPaymentMethodId());
            payment.setStatus(Payment.PaymentStatus.valueOf(paymentDTO.getStatus()));
            payment.setTransactionId(paymentDTO.getTransactionId());
            
            Payment paymentActualizado = paymentService.actualizarPayment(id, payment);
            return ResponseEntity.ok(new PaymentDTO(paymentActualizado));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarPayment(@PathVariable("id") Integer id) {
        try {
            paymentService.eliminarPayment(id);
            return ResponseEntity.ok(Map.of("message", "Payment eliminado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/create-token")
    public ResponseEntity<Map<String, String>> createToken(@RequestBody PaymentRequest request) {
        try {
            String token = paymentApplicationService.generatePaymentToken(request.getAmount());
            Map<String, String> response = new HashMap<>();
            response.put("formToken", token);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    public static class PaymentRequest {
        private double amount;

        public double getAmount() {
            return amount;
        }

        public void setAmount(double amount) {
            this.amount = amount;
        }
    }
}