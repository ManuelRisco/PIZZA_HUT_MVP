package com.example.domain.repository;

import com.example.domain.model.Size;
import java.util.List;
import java.util.Optional;

public interface SizeRepository {
    List<Size> findAll();
    Optional<Size> findById(Integer id);
    boolean existsById(Integer id);
    boolean existsByName(String name);
    Size save(Size size);
    void deleteById(Integer id);
}
