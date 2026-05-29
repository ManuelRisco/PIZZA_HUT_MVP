package com.example.infrastructure.persistence.repositories;

import com.example.domain.model.Review;
import com.example.domain.repository.ReviewRepository;
import com.example.infrastructure.persistence.entities.ReviewEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@SuppressWarnings("null")
public class JpaReviewRepository implements ReviewRepository {

    private final SpringDataReviewRepository springDataRepository;

    @Override
    public List<Review> findAll() {
        return springDataRepository.findAll()
                .stream()
                .map(ReviewEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Review> findById(Integer id) {
        return springDataRepository.findById(id)
                .map(ReviewEntity::toDomain);
    }

    @Override
    public List<Review> findByOrderId(Integer orderId) {
        return springDataRepository.findByOrderId(orderId)
                .stream()
                .map(ReviewEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Review> findByUserId(Integer userId) {
        return springDataRepository.findByUserId(userId)
                .stream()
                .map(ReviewEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsById(Integer id) {
        return springDataRepository.existsById(id);
    }

    @Override
    public Review save(Review review) {
        ReviewEntity entity = ReviewEntity.fromDomain(review);
        ReviewEntity saved = springDataRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public void deleteById(Integer id) {
        springDataRepository.deleteById(id);
    }
}
