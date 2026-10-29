package co.edu.uniquindio.fintech_backend;

import co.edu.uniquindio.fintech_backend.persistence.event.EventStore;
import co.edu.uniquindio.fintech_backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FintechBackendApplicationTests {
    private static final Path TEST_DIRECTORY = Path.of("target", "test-persistence", UUID.randomUUID().toString());
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EventStore eventStore;

    @DynamicPropertySource
    static void configurePersistence(DynamicPropertyRegistry registry) {
        registry.add("fintech.persistence.events-file", () -> TEST_DIRECTORY.resolve("events.jsonl").toString());
        registry.add("fintech.persistence.snapshot-file", () -> TEST_DIRECTORY.resolve("snapshot.json").toString());
    }

    @Test
    void contextLoads() {
        // Assert
        assertNotNull(userRepository);
        assertNotNull(eventStore);
        assertEquals(0, eventStore.getLastSequence());
        assertTrue(userRepository.findAll().isEmpty());
    }
}

