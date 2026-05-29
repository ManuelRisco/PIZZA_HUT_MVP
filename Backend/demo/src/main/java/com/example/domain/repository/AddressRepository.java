package com.example.domain.repository;

import com.example.domain.model.Address;
import java.util.List;
import java.util.Optional;

public interface AddressRepository {
    List<Address> findAll();
    Optional<Address> findById(Integer id);
    List<Address> findByUserId(Integer userId);
    boolean existsById(Integer id);
    Address save(Address address);
    void deleteById(Integer id);
}
