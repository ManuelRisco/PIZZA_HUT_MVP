package com.example.infrastructure.controller;

import com.example.service.ReviewService;
import com.example.domain.dto.ReviewDTO;
import com.example.domain.model.Review;
import com.example.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "http://localhost:4200")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;
    
    @Autowired
    private SecurityUtils securityUtils;

    @GetMapping
    public ResponseEntity<List<ReviewDTO>> listarReviews() {
        List<Review> reviews = reviewService.listarReviews();
        List<ReviewDTO> reviewsDTO = reviews.stream()
            .map(ReviewDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(reviewsDTO);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerReviewPorId(@PathVariable Integer id) {
        Optional<Review> reviewOpt = reviewService.obtenerPorId(id);
        if (reviewOpt.isPresent()) {
            ReviewDTO reviewDTO = new ReviewDTO(reviewOpt.get());
            return ResponseEntity.ok(reviewDTO);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Review no encontrado"));
        }
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<ReviewDTO>> obtenerReviewsPorOrderId(@PathVariable Integer orderId) {
        List<Review> reviews = reviewService.obtenerPorOrderId(orderId);
        List<ReviewDTO> reviewsDTO = reviews.stream()
            .map(ReviewDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(reviewsDTO);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> obtenerReviewsPorUserId(@PathVariable Integer userId) {
        // Validar que el cliente solo pueda ver sus propias reseñas
        if (!securityUtils.isAdmin()) {
            Integer currentUserId = securityUtils.getCurrentUserId();
            if (currentUserId == null || !userId.equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "No tienes permiso para ver estas reseñas"));
            }
        }
        
        List<Review> reviews = reviewService.obtenerPorUserId(userId);
        List<ReviewDTO> reviewsDTO = reviews.stream()
            .map(ReviewDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(reviewsDTO);
    }

    @PostMapping
    public ResponseEntity<?> crearReview(@RequestBody ReviewDTO reviewDTO) {
        try {
            // Validar que el cliente solo pueda crear reseñas para sí mismo
            if (!securityUtils.isAdmin()) {
                Integer currentUserId = securityUtils.getCurrentUserId();
                if (currentUserId == null || !currentUserId.equals(reviewDTO.getUserId())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "No puedes crear reseñas para otros usuarios"));
                }
            }
            
            Review review = new Review();
            review.setUserId(reviewDTO.getUserId());
            review.setOrderId(reviewDTO.getOrderId());
            review.setRating(reviewDTO.getRating());
            review.setComment(reviewDTO.getComment());
            review.setActive(true); // Las nuevas reseñas están activas por defecto
            
            Review reviewCreado = reviewService.crearReview(review);
            return ResponseEntity.status(HttpStatus.CREATED).body(new ReviewDTO(reviewCreado));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarReview(@PathVariable Integer id, @RequestBody ReviewDTO reviewDTO) {
        try {
            // Verificar que la review existe y obtener su userId
            Optional<Review> reviewOpt = reviewService.obtenerPorId(id);
            if (reviewOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Review no encontrado"));
            }
            
            Review existingReview = reviewOpt.get();
            
            // Validar que el cliente solo pueda actualizar sus propias reseñas
            if (!securityUtils.isAdmin()) {
                Integer currentUserId = securityUtils.getCurrentUserId();
                if (currentUserId == null || !currentUserId.equals(existingReview.getUserId())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "No tienes permiso para editar esta reseña"));
                }
            }
            
            Review review = new Review();
            review.setId(id); // Importante: establecer el ID
            review.setUserId(existingReview.getUserId()); // Preservar userId original
            review.setOrderId(existingReview.getOrderId()); // Preservar orderId original
            review.setRating(reviewDTO.getRating());
            review.setComment(reviewDTO.getComment());
            review.setActive(existingReview.getActive()); // Preservar el estado activo
            review.setCreatedAt(existingReview.getCreatedAt()); // Preservar fecha de creación
            review.setUpdatedAt(LocalDateTime.now()); // Actualizar fecha de modificación
            
            Review reviewActualizado = reviewService.actualizarReview(id, review);
            return ResponseEntity.ok(new ReviewDTO(reviewActualizado));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarReview(@PathVariable Integer id) {
        try {
            // Verificar que la review existe y obtener su userId
            Optional<Review> reviewOpt = reviewService.obtenerPorId(id);
            if (reviewOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Review no encontrado"));
            }
            
            Review existingReview = reviewOpt.get();
            
            // Validar que el cliente solo pueda eliminar sus propias reseñas
            if (!securityUtils.isAdmin()) {
                Integer currentUserId = securityUtils.getCurrentUserId();
                if (currentUserId == null || !currentUserId.equals(existingReview.getUserId())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "No tienes permiso para eliminar esta reseña"));
                }
            }
            
            reviewService.eliminarReview(id);
            return ResponseEntity.ok(Map.of("message", "Review eliminado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<?> desactivarReview(@PathVariable Integer id) {
        try {
            // Solo admin puede desactivar reseñas
            if (!securityUtils.isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "No tienes permiso para desactivar reseñas"));
            }
            
            Review reviewActualizado = reviewService.desactivarReview(id);
            return ResponseEntity.ok(new ReviewDTO(reviewActualizado));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/activar")
    public ResponseEntity<?> activarReview(@PathVariable Integer id) {
        try {
            // Solo admin puede activar reseñas
            if (!securityUtils.isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "No tienes permiso para activar reseñas"));
            }
            
            Review reviewActualizado = reviewService.activarReview(id);
            return ResponseEntity.ok(new ReviewDTO(reviewActualizado));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", e.getMessage()));
        }
    }
}
