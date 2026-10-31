package com.example.repositories;

import com.example.models.Ingredient;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IngredientRepository extends JpaRepository<Ingredient, Integer> {
    boolean existsByName(String name);
    List<Ingredient> findByDeletedAtIsNull();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Ingredient i WHERE i.id = :id")
    Optional<Ingredient> findByIdWithLock(@Param("id") Integer id);
}
