package com.example.services;

import com.example.models.Address;
import com.example.repositories.AddressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AddressService {

    private final AddressRepository addressRepository;

    public AddressService(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    public List<Address> listarAddresses() {
        return addressRepository.findAll();
    }

    @SuppressWarnings("null")
    public Optional<Address> obtenerPorId(Integer id) {
        return addressRepository.findById(id);
    }

    public List<Address> obtenerPorUserId(Integer userId) {
        return addressRepository.findByUserId(userId);
    }

    @SuppressWarnings("null")
    public Address crearAddress(Address address) {
        return addressRepository.save(address);
    }

    @SuppressWarnings("null")
    public Address actualizarAddress(Integer id, Address address) {
        if (!addressRepository.existsById(id)) {
            throw new IllegalArgumentException("Address no encontrado");
        }
        address.setId(id);
        return addressRepository.save(address);
    }

    @SuppressWarnings("null")
    public void eliminarAddress(Integer id) {
        if (!addressRepository.existsById(id)) {
            throw new IllegalArgumentException("Address no encontrado");
        }
        addressRepository.deleteById(id);
    }
}
