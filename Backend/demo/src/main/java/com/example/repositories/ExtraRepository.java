package com.example.repositories;

import com.example.models.Extra;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Extra e WHERE e.id = :id")
    Optional<Extra> findByIdWithLock(@Param("id") Integer id);
}
