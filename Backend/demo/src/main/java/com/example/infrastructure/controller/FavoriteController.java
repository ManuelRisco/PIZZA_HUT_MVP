package com.example.infrastructure.controller;

import com.example.service.FavoriteService;
import com.example.domain.dto.FavoriteDTO;
import com.example.domain.model.Favorite;
import com.example.domain.model.FavoriteId;
import com.example.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/favorites")
@CrossOrigin(origins = "http://localhost:4200")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;
    
    @Autowired
    private SecurityUtils securityUtils;

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
            return ResponseEntity.ok(favoritesDTO);
        }
        
        // ADMIN puede ver todos
        List<Favorite> favorites = favoriteService.listarFavorites();
        List<FavoriteDTO> favoritesDTO = favorites.stream()
            .map(FavoriteDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(favoritesDTO);
    }

    @GetMapping("/{userId}/{pizzaId}")
    public ResponseEntity<?> obtenerFavoritePorId(@PathVariable Integer userId, @PathVariable Integer pizzaId) {
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
            return ResponseEntity.ok(favoriteDTO);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Favorite no encontrado"));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> obtenerFavoritesPorUserId(@PathVariable Integer userId) {
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
        return ResponseEntity.ok(favoritesDTO);
    }

    @GetMapping("/pizza/{pizzaId}")
    public ResponseEntity<List<FavoriteDTO>> obtenerFavoritesPorPizzaId(@PathVariable Integer pizzaId) {
        // Solo ADMIN puede ver qué usuarios tienen como favorita una pizza
        List<Favorite> favorites = favoriteService.obtenerPorPizzaId(pizzaId);
        List<FavoriteDTO> favoritesDTO = favorites.stream()
            .map(FavoriteDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(favoritesDTO);
    }

    @PostMapping
    public ResponseEntity<?> crearFavorite(@RequestBody FavoriteDTO favoriteDTO) {
        try {
            // Validar que el cliente solo pueda crear favoritos para sí mismo
            if (!securityUtils.isAdmin()) {
                Integer currentUserId = securityUtils.getCurrentUserId();
                if (currentUserId == null || !currentUserId.equals(favoriteDTO.getUserId())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "No puedes crear favoritos para otros usuarios"));
                }
            }
            
            FavoriteId id = new FavoriteId(favoriteDTO.getUserId(), favoriteDTO.getPizzaId());
            Favorite favorite = new Favorite();
            favorite.setId(id);
            
            Favorite favoriteCreado = favoriteService.crearFavorite(favorite);
            return ResponseEntity.status(HttpStatus.CREATED).body(new FavoriteDTO(favoriteCreado));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{userId}/{pizzaId}")
    public ResponseEntity<?> eliminarFavorite(@PathVariable Integer userId, @PathVariable Integer pizzaId) {
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
            return ResponseEntity.ok(Map.of("message", "Favorite eliminado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", e.getMessage()));
        }
    }
}
