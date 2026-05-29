package com.example.infrastructure.persistence.repositories;

import com.example.domain.model.Ingredient;
import com.example.domain.repository.IngredientRepository;
import com.example.infrastructure.persistence.entities.IngredientEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@SuppressWarnings("null")
public class JpaIngredientRepository implements IngredientRepository {

    private final SpringDataIngredientRepository springDataRepository;

    @Override
    public List<Ingredient> findAll() {
        return springDataRepository.findAll()
                .stream()
                .map(IngredientEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Ingredient> findById(Integer id) {
        return springDataRepository.findById(id)
                .map(IngredientEntity::toDomain);
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
    public Ingredient save(Ingredient ingredient) {
        IngredientEntity entity = IngredientEntity.fromDomain(ingredient);
        IngredientEntity saved = springDataRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public void deleteById(Integer id) {
        springDataRepository.deleteById(id);
    }
}
