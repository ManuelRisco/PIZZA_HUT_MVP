package com.ecopizza.services;

import com.ecopizza.models.Address;
import com.ecopizza.repositories.AddressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class AddressService {

    private final AddressRepository addressRepository;

    public AddressService(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    public List<Address> listarAddresses() {
        return addressRepository.findAll().stream()
                .filter(a -> a.getDeletedAt() == null)
                .toList();
    }

    public Optional<Address> obtenerPorId(Integer id) {
        if (id == null) {
            return Optional.empty();
        }
        return addressRepository.findById(id).filter(a -> a.getDeletedAt() == null);
    }

    public List<Address> obtenerPorUserId(Integer userId) {
        if (userId == null) {
            return List.of();
        }
        List<Address> raw = addressRepository.findByUserIdAndDeletedAtIsNull(userId);
        // Garantizar que nunca se devuelvan direcciones duplicadas al cliente
        Map<String, Address> uniqueBySignature = new LinkedHashMap<>();
        for (Address addr : raw) {
            String key = buildNormalizedKey(addr.getLine1(), addr.getCity(), addr.getDistrict());
            uniqueBySignature.putIfAbsent(key, addr);
        }
        return new ArrayList<>(uniqueBySignature.values());
    }

    /**
     * Crea o reutiliza una dirección existente del usuario si ya tiene la misma
     * combinación normalizada de (line1, city, district), evitando duplicados en la BD.
     */
    public Address crearAddress(Address address) {
        normalizeFields(address);

        if (address.getUserId() != null) {
            String targetKey = buildNormalizedKey(address.getLine1(), address.getCity(), address.getDistrict());
            List<Address> existingList = addressRepository.findByUserIdAndDeletedAtIsNull(address.getUserId());
            for (Address existing : existingList) {
                String existingKey = buildNormalizedKey(existing.getLine1(), existing.getCity(), existing.getDistrict());
                if (targetKey.equals(existingKey)) {
                    boolean changed = false;
                    if (address.getReference() != null && !address.getReference().isBlank()
                            && !address.getReference().equals(existing.getReference())) {
                        existing.setReference(address.getReference());
                        changed = true;
                    }
                    if (Boolean.TRUE.equals(address.getIsDefault()) && !Boolean.TRUE.equals(existing.getIsDefault())) {
                        existing.setIsDefault(true);
                        changed = true;
                    }
                    return changed ? addressRepository.save(existing) : existing;
                }
            }
        }

        return addressRepository.save(address);
    }

    public Address actualizarAddress(Integer id, Address address) {
        if (!addressRepository.existsById(id)) {
            throw new IllegalArgumentException("Address no encontrado");
        }
        normalizeFields(address);
        address.setId(id);
        return addressRepository.save(address);
    }

    /**
     * Eliminación Lógica (Soft Delete).
     * Mantiene integridad de historial de entregas pasadas.
     */
    public void eliminarAddress(Integer id) {
        Optional<Address> addressOpt = addressRepository.findById(id);
        if (addressOpt.isEmpty()) {
            throw new IllegalArgumentException("Address no encontrado");
        }
        Address address = addressOpt.get();
        address.setDeletedAt(LocalDateTime.now());
        addressRepository.save(address);
    }

    private void normalizeFields(Address address) {
        if (address == null) return;
        if (address.getLine1() != null) {
            address.setLine1(address.getLine1().trim().replaceAll("\\s+", " "));
        }
        if (address.getCity() != null) {
            address.setCity(address.getCity().trim().replaceAll("\\s+", " "));
        } else {
            address.setCity("Lima");
        }
        if (address.getDistrict() != null) {
            address.setDistrict(address.getDistrict().trim().replaceAll("\\s+", " "));
        }
        if (address.getReference() != null) {
            String ref = address.getReference().trim();
            address.setReference(ref.isEmpty() ? null : ref);
        }
        if (address.getIsDefault() == null) {
            address.setIsDefault(false);
        }
    }

    private String buildNormalizedKey(String line1, String city, String district) {
        String l = line1 != null ? line1.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT) : "";
        String c = city != null ? city.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT) : "";
        String d = district != null ? district.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT) : "";
        return l + "|" + c + "|" + d;
    }
}
