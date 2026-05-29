package com.example.infrastructure.persistence.repositories;

import com.example.domain.model.Category;
import com.example.domain.repository.CategoryRepository;
import com.example.infrastructure.persistence.entities.CategoryEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@SuppressWarnings("null")
public class JpaCategoryRepository implements CategoryRepository {

    private final SpringDataCategoryRepository springDataRepository;

    @Override
    public List<Category> findAll() {
        return springDataRepository.findAll()
                .stream()
                .map(CategoryEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Category> findById(Integer id) {
        return springDataRepository.findById(id)
                .map(CategoryEntity::toDomain);
    }

    @Override
    public boolean existsById(Integer id) {
        return springDataRepository.existsById(id);
    }

    @Override
    public boolean existsByName(String name) {
        return springDataRepository.existsByName(name);
    }

    @Override
    public Category save(Category category) {
        CategoryEntity entity = CategoryEntity.fromDomain(category);
        CategoryEntity saved = springDataRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public void deleteById(Integer id) {
        springDataRepository.deleteById(id);
    }
}