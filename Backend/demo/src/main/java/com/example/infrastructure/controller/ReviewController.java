package com.example.infrastructure.controller;

import com.example.service.ReviewService;
import com.example.domain.dto.ReviewDTO;
import com.example.domain.model.Review;
import com.example.security.SecurityUtils;
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

    private final ReviewService reviewService;
    
    private final SecurityUtils securityUtils;

    public ReviewController(ReviewService reviewService, SecurityUtils securityUtils) {
        this.reviewService = reviewService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    public ResponseEntity<List<ReviewDTO>> listarReviews() {
        List<Review> reviews = reviewService.listarReviews();
        List<ReviewDTO> reviewsDTO = reviews.stream()
            .map(ReviewDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(reviewsDTO);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerReviewPorId(@PathVariable("id") Integer id) { // Corregido
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
    public ResponseEntity<List<ReviewDTO>> obtenerReviewsPorOrderId(@PathVariable("orderId") Integer orderId) { // Corregido
        List<Review> reviews = reviewService.obtenerPorOrderId(orderId);
        List<ReviewDTO> reviewsDTO = reviews.stream()
            .map(ReviewDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(reviewsDTO);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> obtenerReviewsPorUserId(@PathVariable("userId") Integer userId) { // Corregido
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
            review.setActive(true);
            
            Review reviewCreado = reviewService.crearReview(review);
            return ResponseEntity.status(HttpStatus.CREATED).body(new ReviewDTO(reviewCreado));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarReview(@PathVariable("id") Integer id, @RequestBody ReviewDTO reviewDTO) { // Corregido
        try {
            Optional<Review> reviewOpt = reviewService.obtenerPorId(id);
            if (reviewOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Review no encontrado"));
            }
            
            Review existingReview = reviewOpt.get();
            
            if (!securityUtils.isAdmin()) {
                Integer currentUserId = securityUtils.getCurrentUserId();
                if (currentUserId == null || !currentUserId.equals(existingReview.getUserId())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "No tienes permiso para editar esta reseña"));
                }
            }
            
            Review review = new Review();
            review.setId(id);
            review.setUserId(existingReview.getUserId());
            review.setOrderId(existingReview.getOrderId());
            review.setRating(reviewDTO.getRating());
            review.setComment(reviewDTO.getComment());
            review.setActive(existingReview.getActive());
            review.setCreatedAt(existingReview.getCreatedAt());
            review.setUpdatedAt(LocalDateTime.now());
            
            Review reviewActualizado = reviewService.actualizarReview(id, review);
            return ResponseEntity.ok(new ReviewDTO(reviewActualizado));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarReview(@PathVariable("id") Integer id) { // Corregido
        try {
            Optional<Review> reviewOpt = reviewService.obtenerPorId(id);
            if (reviewOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Review no encontrado"));
            }
            
            Review existingReview = reviewOpt.get();
            
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
    public ResponseEntity<?> desactivarReview(@PathVariable("id") Integer id) { // Corregido
        try {
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
    public ResponseEntity<?> activarReview(@PathVariable("id") Integer id) { // Corregido
        try {
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