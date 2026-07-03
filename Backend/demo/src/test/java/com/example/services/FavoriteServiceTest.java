package com.example.services;

import com.example.models.Favorite;
import com.example.repositories.FavoriteRepository;
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

@DisplayName("Adicional.1: Gestión de Favoritos - Pruebas TDD")
class FavoriteServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @InjectMocks
    private FavoriteService favoriteService;

    private Favorite favorite;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        favorite = new Favorite();
    }

    // Fase RED / GREEN / REFACTOR
    @Test
    @DisplayName("Adicional.2: Listar favoritos")
    void testListarFavorites() {
        List<Favorite> list = new ArrayList<>();
        list.add(favorite);
        when(favoriteRepository.findAll()).thenReturn(list);

        List<Favorite> resultado = favoriteService.listarFavorites();

        assertEquals(1, resultado.size());
        verify(favoriteRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Adicional.3: Crear favorito")
    void testCrearFavorite() {
        when(favoriteRepository.existsById(any())).thenReturn(false);
        when(favoriteRepository.save(any(Favorite.class))).thenReturn(favorite);
        Favorite resultado = favoriteService.crearFavorite(favorite);
        assertNotNull(resultado);
        verify(favoriteRepository, times(1)).save(favorite);
    }

    @Test
    @DisplayName("Adicional.4: Obtener por ID")
    void testObtenerPorId() {
        when(favoriteRepository.findById(any())).thenReturn(Optional.of(favorite));
        Optional<Favorite> result = favoriteService.obtenerPorId(null);
        assertTrue(result.isPresent());
        verify(favoriteRepository, times(1)).findById(any());
    }

    @Test
    @DisplayName("Adicional.5: Eliminar favorito")
    void testEliminar() {
        when(favoriteRepository.existsById(any())).thenReturn(true);
        doNothing().when(favoriteRepository).deleteById(any());
        favoriteService.eliminarFavorite(null);
        verify(favoriteRepository, times(1)).existsById(any());
        verify(favoriteRepository, times(1)).deleteById(any());
    }
}
