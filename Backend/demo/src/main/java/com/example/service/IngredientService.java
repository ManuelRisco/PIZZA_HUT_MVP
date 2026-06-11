package com.example.service;

import com.example.domain.model.Ingredient;
import com.example.domain.repository.IngredientRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class IngredientService {

    private final IngredientRepository ingredientRepository;

    public IngredientService(IngredientRepository ingredientRepository) {
        this.ingredientRepository = ingredientRepository;
    }

    public List<Ingredient> listarIngredientes() {
        return ingredientRepository.findAll();
    }

    public Optional<Ingredient> obtenerPorId(Integer id) {
        return ingredientRepository.findById(id);
    }

    public Ingredient crearIngrediente(Ingredient ingredient) {
        return ingredientRepository.save(ingredient);
    }

    public Ingredient actualizarIngrediente(Integer id, Ingredient ingredientActualizado) {
        Optional<Ingredient> ingredientOpt = ingredientRepository.findById(id);
        if (ingredientOpt.isPresent()) {
            Ingredient ingredient = ingredientOpt.get();
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
        if (ingredientRepository.existsById(id)) {
            ingredientRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public boolean existsByName(String name) {
        return ingredientRepository.existsByName(name);
    }
}
