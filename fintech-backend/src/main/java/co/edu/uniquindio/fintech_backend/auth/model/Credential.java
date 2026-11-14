package co.edu.uniquindio.fintech_backend.auth.model;

import java.util.Objects;
import java.util.UUID;

public record Credential(UUID userId, String passwordHash) {
    public Credential {
        Objects.requireNonNull(userId, "User ID is required");
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash is required");
        }
    }
}

