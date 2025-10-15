package de.coerdevelopment.timetracker.timeentry;

import java.time.Instant;
import java.time.LocalDate;

public record TimeEntryResponse(
        Long id,
        String subject,
        String description,
        LocalDate dateWorked,
        Integer minutesWorked,
        Instant createdAt,
        Instant updatedAt
) {}

