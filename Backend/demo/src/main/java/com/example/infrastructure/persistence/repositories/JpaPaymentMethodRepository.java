package com.example.infrastructure.persistence.repositories;

import com.example.domain.model.PaymentMethod;
import com.example.domain.repository.PaymentMethodRepository;
import com.example.infrastructure.persistence.entities.PaymentMethodEntity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class JpaPaymentMethodRepository implements PaymentMethodRepository {

    @Autowired
    private SpringDataPaymentMethodRepository springDataRepository;

    @Override
    public List<PaymentMethod> findAll() {
        return springDataRepository.findAll().stream()
                .map(PaymentMethodEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @SuppressWarnings("null")
    public Optional<PaymentMethod> findById(Integer id) {
        return springDataRepository.findById(id)
                .map(PaymentMethodEntity::toDomain);
    }

    @Override
    public List<PaymentMethod> findByActive(boolean isActive) {
        return springDataRepository.findByIsActive(isActive).stream()
                .map(PaymentMethodEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @SuppressWarnings("null")
    public PaymentMethod save(PaymentMethod paymentMethod) {
        PaymentMethodEntity entity = PaymentMethodEntity.fromDomain(paymentMethod);
        PaymentMethodEntity savedEntity = springDataRepository.save(entity);
        return savedEntity.toDomain();
    }

    @Override
    @SuppressWarnings("null")
    public void deleteById(Integer id) {
        springDataRepository.deleteById(id);
    }

    @Override
    @SuppressWarnings("null")
    public boolean existsById(Integer id) {
        return springDataRepository.existsById(id);
    }

    @Override
    public boolean existsByName(String name) {
        return springDataRepository.existsByName(name);
    }

    @Override
    public boolean existsByDisplayOrder(int displayOrder) {
        return springDataRepository.existsByDisplayOrder(displayOrder);
    }

    @Override
    public boolean existsByDisplayOrderAndIdNot(int displayOrder, Integer id) {
        return springDataRepository.existsByDisplayOrderAndIdNot(displayOrder, id);
    }
}
