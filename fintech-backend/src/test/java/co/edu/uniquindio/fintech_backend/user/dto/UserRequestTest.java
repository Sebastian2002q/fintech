package co.edu.uniquindio.fintech_backend.user.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserRequestTest {
    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @Test
    void shouldAcceptValidRequest() {
        UserRequest request = new UserRequest("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectBlankFields() {
        assertInvalidField(new UserRequest("", "Carlos", "Perez", "carlos@gmail.com"), "identification");
        assertInvalidField(new UserRequest("1090123456", " ", "Perez", "carlos@gmail.com"), "firstName");
        assertInvalidField(new UserRequest("1090123456", "Carlos", "", "carlos@gmail.com"), "lastName");
        assertInvalidField(new UserRequest("1090123456", "Carlos", "Perez", " "), "email");
    }

    @Test
    void shouldRejectNullFields() {
        assertInvalidField(new UserRequest(null, "Carlos", "Perez", "carlos@gmail.com"), "identification");
        assertInvalidField(new UserRequest("1090123456", null, "Perez", "carlos@gmail.com"), "firstName");
        assertInvalidField(new UserRequest("1090123456", "Carlos", null, "carlos@gmail.com"), "lastName");
        assertInvalidField(new UserRequest("1090123456", "Carlos", "Perez", null), "email");
    }

    @Test
    void shouldRejectInvalidEmailFormat() {
        UserRequest request = new UserRequest("1090123456", "Carlos", "Perez", "invalid-email");
        assertInvalidField(request, "email");
    }

    @Test
    void shouldPreserveProvidedValues() {
        UserRequest request = new UserRequest("1090123456", "Carlos", "Perez", "CARLOS@GMAIL.COM");
        assertEquals("1090123456", request.identification());
        assertEquals("Carlos", request.firstName());
        assertEquals("Perez", request.lastName());
        assertEquals("CARLOS@GMAIL.COM", request.email());
        assertTrue(validator.validate(request).isEmpty());
    }

    private void assertInvalidField(UserRequest request, String field) {
        assertTrue(validator.validate(request).stream().anyMatch(violation -> violation.getPropertyPath().toString().equals(field)));
    }
}
