package com.example.repositories;

import com.example.models.OrderTracking;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderTrackingRepository extends JpaRepository<OrderTracking, Integer> {
List<OrderTracking> findByOrderId(Integer orderId);
}


