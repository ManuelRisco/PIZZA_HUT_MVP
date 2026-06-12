package com.example.domain.repository;

import com.example.domain.model.OrderItemExtra;
import java.util.List;

public interface OrderItemExtraRepository {
    List<OrderItemExtra> findAll();
    List<OrderItemExtra> findByOrderItemId(Integer orderItemId);
    OrderItemExtra save(OrderItemExtra orderItemExtra);
    void deleteByOrderItemId(Integer orderItemId);
    void deleteById(Integer id);
}
