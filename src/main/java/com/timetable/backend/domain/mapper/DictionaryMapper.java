package com.timetable.backend.domain.mapper;

import com.timetable.backend.domain.dto.DanceStyleDTO;
import com.timetable.backend.domain.dto.RoomDTO;
import com.timetable.backend.domain.dto.TimeslotDTO;
import com.timetable.backend.domain.model.DanceStyle;
import com.timetable.backend.domain.model.Room;
import com.timetable.backend.domain.model.Timeslot;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.timetable.backend.domain.dto.DanceGroupDTO;
import com.timetable.backend.domain.model.DanceGroup;

@Mapper(componentModel = "spring")
public interface DictionaryMapper {
    RoomDTO toRoomDTO(Room room);
    Room toRoom(RoomDTO roomDTO);

    DanceStyleDTO toDanceStyleDTO(DanceStyle danceStyle);

    @Mapping(target = "teachers", ignore = true)
    DanceStyle toDanceStyle(DanceStyleDTO danceStyleDTO);

    TimeslotDTO toTimeslotDTO(Timeslot timeslot);
    Timeslot toTimeslot(TimeslotDTO timeslotDTO);

    @Mapping(source = "danceStyle.id", target = "danceStyleId")
    @Mapping(source = "danceStyle.name", target = "danceStyleName")
    DanceGroupDTO toDanceGroupDTO(DanceGroup danceGroup);

    @Mapping(target = "danceStyle", ignore = true) // Handled in service
    DanceGroup toDanceGroup(DanceGroupDTO danceGroupDTO);
}
