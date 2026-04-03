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
    private final StudentRepository studentRepository;
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

        return lessonRepository.findByIsActiveTrue().stream()
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
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found with id: " + request.teacherId()));

        // Rule: pinned lessons must always have an explicit timeslot
        if (request.isPinned() && request.timeslotId() == null) {
            throw new IllegalArgumentException("A pinned lesson must have an explicitly provided timeslotId");
        }

        Lesson lesson = new Lesson();
        lesson.setTeacher(teacher);
        lesson.setDurationMinutes(request.durationMinutes());
        lesson.setPrivate(request.isPrivate());
        lesson.setPinned(request.isPinned());
        lesson.setActive(request.isActive());

        // Rule: private vs group branching
        applyLessonType(lesson, request);

        // Rule: timeslot assignment
        if (request.timeslotId() != null) {
            Timeslot timeslot = timeslotRepository.findById(request.timeslotId())
                    .orElseThrow(() -> new IllegalArgumentException("Timeslot not found with id: " + request.timeslotId()));
            lesson.setTimeslot(timeslot);
        }

        // Rule: auto-assign first available room if none provided
        Room room = resolveRoom(request.roomId());
        lesson.setRoom(room);

        return lessonMapper.toScheduledLessonDTO(lessonRepository.save(lesson));
    }

    @Transactional
    public ScheduledLessonDTO updateLesson(Long id, CreateLessonRequest request) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found with id: " + id));

        // Rule: pinned lessons must always have an explicit timeslot
        if (request.isPinned() && request.timeslotId() == null) {
            throw new IllegalArgumentException("A pinned lesson must have an explicitly provided timeslotId");
        }

        Teacher teacher = teacherRepository.findById(request.teacherId())
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found with id: " + request.teacherId()));

        lesson.setTeacher(teacher);
        lesson.setDurationMinutes(request.durationMinutes());
        lesson.setPrivate(request.isPrivate());
        lesson.setPinned(request.isPinned());
        lesson.setActive(request.isActive());

        // Rule: private vs group branching
        applyLessonType(lesson, request);

        // Rule: timeslot assignment
        if (request.timeslotId() != null) {
            Timeslot timeslot = timeslotRepository.findById(request.timeslotId())
                    .orElseThrow(() -> new IllegalArgumentException("Timeslot not found with id: " + request.timeslotId()));
            lesson.setTimeslot(timeslot);
        } else {
            lesson.setTimeslot(null);
        }

        // Rule: auto-assign first available room if none provided
        Room room = resolveRoom(request.roomId());
        lesson.setRoom(room);

        return lessonMapper.toScheduledLessonDTO(lessonRepository.save(lesson));
    }

    @Transactional
    public void deleteLesson(Long id) {
        if (!lessonRepository.existsById(id)) {
            throw new IllegalArgumentException("Lesson not found with id: " + id);
        }
        lessonRepository.deleteById(id);
    }

    @Transactional
    public ScheduledLessonDTO toggleLessonActive(Long id) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found with id: " + id));

        lesson.setActive(!lesson.isActive());
        Lesson saved = lessonRepository.save(lesson);
        return lessonMapper.toScheduledLessonDTO(saved);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Applies Private/Group lesson type branching to the given lesson.
     * <ul>
     *   <li>Private lesson: {@code student} is required, {@code danceGroup} is set to null.</li>
     *   <li>Group lesson: {@code danceGroup} is required, {@code student} is set to null.</li>
     * </ul>
     */
    private void applyLessonType(Lesson lesson, CreateLessonRequest request) {
        if (request.isPrivate()) {
            if (request.studentId() == null) {
                throw new IllegalArgumentException("A private lesson must have a studentId");
            }
            Student student = studentRepository.findById(request.studentId())
                    .orElseThrow(() -> new IllegalArgumentException("Student not found with id: " + request.studentId()));
            lesson.setStudent(student);
            lesson.setDanceGroup(null);
        } else {
            if (request.danceGroupId() == null) {
                throw new IllegalArgumentException("A group lesson must have a danceGroupId");
            }
            DanceGroup group = danceGroupRepository.findById(request.danceGroupId())
                    .orElseThrow(() -> new IllegalArgumentException("Dance Group not found with id: " + request.danceGroupId()));
            lesson.setDanceGroup(group);
            lesson.setStudent(null);
        }
    }

    /**
     * Resolves the room for a lesson.
     * If {@code roomId} is provided, fetches and returns that room.
     * Otherwise, auto-assigns the first available room from the database.
     *
     * @throws IllegalStateException if no rooms exist in the database
     */
    private Room resolveRoom(Long roomId) {
        if (roomId != null) {
            return roomRepository.findById(roomId)
                    .orElseThrow(() -> new IllegalArgumentException("Room not found with id: " + roomId));
        }
        return roomRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No rooms available in the system. Please create a room first."));
    }
}

