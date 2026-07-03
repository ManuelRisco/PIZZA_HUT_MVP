package com.example.controllers;

import com.example.dtos.ApiResponse;
import com.example.services.PaymentService;
import com.example.services.OrderService;
import com.example.dtos.PaymentDTO;
import com.example.models.Payment;
import com.example.models.Order;
import com.example.security.SecurityUtils;
import com.example.exceptions.BadRequestException;
import com.example.exceptions.ResourceNotFoundException;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "http://localhost:4200")
public class PaymentController {

    private static final String MSG_NOT_FOUND = "Payment no encontrado";

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final SecurityUtils securityUtils;

    public PaymentController(PaymentService paymentService, OrderService orderService, SecurityUtils securityUtils) {
        this.paymentService = paymentService;
        this.orderService = orderService;
        this.securityUtils = securityUtils;
    }

    private void validarAccesoAPago(Payment payment) {
        if (!securityUtils.isAdmin()) {
            Integer currentUserId = securityUtils.getCurrentUserId();
            if (currentUserId == null) {
                throw new org.springframework.security.access.AccessDeniedException("No autenticado");
            }

            Order order = orderService.obtenerPorId(payment.getOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));

            if (!order.getUserId().equals(currentUserId)) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "No tienes permiso para ver este pago");
            }
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentDTO>>> getAllPayments() {
        if (!securityUtils.isAdmin()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Solo los administradores pueden listar todos los pagos");
        }

        List<Payment> payments = paymentService.listarPayments();
        List<PaymentDTO> paymentsDTO = payments.stream()
                .map(PaymentDTO::new)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(paymentsDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentDTO>> getPaymentById(@PathVariable("id") Integer id) {
        Payment payment = paymentService.obtenerPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_NOT_FOUND));

        validarAccesoAPago(payment);

        return ResponseEntity.ok(ApiResponse.success(new PaymentDTO(payment)));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<PaymentDTO>> getPaymentByOrderId(@PathVariable("orderId") Integer orderId) {
        Payment payment = paymentService.obtenerPorOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment no encontrado para este pedido"));

        validarAccesoAPago(payment);

        return ResponseEntity.ok(ApiResponse.success(new PaymentDTO(payment)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentDTO>> createPayment(@Valid @RequestBody PaymentDTO paymentDTO) {
        Payment payment = new Payment();
        payment.setOrderId(paymentDTO.getOrderId());
        payment.setAmount(paymentDTO.getAmount());
        payment.setPaymentMethodId(paymentDTO.getPaymentMethodId());

        try {
            payment.setStatus(Payment.PaymentStatus.valueOf(paymentDTO.getStatus()));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BadRequestException("Estado de pago invÃ¡lido");
        }

        payment.setTransactionId(paymentDTO.getTransactionId());

        // El controller de admin puede crear pagos, pero si es CUSTOMER, validamos IDOR
        validarAccesoAPago(payment);

        Payment paymentCreado = paymentService.crearPayment(payment);
        return new ResponseEntity<>(ApiResponse.success(new PaymentDTO(paymentCreado), "Payment creado con Ã©xito"),
                HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentDTO>> updatePayment(@PathVariable("id") Integer id,
            @Valid @RequestBody PaymentDTO paymentDTO) {
        Payment payment = new Payment();
        payment.setOrderId(paymentDTO.getOrderId());
        payment.setAmount(paymentDTO.getAmount());
        payment.setPaymentMethodId(paymentDTO.getPaymentMethodId());

        try {
            payment.setStatus(Payment.PaymentStatus.valueOf(paymentDTO.getStatus()));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BadRequestException("Estado de pago invÃ¡lido");
        }

        payment.setTransactionId(paymentDTO.getTransactionId());

        Payment paymentActualizado = paymentService.actualizarPayment(id, payment);
        if (paymentActualizado == null) {
            throw new ResourceNotFoundException(MSG_NOT_FOUND);
        }
        return ResponseEntity
                .ok(ApiResponse.success(new PaymentDTO(paymentActualizado), "Payment actualizado con Ã©xito"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePayment(@PathVariable("id") Integer id) {
        try {
            paymentService.eliminarPayment(id);
            return ResponseEntity.ok(ApiResponse.success(null, "Payment eliminado correctamente"));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new ResourceNotFoundException(MSG_NOT_FOUND);
        }
    }
}

