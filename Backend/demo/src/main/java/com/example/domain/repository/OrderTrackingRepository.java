package com.example.domain.repository;

import com.example.domain.model.OrderTracking;
import java.util.List;
import java.util.Optional;

public interface OrderTrackingRepository {
    List<OrderTracking> findAll();
    Optional<OrderTracking> findById(Integer id);
    List<OrderTracking> findByOrderId(Integer orderId);
    boolean existsById(Integer id);
    OrderTracking save(OrderTracking orderTracking);
    void deleteById(Integer id);
}
