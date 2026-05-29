package com.example.infrastructure.controller;

import com.example.service.AddressService;
import com.example.domain.dto.AddressDTO;
import com.example.domain.model.Address;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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

    @Autowired
    private AddressService addressService;

    @GetMapping
    public ResponseEntity<List<AddressDTO>> listarAddresses() {
        List<Address> addresses = addressService.listarAddresses();
        List<AddressDTO> addressesDTO = addresses.stream()
            .map(AddressDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(addressesDTO);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerAddressPorId(@PathVariable Integer id) {
        Optional<Address> addressOpt = addressService.obtenerPorId(id);
        if (addressOpt.isPresent()) {
            AddressDTO addressDTO = new AddressDTO(addressOpt.get());
            return ResponseEntity.ok(addressDTO);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Address no encontrado"));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AddressDTO>> obtenerAddressesPorUserId(@PathVariable Integer userId) {
        List<Address> addresses = addressService.obtenerPorUserId(userId);
        List<AddressDTO> addressesDTO = addresses.stream()
            .map(AddressDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(addressesDTO);
    }

    @PostMapping
    public ResponseEntity<?> crearAddress(@RequestBody AddressDTO addressDTO) {
        try {
            Address address = new Address();
            address.setUserId(addressDTO.getUserId());
            address.setLine1(addressDTO.getLine1());
            address.setCity(addressDTO.getCity());
            address.setDistrict(addressDTO.getDistrict());
            address.setReference(addressDTO.getReference());
            address.setIsDefault(addressDTO.getIsDefault());
            
            Address addressCreado = addressService.crearAddress(address);
            return ResponseEntity.status(HttpStatus.CREATED).body(new AddressDTO(addressCreado));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarAddress(@PathVariable Integer id, @RequestBody AddressDTO addressDTO) {
        try {
            Address address = new Address();
            address.setUserId(addressDTO.getUserId());
            address.setLine1(addressDTO.getLine1());
            address.setCity(addressDTO.getCity());
            address.setDistrict(addressDTO.getDistrict());
            address.setReference(addressDTO.getReference());
            address.setIsDefault(addressDTO.getIsDefault());
            
            Address addressActualizado = addressService.actualizarAddress(id, address);
            return ResponseEntity.ok(new AddressDTO(addressActualizado));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarAddress(@PathVariable Integer id) {
        try {
            addressService.eliminarAddress(id);
            return ResponseEntity.ok(Map.of("message", "Address eliminado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", e.getMessage()));
        }
    }
}
