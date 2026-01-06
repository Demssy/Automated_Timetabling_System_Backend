package com.timetable.backend.controller;

import com.timetable.backend.domain.dto.DanceStyleDTO;
import com.timetable.backend.domain.dto.DanceStylesResponse;
import com.timetable.backend.domain.dto.RoomDTO;
import com.timetable.backend.domain.dto.RoomsResponse;
import com.timetable.backend.domain.exception.ResourceNotFoundException;
import com.timetable.backend.domain.mapper.DictionaryMapper;
import com.timetable.backend.domain.model.DanceStyle;
import com.timetable.backend.domain.model.Room;
import com.timetable.backend.domain.repository.DanceStyleRepository;
import com.timetable.backend.domain.repository.RoomRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dictionaries")
@RequiredArgsConstructor
public class DictionaryController {

    private final RoomRepository roomRepository;
    private final DanceStyleRepository danceStyleRepository;
    private final DictionaryMapper dictionaryMapper;

    // Rooms (ROLE_ADMIN)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/rooms")
    public ResponseEntity<RoomDTO> createRoom(@Valid @RequestBody RoomDTO roomDTO) {
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
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room", id));
        return ResponseEntity.ok(dictionaryMapper.toRoomDTO(room));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/rooms/{id}")
    public ResponseEntity<RoomDTO> updateRoom(@PathVariable Long id, @Valid @RequestBody RoomDTO updated) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room", id));

        room.setName(updated.name());
        room.setCapacity(updated.capacity());
        room.setAllowsParallelPrivate(updated.allowsParallelPrivate());
        roomRepository.save(room);

        return ResponseEntity.ok(dictionaryMapper.toRoomDTO(room));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/rooms/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        if (!roomRepository.existsById(id)) {
            throw new ResourceNotFoundException("Room", id);
        }
        roomRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // Dance styles (ROLE_ADMIN)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/styles")
    public ResponseEntity<DanceStyleDTO> createStyle(@Valid @RequestBody DanceStyleDTO styleDTO) {
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
        DanceStyle style = danceStyleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DanceStyle", id));
        return ResponseEntity.ok(dictionaryMapper.toDanceStyleDTO(style));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/styles/{id}")
    public ResponseEntity<DanceStyleDTO> updateStyle(@PathVariable Long id, @Valid @RequestBody DanceStyleDTO updated) {
        DanceStyle style = danceStyleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DanceStyle", id));

        style.setName(updated.name());
        danceStyleRepository.save(style);

        return ResponseEntity.ok(dictionaryMapper.toDanceStyleDTO(style));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/styles/{id}")
    public ResponseEntity<Void> deleteStyle(@PathVariable Long id) {
        if (!danceStyleRepository.existsById(id)) {
            throw new ResourceNotFoundException("DanceStyle", id);
        }
        danceStyleRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
