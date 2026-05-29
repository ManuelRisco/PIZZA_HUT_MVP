package com.example.infrastructure.persistence.repositories;

import com.example.domain.model.Usuario;
import com.example.infrastructure.persistence.entities.UsuarioEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@SuppressWarnings("null")
public class JpaUsuarioRepository {

    private final SpringDataUsuarioRepository springDataRepository;

    public List<Usuario> findAll() {
        return springDataRepository.findAll()
                .stream()
                .map(UsuarioEntity::toDomain)
                .collect(Collectors.toList());
    }

    public Optional<Usuario> findById(Long id) {
        return springDataRepository.findById(id)
                .map(UsuarioEntity::toDomain);
    }

    public Optional<Usuario> findByEmail(String email) {
        return springDataRepository.findByEmail(email)
                .map(UsuarioEntity::toDomain);
    }

    public boolean existsByEmail(String email) {
        return springDataRepository.existsByEmail(email);
    }

    public Usuario save(Usuario usuario) {
        UsuarioEntity entity = UsuarioEntity.fromDomain(usuario);
        UsuarioEntity savedEntity = springDataRepository.save(entity);
        return savedEntity.toDomain();
    }

    public void deleteById(Long id) {
        springDataRepository.deleteById(id);
    }
}