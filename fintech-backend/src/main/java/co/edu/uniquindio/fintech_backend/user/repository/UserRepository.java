package co.edu.uniquindio.fintech_backend.user.repository;

import co.edu.uniquindio.fintech_backend.user.model.User;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    void save(User user);
    Optional<User> findById(UUID id);
    Optional<User> findByIdentification(String identification);
    Optional<User> findByEmail(String email);
    Collection<User> findAll();
    void replaceAll(Collection<User> users);

}
