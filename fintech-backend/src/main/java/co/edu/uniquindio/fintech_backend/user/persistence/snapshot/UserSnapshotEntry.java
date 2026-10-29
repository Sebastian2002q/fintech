
package co.edu.uniquindio.fintech_backend.user.persistence.snapshot;

import co.edu.uniquindio.fintech_backend.user.model.Role;
import co.edu.uniquindio.fintech_backend.user.model.UserStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record UserSnapshotEntry(
        UUID id,
        String identification,
        String firstName,
        String lastName,
        String email,
        Role role,
        UserStatus status,
        Instant createdAt
) {

    public UserSnapshotEntry {
        Objects.requireNonNull(id, "User ID is required");
        Objects.requireNonNull(identification, "Identification is required");
        Objects.requireNonNull(firstName, "First name is required");
        Objects.requireNonNull(lastName, "Last name is required");
        Objects.requireNonNull(email, "Email is required");
        Objects.requireNonNull(role, "Role is required");
        Objects.requireNonNull(status, "Status is required");
        Objects.requireNonNull(createdAt, "Creation date is required");
    }
}
