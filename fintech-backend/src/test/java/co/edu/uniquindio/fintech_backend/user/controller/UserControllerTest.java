package co.edu.uniquindio.fintech_backend.user.controller;

import co.edu.uniquindio.fintech_backend.user.model.User;
import co.edu.uniquindio.fintech_backend.user.service.UserAlreadyExistsException;
import co.edu.uniquindio.fintech_backend.user.service.UserCommands;
import co.edu.uniquindio.fintech_backend.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(UserExceptionHandler.class)
class UserControllerTest {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private UserCommands userCommands;
    @MockitoBean
    private UserService userService;
    private static final String VALID_REQUEST = """
            {
                "identification": "1090123456",
                "firstName": "Carlos",
                "lastName": "Perez",
                "email": "carlos@gmail.com"
            }
            """;

    @Test
    void shouldRegisterUserAndReturnCreated() throws Exception {
        // Arrange
        User user = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        when(userCommands.register("1090123456", "Carlos", "Perez", "carlos@gmail.com")).thenReturn(user);
        // Act & Assert
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST)).andExpect(status().isCreated()).andExpect(header().string("Location", "/api/users/" + user.getId())).andExpect(jsonPath("$.id").value(user.getId().toString())).andExpect(jsonPath("$.identification").value("1090123456")).andExpect(jsonPath("$.firstName").value("Carlos")).andExpect(jsonPath("$.lastName").value("Perez")).andExpect(jsonPath("$.email").value("carlos@gmail.com")).andExpect(jsonPath("$.role").value("USER")).andExpect(jsonPath("$.status").value("ACTIVE"));
        verify(userCommands).register("1090123456", "Carlos", "Perez", "carlos@gmail.com");
    }

    @Test
    void shouldRejectInvalidEmail() throws Exception {
        // Arrange
        String request = """
                {"identification":"1090123456","firstName":"Carlos","lastName":"Perez","email":"invalid-email"}
                """;
        // Act & Assert
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isBadRequest());
        verifyNoInteractions(userCommands);
    }

    @Test
    void shouldRejectBlankRegistrationFields() throws Exception {
        // Arrange
        String request = """
                {"identification":" ","firstName":"Carlos","lastName":"Perez","email":"carlos@gmail.com"}
                """;
        // Act & Assert
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isBadRequest());
        verifyNoInteractions(userCommands);
    }

    @Test
    void shouldReturnConflictForDuplicateIdentification() throws Exception {
        // Arrange
        when(userCommands.register("1090123456", "Carlos", "Perez", "carlos@gmail.com")).thenThrow(new UserAlreadyExistsException("Identification already registered"));
        // Act & Assert
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST)).andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("Identification already registered"));
    }

    @Test
    void shouldReturnConflictForDuplicateEmail() throws Exception {
        // Arrange
        when(userCommands.register("1090123456", "Carlos", "Perez", "carlos@gmail.com")).thenThrow(new UserAlreadyExistsException("Email already registered"));
        // Act & Assert
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST)).andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("Email already registered"));
    }

    @Test
    void shouldReturnAllUsers() throws Exception {
        // Arrange
        User first = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        User second = new User("1090654321", "Maria", "Gomez", "maria@gmail.com");
        when(userService.getAll()).thenReturn(List.of(first, second));
        // Act & Assert
        mvc.perform(get("/api/users")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].id").value(first.getId().toString())).andExpect(jsonPath("$[1].id").value(second.getId().toString()));
    }

    @Test
    void shouldReturnEmptyArrayWhenNoUsersExist() throws Exception {
        // Arrange
        when(userService.getAll()).thenReturn(List.of());
        // Act & Assert
        mvc.perform(get("/api/users")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldFindUserById() throws Exception {
        // Arrange
        User user = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        when(userService.getById(user.getId())).thenReturn(user);
        // Act & Assert
        mvc.perform(get("/api/users/{id}", user.getId())).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(user.getId().toString())).andExpect(jsonPath("$.firstName").value("Carlos")).andExpect(jsonPath("$.email").value("carlos@gmail.com"));
        verify(userService).getById(user.getId());
    }

    @Test
    void shouldReturnNotFoundForUnknownUser() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        when(userService.getById(id)).thenThrow(new NoSuchElementException("User not found"));
        // Act & Assert
        mvc.perform(get("/api/users/{id}", id)).andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("User not found"));
    }
}
