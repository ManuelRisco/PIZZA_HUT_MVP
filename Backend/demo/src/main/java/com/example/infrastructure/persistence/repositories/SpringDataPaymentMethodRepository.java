package com.example.infrastructure.persistence.repositories;

import com.example.infrastructure.persistence.entities.PaymentMethodEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataPaymentMethodRepository extends JpaRepository<PaymentMethodEntity, Integer> {
    List<PaymentMethodEntity> findByIsActive(boolean isActive);
    boolean existsByName(String name);
    boolean existsByDisplayOrder(int displayOrder);
    boolean existsByDisplayOrderAndIdNot(int displayOrder, Integer id);
}
