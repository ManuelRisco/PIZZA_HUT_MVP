package com.example.service;

import com.example.domain.model.Address;
import com.example.domain.repository.AddressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AddressService {

    @Autowired
    private AddressRepository addressRepository;

    public List<Address> listarAddresses() {
        return addressRepository.findAll();
    }

    public Optional<Address> obtenerPorId(Integer id) {
        return addressRepository.findById(id);
    }

    public List<Address> obtenerPorUserId(Integer userId) {
        return addressRepository.findByUserId(userId);
    }

    public Address crearAddress(Address address) {
        return addressRepository.save(address);
    }

    public Address actualizarAddress(Integer id, Address address) {
        if (!addressRepository.existsById(id)) {
            throw new IllegalArgumentException("Address no encontrado");
        }
        address.setId(id);
        return addressRepository.save(address);
    }

    public void eliminarAddress(Integer id) {
        if (!addressRepository.existsById(id)) {
            throw new IllegalArgumentException("Address no encontrado");
        }
        addressRepository.deleteById(id);
    }
}
