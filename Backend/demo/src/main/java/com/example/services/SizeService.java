package com.example.services;

import com.example.models.Size;
import com.example.repositories.SizeRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SizeService {

    private final SizeRepository sizeRepository;

    public SizeService(SizeRepository sizeRepository) {
        this.sizeRepository = sizeRepository;
    }

    @Cacheable("sizes")
    public List<Size> listarSizes() {
        return sizeRepository.findByDeletedAtIsNull();
    }

    public List<Size> listarSizes(boolean includeDeleted) {
        if (includeDeleted) {
            return sizeRepository.findAll();
        }
        return sizeRepository.findByDeletedAtIsNull();
    }

    public Optional<Size> obtenerPorId(Integer id) {
        return sizeRepository.findById(id);
    }

    @CacheEvict(value = { "sizes", "pizzas" }, allEntries = true)
    public Size crearSize(Size size) {
        if (sizeRepository.existsByNameAndDeletedAtIsNull(size.getName())) {
            throw new IllegalArgumentException("Ya existe un tamaño con ese nombre");
        }
        if (size.getExtraCost() != null && size.getExtraCost().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El multiplicador de precio no puede ser negativo.");
        }
        if (size.getRecipeMultiplier() == null || size.getRecipeMultiplier().compareTo(BigDecimal.ZERO) <= 0) {
            size.setRecipeMultiplier(BigDecimal.ONE);
        }

        Optional<Size> softDeleted = sizeRepository.findByName(size.getName());
        if (softDeleted.isPresent()) {
            Size existing = softDeleted.get();
            existing.setDeletedAt(null);
            existing.setExtraCost(size.getExtraCost());
            existing.setRecipeMultiplier(size.getRecipeMultiplier());
            existing.setStockQuantity(size.getStockQuantity() != null ? size.getStockQuantity() : 0);
            existing.setMinStock(size.getMinStock() != null ? size.getMinStock() : 0);
            existing.setDescription(size.getDescription());
            existing.setDisplayOrder(size.getDisplayOrder());
            return sizeRepository.save(existing);
        }

        if (size.getStockQuantity() == null) size.setStockQuantity(0);
        if (size.getMinStock() == null) size.setMinStock(0);
        return sizeRepository.save(size);
    }

    @CacheEvict(value = { "sizes", "pizzas" }, allEntries = true)
    public Size actualizarSize(Integer id, Size size) {
        Size existingSize = sizeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Size no encontrado"));

        if (!existingSize.getName().equals(size.getName())
                && sizeRepository.existsByNameAndDeletedAtIsNull(size.getName())) {
            throw new IllegalArgumentException("Ya existe un tamaño con ese nombre");
        }

        if (size.getExtraCost() != null && size.getExtraCost().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El multiplicador de precio no puede ser negativo.");
        }

        existingSize.setName(size.getName());
        existingSize.setExtraCost(size.getExtraCost());
        if (size.getRecipeMultiplier() != null && size.getRecipeMultiplier().compareTo(BigDecimal.ZERO) > 0) {
            existingSize.setRecipeMultiplier(size.getRecipeMultiplier());
        }
        if (size.getStockQuantity() != null) {
            existingSize.setStockQuantity(size.getStockQuantity());
        }
        if (size.getMinStock() != null) {
            existingSize.setMinStock(size.getMinStock());
        }
        existingSize.setDescription(size.getDescription());
        existingSize.setDisplayOrder(size.getDisplayOrder());
        if (size.getDeletedAt() != null) {
            existingSize.setDeletedAt(size.getDeletedAt());
        }

        return sizeRepository.save(existingSize);
    }

    @CacheEvict(value = { "sizes", "pizzas" }, allEntries = true)
    public void eliminarSize(Integer id) {
        Optional<Size> sizeOpt = sizeRepository.findById(id);
        if (sizeOpt.isEmpty()) {
            throw new IllegalArgumentException("Size no encontrado");
        }
        Size size = sizeOpt.get();
        size.setDeletedAt(LocalDateTime.now());
        sizeRepository.save(size);
    }

    @CacheEvict(value = { "sizes", "pizzas" }, allEntries = true)
    public boolean restaurarSize(Integer id) {
        Optional<Size> sizeOpt = sizeRepository.findById(id);
        if (sizeOpt.isEmpty()) {
            return false;
        }
        Size size = sizeOpt.get();
        size.setDeletedAt(null);
        sizeRepository.save(size);
        return true;
    }
}
