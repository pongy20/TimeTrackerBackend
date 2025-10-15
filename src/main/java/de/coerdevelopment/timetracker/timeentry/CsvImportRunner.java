package de.coerdevelopment.timetracker.timeentry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Optional;

@Component
public class CsvImportRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(CsvImportRunner.class);

    private final CsvImportService importService;
    private final Environment env;

    public CsvImportRunner(CsvImportService importService, Environment env) {
        this.importService = importService;
        this.env = env;
    }

    @Override
    public void run(String... args) throws Exception {
        String csvPath = optionalEnv("IMPORT_CSV").orElse(null);
        if (csvPath == null || csvPath.isBlank()) {
            return; // kein Import angefordert
        }
        boolean dryRun = optionalEnv("IMPORT_DRY_RUN").map(String::trim).map(String::toLowerCase).map(v -> v.equals("true") || v.equals("1") || v.equals("yes")).orElse(false);
        Optional<String> defaultUsername = optionalEnv("IMPORT_USERNAME");
        try {
            CsvImportService.ImportResult res = importService.importCsv(Path.of(csvPath), defaultUsername, dryRun);
            log.info("CSV import finished{}: imported={}, skipped={}, errors={}, synced={}",
                    dryRun ? " (dry-run)" : "", res.imported(), res.skipped(), res.errors(), res.syncedUpdatedAtRows());
        } catch (Exception e) {
            log.error("CSV import failed: {}", e.getMessage());
        }
    }

    private Optional<String> optionalEnv(String key) {
        String v = env.getProperty(key);
        if (v == null) v = System.getenv(key);
        return Optional.ofNullable(v);
    }
}
