package com.example.services;

import com.example.models.PizzaIngredient;
import com.example.models.PizzaIngredientId;
import com.example.repositories.PizzaIngredientRepository;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    @CacheEvict(value = "pizzas", allEntries = true)
    public PizzaIngredient crearPizzaIngrediente(PizzaIngredient pizzaIngredient) {
        if (pizzaIngredient.getQuantity() == null || pizzaIngredient.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            pizzaIngredient.setQuantity(new BigDecimal("50.00"));
        }
        return pizzaIngredientRepository.save(pizzaIngredient);
    }

    @CacheEvict(value = "pizzas", allEntries = true)
    public PizzaIngredient actualizarPizzaIngrediente(PizzaIngredientId id,
            PizzaIngredient pizzaIngredientActualizado) {
        Optional<PizzaIngredient> piOpt = pizzaIngredientRepository.findById(id);
        if (piOpt.isPresent()) {
            PizzaIngredient pi = piOpt.get();
            pi.setIsDefault(pizzaIngredientActualizado.getIsDefault());
            if (pizzaIngredientActualizado.getQuantity() != null
                    && pizzaIngredientActualizado.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
                pi.setQuantity(pizzaIngredientActualizado.getQuantity());
            }
            if (pizzaIngredientActualizado.getCreatedAt() != null) {
                pi.setCreatedAt(pizzaIngredientActualizado.getCreatedAt());
            }
            return pizzaIngredientRepository.save(pi);
        }
        return null;
    }

    @CacheEvict(value = "pizzas", allEntries = true)
    public boolean eliminarPizzaIngrediente(PizzaIngredientId id) {
        if (pizzaIngredientRepository.existsById(id)) {
            pizzaIngredientRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @CacheEvict(value = "pizzas", allEntries = true)
    public void eliminarRecetaDePizza(Integer pizzaId) {
        pizzaIngredientRepository.deleteAllByPizzaId(pizzaId);
        pizzaIngredientRepository.flush();
    }

    public List<PizzaIngredient> listarPorPizzaId(Integer pizzaId) {
        return pizzaIngredientRepository.findByPizzaId(pizzaId);
    }

    public List<PizzaIngredient> listarPorIngredientId(Integer ingredientId) {
        return pizzaIngredientRepository.findByIngredientId(ingredientId);
    }
}
