package com.ecopizza.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record PasswordResetRequestResultDTO(
        boolean emailSent,
        String maskedEmail,
        int expirationMinutes,
        @JsonIgnore String message
) {}
