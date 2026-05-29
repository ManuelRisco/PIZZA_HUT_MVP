package com.example.domain.repository;

import java.util.List;
import java.util.Optional;
import com.example.domain.model.Pizza;

public interface PizzaRepository {
    List<Pizza> findAll();
    Optional<Pizza> findById(Integer id);
    boolean existsById(Integer id);
    boolean existsByName(String name);
    Pizza save(Pizza pizza);
    void deleteById(Integer id);
}