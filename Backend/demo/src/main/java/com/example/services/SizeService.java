package com.example.services;

import com.example.models.Size;
import com.example.repositories.SizeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SizeService {

    private final SizeRepository sizeRepository;

    public SizeService(SizeRepository sizeRepository) {
        this.sizeRepository = sizeRepository;
    }

    public List<Size> listarSizes() {
        return sizeRepository.findByDeletedAtIsNull();
    }

    @SuppressWarnings("null")
    public Optional<Size> obtenerPorId(Integer id) {
        return sizeRepository.findById(id);
    }

    public Size crearSize(Size size) {
        if (sizeRepository.existsByNameAndDeletedAtIsNull(size.getName())) {
            throw new IllegalArgumentException("Ya existe un tamaño con ese nombre");
        }
        if (size.getExtraCost() != null && size.getExtraCost().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El multiplicador de precio no puede ser negativo.");
        }

        Optional<Size> softDeleted = sizeRepository.findByName(size.getName());
        if (softDeleted.isPresent()) {
            Size existing = softDeleted.get();
            existing.setDeletedAt(null);
            existing.setExtraCost(size.getExtraCost());
            existing.setDescription(size.getDescription());
            existing.setDisplayOrder(size.getDisplayOrder());
            return sizeRepository.save(existing);
        }

        return sizeRepository.save(size);
    }

    public Size actualizarSize(Integer id, Size size) {
        @SuppressWarnings("null")
        Size existingSize = sizeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Size no encontrado"));

        if (!existingSize.getName().equals(size.getName())
                && sizeRepository.existsByNameAndDeletedAtIsNull(size.getName())) {
            throw new IllegalArgumentException("Ya existe un tamaño con ese nombre");
        }

        if (size.getExtraCost() != null && size.getExtraCost().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El multiplicador de precio no puede ser negativo.");
        }

        existingSize.setName(size.getName());
        existingSize.setExtraCost(size.getExtraCost());
        existingSize.setDescription(size.getDescription());
        existingSize.setDisplayOrder(size.getDisplayOrder());

        return sizeRepository.save(existingSize);
    }

    public void eliminarSize(Integer id) {
        @SuppressWarnings("null")
        Optional<Size> sizeOpt = sizeRepository.findById(id);
        if (sizeOpt.isEmpty()) {
            throw new IllegalArgumentException("Size no encontrado");
        }
        Size size = sizeOpt.get();
        size.setDeletedAt(java.time.LocalDateTime.now());
        sizeRepository.save(size);
    }
}
