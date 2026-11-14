package co.edu.uniquindio.fintech_backend.auth.persistence.event;

import co.edu.uniquindio.fintech_backend.auth.model.Credential;
import co.edu.uniquindio.fintech_backend.auth.repository.CredentialRepository;
import co.edu.uniquindio.fintech_backend.persistence.event.EventHandler;
import co.edu.uniquindio.fintech_backend.persistence.event.EventType;
import co.edu.uniquindio.fintech_backend.persistence.event.StoredEvent;
import co.edu.uniquindio.fintech_backend.user.model.User;
import co.edu.uniquindio.fintech_backend.user.persistence.event.UserEventMapper;
import co.edu.uniquindio.fintech_backend.user.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

@Component
public class AccountRegistrationEventHandler implements EventHandler {
    private final UserRepository userRepository;
    private final CredentialRepository credentialRepository;

    public AccountRegistrationEventHandler(UserRepository userRepository, CredentialRepository credentialRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "User repository is required");
        this.credentialRepository = Objects.requireNonNull(credentialRepository, "Credential repository is required");
    }

    @Override
    public Set<EventType> supportedTypes() {
        return Set.of(EventType.ACCOUNT_REGISTERED);
    }

    @Override
    public void apply(StoredEvent event) {
        Objects.requireNonNull(event, "Event is required");
        if (event.type() != EventType.ACCOUNT_REGISTERED) {
            throw new IllegalArgumentException("Unsupported account event");
        }
        User user = UserEventMapper.toUser(event.data());
        Credential credential = new Credential(user.getId(), event.data().get("passwordHash"));
        if (userRepository.findById(user.getId()).isPresent() || userRepository.findByIdentification(user.getIdentification()).isPresent() || userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalStateException("Account user already exists");
        }
        if (credentialRepository.findByUserId(user.getId()).isPresent()) {
            throw new IllegalStateException("Account credential already exists");
        }
        userRepository.save(user);
        credentialRepository.save(credential);
    }
}
