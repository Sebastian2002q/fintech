package co.edu.uniquindio.fintech_backend.user.persistence.event;

import co.edu.uniquindio.fintech_backend.user.model.User;
import co.edu.uniquindio.fintech_backend.user.model.Role;
import co.edu.uniquindio.fintech_backend.user.model.UserStatus;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class UserEventMapper {
    private UserEventMapper() {
    }

    public static Map<String, String> toData(User user) {
        Objects.requireNonNull(user, "User is required");
        return Map.of("id", user.getId().toString(), "identification", user.getIdentification(), "firstName", user.getFirstName(), "lastName", user.getLastName(), "email", user.getEmail(), "role", user.getRole().name(), "status", user.getStatus().name(), "createdAt", user.getCreatedAt().toString());
    }

    public static User toUser(Map<String, String> data) {
        Objects.requireNonNull(data, "Event data is required");
        return User.restore(UUID.fromString(requireField(data, "id")), requireField(data, "identification"), requireField(data, "firstName"), requireField(data, "lastName"), requireField(data, "email"), Role.valueOf(requireField(data, "role")), UserStatus.valueOf(requireField(data, "status")), Instant.parse(requireField(data, "createdAt")));
    }

    private static String requireField(Map<String, String> data, String key) {
        String value = data.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing or blank event field: " + key);
        }
        return value;
    }
}
