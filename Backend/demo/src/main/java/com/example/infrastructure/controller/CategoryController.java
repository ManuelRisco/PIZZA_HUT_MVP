package com.example.infrastructure.controller;

import com.example.domain.dto.CategoryDTO;
import com.example.domain.model.Category;
import com.example.service.CategoryService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "http://localhost:4200")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryDTO>> listarCategorias() {
        List<Category> categorias = categoryService.listarCategorias();
        List<CategoryDTO> categoriasDTO = categorias.stream()
            .map(CategoryDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(categoriasDTO);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerCategoriaPorId(@PathVariable Integer id) {
        Optional<Category> categoryOpt = categoryService.obtenerPorId(id);
        if (categoryOpt.isPresent()) {
            CategoryDTO categoryDTO = new CategoryDTO(categoryOpt.get());
            return ResponseEntity.ok(categoryDTO);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Categoría no encontrada"));
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> crearCategoria(@RequestBody CategoryDTO categoryDTO) {
        if (categoryService.existsByName(categoryDTO.getName())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "El nombre de la categoría ya existe"));
        }
        
        // Crear categoría desde DTO
        Category category = new Category();
        category.setName(categoryDTO.getName());
        category.setDescription(categoryDTO.getDescription());
        category.setImageUrl(categoryDTO.getImageUrl());
        category.setDisplayOrder(categoryDTO.getDisplayOrder());
        
        Category nuevaCategoria = categoryService.crearCategoria(category);
        CategoryDTO responseDTO = new CategoryDTO(nuevaCategoria);
        return ResponseEntity.ok(responseDTO);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> actualizarCategoria(@PathVariable Integer id, @RequestBody CategoryDTO categoryDTO) {
        // Crear categoría desde DTO
        Category categoryActualizada = new Category();
        categoryActualizada.setName(categoryDTO.getName());
        categoryActualizada.setDescription(categoryDTO.getDescription());
        categoryActualizada.setImageUrl(categoryDTO.getImageUrl());
        categoryActualizada.setDisplayOrder(categoryDTO.getDisplayOrder());
        
        Category category = categoryService.actualizarCategoria(id, categoryActualizada);
        if (category != null) {
            CategoryDTO responseDTO = new CategoryDTO(category);
            return ResponseEntity.ok(responseDTO);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Categoría no encontrada"));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> eliminarCategoria(@PathVariable Integer id) {
        boolean eliminado = categoryService.eliminarCategoria(id);
        if (eliminado) {
            return ResponseEntity.ok(Map.of("message", "Categoría eliminada correctamente"));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Categoría no encontrada"));
        }
    }
}
