package com.timetable.backend.controller;

import com.timetable.backend.domain.model.DanceStyle;
import com.timetable.backend.domain.model.Room;
import com.timetable.backend.domain.repository.DanceStyleRepository;
import com.timetable.backend.domain.repository.RoomRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/dictionaries")
public class DictionaryController {

    private final RoomRepository roomRepository;
    private final DanceStyleRepository danceStyleRepository;

    public DictionaryController(RoomRepository roomRepository, DanceStyleRepository danceStyleRepository) {
        this.roomRepository = roomRepository;
        this.danceStyleRepository = danceStyleRepository;
    }

    // Rooms (ROLE_ADMIN)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/rooms")
    public ResponseEntity<Room> createRoom(@RequestBody Room room) {
        Room saved = roomRepository.save(room);
        return ResponseEntity.ok(saved);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/rooms")
    public List<Room> listRooms() {
        return roomRepository.findAll();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/rooms/{id}")
    public ResponseEntity<Room> getRoom(@PathVariable Long id) {
        Optional<Room> r = roomRepository.findById(id);
        return r.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/rooms/{id}")
    public ResponseEntity<Room> updateRoom(@PathVariable Long id, @RequestBody Room updated) {
        return roomRepository.findById(id).map(r -> {
            r.setName(updated.getName());
            r.setCapacity(updated.getCapacity());
            r.setAllowsParallelPrivate(updated.isAllowsParallelPrivate());
            roomRepository.save(r);
            return ResponseEntity.ok(r);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/rooms/{id}")
    public ResponseEntity<?> deleteRoom(@PathVariable Long id) {
        if (roomRepository.existsById(id)) {
            roomRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    // Dance styles (ROLE_ADMIN)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/styles")
    public ResponseEntity<DanceStyle> createStyle(@RequestBody DanceStyle style) {
        DanceStyle saved = danceStyleRepository.save(style);
        return ResponseEntity.ok(saved);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/styles")
    public List<DanceStyle> listStyles() {
        return danceStyleRepository.findAll();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/styles/{id}")
    public ResponseEntity<DanceStyle> getStyle(@PathVariable Long id) {
        Optional<DanceStyle> s = danceStyleRepository.findById(id);
        return s.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/styles/{id}")
    public ResponseEntity<DanceStyle> updateStyle(@PathVariable Long id, @RequestBody DanceStyle updated) {
        return danceStyleRepository.findById(id).map(s -> {
            s.setName(updated.getName());
            danceStyleRepository.save(s);
            return ResponseEntity.ok(s);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/styles/{id}")
    public ResponseEntity<?> deleteStyle(@PathVariable Long id) {
        if (danceStyleRepository.existsById(id)) {
            danceStyleRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}

