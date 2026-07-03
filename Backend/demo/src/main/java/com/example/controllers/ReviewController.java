package com.example.controllers;

import com.example.services.ReviewService;
import com.example.dtos.ReviewDTO;
import com.example.models.Review;
import com.example.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import com.example.dtos.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "http://localhost:4200")
public class ReviewController {

    private static final String MSG_KEY = "message";
    private static final String MSG_NOT_FOUND = "Review no encontrado";

    private final ReviewService reviewService;
    
    private final SecurityUtils securityUtils;

    public ReviewController(ReviewService reviewService, SecurityUtils securityUtils) {
        this.reviewService = reviewService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReviewDTO>>> listarReviews() {
        List<Review> reviews = reviewService.listarReviews();
        List<ReviewDTO> reviewsDTO = reviews.stream()
            .map(ReviewDTO::new)
            .toList();
        return ResponseEntity.ok(ApiResponse.success(reviewsDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> obtenerReviewPorId(@PathVariable("id") Integer id) {
        Optional<Review> reviewOpt = reviewService.obtenerPorId(id);
        if (reviewOpt.isPresent()) {
            ReviewDTO reviewDTO = new ReviewDTO(reviewOpt.get());
            return ResponseEntity.ok(ApiResponse.success(reviewDTO));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(MSG_KEY, MSG_NOT_FOUND));
        }
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<List<ReviewDTO>>> obtenerReviewsPorOrderId(@PathVariable("orderId") Integer orderId) {
        List<Review> reviews = reviewService.obtenerPorOrderId(orderId);
        List<ReviewDTO> reviewsDTO = reviews.stream()
            .map(ReviewDTO::new)
            .toList();
        return ResponseEntity.ok(ApiResponse.success(reviewsDTO));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Object> obtenerReviewsPorUserId(@PathVariable("userId") Integer userId) {
        // Validar que el cliente solo pueda ver sus propias reseÃ±as
        if (!securityUtils.isAdmin()) {
            Integer currentUserId = securityUtils.getCurrentUserId();
            if (currentUserId == null || !userId.equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(MSG_KEY, "No tienes permiso para ver estas reseÃ±as"));
            }
        }
        
        List<Review> reviews = reviewService.obtenerPorUserId(userId);
        List<ReviewDTO> reviewsDTO = reviews.stream()
            .map(ReviewDTO::new)
            .toList();
        return ResponseEntity.ok(ApiResponse.success(reviewsDTO));
    }

    @PostMapping
    public ResponseEntity<Object> crearReview(@Valid @RequestBody ReviewDTO reviewDTO) {
        try {
            if (!securityUtils.isAdmin()) {
                Integer currentUserId = securityUtils.getCurrentUserId();
                if (currentUserId == null || !currentUserId.equals(reviewDTO.getUserId())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(MSG_KEY, "No puedes crear reseÃ±as para otros usuarios"));
                }
            }
            
            Review review = new Review();
            review.setUserId(reviewDTO.getUserId());
            review.setOrderId(reviewDTO.getOrderId());
            review.setRating(reviewDTO.getRating());
            review.setComment(reviewDTO.getComment());
            review.setActive(true);
            
            Review reviewCreado = reviewService.crearReview(review);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(new ReviewDTO(reviewCreado), "Creado exitosamente"));
        } catch (RuntimeException e) { throw e; } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(MSG_KEY, e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> actualizarReview(@PathVariable("id") Integer id, @Valid @RequestBody ReviewDTO reviewDTO) {
        try {
            Optional<Review> reviewOpt = reviewService.obtenerPorId(id);
            if (reviewOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(MSG_KEY, MSG_NOT_FOUND));
            }
            
            Review existingReview = reviewOpt.get();
            
            if (!securityUtils.isAdmin()) {
                Integer currentUserId = securityUtils.getCurrentUserId();
                if (currentUserId == null || !currentUserId.equals(existingReview.getUserId())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(MSG_KEY, "No tienes permiso para editar esta reseÃ±a"));
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
            return ResponseEntity.ok(ApiResponse.success(new ReviewDTO(reviewActualizado)));
        } catch (RuntimeException e) { throw e; } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(MSG_KEY, e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> eliminarReview(@PathVariable("id") Integer id) {
        try {
            Optional<Review> reviewOpt = reviewService.obtenerPorId(id);
            if (reviewOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(MSG_KEY, MSG_NOT_FOUND));
            }
            
            Review existingReview = reviewOpt.get();
            
            if (!securityUtils.isAdmin()) {
                Integer currentUserId = securityUtils.getCurrentUserId();
                if (currentUserId == null || !currentUserId.equals(existingReview.getUserId())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(MSG_KEY, "No tienes permiso para eliminar esta reseÃ±a"));
                }
            }
            
            reviewService.eliminarReview(id);
            return ResponseEntity.ok(ApiResponse.success(Map.of(MSG_KEY, "Review eliminado correctamente")));
        } catch (RuntimeException e) { throw e; } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(MSG_KEY, e.getMessage()));
        }
    }

    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<Object> desactivarReview(@PathVariable("id") Integer id) {
        try {
            if (!securityUtils.isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(MSG_KEY, "No tienes permiso para desactivar reseÃ±as"));
            }
            
            Review reviewActualizado = reviewService.desactivarReview(id);
            return ResponseEntity.ok(ApiResponse.success(new ReviewDTO(reviewActualizado)));
        } catch (RuntimeException e) { throw e; } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(MSG_KEY, e.getMessage()));
        }
    }

    @PatchMapping("/{id}/activar")
    public ResponseEntity<Object> activarReview(@PathVariable("id") Integer id) {
        try {
            if (!securityUtils.isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(MSG_KEY, "No tienes permiso para activar reseÃ±as"));
            }
            
            Review reviewActualizado = reviewService.activarReview(id);
            return ResponseEntity.ok(ApiResponse.success(new ReviewDTO(reviewActualizado)));
        } catch (RuntimeException e) { throw e; } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(MSG_KEY, e.getMessage()));
        }
    }
}

