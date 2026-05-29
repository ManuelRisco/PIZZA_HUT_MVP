package com.example.domain.repository;

import com.example.domain.model.Ingredient;
import java.util.List;
import java.util.Optional;

public interface IngredientRepository {
    List<Ingredient> findAll();
    Optional<Ingredient> findById(Integer id);
    boolean existsById(Integer id);
    boolean existsByName(String name);
    Ingredient save(Ingredient ingredient);
    void deleteById(Integer id);
}