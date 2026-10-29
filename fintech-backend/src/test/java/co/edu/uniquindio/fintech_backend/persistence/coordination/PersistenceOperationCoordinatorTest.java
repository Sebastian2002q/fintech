package co.edu.uniquindio.fintech_backend.persistence.coordination;

import co.edu.uniquindio.fintech_backend.persistence.event.EventStore;
import co.edu.uniquindio.fintech_backend.persistence.snapshot.SnapshotCoordinator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PersistenceOperationCoordinatorTest {
    private final EventStore eventStore = mock(EventStore.class);
    private final SnapshotCoordinator snapshotCoordinator = mock(SnapshotCoordinator.class);
    private final PersistenceOperationCoordinator coordinator = new PersistenceOperationCoordinator(eventStore, snapshotCoordinator);

    @Test
    void shouldExecuteOperationAndReturnResult() {
        // Act
        String result = coordinator.execute(() -> "Operation completed");
        // Assert
        assertEquals("Operation completed", result);
    }

    @Test
    void shouldSaveSnapshotWithCurrentEventSequence() {
        // Arrange
        when(eventStore.getLastSequence()).thenReturn(15L);
        // Act
        coordinator.saveSnapshot();
        // Assert
        verify(eventStore).getLastSequence();
        verify(snapshotCoordinator).save(15L);
    }

    @Test
    void shouldPropagateOperationFailuresAndAllowSubsequentOperations() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> coordinator.execute(() -> {
            throw new IllegalArgumentException("Invalid operation");
        }));
        assertEquals("Recovered", coordinator.execute(() -> "Recovered"));
    }

    @Test
    void shouldRejectNullDependenciesAndOperations() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> new PersistenceOperationCoordinator(null, snapshotCoordinator));
        assertThrows(NullPointerException.class, () -> new PersistenceOperationCoordinator(eventStore, null));
        assertThrows(NullPointerException.class, () -> coordinator.execute(null));
        verifyNoInteractions(eventStore, snapshotCoordinator);
    }

    @Test
    void shouldBlockOperationsAndSnapshotsAfterPersistedEventFailsToApply() {
        // Arrange
        when(eventStore.getLastSequence()).thenReturn(0L, 1L);
        // Act
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> coordinator.execute(() -> {
            throw new IllegalStateException("Repository update failed");
        }));
        // Assert
        assertEquals("Repository update failed", failure.getMessage());
        IllegalStateException blocked = assertThrows(IllegalStateException.class, () -> coordinator.execute(() -> "Another operation"));
        assertEquals("Persistence requires recovery", blocked.getMessage());
        assertThrows(IllegalStateException.class, coordinator::saveSnapshot);
        verify(eventStore, times(2)).getLastSequence();
        verifyNoInteractions(snapshotCoordinator);
    }

    @Test
    void shouldAllowSnapshotAfterSuccessfulPersistedOperation() {
        // Arrange
        when(eventStore.getLastSequence()).thenReturn(0L, 1L);
        // Act
        String result = coordinator.execute(() -> "Saved");
        coordinator.saveSnapshot();
        // Assert
        assertEquals("Saved", result);
        verify(snapshotCoordinator).save(1L);
    }

    @Test
    void shouldBlockOperationsWhenSequenceVerificationFails() {
        // Arrange
        when(eventStore.getLastSequence()).thenReturn(0L).thenThrow(new IllegalStateException("Sequence unavailable"));
        // Act
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> coordinator.execute(() -> {
            throw new IllegalArgumentException("Operation failed");
        }));
        // Assert
        assertEquals("Operation failed", failure.getMessage());
        assertEquals(1, failure.getSuppressed().length);
        assertEquals("Sequence unavailable", failure.getSuppressed()[0].getMessage());
        assertThrows(IllegalStateException.class, () -> coordinator.execute(() -> "Another operation"));
        assertThrows(IllegalStateException.class, coordinator::saveSnapshot);
        verifyNoInteractions(snapshotCoordinator);
    }
}
