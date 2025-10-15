package de.coerdevelopment.timetracker.timeentry;

import de.coerdevelopment.timetracker.user.User;
import de.coerdevelopment.timetracker.user.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TimeEntryService {
    private final TimeEntryRepository repository;
    private final UserRepository userRepository;

    public TimeEntryService(TimeEntryRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    private User currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow();
    }

    @Transactional
    public TimeEntryResponse create(TimeEntryCreateRequest req) {
        User user = currentUser();
        TimeEntry e = new TimeEntry();
        e.setUser(user);
        e.setSubject(req.subject());
        e.setDescription(req.description());
        e.setDateWorked(req.dateWorked());
        e.setMinutesWorked(req.minutesWorked());
        TimeEntry saved = repository.save(e);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TimeEntryResponse> list() {
        User user = currentUser();
        return repository.findAllByUserOrderByDateWorkedDescIdDesc(user).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TimeEntryResponse get(Long id) {
        User user = currentUser();
        TimeEntry e = repository.findByIdAndUser(id, user).orElseThrow();
        return toResponse(e);
    }

    @Transactional
    public TimeEntryResponse update(Long id, TimeEntryUpdateRequest req) {
        User user = currentUser();
        TimeEntry e = repository.findByIdAndUser(id, user).orElseThrow();
        e.setSubject(req.subject());
        e.setDescription(req.description());
        e.setDateWorked(req.dateWorked());
        e.setMinutesWorked(req.minutesWorked());
        return toResponse(e);
    }

    @Transactional
    public void delete(Long id) {
        User user = currentUser();
        TimeEntry e = repository.findByIdAndUser(id, user).orElseThrow();
        repository.delete(e);
    }

    private TimeEntryResponse toResponse(TimeEntry e) {
        return new TimeEntryResponse(
                e.getId(),
                e.getSubject(),
                e.getDescription(),
                e.getDateWorked(),
                e.getMinutesWorked(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}

