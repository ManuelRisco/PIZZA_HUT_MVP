package com.example.domain.repository;

import com.example.domain.model.Extra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExtraRepository extends JpaRepository<Extra, Integer> {
    
    // Buscar todos los extras disponibles
    List<Extra> findByIsAvailableTrueAndDeletedAtIsNull();
    
    // Buscar por categoría
    List<Extra> findByCategoryAndIsAvailableTrueAndDeletedAtIsNull(Extra.ExtraCategory category);
    
    // Buscar por categoría (incluyendo no disponibles)
    List<Extra> findByCategoryAndDeletedAtIsNull(Extra.ExtraCategory category);
    
    // Buscar por nombre
    List<Extra> findByNameContainingIgnoreCaseAndDeletedAtIsNull(String name);
    
    // Verificar si existe por nombre
    boolean existsByName(String name);
    
    // Obtener todos ordenados por displayOrder
    List<Extra> findByDeletedAtIsNullOrderByDisplayOrderAsc();
    
    // Obtener por categoría ordenados
    List<Extra> findByCategoryAndDeletedAtIsNullOrderByDisplayOrderAsc(Extra.ExtraCategory category);
}
