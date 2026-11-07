package co.edu.uniquindio.fintech_backend.user.dto;

import co.edu.uniquindio.fintech_backend.user.model.Role;
import co.edu.uniquindio.fintech_backend.user.model.User;
import co.edu.uniquindio.fintech_backend.user.model.UserStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record UserResponse(UUID id, String identification, String firstName, String lastName, String email, Role role,
                           UserStatus status, Instant createdAt) {
    public static UserResponse from(User user) {
        Objects.requireNonNull(user, "User is required");
        return new UserResponse(user.getId(), user.getIdentification(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getRole(), user.getStatus(), user.getCreatedAt());
    }
}
