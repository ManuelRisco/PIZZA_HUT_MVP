package com.example.repositories;

import com.example.models.PaymentMethod;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Integer> {
List<PaymentMethod> findByActive(boolean isActive);
boolean existsByName(String name);

    boolean existsByDisplayOrder(int displayOrder);

    boolean existsByDisplayOrderAndIdNot(int displayOrder, Integer id);
}


