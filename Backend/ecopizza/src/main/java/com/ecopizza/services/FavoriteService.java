package com.ecopizza.services;

import com.ecopizza.models.Favorite;
import com.ecopizza.models.FavoriteId;
import com.ecopizza.repositories.FavoriteRepository;
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

    public Optional<Favorite> obtenerPorId(FavoriteId id) {
        return favoriteRepository.findById(id);
    }

    public List<Favorite> obtenerPorUserId(Integer userId) {
        return favoriteRepository.findByIdUserId(userId);
    }

    public List<Favorite> obtenerPorPizzaId(Integer pizzaId) {
        return favoriteRepository.findByIdPizzaId(pizzaId);
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
