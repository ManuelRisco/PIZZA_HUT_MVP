package com.example.infrastructure.persistence.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.infrastructure.persistence.entities.OrderTrackingEntity;
import java.util.List;

public interface SpringDataOrderTrackingRepository extends JpaRepository<OrderTrackingEntity, Integer> {
    List<OrderTrackingEntity> findByOrderId(Integer orderId);
}
