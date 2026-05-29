package com.example.infrastructure.persistence.repositories;

import com.example.domain.model.Pizza;
import com.example.domain.repository.PizzaRepository;
import com.example.infrastructure.persistence.entities.PizzaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@SuppressWarnings("null")
public class JpaPizzaRepository implements PizzaRepository {

    private final SpringDataPizzaRepository springDataRepository;

    @Override
    public List<Pizza> findAll() {
        return springDataRepository.findAll()
                .stream()
                .map(PizzaEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Pizza> findById(Integer id) {
        return springDataRepository.findById(id)
                .map(PizzaEntity::toDomain);
    }

    @Override
    public boolean existsById(Integer id) {
        return springDataRepository.existsById(id);
    }

    @Override
    public boolean existsByName(String name) {
        return springDataRepository.existsByName(name);
    }

    @Override
    public Pizza save(Pizza pizza) {
        PizzaEntity entity = PizzaEntity.fromDomain(pizza);
        PizzaEntity saved = springDataRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public void deleteById(Integer id) {
        springDataRepository.deleteById(id);
    }
}