package com.example.controllers;

import com.example.models.PizzaIngredient;
import com.example.models.PizzaIngredientId;
import com.example.services.PizzaIngredientService;

import org.springframework.http.ResponseEntity;
import com.example.dtos.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/pizza-ingredients")
@CrossOrigin(origins = "http://localhost:4200")
public class PizzaIngredientController {

    private final PizzaIngredientService pizzaIngredientService;

    public PizzaIngredientController(PizzaIngredientService pizzaIngredientService) {
        this.pizzaIngredientService = pizzaIngredientService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PizzaIngredient>>> listarPizzaIngredientes() {
        return ResponseEntity.ok(ApiResponse.success(pizzaIngredientService.listarPizzaIngredientes()));
    }

    @GetMapping("/{pizzaId}/{ingredientId}")
    public ResponseEntity<?> obtenerPorId(@PathVariable("pizzaId") Integer pizzaId, @PathVariable("ingredientId") Integer ingredientId) {
        PizzaIngredientId id = new PizzaIngredientId(pizzaId, ingredientId);
        Optional<PizzaIngredient> piOpt = pizzaIngredientService.obtenerPorId(id);
        if (piOpt.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success(piOpt.get()));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Relaci\u00f3n no encontrada"));
        }
    }

    @PostMapping
    public ResponseEntity<?> crearPizzaIngrediente(@RequestBody PizzaIngredient pizzaIngredient) {
        PizzaIngredient nuevo = pizzaIngredientService.crearPizzaIngrediente(pizzaIngredient);
        return ResponseEntity.ok(ApiResponse.success(nuevo));
    }

    @PutMapping("/{pizzaId}/{ingredientId}")
    public ResponseEntity<?> actualizarPizzaIngrediente(@PathVariable("pizzaId") Integer pizzaId, @PathVariable("ingredientId") Integer ingredientId,
                                                       @RequestBody PizzaIngredient pizzaIngredientActualizado) {
        PizzaIngredientId id = new PizzaIngredientId(pizzaId, ingredientId);
        PizzaIngredient pi = pizzaIngredientService.actualizarPizzaIngrediente(id, pizzaIngredientActualizado);
        if (pi != null) {
            return ResponseEntity.ok(ApiResponse.success(pi));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Relaci\u00f3n no encontrada"));
        }
    }

    @DeleteMapping("/{pizzaId}/{ingredientId}")
    public ResponseEntity<?> eliminarPizzaIngrediente(@PathVariable("pizzaId") Integer pizzaId, @PathVariable("ingredientId") Integer ingredientId) {
        PizzaIngredientId id = new PizzaIngredientId(pizzaId, ingredientId);
        boolean eliminado = pizzaIngredientService.eliminarPizzaIngrediente(id);
        if (eliminado) {
            return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Relaci\u00f3n eliminada correctamente")));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Relaci\u00f3n no encontrada"));
        }
    }

    @GetMapping("/pizza/{pizzaId}")
    public ResponseEntity<ApiResponse<List<PizzaIngredient>>> listarPorPizzaId(@PathVariable("pizzaId") Integer pizzaId) {
        return ResponseEntity.ok(ApiResponse.success(pizzaIngredientService.listarPorPizzaId(pizzaId)));
    }

    @GetMapping("/ingredient/{ingredientId}")
    public ResponseEntity<ApiResponse<List<PizzaIngredient>>> listarPorIngredientId(@PathVariable("ingredientId") Integer ingredientId) {
        return ResponseEntity.ok(ApiResponse.success(pizzaIngredientService.listarPorIngredientId(ingredientId)));
    }
}
