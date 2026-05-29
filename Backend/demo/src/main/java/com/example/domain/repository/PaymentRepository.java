package com.example.domain.repository;

import com.example.domain.model.Payment;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository {
    List<Payment> findAll();
    Optional<Payment> findById(Integer id);
    Optional<Payment> findByOrderId(Integer orderId);
    boolean existsById(Integer id);
    Payment save(Payment payment);
    void deleteById(Integer id);
}
