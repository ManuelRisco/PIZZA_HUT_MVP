package com.ecopizza.dtos;

public record GoogleConfigDTO(
        String clientId,
        boolean googleCloudConfigured,
        boolean clientSecretConfigured,
        boolean allowQuickGoogleSignIn
) {}
