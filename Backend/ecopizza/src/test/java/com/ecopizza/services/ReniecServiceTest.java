package com.ecopizza.services;

import com.ecopizza.dtos.DniLookupDTO;
import com.ecopizza.exceptions.BadRequestException;
import com.ecopizza.models.Usuario;
import com.ecopizza.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReniecService - Pruebas unitarias de consulta y configuración de DNI")
class ReniecServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private UsuarioRepository usuarioRepository;

    private ReniecService reniecService;

    @BeforeEach
    void setUp() {
        reniecService = new ReniecService(jdbcTemplate, usuarioRepository);
    }

    @Test
    @DisplayName("Debe rechazar DNI inválido con menos o más de 8 dígitos")
    void consultarDniInvalido() {
        assertThrows(BadRequestException.class, () -> reniecService.consultarDni("1234"));
        assertThrows(BadRequestException.class, () -> reniecService.consultarDni("ABCDEFGH"));
        assertThrows(BadRequestException.class, () -> reniecService.consultarDni(null));
    }

    @Test
    @DisplayName("Debe devolver DNI desde caché SQL y luego desde caché en memoria")
    @SuppressWarnings("unchecked")
    void consultarDniDesdeCache() {
        DniLookupDTO cached = new DniLookupDTO(
                "76949746",
                "MANUEL FABRIZZIO",
                "RISCO",
                "GIL",
                "MANUEL FABRIZZIO RISCO GIL",
                "CACHE_RENIEC"
        );
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("76949746")))
                .thenReturn(List.of(cached));

        DniLookupDTO first = reniecService.consultarDni(" 76949746 ");
        DniLookupDTO second = reniecService.consultarDni("76949746");

        assertEquals("76949746", first.dni());
        assertEquals("MANUEL FABRIZZIO RISCO GIL", first.nombreCompleto());
        assertEquals("CACHE_RENIEC", first.source());
        assertSame(first, second);
        verify(jdbcTemplate, times(1)).query(anyString(), any(RowMapper.class), eq("76949746"));
    }

    @Test
    @DisplayName("Debe guardar DNI, verificación de edad y nombre sin conflictos")
    void configurarDniUsuarioExitoso() {
        Usuario user = new Usuario();
        user.setId(5L);
        user.setName("Antiguo Nombre");

        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(user));
        when(usuarioRepository.findAll()).thenReturn(List.of(user));
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario updated = reniecService.configurarDniUsuario(5L, "76949746", true, "Manuel Risco");

        assertEquals("76949746", updated.getDni());
        assertTrue(Boolean.TRUE.equals(updated.getAgeVerified()));
        assertEquals("Manuel Risco", updated.getName());
        verify(usuarioRepository).saveAndFlush(user);
    }

    @Test
    @DisplayName("Debe exigir confirmación de mayoría de edad al configurar DNI")
    void configurarDniRequiereMayoriaEdad() {
        assertThrows(BadRequestException.class,
                () -> reniecService.configurarDniUsuario(5L, "76949746", false, "Manuel"));
        verifyNoInteractions(usuarioRepository);
    }
}
