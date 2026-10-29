package co.edu.uniquindio.fintech_backend.user.persistence.event;

import co.edu.uniquindio.fintech_backend.persistence.event.EventHandler;
import co.edu.uniquindio.fintech_backend.persistence.event.EventType;
import co.edu.uniquindio.fintech_backend.persistence.event.StoredEvent;
import co.edu.uniquindio.fintech_backend.user.model.User;
import co.edu.uniquindio.fintech_backend.user.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

@Component
public class UserEventHandler implements EventHandler {
    private static final Set<EventType> SUPPORTED_TYPES = Set.of(EventType.USER_REGISTERED, EventType.USER_UPDATED);
    private final UserRepository userRepository;

    public UserEventHandler(UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "User repository is required");
    }

    @Override
    public Set<EventType> supportedTypes() {
        return SUPPORTED_TYPES;
    }

    @Override
    public void apply(StoredEvent event) {
        Objects.requireNonNull(event, "Event is required");
        if (!SUPPORTED_TYPES.contains(event.type())) {
            throw new IllegalArgumentException("Unsupported user event: " + event.type());
        }
        User user = UserEventMapper.toUser(event.data());
        if (event.type() == EventType.USER_REGISTERED) {
            if (userRepository.findById(user.getId()).isPresent()) {
                throw new IllegalStateException("User is already registered");
            }
        } else {
            User previous = userRepository.findById(user.getId()).orElseThrow(() -> new IllegalStateException("User does not exist"));
            if (!previous.getIdentification().equals(user.getIdentification()) || !previous.getCreatedAt().equals(user.getCreatedAt())) {
                throw new IllegalStateException("User identity cannot change");
            }
        }
        userRepository.save(user);
    }
}
