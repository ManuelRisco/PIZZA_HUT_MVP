package com.ecopizza.mappers;

import com.ecopizza.dtos.CategoryDTO;
import com.ecopizza.models.Category;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoryMapper {
    CategoryDTO toDto(Category category);
    Category toEntity(CategoryDTO categoryDTO);
    List<CategoryDTO> toDtoList(List<Category> categories);
    void updateEntityFromDto(CategoryDTO categoryDTO, @MappingTarget Category category);
}
