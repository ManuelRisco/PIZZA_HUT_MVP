package com.example.infrastructure.persistence.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.example.infrastructure.persistence.entities.FavoriteEntity;
import com.example.infrastructure.persistence.entities.FavoriteIdEntity;
import java.util.List;

public interface SpringDataFavoriteRepository extends JpaRepository<FavoriteEntity, FavoriteIdEntity> {
    @Query("SELECT f FROM FavoriteEntity f WHERE f.id.userId = :userId")
    List<FavoriteEntity> findByUserId(@Param("userId") Integer userId);
    
    @Query("SELECT f FROM FavoriteEntity f WHERE f.id.pizzaId = :pizzaId")
    List<FavoriteEntity> findByPizzaId(@Param("pizzaId") Integer pizzaId);
}
