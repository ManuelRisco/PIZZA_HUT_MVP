package com.example.dtos;

public record GoogleIdentityPayloadDTO(
        String email,
        String name,
        String sub,
        boolean verified
) {}
