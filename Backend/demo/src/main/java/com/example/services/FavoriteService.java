package com.example.services;

import com.example.models.Favorite;
import com.example.models.FavoriteId;
import com.example.repositories.FavoriteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;

    public FavoriteService(FavoriteRepository favoriteRepository) {
        this.favoriteRepository = favoriteRepository;
    }

    public List<Favorite> listarFavorites() {
        return favoriteRepository.findAll();
    }

    @SuppressWarnings("null")
    public Optional<Favorite> obtenerPorId(FavoriteId id) {
        return favoriteRepository.findById(id);
    }

    public List<Favorite> obtenerPorUserId(Integer userId) {
        return favoriteRepository.findByIdUserId(userId);
    }

    public List<Favorite> obtenerPorPizzaId(Integer pizzaId) {
        return favoriteRepository.findByIdPizzaId(pizzaId);
    }

    @SuppressWarnings("null")
    public Favorite crearFavorite(Favorite favorite) {
        if (favoriteRepository.existsById(favorite.getId())) {
            throw new IllegalArgumentException("Este favorito ya existe");
        }
        return favoriteRepository.save(favorite);
    }

    @SuppressWarnings("null")
    public void eliminarFavorite(FavoriteId id) {
        if (!favoriteRepository.existsById(id)) {
            throw new IllegalArgumentException("Favorite no encontrado");
        }
        favoriteRepository.deleteById(id);
    }
}
