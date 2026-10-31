package com.example.controllers;

import com.example.dtos.ExtraDTO;
import com.example.models.Extra;
import com.example.services.ExtraService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.example.dtos.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/extras")
@CrossOrigin(origins = "http://localhost:4200")
public class ExtraController {

    private final ExtraService extraService;

    public ExtraController(ExtraService extraService) {
        this.extraService = extraService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ExtraDTO>>> listarTodos() {
        List<Extra> extras = extraService.listarExtras();
        List<ExtraDTO> dtos = extras.stream()
            .map(ExtraDTO::new)
            .toList();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/disponibles")
    public ResponseEntity<ApiResponse<List<ExtraDTO>>> listarDisponibles() {
        List<Extra> extras = extraService.listarExtrasDisponibles();
        List<ExtraDTO> dtos = extras.stream()
            .map(ExtraDTO::new)
            .toList();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/categoria/{categoria}")
    public ResponseEntity<ApiResponse<List<ExtraDTO>>> listarPorCategoria(@PathVariable("categoria") Extra.ExtraCategory categoria) {
        List<Extra> extras = extraService.listarExtrasPorCategoria(categoria);
        List<ExtraDTO> dtos = extras.stream()
            .map(ExtraDTO::new)
            .toList();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExtraDTO>> obtenerPorId(@PathVariable("id") Integer id) {
        Optional<Extra> extra = extraService.obtenerPorId(id);
        return extra.map(e -> ResponseEntity.ok(ApiResponse.success(new ExtraDTO(e))))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/buscar")
    public ResponseEntity<ApiResponse<List<ExtraDTO>>> buscarPorNombre(@RequestParam("nombre") String nombre) {
        List<Extra> extras = extraService.buscarPorNombre(nombre);
        List<ExtraDTO> dtos = extras.stream()
            .map(ExtraDTO::new)
            .toList();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> crear(@RequestBody ExtraDTO extraDTO) {
        try {
            Extra extra = mapFromDto(extraDTO);
            Extra nuevoExtra = extraService.crearExtra(extra);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(new ExtraDTO(nuevoExtra), "Creado exitosamente"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage() != null ? e.getMessage() : "Datos de extra inválidos"));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> actualizar(@PathVariable("id") Integer id, @RequestBody ExtraDTO extraDTO) {
        try {
            Extra extra = mapFromDto(extraDTO);
            Extra extraActualizado = extraService.actualizarExtra(id, extra);
            return ResponseEntity.ok(ApiResponse.success(new ExtraDTO(extraActualizado)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage() != null ? e.getMessage() : "Datos de extra inválidos"));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> eliminar(@PathVariable("id") Integer id) {
        try {
            extraService.eliminarExtra(id);
            return ResponseEntity.ok().body(Map.of("message", "Extra eliminado correctamente", "id", id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage() != null ? e.getMessage() : "Datos de extra inválidos"));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error al eliminar extra: " + e.getMessage()));
        }
    }

    @PatchMapping("/{id}/disponibilidad")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> cambiarDisponibilidad(@PathVariable("id") Integer id) {
        try {
            Extra extraActualizado = extraService.cambiarDisponibilidad(id);
            return ResponseEntity.ok(ApiResponse.success(new ExtraDTO(extraActualizado)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage() != null ? e.getMessage() : "Datos de extra inválidos"));
        }
    }

    private Extra mapFromDto(ExtraDTO extraDTO) {
        Extra extra = new Extra();
        extra.setName(extraDTO.getName());
        extra.setDescription(extraDTO.getDescription());
        extra.setImageUrl(extraDTO.getImageUrl());
        extra.setPrice(extraDTO.getPrice());
        if (extraDTO.getCategory() != null) {
            extra.setCategory(Extra.ExtraCategory.valueOf(extraDTO.getCategory()));
        }
        extra.setIsAvailable(extraDTO.getIsAvailable() != null ? extraDTO.getIsAvailable() : true);
        extra.setDisplayOrder(extraDTO.getDisplayOrder() != null ? extraDTO.getDisplayOrder() : 0);
        extra.setStockQuantity(extraDTO.getStockQuantity() != null ? extraDTO.getStockQuantity() : 0);
        extra.setMinStock(extraDTO.getMinStock() != null ? extraDTO.getMinStock() : 5);
        extra.setUnitCost(extraDTO.getUnitCost() != null ? extraDTO.getUnitCost() : BigDecimal.ZERO);
        return extra;
    }
}
