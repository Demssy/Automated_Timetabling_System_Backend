package com.timetable.backend.controller;

import com.timetable.backend.domain.dto.ScheduleMetadataDTO;
import com.timetable.backend.service.ScheduleMetadataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleMetadataController {

    private final ScheduleMetadataService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT', 'TEACHER')")
    public ResponseEntity<List<ScheduleMetadataDTO>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT', 'TEACHER')")
    public ResponseEntity<ScheduleMetadataDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ScheduleMetadataDTO> create(@RequestBody @Valid ScheduleMetadataDTO dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ScheduleMetadataDTO> update(
            @PathVariable Long id,
            @RequestBody @Valid ScheduleMetadataDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    /**
     * Publishes a schedule, transitioning its status from DRAFT to PUBLISHED.
     * Only administrators are allowed to perform this action.
     *
     * @param id the schedule ID to publish
     * @return the updated schedule DTO with PUBLISHED status
     */
    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ScheduleMetadataDTO> publish(@PathVariable Long id) {
        return ResponseEntity.ok(service.publish(id));
    }

    /**
     * Archives a schedule, transitioning its status to ARCHIVED.
     *
     * @param id the schedule ID to archive
     * @return the updated schedule DTO with ARCHIVED status
     */
    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ScheduleMetadataDTO> archive(@PathVariable Long id) {
        return ResponseEntity.ok(service.archive(id));
    }

    /**
     * Reverts a schedule back to DRAFT status, allowing further editing.
     *
     * @param id the schedule ID to revert
     * @return the updated schedule DTO with DRAFT status
     */
    @PatchMapping("/{id}/draft")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ScheduleMetadataDTO> revertToDraft(@PathVariable Long id) {
        return ResponseEntity.ok(service.revertToDraft(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

