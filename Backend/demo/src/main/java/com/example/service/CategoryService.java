package com.example.service;

import com.example.domain.repository.CategoryRepository;
import com.example.domain.model.Category;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    public List<Category> listarCategorias() {
        return categoryRepository.findAll();
    }

    public Optional<Category> obtenerPorId(Integer id) {
        return categoryRepository.findById(id);
    }

    public Category crearCategoria(Category category) {
        return categoryRepository.save(category);
    }

    public Category actualizarCategoria(Integer id, Category categoryActualizada) {
        Optional<Category> categoryOpt = categoryRepository.findById(id);
        if (categoryOpt.isPresent()) {
            Category category = categoryOpt.get();
            category.setName(categoryActualizada.getName());
            category.setDescription(categoryActualizada.getDescription());
            category.setImageUrl(categoryActualizada.getImageUrl());
            category.setDisplayOrder(categoryActualizada.getDisplayOrder());
            category.setUpdatedAt(java.time.LocalDateTime.now());
            category.setDeletedAt(categoryActualizada.getDeletedAt());
            return categoryRepository.save(category);
        }
        return null;
    }

    public boolean eliminarCategoria(Integer id) {
        if (categoryRepository.existsById(id)) {
            categoryRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public boolean existsByName(String name) {
        return categoryRepository.existsByName(name);
    }
}
