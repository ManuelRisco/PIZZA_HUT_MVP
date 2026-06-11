package com.example.controller;

import com.example.domain.dto.ExtraDTO;
import com.example.domain.model.Extra;
import com.example.service.ExtraService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/extras")
@CrossOrigin(origins = "http://localhost:4200")
public class ExtraController {

    private final ExtraService extraService;

    public ExtraController(ExtraService extraService) {
        this.extraService = extraService;
    }

    @GetMapping
    public ResponseEntity<List<ExtraDTO>> listarTodos() {
        List<Extra> extras = extraService.listarExtras();
        List<ExtraDTO> dtos = extras.stream()
            .map(ExtraDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/disponibles")
    public ResponseEntity<List<ExtraDTO>> listarDisponibles() {
        List<Extra> extras = extraService.listarExtrasDisponibles();
        List<ExtraDTO> dtos = extras.stream()
            .map(ExtraDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/categoria/{categoria}")
    public ResponseEntity<List<ExtraDTO>> listarPorCategoria(@PathVariable Extra.ExtraCategory categoria) {
        List<Extra> extras = extraService.listarExtrasPorCategoria(categoria);
        List<ExtraDTO> dtos = extras.stream()
            .map(ExtraDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExtraDTO> obtenerPorId(@PathVariable Integer id) {
        Optional<Extra> extra = extraService.obtenerPorId(id);
        return extra.map(e -> ResponseEntity.ok(new ExtraDTO(e)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<ExtraDTO>> buscarPorNombre(@RequestParam String nombre) {
        List<Extra> extras = extraService.buscarPorNombre(nombre);
        List<ExtraDTO> dtos = extras.stream()
            .map(ExtraDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Extra extra) {
        try {
            Extra nuevoExtra = extraService.crearExtra(extra);
            return ResponseEntity.status(HttpStatus.CREATED).body(new ExtraDTO(nuevoExtra));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody Extra extra) {
        try {
            Extra extraActualizado = extraService.actualizarExtra(id, extra);
            return ResponseEntity.ok(new ExtraDTO(extraActualizado));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        try {
            extraService.eliminarExtra(id);
            return ResponseEntity.ok().body(Map.of("message", "Extra eliminado correctamente", "id", id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al eliminar extra: " + e.getMessage()));
        }
    }

    @PatchMapping("/{id}/disponibilidad")
    public ResponseEntity<?> cambiarDisponibilidad(@PathVariable Integer id) {
        try {
            Extra extraActualizado = extraService.cambiarDisponibilidad(id);
            return ResponseEntity.ok(new ExtraDTO(extraActualizado));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
