package com.timetable.backend.controller;

import com.timetable.backend.domain.dto.DanceStyleDTO;
import com.timetable.backend.domain.dto.DanceStylesResponse;
import com.timetable.backend.domain.dto.RoomDTO;
import com.timetable.backend.domain.dto.RoomsResponse;
import com.timetable.backend.domain.mapper.DictionaryMapper;
import com.timetable.backend.domain.model.DanceStyle;
import com.timetable.backend.domain.model.Room;
import com.timetable.backend.domain.repository.DanceStyleRepository;
import com.timetable.backend.domain.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/dictionaries")
@RequiredArgsConstructor
public class DictionaryController {

    private final RoomRepository roomRepository;
    private final DanceStyleRepository danceStyleRepository;
    private final DictionaryMapper dictionaryMapper;

    // Rooms (ROLE_ADMIN)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/rooms")
    public ResponseEntity<RoomDTO> createRoom(@RequestBody RoomDTO roomDTO) {
        Room room = dictionaryMapper.toRoom(roomDTO);
        Room saved = roomRepository.save(room);
        return ResponseEntity.ok(dictionaryMapper.toRoomDTO(saved));
    }


    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/rooms")
    public ResponseEntity<RoomsResponse> listRooms() {
        List<RoomDTO> rooms = roomRepository.findAll().stream()
                .map(dictionaryMapper::toRoomDTO)
                .toList();
        return ResponseEntity.ok(new RoomsResponse(rooms));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/rooms/{id}")
    public ResponseEntity<RoomDTO> getRoom(@PathVariable Long id) {
        Optional<Room> r = roomRepository.findById(id);
        return r.map(room -> ResponseEntity.ok(dictionaryMapper.toRoomDTO(room)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/rooms/{id}")
    public ResponseEntity<RoomDTO> updateRoom(@PathVariable Long id, @RequestBody RoomDTO updated) {
        return roomRepository.findById(id).map(r -> {
            r.setName(updated.name());
            r.setCapacity(updated.capacity());
            r.setAllowsParallelPrivate(updated.allowsParallelPrivate());
            roomRepository.save(r);
            return ResponseEntity.ok(dictionaryMapper.toRoomDTO(r));
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
    public ResponseEntity<DanceStyleDTO> createStyle(@RequestBody DanceStyleDTO styleDTO) {
        DanceStyle style = dictionaryMapper.toDanceStyle(styleDTO);
        DanceStyle saved = danceStyleRepository.save(style);
        return ResponseEntity.ok(dictionaryMapper.toDanceStyleDTO(saved));
    }


    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/styles")
    public ResponseEntity<DanceStylesResponse> listStyles() {
        List<DanceStyleDTO> styles = danceStyleRepository.findAll().stream()
                .map(dictionaryMapper::toDanceStyleDTO)
                .toList();
        return ResponseEntity.ok(new DanceStylesResponse(styles));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/styles/{id}")
    public ResponseEntity<DanceStyleDTO> getStyle(@PathVariable Long id) {
        Optional<DanceStyle> s = danceStyleRepository.findById(id);
        return s.map(style -> ResponseEntity.ok(dictionaryMapper.toDanceStyleDTO(style)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/styles/{id}")
    public ResponseEntity<DanceStyleDTO> updateStyle(@PathVariable Long id, @RequestBody DanceStyleDTO updated) {
        return danceStyleRepository.findById(id).map(s -> {
            s.setName(updated.name());
            danceStyleRepository.save(s);
            return ResponseEntity.ok(dictionaryMapper.toDanceStyleDTO(s));
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
