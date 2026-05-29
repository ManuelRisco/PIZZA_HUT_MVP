package com.example.infrastructure.persistence.repositories;

import com.example.domain.model.Favorite;
import com.example.domain.model.FavoriteId;
import com.example.domain.repository.FavoriteRepository;
import com.example.infrastructure.persistence.entities.FavoriteEntity;
import com.example.infrastructure.persistence.entities.FavoriteIdEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@SuppressWarnings("null")
public class JpaFavoriteRepository implements FavoriteRepository {

    private final SpringDataFavoriteRepository springDataRepository;

    @Override
    public List<Favorite> findAll() {
        return springDataRepository.findAll()
                .stream()
                .map(FavoriteEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Favorite> findById(FavoriteId id) {
        FavoriteIdEntity entityId = new FavoriteIdEntity(id.getUserId(), id.getPizzaId());
        return springDataRepository.findById(entityId)
                .map(FavoriteEntity::toDomain);
    }

    @Override
    public List<Favorite> findByUserId(Integer userId) {
        return springDataRepository.findByUserId(userId)
                .stream()
                .map(FavoriteEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Favorite> findByPizzaId(Integer pizzaId) {
        return springDataRepository.findByPizzaId(pizzaId)
                .stream()
                .map(FavoriteEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsById(FavoriteId id) {
        FavoriteIdEntity entityId = new FavoriteIdEntity(id.getUserId(), id.getPizzaId());
        return springDataRepository.existsById(entityId);
    }

    @Override
    public Favorite save(Favorite favorite) {
        FavoriteEntity entity = FavoriteEntity.fromDomain(favorite);
        FavoriteEntity saved = springDataRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public void deleteById(FavoriteId id) {
        FavoriteIdEntity entityId = new FavoriteIdEntity(id.getUserId(), id.getPizzaId());
        springDataRepository.deleteById(entityId);
    }
}
