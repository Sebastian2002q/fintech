package co.edu.uniquindio.fintech_backend.auth.service;

import co.edu.uniquindio.fintech_backend.user.model.User;

public interface AuthCommands {
    User register(String identification, String firstName, String lastName, String email, String password);
}
