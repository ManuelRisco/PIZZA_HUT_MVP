package com.example.controllers;

import com.example.dtos.ApiResponse;
import com.example.dtos.PizzaCreateDTO;
import com.example.dtos.PizzaDTO;
import com.example.dtos.PizzaIngredientDTO;
import com.example.exceptions.BadRequestException;
import com.example.exceptions.ResourceNotFoundException;
import com.example.mappers.PizzaMapper;
import com.example.models.Category;
import com.example.models.Ingredient;
import com.example.models.Pizza;
import com.example.models.PizzaIngredient;
import com.example.models.PizzaIngredientId;
import com.example.services.CategoryService;
import com.example.services.IngredientService;
import com.example.services.PizzaIngredientService;
import com.example.services.PizzaService;
import com.example.services.SizeService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pizzas")
@CrossOrigin(origins = "http://localhost:4200")
public class PizzaController {

    private static final String MSG_NOT_FOUND_PREFIX = "Pizza no encontrada con id: ";

    private final PizzaService pizzaService;
    private final CategoryService categoryService;
    private final PizzaMapper pizzaMapper;
    private final PizzaIngredientService pizzaIngredientService;
    private final IngredientService ingredientService;
    private final SizeService sizeService;

    public PizzaController(PizzaService pizzaService,
                           CategoryService categoryService,
                           PizzaMapper pizzaMapper,
                           PizzaIngredientService pizzaIngredientService,
                           IngredientService ingredientService,
                           SizeService sizeService) {
        this.pizzaService = pizzaService;
        this.categoryService = categoryService;
        this.pizzaMapper = pizzaMapper;
        this.pizzaIngredientService = pizzaIngredientService;
        this.ingredientService = ingredientService;
        this.sizeService = sizeService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PizzaDTO>>> getAllPizzas() {
        List<Pizza> pizzas = pizzaService.listarPizzas();
        List<PizzaDTO> dtos = pizzaMapper.toDtoList(pizzas);

        Map<Integer, Ingredient> ingredientMap = ingredientService.listarIngredientes().stream()
                .collect(Collectors.toMap(Ingredient::getId, Function.identity(), (a, b) -> a));

        Map<Integer, List<PizzaIngredient>> recipesByPizza = new HashMap<>();
        for (PizzaIngredient pi : pizzaIngredientService.listarPizzaIngredientes()) {
            Integer pId = pi.getId() != null ? pi.getId().getPizzaId() : null;
            if (pId != null) {
                recipesByPizza.computeIfAbsent(pId, k -> new ArrayList<>()).add(pi);
            }
        }

        for (PizzaDTO dto : dtos) {
            enrichPizzaDto(dto, recipesByPizza.getOrDefault(dto.getId(), Collections.emptyList()), ingredientMap);
        }

        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PizzaDTO>> getPizzaById(@PathVariable("id") Integer id) {
        Pizza pizza = pizzaService.obtenerPorId(id)
            .orElseThrow(() -> new ResourceNotFoundException(MSG_NOT_FOUND_PREFIX + id));
        PizzaDTO dto = pizzaMapper.toDto(pizza);
        Map<Integer, Ingredient> ingredientMap = ingredientService.listarIngredientes().stream()
                .collect(Collectors.toMap(Ingredient::getId, Function.identity(), (a, b) -> a));
        enrichPizzaDto(dto, pizzaIngredientService.listarPorPizzaId(id), ingredientMap);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @GetMapping("/{id}/recipe")
    public ResponseEntity<ApiResponse<List<PizzaIngredientDTO>>> getPizzaRecipe(@PathVariable("id") Integer id) {
        pizzaService.obtenerPorId(id)
            .orElseThrow(() -> new ResourceNotFoundException(MSG_NOT_FOUND_PREFIX + id));
        Map<Integer, Ingredient> ingredientMap = ingredientService.listarIngredientes().stream()
                .collect(Collectors.toMap(Ingredient::getId, Function.identity(), (a, b) -> a));
        List<PizzaIngredientDTO> recipe = buildRecipeDtos(id, pizzaIngredientService.listarPorPizzaId(id), ingredientMap);
        return ResponseEntity.ok(ApiResponse.success(recipe));
    }

    @PutMapping("/{id}/recipe")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PizzaDTO>> updatePizzaRecipe(
            @PathVariable("id") Integer id,
            @RequestBody List<PizzaIngredientDTO> recipeItems) {
        Pizza pizza = pizzaService.obtenerPorId(id)
            .orElseThrow(() -> new ResourceNotFoundException(MSG_NOT_FOUND_PREFIX + id));

        savePizzaRecipe(pizza, recipeItems);

        Map<Integer, Ingredient> ingredientMap = ingredientService.listarIngredientes().stream()
                .collect(Collectors.toMap(Ingredient::getId, Function.identity(), (a, b) -> a));
        PizzaDTO dto = pizzaMapper.toDto(pizza);
        enrichPizzaDto(dto, pizzaIngredientService.listarPorPizzaId(id), ingredientMap);
        return ResponseEntity.ok(ApiResponse.success(dto, "Receta de la pizza actualizada con éxito"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PizzaDTO>> createPizza(@RequestBody PizzaCreateDTO pizzaDTO) {
        if (pizzaDTO.getName() == null || pizzaDTO.getName().isBlank()) {
            throw new BadRequestException("El nombre de la pizza es obligatorio");
        }
        if (pizzaDTO.getCategoryId() == null) {
            throw new BadRequestException("La categoría de la pizza es obligatoria");
        }
        if (pizzaService.existsByName(pizzaDTO.getName())) {
            throw new BadRequestException("El nombre de la pizza ya existe");
        }

        Category category = categoryService.obtenerPorId(pizzaDTO.getCategoryId())
            .orElseThrow(() -> new BadRequestException("Categoría no encontrada"));

        Pizza pizza = pizzaMapper.toEntity(pizzaDTO);
        pizza.setCategory(category);

        Pizza nuevaPizza = pizzaService.crearPizza(pizza);
        if (pizzaDTO.getRecipe() != null) {
            savePizzaRecipe(nuevaPizza, pizzaDTO.getRecipe());
        }

        Map<Integer, Ingredient> ingredientMap = ingredientService.listarIngredientes().stream()
                .collect(Collectors.toMap(Ingredient::getId, Function.identity(), (a, b) -> a));
        PizzaDTO responseDto = pizzaMapper.toDto(nuevaPizza);
        enrichPizzaDto(responseDto, pizzaIngredientService.listarPorPizzaId(nuevaPizza.getId()), ingredientMap);

        return new ResponseEntity<>(ApiResponse.success(responseDto, "Pizza creada con éxito"), HttpStatus.CREATED);
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

        if (pizzaDTO.getRecipe() != null) {
            savePizzaRecipe(pizzaActualizada, pizzaDTO.getRecipe());
        }

        Map<Integer, Ingredient> ingredientMap = ingredientService.listarIngredientes().stream()
                .collect(Collectors.toMap(Ingredient::getId, Function.identity(), (a, b) -> a));
        PizzaDTO responseDto = pizzaMapper.toDto(pizzaActualizada);
        enrichPizzaDto(responseDto, pizzaIngredientService.listarPorPizzaId(id), ingredientMap);

        return ResponseEntity.ok(ApiResponse.success(responseDto, "Pizza actualizada con éxito"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePizza(@PathVariable("id") Integer id) {
        if (!pizzaService.eliminarPizza(id)) {
            throw new ResourceNotFoundException(MSG_NOT_FOUND_PREFIX + id);
        }
        return ResponseEntity.ok(ApiResponse.success(null, "Pizza eliminada correctamente"));
    }

    private void savePizzaRecipe(Pizza pizza, List<PizzaIngredientDTO> recipeDtos) {
        pizzaIngredientService.eliminarRecetaDePizza(pizza.getId());
        if (recipeDtos == null || recipeDtos.isEmpty()) {
            return;
        }
        Map<Integer, BigDecimal> mergedQuantities = new HashMap<>();
        Map<Integer, Boolean> defaultFlags = new HashMap<>();
        for (PizzaIngredientDTO item : recipeDtos) {
            if (item.getIngredientId() == null) continue;
            BigDecimal qty = item.getQuantity() != null && item.getQuantity().compareTo(BigDecimal.ZERO) > 0
                    ? item.getQuantity()
                    : new BigDecimal("50.00");
            mergedQuantities.merge(item.getIngredientId(), qty, BigDecimal::add);
            defaultFlags.put(item.getIngredientId(), item.getIsDefault());
        }

        for (Map.Entry<Integer, BigDecimal> entry : mergedQuantities.entrySet()) {
            Ingredient ing = ingredientService.obtenerPorId(entry.getKey())
                    .orElseThrow(() -> new BadRequestException("Ingrediente no encontrado: " + entry.getKey()));
            PizzaIngredient pi = new PizzaIngredient();
            pi.setId(new PizzaIngredientId(pizza.getId(), ing.getId()));
            pi.setPizza(pizza);
            pi.setIngredient(ing);
            pi.setIsDefault(defaultFlags.getOrDefault(ing.getId(), true));
            pi.setQuantity(entry.getValue());
            pizzaIngredientService.crearPizzaIngrediente(pi);
        }
    }

    private List<PizzaIngredientDTO> buildRecipeDtos(Integer pizzaId, List<PizzaIngredient> recipeList, Map<Integer, Ingredient> ingredientMap) {
        List<PizzaIngredientDTO> result = new ArrayList<>();
        for (PizzaIngredient pi : recipeList) {
            Integer ingId = pi.getId() != null ? pi.getId().getIngredientId() : null;
            Ingredient ing = ingId != null ? ingredientMap.get(ingId) : pi.getIngredient();
            PizzaIngredientDTO itemDto = new PizzaIngredientDTO();
            itemDto.setPizzaId(pizzaId);
            itemDto.setIngredientId(ingId);
            itemDto.setIsDefault(pi.getIsDefault());
            BigDecimal qty = pi.getQuantity() != null && pi.getQuantity().compareTo(BigDecimal.ZERO) > 0
                    ? pi.getQuantity()
                    : new BigDecimal("50.00");
            itemDto.setQuantity(qty);
            if (ing != null) {
                itemDto.setIngredientName(ing.getName());
                itemDto.setUnit(ing.getUnit() != null ? ing.getUnit() : "g");
                BigDecimal uCost = ing.getUnitCost() != null ? ing.getUnitCost() : BigDecimal.ZERO;
                itemDto.setUnitCost(uCost);
                itemDto.setStockQuantity(ing.getStockQuantity() != null ? ing.getStockQuantity() : BigDecimal.ZERO);
                itemDto.setLineCost(qty.multiply(uCost).setScale(2, RoundingMode.HALF_UP));
            } else {
                itemDto.setUnit("g");
                itemDto.setUnitCost(BigDecimal.ZERO);
                itemDto.setStockQuantity(BigDecimal.ZERO);
                itemDto.setLineCost(BigDecimal.ZERO);
            }
            result.add(itemDto);
        }
        return result;
    }

    private void enrichPizzaDto(PizzaDTO dto, List<PizzaIngredient> recipeList, Map<Integer, Ingredient> ingredientMap) {
        List<PizzaIngredientDTO> recipeDtos = buildRecipeDtos(dto.getId(), recipeList, ingredientMap);
        dto.setRecipe(recipeDtos);
        dto.setRecipeCount(recipeDtos.size());

        List<String> visibleIngredientNames = new ArrayList<>();
        BigDecimal totalCost = BigDecimal.ZERO;
        int minProducible = recipeDtos.isEmpty() ? 0 : Integer.MAX_VALUE;

        for (PizzaIngredientDTO item : recipeDtos) {
            Ingredient ing = item.getIngredientId() != null ? ingredientMap.get(item.getIngredientId()) : null;
            if (item.getIngredientName() != null && !item.getIngredientName().equalsIgnoreCase("Masa Fresca")) {
                visibleIngredientNames.add(item.getIngredientName());
            }
            BigDecimal qty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
            BigDecimal uCost = item.getUnitCost() != null ? item.getUnitCost() : BigDecimal.ZERO;
            totalCost = totalCost.add(qty.multiply(uCost));

            if (ing == null || !ing.isAvailable() || ing.getDeletedAt() != null || qty.compareTo(BigDecimal.ZERO) <= 0) {
                minProducible = 0;
            } else {
                BigDecimal stock = ing.getStockQuantity() != null ? ing.getStockQuantity() : BigDecimal.ZERO;
                int possible = stock.divide(qty, 0, RoundingMode.FLOOR).intValue();
                minProducible = Math.min(minProducible, Math.max(0, possible));
            }
        }

        if (minProducible == Integer.MAX_VALUE) {
            minProducible = 0;
        }

        int totalDoughAvailable = sizeService.listarSizes().stream()
                .mapToInt(s -> s.getStockQuantity() != null ? s.getStockQuantity() : 0)
                .sum();

        int effectiveStock = (minProducible > 0 && totalDoughAvailable > 0)
                ? Math.min(minProducible, totalDoughAvailable)
                : 0;

        BigDecimal roundedCost = totalCost.setScale(2, RoundingMode.HALF_UP);
        dto.setIngredients(visibleIngredientNames);
        dto.setProductionCost(roundedCost);
        dto.setAvailableStock(effectiveStock);
        dto.setOutOfStock(!dto.getIsAvailable() || effectiveStock <= 0);
        dto.setLowStock(!dto.isOutOfStock() && effectiveStock <= 10);

        if (dto.getPrice() != null && dto.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal margin = dto.getPrice().subtract(roundedCost)
                    .multiply(new BigDecimal("100"))
                    .divide(dto.getPrice(), 2, RoundingMode.HALF_UP);
            dto.setProfitMargin(margin);
        } else {
            dto.setProfitMargin(BigDecimal.ZERO);
        }
    }
}
