package com.example.infrastructure.persistence.repositories;

import com.example.domain.model.OrderItemExtra;
import com.example.domain.repository.OrderItemExtraRepository;
import com.example.infrastructure.persistence.entities.OrderItemExtraEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class JpaOrderItemExtraRepository implements OrderItemExtraRepository {

    private final SpringDataOrderItemExtraRepository springDataRepository;

    @Override
    public List<OrderItemExtra> findAll() {
        return springDataRepository.findAll()
                .stream()
                .map(OrderItemExtraEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderItemExtra> findByOrderItemId(Integer orderItemId) {
        return springDataRepository.findByOrderItemId(orderItemId)
                .stream()
                .map(OrderItemExtraEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public OrderItemExtra save(OrderItemExtra orderItemExtra) {
        OrderItemExtraEntity entity = OrderItemExtraEntity.fromDomain(orderItemExtra);
        OrderItemExtraEntity savedEntity = springDataRepository.save(java.util.Objects.requireNonNull(entity));
        return savedEntity.toDomain();
    }

    @Override
    public void deleteByOrderItemId(Integer orderItemId) {
        springDataRepository.deleteByOrderItemId(orderItemId);
    }

    @Override
    public void deleteById(Integer id) {
        springDataRepository.deleteById(java.util.Objects.requireNonNull(id));
    }
}

