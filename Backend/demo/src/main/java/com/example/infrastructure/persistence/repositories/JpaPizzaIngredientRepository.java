package com.example.infrastructure.persistence.repositories;

import com.example.domain.model.PizzaIngredient;
import com.example.domain.model.PizzaIngredientId;
import com.example.domain.repository.PizzaIngredientRepository;
import com.example.infrastructure.persistence.entities.PizzaIngredientEntity;
import com.example.infrastructure.persistence.entities.PizzaIngredientIdEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@SuppressWarnings("null")
public class JpaPizzaIngredientRepository implements PizzaIngredientRepository {

    private final SpringDataPizzaIngredientRepository springDataRepository;

    @Override
    public List<PizzaIngredient> findAll() {
        return springDataRepository.findAll()
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<PizzaIngredient> findById(PizzaIngredientId id) {
        PizzaIngredientIdEntity idEntity = new PizzaIngredientIdEntity(id.getPizzaId(), id.getIngredientId());
        return springDataRepository.findById(idEntity)
                .map(this::toDomain);
    }

    @Override
    public PizzaIngredient save(PizzaIngredient pizzaIngredient) {
        PizzaIngredientEntity entity = fromDomain(pizzaIngredient);
        return springDataRepository.save(entity).getId() != null ? toDomain(entity) : null;
    }

    @Override
    public void deleteById(PizzaIngredientId id) {
        PizzaIngredientIdEntity idEntity = new PizzaIngredientIdEntity(id.getPizzaId(), id.getIngredientId());
        springDataRepository.deleteById(idEntity);
    }

    @Override
    public boolean existsById(PizzaIngredientId id) {
        PizzaIngredientIdEntity idEntity = new PizzaIngredientIdEntity(id.getPizzaId(), id.getIngredientId());
        return springDataRepository.existsById(idEntity);
    }

    @Override
    public List<PizzaIngredient> findByPizzaId(Integer pizzaId) {
        return springDataRepository.findById_PizzaId(pizzaId)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<PizzaIngredient> findByIngredientId(Integer ingredientId) {
        return springDataRepository.findById_IngredientId(ingredientId)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // ==== Conversión entre dominio y entidad ====

    private PizzaIngredientEntity fromDomain(PizzaIngredient domain) {
        PizzaIngredientEntity entity = new PizzaIngredientEntity();
        entity.setId(new PizzaIngredientIdEntity(
                domain.getId().getPizzaId(),
                domain.getId().getIngredientId()
        ));
        entity.setIsDefault(domain.getIsDefault());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }

    private PizzaIngredient toDomain(PizzaIngredientEntity entity) {
        return new PizzaIngredient(
                new PizzaIngredientId(
                        entity.getId().getPizzaId(),
                        entity.getId().getIngredientId()
                ),
                null, // pizza (evitamos carga circular)
                null, // ingredient
                entity.getIsDefault(),
                entity.getCreatedAt()
        );
    }
}