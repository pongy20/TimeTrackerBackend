package de.coerdevelopment.timetracker.timeentry;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/time-entries")
@Tag(name = "Time Entries")
@SecurityRequirement(name = "bearerAuth")
public class TimeEntryController {
    private final TimeEntryService service;

    public TimeEntryController(TimeEntryService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Track time")
    public ResponseEntity<TimeEntryResponse> create(@RequestBody @Valid TimeEntryCreateRequest request) {
        TimeEntryResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/time-entries/" + created.id())).body(created);
    }

    @GetMapping
    @Operation(summary = "List tracked times")
    public ResponseEntity<List<TimeEntryResponse>> list() {
        return ResponseEntity.ok(service.list());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Time entry detail")
    public ResponseEntity<TimeEntryResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit time entry")
    public ResponseEntity<TimeEntryResponse> update(@PathVariable Long id, @RequestBody @Valid TimeEntryUpdateRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete time entry")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
