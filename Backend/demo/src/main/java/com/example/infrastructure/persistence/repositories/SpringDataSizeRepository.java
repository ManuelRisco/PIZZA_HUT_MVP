package com.example.infrastructure.persistence.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.infrastructure.persistence.entities.SizeEntity;

public interface SpringDataSizeRepository extends JpaRepository<SizeEntity, Integer> {
    boolean existsByName(String name);
}
