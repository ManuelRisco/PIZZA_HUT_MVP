package com.example.domain.repository;

import com.example.domain.model.OrderItem;
import java.util.List;
import java.util.Optional;

public interface OrderItemRepository {
    List<OrderItem> findAll();
    Optional<OrderItem> findById(Integer id);
    List<OrderItem> findByOrderId(Integer orderId);
    boolean existsById(Integer id);
    OrderItem save(OrderItem orderItem);
    void deleteById(Integer id);
}
