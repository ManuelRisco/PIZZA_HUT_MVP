package com.example.controllers;

import com.example.dtos.ApiResponse;
import com.example.mappers.PizzaMapper;
import com.example.dtos.PizzaCreateDTO;
import com.example.dtos.PizzaDTO;
import com.example.models.Category;
import com.example.models.Pizza;
import com.example.services.CategoryService;
import com.example.services.PizzaService;
import com.example.exceptions.BadRequestException;
import com.example.exceptions.ResourceNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pizzas")
@CrossOrigin(origins = "http://localhost:4200")
public class PizzaController {

    private static final String MSG_NOT_FOUND_PREFIX = "Pizza no encontrada con id: ";

    private final PizzaService pizzaService;
    private final CategoryService categoryService;
    private final PizzaMapper pizzaMapper;

    public PizzaController(PizzaService pizzaService, CategoryService categoryService, PizzaMapper pizzaMapper) {
        this.pizzaService = pizzaService;
        this.categoryService = categoryService;
        this.pizzaMapper = pizzaMapper;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PizzaDTO>>> getAllPizzas() {
        List<Pizza> pizzas = pizzaService.listarPizzas();
        return ResponseEntity.ok(ApiResponse.success(pizzaMapper.toDtoList(pizzas)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PizzaDTO>> getPizzaById(@PathVariable("id") Integer id) {
        Pizza pizza = pizzaService.obtenerPorId(id)
            .orElseThrow(() -> new ResourceNotFoundException(MSG_NOT_FOUND_PREFIX + id));
        return ResponseEntity.ok(ApiResponse.success(pizzaMapper.toDto(pizza)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PizzaDTO>> createPizza(@RequestBody PizzaCreateDTO pizzaDTO) {
        if (pizzaService.existsByName(pizzaDTO.getName())) {
            throw new BadRequestException("El nombre de la pizza ya existe");
        }
        
        Category category = categoryService.obtenerPorId(pizzaDTO.getCategoryId())
            .orElseThrow(() -> new BadRequestException("Categoría no encontrada"));
            
        Pizza pizza = pizzaMapper.toEntity(pizzaDTO);
        pizza.setCategory(category);
        
        Pizza nuevaPizza = pizzaService.crearPizza(pizza);
        return new ResponseEntity<>(ApiResponse.success(pizzaMapper.toDto(nuevaPizza), "Pizza creada con éxito"), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PizzaDTO>> updatePizza(@PathVariable("id") Integer id, @RequestBody PizzaCreateDTO pizzaDTO) {
        Pizza pizzaExistente = pizzaService.obtenerPorId(id)
            .orElseThrow(() -> new ResourceNotFoundException(MSG_NOT_FOUND_PREFIX + id));
            
        if (pizzaDTO.getCategoryId() != null) {
            Category category = categoryService.obtenerPorId(pizzaDTO.getCategoryId())
                .orElseThrow(() -> new BadRequestException("Categoría no encontrada"));
            pizzaExistente.setCategory(category);
        }
        
        pizzaMapper.updateEntityFromDto(pizzaDTO, pizzaExistente);
        Pizza pizzaActualizada = pizzaService.actualizarPizza(id, pizzaExistente);
        
        return ResponseEntity.ok(ApiResponse.success(pizzaMapper.toDto(pizzaActualizada), "Pizza actualizada con éxito"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePizza(@PathVariable("id") Integer id) {
        if (!pizzaService.eliminarPizza(id)) {
            throw new ResourceNotFoundException(MSG_NOT_FOUND_PREFIX + id);
        }
        return ResponseEntity.ok(ApiResponse.success(null, "Pizza eliminada correctamente"));
    }
}
