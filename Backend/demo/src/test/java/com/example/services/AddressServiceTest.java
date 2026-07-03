package com.example.services;

import com.example.models.Address;
import com.example.repositories.AddressRepository;
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

@DisplayName("Adicional.1: Gestión de Direcciones - Pruebas TDD")
class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;

    @InjectMocks
    private AddressService addressService;

    private Address address;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        address = new Address();
        address.setId(1);
        address.setUserId(1);
        address.setLine1("Calle Falsa 123");
    }

    // Fase RED / GREEN / REFACTOR aplicadas
    @Test
    @DisplayName("Adicional.2: Listar direcciones")
    void testListarAddresses() {
        List<Address> list = new ArrayList<>();
        list.add(address);
        when(addressRepository.findAll()).thenReturn(list);

        List<Address> resultado = addressService.listarAddresses();

        assertEquals(1, resultado.size());
        verify(addressRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Adicional.3: Crear dirección")
    void testCrearAddress() {
        when(addressRepository.save(any(Address.class))).thenReturn(address);
        Address resultado = addressService.crearAddress(address);
        assertNotNull(resultado);
        verify(addressRepository, times(1)).save(address);
    }

    @Test
    @DisplayName("Adicional.4: Obtener por ID")
    void testObtenerPorId() {
        when(addressRepository.findById(1)).thenReturn(Optional.of(address));
        Optional<Address> result = addressService.obtenerPorId(1);
        assertTrue(result.isPresent());
        verify(addressRepository, times(1)).findById(1);
    }

    @Test
    @DisplayName("Adicional.5: Actualizar Address")
    void testActualizarAddress() {
        when(addressRepository.existsById(1)).thenReturn(true);
        when(addressRepository.save(any(Address.class))).thenReturn(address);
        Address result = addressService.actualizarAddress(1, address);
        assertNotNull(result);
        verify(addressRepository, times(1)).existsById(1);
        verify(addressRepository, times(1)).save(address);
    }

    @Test
    @DisplayName("Adicional.6: Eliminar Address")
    void testEliminarAddress() {
        when(addressRepository.existsById(1)).thenReturn(true);
        doNothing().when(addressRepository).deleteById(1);
        addressService.eliminarAddress(1);
        verify(addressRepository, times(1)).existsById(1);
        verify(addressRepository, times(1)).deleteById(1);
    }
}
