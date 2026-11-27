package com.ecopizza.repositories;

import com.ecopizza.models.OrderItem;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
List<OrderItem> findByOrderId(Integer orderId);
}


