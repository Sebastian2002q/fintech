package co.edu.uniquindio.fintech_backend.user.persistence.service;

import co.edu.uniquindio.fintech_backend.persistence.coordination.PersistenceOperationCoordinator;
import co.edu.uniquindio.fintech_backend.persistence.event.EventType;
import co.edu.uniquindio.fintech_backend.persistence.event.EventWriter;
import co.edu.uniquindio.fintech_backend.user.model.User;
import co.edu.uniquindio.fintech_backend.user.persistence.event.UserEventMapper;
import co.edu.uniquindio.fintech_backend.user.repository.UserRepository;
import co.edu.uniquindio.fintech_backend.user.service.UserAlreadyExistsException;
import co.edu.uniquindio.fintech_backend.user.service.UserCommands;
import org.springframework.stereotype.Service;

import java.util.Objects;

import co.edu.uniquindio.fintech_backend.user.service.UserAlreadyExistsException;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class UserCommandService implements UserCommands {
    private final UserRepository userRepository;
    private final EventWriter eventWriter;
    private final PersistenceOperationCoordinator persistenceCoordinator;

    public UserCommandService(UserRepository userRepository, EventWriter eventWriter, PersistenceOperationCoordinator persistenceCoordinator) {
        this.userRepository = Objects.requireNonNull(userRepository, "User repository is required");
        this.eventWriter = Objects.requireNonNull(eventWriter, "Event writer is required");
        this.persistenceCoordinator = Objects.requireNonNull(persistenceCoordinator, "Persistence coordinator is required");
    }

    @Override
    public User register(String identification, String firstName, String lastName, String email) {
        return persistenceCoordinator.execute(() -> {
            User user = new User(identification, firstName, lastName, email);
            if (userRepository.findByIdentification(user.getIdentification()).isPresent()) {
                throw new UserAlreadyExistsException("Identification already registered");
            }
            if (userRepository.findByEmail(user.getEmail()).isPresent()) {
                throw new UserAlreadyExistsException("Email already registered");
            }
            eventWriter.record(EventType.USER_REGISTERED, UserEventMapper.toData(user));
            userRepository.save(user);
            return new User(user);
        });
    }

    @Override
    public User updateProfile(UUID id, String firstName, String lastName, String email) {
        return persistenceCoordinator.execute(() -> {
            User user = userRepository.findById(id).orElseThrow(() -> new NoSuchElementException("User not found"));
            user.updateProfile(firstName, lastName, email);
            if (userRepository.findByEmail(user.getEmail()).filter(existing -> !existing.getId().equals(id)).isPresent()) {
                throw new UserAlreadyExistsException("Email already registered");
            }
            eventWriter.record(EventType.USER_UPDATED, UserEventMapper.toData(user));
            userRepository.save(user);
            return new User(user);
        });
    }

}
