package co.edu.uniquindio.fintech_backend.auth.persistence.event;

import co.edu.uniquindio.fintech_backend.auth.model.Credential;
import co.edu.uniquindio.fintech_backend.auth.repository.CredentialRepository;
import co.edu.uniquindio.fintech_backend.persistence.event.EventHandler;
import co.edu.uniquindio.fintech_backend.persistence.event.EventType;
import co.edu.uniquindio.fintech_backend.persistence.event.StoredEvent;
import co.edu.uniquindio.fintech_backend.user.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Component
public class CredentialEventHandler implements EventHandler {
    private static final Set<EventType> SUPPORTED_TYPES = Set.of(EventType.CREDENTIAL_CREATED, EventType.PASSWORD_CHANGED);
    private final UserRepository userRepository;
    private final CredentialRepository credentialRepository;

    public CredentialEventHandler(UserRepository userRepository, CredentialRepository credentialRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "User repository is required");
        this.credentialRepository = Objects.requireNonNull(credentialRepository, "Credential repository is required");
    }

    @Override
    public Set<EventType> supportedTypes() {
        return SUPPORTED_TYPES;
    }

    @Override
    public void apply(StoredEvent event) {
        Objects.requireNonNull(event, "Event is required");
        if (!SUPPORTED_TYPES.contains(event.type())) {
            throw new IllegalArgumentException("Unsupported credential event: " + event.type());
        }
        Credential credential = CredentialEventMapper.toCredential(event.data());
        if (userRepository.findById(credential.userId()).isEmpty()) {
            throw new IllegalStateException("Credential user does not exist");
        }
        Optional<Credential> previous = credentialRepository.findByUserId(credential.userId());
        if (event.type() == EventType.CREDENTIAL_CREATED && previous.isPresent()) {
            throw new IllegalStateException("Credential already exists");
        }
        if (event.type() == EventType.PASSWORD_CHANGED && previous.isEmpty()) {
            throw new IllegalStateException("Credential does not exist");
        }
        credentialRepository.save(credential);
    }
}
