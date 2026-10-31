package com.example.controllers;

import com.example.models.PizzaIngredient;
import com.example.models.PizzaIngredientId;
import com.example.services.PizzaIngredientService;
import com.example.dtos.PizzaIngredientDTO;
import com.example.models.Pizza;
import com.example.models.Ingredient;

import org.springframework.http.ResponseEntity;
import com.example.dtos.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/pizza-ingredients")
@CrossOrigin(origins = "http://localhost:4200")
@PreAuthorize("hasRole('ADMIN')")
public class PizzaIngredientController {

    private static final String MSG_KEY = "message";
    private static final String MSG_NOT_FOUND = "Relación no encontrada";

    private final PizzaIngredientService pizzaIngredientService;

    public PizzaIngredientController(PizzaIngredientService pizzaIngredientService) {
        this.pizzaIngredientService = pizzaIngredientService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PizzaIngredientDTO>>> listarPizzaIngredientes() {
        List<PizzaIngredientDTO> dtos = pizzaIngredientService.listarPizzaIngredientes().stream()
                .map(PizzaIngredientDTO::new)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/{pizzaId}/{ingredientId}")
    public ResponseEntity<Object> obtenerPorId(@PathVariable("pizzaId") Integer pizzaId, @PathVariable("ingredientId") Integer ingredientId) {
        PizzaIngredientId id = new PizzaIngredientId(pizzaId, ingredientId);
        Optional<PizzaIngredient> piOpt = pizzaIngredientService.obtenerPorId(id);
        if (piOpt.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success(new PizzaIngredientDTO(piOpt.get())));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(MSG_KEY, MSG_NOT_FOUND));
        }
    }

    @PostMapping
    public ResponseEntity<Object> crearPizzaIngrediente(@RequestBody PizzaIngredientDTO dto) {
        PizzaIngredient pizzaIngredient = new PizzaIngredient();
        pizzaIngredient.setId(new PizzaIngredientId(dto.getPizzaId(), dto.getIngredientId()));

        Pizza pizza = new Pizza();
        pizza.setId(dto.getPizzaId());
        pizzaIngredient.setPizza(pizza);

        Ingredient ingredient = new Ingredient();
        ingredient.setId(dto.getIngredientId());
        pizzaIngredient.setIngredient(ingredient);

        pizzaIngredient.setIsDefault(dto.getIsDefault());
        pizzaIngredient.setQuantity(dto.getQuantity() != null && dto.getQuantity().compareTo(BigDecimal.ZERO) > 0
                ? dto.getQuantity()
                : new BigDecimal("50.00"));

        PizzaIngredient nuevo = pizzaIngredientService.crearPizzaIngrediente(pizzaIngredient);
        return ResponseEntity.ok(ApiResponse.success(new PizzaIngredientDTO(nuevo)));
    }

    @PutMapping("/{pizzaId}/{ingredientId}")
    public ResponseEntity<Object> actualizarPizzaIngrediente(@PathVariable("pizzaId") Integer pizzaId, @PathVariable("ingredientId") Integer ingredientId,
                                                       @RequestBody PizzaIngredientDTO dto) {
        PizzaIngredient pizzaIngredientActualizado = new PizzaIngredient();
        pizzaIngredientActualizado.setId(new PizzaIngredientId(pizzaId, ingredientId));
        pizzaIngredientActualizado.setIsDefault(dto.getIsDefault());
        pizzaIngredientActualizado.setQuantity(dto.getQuantity());

        PizzaIngredientId id = new PizzaIngredientId(pizzaId, ingredientId);
        PizzaIngredient pi = pizzaIngredientService.actualizarPizzaIngrediente(id, pizzaIngredientActualizado);
        if (pi != null) {
            return ResponseEntity.ok(ApiResponse.success(new PizzaIngredientDTO(pi)));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(MSG_KEY, MSG_NOT_FOUND));
        }
    }

    @DeleteMapping("/{pizzaId}/{ingredientId}")
    public ResponseEntity<Object> eliminarPizzaIngrediente(@PathVariable("pizzaId") Integer pizzaId, @PathVariable("ingredientId") Integer ingredientId) {
        PizzaIngredientId id = new PizzaIngredientId(pizzaId, ingredientId);
        boolean eliminado = pizzaIngredientService.eliminarPizzaIngrediente(id);
        if (eliminado) {
            return ResponseEntity.ok(ApiResponse.success(Map.of(MSG_KEY, "Relación eliminada correctamente")));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(MSG_KEY, MSG_NOT_FOUND));
        }
    }

    @GetMapping("/pizza/{pizzaId}")
    public ResponseEntity<ApiResponse<List<PizzaIngredientDTO>>> listarPorPizzaId(@PathVariable("pizzaId") Integer pizzaId) {
        List<PizzaIngredientDTO> dtos = pizzaIngredientService.listarPorPizzaId(pizzaId).stream()
                .map(PizzaIngredientDTO::new)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/ingredient/{ingredientId}")
    public ResponseEntity<ApiResponse<List<PizzaIngredientDTO>>> listarPorIngredientId(@PathVariable("ingredientId") Integer ingredientId) {
        List<PizzaIngredientDTO> dtos = pizzaIngredientService.listarPorIngredientId(ingredientId).stream()
                .map(PizzaIngredientDTO::new)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }
}
