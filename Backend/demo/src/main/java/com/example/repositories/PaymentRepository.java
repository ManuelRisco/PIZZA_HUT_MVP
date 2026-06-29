package com.example.repositories;

import com.example.models.Payment;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    Optional<Payment> findByOrderId(Integer orderId);
    boolean existsByPaymentMethodId(Integer paymentMethodId);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT p.paymentMethodId FROM Payment p WHERE p.paymentMethodId IS NOT NULL")
    java.util.Set<Integer> findPaymentMethodIdsInUse();
}


