package com.timetable.backend.domain.mapper;

import com.timetable.backend.domain.dto.ScheduledLessonDTO;
import com.timetable.backend.domain.model.Lesson;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for converting Lesson entities to DTOs.
 * Handles null-safe mapping of nested properties.
 */
@Mapper(componentModel = "spring")
public interface LessonMapper {

    /**
     * Maps a Lesson entity to ScheduledLessonDTO.
     * Handles nested properties and provides default values for unassigned fields.
     *
     * @param lesson the lesson entity
     * @return scheduled lesson DTO with all assignments
     */
    @Mapping(source = "id", target = "lessonId")
    @Mapping(source = "teacher.user.fullName", target = "teacherName", defaultValue = "N/A")
    @Mapping(source = "danceGroup.name", target = "groupName", defaultValue = "N/A")
    @Mapping(source = "timeslot.dayOfWeek", target = "dayOfWeek")
    @Mapping(source = "timeslot.startTime", target = "startTime")
    @Mapping(source = "timeslot.endTime", target = "endTime")
    @Mapping(source = "room.name", target = "roomName", defaultValue = "Unassigned")
    @Mapping(source = "durationMinutes", target = "durationMinutes")
    @Mapping(source = "private", target = "isPrivate")
    @Mapping(source = "pinned", target = "isPinned")
    ScheduledLessonDTO toScheduledLessonDTO(Lesson lesson);
}

