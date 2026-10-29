package co.edu.uniquindio.fintech_backend.user.persistence.snapshot;

import java.util.List;
import java.util.Objects;

public record UserSnapshotState(List<UserSnapshotEntry> users) {
    public UserSnapshotState {
        Objects.requireNonNull(users, "Users are required");
        users = List.copyOf(users);
    }
}
