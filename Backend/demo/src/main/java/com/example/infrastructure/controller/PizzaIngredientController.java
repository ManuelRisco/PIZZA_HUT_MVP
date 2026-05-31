package com.example.infrastructure.controller;

import com.example.domain.model.PizzaIngredient;
import com.example.domain.model.PizzaIngredientId;
import com.example.service.PizzaIngredientService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/pizza-ingredients")
@CrossOrigin(origins = "http://localhost:4200")
public class PizzaIngredientController {

    @Autowired
    private PizzaIngredientService pizzaIngredientService;

    @GetMapping
    public ResponseEntity<List<PizzaIngredient>> listarPizzaIngredientes() {
        return ResponseEntity.ok(pizzaIngredientService.listarPizzaIngredientes());
    }

    @GetMapping("/{pizzaId}/{ingredientId}")
    public ResponseEntity<?> obtenerPorId(@PathVariable("pizzaId") Integer pizzaId, @PathVariable("ingredientId") Integer ingredientId) {
        PizzaIngredientId id = new PizzaIngredientId(pizzaId, ingredientId);
        Optional<PizzaIngredient> piOpt = pizzaIngredientService.obtenerPorId(id);
        if (piOpt.isPresent()) {
            return ResponseEntity.ok(piOpt.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Relación no encontrada"));
        }
    }

    @PostMapping
    public ResponseEntity<?> crearPizzaIngrediente(@RequestBody PizzaIngredient pizzaIngredient) {
        PizzaIngredient nuevo = pizzaIngredientService.crearPizzaIngrediente(pizzaIngredient);
        return ResponseEntity.ok(nuevo);
    }

    @PutMapping("/{pizzaId}/{ingredientId}")
    public ResponseEntity<?> actualizarPizzaIngrediente(@PathVariable("pizzaId") Integer pizzaId, @PathVariable("ingredientId") Integer ingredientId,
                                                       @RequestBody PizzaIngredient pizzaIngredientActualizado) {
        PizzaIngredientId id = new PizzaIngredientId(pizzaId, ingredientId);
        PizzaIngredient pi = pizzaIngredientService.actualizarPizzaIngrediente(id, pizzaIngredientActualizado);
        if (pi != null) {
            return ResponseEntity.ok(pi);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Relación no encontrada"));
        }
    }

    @DeleteMapping("/{pizzaId}/{ingredientId}")
    public ResponseEntity<?> eliminarPizzaIngrediente(@PathVariable("pizzaId") Integer pizzaId, @PathVariable("ingredientId") Integer ingredientId) {
        PizzaIngredientId id = new PizzaIngredientId(pizzaId, ingredientId);
        boolean eliminado = pizzaIngredientService.eliminarPizzaIngrediente(id);
        if (eliminado) {
            return ResponseEntity.ok(Map.of("message", "Relación eliminada correctamente"));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Relación no encontrada"));
        }
    }

    @GetMapping("/pizza/{pizzaId}")
    public ResponseEntity<List<PizzaIngredient>> listarPorPizzaId(@PathVariable("pizzaId") Integer pizzaId) {
        return ResponseEntity.ok(pizzaIngredientService.listarPorPizzaId(pizzaId));
    }

    @GetMapping("/ingredient/{ingredientId}")
    public ResponseEntity<List<PizzaIngredient>> listarPorIngredientId(@PathVariable("ingredientId") Integer ingredientId) {
        return ResponseEntity.ok(pizzaIngredientService.listarPorIngredientId(ingredientId));
    }
}