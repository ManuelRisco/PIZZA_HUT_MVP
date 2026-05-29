package com.example.infrastructure.persistence.repositories;

import com.example.domain.model.Order;
import com.example.domain.repository.OrderRepository;
import com.example.infrastructure.persistence.entities.OrderEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@SuppressWarnings("null")
public class JpaOrderRepository implements OrderRepository {

    private final SpringDataOrderRepository springDataRepository;

    @Override
    public List<Order> findAll() {
        return springDataRepository.findAll()
                .stream()
                .map(OrderEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Order> findById(Integer id) {
        return springDataRepository.findById(id)
                .map(OrderEntity::toDomain);
    }

    @Override
    public List<Order> findByUserId(Integer userId) {
        return springDataRepository.findByUserId(userId)
                .stream()
                .map(OrderEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Order> findByStatus(Order.OrderStatus status) {
        return springDataRepository.findByStatus(status)
                .stream()
                .map(OrderEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countByUserId(Integer userId) {
        return springDataRepository.countByUserId(userId);
    }

    @Override
    public boolean existsById(Integer id) {
        return springDataRepository.existsById(id);
    }

    @Override
    public Order save(Order order) {
        OrderEntity entity = OrderEntity.fromDomain(order);
        OrderEntity saved = springDataRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public void deleteById(Integer id) {
        springDataRepository.deleteById(id);
    }
}
