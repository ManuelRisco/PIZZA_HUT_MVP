package com.example.service;

import com.example.domain.model.Pizza;
import com.example.domain.repository.PizzaRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PizzaService {

    private final PizzaRepository pizzaRepository;

    public PizzaService(PizzaRepository pizzaRepository) {
        this.pizzaRepository = pizzaRepository;
    }

    public List<Pizza> listarPizzas() {
        return pizzaRepository.findAll();
    }

    public Optional<Pizza> obtenerPorId(Integer id) {
        return pizzaRepository.findById(id);
    }

    public Pizza crearPizza(Pizza pizza) {
        return pizzaRepository.save(pizza);
    }

    public Pizza actualizarPizza(Integer id, Pizza pizzaActualizada) {
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
            pizza.setUpdatedAt(java.time.LocalDateTime.now());
            pizza.setDeletedAt(pizzaActualizada.getDeletedAt());
            return pizzaRepository.save(pizza);
        }
        return null;
    }

    public boolean eliminarPizza(Integer id) {
        if (pizzaRepository.existsById(id)) {
            pizzaRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public boolean existsByName(String name) {
        return pizzaRepository.existsByName(name);
    }
}
