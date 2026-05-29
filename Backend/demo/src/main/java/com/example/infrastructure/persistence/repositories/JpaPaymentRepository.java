package com.example.infrastructure.persistence.repositories;

import com.example.domain.model.Payment;
import com.example.domain.repository.PaymentRepository;
import com.example.infrastructure.persistence.entities.PaymentEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@SuppressWarnings("null")
public class JpaPaymentRepository implements PaymentRepository {

    private final SpringDataPaymentRepository springDataRepository;

    @Override
    public List<Payment> findAll() {
        return springDataRepository.findAll()
                .stream()
                .map(PaymentEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Payment> findById(Integer id) {
        return springDataRepository.findById(id)
                .map(PaymentEntity::toDomain);
    }

    @Override
    public Optional<Payment> findByOrderId(Integer orderId) {
        return springDataRepository.findByOrderId(orderId)
                .map(PaymentEntity::toDomain);
    }

    @Override
    public boolean existsById(Integer id) {
        return springDataRepository.existsById(id);
    }

    @Override
    public Payment save(Payment payment) {
        PaymentEntity entity = PaymentEntity.fromDomain(payment);
        PaymentEntity saved = springDataRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public void deleteById(Integer id) {
        springDataRepository.deleteById(id);
    }
}
