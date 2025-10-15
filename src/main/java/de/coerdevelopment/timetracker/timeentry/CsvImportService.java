package de.coerdevelopment.timetracker.timeentry;

import com.opencsv.CSVReaderHeaderAware;
import de.coerdevelopment.timetracker.user.User;
import de.coerdevelopment.timetracker.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class CsvImportService {
    private static final Logger log = LoggerFactory.getLogger(CsvImportService.class);

    public record ImportResult(int imported, int skipped, int errors, int syncedUpdatedAtRows) {}

    private final TimeEntryRepository timeEntryRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate txTemplate;

    public CsvImportService(TimeEntryRepository timeEntryRepository,
                            UserRepository userRepository,
                            PasswordEncoder passwordEncoder,
                            PlatformTransactionManager txManager) {
        this.timeEntryRepository = timeEntryRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.txTemplate = new TransactionTemplate(txManager);
    }

    public ImportResult importCsv(Path path, Optional<String> defaultUsername, boolean dryRun) throws IOException {
        if (!Files.exists(path)) {
            throw new IOException("CSV file not found: " + path);
        }
        int imported = 0;
        int skipped = 0;
        int errors = 0;
        List<Long> importedIds = new ArrayList<>();

        try (CSVReaderHeaderAware reader = new CSVReaderHeaderAware(new FileReader(path.toFile()))) {
            Map<String, String> row;
            int rowNum = 1; // header handled by reader
            while ((row = reader.readMap()) != null) {
                rowNum++;
                try {
                    Map<String, String> norm = normalizeKeys(row);

                    String username = firstNonBlank(norm, List.of("username", "user", "email")).orElse(null);
                    if (username == null) {
                        username = defaultUsername.orElse(null);
                    }
                    if (isBlank(username)) {
                        log.warn("Row {} skipped: missing username and no default username provided", rowNum);
                        skipped++;
                        continue;
                    }
                    User user = userRepository.findByUsername(username).orElseGet(() -> createImportedUser(username));

                    String subject = firstNonBlank(norm, List.of("subject", "title", "betreff", "task")).orElse(null);
                    if (isBlank(subject)) {
                        log.warn("Row {} skipped: missing subject", rowNum);
                        skipped++;
                        continue;
                    }
                    String description = firstNonBlank(norm, List.of("description", "desc", "beschreibung", "notes", "note")).orElse("");

                    LocalDate dateWorked = parseDate(firstNonBlank(norm, List.of("dateworked", "date", "workdate", "datum", "day")).orElse(null));
                    if (dateWorked == null) {
                        log.warn("Row {} skipped: invalid/missing dateWorked", rowNum);
                        skipped++;
                        continue;
                    }

                    Integer minutes = parseInt(firstNonBlank(norm, List.of("minutesworked", "minutes", "duration", "dauer", "mins", "zeitmin")).orElse(null));
                    if (minutes == null || minutes <= 0) {
                        log.warn("Row {} skipped: invalid minutesWorked", rowNum);
                        skipped++;
                        continue;
                    }

                    Instant createdAt = parseInstant(firstNonBlank(norm, List.of("createdat", "created", "erstelltam")).orElse(null));
                    if (createdAt == null) {
                        createdAt = dateWorked.atStartOfDay(ZoneId.systemDefault()).toInstant();
                    }
                    Instant updatedAt = parseInstant(firstNonBlank(norm, List.of("updatedat", "lastupdated", "modified", "geaendertam")).orElse(null));
                    if (updatedAt == null) {
                        updatedAt = createdAt;
                    }

                    // Dedup
                    boolean duplicate = timeEntryRepository.existsByUserAndSubjectAndDateWorkedAndMinutesWorked(user, subject, dateWorked, minutes);
                    if (duplicate) {
                        skipped++;
                        continue;
                    }

                    if (dryRun) {
                        imported++;
                        continue;
                    }

                    TimeEntry e = new TimeEntry();
                    e.setUser(user);
                    e.setSubject(subject);
                    e.setDescription(description);
                    e.setDateWorked(dateWorked);
                    e.setMinutesWorked(minutes);
                    e.setCreatedAt(createdAt);
                    e.setUpdatedAt(updatedAt);
                    TimeEntry saved = timeEntryRepository.save(e);
                    if (saved.getId() != null) importedIds.add(saved.getId());
                    imported++;
                } catch (Exception ex) {
                    errors++;
                    log.warn("Row {} error: {}", rowNum, ex.getMessage());
                }
            }
        }
        int synced = 0;
        if (!dryRun && !importedIds.isEmpty()) {
            synced = txTemplate.execute(status -> timeEntryRepository.syncUpdatedAtToCreatedAt(importedIds));
        }
        return new ImportResult(imported, skipped, errors, synced == 0 ? 0 : synced);
    }

    // Helpers
    private Map<String, String> normalizeKeys(Map<String, String> original) {
        Map<String, String> norm = new HashMap<>();
        for (Map.Entry<String, String> e : original.entrySet()) {
            if (e.getKey() == null) continue;
            String k = e.getKey().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
            norm.put(k, e.getValue());
        }
        return norm;
    }

    private Optional<String> firstNonBlank(Map<String, String> norm, List<String> keys) {
        for (String k : keys) {
            String v = norm.get(k);
            if (!isBlank(v)) return Optional.of(v.trim());
        }
        return Optional.empty();
    }

    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private Integer parseInt(String s) {
        if (isBlank(s)) return null;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    private LocalDate parseDate(String s) {
        if (isBlank(s)) return null;
        List<DateTimeFormatter> fmts = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("dd.MM.yyyy"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy")
        );
        for (DateTimeFormatter f : fmts) {
            try { return LocalDate.parse(s.trim(), f); } catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    private Instant parseInstant(String s) {
        if (isBlank(s)) return null;
        List<DateTimeFormatter> instantLike = List.of(
                DateTimeFormatter.ISO_INSTANT,
                DateTimeFormatter.ISO_OFFSET_DATE_TIME,
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
        );
        for (DateTimeFormatter f : instantLike) {
            try {
                if (f == DateTimeFormatter.ISO_LOCAL_DATE_TIME) {
                    LocalDateTime ldt = LocalDateTime.parse(s.trim(), f);
                    return ldt.atZone(ZoneId.systemDefault()).toInstant();
                } else {
                    return Instant.from(f.parse(s.trim()));
                }
            } catch (DateTimeParseException ignored) {}
        }
        LocalDate d = parseDate(s);
        if (d != null) return d.atStartOfDay(ZoneId.systemDefault()).toInstant();
        return null;
    }

    private User createImportedUser(String username) {
        User u = new User();
        u.setUsername(username);
        String pw = "imported-" + UUID.randomUUID();
        u.setPassword(passwordEncoder.encode(pw));
        u.setRole("USER");
        return userRepository.save(u);
    }
}

