package com.example.application.pizzas.estructurales;

import java.math.BigDecimal;
import com.example.domain.model.Ingredient;
import com.example.domain.repository.IngredientRepository;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Patrón Decorator - Decorador genérico para cualquier ingrediente extra
 * Obtiene el precio dinámicamente desde la base de datos
 * NOTA: No es un @Component porque se instancia manualmente con new
 */
public class IngredientDecorator extends PizzaDecorator {
    
    private static IngredientRepository ingredientRepository;
    private final String ingredientName;
    private final BigDecimal extraCost;

    @Autowired
    public static void setIngredientRepository(IngredientRepository repository) {
        IngredientDecorator.ingredientRepository = repository;
    }

    /**
     * Constructor que recibe el nombre del ingrediente
     * @param pizza Pizza base o decorada
     * @param ingredientName Nombre del ingrediente a buscar en la BD
     */
    public IngredientDecorator(PizzaComponent pizza, String ingredientName) {
        super(pizza);
        this.ingredientName = ingredientName;
        this.extraCost = getIngredientCost(ingredientName);
    }

    /**
     * Busca el costo del ingrediente en la base de datos
     * @param ingredientName Nombre del ingrediente
     * @return Costo extra del ingrediente o 0.00 si no se encuentra
     */
    private BigDecimal getIngredientCost(String ingredientName) {
        if (ingredientRepository != null) {
            return ingredientRepository.findAll().stream()
                    .filter(ing -> ing.getName().equalsIgnoreCase(ingredientName))
                    .findFirst()
                    .map(Ingredient::getExtraCost)
                    .orElse(BigDecimal.ZERO); // Retorna 0 si no se encuentra
        }
        return BigDecimal.ZERO;
    }

    @Override
    public String getDescription() {
        return pizza.getDescription() + " + " + ingredientName;
    }

    @Override
    public BigDecimal getPrice() {
        return pizza.getPrice().add(extraCost);
    }

    /**
     * Obtiene el nombre del ingrediente
     * @return Nombre del ingrediente
     */
    public String getIngredientName() {
        return ingredientName;
    }

    /**
     * Obtiene el costo extra del ingrediente
     * @return Costo extra
     */
    public BigDecimal getExtraCost() {
        return extraCost;
    }
}
