package com.ecopizza.dtos;

public record FileUploadResponseDTO(
        String imageUrl,
        String thumbnailUrl
) {}
