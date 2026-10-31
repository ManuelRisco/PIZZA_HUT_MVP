package com.example.repositories;

import com.example.models.PaymentMethod;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Integer> {
    List<PaymentMethod> findByActive(boolean isActive);
    List<PaymentMethod> findByDeletedAtIsNullOrderByDisplayOrderAsc();
    List<PaymentMethod> findByActiveAndDeletedAtIsNull(boolean isActive);
    List<PaymentMethod> findByActiveAndDeletedAtIsNullOrderByDisplayOrderAsc(boolean isActive);

    boolean existsByName(String name);
    boolean existsByNameAndDeletedAtIsNull(String name);
    boolean existsByNameAndDeletedAtIsNullAndIdNot(String name, Integer id);

    boolean existsByDisplayOrder(int displayOrder);
    boolean existsByDisplayOrderAndDeletedAtIsNull(int displayOrder);
    boolean existsByDisplayOrderAndDeletedAtIsNullAndIdNot(int displayOrder, Integer id);
    boolean existsByDisplayOrderAndIdNot(int displayOrder, Integer id);

    Optional<PaymentMethod> findByName(String name);
    Optional<PaymentMethod> findByNameAndDeletedAtIsNull(String name);
    Optional<PaymentMethod> findByIdAndDeletedAtIsNull(Integer id);
}
