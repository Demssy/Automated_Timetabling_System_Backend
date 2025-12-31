package com.timetable.backend.domain.mapper;

import com.timetable.backend.domain.dto.DanceStyleDTO;
import com.timetable.backend.domain.dto.RoomDTO;
import com.timetable.backend.domain.model.DanceStyle;
import com.timetable.backend.domain.model.Room;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DictionaryMapper {
    RoomDTO toRoomDTO(Room room);
    Room toRoom(RoomDTO roomDTO);

    DanceStyleDTO toDanceStyleDTO(DanceStyle danceStyle);
    DanceStyle toDanceStyle(DanceStyleDTO danceStyleDTO);
}
