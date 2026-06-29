package com.example.services;

import com.example.models.Payment;
import com.example.repositories.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public List<Payment> listarPayments() {
        return paymentRepository.findAll();
    }

    @SuppressWarnings("null")
    public Optional<Payment> obtenerPorId(Integer id) {
        return paymentRepository.findById(id);
    }

    public Optional<Payment> obtenerPorOrderId(Integer orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    @SuppressWarnings("null")
    public Payment crearPayment(Payment payment) {
        return paymentRepository.save(payment);
    }

    public Payment actualizarPayment(Integer id, Payment payment) {
        @SuppressWarnings("null")
        Payment existingPayment = paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payment no encontrado"));

        existingPayment.setStatus(payment.getStatus());
        existingPayment.setAmount(payment.getAmount());
        existingPayment.setTransactionId(payment.getTransactionId());
        existingPayment.setPaymentMethodId(payment.getPaymentMethodId());
        existingPayment.setUpdatedAt(java.time.LocalDateTime.now());

        return paymentRepository.save(existingPayment);
    }

    @SuppressWarnings("null")
    public void eliminarPayment(Integer id) {
        if (!paymentRepository.existsById(id)) {
            throw new IllegalArgumentException("Payment no encontrado");
        }
        paymentRepository.deleteById(id);
    }
}
