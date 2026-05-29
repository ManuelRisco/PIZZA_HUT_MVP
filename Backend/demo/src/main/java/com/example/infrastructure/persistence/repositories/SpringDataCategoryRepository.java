package com.example.infrastructure.persistence.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.infrastructure.persistence.entities.CategoryEntity;

public interface SpringDataCategoryRepository extends JpaRepository<CategoryEntity, Integer> {
    boolean existsByName(String name);
}