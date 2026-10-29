package co.edu.uniquindio.fintech_backend.user.persistence.snapshot;

import co.edu.uniquindio.fintech_backend.user.model.User;

import java.util.Objects;

public final class UserSnapshotMapper {

    private UserSnapshotMapper() {
    }

    public static UserSnapshotEntry toEntry(User user) {
        Objects.requireNonNull(user, "User is required");
        return new UserSnapshotEntry(user.getId(), user.getIdentification(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getRole(), user.getStatus(), user.getCreatedAt());
    }

    public static User toUser(UserSnapshotEntry entry) {
        Objects.requireNonNull(entry, "Snapshot entry is required");
        return User.restore(entry.id(), entry.identification(), entry.firstName(), entry.lastName(), entry.email(), entry.role(), entry.status(), entry.createdAt());
    }
}
