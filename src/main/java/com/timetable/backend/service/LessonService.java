package com.timetable.backend.service;

import com.timetable.backend.domain.dto.CreateLessonRequest;
import com.timetable.backend.domain.dto.ScheduledLessonDTO;
import com.timetable.backend.domain.dto.UpdateLessonRequest;
import com.timetable.backend.domain.mapper.LessonMapper;
import com.timetable.backend.domain.model.*;
import com.timetable.backend.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

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
    private final AddedLessonRepository addedLessonRepository;
    private final LessonMapper lessonMapper;
    private final ScheduledLessonRepository scheduledLessonRepository;
    private final UserRepository userRepository;
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
        if (request.scheduleId() != null) {
            return createOneTimeLesson(request);
        }

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
        ScheduledLesson scheduledLesson = scheduledLessonRepository.findById(id).orElse(null);
        if (scheduledLesson != null && scheduledLesson.getAddedLesson() != null) {
            AddedLesson addedLesson = scheduledLesson.getAddedLesson();
            scheduledLessonRepository.delete(scheduledLesson);
            addedLessonRepository.deleteById(addedLesson.getId());
            return;
        }

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

    /**
     * Updates a lesson in response to a manual drag-and-drop reschedule from the frontend.
     *
     * <p>Business rules:
     * <ul>
     *   <li>If the persisted lesson is currently <b>pinned</b> and the request
     *       attempts to change {@code timeslotId} or {@code roomId}, the operation
     *       is rejected with {@link HttpClientErrorException} (HTTP 409 Conflict).</li>
     *   <li>If {@code timeslotId} is null the timeslot is cleared (unassigned).</li>
     *   <li>If {@code roomId} is null the room is cleared (unassigned) — no
     *       auto-assignment in this method, unlike {@link #createLesson}.</li>
     * </ul>
     *
     * @param id      lesson identifier
     * @param request updated lesson data from the frontend
     * @return fully populated {@link ScheduledLessonDTO}
     * @throws IllegalArgumentException if the lesson or any referenced entity is not found
     * @throws
     *  HttpClientErrorException if the lesson is pinned and timeslot/room would change
     */
    @Transactional
    public ScheduledLessonDTO updateLessonManually(Long id, UpdateLessonRequest request) {
        ScheduledLesson scheduledLesson = scheduledLessonRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found with id: " + id));
        Lesson lesson = scheduledLesson.getLesson();
        AddedLesson addedLesson = scheduledLesson.getAddedLesson();
        if (lesson == null && addedLesson == null) {
            throw new IllegalArgumentException("Scheduled lesson has no source entity: " + id);
        }

        // ── Pinned-lesson protection ──────────────────────────────────────────
        if (scheduledLesson.isSourcePinned()) {
            Long currentTimeslotId = scheduledLesson.getTimeslot() != null ? scheduledLesson.getTimeslot().getId() : null;
            Long currentRoomId = scheduledLesson.getRoom() != null ? scheduledLesson.getRoom().getId() : null;

            boolean timeslotChanged = !Objects.equals(currentTimeslotId, request.timeslotId());
            boolean roomChanged = !Objects.equals(currentRoomId, request.roomId());

            if (timeslotChanged || roomChanged) {
                throw new HttpClientErrorException(HttpStatus.BAD_REQUEST,"Cannot reschedule a pinned lesson. Unpin it first.");
            }
        }

        // ── Timeslot assignment (nullable — explicit unassign on null) ────────
        if (request.timeslotId() != null) {
            Timeslot timeslot = timeslotRepository.findById(request.timeslotId())
                    .orElseThrow(() -> new IllegalArgumentException("Timeslot not found with id: " + request.timeslotId()));
            scheduledLesson.setTimeslot(timeslot);
        } else {
            scheduledLesson.setTimeslot(null);
        }

        if (request.roomId() != null) {
            Room room = roomRepository.findById(request.roomId())
                    .orElseThrow(() -> new IllegalArgumentException("Room not found with id: " + request.roomId()));
            scheduledLesson.setRoom(room);
        } else {
            scheduledLesson.setRoom(null);
        }

        if (addedLesson != null) {
            addedLesson.setTimeslot(scheduledLesson.getTimeslot());
            addedLesson.setRoom(scheduledLesson.getRoom());
            addedLessonRepository.save(addedLesson);
        }

        return lessonMapper.toScheduledLessonDTO(scheduledLessonRepository.save(scheduledLesson));
    }

    // -------------------------------------------------------------------------
    // Personal schedule (my-schedule)
    // -------------------------------------------------------------------------

    /**
     * Returns scheduled lessons from the currently active PUBLISHED schedule
     * that belong to the authenticated user.
     *
     * <p>Data is sourced exclusively from {@code scheduled_lessons} — the solver snapshot table.
     * The {@code lessons} (template) table is never touched here.</p>
     *
     * <ul>
     *   <li>STUDENT — private lessons where the solver assigned them + group lessons
     *       where they are enrolled in the dance group.</li>
     *   <li>TEACHER — all lessons (private and group) that they teach.</li>
     * </ul>
     *
     * @param authentication Spring Security authentication of the current user
     * @return sorted list of {@link ScheduledLessonDTO}; empty list if no active schedule exists
     */
    @Transactional(readOnly = true)
    public List<ScheduledLessonDTO> getMySchedule(Authentication authentication) {
        String email = authentication.getName();

        AbstractUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found: " + email));

        Long activeScheduleId = resolveActiveScheduleId();
        if (activeScheduleId == null) {
            return List.of();
        }

        boolean isTeacher = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"));

        if (isTeacher) {
            Teacher teacher = teacherRepository.findById(user.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Teacher profile not found for: " + email));

            return scheduledLessonRepository
                    .findByScheduleIdAndTeacherId(activeScheduleId, teacher.getId())
                    .stream()
                    .filter(sl -> sl.getTimeslot() != null)
                    .sorted(Comparator
                            .comparing((ScheduledLesson sl) -> sl.getTimeslot().getDayOfWeek())
                            .thenComparing(sl -> sl.getTimeslot().getStartTime()))
                    .map(lessonMapper::toScheduledLessonDTO)
                    .toList();
        }

        // STUDENT role
        Student student = studentRepository.findById(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Student profile not found for: " + email));

        return scheduledLessonRepository
                .findByScheduleIdAndStudentId(activeScheduleId, student.getId())
                .stream()
                .filter(sl -> sl.getTimeslot() != null)
                .sorted(Comparator
                        .comparing((ScheduledLesson sl) -> sl.getTimeslot().getDayOfWeek())
                        .thenComparing(sl -> sl.getTimeslot().getStartTime()))
                .map(lessonMapper::toScheduledLessonDTO)
                .toList();
    }

    /**
     * Finds the ID of the currently active PUBLISHED schedule (valid today).
     * Returns null if no such schedule exists.
     */
    private Long resolveActiveScheduleId() {
        LocalDate today = LocalDate.now();
        return scheduleMetadataRepository.findAll().stream()
                .filter(s -> s.getStatus() == ScheduleStatus.PUBLISHED)
                .filter(s -> !today.isBefore(s.getValidFrom()) && !today.isAfter(s.getValidTo()))
                .max(Comparator.comparing(ScheduleMetadata::getCreatedAt))
                .map(ScheduleMetadata::getId)
                .orElse(null);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Applies Private/Group lesson type branching to the given lesson.
     * <ul>
     *   <li>Private lesson: {@code student} is optional (null = solver template), {@code danceGroup} is set to null.</li>
     *   <li>Group lesson: {@code danceGroup} is required, {@code student} is set to null.</li>
     * </ul>
     */
    private void applyLessonType(Lesson lesson, CreateLessonRequest request) {
        if (request.isPrivate()) {
            // studentId is optional: null means a "template" lesson for the solver to fill.
            if (request.studentId() != null) {
                Student student = studentRepository.findById(request.studentId())
                        .orElseThrow(() -> new IllegalArgumentException("Student not found with id: " + request.studentId()));
                lesson.setStudent(student);
            } else {
                lesson.setStudent(null);
            }
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

    private ScheduledLessonDTO createOneTimeLesson(CreateLessonRequest request) {
        ScheduleMetadata schedule = scheduleMetadataRepository.findById(request.scheduleId())
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found with id: " + request.scheduleId()));
        Teacher teacher = teacherRepository.findById(request.teacherId())
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found with id: " + request.teacherId()));

        if (request.isPinned() && request.timeslotId() == null) {
            throw new IllegalArgumentException("A pinned lesson must have an explicitly provided timeslotId");
        }

        AddedLesson addedLesson = new AddedLesson();
        addedLesson.setSchedule(schedule);
        addedLesson.setTeacher(teacher);
        addedLesson.setDurationMinutes(request.durationMinutes());
        addedLesson.setPrivate(request.isPrivate());
        addedLesson.setPinned(request.isPinned());
        addedLesson.setActive(request.isActive());
        applyAddedLessonType(addedLesson, request);

        if (request.timeslotId() != null) {
            Timeslot timeslot = timeslotRepository.findById(request.timeslotId())
                    .orElseThrow(() -> new IllegalArgumentException("Timeslot not found with id: " + request.timeslotId()));
            addedLesson.setTimeslot(timeslot);
        }

        Room room = resolveRoom(request.roomId());
        addedLesson.setRoom(room);
        AddedLesson savedAddedLesson = addedLessonRepository.save(addedLesson);

        ScheduledLesson snapshot = new ScheduledLesson();
        snapshot.setLesson(null);
        snapshot.setAddedLesson(savedAddedLesson);
        snapshot.setSchedule(schedule);
        snapshot.setTimeslot(savedAddedLesson.getTimeslot());
        snapshot.setRoom(savedAddedLesson.getRoom());
        snapshot.setStudent(savedAddedLesson.getStudent());
        snapshot.setStatus(savedAddedLesson.getTimeslot() != null
                ? ScheduledLessonStatus.ASSIGNED
                : ScheduledLessonStatus.UNASSIGNED);
        snapshot.setCancelled(false);
        snapshot.setCancelledBy(null);
        snapshot.setCancelledAt(null);
        snapshot.setCancelReason(null);

        return lessonMapper.toScheduledLessonDTO(scheduledLessonRepository.save(snapshot));
    }

    /**
     * Applies Private/Group lesson type branching for {@link UpdateLessonRequest}.
     * Logic mirrors {@link #applyLessonType(Lesson, CreateLessonRequest)}.
     */
    private void applyLessonTypeForUpdate(Lesson lesson, UpdateLessonRequest request) {
        if (request.isPrivate()) {
            if (request.studentId() != null) {
                Student student = studentRepository.findById(request.studentId())
                        .orElseThrow(() -> new IllegalArgumentException("Student not found with id: " + request.studentId()));
                lesson.setStudent(student);
            } else {
                lesson.setStudent(null);
            }
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

    private void applyAddedLessonType(AddedLesson lesson, CreateLessonRequest request) {
        if (request.isPrivate()) {
            if (request.studentId() != null) {
                Student student = studentRepository.findById(request.studentId())
                        .orElseThrow(() -> new IllegalArgumentException("Student not found with id: " + request.studentId()));
                lesson.setStudent(student);
            } else {
                lesson.setStudent(null);
            }
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
}
