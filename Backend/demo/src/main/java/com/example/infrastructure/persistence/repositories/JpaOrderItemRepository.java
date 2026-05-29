package com.example.infrastructure.persistence.repositories;

import com.example.domain.model.OrderItem;
import com.example.domain.repository.OrderItemRepository;
import com.example.infrastructure.persistence.entities.OrderItemEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@SuppressWarnings("null")
public class JpaOrderItemRepository implements OrderItemRepository {

    private final SpringDataOrderItemRepository springDataRepository;

    @Override
    public List<OrderItem> findAll() {
        return springDataRepository.findAll()
                .stream()
                .map(OrderItemEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<OrderItem> findById(Integer id) {
        return springDataRepository.findById(id)
                .map(OrderItemEntity::toDomain);
    }

    @Override
    public List<OrderItem> findByOrderId(Integer orderId) {
        return springDataRepository.findByOrderId(orderId)
                .stream()
                .map(OrderItemEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsById(Integer id) {
        return springDataRepository.existsById(id);
    }

    @Override
    public OrderItem save(OrderItem orderItem) {
        OrderItemEntity entity = OrderItemEntity.fromDomain(orderItem);
        OrderItemEntity saved = springDataRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public void deleteById(Integer id) {
        springDataRepository.deleteById(id);
    }
}
