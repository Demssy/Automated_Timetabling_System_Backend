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
    ScheduledLessonDTO toScheduledLessonDTO(ScheduledLesson lesson);
}
