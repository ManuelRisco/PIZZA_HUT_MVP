// 📁 com.example.domain.repository.CategoryRepository
package com.example.domain.repository;

import java.util.List;
import java.util.Optional;
import com.example.domain.model.Category;

public interface CategoryRepository {
    List<Category> findAll();
    Optional<Category> findById(Integer id);
    boolean existsById(Integer id);
    boolean existsByName(String name);
    Category save(Category category);
    void deleteById(Integer id);
}