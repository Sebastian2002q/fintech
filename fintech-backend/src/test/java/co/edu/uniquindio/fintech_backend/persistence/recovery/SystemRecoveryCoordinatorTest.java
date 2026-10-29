package co.edu.uniquindio.fintech_backend.persistence.recovery;

import co.edu.uniquindio.fintech_backend.persistence.event.EventReplayCoordinator;
import co.edu.uniquindio.fintech_backend.persistence.snapshot.SnapshotCoordinator;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SystemRecoveryCoordinatorTest {
    private final SnapshotCoordinator snapshotCoordinator = mock(SnapshotCoordinator.class);
    private final EventReplayCoordinator eventReplayCoordinator = mock(EventReplayCoordinator.class);
    private final SystemRecoveryCoordinator coordinator = new SystemRecoveryCoordinator(snapshotCoordinator, eventReplayCoordinator);

    @Test
    void shouldRecoverFromSnapshotAndReplayRemainingEvents() {
        // Arrange
        when(snapshotCoordinator.restoreLatest()).thenReturn(OptionalLong.of(5));
        when(eventReplayCoordinator.replayAfter(5)).thenReturn(8L);
        // Act
        long lastSequence = coordinator.recover();
        // Assert
        assertEquals(8, lastSequence);
        InOrder order = inOrder(snapshotCoordinator, eventReplayCoordinator);
        order.verify(snapshotCoordinator).restoreLatest();
        order.verify(eventReplayCoordinator).replayAfter(5);
    }

    @Test
    void shouldReplayAllEventsWhenNoSnapshotExists() {
        // Arrange
        when(snapshotCoordinator.restoreLatest()).thenReturn(OptionalLong.empty());
        when(eventReplayCoordinator.replayAfter(0)).thenReturn(3L);
        // Act
        long lastSequence = coordinator.recover();
        // Assert
        assertEquals(3, lastSequence);
        verify(eventReplayCoordinator).replayAfter(0);
    }

    @Test
    void shouldRecoverEmptySystem() {
        // Arrange
        when(snapshotCoordinator.restoreLatest()).thenReturn(OptionalLong.empty());
        when(eventReplayCoordinator.replayAfter(0)).thenReturn(0L);
        // Act
        long lastSequence = coordinator.recover();
        // Assert
        assertEquals(0, lastSequence);
        verify(snapshotCoordinator).restoreLatest();
        verify(eventReplayCoordinator).replayAfter(0);
    }

    @Test
    void shouldPreserveSnapshotSequenceWhenNoNewEventsExist() {
        // Arrange
        when(snapshotCoordinator.restoreLatest()).thenReturn(OptionalLong.of(10));
        when(eventReplayCoordinator.replayAfter(10)).thenReturn(10L);
        // Act
        long lastSequence = coordinator.recover();
        // Assert
        assertEquals(10, lastSequence);
        verify(eventReplayCoordinator).replayAfter(10);
    }

    @Test
    void shouldNotReplayEventsWhenSnapshotRestorationFails() {
        // Arrange
        when(snapshotCoordinator.restoreLatest()).thenThrow(new IllegalStateException("Invalid snapshot"));
        // Act & Assert
        assertThrows(IllegalStateException.class, coordinator::recover);
        verifyNoInteractions(eventReplayCoordinator);
    }

    @Test
    void shouldPropagateEventReplayFailures() {
        // Arrange
        when(snapshotCoordinator.restoreLatest()).thenReturn(OptionalLong.of(5));
        when(eventReplayCoordinator.replayAfter(5)).thenThrow(new IllegalStateException("Invalid event sequence"));
        // Act & Assert
        assertThrows(IllegalStateException.class, coordinator::recover);
        verify(eventReplayCoordinator).replayAfter(5);
    }

    @Test
    void shouldRejectNullDependencies() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> new SystemRecoveryCoordinator(null, eventReplayCoordinator));
        assertThrows(NullPointerException.class, () -> new SystemRecoveryCoordinator(snapshotCoordinator, null));
    }
}
