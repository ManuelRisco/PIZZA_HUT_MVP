package com.example.service;

import com.example.domain.model.Extra;
import com.example.domain.repository.ExtraRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ExtraService {

    private final ExtraRepository extraRepository;

    public ExtraService(ExtraRepository extraRepository) {
        this.extraRepository = extraRepository;
    }

    public List<Extra> listarExtras() {
        return extraRepository.findByDeletedAtIsNullOrderByDisplayOrderAsc();
    }

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

    public Extra crearExtra(Extra extra) {
        if (extraRepository.existsByName(extra.getName())) {
            throw new IllegalArgumentException("Ya existe un extra con ese nombre");
        }
        return extraRepository.save(extra);
    }

    public Extra actualizarExtra(Integer id, Extra extraActualizado) {
        if (id == null) {
            throw new IllegalArgumentException("ID no puede ser null");
        }
        Optional<Extra> extraOpt = extraRepository.findById(id);
        if (extraOpt.isEmpty() || extraOpt.get().getDeletedAt() != null) {
            throw new IllegalArgumentException("Extra no encontrado");
        }

        Extra extra = extraOpt.get();
        extra.setName(extraActualizado.getName());
        extra.setDescription(extraActualizado.getDescription());
        extra.setPrice(extraActualizado.getPrice());
        extra.setCategory(extraActualizado.getCategory());
        extra.setIsAvailable(extraActualizado.getIsAvailable());
        extra.setDisplayOrder(extraActualizado.getDisplayOrder());
        extra.setUpdatedAt(LocalDateTime.now());

        return extraRepository.save(extra);
    }

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

    public Extra cambiarDisponibilidad(Integer id, boolean disponible) {
        Optional<Extra> extraOpt = obtenerPorId(id);
        if (extraOpt.isEmpty()) {
            throw new IllegalArgumentException("Extra no encontrado");
        }

        Extra extra = extraOpt.get();
        extra.setIsAvailable(disponible);
        return extraRepository.save(extra);
    }

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
