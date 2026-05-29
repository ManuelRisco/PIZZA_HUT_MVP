package com.example.infrastructure.persistence.repositories;

import com.example.domain.model.Address;
import com.example.domain.repository.AddressRepository;
import com.example.infrastructure.persistence.entities.AddressEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@SuppressWarnings("null")
public class JpaAddressRepository implements AddressRepository {

    private final SpringDataAddressRepository springDataRepository;

    @Override
    public List<Address> findAll() {
        return springDataRepository.findAll()
                .stream()
                .map(AddressEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Address> findById(Integer id) {
        return springDataRepository.findById(id)
                .map(AddressEntity::toDomain);
    }

    @Override
    public List<Address> findByUserId(Integer userId) {
        return springDataRepository.findByUserId(userId)
                .stream()
                .map(AddressEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsById(Integer id) {
        return springDataRepository.existsById(id);
    }

    @Override
    public Address save(Address address) {
        AddressEntity entity = AddressEntity.fromDomain(address);
        AddressEntity saved = springDataRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public void deleteById(Integer id) {
        springDataRepository.deleteById(id);
    }
}
