package co.edu.uniquindio.fintech_backend.user.service;

import co.edu.uniquindio.fintech_backend.user.model.User;
import co.edu.uniquindio.fintech_backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getById(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new NoSuchElementException("User not found"));
    }

    public Collection<User> getAll() {
        return userRepository.findAll();
    }
}
