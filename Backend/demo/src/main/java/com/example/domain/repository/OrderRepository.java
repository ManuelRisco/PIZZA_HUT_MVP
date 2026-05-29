package com.example.domain.repository;

import com.example.domain.model.Order;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    List<Order> findAll();
    Optional<Order> findById(Integer id);
    List<Order> findByUserId(Integer userId);
    List<Order> findByStatus(Order.OrderStatus status);
    long countByUserId(Integer userId);
    boolean existsById(Integer id);
    Order save(Order order);
    void deleteById(Integer id);
}
