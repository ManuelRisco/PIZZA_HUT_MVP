package com.example.infrastructure.persistence.repositories;

import com.example.infrastructure.persistence.entities.PizzaIngredientEntity;
import com.example.infrastructure.persistence.entities.PizzaIngredientIdEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SpringDataPizzaIngredientRepository extends JpaRepository<PizzaIngredientEntity, PizzaIngredientIdEntity> {
    List<PizzaIngredientEntity> findById_PizzaId(Integer pizzaId);
    List<PizzaIngredientEntity> findById_IngredientId(Integer ingredientId);
}