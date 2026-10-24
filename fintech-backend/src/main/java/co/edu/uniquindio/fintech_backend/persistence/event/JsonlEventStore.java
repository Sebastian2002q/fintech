package co.edu.uniquindio.fintech_backend.persistence.event;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class JsonlEventStore implements EventStore {
    private final Path filePath;
    private final JsonMapper jsonMapper;
    private long lastSequence;
    private boolean writable = true;

    public JsonlEventStore(@Value("${fintech.persistence.events-file:data/events.jsonl}") String fileName, JsonMapper jsonMapper) {
        this.filePath = Path.of(fileName);
        this.jsonMapper = Objects.requireNonNull(jsonMapper);
        initialize();
    }

    private void initialize() {
        try {
            Path parent = filePath.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            if (Files.notExists(filePath)) {
                Files.createFile(filePath);
            }
            List<StoredEvent> events = readAfter(0);
            lastSequence = events.isEmpty() ? 0 : events.getLast().sequence();
        } catch (IOException e) {
            throw new IllegalStateException("Could not initialize event store", e);
        }
    }

    @Override
    public synchronized void append(StoredEvent event) {
        Objects.requireNonNull(event);
        if (!writable) {
            throw new IllegalStateException("Event store requires recovery");
        }
        if (event.sequence() != lastSequence + 1) {
            throw new IllegalArgumentException("Invalid event sequence");
        }
        String json = jsonMapper.writeValueAsString(event);
        byte[] bytes = (json + "\n").getBytes(StandardCharsets.UTF_8);
        try (FileChannel channel = FileChannel.open(filePath, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            channel.force(true);
            lastSequence = event.sequence();
        } catch (IOException e) {
            writable = false;
            throw new IllegalStateException("Could not persist event", e);
        }
    }

    @Override
    public synchronized List<StoredEvent> readAfter(long sequence) {
        if (sequence < 0) {
            throw new IllegalArgumentException("Sequence cannot be negative");
        }
        List<StoredEvent> events = new ArrayList<>();
        try {
            if (Files.notExists(filePath)) {
                throw new IllegalStateException("Event file does not exist");
            }
            validateFileEnding();
            try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
                String line;
                long expectedSequence = 1;
                while ((line = reader.readLine()) != null) {
                    StoredEvent event = jsonMapper.readValue(line, StoredEvent.class);
                    if (event.sequence() != expectedSequence) {
                        throw new IllegalStateException("Invalid event sequence in file");
                    }
                    expectedSequence++;
                    if (event.sequence() > sequence) {
                        events.add(event);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not read events", e);
        }
        return List.copyOf(events);
    }

    private void validateFileEnding() throws IOException {
        try (FileChannel channel = FileChannel.open(filePath, StandardOpenOption.READ)) {
            long size = channel.size();
            if (size == 0) {
                return;
            }
            ByteBuffer buffer = ByteBuffer.allocate(1);
            channel.read(buffer, size - 1);
            buffer.flip();
            if (buffer.get() != '\n') {
                throw new IllegalStateException("Incomplete event log");
            }
        }
    }

    @Override
    public synchronized long getLastSequence() {
        return lastSequence;
    }
}
