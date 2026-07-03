package com.example.controllers;

import com.example.services.FavoriteService;
import com.example.dtos.FavoriteDTO;
import com.example.models.Favorite;
import com.example.models.FavoriteId;
import com.example.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import com.example.dtos.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/favorites")
@CrossOrigin(origins = "http://localhost:4200")
public class FavoriteController {

    private final FavoriteService favoriteService;
    
    private final SecurityUtils securityUtils;

    public FavoriteController(FavoriteService favoriteService, SecurityUtils securityUtils) {
        this.favoriteService = favoriteService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    public ResponseEntity<?> listarFavorites() {
        // Los CUSTOMER solo pueden ver sus propios favoritos
        if (!securityUtils.isAdmin()) {
            Integer currentUserId = securityUtils.getCurrentUserId();
            if (currentUserId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            List<Favorite> favorites = favoriteService.obtenerPorUserId(currentUserId);
            List<FavoriteDTO> favoritesDTO = favorites.stream()
                .map(FavoriteDTO::new)
                .collect(Collectors.toList());
            return ResponseEntity.ok(ApiResponse.success(favoritesDTO));
        }
        
        // ADMIN puede ver todos
        List<Favorite> favorites = favoriteService.listarFavorites();
        List<FavoriteDTO> favoritesDTO = favorites.stream()
            .map(FavoriteDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(favoritesDTO));
    }

    @GetMapping("/{userId}/{pizzaId}")
    public ResponseEntity<?> obtenerFavoritePorId(
            @PathVariable("userId") Integer userId, 
            @PathVariable("pizzaId") Integer pizzaId) {
        
        // Validar que el cliente solo pueda ver sus propios favoritos
        if (!securityUtils.isAdmin()) {
            Integer currentUserId = securityUtils.getCurrentUserId();
            if (currentUserId == null || !userId.equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "No tienes permiso para ver este favorito"));
            }
        }
        
        FavoriteId id = new FavoriteId(userId, pizzaId);
        Optional<Favorite> favoriteOpt = favoriteService.obtenerPorId(id);
        if (favoriteOpt.isPresent()) {
            FavoriteDTO favoriteDTO = new FavoriteDTO(favoriteOpt.get());
            return ResponseEntity.ok(ApiResponse.success(favoriteDTO));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Favorite no encontrado"));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> obtenerFavoritesPorUserId(@PathVariable("userId") Integer userId) {
        // Validar que el cliente solo pueda ver sus propios favoritos
        if (!securityUtils.isAdmin()) {
            Integer currentUserId = securityUtils.getCurrentUserId();
            if (currentUserId == null || !userId.equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "No tienes permiso para ver estos favoritos"));
            }
        }
        
        List<Favorite> favorites = favoriteService.obtenerPorUserId(userId);
        List<FavoriteDTO> favoritesDTO = favorites.stream()
            .map(FavoriteDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(favoritesDTO));
    }

    @GetMapping("/pizza/{pizzaId}")
    public ResponseEntity<ApiResponse<List<FavoriteDTO>>> obtenerFavoritesPorPizzaId(@PathVariable("pizzaId") Integer pizzaId) {
        // Solo ADMIN deber\u00eda ver qu\u00e9 usuarios tienen como favorita una pizza (por privacidad)
        // Si necesitas que los usuarios lo vean, quita la l\u00f3gica de seguridad aqu\u00ed
        List<Favorite> favorites = favoriteService.obtenerPorPizzaId(pizzaId);
        List<FavoriteDTO> favoritesDTO = favorites.stream()
            .map(FavoriteDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(favoritesDTO));
    }

    @PostMapping
    public ResponseEntity<?> crearFavorite(@Valid @RequestBody FavoriteDTO favoriteDTO) {
        try {
            if (!securityUtils.isAdmin()) {
                Integer currentUserId = securityUtils.getCurrentUserId();
                if (currentUserId == null || !currentUserId.equals(favoriteDTO.getUserId())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "No puedes agregar favoritos para otros usuarios"));
                }
            }

            FavoriteId id = new FavoriteId(favoriteDTO.getUserId(), favoriteDTO.getPizzaId());
            
            // Verificar si ya existe
            if (favoriteService.obtenerPorId(id).isPresent()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "La pizza ya está en tus favoritos"));
            }
            
            Favorite favorite = new Favorite();
            favorite.setId(id);
            
            Favorite favoriteCreado = favoriteService.crearFavorite(favorite);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(new FavoriteDTO(favoriteCreado), "Favorito creado exitosamente"));
        } catch (RuntimeException e) { throw e; } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Error al crear el favorito"));
        }
    }

    @DeleteMapping("/{userId}/{pizzaId}")
    public ResponseEntity<?> eliminarFavorite(
            @PathVariable("userId") Integer userId, 
            @PathVariable("pizzaId") Integer pizzaId) {
        try {
            // Validar que el cliente solo pueda eliminar sus propios favoritos
            if (!securityUtils.isAdmin()) {
                Integer currentUserId = securityUtils.getCurrentUserId();
                if (currentUserId == null || !userId.equals(currentUserId)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "No tienes permiso para eliminar este favorito"));
                }
            }
            
            FavoriteId id = new FavoriteId(userId, pizzaId);
            favoriteService.eliminarFavorite(id);
            return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Favorite eliminado correctamente")));
        } catch (RuntimeException e) { throw e; } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", e.getMessage()));
        }
    }
}
