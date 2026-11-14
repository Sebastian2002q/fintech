package co.edu.uniquindio.fintech_backend.auth.persistence.snapshot;

import co.edu.uniquindio.fintech_backend.auth.model.Credential;

import java.util.List;
import java.util.Objects;

public record CredentialSnapshotState(List<Credential> credentials) {
    public CredentialSnapshotState {
        Objects.requireNonNull(credentials, "Credentials are required");
        credentials = List.copyOf(credentials);
    }
}
