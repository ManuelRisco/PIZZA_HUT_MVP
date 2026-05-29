package com.example.infrastructure.controller;

import com.example.service.PizzaService;
import com.example.service.CategoryService;
import com.example.domain.dto.PizzaCreateDTO;
import com.example.domain.dto.PizzaDTO;
import com.example.domain.model.Category;
import com.example.domain.model.Pizza;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pizzas")
@CrossOrigin(origins = "http://localhost:4200")
public class PizzaController {

    @Autowired
    private PizzaService pizzaService;

    @Autowired
    private CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<PizzaDTO>> listarPizzas() {
        List<Pizza> pizzas = pizzaService.listarPizzas();
        List<PizzaDTO> pizzasDTO = pizzas.stream()
            .map(PizzaDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(pizzasDTO);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPizzaPorId(@PathVariable Integer id) {
        Optional<Pizza> pizzaOpt = pizzaService.obtenerPorId(id);
        if (pizzaOpt.isPresent()) {
            PizzaDTO pizzaDTO = new PizzaDTO(pizzaOpt.get());
            return ResponseEntity.ok(pizzaDTO);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Pizza no encontrada"));
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> crearPizza(@RequestBody PizzaCreateDTO pizzaDTO) {
        if (pizzaService.existsByName(pizzaDTO.getName())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "El nombre de la pizza ya existe"));
        }
        
        // Buscar la categoría
        Optional<Category> categoryOpt = categoryService.obtenerPorId(pizzaDTO.getCategoryId());
        if (!categoryOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Categoría no encontrada"));
        }
        
        // Crear pizza desde DTO
        Pizza pizza = new Pizza();
        pizza.setCategory(categoryOpt.get());
        pizza.setName(pizzaDTO.getName());
        pizza.setDescription(pizzaDTO.getDescription());
        pizza.setImageUrl(pizzaDTO.getImageUrl());
        pizza.setPrice(pizzaDTO.getPrice());
        pizza.setIsAvailable(pizzaDTO.isAvailable());
        pizza.setIsPopular(pizzaDTO.isPopular());
        
        Pizza nuevaPizza = pizzaService.crearPizza(pizza);
        PizzaDTO responseDTO = new PizzaDTO(nuevaPizza);
        return ResponseEntity.ok(responseDTO);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> actualizarPizza(@PathVariable Integer id, @RequestBody PizzaCreateDTO pizzaDTO) {
        // Buscar la categoría si se proporciona
        Category category = null;
        if (pizzaDTO.getCategoryId() != null) {
            Optional<Category> categoryOpt = categoryService.obtenerPorId(pizzaDTO.getCategoryId());
            if (!categoryOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Categoría no encontrada"));
            }
            category = categoryOpt.get();
        }
        
        // Crear pizza desde DTO
        Pizza pizzaActualizada = new Pizza();
        pizzaActualizada.setCategory(category);
        pizzaActualizada.setName(pizzaDTO.getName());
        pizzaActualizada.setDescription(pizzaDTO.getDescription());
        pizzaActualizada.setImageUrl(pizzaDTO.getImageUrl());
        pizzaActualizada.setPrice(pizzaDTO.getPrice());
        pizzaActualizada.setIsAvailable(pizzaDTO.isAvailable());
        pizzaActualizada.setIsPopular(pizzaDTO.isPopular());
        
        Pizza pizza = pizzaService.actualizarPizza(id, pizzaActualizada);
        if (pizza != null) {
            PizzaDTO responseDTO = new PizzaDTO(pizza);
            return ResponseEntity.ok(responseDTO);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Pizza no encontrada"));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> eliminarPizza(@PathVariable Integer id) {
        boolean eliminado = pizzaService.eliminarPizza(id);
        if (eliminado) {
            return ResponseEntity.ok(Map.of("message", "Pizza eliminada correctamente"));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Pizza no encontrada"));
        }
    }
}
