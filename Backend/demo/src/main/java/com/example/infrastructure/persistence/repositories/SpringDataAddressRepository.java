package com.example.infrastructure.persistence.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.infrastructure.persistence.entities.AddressEntity;
import java.util.List;

public interface SpringDataAddressRepository extends JpaRepository<AddressEntity, Integer> {
    List<AddressEntity> findByUserId(Integer userId);
}
