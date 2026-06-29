package com.example.controllers;

import com.example.services.SizeService;
import com.example.dtos.SizeDTO;
import com.example.models.Size;
import org.springframework.http.ResponseEntity;
import com.example.dtos.ApiResponse;
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

    private final SizeService sizeService;

    public SizeController(SizeService sizeService) {
        this.sizeService = sizeService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SizeDTO>>> listarSizes() {
        List<Size> sizes = sizeService.listarSizes();
        List<SizeDTO> sizesDTO = sizes.stream()
            .map(SizeDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(sizesDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerSizePorId(@PathVariable("id") Integer id) {
        Optional<Size> sizeOpt = sizeService.obtenerPorId(id);
        if (sizeOpt.isPresent()) {
            SizeDTO sizeDTO = new SizeDTO(sizeOpt.get());
            return ResponseEntity.ok(ApiResponse.success(sizeDTO));
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
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(new SizeDTO(sizeCreado), "Creado exitosamente"));
        } catch (RuntimeException e) { throw e; } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarSize(@PathVariable("id") Integer id, @RequestBody SizeDTO sizeDTO) {
        try {
            Size size = new Size();
            size.setName(sizeDTO.getName());
            size.setExtraCost(sizeDTO.getExtraCost());
            size.setDescription(sizeDTO.getDescription());
            size.setDisplayOrder(sizeDTO.getDisplayOrder());
            
            Size sizeActualizado = sizeService.actualizarSize(id, size);
            return ResponseEntity.ok(ApiResponse.success(new SizeDTO(sizeActualizado)));
        } catch (RuntimeException e) { throw e; } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarSize(@PathVariable("id") Integer id) {
        try {
            sizeService.eliminarSize(id);
            return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Size eliminado correctamente")));
        } catch (RuntimeException e) { throw e; } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", e.getMessage()));
        }
    }
}
