package com.example.infrastructure.persistence.repositories;

import com.example.infrastructure.persistence.entities.PaymentMethodEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataPaymentMethodRepository extends JpaRepository<PaymentMethodEntity, Integer> {
    List<PaymentMethodEntity> findByIsActive(boolean isActive);
    boolean existsByName(String name);
    boolean existsByDisplayOrder(int displayOrder);
    boolean existsByDisplayOrderAndIdNot(int displayOrder, Integer id);
}
