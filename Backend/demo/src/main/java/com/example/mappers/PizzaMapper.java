package com.example.mappers;

import com.example.dtos.PizzaCreateDTO;
import com.example.dtos.PizzaDTO;
import com.example.models.Pizza;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PizzaMapper {

    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    @Mapping(target = "ingredients", ignore = true)
    @Mapping(target = "recipe", ignore = true)
    PizzaDTO toDto(Pizza pizza);

    @Mapping(source = "categoryId", target = "category.id")
    Pizza toEntity(PizzaCreateDTO pizzaCreateDTO);

    List<PizzaDTO> toDtoList(List<Pizza> pizzas);

    @Mapping(source = "categoryId", target = "category.id")
    void updateEntityFromDto(PizzaCreateDTO pizzaDTO, @MappingTarget Pizza pizza);
}
