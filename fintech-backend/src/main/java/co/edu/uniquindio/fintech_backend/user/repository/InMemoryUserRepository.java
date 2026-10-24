package co.edu.uniquindio.fintech_backend.user.repository;

import co.edu.uniquindio.fintech_backend.user.model.User;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class InMemoryUserRepository implements UserRepository {
    private final Map<UUID, User> users = new HashMap<>();
    private final Map<String, UUID> usersByIdentification = new HashMap<>();
    private final Map<String, UUID> usersByEmail = new HashMap<>();

    @Override
    public synchronized void save(User user) {
        Objects.requireNonNull(user, "User cannot be null");
        User copy = new User(user);
        UUID id = copy.getId();
        String identification = copy.getIdentification();
        String email = copy.getEmail();
        UUID identificationOwner = usersByIdentification.get(identification);
        if (identificationOwner != null && !identificationOwner.equals(id)) {
            throw new IllegalArgumentException("Identification already registered");
        }
        UUID emailOwner = usersByEmail.get(email);
        if (emailOwner != null && !emailOwner.equals(id)) {
            throw new IllegalArgumentException("Email already registered");
        }
        User previous = users.get(id);
        if (previous != null) {
            usersByIdentification.remove(previous.getIdentification());
            usersByEmail.remove(previous.getEmail());
        }
        users.put(id, copy);
        usersByIdentification.put(identification, id);
        usersByEmail.put(email, id);
    }

    @Override
    public synchronized Optional<User> findById(UUID id) {
        User user = users.get(id);
        return Optional.ofNullable(user).map(User::new);
    }

    @Override
    public synchronized Optional<User> findByIdentification(String identification) {
        if (identification == null) {
            return Optional.empty();
        }
        UUID id = usersByIdentification.get(identification);
        return findById(id);
    }

    @Override
    public synchronized Optional<User> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        String normalizedEmail = email.strip().toLowerCase(Locale.ROOT);
        UUID id = usersByEmail.get(normalizedEmail);
        return findById(id);
    }

    @Override
    public synchronized Collection<User> findAll() {
        return users.values().stream().map(User::new).toList();
    }
}
