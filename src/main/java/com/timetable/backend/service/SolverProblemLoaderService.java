package com.timetable.backend.service;

import com.timetable.backend.domain.model.Lesson;
import com.timetable.backend.domain.model.ResourceUnavailability;
import com.timetable.backend.domain.model.ScheduleMetadata;
import com.timetable.backend.domain.model.Student;
import com.timetable.backend.domain.model.Teacher;
import com.timetable.backend.domain.model.Timeslot;
import com.timetable.backend.domain.model.WeeklyAvailability;
import com.timetable.backend.domain.repository.LessonRepository;
import com.timetable.backend.domain.repository.ResourceUnavailabilityRepository;
import com.timetable.backend.domain.repository.ScheduleMetadataRepository;
import com.timetable.backend.domain.repository.StudentRepository;
import com.timetable.backend.domain.repository.TeacherRepository;
import com.timetable.backend.domain.repository.TimeslotRepository;
import com.timetable.backend.domain.repository.WeeklyAvailabilityRepository;
import com.timetable.backend.solver.DanceSchedule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Transactional loader for solver input data.
 * Extracted from SolverService to avoid self-invocation proxy patterns.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SolverProblemLoaderService {

    private final LessonRepository lessonRepository;
    private final TimeslotRepository timeslotRepository;
    private final TeacherRepository teacherRepository;
    private final ResourceUnavailabilityRepository resourceUnavailabilityRepository;
    private final WeeklyAvailabilityRepository weeklyAvailabilityRepository;
    private final ScheduleMetadataRepository scheduleMetadataRepository;
    private final StudentRepository studentRepository;

    @Transactional(readOnly = true)
    public DanceSchedule loadProblem(Long scheduleId) {
        DanceSchedule schedule = loadScheduleFromDatabase(scheduleId);

        // Group lessons are always pinned — the solver only respects their time/teacher.
        // Only private (non-pinned) lessons get their planning variables cleared.
        schedule.getLessonList().forEach(lesson -> {
            if (!lesson.isPrivate()) {
                // Ensure group lessons are pinned so the solver never moves them.
                if (!lesson.isPinned()) {
                    log.warn("Group lesson {} was not pinned — forcing pinned=true for solver run.", lesson.getId());
                    lesson.setPinned(true);
                }
            } else if (!lesson.isPinned()) {
                // Clear planning variables for non-pinned private lessons so solver starts fresh.
                lesson.setTimeslot(null);
                lesson.setStudent(null);
            }
        });

        return schedule;
    }

    private DanceSchedule loadScheduleFromDatabase(Long scheduleId) {
        log.info("Loading problem data from database for schedule ID: {}", scheduleId);

        List<Timeslot> timeslots = timeslotRepository.findAll();
        List<Teacher> teachers = teacherRepository.findAll();
        List<ResourceUnavailability> resourceUnavailabilities = resourceUnavailabilityRepository.findAll();
        List<WeeklyAvailability> weeklyAvailabilities = weeklyAvailabilityRepository.findAll();
        List<Lesson> lessons = lessonRepository.findByIsActiveTrueAndScheduleId(scheduleId);
        List<Student> students = studentRepository.findAll();
        LocalDate scheduleStartDate = scheduleMetadataRepository.findById(scheduleId)
                .map(ScheduleMetadata::getValidFrom)
                .orElse(null);

        if (scheduleStartDate == null) {
            log.warn("Schedule {} has no validFrom date — teacherOneTimeUnavailability constraint " +
                     "will be DISABLED for this run. Set ScheduleMetadata.validFrom to enable it.", scheduleId);
        }

        // Initialize lazy collections inside this @Transactional boundary to prevent
        // LazyInitializationException when the Solver accesses them outside a session.
        teachers.forEach(teacher -> {
            if (teacher.getPrivateStudents() != null) {
                teacher.getPrivateStudents().size(); // forces Hibernate to fetch the Set
            }
        });

        log.info("Loaded {} timeslots, {} teachers, {} lessons, {} students, {} weekly schedules",
                timeslots.size(), teachers.size(), lessons.size(), students.size(), weeklyAvailabilities.size());

        return new DanceSchedule(
                scheduleId,
                timeslots,
                teachers,
                resourceUnavailabilities,
                weeklyAvailabilities,
                scheduleStartDate == null ? List.of() : List.of(scheduleStartDate),
                students,
                lessons
        );
    }
}
