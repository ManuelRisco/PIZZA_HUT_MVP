package com.example.domain.repository;

import com.example.domain.model.Review;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository {
    List<Review> findAll();
    Optional<Review> findById(Integer id);
    List<Review> findByOrderId(Integer orderId);  // Cambiado de findByPizzaId a findByOrderId
    List<Review> findByUserId(Integer userId);
    boolean existsById(Integer id);
    Review save(Review review);
    void deleteById(Integer id);
}
