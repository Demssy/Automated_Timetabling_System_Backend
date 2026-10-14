package com.timetable.backend.integration;

import com.timetable.backend.domain.dto.RegisterRequest;
import com.timetable.backend.domain.dto.ScheduledLessonDTO;
import com.timetable.backend.domain.dto.UserRole;
import com.timetable.backend.domain.model.AbstractUser;
import com.timetable.backend.domain.model.AvailabilityRole;
import com.timetable.backend.domain.model.DanceGroup;
import com.timetable.backend.domain.model.DanceLevel;
import com.timetable.backend.domain.model.DanceStyle;
import com.timetable.backend.domain.model.Lesson;
import com.timetable.backend.domain.model.ScheduleMetadata;
import com.timetable.backend.domain.model.ScheduleStatus;
import com.timetable.backend.domain.model.ScheduledLesson;
import com.timetable.backend.domain.model.ScheduledLessonStatus;
import com.timetable.backend.domain.model.Student;
import com.timetable.backend.domain.model.Teacher;
import com.timetable.backend.domain.model.Timeslot;
import com.timetable.backend.domain.repository.DanceGroupRepository;
import com.timetable.backend.domain.repository.DanceStyleRepository;
import com.timetable.backend.domain.repository.LessonRepository;
import com.timetable.backend.domain.repository.RoleRepository;
import com.timetable.backend.domain.repository.ScheduleMetadataRepository;
import com.timetable.backend.domain.repository.ScheduledLessonRepository;
import com.timetable.backend.domain.repository.StudentRepository;
import com.timetable.backend.domain.repository.TeacherRepository;
import com.timetable.backend.domain.repository.TimeslotRepository;
import com.timetable.backend.domain.repository.UserRepository;
import com.timetable.backend.service.AuthService;
import com.timetable.backend.service.LessonService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A dual-role user must get lessons he attends under STUDENT and lessons he teaches under TEACHER. */
@SpringBootTest
@ActiveProfiles("test")
@org.springframework.transaction.annotation.Transactional
class DualRoleScheduleIntegrationTest {

    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    @Autowired private AuthService authService;
    @Autowired private LessonService lessonService;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private DanceGroupRepository danceGroupRepository;
    @Autowired private DanceStyleRepository danceStyleRepository;
    @Autowired private LessonRepository lessonRepository;
    @Autowired private TimeslotRepository timeslotRepository;
    @Autowired private ScheduleMetadataRepository scheduleMetadataRepository;
    @Autowired private ScheduledLessonRepository scheduledLessonRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void dualRoleUser_getsStudentAndTeacherLessonsSeparately() {
        DanceStyle style = danceStyleRepository.save(new DanceStyle("ScheduleStyle"));
        authService.register(new RegisterRequest(
                "dual.schedule@example.com", "password123", "Dual Schedule", LocalDate.of(1990, 1, 1),
                UserRole.TEACHER, null, null, null, "+1", List.of(style.getId()), "bio",
                true, DanceLevel.BEGINNER, Set.of()));
        // The students row was inserted natively: drop the cached base entity so Student loads polymorphically
        entityManager.flush();
        entityManager.clear();
        AbstractUser dualUser = userRepository.findByEmail("dual.schedule@example.com").orElseThrow();
        Teacher dualTeacher = teacherRepository.findById(dualUser.getId()).orElseThrow();
        Student dualStudent = studentRepository.findById(dualUser.getId()).orElseThrow();

        // Another teacher who teaches the dual-role user
        var otherBase = new AbstractUser();
        otherBase.setEmail("other.teacher@example.com");
        otherBase.setPasswordHash("hashedPassword");
        otherBase.setFullName("Other Teacher");
        otherBase.setRole(roleRepository.findByName("TEACHER").orElseThrow());
        otherBase = userRepository.saveAndFlush(otherBase);
        Teacher otherTeacher = new Teacher();
        otherTeacher.setUser(otherBase);
        otherTeacher = teacherRepository.save(otherTeacher);

        Timeslot slotGroup = timeslotRepository.save(new Timeslot(DayOfWeek.MONDAY, LocalTime.of(18, 0), LocalTime.of(19, 0)));
        Timeslot slotPrivate = timeslotRepository.save(new Timeslot(DayOfWeek.TUESDAY, LocalTime.of(18, 0), LocalTime.of(19, 0)));
        Timeslot slotTeaching = timeslotRepository.save(new Timeslot(DayOfWeek.WEDNESDAY, LocalTime.of(18, 0), LocalTime.of(19, 0)));

        DanceGroup group = danceGroupRepository.save(new DanceGroup("Dual Group", style, DanceLevel.BEGINNER));
        jdbcTemplate.update("INSERT INTO student_groups (student_id, group_id) VALUES (?, ?)",
                dualUser.getId(), group.getId());

        Lesson groupLesson = lessonRepository.save(new Lesson(otherTeacher, group, 60, false, true));
        Lesson privateLesson = lessonRepository.save(new Lesson(otherTeacher, dualStudent, 60, true));
        Lesson teachingLesson = lessonRepository.save(new Lesson(dualTeacher, null, 60, true));

        ScheduleMetadata schedule = scheduleMetadataRepository.save(ScheduleMetadata.builder()
                .name("Current week")
                .validFrom(LocalDate.now().minusDays(1))
                .validTo(LocalDate.now().plusDays(5))
                .status(ScheduleStatus.PUBLISHED)
                .build());

        saveScheduled(schedule, groupLesson, slotGroup, null);
        saveScheduled(schedule, privateLesson, slotPrivate, dualStudent);
        saveScheduled(schedule, teachingLesson, slotTeaching, null);
        entityManager.flush();
        entityManager.clear();

        Authentication auth = new UsernamePasswordAuthenticationToken("dual.schedule@example.com", null,
                List.of(new SimpleGrantedAuthority("ROLE_TEACHER"), new SimpleGrantedAuthority("ROLE_STUDENT")));

        List<ScheduledLessonDTO> asStudent = lessonService.getMySchedule(auth, AvailabilityRole.STUDENT);
        List<ScheduledLessonDTO> asTeacher = lessonService.getMySchedule(auth, AvailabilityRole.TEACHER);

        assertEquals(2, asStudent.size(), "group + private lesson where the user is the student");
        assertEquals(1, asTeacher.size(), "only the lesson he teaches");

        // A stale token that predates the STUDENT role must not hide the student lessons
        Authentication staleAuth = new UsernamePasswordAuthenticationToken("dual.schedule@example.com", null,
                List.of(new SimpleGrantedAuthority("ROLE_TEACHER")));
        assertEquals(2, lessonService.getMySchedule(staleAuth, AvailabilityRole.STUDENT).size());
    }

    private void saveScheduled(ScheduleMetadata schedule, Lesson lesson, Timeslot slot, Student student) {
        ScheduledLesson sl = new ScheduledLesson();
        sl.setSchedule(schedule);
        sl.setLesson(lesson);
        sl.setTimeslot(slot);
        sl.setStudent(student);
        sl.setStatus(ScheduledLessonStatus.ASSIGNED);
        scheduledLessonRepository.save(sl);
    }
}
