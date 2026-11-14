package co.edu.uniquindio.fintech_backend.auth.persistence.event;

import co.edu.uniquindio.fintech_backend.auth.model.Credential;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class CredentialEventMapper {
    private CredentialEventMapper() {
    }

    public static Map<String, String> toData(Credential credential) {
        Objects.requireNonNull(credential, "Credential is required");
        return Map.of("userId", credential.userId().toString(), "passwordHash", credential.passwordHash());
    }

    public static Credential toCredential(Map<String, String> data) {
        Objects.requireNonNull(data, "Event data is required");
        return new Credential(UUID.fromString(requireField(data, "userId")), requireField(data, "passwordHash"));
    }

    private static String requireField(Map<String, String> data, String key) {
        String value = data.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing or blank credential field: " + key);
        }
        return value;
    }
}
