package com.example.controllers;

import com.example.services.IngredientService;
import com.example.dtos.IngredientDTO;
import com.example.models.Ingredient;

import org.springframework.http.ResponseEntity;
import com.example.dtos.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/ingredients")
@CrossOrigin(origins = "http://localhost:4200")
public class IngredientController {

    private static final String MSG_KEY = "message";
    private static final String MSG_NOT_FOUND = "Ingrediente no encontrado";

    private final IngredientService ingredientService;

    public IngredientController(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<IngredientDTO>>> listarIngredientes() {
        List<Ingredient> ingredientes = ingredientService.listarIngredientes();
        List<IngredientDTO> ingredientesDTO = ingredientes.stream()
                .map(IngredientDTO::new)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(ingredientesDTO));
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<IngredientDTO>>> listarIngredientesDisponibles() {
        List<Ingredient> ingredientes = ingredientService.listarIngredientes().stream()
                .filter(i -> i.isAvailable())
                .toList();
        List<IngredientDTO> ingredientesDTO = ingredientes.stream()
                .map(IngredientDTO::new)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(ingredientesDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> obtenerIngredientePorId(@PathVariable("id") Integer id) {
        Optional<Ingredient> ingredientOpt = ingredientService.obtenerPorId(id);
        if (ingredientOpt.isPresent()) {
            IngredientDTO ingredientDTO = new IngredientDTO(ingredientOpt.get());
            return ResponseEntity.ok(ApiResponse.success(ingredientDTO));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(MSG_KEY, MSG_NOT_FOUND));
        }
    }

    @PostMapping
    public ResponseEntity<Object> crearIngrediente(@RequestBody IngredientDTO ingredientDTO) {
        if (ingredientService.existsByName(ingredientDTO.getName())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(MSG_KEY, "El nombre del ingrediente ya existe"));
        }

        Ingredient ingredient = new Ingredient();
        ingredient.setName(ingredientDTO.getName());
        ingredient.setExtraCost(ingredientDTO.getExtraCost());
        ingredient.setAvailable(ingredientDTO.getIsAvailable());

        Ingredient nuevoIngrediente = ingredientService.crearIngrediente(ingredient);
        IngredientDTO responseDTO = new IngredientDTO(nuevoIngrediente);
        return ResponseEntity.ok(ApiResponse.success(responseDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> actualizarIngrediente(@PathVariable("id") Integer id,
            @RequestBody IngredientDTO ingredientDTO) {
        Ingredient ingredientActualizado = new Ingredient();
        ingredientActualizado.setName(ingredientDTO.getName());
        ingredientActualizado.setExtraCost(ingredientDTO.getExtraCost());
        ingredientActualizado.setAvailable(ingredientDTO.getIsAvailable());

        Ingredient ingredient = ingredientService.actualizarIngrediente(id, ingredientActualizado);
        if (ingredient != null) {
            IngredientDTO responseDTO = new IngredientDTO(ingredient);
            return ResponseEntity.ok(ApiResponse.success(responseDTO));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(MSG_KEY, MSG_NOT_FOUND));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> eliminarIngrediente(@PathVariable("id") Integer id) {
        boolean eliminado = ingredientService.eliminarIngrediente(id);
        if (eliminado) {
            return ResponseEntity.ok(ApiResponse.success(Map.of(MSG_KEY, "Ingrediente eliminado correctamente")));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(MSG_KEY, MSG_NOT_FOUND));
        }
    }

    @PatchMapping("/{id}/availability")
    public ResponseEntity<Object> cambiarDisponibilidad(
            @PathVariable("id") Integer id,
            @RequestParam("available") boolean available) {
        Optional<Ingredient> ingredientOpt = ingredientService.obtenerPorId(id);
        if (ingredientOpt.isPresent()) {
            Ingredient ingredient = ingredientOpt.get();
            ingredient.setAvailable(available);
            Ingredient actualizado = ingredientService.actualizarIngrediente(id, ingredient);
            IngredientDTO responseDTO = new IngredientDTO(actualizado);
            return ResponseEntity.ok(ApiResponse.success(responseDTO));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(MSG_KEY, MSG_NOT_FOUND));
        }
    }
}

