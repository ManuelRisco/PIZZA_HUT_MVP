package com.example.repositories;

import com.example.models.Category;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    boolean existsByName(String name);
    boolean existsByNameAndDeletedAtIsNull(String name);
    boolean existsByNameAndDeletedAtIsNullAndIdNot(String name, Integer id);
    Optional<Category> findByName(String name);
    Optional<Category> findByIdAndDeletedAtIsNull(Integer id);
    List<Category> findByDeletedAtIsNull();
}
