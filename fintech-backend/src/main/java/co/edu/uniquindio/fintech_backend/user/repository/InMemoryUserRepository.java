package co.edu.uniquindio.fintech_backend.user.repository;

import co.edu.uniquindio.fintech_backend.structures.hash.CustomHashMap;
import co.edu.uniquindio.fintech_backend.user.model.User;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class InMemoryUserRepository implements UserRepository {
    private CustomHashMap<UUID, User> users = new CustomHashMap<>();
    private CustomHashMap<String, UUID> usersByIdentification = new CustomHashMap<>();
    private CustomHashMap<String, UUID> usersByEmail = new CustomHashMap<>();

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
        UUID userId = usersByIdentification.get(identification);
        if (userId == null) {
            return Optional.empty();
        }
        return findById(userId);
    }

    @Override
    public synchronized Optional<User> findByEmail(String email) {
        UUID userId = usersByEmail.get(email);
        if (userId == null) {
            return Optional.empty();
        }
        return findById(userId);
    }

    @Override
    public synchronized Collection<User> findAll() {
        List<User> result = new ArrayList<>();
        users.forEachValue(user -> result.add(new User(user)));
        return List.copyOf(result);
    }

    @Override
    public synchronized void replaceAll(Collection<User> users) {
        Objects.requireNonNull(users, "Users collection is required");
        // Build new indexes without modifying the current repository
        CustomHashMap<UUID, User> newUsers = new CustomHashMap<>();
        CustomHashMap<String, UUID> newIdentificationIndex = new CustomHashMap<>();
        CustomHashMap<String, UUID> newEmailIndex = new CustomHashMap<>();
        for (User user : users) {
            Objects.requireNonNull(user, "User cannot be null");
            User copy = new User(user);
            UUID id = copy.getId();
            String identification = copy.getIdentification();
            String email = copy.getEmail();
            // Validate uniqueness
            if (newUsers.containsKey(id)) {
                throw new IllegalArgumentException("Duplicate user ID");
            }
            if (newIdentificationIndex.containsKey(identification)) {
                throw new IllegalArgumentException("Duplicate identification");
            }
            if (newEmailIndex.containsKey(email)) {
                throw new IllegalArgumentException("Duplicate email");
            }
            // Populate temporary indexes
            newUsers.put(id, copy);
            newIdentificationIndex.put(identification, id);
            newEmailIndex.put(email, id);
        }
        // Replace the repository only after successful validation
        this.users = newUsers;
        this.usersByIdentification = newIdentificationIndex;
        this.usersByEmail = newEmailIndex;
    }
}
