// 📁 com.example.repositories.CategoryRepository
package com.example.repositories;

import com.example.models.Category;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
boolean existsByName(String name);
}

