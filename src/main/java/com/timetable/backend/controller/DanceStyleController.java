package com.timetable.backend.controller;

import com.timetable.backend.domain.dto.DanceStyleDTO;
import com.timetable.backend.service.DanceStyleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dance-styles")
@RequiredArgsConstructor
public class DanceStyleController {

    private final DanceStyleService danceStyleService;

    @GetMapping
    public ResponseEntity<List<DanceStyleDTO>> getAllStyles() {
        return ResponseEntity.ok(danceStyleService.getAllStyles());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DanceStyleDTO> getStyleById(@PathVariable Long id) {
        return ResponseEntity.ok(danceStyleService.getStyleById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DanceStyleDTO> createStyle(@RequestBody @Valid DanceStyleDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(danceStyleService.createStyle(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DanceStyleDTO> updateStyle(@PathVariable Long id, @RequestBody @Valid DanceStyleDTO dto) {
        return ResponseEntity.ok(danceStyleService.updateStyle(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteStyle(@PathVariable Long id) {
        danceStyleService.deleteStyle(id);
        return ResponseEntity.noContent().build();
    }
}


