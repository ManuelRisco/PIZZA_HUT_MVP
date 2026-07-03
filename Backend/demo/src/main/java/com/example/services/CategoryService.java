package com.example.services;

import com.example.repositories.CategoryRepository;
import com.example.models.Category;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final com.example.repositories.PizzaRepository pizzaRepository;

    public CategoryService(CategoryRepository categoryRepository,
            com.example.repositories.PizzaRepository pizzaRepository) {
        this.categoryRepository = categoryRepository;
        this.pizzaRepository = pizzaRepository;
    }

    @Cacheable("categories")
    public List<Category> listarCategorias() {
        return categoryRepository.findByDeletedAtIsNull();
    }

    @SuppressWarnings("null")
    public Optional<Category> obtenerPorId(Integer id) {
        return categoryRepository.findById(id);
    }

    @CacheEvict(value = "categories", allEntries = true)
    public Category crearCategoria(Category category) {
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Debe ingresar un nombre válido para la categoría.");
        }
        if (categoryRepository.existsByName(category.getName())) {
            throw new IllegalArgumentException("El nombre de la categoría ya está registrado.");
        }
        return categoryRepository.save(category);
    }

    @CacheEvict(value = "categories", allEntries = true)
    public Category actualizarCategoria(Integer id, Category categoryActualizada) {
        if (categoryActualizada.getName() == null || categoryActualizada.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Debe ingresar un nombre válido para la categoría.");
        }

        @SuppressWarnings("null")
        Optional<Category> categoryOpt = categoryRepository.findById(id);
        if (categoryOpt.isPresent()) {
            Category category = categoryOpt.get();
            // Validar duplicado si cambia el nombre
            if (!category.getName().equals(categoryActualizada.getName())
                    && categoryRepository.existsByName(categoryActualizada.getName())) {
                throw new IllegalArgumentException("El nombre de la categoría ya está registrado.");
            }
            category.setName(categoryActualizada.getName());
            category.setDescription(categoryActualizada.getDescription());
            category.setImageUrl(categoryActualizada.getImageUrl());
            category.setDisplayOrder(categoryActualizada.getDisplayOrder());
            category.setDeletedAt(categoryActualizada.getDeletedAt());
            return categoryRepository.save(category);
        }
        return null;
    }

    @CacheEvict(value = "categories", allEntries = true)
    public boolean eliminarCategoria(Integer id) {
        @SuppressWarnings("null")
        Optional<Category> categoryOpt = categoryRepository.findById(id);
        if (categoryOpt.isPresent()) {
            if (pizzaRepository.existsByCategoryId(id)) {
                throw new IllegalStateException("No se puede eliminar la categoría porque está en uso.");
            }
            Category category = categoryOpt.get();
            category.setDeletedAt(java.time.LocalDateTime.now());
            categoryRepository.save(category);
            return true;
        }
        return false;
    }

    public boolean existsByName(String name) {
        return categoryRepository.existsByName(name);
    }
}
