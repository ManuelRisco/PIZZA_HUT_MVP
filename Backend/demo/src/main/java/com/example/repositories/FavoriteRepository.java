package com.example.repositories;

import com.example.models.Favorite;
import com.example.models.FavoriteId;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteRepository extends JpaRepository<Favorite, FavoriteId> {
List<Favorite> findByIdUserId(Integer userId);

    List<Favorite> findByIdPizzaId(Integer pizzaId);
}


