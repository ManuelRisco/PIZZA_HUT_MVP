package com.example.infrastructure.controller;

import com.example.service.IngredientService;
import com.example.domain.dto.IngredientDTO;
import com.example.domain.model.Ingredient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ingredients")
@CrossOrigin(origins = "http://localhost:4200")
public class IngredientController {

    @Autowired
    private IngredientService ingredientService;

    @GetMapping
    public ResponseEntity<List<IngredientDTO>> listarIngredientes() {
        List<Ingredient> ingredientes = ingredientService.listarIngredientes();
        List<IngredientDTO> ingredientesDTO = ingredientes.stream()
            .map(IngredientDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(ingredientesDTO);
    }

    @GetMapping("/available")
    public ResponseEntity<List<IngredientDTO>> listarIngredientesDisponibles() {
        List<Ingredient> ingredientes = ingredientService.listarIngredientes().stream()
            .filter(Ingredient::isAvailable)
            .collect(Collectors.toList());
        List<IngredientDTO> ingredientesDTO = ingredientes.stream()
            .map(IngredientDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(ingredientesDTO);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerIngredientePorId(@PathVariable("id") Integer id) { // Corregido
        Optional<Ingredient> ingredientOpt = ingredientService.obtenerPorId(id);
        if (ingredientOpt.isPresent()) {
            IngredientDTO ingredientDTO = new IngredientDTO(ingredientOpt.get());
            return ResponseEntity.ok(ingredientDTO);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Ingrediente no encontrado"));
        }
    }

    @PostMapping
    public ResponseEntity<?> crearIngrediente(@RequestBody IngredientDTO ingredientDTO) {
        if (ingredientService.existsByName(ingredientDTO.getName())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "El nombre del ingrediente ya existe"));
        }
        
        Ingredient ingredient = new Ingredient();
        ingredient.setName(ingredientDTO.getName());
        ingredient.setExtraCost(ingredientDTO.getExtraCost());
        ingredient.setAvailable(ingredientDTO.isAvailable());
        
        Ingredient nuevoIngrediente = ingredientService.crearIngrediente(ingredient);
        IngredientDTO responseDTO = new IngredientDTO(nuevoIngrediente);
        return ResponseEntity.ok(responseDTO);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarIngrediente(@PathVariable("id") Integer id, @RequestBody IngredientDTO ingredientDTO) { // Corregido
        Ingredient ingredientActualizado = new Ingredient();
        ingredientActualizado.setName(ingredientDTO.getName());
        ingredientActualizado.setExtraCost(ingredientDTO.getExtraCost());
        ingredientActualizado.setAvailable(ingredientDTO.isAvailable());
        
        Ingredient ingredient = ingredientService.actualizarIngrediente(id, ingredientActualizado);
        if (ingredient != null) {
            IngredientDTO responseDTO = new IngredientDTO(ingredient);
            return ResponseEntity.ok(responseDTO);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Ingrediente no encontrado"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarIngrediente(@PathVariable("id") Integer id) { // Corregido
        boolean eliminado = ingredientService.eliminarIngrediente(id);
        if (eliminado) {
            return ResponseEntity.ok(Map.of("message", "Ingrediente eliminado correctamente"));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Ingrediente no encontrado"));
        }
    }

    @PatchMapping("/{id}/availability")
    public ResponseEntity<?> cambiarDisponibilidad(
            @PathVariable("id") Integer id,            // Corregido
            @RequestParam("available") boolean available) { // Corregido: @RequestParam también lo necesita
        Optional<Ingredient> ingredientOpt = ingredientService.obtenerPorId(id);
        if (ingredientOpt.isPresent()) {
            Ingredient ingredient = ingredientOpt.get();
            ingredient.setAvailable(available);
            Ingredient actualizado = ingredientService.actualizarIngrediente(id, ingredient);
            IngredientDTO responseDTO = new IngredientDTO(actualizado);
            return ResponseEntity.ok(responseDTO);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Ingrediente no encontrado"));
        }
    }
}