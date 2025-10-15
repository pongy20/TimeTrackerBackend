package de.coerdevelopment.timetracker.timeentry;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/imports")
@Tag(name = "Imports")
@SecurityRequirement(name = "bearerAuth")
public class ImportController {

    private final CsvImportService importService;
    private final Environment env;

    public ImportController(CsvImportService importService, Environment env) {
        this.importService = importService;
        this.env = env;
    }

    @PostMapping("/time-entries")
    @Operation(summary = "Importiere Time Entries aus CSV im Import-Ordner",
            description = "Liest eine CSV-Datei aus dem Import-Ordner (ENV IMPORT_DIR, Default /app/imports). " +
                    "Dateiname ohne Pfadangaben übergeben. Optional: username (Fallback, wenn CSV keine username-Spalte hat), dryRun=true für Testlauf.")
    public ResponseEntity<?> importTimeEntries(
            @RequestParam("filename") String filename,
            @RequestParam(value = "username", required = false) String defaultUsername,
            @RequestParam(value = "dryRun", required = false, defaultValue = "false") boolean dryRun
    ) {
        try {
            Path baseDir = Path.of(Optional.ofNullable(env.getProperty("IMPORT_DIR")).orElse("/app/imports"));
            Path normalizedBase = baseDir.toAbsolutePath().normalize();
            Path candidate = normalizedBase.resolve(filename).normalize();

            // Path Traversal verhindern
            if (!candidate.startsWith(normalizedBase)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Invalid filename",
                        "message", "Filename must not contain path separators"
                ));
            }
            if (!Files.exists(candidate)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "File not found",
                        "path", candidate.toString()
                ));
            }

            CsvImportService.ImportResult res = importService.importCsv(candidate, Optional.ofNullable(defaultUsername), dryRun);
            return ResponseEntity.ok(Map.of(
                    "file", candidate.toString(),
                    "imported", res.imported(),
                    "skipped", res.skipped(),
                    "errors", res.errors(),
                    "syncedUpdatedAtRows", res.syncedUpdatedAtRows(),
                    "dryRun", dryRun
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Import failed",
                    "message", e.getMessage()
            ));
        }
    }
}

