package co.edu.uniquindio.fintech_backend.auth.security;

import co.edu.uniquindio.fintech_backend.auth.service.AuthCommands;
import co.edu.uniquindio.fintech_backend.user.model.Role;
import co.edu.uniquindio.fintech_backend.user.model.User;
import co.edu.uniquindio.fintech_backend.user.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityHttpIntegrationTest {
    private static final String PASSWORD = "MySecurePassword2026!";
    private static final Path TEST_DIRECTORY = Path.of("target", "security-http", UUID.randomUUID().toString());
    @Autowired
    private MockMvc mvc;
    @Autowired
    private AuthCommands authCommands;
    @Autowired
    private UserRepository userRepository;

    @DynamicPropertySource
    static void configurePersistence(DynamicPropertyRegistry registry) {
        registry.add("fintech.persistence.events-file", () -> TEST_DIRECTORY.resolve("events.jsonl").toString());
        registry.add("fintech.persistence.snapshot-file", () -> TEST_DIRECTORY.resolve("snapshot.json").toString());
    }

    private User createAccount() {
        String key = UUID.randomUUID().toString().replace("-", "");
        return authCommands.register("ID-" + key, "Carlos", "Perez", "user" + key + "@example.com", PASSWORD);
    }

    private MockHttpSession login(User user) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login").with(csrf()).param("email", user.getEmail()).param("password", PASSWORD)).andExpect(status().isOk()).andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(session);
        return session;
    }

    @Test
    void shouldExposeCsrfTokenWithoutAuthentication() throws Exception {
        mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty()).andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"));
    }

    @Test
    void shouldRejectAnonymousAccessAndRequestsWithoutCsrf() throws Exception {
        mvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login")).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/register")).andExpect(status().isForbidden());
    }

    @Test
    void shouldRegisterAccountWithCsrfAndRejectDuplicates() throws Exception {
        String key = UUID.randomUUID().toString().replace("-", "");
        String email = "user" + key + "@example.com";
        String request = """
                {"identification":"ID-%s","firstName":"Carlos","lastName":"Perez","email":"%s","password":"%s"}
                """.formatted(key, email, PASSWORD);
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isCreated()).andExpect(jsonPath("$.email").value(email)).andExpect(jsonPath("$.role").value("USER")).andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isConflict());
    }

    @Test
    void shouldAuthenticateAndMaintainHttpSession() throws Exception {
        User user = createAccount();
        MockHttpSession session = login(user);
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(user.getId().toString())).andExpect(jsonPath("$.email").value(user.getEmail()));
        mvc.perform(get("/api/users").session(session)).andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectIncorrectPassword() throws Exception {
        User user = createAccount();
        mvc.perform(post("/api/auth/login").with(csrf()).param("email", user.getEmail()).param("password", "IncorrectPassword2026!")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void shouldVerifyCurrentRoleAndStatusOnEveryProtectedRequest() throws Exception {
        User user = createAccount();
        MockHttpSession session = login(user);
        mvc.perform(get("/api/users").session(session)).andExpect(status().isForbidden());
        User admin = userRepository.findById(user.getId()).orElseThrow();
        admin.changeRole(Role.ADMIN);
        userRepository.save(admin);
        mvc.perform(get("/api/users").session(session)).andExpect(status().isOk());
        admin.block();
        userRepository.save(admin);
        mvc.perform(get("/api/users").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isForbidden());
    }

    @Test
    void shouldInvalidateSessionOnLogout() throws Exception {
        User user = createAccount();
        MockHttpSession session = login(user);
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());
        assertTrue(session.isInvalid());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAcceptCsrfTokenObtainedFromPublicEndpoint() throws Exception {
        User user = createAccount();
        MvcResult csrfResult = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        String token = JsonPath.read(csrfResult.getResponse().getContentAsString(), "$.token");
        MockHttpSession session = (MockHttpSession) csrfResult.getRequest().getSession(false);
        assertNotNull(session);
        MvcResult loginResult = mvc.perform(post("/api/auth/login").session(session).header("X-CSRF-TOKEN", token).param("email", user.getEmail()).param("password", PASSWORD)).andExpect(status().isOk()).andReturn();
        MockHttpSession authenticatedSession = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(authenticatedSession);
        mvc.perform(get("/api/auth/me").session(authenticatedSession)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(user.getId().toString()));
    }
}
