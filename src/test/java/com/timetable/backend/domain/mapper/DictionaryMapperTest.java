package com.timetable.backend.domain.mapper;

import com.timetable.backend.domain.dto.DanceStyleDTO;
import com.timetable.backend.domain.dto.RoomDTO;
import com.timetable.backend.domain.model.DanceStyle;
import com.timetable.backend.domain.model.Room;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DictionaryMapperTest {

    private final DictionaryMapper mapper = Mappers.getMapper(DictionaryMapper.class);

    @Test
    void toRoomDTO() {
        Room room = new Room("Room 1", 10, true);
        room.setId(1L);

        RoomDTO dto = mapper.toRoomDTO(room);

        assertNotNull(dto);
        assertEquals(room.getId(), dto.id());
        assertEquals(room.getName(), dto.name());
        assertEquals(room.getCapacity(), dto.capacity());
        assertEquals(room.isAllowsParallelPrivate(), dto.allowsParallelPrivate());
    }

    @Test
    void toRoom() {
        RoomDTO dto = new RoomDTO(1L, "Room 1", 10, true);

        Room room = mapper.toRoom(dto);

        assertNotNull(room);
        assertEquals(dto.id(), room.getId());
        assertEquals(dto.name(), room.getName());
        assertEquals(dto.capacity(), room.getCapacity());
        assertEquals(dto.allowsParallelPrivate(), room.isAllowsParallelPrivate());
    }

    @Test
    void toDanceStyleDTO() {
        DanceStyle style = new DanceStyle("Salsa");
        style.setId(1L);

        DanceStyleDTO dto = mapper.toDanceStyleDTO(style);

        assertNotNull(dto);
        assertEquals(style.getId(), dto.id());
        assertEquals(style.getName(), dto.name());
    }

    @Test
    void toDanceStyle() {
        DanceStyleDTO dto = new DanceStyleDTO(1L, "Salsa");

        DanceStyle style = mapper.toDanceStyle(dto);

        assertNotNull(style);
        assertEquals(dto.id(), style.getId());
        assertEquals(dto.name(), style.getName());
    }
}

