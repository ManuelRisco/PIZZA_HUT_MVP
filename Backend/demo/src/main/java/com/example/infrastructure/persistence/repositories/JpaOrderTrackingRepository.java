package com.example.infrastructure.persistence.repositories;

import com.example.domain.model.OrderTracking;
import com.example.domain.repository.OrderTrackingRepository;
import com.example.infrastructure.persistence.entities.OrderTrackingEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@SuppressWarnings("null")
public class JpaOrderTrackingRepository implements OrderTrackingRepository {

    private final SpringDataOrderTrackingRepository springDataRepository;

    @Override
    public List<OrderTracking> findAll() {
        return springDataRepository.findAll()
                .stream()
                .map(OrderTrackingEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<OrderTracking> findById(Integer id) {
        return springDataRepository.findById(id)
                .map(OrderTrackingEntity::toDomain);
    }

    @Override
    public List<OrderTracking> findByOrderId(Integer orderId) {
        return springDataRepository.findByOrderId(orderId)
                .stream()
                .map(OrderTrackingEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsById(Integer id) {
        return springDataRepository.existsById(id);
    }

    @Override
    public OrderTracking save(OrderTracking orderTracking) {
        OrderTrackingEntity entity = OrderTrackingEntity.fromDomain(orderTracking);
        OrderTrackingEntity saved = springDataRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public void deleteById(Integer id) {
        springDataRepository.deleteById(id);
    }
}
