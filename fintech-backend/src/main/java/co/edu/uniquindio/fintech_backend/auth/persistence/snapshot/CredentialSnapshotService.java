package co.edu.uniquindio.fintech_backend.auth.persistence.snapshot;

import co.edu.uniquindio.fintech_backend.auth.repository.CredentialRepository;
import co.edu.uniquindio.fintech_backend.persistence.snapshot.SnapshotParticipant;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class CredentialSnapshotService implements SnapshotParticipant<CredentialSnapshotState> {
    private static final String MODULE_KEY = "credentials";
    private final CredentialRepository credentialRepository;

    public CredentialSnapshotService(CredentialRepository credentialRepository) {
        this.credentialRepository = Objects.requireNonNull(credentialRepository, "Credential repository is required");
    }

    @Override
    public String moduleKey() {
        return MODULE_KEY;
    }

    @Override
    public Class<CredentialSnapshotState> stateType() {
        return CredentialSnapshotState.class;
    }

    @Override
    public CredentialSnapshotState capture() {
        return new CredentialSnapshotState(java.util.List.copyOf(credentialRepository.findAll()));
    }

    @Override
    public void restore(CredentialSnapshotState state) {
        Objects.requireNonNull(state, "Snapshot state is required");
        credentialRepository.replaceAll(state.credentials());
    }
}
