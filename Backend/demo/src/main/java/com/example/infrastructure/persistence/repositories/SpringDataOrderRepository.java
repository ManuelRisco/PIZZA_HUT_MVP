package com.example.infrastructure.persistence.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.infrastructure.persistence.entities.OrderEntity;
import com.example.domain.model.Order;
import java.util.List;

public interface SpringDataOrderRepository extends JpaRepository<OrderEntity, Integer> {
    List<OrderEntity> findByUserId(Integer userId);
    List<OrderEntity> findByStatus(Order.OrderStatus status);
    long countByUserId(Integer userId);
}
