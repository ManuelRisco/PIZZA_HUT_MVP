package com.example.services;

import com.example.models.PizzaIngredient;
import com.example.models.PizzaIngredientId;
import com.example.repositories.PizzaIngredientRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PizzaIngredientService {

    private final PizzaIngredientRepository pizzaIngredientRepository;

    public PizzaIngredientService(PizzaIngredientRepository pizzaIngredientRepository) {
        this.pizzaIngredientRepository = pizzaIngredientRepository;
    }

    public List<PizzaIngredient> listarPizzaIngredientes() {
        return pizzaIngredientRepository.findAll();
    }

    public Optional<PizzaIngredient> obtenerPorId(PizzaIngredientId id) {
        return pizzaIngredientRepository.findById(id);
    }

    public PizzaIngredient crearPizzaIngrediente(PizzaIngredient pizzaIngredient) {
        return pizzaIngredientRepository.save(pizzaIngredient);
    }

    public PizzaIngredient actualizarPizzaIngrediente(PizzaIngredientId id,
            PizzaIngredient pizzaIngredientActualizado) {
        Optional<PizzaIngredient> piOpt = pizzaIngredientRepository.findById(id);
        if (piOpt.isPresent()) {
            PizzaIngredient pi = piOpt.get();
            pi.setIsDefault(pizzaIngredientActualizado.getIsDefault());
            pi.setCreatedAt(pizzaIngredientActualizado.getCreatedAt());
            // Puedes agregar m\u00e1s campos si lo necesitas
            return pizzaIngredientRepository.save(pi);
        }
        return null;
    }

    public boolean eliminarPizzaIngrediente(PizzaIngredientId id) {
        if (pizzaIngredientRepository.existsById(id)) {
            pizzaIngredientRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<PizzaIngredient> listarPorPizzaId(Integer pizzaId) {
        return pizzaIngredientRepository.findByPizzaId(pizzaId);
    }

    public List<PizzaIngredient> listarPorIngredientId(Integer ingredientId) {
        return pizzaIngredientRepository.findByIngredientId(ingredientId);
    }
}
