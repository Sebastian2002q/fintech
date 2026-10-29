package co.edu.uniquindio.fintech_backend.user.service;

import co.edu.uniquindio.fintech_backend.user.model.User;

public interface UserCommands {
    User register(String identification, String firstName, String lastName, String email);
}
