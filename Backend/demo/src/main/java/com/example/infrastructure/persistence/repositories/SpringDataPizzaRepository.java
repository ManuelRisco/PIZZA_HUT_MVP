package com.example.infrastructure.persistence.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.infrastructure.persistence.entities.PizzaEntity;

public interface SpringDataPizzaRepository extends JpaRepository<PizzaEntity, Integer> {
    boolean existsByName(String name);
}