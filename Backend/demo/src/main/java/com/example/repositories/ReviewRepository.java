package com.example.repositories;

import com.example.models.Review;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Integer> {
List<Review> findByOrderId(Integer orderId); // Cambiado de findByPizzaId a findByOrderId

    List<Review> findByUserId(Integer userId);
}


