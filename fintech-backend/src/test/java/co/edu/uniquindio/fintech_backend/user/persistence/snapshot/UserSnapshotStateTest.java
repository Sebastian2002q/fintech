package co.edu.uniquindio.fintech_backend.user.persistence.snapshot;

import co.edu.uniquindio.fintech_backend.user.model.Role;
import co.edu.uniquindio.fintech_backend.user.model.UserStatus;
import co.edu.uniquindio.fintech_backend.user.persistence.snapshot.UserSnapshotEntry;
import co.edu.uniquindio.fintech_backend.user.persistence.snapshot.UserSnapshotState;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserSnapshotStateTest {

    @Test
    void shouldPreserveAllUserEntries() {
        // Arrange
        UserSnapshotEntry firstEntry = new UserSnapshotEntry(UUID.randomUUID(), "1090123456", "Carlos", "Perez", "carlos@gmail.com", Role.USER, UserStatus.ACTIVE, Instant.parse("2026-01-15T10:30:00Z"));
        UserSnapshotEntry secondEntry = new UserSnapshotEntry(UUID.randomUUID(), "1090654321", "Maria", "Gomez", "maria@gmail.com", Role.ADMIN, UserStatus.BLOCKED, Instant.parse("2026-02-20T14:00:00Z"));
        // Act
        UserSnapshotState state = new UserSnapshotState(List.of(firstEntry, secondEntry));
        // Assert
        assertNotNull(state.users());
        assertEquals(2, state.users().size());
        assertEquals(firstEntry, state.users().get(0));
        assertEquals(secondEntry, state.users().get(1));
    }

    @Test
    void shouldProtectSnapshotEntriesFromModification() {
        // Arrange
        UserSnapshotEntry entry = new UserSnapshotEntry(UUID.randomUUID(), "1090123456", "Carlos", "Perez", "carlos@gmail.com", Role.USER, UserStatus.ACTIVE, Instant.parse("2026-01-15T10:30:00Z"));
        List<UserSnapshotEntry> entries = new ArrayList<>();
        entries.add(entry);
        // Act
        UserSnapshotState state = new UserSnapshotState(entries);
        // Modify the original list
        entries.clear();
        // Assert - Snapshot remains unchanged
        assertEquals(1, state.users().size());
        assertEquals(entry, state.users().get(0));
        // Assert - Snapshot list cannot be modified
        assertThrows(UnsupportedOperationException.class, () -> state.users().clear());
        assertEquals(1, state.users().size());
    }

    @Test
    void shouldRejectNullListAndNullEntries() {
        // Act & Assert - Null list
        assertThrows(NullPointerException.class, () -> new UserSnapshotState(null));
        // Arrange - List containing a null entry
        List<UserSnapshotEntry> entries = new ArrayList<>();
        entries.add(null);
        // Act & Assert - Null entry
        assertThrows(NullPointerException.class, () -> new UserSnapshotState(entries));
    }

}
