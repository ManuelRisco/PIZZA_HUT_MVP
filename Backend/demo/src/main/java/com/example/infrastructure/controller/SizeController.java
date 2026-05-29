package com.example.infrastructure.controller;

import com.example.service.SizeService;
import com.example.domain.dto.SizeDTO;
import com.example.domain.model.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sizes")
@CrossOrigin(origins = "http://localhost:4200")
public class SizeController {

    @Autowired
    private SizeService sizeService;

    @GetMapping
    public ResponseEntity<List<SizeDTO>> listarSizes() {
        List<Size> sizes = sizeService.listarSizes();
        List<SizeDTO> sizesDTO = sizes.stream()
            .map(SizeDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(sizesDTO);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerSizePorId(@PathVariable Integer id) {
        Optional<Size> sizeOpt = sizeService.obtenerPorId(id);
        if (sizeOpt.isPresent()) {
            SizeDTO sizeDTO = new SizeDTO(sizeOpt.get());
            return ResponseEntity.ok(sizeDTO);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Size no encontrado"));
        }
    }

    @PostMapping
    public ResponseEntity<?> crearSize(@RequestBody SizeDTO sizeDTO) {
        try {
            Size size = new Size();
            size.setName(sizeDTO.getName());
            size.setExtraCost(sizeDTO.getExtraCost());
            size.setDescription(sizeDTO.getDescription());
            size.setDisplayOrder(sizeDTO.getDisplayOrder());
            
            Size sizeCreado = sizeService.crearSize(size);
            return ResponseEntity.status(HttpStatus.CREATED).body(new SizeDTO(sizeCreado));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarSize(@PathVariable Integer id, @RequestBody SizeDTO sizeDTO) {
        try {
            Size size = new Size();
            size.setName(sizeDTO.getName());
            size.setExtraCost(sizeDTO.getExtraCost());
            size.setDescription(sizeDTO.getDescription());
            size.setDisplayOrder(sizeDTO.getDisplayOrder());
            
            Size sizeActualizado = sizeService.actualizarSize(id, size);
            return ResponseEntity.ok(new SizeDTO(sizeActualizado));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarSize(@PathVariable Integer id) {
        try {
            sizeService.eliminarSize(id);
            return ResponseEntity.ok(Map.of("message", "Size eliminado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", e.getMessage()));
        }
    }
}
