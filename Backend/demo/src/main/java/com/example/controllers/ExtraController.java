package com.example.controllers;

import com.example.dtos.ExtraDTO;
import com.example.models.Extra;
import com.example.services.ExtraService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.example.dtos.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/extras")
@CrossOrigin(origins = "http://localhost:4200")
public class ExtraController {

    private static final String MSG_ERROR_KEY = "error";

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
    public ResponseEntity<Object> crear(@RequestBody ExtraDTO extraDTO) {
        try {
            Extra extra = new Extra();
            extra.setName(extraDTO.getName());
            extra.setPrice(extraDTO.getPrice());
            if (extraDTO.getCategory() != null) {
                extra.setCategory(Extra.ExtraCategory.valueOf(extraDTO.getCategory()));
            }
            extra.setIsAvailable(extraDTO.getIsAvailable());
            Extra nuevoExtra = extraService.crearExtra(extra);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(new ExtraDTO(nuevoExtra), "Creado exitosamente"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(MSG_ERROR_KEY, e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> actualizar(@PathVariable("id") Integer id, @RequestBody ExtraDTO extraDTO) {
        try {
            Extra extra = new Extra();
            extra.setName(extraDTO.getName());
            extra.setPrice(extraDTO.getPrice());
            if (extraDTO.getCategory() != null) {
                extra.setCategory(Extra.ExtraCategory.valueOf(extraDTO.getCategory()));
            }
            extra.setIsAvailable(extraDTO.getIsAvailable());
            Extra extraActualizado = extraService.actualizarExtra(id, extra);
            return ResponseEntity.ok(ApiResponse.success(new ExtraDTO(extraActualizado)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(MSG_ERROR_KEY, e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> eliminar(@PathVariable("id") Integer id) {
        try {
            extraService.eliminarExtra(id);
            return ResponseEntity.ok().body(Map.of("message", "Extra eliminado correctamente", "id", id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(MSG_ERROR_KEY, e.getMessage()));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(MSG_ERROR_KEY, "Error al eliminar extra: " + e.getMessage()));
        }
    }

    @PatchMapping("/{id}/disponibilidad")
    public ResponseEntity<Object> cambiarDisponibilidad(@PathVariable("id") Integer id) {
        try {
            Extra extraActualizado = extraService.cambiarDisponibilidad(id);
            return ResponseEntity.ok(ApiResponse.success(new ExtraDTO(extraActualizado)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(MSG_ERROR_KEY, e.getMessage()));
        }
    }
}