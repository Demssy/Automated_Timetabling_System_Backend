package com.timetable.backend.controller;

import com.timetable.backend.domain.dto.DanceGroupDTO;
import com.timetable.backend.domain.dto.DanceStyleDTO;
import com.timetable.backend.domain.dto.DanceStylesResponse;
import com.timetable.backend.domain.dto.RoomDTO;
import com.timetable.backend.domain.dto.RoomsResponse;
import com.timetable.backend.domain.dto.TimeslotDTO;
import com.timetable.backend.domain.mapper.DictionaryMapper;
import com.timetable.backend.domain.model.DanceGroup;
import com.timetable.backend.domain.model.DanceStyle;
import com.timetable.backend.domain.model.Room;
import com.timetable.backend.domain.model.Timeslot;
import com.timetable.backend.domain.repository.DanceGroupRepository;
import com.timetable.backend.domain.repository.DanceStyleRepository;
import com.timetable.backend.domain.repository.RoomRepository;
import com.timetable.backend.domain.repository.TimeslotRepository;
import jakarta.validation.Valid;
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
    private final TimeslotRepository timeslotRepository;
    private final DanceGroupRepository danceGroupRepository;
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
        Optional<Room> r = roomRepository.findById(id);
        return r.map(room -> ResponseEntity.ok(dictionaryMapper.toRoomDTO(room)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/rooms/{id}")
    public ResponseEntity<RoomDTO> updateRoom(@PathVariable Long id, @Valid @RequestBody RoomDTO updated) {
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

    // Dance styles — public list, admin-only modifications
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping({"/styles", "/dance-styles"})
    public ResponseEntity<DanceStyleDTO> createStyle(@Valid @RequestBody DanceStyleDTO styleDTO) {
        DanceStyle style = dictionaryMapper.toDanceStyle(styleDTO);
        DanceStyle saved = danceStyleRepository.save(style);
        return ResponseEntity.ok(dictionaryMapper.toDanceStyleDTO(saved));
    }

    @GetMapping("/styles")
    public ResponseEntity<DanceStylesResponse> listStyles() {
        List<DanceStyleDTO> styles = danceStyleRepository.findAll().stream()
                .map(dictionaryMapper::toDanceStyleDTO)
                .toList();
        return ResponseEntity.ok(new DanceStylesResponse(styles));
    }

    @GetMapping("/dance-styles")
    public ResponseEntity<List<DanceStyleDTO>> listDanceStyles() {
        List<DanceStyleDTO> styles = danceStyleRepository.findAll().stream()
                .map(dictionaryMapper::toDanceStyleDTO)
                .toList();
        return ResponseEntity.ok(styles);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping({"/styles/{id}", "/dance-styles/{id}"})
    public ResponseEntity<DanceStyleDTO> getStyle(@PathVariable Long id) {
        Optional<DanceStyle> s = danceStyleRepository.findById(id);
        return s.map(style -> ResponseEntity.ok(dictionaryMapper.toDanceStyleDTO(style)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping({"/styles/{id}", "/dance-styles/{id}"})
    public ResponseEntity<DanceStyleDTO> updateStyle(@PathVariable Long id, @Valid @RequestBody DanceStyleDTO updated) {
        return danceStyleRepository.findById(id).map(s -> {
            s.setName(updated.name());
            danceStyleRepository.save(s);
            return ResponseEntity.ok(dictionaryMapper.toDanceStyleDTO(s));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping({"/styles/{id}", "/dance-styles/{id}"})
    public ResponseEntity<?> deleteStyle(@PathVariable Long id) {
        if (danceStyleRepository.existsById(id)) {
            danceStyleRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    // Timeslots (ROLE_ADMIN)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/timeslots")
    public ResponseEntity<TimeslotDTO> createTimeslot(@Valid @RequestBody TimeslotDTO timeslotDTO) {
        Timeslot timeslot = dictionaryMapper.toTimeslot(timeslotDTO);
        Timeslot saved = timeslotRepository.save(timeslot);
        return ResponseEntity.ok(dictionaryMapper.toTimeslotDTO(saved));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/timeslots")
    public ResponseEntity<List<TimeslotDTO>> listTimeslots() {
        List<TimeslotDTO> timeslots = timeslotRepository.findAll().stream()
                .map(dictionaryMapper::toTimeslotDTO)
                .toList();
        return ResponseEntity.ok(timeslots);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/timeslots/{id}")
    public ResponseEntity<TimeslotDTO> getTimeslot(@PathVariable Long id) {
        Optional<Timeslot> t = timeslotRepository.findById(id);
        return t.map(timeslot -> ResponseEntity.ok(dictionaryMapper.toTimeslotDTO(timeslot)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/timeslots/{id}")
    public ResponseEntity<TimeslotDTO> updateTimeslot(@PathVariable Long id, @Valid @RequestBody TimeslotDTO updated) {
        return timeslotRepository.findById(id).map(t -> {
            t.setDayOfWeek(updated.dayOfWeek());
            t.setStartTime(updated.startTime());
            t.setEndTime(updated.endTime());
            timeslotRepository.save(t);
            return ResponseEntity.ok(dictionaryMapper.toTimeslotDTO(t));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/timeslots/{id}")
    public ResponseEntity<?> deleteTimeslot(@PathVariable Long id) {
        if (timeslotRepository.existsById(id)) {
            timeslotRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    // Dance Groups (ROLE_ADMIN)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/groups")
    public ResponseEntity<DanceGroupDTO> createGroup(@Valid @RequestBody DanceGroupDTO groupDTO) {
        DanceGroup group = dictionaryMapper.toDanceGroup(groupDTO);

        // Resolve DanceStyle
        DanceStyle style = danceStyleRepository.findById(groupDTO.danceStyleId())
                .orElseThrow(() -> new IllegalArgumentException("Dance Style not found with id: " + groupDTO.danceStyleId()));
        group.setDanceStyle(style);

        DanceGroup saved = danceGroupRepository.save(group);
        return ResponseEntity.ok(dictionaryMapper.toDanceGroupDTO(saved));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/groups")
    public ResponseEntity<List<DanceGroupDTO>> listGroups() {
        List<DanceGroupDTO> groups = danceGroupRepository.findAll().stream()
                .map(dictionaryMapper::toDanceGroupDTO)
                .toList();
        return ResponseEntity.ok(groups);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/groups/{id}")
    public ResponseEntity<DanceGroupDTO> getGroup(@PathVariable Long id) {
        Optional<DanceGroup> g = danceGroupRepository.findById(id);
        return g.map(group -> ResponseEntity.ok(dictionaryMapper.toDanceGroupDTO(group)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/groups/{id}")
    public ResponseEntity<DanceGroupDTO> updateGroup(@PathVariable Long id, @Valid @RequestBody DanceGroupDTO updated) {
        return danceGroupRepository.findById(id).map(g -> {
            g.setName(updated.name());
            g.setDanceLevel(updated.danceLevel());
            g.setMinSize(updated.minSize());
            g.setTargetAgeRange(updated.targetAgeRange());

            if (!g.getDanceStyle().getId().equals(updated.danceStyleId())) {
                DanceStyle style = danceStyleRepository.findById(updated.danceStyleId())
                        .orElseThrow(() -> new IllegalArgumentException("Dance Style not found with id: " + updated.danceStyleId()));
                g.setDanceStyle(style);
            }

            danceGroupRepository.save(g);
            return ResponseEntity.ok(dictionaryMapper.toDanceGroupDTO(g));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/groups/{id}")
    public ResponseEntity<?> deleteGroup(@PathVariable Long id) {
        if (danceGroupRepository.existsById(id)) {
            danceGroupRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
