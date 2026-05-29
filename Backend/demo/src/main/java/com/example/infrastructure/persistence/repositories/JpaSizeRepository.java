package com.example.infrastructure.persistence.repositories;

import com.example.domain.model.Size;
import com.example.domain.repository.SizeRepository;
import com.example.infrastructure.persistence.entities.SizeEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@SuppressWarnings("null")
public class JpaSizeRepository implements SizeRepository {

    private final SpringDataSizeRepository springDataRepository;

    @Override
    public List<Size> findAll() {
        return springDataRepository.findAll()
                .stream()
                .map(SizeEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Size> findById(Integer id) {
        return springDataRepository.findById(id)
                .map(SizeEntity::toDomain);
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
    public Size save(Size size) {
        SizeEntity entity = SizeEntity.fromDomain(size);
        SizeEntity saved = springDataRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public void deleteById(Integer id) {
        springDataRepository.deleteById(id);
    }
}
