package com.example.domain.dto;

import java.util.List;
import java.util.stream.Collectors;

import com.example.domain.model.*;

public class DTOMapper {

    // Mapear Usuario a UsuarioDTO
    public static UsuarioDTO toUsuarioDTO(Usuario usuario) {
        return new UsuarioDTO(usuario);
    }

    // Mapear UsuarioCreateDTO a Usuario
    public static Usuario toUsuario(UsuarioCreateDTO usuarioDTO) {
        Usuario usuario = new Usuario();
        usuario.setEmail(usuarioDTO.getEmail());
        usuario.setPasswordHash(usuarioDTO.getPassword());
        usuario.setName(usuarioDTO.getName());
        usuario.setPhone(usuarioDTO.getPhone());
        usuario.setRole(usuarioDTO.getRole() != null ? usuarioDTO.getRole() : Usuario.Role.CUSTOMER);
        return usuario;
    }

    // Mapear Pizza a PizzaDTO
    public static PizzaDTO toPizzaDTO(Pizza pizza) {
        PizzaDTO dto = new PizzaDTO(pizza);
        // Agregar ingredientes si están disponibles
        if (pizza.getPizzaIngredients() != null) {
            List<String> ingredientes = pizza.getPizzaIngredients().stream()
                .map(pi -> pi.getIngredient().getName())
                .collect(Collectors.toList());
            dto.setIngredients(ingredientes);
        }
        return dto;
    }

    // Mapear PizzaCreateDTO a Pizza (necesita Category)
    public static Pizza toPizza(PizzaCreateDTO pizzaDTO, Category category) {
        Pizza pizza = new Pizza();
        pizza.setCategory(category);
        pizza.setName(pizzaDTO.getName());
        pizza.setDescription(pizzaDTO.getDescription());
        pizza.setImageUrl(pizzaDTO.getImageUrl());
        pizza.setPrice(pizzaDTO.getPrice());
        pizza.setIsAvailable(pizzaDTO.isAvailable());
        pizza.setIsPopular(pizzaDTO.isPopular());
        return pizza;
    }

    // Mapear Category a CategoryDTO
    public static CategoryDTO toCategoryDTO(Category category) {
        return new CategoryDTO(category);
    }

    // Mapear CategoryDTO a Category
    public static Category toCategory(CategoryDTO categoryDTO) {
        Category category = new Category();
        category.setName(categoryDTO.getName());
        category.setDescription(categoryDTO.getDescription());
        category.setImageUrl(categoryDTO.getImageUrl());
        category.setDisplayOrder(categoryDTO.getDisplayOrder());
        return category;
    }

    // Mapear Ingredient a IngredientDTO
    public static IngredientDTO toIngredientDTO(Ingredient ingredient) {
        return new IngredientDTO(ingredient);
    }

    // Mapear IngredientDTO a Ingredient
    public static Ingredient toIngredient(IngredientDTO ingredientDTO) {
        Ingredient ingredient = new Ingredient();
        ingredient.setName(ingredientDTO.getName());
        ingredient.setExtraCost(ingredientDTO.getExtraCost());
        ingredient.setAvailable(ingredientDTO.isAvailable());
        return ingredient;
    }

    // Mapear listas
    public static List<UsuarioDTO> toUsuarioDTOList(List<Usuario> usuarios) {
        return usuarios.stream().map(DTOMapper::toUsuarioDTO).collect(Collectors.toList());
    }

    public static List<PizzaDTO> toPizzaDTOList(List<Pizza> pizzas) {
        return pizzas.stream().map(DTOMapper::toPizzaDTO).collect(Collectors.toList());
    }

    public static List<CategoryDTO> toCategoryDTOList(List<Category> categories) {
        return categories.stream().map(DTOMapper::toCategoryDTO).collect(Collectors.toList());
    }

    public static List<IngredientDTO> toIngredientDTOList(List<Ingredient> ingredients) {
        return ingredients.stream().map(DTOMapper::toIngredientDTO).collect(Collectors.toList());
    }
}