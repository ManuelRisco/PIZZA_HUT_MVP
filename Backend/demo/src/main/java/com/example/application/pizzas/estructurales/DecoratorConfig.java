package com.example.application.pizzas.estructurales;

import com.example.domain.repository.IngredientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

/**
 * Configuración para inyectar el repositorio en los decoradores
 */
@Configuration
public class DecoratorConfig {

    @Autowired
    private IngredientRepository ingredientRepository;

    @PostConstruct
    public void init() {
        // Inyecta el repositorio en el decorador genérico
        IngredientDecorator.setIngredientRepository(ingredientRepository);
    }
}
