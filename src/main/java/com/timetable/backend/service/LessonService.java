package com.timetable.backend.service;

import com.timetable.backend.domain.dto.CreateLessonRequest;
import com.timetable.backend.domain.dto.ScheduledLessonDTO;
import com.timetable.backend.domain.mapper.LessonMapper;
import com.timetable.backend.domain.model.*;
import com.timetable.backend.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository lessonRepository;
    private final TeacherRepository teacherRepository;
    private final DanceGroupRepository danceGroupRepository;
    private final TimeslotRepository timeslotRepository;
    private final RoomRepository roomRepository;
    private final ScheduleMetadataRepository scheduleMetadataRepository;
    private final LessonMapper lessonMapper;

    @Transactional(readOnly = true)
    public List<ScheduledLessonDTO> getAllLessons() {
        return lessonRepository.findAll().stream()
                .map(lessonMapper::toScheduledLessonDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ScheduledLessonDTO> getActiveScheduleLessons() {
        LocalDate today = LocalDate.now();

        boolean hasActivePublishedSchedule = scheduleMetadataRepository.findAll().stream()
                .filter(schedule -> schedule.getStatus() == ScheduleStatus.PUBLISHED)
                .filter(schedule -> !today.isBefore(schedule.getValidFrom()) && !today.isAfter(schedule.getValidTo()))
                .max(Comparator.comparing(ScheduleMetadata::getCreatedAt))
                .isPresent();

        if (!hasActivePublishedSchedule) {
            return List.of();
        }

        return lessonRepository.findAll().stream()
                .filter(lesson -> lesson.getTimeslot() != null)
                .filter(lesson -> lesson.getRoom() != null)
                .sorted(Comparator
                        .comparing((Lesson lesson) -> lesson.getTimeslot().getDayOfWeek())
                        .thenComparing(lesson -> lesson.getTimeslot().getStartTime())
                        .thenComparing(Lesson::getId))
                .map(lessonMapper::toScheduledLessonDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public ScheduledLessonDTO getLessonById(Long id) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found with id: " + id));
        return lessonMapper.toScheduledLessonDTO(lesson);
    }



    @Transactional
    public ScheduledLessonDTO createLesson(CreateLessonRequest request) {
        Teacher teacher = teacherRepository.findById(request.teacherId())
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));
        DanceGroup group = danceGroupRepository.findById(request.danceGroupId())
                .orElseThrow(() -> new IllegalArgumentException("Dance Group not found"));

        Lesson lesson = new Lesson();
        lesson.setTeacher(teacher);
        lesson.setDanceGroup(group);
        lesson.setDurationMinutes(request.durationMinutes());
        lesson.setPrivate(request.isPrivate());
        lesson.setPinned(request.isPinned());

        if (request.timeslotId() != null) {
            Timeslot timeslot = timeslotRepository.findById(request.timeslotId())
                    .orElseThrow(() -> new IllegalArgumentException("Timeslot not found"));
            lesson.setTimeslot(timeslot);
        }

        if (request.roomId() != null) {
            Room room = roomRepository.findById(request.roomId())
                    .orElseThrow(() -> new IllegalArgumentException("Room not found"));
            lesson.setRoom(room);
        }

        Lesson saved = lessonRepository.save(lesson);
        return lessonMapper.toScheduledLessonDTO(saved);
    }

    @Transactional
    public ScheduledLessonDTO updateLesson(Long id, CreateLessonRequest request) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found with id: " + id));

        // Use setter methods for updates
        if (!lesson.getTeacher().getId().equals(request.teacherId())) {
             Teacher teacher = teacherRepository.findById(request.teacherId())
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));
             lesson.setTeacher(teacher);
        }

        if (!lesson.getDanceGroup().getId().equals(request.danceGroupId())) {
             DanceGroup group = danceGroupRepository.findById(request.danceGroupId())
                .orElseThrow(() -> new IllegalArgumentException("Dance Group not found"));
             lesson.setDanceGroup(group);
        }

        lesson.setDurationMinutes(request.durationMinutes());
        lesson.setPrivate(request.isPrivate());
        lesson.setPinned(request.isPinned());

        if (request.timeslotId() != null) {
            Timeslot timeslot = timeslotRepository.findById(request.timeslotId())
                    .orElseThrow(() -> new IllegalArgumentException("Timeslot not found"));
            lesson.setTimeslot(timeslot);
        } else {
            lesson.setTimeslot(null);
        }

        if (request.roomId() != null) {
            Room room = roomRepository.findById(request.roomId())
                    .orElseThrow(() -> new IllegalArgumentException("Room not found"));
            lesson.setRoom(room);
        } else {
            lesson.setRoom(null);
        }

        Lesson saved = lessonRepository.save(lesson);
        return lessonMapper.toScheduledLessonDTO(saved);
    }

    @Transactional
    public void deleteLesson(Long id) {
        if (!lessonRepository.existsById(id)) {
            throw new IllegalArgumentException("Lesson not found with id: " + id);
        }
        lessonRepository.deleteById(id);
    }
}

