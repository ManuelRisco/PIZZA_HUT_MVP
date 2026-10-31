package com.example.controllers;

import com.example.services.SizeService;
import com.example.dtos.SizeDTO;
import com.example.models.Size;
import org.springframework.http.ResponseEntity;
import com.example.dtos.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/sizes")
@CrossOrigin(origins = "http://localhost:4200")
public class SizeController {

    private static final String MSG_KEY = "message";

    private final SizeService sizeService;

    public SizeController(SizeService sizeService) {
        this.sizeService = sizeService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SizeDTO>>> listarSizes(@RequestParam(value = "includeDeleted", defaultValue = "false") boolean includeDeleted) {
        List<Size> sizes = sizeService.listarSizes(includeDeleted);
        List<SizeDTO> sizesDTO = sizes.stream()
            .map(SizeDTO::new)
            .toList();
        return ResponseEntity.ok(ApiResponse.success(sizesDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> obtenerSizePorId(@PathVariable("id") Integer id) {
        Optional<Size> sizeOpt = sizeService.obtenerPorId(id);
        if (sizeOpt.isPresent()) {
            SizeDTO sizeDTO = new SizeDTO(sizeOpt.get());
            return ResponseEntity.ok(ApiResponse.success(sizeDTO));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(MSG_KEY, "Size no encontrado"));
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> crearSize(@RequestBody SizeDTO sizeDTO) {
        try {
            Size size = new Size();
            size.setName(sizeDTO.getName());
            size.setExtraCost(sizeDTO.getExtraCost());
            size.setRecipeMultiplier(sizeDTO.getRecipeMultiplier() != null ? sizeDTO.getRecipeMultiplier() : BigDecimal.ONE);
            size.setStockQuantity(sizeDTO.getStockQuantity() != null ? sizeDTO.getStockQuantity() : 50);
            size.setMinStock(sizeDTO.getMinStock() != null ? sizeDTO.getMinStock() : 10);
            size.setDescription(sizeDTO.getDescription());
            size.setDisplayOrder(sizeDTO.getDisplayOrder() != null ? sizeDTO.getDisplayOrder() : 0);

            Size sizeCreado = sizeService.crearSize(size);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(new SizeDTO(sizeCreado), "Creado exitosamente"));
        } catch (RuntimeException e) { throw e; } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(MSG_KEY, e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> actualizarSize(@PathVariable("id") Integer id, @RequestBody SizeDTO sizeDTO) {
        try {
            Size size = new Size();
            size.setName(sizeDTO.getName());
            size.setExtraCost(sizeDTO.getExtraCost());
            size.setRecipeMultiplier(sizeDTO.getRecipeMultiplier() != null && sizeDTO.getRecipeMultiplier().compareTo(BigDecimal.ZERO) > 0
                    ? sizeDTO.getRecipeMultiplier()
                    : BigDecimal.ONE);
            if (sizeDTO.getStockQuantity() != null) {
                size.setStockQuantity(sizeDTO.getStockQuantity());
            }
            if (sizeDTO.getMinStock() != null) {
                size.setMinStock(sizeDTO.getMinStock());
            }
            size.setDescription(sizeDTO.getDescription());
            size.setDisplayOrder(sizeDTO.getDisplayOrder() != null ? sizeDTO.getDisplayOrder() : 0);

            Size sizeActualizado = sizeService.actualizarSize(id, size);
            return ResponseEntity.ok(ApiResponse.success(new SizeDTO(sizeActualizado)));
        } catch (RuntimeException e) { throw e; } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(MSG_KEY, e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> eliminarSize(@PathVariable("id") Integer id) {
        try {
            sizeService.eliminarSize(id);
            return ResponseEntity.ok(ApiResponse.success(Map.of(MSG_KEY, "Size eliminado lógicamente")));
        } catch (RuntimeException e) { throw e; } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(MSG_KEY, e.getMessage()));
        }
    }

    @PatchMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> restoreSize(@PathVariable("id") Integer id) {
        boolean restaurado = sizeService.restaurarSize(id);
        if (restaurado) {
            return ResponseEntity.ok(ApiResponse.success(Map.of(MSG_KEY, "Tamaño restaurado con éxito")));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(MSG_KEY, "Size no encontrado"));
        }
    }
}
