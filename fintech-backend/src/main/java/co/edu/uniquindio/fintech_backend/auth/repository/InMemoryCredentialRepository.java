package co.edu.uniquindio.fintech_backend.auth.repository;

import co.edu.uniquindio.fintech_backend.auth.model.Credential;
import co.edu.uniquindio.fintech_backend.structures.hash.CustomHashMap;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class InMemoryCredentialRepository implements CredentialRepository {
    private CustomHashMap<UUID, Credential> credentials = new CustomHashMap<>();

    @Override
    public synchronized void save(Credential credential) {
        Objects.requireNonNull(credential, "Credential is required");
        credentials.put(credential.userId(), credential);
    }

    @Override
    public synchronized Optional<Credential> findByUserId(UUID userId) {
        Objects.requireNonNull(userId, "User ID is required");
        return Optional.ofNullable(credentials.get(userId));
    }

    @Override
    public synchronized Collection<Credential> findAll() {
        List<Credential> result = new ArrayList<>();
        credentials.forEachValue(result::add);
        return List.copyOf(result);
    }

    @Override
    public synchronized void replaceAll(Collection<Credential> entries) {
        Objects.requireNonNull(entries, "Credentials collection is required");
        CustomHashMap<UUID, Credential> replacement = new CustomHashMap<>();
        for (Credential credential : entries) {
            Objects.requireNonNull(credential, "Credential cannot be null");
            if (replacement.containsKey(credential.userId())) {
                throw new IllegalArgumentException("Duplicate credential");
            }
            replacement.put(credential.userId(), credential);
        }
        credentials = replacement;
    }
}
