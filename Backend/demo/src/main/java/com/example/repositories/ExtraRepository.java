package com.example.repositories;

import com.example.models.Extra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExtraRepository extends JpaRepository<Extra, Integer> {
    
    // Buscar todos los extras disponibles
    List<Extra> findByIsAvailableTrueAndDeletedAtIsNull();
    
    // Buscar por categor\u00eda
    List<Extra> findByCategoryAndIsAvailableTrueAndDeletedAtIsNull(Extra.ExtraCategory category);
    
    // Buscar por categor\u00eda (incluyendo no disponibles)
    List<Extra> findByCategoryAndDeletedAtIsNull(Extra.ExtraCategory category);
    
    // Buscar por nombre
    List<Extra> findByNameContainingIgnoreCaseAndDeletedAtIsNull(String name);
    
    // Verificar si existe por nombre
    boolean existsByName(String name);
    
    // Obtener todos ordenados por displayOrder
    List<Extra> findByDeletedAtIsNullOrderByDisplayOrderAsc();
    
    // Obtener por categor\u00eda ordenados
    List<Extra> findByCategoryAndDeletedAtIsNullOrderByDisplayOrderAsc(Extra.ExtraCategory category);
}
