package com.example.service;

import com.example.domain.model.Payment;
import com.example.domain.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public List<Payment> listarPayments() {
        return paymentRepository.findAll();
    }

    public Optional<Payment> obtenerPorId(Integer id) {
        return paymentRepository.findById(id);
    }

    public Optional<Payment> obtenerPorOrderId(Integer orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    public Payment crearPayment(Payment payment) {
        return paymentRepository.save(payment);
    }

    public Payment actualizarPayment(Integer id, Payment payment) {
        if (!paymentRepository.existsById(id)) {
            throw new IllegalArgumentException("Payment no encontrado");
        }
        payment.setId(id);
        return paymentRepository.save(payment);
    }

    public void eliminarPayment(Integer id) {
        if (!paymentRepository.existsById(id)) {
            throw new IllegalArgumentException("Payment no encontrado");
        }
        paymentRepository.deleteById(id);
    }
}
