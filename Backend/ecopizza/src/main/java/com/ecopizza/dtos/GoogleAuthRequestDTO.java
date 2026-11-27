package com.ecopizza.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleAuthRequestDTO(
        String credential,
        String idToken,
        String accessToken,
        String email,
        String name
) {}
