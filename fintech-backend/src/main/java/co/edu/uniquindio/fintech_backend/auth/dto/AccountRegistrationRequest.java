package co.edu.uniquindio.fintech_backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccountRegistrationRequest(@NotBlank String identification, @NotBlank String firstName,
                                         @NotBlank String lastName, @NotBlank @Email String email,
                                         @NotBlank @Size(min = 12) String password) {
}
