package com.example.controllers;

import com.example.dtos.ApiResponse;
import com.example.services.PaymentService;
import com.example.dtos.PaymentDTO;
import com.example.models.Payment;
import com.example.exceptions.BadRequestException;
import com.example.exceptions.ResourceNotFoundException;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "http://localhost:4200")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentDTO>>> getAllPayments() {
        List<Payment> payments = paymentService.listarPayments();
        List<PaymentDTO> paymentsDTO = payments.stream()
            .map(PaymentDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(paymentsDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentDTO>> getPaymentById(@PathVariable("id") Integer id) {
        Payment payment = paymentService.obtenerPorId(id)
            .orElseThrow(() -> new ResourceNotFoundException("Payment no encontrado"));
        return ResponseEntity.ok(ApiResponse.success(new PaymentDTO(payment)));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<PaymentDTO>> getPaymentByOrderId(@PathVariable("orderId") Integer orderId) {
        Payment payment = paymentService.obtenerPorOrderId(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Payment no encontrado para este pedido"));
        return ResponseEntity.ok(ApiResponse.success(new PaymentDTO(payment)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentDTO>> createPayment(@RequestBody PaymentDTO paymentDTO) {
        Payment payment = new Payment();
        payment.setOrderId(paymentDTO.getOrderId());
        payment.setAmount(paymentDTO.getAmount());
        payment.setPaymentMethodId(paymentDTO.getPaymentMethodId());
        
        try {
            payment.setStatus(Payment.PaymentStatus.valueOf(paymentDTO.getStatus()));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BadRequestException("Estado de pago inv\u00e1lido");
        }
        
        payment.setTransactionId(paymentDTO.getTransactionId());
        
        Payment paymentCreado = paymentService.crearPayment(payment);
        return new ResponseEntity<>(ApiResponse.success(new PaymentDTO(paymentCreado), "Payment creado con \u00e9xito"), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentDTO>> updatePayment(@PathVariable("id") Integer id, @RequestBody PaymentDTO paymentDTO) {
        Payment payment = new Payment();
        payment.setOrderId(paymentDTO.getOrderId());
        payment.setAmount(paymentDTO.getAmount());
        payment.setPaymentMethodId(paymentDTO.getPaymentMethodId());
        
        try {
            payment.setStatus(Payment.PaymentStatus.valueOf(paymentDTO.getStatus()));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BadRequestException("Estado de pago inv\u00e1lido");
        }
        
        payment.setTransactionId(paymentDTO.getTransactionId());
        
        Payment paymentActualizado = paymentService.actualizarPayment(id, payment);
        if (paymentActualizado == null) {
            throw new ResourceNotFoundException("Payment no encontrado");
        }
        return ResponseEntity.ok(ApiResponse.success(new PaymentDTO(paymentActualizado), "Payment actualizado con \u00e9xito"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePayment(@PathVariable("id") Integer id) {
        try {
            paymentService.eliminarPayment(id);
            return ResponseEntity.ok(ApiResponse.success(null, "Payment eliminado correctamente"));
        } catch (RuntimeException e) { throw e; } catch (Exception e) {
            throw new ResourceNotFoundException("Payment no encontrado");
        }
    }
}
