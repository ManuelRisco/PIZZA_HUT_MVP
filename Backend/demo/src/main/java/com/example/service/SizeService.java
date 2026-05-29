package com.example.service;

import com.example.domain.model.Size;
import com.example.domain.repository.SizeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SizeService {

    @Autowired
    private SizeRepository sizeRepository;

    public List<Size> listarSizes() {
        return sizeRepository.findAll();
    }

    public Optional<Size> obtenerPorId(Integer id) {
        return sizeRepository.findById(id);
    }

    public Size crearSize(Size size) {
        if (sizeRepository.existsByName(size.getName())) {
            throw new IllegalArgumentException("Ya existe un tamaño con ese nombre");
        }
        return sizeRepository.save(size);
    }

    public Size actualizarSize(Integer id, Size size) {
        if (!sizeRepository.existsById(id)) {
            throw new IllegalArgumentException("Size no encontrado");
        }
        size.setId(id);
        return sizeRepository.save(size);
    }

    public void eliminarSize(Integer id) {
        if (!sizeRepository.existsById(id)) {
            throw new IllegalArgumentException("Size no encontrado");
        }
        sizeRepository.deleteById(id);
    }
}
