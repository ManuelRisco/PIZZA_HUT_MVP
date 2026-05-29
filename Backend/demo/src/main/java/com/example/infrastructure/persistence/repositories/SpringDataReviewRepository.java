package com.example.infrastructure.persistence.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.infrastructure.persistence.entities.ReviewEntity;
import java.util.List;

public interface SpringDataReviewRepository extends JpaRepository<ReviewEntity, Integer> {
    List<ReviewEntity> findByOrderId(Integer orderId);  // Cambiado de findByPizzaId a findByOrderId
    List<ReviewEntity> findByUserId(Integer userId);
}
