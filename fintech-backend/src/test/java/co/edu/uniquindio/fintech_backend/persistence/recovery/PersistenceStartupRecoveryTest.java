package co.edu.uniquindio.fintech_backend.persistence.recovery;

import co.edu.uniquindio.fintech_backend.persistence.event.EventStore;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PersistenceStartupRecoveryTest {
    private final SystemRecoveryCoordinator recoveryCoordinator = mock(SystemRecoveryCoordinator.class);
    private final EventStore eventStore = mock(EventStore.class);
    private final PersistenceStartupRecovery startupRecovery = new PersistenceStartupRecovery(recoveryCoordinator, eventStore);

    @Test
    void shouldRecoverSystemDuringStartup() {
        // Arrange
        when(recoveryCoordinator.recover()).thenReturn(5L);
        when(eventStore.getLastSequence()).thenReturn(5L);
        // Act
        startupRecovery.afterSingletonsInstantiated();
        // Assert
        InOrder order = inOrder(recoveryCoordinator, eventStore);
        order.verify(recoveryCoordinator).recover();
        order.verify(eventStore).getLastSequence();
    }

    @Test
    void shouldAllowStartupWithEmptySystem() {
        // Arrange
        when(recoveryCoordinator.recover()).thenReturn(0L);
        when(eventStore.getLastSequence()).thenReturn(0L);
        // Act & Assert
        assertDoesNotThrow(startupRecovery::afterSingletonsInstantiated);
        verify(recoveryCoordinator).recover();
        verify(eventStore).getLastSequence();
    }

    @Test
    void shouldRejectSnapshotSequenceAheadOfEventStore() {
        // Arrange
        when(recoveryCoordinator.recover()).thenReturn(10L);
        when(eventStore.getLastSequence()).thenReturn(8L);
        // Act & Assert
        assertThrows(IllegalStateException.class, startupRecovery::afterSingletonsInstantiated);
    }

    @Test
    void shouldRejectIncompleteEventRecovery() {
        // Arrange
        when(recoveryCoordinator.recover()).thenReturn(5L);
        when(eventStore.getLastSequence()).thenReturn(8L);
        // Act & Assert
        assertThrows(IllegalStateException.class, startupRecovery::afterSingletonsInstantiated);
    }

    @Test
    void shouldPropagateRecoveryFailure() {
        // Arrange
        when(recoveryCoordinator.recover()).thenThrow(new IllegalStateException("Recovery failed"));
        // Act & Assert
        assertThrows(IllegalStateException.class, startupRecovery::afterSingletonsInstantiated);
        verifyNoInteractions(eventStore);
    }

    @Test
    void shouldRejectNullDependencies() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> new PersistenceStartupRecovery(null, eventStore));
        assertThrows(NullPointerException.class, () -> new PersistenceStartupRecovery(recoveryCoordinator, null));
    }
}
