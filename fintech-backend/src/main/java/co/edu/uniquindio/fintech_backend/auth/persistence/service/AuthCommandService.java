package co.edu.uniquindio.fintech_backend.auth.persistence.service;

import co.edu.uniquindio.fintech_backend.auth.model.Credential;
import co.edu.uniquindio.fintech_backend.auth.repository.CredentialRepository;
import co.edu.uniquindio.fintech_backend.auth.service.AuthCommands;
import co.edu.uniquindio.fintech_backend.auth.service.PasswordHasher;
import co.edu.uniquindio.fintech_backend.persistence.coordination.PersistenceOperationCoordinator;
import co.edu.uniquindio.fintech_backend.persistence.event.EventType;
import co.edu.uniquindio.fintech_backend.persistence.event.EventWriter;
import co.edu.uniquindio.fintech_backend.user.model.User;
import co.edu.uniquindio.fintech_backend.user.persistence.event.UserEventMapper;
import co.edu.uniquindio.fintech_backend.user.repository.UserRepository;
import co.edu.uniquindio.fintech_backend.user.service.UserAlreadyExistsException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class AuthCommandService implements AuthCommands {
    private final UserRepository userRepository;
    private final CredentialRepository credentialRepository;
    private final PasswordHasher passwordHasher;
    private final EventWriter eventWriter;
    private final PersistenceOperationCoordinator persistenceCoordinator;

    public AuthCommandService(UserRepository userRepository, CredentialRepository credentialRepository, PasswordHasher passwordHasher, EventWriter eventWriter, PersistenceOperationCoordinator persistenceCoordinator) {
        this.userRepository = Objects.requireNonNull(userRepository, "User repository is required");
        this.credentialRepository = Objects.requireNonNull(credentialRepository, "Credential repository is required");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "Password hasher is required");
        this.eventWriter = Objects.requireNonNull(eventWriter, "Event writer is required");
        this.persistenceCoordinator = Objects.requireNonNull(persistenceCoordinator, "Persistence coordinator is required");
    }

    @Override
    public User register(String identification, String firstName, String lastName, String email, String password) {
        User user = new User(identification, firstName, lastName, email);
        Credential credential = new Credential(user.getId(), passwordHasher.hash(password));
        return persistenceCoordinator.execute(() -> {
            if (userRepository.findByIdentification(user.getIdentification()).isPresent()) {
                throw new UserAlreadyExistsException("Identification already registered");
            }
            if (userRepository.findByEmail(user.getEmail()).isPresent()) {
                throw new UserAlreadyExistsException("Email already registered");
            }
            Map<String, String> data = new HashMap<>(UserEventMapper.toData(user));
            data.put("passwordHash", credential.passwordHash());
            eventWriter.record(EventType.ACCOUNT_REGISTERED, data);
            userRepository.save(user);
            credentialRepository.save(credential);
            return new User(user);
        });
    }
}
