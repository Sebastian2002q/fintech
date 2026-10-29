package co.edu.uniquindio.fintech_backend.user.persistence.snapshot;

import co.edu.uniquindio.fintech_backend.persistence.snapshot.SnapshotParticipant;
import co.edu.uniquindio.fintech_backend.user.model.User;
import co.edu.uniquindio.fintech_backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class UserSnapshotService implements SnapshotParticipant<UserSnapshotState> {
    private static final String MODULE_KEY = "users";
    private final UserRepository userRepository;

    public UserSnapshotService(UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository);
    }

    @Override
    public String moduleKey() {
        return MODULE_KEY;
    }

    @Override
    public Class<UserSnapshotState> stateType() {
        return UserSnapshotState.class;
    }

    @Override
    public UserSnapshotState capture() {
        List<UserSnapshotEntry> entries = new ArrayList<>();
        for (User user : userRepository.findAll()) {
            entries.add(UserSnapshotMapper.toEntry(user));
        }
        return new UserSnapshotState(entries);
    }

    @Override
    public void restore(UserSnapshotState state) {
        Objects.requireNonNull(state, "Snapshot state is required");
        List<User> restoredUsers = new ArrayList<>();
        for (UserSnapshotEntry entry : state.users()) {
            restoredUsers.add(UserSnapshotMapper.toUser(entry));
        }
        userRepository.replaceAll(restoredUsers);
    }
}

