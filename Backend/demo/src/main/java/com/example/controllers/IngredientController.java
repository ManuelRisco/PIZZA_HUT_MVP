package com.example.controllers;

import com.example.services.IngredientService;
import com.example.dtos.IngredientDTO;
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
    public ResponseEntity<ApiResponse<List<IngredientDTO>>> listarIngredientes(@RequestParam(value = "includeDeleted", defaultValue = "false") boolean includeDeleted) {
        List<Ingredient> ingredientes = ingredientService.listarIngredientes(includeDeleted);
        List<IngredientDTO> ingredientesDTO = ingredientes.stream()
                .map(IngredientDTO::new)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(ingredientesDTO));
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<IngredientDTO>>> listarIngredientesDisponibles() {
        List<Ingredient> ingredientes = ingredientService.listarIngredientes().stream()
                .filter(Ingredient::isAvailable)
                .filter(Ingredient::isAllowAsExtra)
                .filter(i -> {
                    BigDecimal stock = i.getStockQuantity() != null ? i.getStockQuantity() : BigDecimal.ZERO;
                    BigDecimal minPortion = i.getExtraPortionQuantity() != null && i.getExtraPortionQuantity().compareTo(BigDecimal.ZERO) > 0
                            ? i.getExtraPortionQuantity()
                            : BigDecimal.ONE;
                    return stock.compareTo(minPortion) >= 0;
                })
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> crearIngrediente(@RequestBody IngredientDTO ingredientDTO) {
        if (ingredientService.existsByName(ingredientDTO.getName())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(MSG_KEY, "El nombre del ingrediente ya existe"));
        }

        Ingredient ingredient = mapFromDto(ingredientDTO);
        Ingredient nuevoIngrediente = ingredientService.crearIngrediente(ingredient);
        IngredientDTO responseDTO = new IngredientDTO(nuevoIngrediente);
        return ResponseEntity.ok(ApiResponse.success(responseDTO));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> actualizarIngrediente(@PathVariable("id") Integer id,
            @RequestBody IngredientDTO ingredientDTO) {
        Optional<Ingredient> existingOpt = ingredientService.obtenerPorId(id);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(MSG_KEY, MSG_NOT_FOUND));
        }
        Ingredient existing = existingOpt.get();
        Ingredient ingredientActualizado = mapFromDtoWithFallback(ingredientDTO, existing);

        Ingredient ingredient = ingredientService.actualizarIngrediente(id, ingredientActualizado);
        if (ingredient != null) {
            IngredientDTO responseDTO = new IngredientDTO(ingredient);
            return ResponseEntity.ok(ApiResponse.success(responseDTO));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(MSG_KEY, MSG_NOT_FOUND));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> eliminarIngrediente(@PathVariable("id") Integer id) {
        boolean eliminado = ingredientService.eliminarIngrediente(id);
        if (eliminado) {
            return ResponseEntity.ok(ApiResponse.success(Map.of(MSG_KEY, "Ingrediente eliminado lógicamente")));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(MSG_KEY, MSG_NOT_FOUND));
        }
    }

    @PatchMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> restoreIngredient(@PathVariable("id") Integer id) {
        boolean restaurado = ingredientService.restaurarIngrediente(id);
        if (restaurado) {
            return ResponseEntity.ok(ApiResponse.success(Map.of(MSG_KEY, "Ingrediente restaurado con éxito")));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(MSG_KEY, MSG_NOT_FOUND));
        }
    }

    @PatchMapping("/{id}/availability")
    @PreAuthorize("hasRole('ADMIN')")
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

    private Ingredient mapFromDto(IngredientDTO dto) {
        Ingredient ingredient = new Ingredient();
        ingredient.setName(dto.getName());
        ingredient.setExtraCost(dto.getExtraCost() != null ? dto.getExtraCost() : BigDecimal.ZERO);
        ingredient.setAvailable(dto.getIsAvailable());
        ingredient.setUnit(dto.getUnit() != null && !dto.getUnit().isBlank() ? dto.getUnit().trim() : "g");
        ingredient.setStockQuantity(dto.getStockQuantity() != null ? dto.getStockQuantity() : BigDecimal.ZERO);
        ingredient.setMinStock(dto.getMinStock() != null ? dto.getMinStock() : BigDecimal.ZERO);
        ingredient.setUnitCost(dto.getUnitCost() != null ? dto.getUnitCost() : BigDecimal.ZERO);
        ingredient.setExtraPortionQuantity(dto.getExtraPortionQuantity() != null ? dto.getExtraPortionQuantity() : new BigDecimal("50.00"));
        ingredient.setAllowAsExtra(dto.getAllowAsExtra() != null ? dto.getAllowAsExtra() : true);
        return ingredient;
    }

    private Ingredient mapFromDtoWithFallback(IngredientDTO dto, Ingredient existing) {
        Ingredient ingredient = new Ingredient();
        ingredient.setName(dto.getName() != null ? dto.getName() : existing.getName());
        ingredient.setExtraCost(dto.getExtraCost() != null ? dto.getExtraCost() : existing.getExtraCost());
        ingredient.setAvailable(dto.getIsAvailable());
        ingredient.setUnit(dto.getUnit() != null && !dto.getUnit().isBlank() ? dto.getUnit().trim() : existing.getUnit());
        ingredient.setStockQuantity(dto.getStockQuantity() != null ? dto.getStockQuantity() : existing.getStockQuantity());
        ingredient.setMinStock(dto.getMinStock() != null ? dto.getMinStock() : existing.getMinStock());
        ingredient.setUnitCost(dto.getUnitCost() != null ? dto.getUnitCost() : existing.getUnitCost());
        ingredient.setExtraPortionQuantity(dto.getExtraPortionQuantity() != null ? dto.getExtraPortionQuantity() : existing.getExtraPortionQuantity());
        ingredient.setAllowAsExtra(dto.getAllowAsExtra() != null ? dto.getAllowAsExtra() : existing.isAllowAsExtra());
        return ingredient;
    }
}
