package com.example.controllers;

import com.example.dtos.ApiResponse;
import com.example.mappers.CategoryMapper;
import com.example.dtos.CategoryDTO;
import com.example.models.Category;
import com.example.services.CategoryService;
import com.example.exceptions.BadRequestException;
import com.example.exceptions.ResourceNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "http://localhost:4200")
public class CategoryController {

    private final CategoryService categoryService;
    private final CategoryMapper categoryMapper;

    public CategoryController(CategoryService categoryService, CategoryMapper categoryMapper) {
        this.categoryService = categoryService;
        this.categoryMapper = categoryMapper;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryDTO>>> getAllCategories() {
        List<Category> categorias = categoryService.listarCategorias();
        return ResponseEntity.ok(ApiResponse.success(categoryMapper.toDtoList(categorias)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryDTO>> getCategoryById(@PathVariable("id") Integer id) {
        Category category = categoryService.obtenerPorId(id)
            .orElseThrow(() -> new ResourceNotFoundException("Categor\u00eda no encontrada con id: " + id));
        return ResponseEntity.ok(ApiResponse.success(categoryMapper.toDto(category)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryDTO>> createCategory(@RequestBody CategoryDTO categoryDTO) {
        if (categoryService.existsByName(categoryDTO.getName())) {
            throw new BadRequestException("El nombre de la categor\u00eda ya existe");
        }
        
        Category category = categoryMapper.toEntity(categoryDTO);
        Category nuevaCategoria = categoryService.crearCategoria(category);
        return new ResponseEntity<>(ApiResponse.success(categoryMapper.toDto(nuevaCategoria), "Categor\u00eda creada con \u00e9xito"), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryDTO>> updateCategory(@PathVariable("id") Integer id, @RequestBody CategoryDTO categoryDTO) {
        Category categoryExistente = categoryService.obtenerPorId(id)
            .orElseThrow(() -> new ResourceNotFoundException("Categor\u00eda no encontrada con id: " + id));
            
        categoryMapper.updateEntityFromDto(categoryDTO, categoryExistente);
        Category categoryActualizada = categoryService.actualizarCategoria(id, categoryExistente);
        
        return ResponseEntity.ok(ApiResponse.success(categoryMapper.toDto(categoryActualizada), "Categor\u00eda actualizada con \u00e9xito"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable("id") Integer id) {
        if (!categoryService.eliminarCategoria(id)) {
            throw new ResourceNotFoundException("Categor\u00eda no encontrada con id: " + id);
        }
        return ResponseEntity.ok(ApiResponse.success(null, "Categor\u00eda eliminada correctamente"));
    }
}
