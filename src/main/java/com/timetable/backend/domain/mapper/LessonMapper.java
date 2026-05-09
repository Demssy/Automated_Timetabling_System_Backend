package com.timetable.backend.domain.mapper;

import com.timetable.backend.domain.dto.ScheduledLessonDTO;
import com.timetable.backend.domain.model.Lesson;
import com.timetable.backend.domain.model.ScheduledLesson;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for converting lesson entities to DTOs.
 */
@Mapper(componentModel = "spring", uses = {TeacherMapper.class, DictionaryMapper.class, StudentMapper.class})
public interface LessonMapper {

    @Mapping(source = "id", target = "id")
    @Mapping(source = "teacher", target = "teacher")
    @Mapping(source = "danceGroup", target = "danceGroup")
    @Mapping(source = "student", target = "student")
    @Mapping(source = "private", target = "isPrivate")
    @Mapping(source = "pinned", target = "isPinned")
    @Mapping(source = "active", target = "isActive")
    @Mapping(source = "timeslot", target = "timeslot")
    @Mapping(source = "room", target = "room")
    @Mapping(target = "isCancelled", constant = "false")
    @Mapping(target = "cancelledById", ignore = true)
    @Mapping(target = "cancelledAt", ignore = true)
    @Mapping(target = "cancelReason", ignore = true)
    ScheduledLessonDTO toScheduledLessonDTO(Lesson lesson);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "lesson.teacher", target = "teacher")
    @Mapping(source = "lesson.danceGroup", target = "danceGroup")
    @Mapping(source = "student", target = "student")
    @Mapping(source = "lesson.durationMinutes", target = "durationMinutes")
    @Mapping(source = "lesson.private", target = "isPrivate")
    @Mapping(source = "lesson.pinned", target = "isPinned")
    @Mapping(source = "lesson.active", target = "isActive")
    @Mapping(source = "timeslot", target = "timeslot")
    @Mapping(source = "room", target = "room")
    @Mapping(source = "cancelled", target = "isCancelled")
    @Mapping(source = "cancelledBy.id", target = "cancelledById")
    @Mapping(source = "cancelledAt", target = "cancelledAt")
    @Mapping(source = "cancelReason", target = "cancelReason")
    ScheduledLessonDTO toScheduledLessonDTO(ScheduledLesson lesson);
}
