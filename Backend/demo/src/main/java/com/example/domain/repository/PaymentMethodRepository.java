package com.example.domain.repository;

import com.example.domain.model.PaymentMethod;
import java.util.List;
import java.util.Optional;

public interface PaymentMethodRepository {
    List<PaymentMethod> findAll();
    Optional<PaymentMethod> findById(Integer id);
    List<PaymentMethod> findByActive(boolean isActive);
    PaymentMethod save(PaymentMethod paymentMethod);
    void deleteById(Integer id);
    boolean existsById(Integer id);
    boolean existsByName(String name);
    boolean existsByDisplayOrder(int displayOrder);
    boolean existsByDisplayOrderAndIdNot(int displayOrder, Integer id);
}
