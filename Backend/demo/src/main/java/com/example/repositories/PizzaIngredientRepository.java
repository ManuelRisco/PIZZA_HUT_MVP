package com.example.repositories;

import com.example.models.PizzaIngredient;
import com.example.models.PizzaIngredientId;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PizzaIngredientRepository extends JpaRepository<PizzaIngredient, PizzaIngredientId> {
    List<PizzaIngredient> findByPizzaId(Integer pizzaId);

    List<PizzaIngredient> findByIngredientId(Integer ingredientId);

    @Modifying
    @Query("DELETE FROM PizzaIngredient pi WHERE pi.id.pizzaId = :pizzaId")
    void deleteAllByPizzaId(@Param("pizzaId") Integer pizzaId);
}
