package com.timetable.backend.service;

import com.timetable.backend.domain.dto.RoomDTO;
import com.timetable.backend.domain.mapper.DictionaryMapper;
import com.timetable.backend.domain.model.Room;
import com.timetable.backend.domain.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing Room entities.
 * Provides CRUD operations for dance rooms used in schedule generation.
 */
@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final DictionaryMapper dictionaryMapper;

    /**
     * Returns all rooms.
     *
     * @return list of all RoomDTOs
     */
    @Transactional(readOnly = true)
    public List<RoomDTO> getAllRooms() {
        return roomRepository.findAll().stream()
                .map(dictionaryMapper::toRoomDTO)
                .toList();
    }

    /**
     * Returns a room by ID.
     *
     * @param id room identifier
     * @return RoomDTO
     * @throws IllegalArgumentException if room not found
     */
    @Transactional(readOnly = true)
    public RoomDTO getRoomById(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with id: " + id));
        return dictionaryMapper.toRoomDTO(room);
    }

    /**
     * Creates a new room.
     *
     * @param dto room data (name, capacity, allowsParallelPrivate)
     * @return created RoomDTO
     * @throws IllegalArgumentException if a room with the same name already exists
     */
    @Transactional
    public RoomDTO createRoom(RoomDTO dto) {
        if (roomRepository.findByName(dto.name()).isPresent()) {
            throw new IllegalArgumentException("Room with name '" + dto.name() + "' already exists");
        }
        Room saved = roomRepository.save(dictionaryMapper.toRoom(dto));
        return dictionaryMapper.toRoomDTO(saved);
    }

    /**
     * Updates an existing room.
     *
     * @param id  room identifier
     * @param dto updated room data
     * @return updated RoomDTO
     * @throws IllegalArgumentException if room not found
     */
    @Transactional
    public RoomDTO updateRoom(Long id, RoomDTO dto) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with id: " + id));

        room.setName(dto.name());
        room.setCapacity(dto.capacity());
        room.setAllowsParallelPrivate(dto.allowsParallelPrivate());

        return dictionaryMapper.toRoomDTO(roomRepository.save(room));
    }

    /**
     * Deletes a room by ID.
     *
     * @param id room identifier
     * @throws IllegalArgumentException if room not found
     */
    @Transactional
    public void deleteRoom(Long id) {
        if (!roomRepository.existsById(id)) {
            throw new IllegalArgumentException("Room not found with id: " + id);
        }
        roomRepository.deleteById(id);
    }
}
