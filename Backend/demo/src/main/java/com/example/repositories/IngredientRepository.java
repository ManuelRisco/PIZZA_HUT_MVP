package com.example.repositories;

import com.example.models.Ingredient;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientRepository extends JpaRepository<Ingredient, Integer> {
boolean existsByName(String name);
}

