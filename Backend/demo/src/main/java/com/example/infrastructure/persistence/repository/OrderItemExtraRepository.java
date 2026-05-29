package com.example.infrastructure.persistence.repository;

import com.example.infrastructure.persistence.entities.OrderItemExtraEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderItemExtraRepository extends JpaRepository<OrderItemExtraEntity, Integer> {
    
    List<OrderItemExtraEntity> findByOrderItemId(Integer orderItemId);
    
    void deleteByOrderItemId(Integer orderItemId);
}
