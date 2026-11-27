package com.ecopizza.dtos;

public record GoogleIdentityPayloadDTO(
        String email,
        String name,
        String sub,
        boolean verified
) {}
