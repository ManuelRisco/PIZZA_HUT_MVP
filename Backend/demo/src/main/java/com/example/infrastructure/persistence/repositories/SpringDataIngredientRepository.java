package com.example.infrastructure.persistence.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.infrastructure.persistence.entities.IngredientEntity;

public interface SpringDataIngredientRepository extends JpaRepository<IngredientEntity, Integer> {
    boolean existsByName(String name);
}