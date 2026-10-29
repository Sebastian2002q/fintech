package co.edu.uniquindio.fintech_backend.user.model;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class User {
    private final UUID id;
    private final String identification;
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
    private UserStatus status;
    private final Instant createdAt;

    public User(String identification, String firstName, String lastName, String email) {
        this.id = UUID.randomUUID();
        this.identification = requireText(identification);
        this.firstName = requireText(firstName);
        this.lastName = requireText(lastName);
        this.email = normalizeEmail(email);
        this.role = Role.USER;
        this.status = UserStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    public User(User source) {
        Objects.requireNonNull(source, "User cannot be null");
        this.id = source.id;
        this.identification = source.identification;
        this.firstName = source.firstName;
        this.lastName = source.lastName;
        this.email = source.email;
        this.role = source.role;
        this.status = source.status;
        this.createdAt = source.createdAt;
    }

    private User(UUID id, String identification, String firstName, String lastName, String email, Role role, UserStatus status, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "User ID is required");
        this.createdAt = Objects.requireNonNull(createdAt, "Creation date is required");
        this.role = Objects.requireNonNull(role, "User role is required");
        this.status = Objects.requireNonNull(status, "User status is required");
        if (identification == null || identification.isBlank()) {
            throw new IllegalArgumentException("Identification cannot be blank");
        }
        this.identification = identification.trim();
        // Reuse the existing profile validation
        updateProfile(firstName, lastName, email);
    }

    public static User restore(UUID id, String identification, String firstName, String lastName, String email, Role role, UserStatus status, Instant createdAt) {
        return new User(id, identification, firstName, lastName, email, role, status, createdAt);
    }

    public void updateProfile(String firstName, String lastName, String email) {
        String newFirstName = requireText(firstName);
        String newLastName = requireText(lastName);
        String newEmail = normalizeEmail(email);
        this.firstName = newFirstName;
        this.lastName = newLastName;
        this.email = newEmail;
    }

    public void changeRole(Role role) {
        this.role = Objects.requireNonNull(role);
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    public void block() {
        this.status = UserStatus.BLOCKED;
    }

    public void deactivate() {
        this.status = UserStatus.INACTIVE;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    private static String requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Value cannot be empty");
        }
        return value.strip();
    }

    private static String normalizeEmail(String email) {
        return requireText(email).toLowerCase(Locale.ROOT);
    }

    public UUID getId() {
        return id;
    }

    public String getIdentification() {
        return identification;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
