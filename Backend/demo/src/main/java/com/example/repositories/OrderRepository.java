package com.example.repositories;

import com.example.models.Order;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Integer> {
List<Order> findByUserId(Integer userId);

    List<Order> findByStatus(Order.OrderStatus status);

    long countByUserId(Integer userId);
    
    List<Order> findTop100ByOrderByCreatedAtDesc();
}


