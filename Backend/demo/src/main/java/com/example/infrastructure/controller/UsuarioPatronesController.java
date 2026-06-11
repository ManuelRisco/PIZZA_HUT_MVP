package com.example.infrastructure.controller;

import com.example.domain.model.Usuario;
import com.example.application.users.comportamiento.AdminUsuarioSpecification;
import com.example.application.users.estructurales.UsuarioSpecificationComposite;
import com.example.application.users.comportamiento.ActiveUsuarioSpecification;
import com.example.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador de ejemplo que demuestra el uso de los patrones de diseño
 */
@RestController
@RequestMapping("/api/usuarios/patrones")
@CrossOrigin(origins = "http://localhost:4200")
public class UsuarioPatronesController {

    private final UsuarioService usuarioService;

    public UsuarioPatronesController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * Endpoint que usa Factory Pattern para crear un cliente
     * POST /api/usuarios/patrones/crear-cliente
     */
    @PostMapping("/crear-cliente")
    public ResponseEntity<?> crearCliente(@RequestBody Map<String, String> datos) {
        String resultado = usuarioService.registrarCliente(
            datos.get("email"),
            datos.get("password"),
            datos.get("name"),
            datos.get("phone")
        );
        
        if (resultado.contains("éxito")) {
            return ResponseEntity.ok(Map.of("message", resultado));
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", resultado));
        }
    }

    /**
     * Endpoint que usa Factory Pattern para crear un administrador
     * POST /api/usuarios/patrones/crear-admin
     */
    @PostMapping("/crear-admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> crearAdministrador(@RequestBody Map<String, String> datos) {
        String resultado = usuarioService.registrarAdministrador(
            datos.get("email"),
            datos.get("password"),
            datos.get("name")
        );
        
        if (resultado.contains("éxito")) {
            return ResponseEntity.ok(Map.of("message", resultado));
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", resultado));
        }
    }

    /**
     * Endpoint que usa Specification Pattern para listar solo administradores
     * GET /api/usuarios/patrones/admins
     */
    @GetMapping("/admins")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Usuario>> listarAdministradores() {
        List<Usuario> admins = usuarioService.listarUsuariosPorEspecificacion(
            new AdminUsuarioSpecification()
        );
        return ResponseEntity.ok(admins);
    }

    /**
     * Endpoint que usa Specification Pattern para listar usuarios activos
     * GET /api/usuarios/patrones/activos
     */
    @GetMapping("/activos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Usuario>> listarUsuariosActivos() {
        List<Usuario> activos = usuarioService.listarUsuariosPorEspecificacion(
            new ActiveUsuarioSpecification()
        );
        return ResponseEntity.ok(activos);
    }

    /**
     * Endpoint que combina especificaciones (admins activos)
     * GET /api/usuarios/patrones/admins-activos
     */
    @GetMapping("/admins-activos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Usuario>> listarAdministradoresActivos() {
        // Combina dos especificaciones usando el patrón Composite
        List<Usuario> adminActivos = usuarioService.listarUsuariosPorEspecificacion(
            UsuarioSpecificationComposite.and(
                new AdminUsuarioSpecification(),
                new ActiveUsuarioSpecification()
            )
        );
        return ResponseEntity.ok(adminActivos);
    }

    /**
     * Endpoint de información sobre los patrones implementados
     * GET /api/usuarios/patrones/info
     */
    @GetMapping("/info")
    public ResponseEntity<?> informacionPatrones() {
        return ResponseEntity.ok(Map.of(
            "patrones_implementados", List.of(
                "Builder Pattern - Construcción fluida de objetos Usuario",
                "Factory Pattern - Creación de diferentes tipos de usuarios",
                "Strategy Pattern - Validación modular y extensible",
                "Specification Pattern - Filtrado flexible de usuarios"
            ),
            "endpoints_disponibles", Map.of(
                "POST /crear-cliente", "Crea un cliente usando Factory Pattern",
                "POST /crear-admin", "Crea un admin usando Factory Pattern (requiere auth)",
                "GET /admins", "Lista administradores usando Specification Pattern",
                "GET /activos", "Lista usuarios activos usando Specification Pattern",
                "GET /admins-activos", "Combina especificaciones (Composite Pattern)"
            ),
            "documentacion", "Ver Backend/PATRONES_DISENO.md para más detalles"
        ));
    }
}