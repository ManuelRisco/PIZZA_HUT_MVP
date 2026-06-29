package com.example.repositories;

import com.example.models.OrderItemExtra;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemExtraRepository extends JpaRepository<OrderItemExtra, Integer> {
List<OrderItemExtra> findByOrderItemId(Integer orderItemId);
void deleteByOrderItemId(Integer orderItemId);
}


