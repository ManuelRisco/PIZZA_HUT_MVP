package com.example.models;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PizzaIngredientId implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer pizzaId;
    private Integer ingredientId;
}
