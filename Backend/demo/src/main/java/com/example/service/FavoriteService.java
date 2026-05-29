package com.example.service;

import com.example.domain.model.Favorite;
import com.example.domain.model.FavoriteId;
import com.example.domain.repository.FavoriteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FavoriteService {

    @Autowired
    private FavoriteRepository favoriteRepository;

    public List<Favorite> listarFavorites() {
        return favoriteRepository.findAll();
    }

    public Optional<Favorite> obtenerPorId(FavoriteId id) {
        return favoriteRepository.findById(id);
    }

    public List<Favorite> obtenerPorUserId(Integer userId) {
        return favoriteRepository.findByUserId(userId);
    }

    public List<Favorite> obtenerPorPizzaId(Integer pizzaId) {
        return favoriteRepository.findByPizzaId(pizzaId);
    }

    public Favorite crearFavorite(Favorite favorite) {
        if (favoriteRepository.existsById(favorite.getId())) {
            throw new IllegalArgumentException("Este favorito ya existe");
        }
        return favoriteRepository.save(favorite);
    }

    public void eliminarFavorite(FavoriteId id) {
        if (!favoriteRepository.existsById(id)) {
            throw new IllegalArgumentException("Favorite no encontrado");
        }
        favoriteRepository.deleteById(id);
    }
}
