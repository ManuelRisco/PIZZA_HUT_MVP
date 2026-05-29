package com.example.service;

import com.example.domain.model.Review;
import com.example.domain.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    public List<Review> listarReviews() {
        return reviewRepository.findAll();
    }

    public Optional<Review> obtenerPorId(Integer id) {
        return reviewRepository.findById(id);
    }

    public List<Review> obtenerPorOrderId(Integer orderId) {
        return reviewRepository.findByOrderId(orderId);
    }

    public List<Review> obtenerPorUserId(Integer userId) {
        return reviewRepository.findByUserId(userId);
    }

    public Review crearReview(Review review) {
        return reviewRepository.save(review);
    }

    public Review actualizarReview(Integer id, Review review) {
        if (!reviewRepository.existsById(id)) {
            throw new IllegalArgumentException("Review no encontrado");
        }
        review.setId(id);
        return reviewRepository.save(review);
    }

    public void eliminarReview(Integer id) {
        if (!reviewRepository.existsById(id)) {
            throw new IllegalArgumentException("Review no encontrado");
        }
        reviewRepository.deleteById(id);
    }

    public Review desactivarReview(Integer id) {
        Optional<Review> reviewOpt = reviewRepository.findById(id);
        if (!reviewOpt.isPresent()) {
            throw new IllegalArgumentException("Review no encontrado");
        }
        Review review = reviewOpt.get();
        review.setActive(false);
        return reviewRepository.save(review);
    }

    public Review activarReview(Integer id) {
        Optional<Review> reviewOpt = reviewRepository.findById(id);
        if (!reviewOpt.isPresent()) {
            throw new IllegalArgumentException("Review no encontrado");
        }
        Review review = reviewOpt.get();
        review.setActive(true);
        return reviewRepository.save(review);
    }
}
