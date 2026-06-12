package com.example.infrastructure.persistence.repositories;

import com.example.infrastructure.persistence.entities.OrderItemExtraEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SpringDataOrderItemExtraRepository extends JpaRepository<OrderItemExtraEntity, Integer> {
    
    List<OrderItemExtraEntity> findByOrderItemId(Integer orderItemId);
    
    void deleteByOrderItemId(Integer orderItemId);
}
