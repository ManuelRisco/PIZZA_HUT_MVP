package com.example.services;

import com.example.models.Ingredient;
import com.example.repositories.IngredientRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class IngredientService {

    private final IngredientRepository ingredientRepository;

    public IngredientService(IngredientRepository ingredientRepository) {
        this.ingredientRepository = ingredientRepository;
    }

    public List<Ingredient> listarIngredientes() {
        return ingredientRepository.findAll();
    }

    @SuppressWarnings("null")
    public Optional<Ingredient> obtenerPorId(Integer id) {
        return ingredientRepository.findById(id);
    }

    public Ingredient crearIngrediente(Ingredient ingredient) {
        if (ingredientRepository.existsByName(ingredient.getName())) {
            throw new IllegalArgumentException("El nombre del ingrediente ya está registrado.");
        }
        return ingredientRepository.save(ingredient);
    }

    public Ingredient actualizarIngrediente(Integer id, Ingredient ingredientActualizado) {
        @SuppressWarnings("null")
        Optional<Ingredient> ingredientOpt = ingredientRepository.findById(id);
        if (ingredientOpt.isPresent()) {
            Ingredient ingredient = ingredientOpt.get();

            if (!ingredient.getName().equals(ingredientActualizado.getName())
                    && ingredientRepository.existsByName(ingredientActualizado.getName())) {
                throw new IllegalArgumentException("El nombre del ingrediente ya está registrado.");
            }

            ingredient.setName(ingredientActualizado.getName());
            ingredient.setExtraCost(ingredientActualizado.getExtraCost());
            ingredient.setAvailable(ingredientActualizado.isAvailable());
            ingredient.setUpdatedAt(java.time.LocalDateTime.now());
            ingredient.setDeletedAt(ingredientActualizado.getDeletedAt());
            return ingredientRepository.save(ingredient);
        }
        return null;
    }

    public boolean eliminarIngrediente(Integer id) {
        @SuppressWarnings("null")
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
