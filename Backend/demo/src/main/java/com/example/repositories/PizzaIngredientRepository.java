package com.example.repositories;

import com.example.models.PizzaIngredient;
import com.example.models.PizzaIngredientId;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PizzaIngredientRepository extends JpaRepository<PizzaIngredient, PizzaIngredientId> {
List<PizzaIngredient> findByPizzaId(Integer pizzaId);

    List<PizzaIngredient> findByIngredientId(Integer ingredientId);
}

