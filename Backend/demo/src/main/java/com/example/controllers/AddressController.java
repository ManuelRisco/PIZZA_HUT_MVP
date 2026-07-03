package com.example.controllers;

import com.example.services.AddressService;
import com.example.dtos.AddressDTO;
import com.example.models.Address;
import com.example.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import com.example.dtos.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/addresses")
@CrossOrigin(origins = "http://localhost:4200")
public class AddressController {

    private final AddressService addressService;
    private final SecurityUtils securityUtils;

    public AddressController(AddressService addressService, SecurityUtils securityUtils) {
        this.addressService = addressService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressDTO>>> listarAddresses() {
        List<Address> addresses;
        if (securityUtils.isAdmin()) {
            addresses = addressService.listarAddresses();
        } else {
            Integer userId = securityUtils.getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            addresses = addressService.obtenerPorUserId(userId);
        }
        
        List<AddressDTO> addressesDTO = addresses.stream()
            .map(AddressDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(addressesDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerAddressPorId(@PathVariable("id") Integer id) {
        Optional<Address> addressOpt = addressService.obtenerPorId(id);
        if (addressOpt.isPresent()) {
            Address address = addressOpt.get();
            if (!securityUtils.isAdmin()) {
                Integer currentUserId = securityUtils.getCurrentUserId();
                if (currentUserId == null || !address.getUserId().equals(currentUserId)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "No tienes permiso para ver esta dirección"));
                }
            }
            AddressDTO addressDTO = new AddressDTO(address);
            return ResponseEntity.ok(ApiResponse.success(addressDTO));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Address no encontrado"));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> obtenerAddressesPorUserId(@PathVariable("userId") Integer userId) {
        if (!securityUtils.isAdmin()) {
            Integer currentUserId = securityUtils.getCurrentUserId();
            if (currentUserId == null || !userId.equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "No tienes permiso para ver las direcciones de este usuario"));
            }
        }
        List<Address> addresses = addressService.obtenerPorUserId(userId);
        List<AddressDTO> addressesDTO = addresses.stream()
            .map(AddressDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(addressesDTO));
    }

    @PostMapping
    public ResponseEntity<?> crearAddress(@Valid @RequestBody AddressDTO addressDTO) {
        try {
            if (!securityUtils.isAdmin()) {
                Integer currentUserId = securityUtils.getCurrentUserId();
                if (currentUserId == null || !addressDTO.getUserId().equals(currentUserId)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "No tienes permiso para crear una dirección para otro usuario"));
                }
            }
            Address address = new Address();
            address.setUserId(addressDTO.getUserId());
            address.setLine1(addressDTO.getLine1());
            address.setCity(addressDTO.getCity());
            address.setDistrict(addressDTO.getDistrict());
            address.setReference(addressDTO.getReference());
            address.setIsDefault(addressDTO.getIsDefault());
            
            Address addressCreado = addressService.crearAddress(address);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(new AddressDTO(addressCreado), "Creado exitosamente"));
        } catch (RuntimeException e) { throw e; } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarAddress(@PathVariable("id") Integer id, @Valid @RequestBody AddressDTO addressDTO) {
        try {
            Optional<Address> existingOpt = addressService.obtenerPorId(id);
            if (existingOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Address no encontrado"));
            }
            if (!securityUtils.isAdmin()) {
                Integer currentUserId = securityUtils.getCurrentUserId();
                if (currentUserId == null || !existingOpt.get().getUserId().equals(currentUserId)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "No tienes permiso para modificar esta dirección"));
                }
            }
            Address address = new Address();
            address.setUserId(addressDTO.getUserId());
            address.setLine1(addressDTO.getLine1());
            address.setCity(addressDTO.getCity());
            address.setDistrict(addressDTO.getDistrict());
            address.setReference(addressDTO.getReference());
            address.setIsDefault(addressDTO.getIsDefault());
            
            Address addressActualizado = addressService.actualizarAddress(id, address);
            return ResponseEntity.ok(ApiResponse.success(new AddressDTO(addressActualizado)));
        } catch (RuntimeException e) { throw e; } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarAddress(@PathVariable("id") Integer id) {
        try {
            Optional<Address> existingOpt = addressService.obtenerPorId(id);
            if (existingOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Address no encontrado"));
            }
            if (!securityUtils.isAdmin()) {
                Integer currentUserId = securityUtils.getCurrentUserId();
                if (currentUserId == null || !existingOpt.get().getUserId().equals(currentUserId)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "No tienes permiso para eliminar esta dirección"));
                }
            }
            addressService.eliminarAddress(id);
            return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Address eliminado correctamente")));
        } catch (RuntimeException e) { throw e; } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", e.getMessage()));
        }
    }
}
