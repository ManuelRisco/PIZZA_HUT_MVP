package com.example.infrastructure.controller;

import com.example.application.pizzas.comportamiento.*;
import com.example.application.pizzas.creacionales.PizzaBuilder;
import com.example.application.pizzas.estructurales.*;
import com.example.domain.model.Pizza;
import com.example.domain.model.Category;
import com.example.domain.repository.CategoryRepository;
import com.example.domain.repository.IngredientRepository;
import com.example.service.PizzaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controlador que demuestra el uso de patrones de diseño para pizzas
 */
@RestController
@RequestMapping("/api/pizzas/patrones")
@CrossOrigin(origins = "http://localhost:4200")
public class PizzaPatronesController {

    @Autowired
    private PizzaService pizzaService;

    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private IngredientRepository ingredientRepository;

    /**
     * SPECIFICATION PATTERN - Listar pizzas disponibles
     */
    @GetMapping("/disponibles")
    public ResponseEntity<List<Pizza>> listarPizzasDisponibles() {
        AvailablePizzaSpecification spec = new AvailablePizzaSpecification();
        List<Pizza> pizzas = pizzaService.listarPizzas().stream()
                .filter(spec::isSatisfiedBy)
                .collect(Collectors.toList());
        return ResponseEntity.ok(pizzas);
    }

    /**
     * SPECIFICATION PATTERN - Listar pizzas populares
     */
    @GetMapping("/populares")
    public ResponseEntity<List<Pizza>> listarPizzasPopulares() {
        PopularPizzaSpecification spec = new PopularPizzaSpecification();
        List<Pizza> pizzas = pizzaService.listarPizzas().stream()
                .filter(spec::isSatisfiedBy)
                .collect(Collectors.toList());
        return ResponseEntity.ok(pizzas);
    }

    /**
     * SPECIFICATION PATTERN - Listar pizzas por categoría
     */
    @GetMapping("/categoria/{categoryId}")
    public ResponseEntity<List<Pizza>> listarPizzasPorCategoria(@PathVariable Integer categoryId) {
        CategoryPizzaSpecification spec = new CategoryPizzaSpecification(categoryId);
        List<Pizza> pizzas = pizzaService.listarPizzas().stream()
                .filter(spec::isSatisfiedBy)
                .collect(Collectors.toList());
        return ResponseEntity.ok(pizzas);
    }

    /**
     * COMPOSITE PATTERN - Listar pizzas que sean disponibles Y populares
     */
    @GetMapping("/disponibles-populares")
    public ResponseEntity<List<Pizza>> listarPizzasDisponiblesYPopulares() {
        PizzaSpecificationComposite composite = PizzaSpecificationComposite.and(
            new AvailablePizzaSpecification(),
            new PopularPizzaSpecification()
        );
        
        List<Pizza> pizzas = pizzaService.listarPizzas().stream()
                .filter(composite::isSatisfiedBy)
                .collect(Collectors.toList());
        return ResponseEntity.ok(pizzas);
    }

    /**
     * DECORATOR PATTERN - Calcular precio con ingredientes extras
     * Ahora usa un decorador genérico que obtiene los precios de la BD
     */
    @PostMapping("/{pizzaId}/extras")
    public ResponseEntity<Map<String, Object>> calcularPrecioConExtras(
            @PathVariable Integer pizzaId,
            @RequestBody Map<String, List<String>> request) {
        
        Pizza pizza = pizzaService.obtenerPorId(pizzaId)
                .orElseThrow(() -> new RuntimeException("Pizza no encontrada"));
        PizzaComponent pizzaComponent = new BasicPizza(pizza);

        List<String> extras = request.get("extras");
        
        if (extras == null || extras.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "descripcion", pizza.getName(),
                "precioBase", pizza.getPrice(),
                "precioFinal", pizza.getPrice(),
                "extras", extras != null ? extras : List.of()
            ));
        }
        
        // Aplicar decorador genérico para cada ingrediente extra
        // Verificar si son IDs (números) o nombres
        for (String extra : extras) {
            try {
                // Intentar parsear como ID
                Integer ingredientId = Integer.parseInt(extra);
                // Buscar ingrediente por ID y usar su nombre
                var ingrediente = ingredientRepository.findById(ingredientId);
                if (ingrediente.isPresent()) {
                    pizzaComponent = new IngredientDecorator(pizzaComponent, ingrediente.get().getName());
                }
            } catch (NumberFormatException e) {
                // Si no es un número, asumir que es el nombre del ingrediente
                pizzaComponent = new IngredientDecorator(pizzaComponent, extra);
            }
        }

        return ResponseEntity.ok(Map.of(
            "descripcion", pizzaComponent.getDescription(),
            "precioBase", pizza.getPrice(),
            "precioFinal", pizzaComponent.getPrice(),
            "extras", extras
        ));
    }

    /**
     * BUILDER PATTERN - Crear pizza personalizada
     */
    @PostMapping("/personalizada")
    public ResponseEntity<Pizza> crearPizzaPersonalizada(@RequestBody Map<String, Object> request) {
        // Obtener categoría
        Integer categoryId = (Integer) request.get("categoryId");
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));

        // Usar Builder Pattern para crear la pizza
        Pizza pizza = PizzaBuilder.builder()
                .withName((String) request.get("name"))
                .withDescription((String) request.get("description"))
                .withCategory(category)
                .withPrice(new BigDecimal(request.get("price").toString()))
                .withImageUrl((String) request.getOrDefault("imageUrl", ""))
                .available((Boolean) request.getOrDefault("available", true))
                .popular((Boolean) request.getOrDefault("popular", false))
                .build();

        Pizza pizzaGuardada = pizzaService.crearPizza(pizza);
        return ResponseEntity.ok(pizzaGuardada);
    }
}
