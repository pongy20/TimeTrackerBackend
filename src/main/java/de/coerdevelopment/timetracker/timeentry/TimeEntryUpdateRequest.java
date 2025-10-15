package de.coerdevelopment.timetracker.timeentry;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record TimeEntryUpdateRequest(
        @NotBlank String subject,
        String description,
        @NotNull LocalDate dateWorked,
        @NotNull @Min(1) Integer minutesWorked
) {}

