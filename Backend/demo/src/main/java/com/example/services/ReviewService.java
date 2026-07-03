package com.example.services;

import com.example.models.Review;
import com.example.repositories.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;

    public ReviewService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

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
        Review existingReview = reviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Review no encontrado"));

        existingReview.setRating(review.getRating());
        existingReview.setComment(review.getComment());

        return reviewRepository.save(existingReview);
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
