package com.example.repositories;

import com.example.models.Address;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<Address, Integer> {
List<Address> findByUserId(Integer userId);
}


