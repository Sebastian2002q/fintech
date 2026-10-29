package co.edu.uniquindio.fintech_backend.persistence.snapshot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.Optional;

@Repository
public class JsonSnapshotStore implements SnapshotStore {
    private final Path filePath;
    private final JsonMapper jsonMapper;

    public JsonSnapshotStore(@Value("${fintech.persistence.snapshot-file:data/snapshot.json}") String fileName, JsonMapper jsonMapper) {
        this.filePath = Path.of(fileName).toAbsolutePath().normalize();
        this.jsonMapper = Objects.requireNonNull(jsonMapper);
    }

    @Override
    public synchronized void save(StoredSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "Snapshot cannot be null");
        Optional<StoredSnapshot> currentSnapshot = loadLatest();
        if (currentSnapshot.isPresent() && snapshot.lastSequence() <= currentSnapshot.get().lastSequence()) {
            throw new IllegalArgumentException("Snapshot sequence must be greater than the current one");
        }
        Path parent = filePath.getParent();
        Path temporaryFile = null;
        try {
            if (parent != null) {
                Files.createDirectories(parent);
            }
            String json = jsonMapper.writeValueAsString(snapshot);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            temporaryFile = Files.createTempFile(parent, "snapshot-", ".tmp");
            try (FileChannel channel = FileChannel.open(temporaryFile, StandardOpenOption.WRITE)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                channel.force(true);
            }
            Files.move(temporaryFile, filePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            throw new IllegalStateException("Atomic snapshot replacement is not supported", e);
        } catch (IOException e) {
            throw new IllegalStateException("Could not save snapshot", e);
        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException ignored) {
                    // Temporary file cleanup failed
                }
            }
        }
    }

    @Override
    public synchronized Optional<StoredSnapshot> loadLatest() {
        if (Files.notExists(filePath)) {
            return Optional.empty();
        }
        try {
            String json = Files.readString(filePath, StandardCharsets.UTF_8);
            StoredSnapshot snapshot = jsonMapper.readValue(json, StoredSnapshot.class);
            if (snapshot == null) {
                throw new IllegalStateException("Snapshot file contains no snapshot");
            }
            return Optional.of(snapshot);
        } catch (JacksonException e) {
            throw new IllegalStateException("Invalid snapshot JSON", e);
        } catch (IOException e) {
            throw new IllegalStateException("Could not load snapshot", e);
        }
    }
}
