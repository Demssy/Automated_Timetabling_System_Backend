package com.timetable.backend.solver;

import com.timetable.backend.domain.model.*;
import com.timetable.backend.domain.repository.*;
import com.timetable.backend.service.SolverService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Tag("load")
class SolverLoadIntegrationTest {

    @Autowired
    private SolverService solverService;

    @Autowired
    private LessonRepository lessonRepository;
    @Autowired
    private TimeslotRepository timeslotRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private TeacherRepository teacherRepository;
    @Autowired
    private DanceGroupRepository danceGroupRepository;
    @Autowired
    private DanceStyleRepository danceStyleRepository;
    @Autowired
    private ResourceUnavailabilityRepository resourceUnavailabilityRepository;
    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void setUp() {
        lessonRepository.deleteAll();
        resourceUnavailabilityRepository.deleteAll();
        danceGroupRepository.deleteAll();
        teacherRepository.deleteAll();
        danceStyleRepository.deleteAll();
        timeslotRepository.deleteAll();
        roomRepository.deleteAll();
    }

    @Test
    @DisplayName("Load Test: 50 lessons, 10 teachers")
    void testSolverLoad() throws InterruptedException {
        // 1. Generate Data
        generateLoadData(10, 5, 50);

        // 2. Start Solver
        Long scheduleId = System.currentTimeMillis();
        solverService.solve(scheduleId);

        // 3. Wait for solution (max 75 seconds with 5-second intervals to reduce DB load)
        boolean solved = false;
        for (int i = 0; i < 15; i++) {
            Thread.sleep(5000);
            DanceSchedule solution = solverService.getCurrentSolutionFromDatabase(scheduleId);

            if (solution != null) {
                long assignedCount = solution.getLessonList().stream()
                        .filter(l -> l.getTimeslot() != null && l.getRoom() != null)
                        .count();

                if (assignedCount == 50) {
                    solved = true;
                    break;
                }
            }
        }

        // 4. Verify
        assertThat(solved).as("Solver should assign all 50 lessons within 75 seconds").isTrue();

        DanceSchedule finalSolution = solverService.getCurrentSolutionFromDatabase(scheduleId);
        // Note: Score might be null if read from DB before solver writes it back,
        // but since we checked assignments, at least one save happened.
        // However, the score is transient in the solution passed to saveSolution,
        // and might not be persisted in the DB unless we have a separate table for Schedule/Score.
        // The current implementation updates Lessons.
        // So we just check assignments.

        long assignedCount = finalSolution.getLessonList().stream()
                .filter(l -> l.getTimeslot() != null && l.getRoom() != null)
                .count();
        assertThat(assignedCount).isEqualTo(50);
    }

    private void generateLoadData(int teacherCount, int roomCount, int lessonCount) {
        // Create role for teachers
        Role teacherRole = roleRepository.findByName("TEACHER")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("TEACHER");
                    return roleRepository.save(role);
                });

        // Create Timeslots (Mon-Fri, 09:00-18:00 -> 9 slots * 5 days = 45 slots)
        List<Timeslot> timeslots = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) continue;
            for (int h = 9; h < 18; h++) {
                timeslots.add(createTimeslot(day, String.format("%02d:00", h), String.format("%02d:00", h + 1)));
            }
        }
        timeslotRepository.saveAll(timeslots);

        // Create Rooms
        List<Room> rooms = new ArrayList<>();
        for (int i = 0; i < roomCount; i++) {
            rooms.add(createRoom("Room " + i, 20, i % 2 == 0));
        }
        roomRepository.saveAll(rooms);

        // Create Teachers
        List<Teacher> teachers = new ArrayList<>();
        for (int i = 0; i < teacherCount; i++) {
            teachers.add(createTeacher("Teacher " + i, teacherRole));
        }
        teacherRepository.saveAll(teachers);

        // Create Dance Groups
        List<DanceGroup> groups = new ArrayList<>();
        for (int i = 0; i < lessonCount; i++) {
            groups.add(createDanceGroup("Group " + i));
        }
        danceGroupRepository.saveAll(groups);

        // Create Lessons
        List<Lesson> lessons = new ArrayList<>();
        for (int i = 0; i < lessonCount; i++) {
            Lesson lesson = new Lesson();
            lesson.setTeacher(teachers.get(i % teacherCount));
            lesson.setDanceGroup(groups.get(i));
            lesson.setDurationMinutes(60);
            lesson.setPrivate(i % 5 == 0); // 20% private
            lessons.add(lesson);
        }
        lessonRepository.saveAll(lessons);
    }

    private Timeslot createTimeslot(DayOfWeek day, String start, String end) {
        Timeslot t = new Timeslot();
        t.setDayOfWeek(day);
        t.setStartTime(LocalTime.parse(start));
        t.setEndTime(LocalTime.parse(end));
        return t;
    }

    private Room createRoom(String name, int capacity, boolean dualMode) {
        Room r = new Room();
        r.setName(name);
        r.setCapacity(capacity);
        r.setAllowsParallelPrivate(dualMode);
        return r;
    }

    private Teacher createTeacher(String name, Role role) {
        Teacher t = new Teacher();
        t.setFullName(name);
        t.setEmail(name.replace(" ", "").toLowerCase() + "@test.com");
        t.setPasswordHash("hashedPassword");
        t.setRole(role);
        t.setMaxDailyHours(5);
        t.setColorCode("#FFFFFF");
        return t;
    }

    private DanceGroup createDanceGroup(String name) {
        DanceGroup g = new DanceGroup();
        g.setName(name);
        return g;
    }
}

