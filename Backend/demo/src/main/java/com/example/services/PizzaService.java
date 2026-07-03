package com.example.services;

import com.example.models.Pizza;
import com.example.repositories.PizzaRepository;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PizzaService {

    private final PizzaRepository pizzaRepository;
    private final com.example.repositories.CategoryRepository categoryRepository;

    public PizzaService(PizzaRepository pizzaRepository,
            com.example.repositories.CategoryRepository categoryRepository) {
        this.pizzaRepository = pizzaRepository;
        this.categoryRepository = categoryRepository;
    }

    @Cacheable("pizzas")
    public List<Pizza> listarPizzas() {
        return pizzaRepository.findByDeletedAtIsNull();
    }

    public Optional<Pizza> obtenerPorId(Integer id) {
        return pizzaRepository.findById(id);
    }

    @CacheEvict(value = "pizzas", allEntries = true)
    public Pizza crearPizza(Pizza pizza) {
        if (pizza.getName() == null || pizza.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Complete todos los campos requeridos.");
        }
        if (pizza.getCategory() == null || pizza.getCategory().getId() == null
                || !categoryRepository.existsById(pizza.getCategory().getId())) {
            throw new IllegalArgumentException("Categoría no encontrada");
        }
        return pizzaRepository.save(pizza);
    }

    @CacheEvict(value = "pizzas", allEntries = true)
    public Pizza actualizarPizza(Integer id, Pizza pizzaActualizada) {
        if (pizzaActualizada.getName() == null || pizzaActualizada.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Complete todos los campos requeridos.");
        }
        if (pizzaActualizada.getCategory() == null || pizzaActualizada.getCategory().getId() == null
                || !categoryRepository.existsById(pizzaActualizada.getCategory().getId())) {
            throw new IllegalArgumentException("Categoría no encontrada");
        }

        Optional<Pizza> pizzaOpt = pizzaRepository.findById(id);
        if (pizzaOpt.isPresent()) {
            Pizza pizza = pizzaOpt.get();
            pizza.setCategory(pizzaActualizada.getCategory());
            pizza.setName(pizzaActualizada.getName());
            pizza.setDescription(pizzaActualizada.getDescription());
            pizza.setImageUrl(pizzaActualizada.getImageUrl());
            pizza.setPrice(pizzaActualizada.getPrice());
            pizza.setIsAvailable(pizzaActualizada.getIsAvailable());
            pizza.setIsPopular(pizzaActualizada.getIsPopular());
            pizza.setDeletedAt(pizzaActualizada.getDeletedAt());
            return pizzaRepository.save(pizza);
        }
        return null;
    }

    @CacheEvict(value = "pizzas", allEntries = true)
    public boolean eliminarPizza(Integer id) {
        Optional<Pizza> pizzaOpt = pizzaRepository.findById(id);
        if (pizzaOpt.isPresent()) {
            Pizza pizza = pizzaOpt.get();
            pizza.setDeletedAt(java.time.LocalDateTime.now());
            pizzaRepository.save(pizza);
            return true;
        }
        return false;
    }

    public boolean existsByName(String name) {
        return pizzaRepository.existsByName(name);
    }
}
