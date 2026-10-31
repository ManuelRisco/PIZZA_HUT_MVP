package com.example.repositories;

import com.example.models.Size;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SizeRepository extends JpaRepository<Size, Integer> {
    boolean existsByNameAndDeletedAtIsNull(String name);
    List<Size> findByDeletedAtIsNull();
    Optional<Size> findByName(String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Size s WHERE s.id = :id")
    Optional<Size> findByIdWithLock(@Param("id") Integer id);
}
