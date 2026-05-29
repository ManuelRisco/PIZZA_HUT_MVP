package com.example.domain.repository;

import com.example.domain.model.Favorite;
import com.example.domain.model.FavoriteId;
import java.util.List;
import java.util.Optional;

public interface FavoriteRepository {
    List<Favorite> findAll();
    Optional<Favorite> findById(FavoriteId id);
    List<Favorite> findByUserId(Integer userId);
    List<Favorite> findByPizzaId(Integer pizzaId);
    boolean existsById(FavoriteId id);
    Favorite save(Favorite favorite);
    void deleteById(FavoriteId id);
}
