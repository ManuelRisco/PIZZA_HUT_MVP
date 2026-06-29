package com.example.repositories;

import com.example.models.Pizza;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PizzaRepository extends JpaRepository<Pizza, Integer> {

    @SuppressWarnings("null")
    @EntityGraph(attributePaths = { "category" })
    List<Pizza> findAll();

    @SuppressWarnings("null")
    @EntityGraph(attributePaths = { "category" })
    Optional<Pizza> findById(Integer id);

    boolean existsByName(String name);

    boolean existsByCategoryId(Integer categoryId);
}
