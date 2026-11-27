package com.ecopizza.repositories;

import com.ecopizza.models.Address;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<Address, Integer> {
    List<Address> findByUserId(Integer userId);
    List<Address> findByUserIdAndDeletedAtIsNull(Integer userId);
    Optional<Address> findByIdAndDeletedAtIsNull(Integer id);
}
