package com.example.domain.repository;

import com.example.domain.model.PizzaIngredient;
import com.example.domain.model.PizzaIngredientId;
import java.util.List;
import java.util.Optional;

public interface PizzaIngredientRepository {
    List<PizzaIngredient> findAll();
    Optional<PizzaIngredient> findById(PizzaIngredientId id);
    PizzaIngredient save(PizzaIngredient pizzaIngredient);
    boolean existsById(PizzaIngredientId id);
    void deleteById(PizzaIngredientId id);
    List<PizzaIngredient> findByPizzaId(Integer pizzaId);
    List<PizzaIngredient> findByIngredientId(Integer ingredientId);
}