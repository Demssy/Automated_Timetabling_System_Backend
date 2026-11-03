package com.timetable.backend.integration;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import com.timetable.backend.domain.dto.CreateLessonRequest;
import com.timetable.backend.domain.dto.UpdateLessonRequest;
import com.timetable.backend.domain.model.*;
import com.timetable.backend.domain.repository.*;
import com.timetable.backend.service.LessonService;
import com.timetable.backend.service.ScheduleMetadataService;
import com.timetable.backend.service.ScheduleScoreEvaluationService;
import com.timetable.backend.service.ScheduledLessonCancellationService;
import com.timetable.backend.service.SolverRunService;
import com.timetable.backend.service.SolverService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ManualEditAndPublishGuardIntegrationTest {
    @Autowired private RoleRepository roles;
    @Autowired private UserRepository users;
    @Autowired private TeacherRepository teachers;
    @Autowired private WeeklyAvailabilityRepository availability;
    @Autowired private TimeslotRepository timeslots;
    @Autowired private RoomRepository rooms;
    @Autowired private DanceGroupRepository groups;
    @Autowired private LessonRepository lessons;
    @Autowired private ScheduledLessonRepository snapshots;
    @Autowired private ScheduleMetadataRepository schedules;
    @Autowired private LessonService lessonService;
    @Autowired private ScheduleScoreEvaluationService evaluator;
    @Autowired private ScheduleMetadataService metadataService;
    @Autowired private ScheduledLessonCancellationService cancellationService;
    @Autowired private SolverService solverService;
    @Autowired private SolverRunService solverRuns;

    private ScheduleMetadata schedule;
    private Teacher secondTeacher;
    private DanceGroup secondGroup;
    private Timeslot overlappingSlot;
    private Room room;
    private Authentication admin;
    private Long firstSnapshotId;
    private Teacher firstTeacher;

    @BeforeEach
    void setUp() {
        Role teacherRole = roles.findByName("TEACHER").orElseGet(() -> roles.save(new Role(null, "TEACHER")));
        Role adminRole = roles.findByName("ADMIN").orElseGet(() -> roles.save(new Role(null, "ADMIN")));
        firstTeacher = teacher("first@example.test", teacherRole);
        secondTeacher = teacher("second@example.test", teacherRole);
        AbstractUser administrator = user("admin@example.test", adminRole);
        admin = new UsernamePasswordAuthenticationToken(administrator.getEmail(), null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        Timeslot firstSlot = timeslots.save(new Timeslot(DayOfWeek.MONDAY,
                LocalTime.of(17, 0), LocalTime.of(18, 0)));
        overlappingSlot = timeslots.save(new Timeslot(DayOfWeek.MONDAY,
                LocalTime.of(17, 30), LocalTime.of(18, 30)));
        room = rooms.save(new Room("Main Hall", 4, true));
        DanceGroup firstGroup = groups.save(new DanceGroup("First", null, DanceLevel.BEGINNER));
        secondGroup = groups.save(new DanceGroup("Second", null, DanceLevel.BEGINNER));
        schedule = schedules.save(ScheduleMetadata.builder().name("Draft")
                .validFrom(LocalDate.of(2026, 11, 2)).validTo(LocalDate.of(2026, 11, 8))
                .status(ScheduleStatus.DRAFT).build());
        Lesson source = new Lesson(firstTeacher, firstGroup, 60, false, true);
        source.setPinned(true);
        source = lessons.save(source);
        ScheduledLesson row = new ScheduledLesson();
        row.setSchedule(schedule);
        row.setLesson(source);
        row.setTimeslot(firstSlot);
        row.setRoom(room);
        row.setStatus(ScheduledLessonStatus.ASSIGNED);
        firstSnapshotId = snapshots.save(row).getId();
        evaluator.recalculate(schedule.getId());
        assertThat(HardSoftScore.parseScore(schedule.getSolverScore()).hardScore()).isZero();
    }

    @Test
    void conflictingManualLessonUpdatesScoreAndBlocksPublication() {
        addConflictingLesson();
        assertThat(HardSoftScore.parseScore(schedule.getSolverScore()).hardScore()).isNegative();
        assertThat(schedule.getScoreExplanation()).contains("Room conflict");
        assertThatThrownBy(() -> metadataService.publish(schedule.getId()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("hard constraint violations");
    }

    @Test
    void cancellingConflictingManualLessonAllowsPublication() {
        Long snapshotId = addConflictingLesson();
        cancellationService.cancelLesson(snapshotId, null, admin);
        assertThat(HardSoftScore.parseScore(schedule.getSolverScore()).hardScore()).isZero();
        metadataService.publish(schedule.getId());
        assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.PUBLISHED);
        assertThatThrownBy(() -> cancellationService.restoreLesson(snapshotId, admin))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("hard constraint violations");
    }

    @Test
    void movingManualLessonInDraftUpdatesScore() {
        Timeslot later = timeslots.save(new Timeslot(DayOfWeek.MONDAY,
                LocalTime.of(19, 0), LocalTime.of(20, 0)));
        Long id = lessonService.createLesson(new CreateLessonRequest(schedule.getId(), secondTeacher.getId(),
                secondGroup.getId(), null, 60, false, false, true, later.getId(), room.getId())).id();
        assertThat(HardSoftScore.parseScore(schedule.getSolverScore()).hardScore()).isZero();
        lessonService.updateLessonManually(id, new UpdateLessonRequest(secondTeacher.getId(), secondGroup.getId(),
                null, 60, false, false, true, overlappingSlot.getId(), room.getId()));
        assertThat(HardSoftScore.parseScore(schedule.getSolverScore()).hardScore()).isNegative();
    }

    @Test
    void cancellingLastPublishedLessonIsAllowed() {
        metadataService.publish(schedule.getId());
        cancellationService.cancelLesson(firstSnapshotId, null, admin);
        assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.PUBLISHED);
        assertThat(HardSoftScore.parseScore(schedule.getSolverScore()).hardScore()).isZero();
    }

    @Test
    void archivedScheduleRejectsManualEditAndSolver() {
        metadataService.archive(schedule.getId());
        assertThatThrownBy(this::addConflictingLesson).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("archived");
        assertThatThrownBy(() -> solverRuns.begin(schedule.getId())).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("draft");
    }

    @Test
    void explanationReflectsChangedAvailabilityWithoutOverwritingStoredScore() {
        String oldScore = schedule.getSolverScore();
        WeeklyAvailability window = availability.findByUserIdAndAvailabilityRole(
                firstTeacher.getId(), AvailabilityRole.TEACHER).get(0);
        window.setStartTime(LocalTime.of(17, 30));
        availability.save(window);
        assertThat(HardSoftScore.parseScore(solverService.getScoreExplanation(schedule.getId()).totalScore())
                .hardScore()).isNegative();
        assertThat(schedule.getSolverScore()).isEqualTo(oldScore);
    }

    private Long addConflictingLesson() {
        return lessonService.createLesson(new CreateLessonRequest(schedule.getId(), secondTeacher.getId(),
                secondGroup.getId(), null, 60, false, false, true, overlappingSlot.getId(), room.getId())).id();
    }

    private Teacher teacher(String email, Role role) {
        AbstractUser user = user(email, role);
        Teacher teacher = new Teacher();
        teacher.setUser(user);
        teacher = teachers.save(teacher);
        WeeklyAvailability window = new WeeklyAvailability();
        window.setUser(user);
        window.setAvailabilityRole(AvailabilityRole.TEACHER);
        window.setDayOfWeek(DayOfWeek.MONDAY);
        window.setStartTime(LocalTime.of(9, 0));
        window.setEndTime(LocalTime.of(21, 0));
        availability.save(window);
        return teacher;
    }

    private AbstractUser user(String email, Role role) {
        AbstractUser user = new AbstractUser();
        user.setEmail(email);
        user.setFullName(email);
        user.setPasswordHash("test-password-hash");
        user.setRole(role);
        return users.save(user);
    }
}
