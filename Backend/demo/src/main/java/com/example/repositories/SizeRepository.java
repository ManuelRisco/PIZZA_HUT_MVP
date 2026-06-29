package com.example.repositories;

import com.example.models.Size;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SizeRepository extends JpaRepository<Size, Integer> {
    boolean existsByNameAndDeletedAtIsNull(String name);
    List<Size> findByDeletedAtIsNull();
    Optional<Size> findByName(String name);
}


