package co.edu.uniquindio.fintech_backend.user.controller;

import co.edu.uniquindio.fintech_backend.persistence.event.EventStore;
import co.edu.uniquindio.fintech_backend.user.model.Role;
import co.edu.uniquindio.fintech_backend.user.model.User;
import co.edu.uniquindio.fintech_backend.user.model.UserStatus;
import co.edu.uniquindio.fintech_backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "ADMIN")
class UserApiIntegrationTest {
    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Path TEST_DIRECTORY = Path.of("target", "user-api-integration", UUID.randomUUID().toString());
    @Autowired
    private MockMvc mvc;
    @Autowired
    private UserRepository repository;
    @Autowired
    private EventStore eventStore;

    @DynamicPropertySource
    static void configurePersistence(DynamicPropertyRegistry registry) {
        registry.add("fintech.persistence.events-file", () -> TEST_DIRECTORY.resolve("events.jsonl").toString());
        registry.add("fintech.persistence.snapshot-file", () -> TEST_DIRECTORY.resolve("snapshot.json").toString());
    }

    @BeforeEach
    void prepareAdmin() {
        if (repository.findById(ADMIN_ID).isEmpty()) {
            repository.save(User.restore(ADMIN_ID, "TEST-ADMIN-001", "Test", "Admin", "admin@test.local", Role.ADMIN, UserStatus.ACTIVE, Instant.parse("2026-01-01T00:00:00Z")));
        }
    }

    private String request(String identification, String email) {
        return """
                {"identification":"%s","firstName":"Carlos","lastName":"Perez","email":"%s"}
                """.formatted(identification, email);
    }

    @Test
    void shouldRegisterAndRetrieveUserThroughRealServices() throws Exception {
        // Arrange
        String key = UUID.randomUUID().toString().replace("-", "");
        String identification = "ID-" + key;
        String email = "user" + key + "@gmail.com";
        long initialSequence = eventStore.getLastSequence();
        // Act
        String location = mvc.perform(post("/api/users").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(request(identification, email))).andExpect(status().isCreated()).andExpect(jsonPath("$.identification").value(identification)).andExpect(jsonPath("$.email").value(email)).andExpect(jsonPath("$.role").value("USER")).andExpect(jsonPath("$.status").value("ACTIVE")).andReturn().getResponse().getHeader("Location");
        // Assert
        assertNotNull(location);
        assertTrue(location.startsWith("/api/users/"));
        UUID id = UUID.fromString(location.substring("/api/users/".length()));
        assertEquals(initialSequence + 1, eventStore.getLastSequence());
        assertEquals(id, repository.findById(id).orElseThrow().getId());
        assertEquals(id, repository.findByIdentification(identification).orElseThrow().getId());
        assertEquals(id, repository.findByEmail(email).orElseThrow().getId());
        mvc.perform(get("/api/users/{id}", id)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id.toString())).andExpect(jsonPath("$.firstName").value("Carlos")).andExpect(jsonPath("$.email").value(email));
        mvc.perform(get("/api/users")).andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id == '" + id + "')]").exists());
    }

    @Test
    void shouldRejectDuplicateUsersWithoutPersistingAdditionalEvents() throws Exception {
        // Arrange
        String key = UUID.randomUUID().toString().replace("-", "");
        String identification = "ID-" + key;
        String email = "user" + key + "@gmail.com";
        String anotherIdentification = "OTHER-" + key;
        mvc.perform(post("/api/users").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(request(identification, email))).andExpect(status().isCreated());
        long sequence = eventStore.getLastSequence();
        // Act & Assert
        mvc.perform(post("/api/users").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(request(identification, "another" + key + "@gmail.com"))).andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("Identification already registered"));
        mvc.perform(post("/api/users").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(request(anotherIdentification, email))).andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("Email already registered"));
        assertEquals(sequence, eventStore.getLastSequence());
        assertTrue(repository.findByIdentification(anotherIdentification).isEmpty());
    }

    @Test
    void shouldRejectInvalidRequestsAndUnknownUsers() throws Exception {
        // Arrange
        long initialSequence = eventStore.getLastSequence();
        String invalidRequest = """
                {"identification":"12345","firstName":"Carlos","lastName":"Perez","email":"invalid-email"}
                """;
        // Act & Assert
        mvc.perform(post("/api/users").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(invalidRequest)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/users/{id}", UUID.randomUUID())).andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("User not found"));
        assertEquals(initialSequence, eventStore.getLastSequence());
    }
}
