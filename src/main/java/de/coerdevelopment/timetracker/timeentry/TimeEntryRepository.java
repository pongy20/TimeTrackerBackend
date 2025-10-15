package de.coerdevelopment.timetracker.timeentry;

import de.coerdevelopment.timetracker.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long> {
    List<TimeEntry> findAllByUserOrderByDateWorkedDescIdDesc(User user);
    Optional<TimeEntry> findByIdAndUser(Long id, User user);
}
