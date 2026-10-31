package com.example.services;

import com.example.models.Extra;
import com.example.repositories.ExtraRepository;
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
public class ExtraService {

    private final ExtraRepository extraRepository;

    public ExtraService(ExtraRepository extraRepository) {
        this.extraRepository = extraRepository;
    }

    @Cacheable("extras")
    public List<Extra> listarExtras() {
        return extraRepository.findByDeletedAtIsNullOrderByDisplayOrderAsc();
    }

    @Cacheable("extrasDisponibles")
    public List<Extra> listarExtrasDisponibles() {
        return extraRepository.findByIsAvailableTrueAndDeletedAtIsNull();
    }

    public List<Extra> listarExtrasPorCategoria(Extra.ExtraCategory category) {
        return extraRepository.findByCategoryAndDeletedAtIsNullOrderByDisplayOrderAsc(category);
    }

    public List<Extra> listarExtrasDisponiblesPorCategoria(Extra.ExtraCategory category) {
        return extraRepository.findByCategoryAndIsAvailableTrueAndDeletedAtIsNull(category);
    }

    public Optional<Extra> obtenerPorId(Integer id) {
        if (id == null) {
            return Optional.empty();
        }
        return extraRepository.findById(id)
            .filter(extra -> extra.getDeletedAt() == null);
    }

    public List<Extra> buscarPorNombre(String nombre) {
        return extraRepository.findByNameContainingIgnoreCaseAndDeletedAtIsNull(nombre);
    }

    @CacheEvict(value = {"extras", "extrasDisponibles"}, allEntries = true)
    public Extra crearExtra(Extra extra) {
        if (extraRepository.existsByName(extra.getName())) {
            throw new IllegalArgumentException("Ya existe un extra con ese nombre");
        }
        if (extra.getDisplayOrder() == null) extra.setDisplayOrder(0);
        if (extra.getStockQuantity() == null) extra.setStockQuantity(0);
        if (extra.getMinStock() == null) extra.setMinStock(5);
        if (extra.getUnitCost() == null) extra.setUnitCost(BigDecimal.ZERO);
        if (extra.getIsAvailable() == null) extra.setIsAvailable(true);
        return extraRepository.save(extra);
    }

    @CacheEvict(value = {"extras", "extrasDisponibles"}, allEntries = true)
    public Extra actualizarExtra(Integer id, Extra extraActualizado) {
        if (id == null) {
            throw new IllegalArgumentException("ID no puede ser null");
        }
        Optional<Extra> extraOpt = extraRepository.findById(id);
        if (extraOpt.isEmpty() || extraOpt.get().getDeletedAt() != null) {
            throw new IllegalArgumentException("Extra no encontrado");
        }

        Extra extra = extraOpt.get();
        if (extraActualizado.getName() != null) {
            extra.setName(extraActualizado.getName());
        }
        extra.setDescription(extraActualizado.getDescription());
        if (extraActualizado.getImageUrl() != null) {
            extra.setImageUrl(extraActualizado.getImageUrl());
        }
        if (extraActualizado.getPrice() != null) {
            extra.setPrice(extraActualizado.getPrice());
        }
        if (extraActualizado.getCategory() != null) {
            extra.setCategory(extraActualizado.getCategory());
        }
        if (extraActualizado.getIsAvailable() != null) {
            extra.setIsAvailable(extraActualizado.getIsAvailable());
        }
        if (extraActualizado.getDisplayOrder() != null) {
            extra.setDisplayOrder(extraActualizado.getDisplayOrder());
        }
        if (extraActualizado.getStockQuantity() != null) {
            extra.setStockQuantity(extraActualizado.getStockQuantity());
        }
        if (extraActualizado.getMinStock() != null) {
            extra.setMinStock(extraActualizado.getMinStock());
        }
        if (extraActualizado.getUnitCost() != null) {
            extra.setUnitCost(extraActualizado.getUnitCost());
        }
        extra.setUpdatedAt(LocalDateTime.now());

        return extraRepository.save(extra);
    }

    @CacheEvict(value = {"extras", "extrasDisponibles"}, allEntries = true)
    public void eliminarExtra(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("ID no puede ser null");
        }
        Optional<Extra> extraOpt = extraRepository.findById(id);
        if (extraOpt.isEmpty()) {
            throw new IllegalArgumentException("Extra no encontrado");
        }

        Extra extra = extraOpt.get();
        extra.setDeletedAt(LocalDateTime.now());
        extraRepository.save(extra);
    }

    @CacheEvict(value = {"extras", "extrasDisponibles"}, allEntries = true)
    public Extra cambiarDisponibilidad(Integer id, boolean disponible) {
        Optional<Extra> extraOpt = obtenerPorId(id);
        if (extraOpt.isEmpty()) {
            throw new IllegalArgumentException("Extra no encontrado");
        }

        Extra extra = extraOpt.get();
        extra.setIsAvailable(disponible);
        return extraRepository.save(extra);
    }

    @CacheEvict(value = {"extras", "extrasDisponibles"}, allEntries = true)
    public Extra cambiarDisponibilidad(Integer id) {
        Optional<Extra> extraOpt = obtenerPorId(id);
        if (extraOpt.isEmpty()) {
            throw new IllegalArgumentException("Extra no encontrado");
        }

        Extra extra = extraOpt.get();
        extra.setIsAvailable(!extra.getIsAvailable());
        return extraRepository.save(extra);
    }

    public boolean existePorNombre(String nombre) {
        return extraRepository.existsByName(nombre);
    }
}
