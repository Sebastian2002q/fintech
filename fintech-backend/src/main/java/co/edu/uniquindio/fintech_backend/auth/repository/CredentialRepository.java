package co.edu.uniquindio.fintech_backend.auth.repository;

import co.edu.uniquindio.fintech_backend.auth.model.Credential;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface CredentialRepository {
    void save(Credential credential);

    Optional<Credential> findByUserId(UUID userId);

    Collection<Credential> findAll();

    void replaceAll(Collection<Credential> credentials);
}