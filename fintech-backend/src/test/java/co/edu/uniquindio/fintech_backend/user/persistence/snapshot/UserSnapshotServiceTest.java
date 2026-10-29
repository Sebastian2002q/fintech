package co.edu.uniquindio.fintech_backend.user.persistence.snapshot;

import co.edu.uniquindio.fintech_backend.persistence.snapshot.SnapshotParticipant;
import co.edu.uniquindio.fintech_backend.user.model.Role;
import co.edu.uniquindio.fintech_backend.user.model.User;
import co.edu.uniquindio.fintech_backend.user.persistence.snapshot.UserSnapshotEntry;
import co.edu.uniquindio.fintech_backend.user.persistence.snapshot.UserSnapshotMapper;
import co.edu.uniquindio.fintech_backend.user.persistence.snapshot.UserSnapshotService;
import co.edu.uniquindio.fintech_backend.user.persistence.snapshot.UserSnapshotState;
import co.edu.uniquindio.fintech_backend.user.repository.InMemoryUserRepository;
import org.junit.jupiter.api.Test;
import co.edu.uniquindio.fintech_backend.user.model.UserStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;


class UserSnapshotServiceTest {

    @Test
    void shouldCaptureAllUsersFromRepository() {
        // Arrange
        InMemoryUserRepository repository = new InMemoryUserRepository();
        User firstUser = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        User secondUser = new User("1090654321", "Maria", "Gomez", "maria@gmail.com");
        firstUser.changeRole(Role.ADMIN);
        secondUser.block();
        repository.save(firstUser);
        repository.save(secondUser);
        UserSnapshotService service = new UserSnapshotService(repository);
        // Act
        UserSnapshotState state = service.capture();
        // Assert
        assertNotNull(state);
        assertEquals(2, state.users().size());
        assertTrue(state.users().contains(UserSnapshotMapper.toEntry(firstUser)));
        assertTrue(state.users().contains(UserSnapshotMapper.toEntry(secondUser)));
    }

    @Test
    void shouldCaptureEmptyRepository() {
        // Arrange
        InMemoryUserRepository repository = new InMemoryUserRepository();
        UserSnapshotService service = new UserSnapshotService(repository);
        // Act
        UserSnapshotState state = service.capture();
        // Assert
        assertNotNull(state);
        assertNotNull(state.users());
        assertTrue(state.users().isEmpty());
        assertEquals(0, state.users().size());
    }

    @Test
    void shouldRejectNullRepository() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> new UserSnapshotService(null));
    }

    @Test
    void shouldRestoreAllUsersAndReplacePreviousState() {
        // Arrange
        InMemoryUserRepository repository = new InMemoryUserRepository();
        User originalUser = new User("1000000001", "Original", "User", "original@gmail.com");
        repository.save(originalUser);
        UUID firstId = UUID.randomUUID();
        Instant firstCreatedAt = Instant.parse("2026-01-15T10:30:00Z");
        UserSnapshotEntry firstEntry = new UserSnapshotEntry(firstId, "1090123456", "Carlos", "Perez", "carlos@gmail.com", Role.ADMIN, UserStatus.BLOCKED, firstCreatedAt);
        User secondUser = new User("1090654321", "Maria", "Gomez", "maria@gmail.com");
        UserSnapshotEntry secondEntry = UserSnapshotMapper.toEntry(secondUser);
        UserSnapshotState state = new UserSnapshotState(List.of(firstEntry, secondEntry));
        UserSnapshotService service = new UserSnapshotService(repository);
        // Act
        service.restore(state);
        // Assert
        assertEquals(2, repository.findAll().size());
        assertTrue(repository.findById(originalUser.getId()).isEmpty());
        assertTrue(repository.findByEmail("original@gmail.com").isEmpty());
        User restoredFirst = repository.findById(firstId).orElseThrow();
        assertEquals("Carlos", restoredFirst.getFirstName());
        assertEquals(Role.ADMIN, restoredFirst.getRole());
        assertEquals(UserStatus.BLOCKED, restoredFirst.getStatus());
        assertEquals(firstCreatedAt, restoredFirst.getCreatedAt());
        assertEquals(firstId, repository.findByIdentification("1090123456").orElseThrow().getId());
        assertEquals(firstId, repository.findByEmail("carlos@gmail.com").orElseThrow().getId());
        assertEquals(secondUser.getId(), repository.findById(secondUser.getId()).orElseThrow().getId());
        assertEquals(secondUser.getId(), repository.findByIdentification("1090654321").orElseThrow().getId());
        assertEquals(secondUser.getId(), repository.findByEmail("maria@gmail.com").orElseThrow().getId());
    }

    @Test
    void shouldRestoreEmptySnapshotState() {
        // Arrange
        InMemoryUserRepository repository = new InMemoryUserRepository();
        User user = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        repository.save(user);
        UserSnapshotService service = new UserSnapshotService(repository);
        UserSnapshotState state = new UserSnapshotState(List.of());
        // Act
        service.restore(state);
        // Assert
        assertTrue(repository.findAll().isEmpty());
        assertTrue(repository.findById(user.getId()).isEmpty());
        assertTrue(repository.findByIdentification("1090123456").isEmpty());
        assertTrue(repository.findByEmail("carlos@gmail.com").isEmpty());
    }

    @Test
    void shouldPreserveRepositoryWhenSnapshotContainsDuplicateEmails() {
        // Arrange
        InMemoryUserRepository repository = new InMemoryUserRepository();
        User original = new User("1000000001", "Original", "User", "original@gmail.com");
        repository.save(original);
        User firstUser = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        User secondUser = new User("1090654321", "Maria", "Gomez", "  CARLOS@GMAIL.COM  ");
        UserSnapshotState state = new UserSnapshotState(List.of(UserSnapshotMapper.toEntry(firstUser), UserSnapshotMapper.toEntry(secondUser)));
        UserSnapshotService service = new UserSnapshotService(repository);
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> service.restore(state));
        assertEquals(1, repository.findAll().size());
        assertTrue(repository.findById(original.getId()).isPresent());
        assertTrue(repository.findByEmail("original@gmail.com").isPresent());
        assertTrue(repository.findById(firstUser.getId()).isEmpty());
        assertTrue(repository.findById(secondUser.getId()).isEmpty());
    }

    @Test
    void shouldRejectNullSnapshotStateWithoutChangingRepository() {
        // Arrange
        InMemoryUserRepository repository = new InMemoryUserRepository();
        User user = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        repository.save(user);
        UserSnapshotService service = new UserSnapshotService(repository);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> service.restore(null));
        assertEquals(1, repository.findAll().size());
        assertTrue(repository.findById(user.getId()).isPresent());
    }

    @Test
    void shouldImplementSnapshotParticipantContract() {
        // Arrange
        InMemoryUserRepository repository = new InMemoryUserRepository();
        SnapshotParticipant<UserSnapshotState> participant = new UserSnapshotService(repository);
        User user = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        repository.save(user);
        // Act
        UserSnapshotState state = participant.capture();
        repository.replaceAll(List.of());
        participant.restore(state);
        // Assert
        assertEquals("users", participant.moduleKey());
        assertEquals(UserSnapshotState.class, participant.stateType());
        assertEquals(1, state.users().size());
        assertTrue(repository.findById(user.getId()).isPresent());
    }

}
