package com.example.services;

import com.example.models.Review;
import com.example.repositories.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("CU08/CU19: Gestión de Reseñas - Pruebas TDD")
class ReviewServiceTest {

    @Mock
    private ReviewRepository repository;

    @InjectMocks
    private ReviewService service;

    private Review review;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        review = new Review();
        review.setId(1);
        review.setRating(5);
        review.setComment("Excelente pizza");
        review.setActive(false);
    }

    // ==========================================
    // CU08: Ver Reseñas
    // ==========================================

    @Test
    @DisplayName("CU08 - Flujo Principal: Listar reseñas [RF15, RF16]")
    void testListarReviews() {
        List<Review> list = new ArrayList<>();
        list.add(review);
        when(repository.findAll()).thenReturn(list);

        List<Review> resultado = service.listarReviews();

        assertEquals(1, resultado.size());
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("CU08 - A1: Sin reseñas disponibles")
    void testListarReviewsVacio() {
        when(repository.findAll()).thenReturn(new ArrayList<>());
        List<Review> resultado = service.listarReviews();

        assertTrue(resultado.isEmpty());
        verify(repository, times(1)).findAll();
    }

    // ==========================================
    // CU19: Gestionar Reseñas
    // ==========================================

    @Test
    @DisplayName("CU19 - Flujo Principal: Aprobar reseña [RF35]")
    void testAprobarReview() {
        when(repository.findById(1)).thenReturn(Optional.of(review));

        Review approvedReview = new Review();
        approvedReview.setId(1);
        approvedReview.setActive(true);
        when(repository.save(any(Review.class))).thenReturn(approvedReview);

        // Asumimos que actualizar modifica el estado
        Review result = service.actualizarReview(1, approvedReview);

        assertNotNull(result);
        assertTrue(result.getActive());
        verify(repository, times(1)).findById(1);
        verify(repository, times(1)).save(any(Review.class));
    }

    @Test
    @DisplayName("CU19 - Flujo Principal: Eliminar reseña [RF35]")
    void testEliminarReview() {
        when(repository.existsById(1)).thenReturn(true);
        doNothing().when(repository).deleteById(1);

        service.eliminarReview(1);

        verify(repository, times(1)).existsById(1);
        verify(repository, times(1)).deleteById(1);
    }

    @Test
    @DisplayName("Adicional: Crear Review")
    void testCrearReview() {
        when(repository.save(any(Review.class))).thenReturn(review);

        Review resultado = service.crearReview(review);

        assertNotNull(resultado);
        assertEquals("Excelente pizza", resultado.getComment());
        verify(repository, times(1)).save(review);
    }
}
