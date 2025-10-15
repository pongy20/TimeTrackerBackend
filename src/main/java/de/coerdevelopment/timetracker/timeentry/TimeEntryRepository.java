package de.coerdevelopment.timetracker.timeentry;

import de.coerdevelopment.timetracker.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long> {
    List<TimeEntry> findAllByUserOrderByDateWorkedDescIdDesc(User user);
    Optional<TimeEntry> findByIdAndUser(Long id, User user);
    boolean existsByUserAndSubjectAndDateWorkedAndMinutesWorked(User user, String subject, java.time.LocalDate dateWorked, Integer minutesWorked);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update TimeEntry e set e.updatedAt = e.createdAt where e.id in :ids")
    int syncUpdatedAtToCreatedAt(@Param("ids") List<Long> ids);
}
