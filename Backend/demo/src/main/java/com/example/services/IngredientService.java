package com.example.services;

import com.example.models.Ingredient;
import com.example.repositories.IngredientRepository;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class IngredientService {

    private final IngredientRepository ingredientRepository;

    public IngredientService(IngredientRepository ingredientRepository) {
        this.ingredientRepository = ingredientRepository;
    }

    @Cacheable("ingredients")
    public List<Ingredient> listarIngredientes() {
        return ingredientRepository.findByDeletedAtIsNull();
    }

    public Optional<Ingredient> obtenerPorId(Integer id) {
        return ingredientRepository.findById(id);
    }

    @CacheEvict(value = { "ingredients", "pizzas" }, allEntries = true)
    public Ingredient crearIngrediente(Ingredient ingredient) {
        if (ingredientRepository.existsByName(ingredient.getName())) {
            throw new IllegalArgumentException("El nombre del ingrediente ya está registrado.");
        }
        if (ingredient.getUnit() == null || ingredient.getUnit().isBlank()) {
            ingredient.setUnit("g");
        }
        if (ingredient.getStockQuantity() == null) {
            ingredient.setStockQuantity(BigDecimal.ZERO);
        }
        if (ingredient.getMinStock() == null) {
            ingredient.setMinStock(BigDecimal.ZERO);
        }
        if (ingredient.getUnitCost() == null) {
            ingredient.setUnitCost(BigDecimal.ZERO);
        }
        if (ingredient.getExtraPortionQuantity() == null) {
            ingredient.setExtraPortionQuantity(new BigDecimal("50.00"));
        }
        if (ingredient.getExtraCost() == null) {
            ingredient.setExtraCost(BigDecimal.ZERO);
        }
        return ingredientRepository.save(ingredient);
    }

    @CacheEvict(value = { "ingredients", "pizzas" }, allEntries = true)
    public Ingredient actualizarIngrediente(Integer id, Ingredient ingredientActualizado) {
        Optional<Ingredient> ingredientOpt = ingredientRepository.findById(id);
        if (ingredientOpt.isPresent()) {
            Ingredient ingredient = ingredientOpt.get();

            if (ingredientActualizado.getName() != null
                    && !ingredient.getName().equals(ingredientActualizado.getName())
                    && ingredientRepository.existsByName(ingredientActualizado.getName())) {
                throw new IllegalArgumentException("El nombre del ingrediente ya está registrado.");
            }

            if (ingredientActualizado.getName() != null) {
                ingredient.setName(ingredientActualizado.getName());
            }
            if (ingredientActualizado.getExtraCost() != null) {
                ingredient.setExtraCost(ingredientActualizado.getExtraCost());
            }
            ingredient.setAvailable(ingredientActualizado.isAvailable());
            if (ingredientActualizado.getUnit() != null && !ingredientActualizado.getUnit().isBlank()) {
                ingredient.setUnit(ingredientActualizado.getUnit());
            }
            if (ingredientActualizado.getStockQuantity() != null) {
                ingredient.setStockQuantity(ingredientActualizado.getStockQuantity());
            }
            if (ingredientActualizado.getMinStock() != null) {
                ingredient.setMinStock(ingredientActualizado.getMinStock());
            }
            if (ingredientActualizado.getUnitCost() != null) {
                ingredient.setUnitCost(ingredientActualizado.getUnitCost());
            }
            if (ingredientActualizado.getExtraPortionQuantity() != null) {
                ingredient.setExtraPortionQuantity(ingredientActualizado.getExtraPortionQuantity());
            }
            ingredient.setAllowAsExtra(ingredientActualizado.isAllowAsExtra());
            ingredient.setDeletedAt(ingredientActualizado.getDeletedAt());
            return ingredientRepository.save(ingredient);
        }
        return null;
    }

    @CacheEvict(value = { "ingredients", "pizzas" }, allEntries = true)
    public boolean eliminarIngrediente(Integer id) {
        Optional<Ingredient> ingredientOpt = ingredientRepository.findById(id);
        if (ingredientOpt.isPresent()) {
            Ingredient ingredient = ingredientOpt.get();
            ingredient.setDeletedAt(java.time.LocalDateTime.now());
            ingredientRepository.save(ingredient);
            return true;
        }
        return false;
    }

    public boolean existsByName(String name) {
        return ingredientRepository.existsByName(name);
    }
}
