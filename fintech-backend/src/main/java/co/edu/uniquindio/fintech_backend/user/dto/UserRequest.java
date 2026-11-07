package co.edu.uniquindio.fintech_backend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserRequest(@NotBlank String identification, @NotBlank String firstName, @NotBlank String lastName,
                          @NotBlank @Email String email) {
}
