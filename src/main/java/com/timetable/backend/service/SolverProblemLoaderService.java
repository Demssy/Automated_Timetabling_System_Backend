package com.timetable.backend.service;

import com.timetable.backend.domain.model.Lesson;
import com.timetable.backend.domain.model.ResourceUnavailability;
import com.timetable.backend.domain.model.Room;
import com.timetable.backend.domain.model.ScheduleMetadata;
import com.timetable.backend.domain.model.Teacher;
import com.timetable.backend.domain.model.Timeslot;
import com.timetable.backend.domain.model.WeeklyAvailability;
import com.timetable.backend.domain.repository.LessonRepository;
import com.timetable.backend.domain.repository.ResourceUnavailabilityRepository;
import com.timetable.backend.domain.repository.RoomRepository;
import com.timetable.backend.domain.repository.ScheduleMetadataRepository;
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
    private final RoomRepository roomRepository;
    private final TeacherRepository teacherRepository;
    private final ResourceUnavailabilityRepository resourceUnavailabilityRepository;
    private final WeeklyAvailabilityRepository weeklyAvailabilityRepository;
    private final ScheduleMetadataRepository scheduleMetadataRepository;

    @Transactional(readOnly = true)
    public DanceSchedule loadProblem(Long scheduleId) {
        DanceSchedule schedule = loadScheduleFromDatabase(scheduleId);

        // Clear planning variables for non-pinned lessons.
        schedule.getLessonList().forEach(lesson -> {
            if (!lesson.isPinned()) {
                lesson.setTimeslot(null);
                lesson.setRoom(null);
            }
        });

        return schedule;
    }

    private DanceSchedule loadScheduleFromDatabase(Long scheduleId) {
        log.info("Loading problem data from database for schedule ID: {}", scheduleId);

        List<Timeslot> timeslots = timeslotRepository.findAll();
        List<Room> rooms = roomRepository.findAll();
        List<Teacher> teachers = teacherRepository.findAll();
        List<ResourceUnavailability> resourceUnavailabilities = resourceUnavailabilityRepository.findAll();
        List<WeeklyAvailability> weeklyAvailabilities = weeklyAvailabilityRepository.findAll();
        List<Lesson> lessons = lessonRepository.findByIsActiveTrueAndScheduleId(scheduleId);
        LocalDate scheduleStartDate = scheduleMetadataRepository.findById(scheduleId)
                .map(ScheduleMetadata::getValidFrom)
                .orElse(null);

        if (scheduleStartDate == null) {
            log.warn("Schedule {} has no validFrom date — teacherOneTimeUnavailability constraint " +
                     "will be DISABLED for this run. Set ScheduleMetadata.validFrom to enable it.", scheduleId);
        }

        log.info("Loaded {} timeslots, {} rooms, {} teachers, {} lessons, {} weekly schedules",
                timeslots.size(), rooms.size(), teachers.size(), lessons.size(), weeklyAvailabilities.size());

        return new DanceSchedule(
                scheduleId,
                timeslots,
                rooms,
                teachers,
                resourceUnavailabilities,
                weeklyAvailabilities,
                scheduleStartDate == null ? List.of() : List.of(scheduleStartDate),
                lessons
        );
    }
}


