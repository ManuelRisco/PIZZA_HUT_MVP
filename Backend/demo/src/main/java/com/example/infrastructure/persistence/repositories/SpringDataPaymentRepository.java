package com.example.infrastructure.persistence.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.infrastructure.persistence.entities.PaymentEntity;
import java.util.Optional;

public interface SpringDataPaymentRepository extends JpaRepository<PaymentEntity, Integer> {
    Optional<PaymentEntity> findByOrderId(Integer orderId);
}
