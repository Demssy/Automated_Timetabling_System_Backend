package com.timetable.backend.integration;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import com.timetable.backend.domain.dto.CancelLessonRequest;
import com.timetable.backend.domain.dto.CreateLessonRequest;
import com.timetable.backend.domain.dto.TimeslotDTO;
import com.timetable.backend.domain.dto.UpdateLessonRequest;
import com.timetable.backend.domain.dto.WeeklyAvailabilityRequest;
import com.timetable.backend.domain.exception.ScheduleConflictException;
import com.timetable.backend.domain.model.*;
import com.timetable.backend.domain.repository.*;
import com.timetable.backend.service.DanceGroupService;
import com.timetable.backend.service.LessonService;
import com.timetable.backend.service.ScheduleMetadataService;
import com.timetable.backend.service.ScheduleScoreEvaluationService;
import com.timetable.backend.service.ScheduledLessonCancellationService;
import com.timetable.backend.service.TimeslotService;
import com.timetable.backend.service.WeeklyAvailabilityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;

@SpringBootTest
@ActiveProfiles("test")
class PublishedEditRollbackIntegrationTest {
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
    @Autowired private ScheduleMetadataService metadataService;
    @Autowired private ScheduledLessonCancellationService cancellationService;
    @Autowired private WeeklyAvailabilityService weeklyAvailabilityService;
    @Autowired private TimeslotService timeslotService;
    @Autowired private DanceGroupService danceGroupService;
    @Autowired private TransactionTemplate transactions;
    @Autowired private JdbcTemplate jdbc;
    @MockitoSpyBean private ScheduleScoreEvaluationService evaluator;

    private record Fixture(Long schedule, Long firstUser, Long secondUser, Long admin,
                           Long firstGroup, Long secondGroup, Long firstLesson, Long secondLesson,
                           Long firstSlot, Long overlapSlot, Long secondSlot, Long room, Long secondSnapshot) {}

    private enum SharedFact { AVAILABILITY, TIMESLOT, GROUP_MEMBERSHIP }

    @Test
    void rejectedPublishedCreateMoveAndRestoreLeaveCommittedStateUnchanged() {
        Fixture fixture = transactions.execute(status -> createFixture());
        Authentication admin = new UsernamePasswordAuthenticationToken("rollback-admin@example.test", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        try {
            metadataService.publish(fixture.schedule());
            ScheduleMetadata before = schedules.findById(fixture.schedule()).orElseThrow();
            String score = before.getSolverScore();
            String explanation = before.getScoreExplanation();
            long revision = before.getSolverRevision();

            assertThatThrownBy(() -> lessonService.createLesson(new CreateLessonRequest(
                    fixture.schedule(), fixture.secondUser(), fixture.secondGroup(), null,
                    60, false, false, true, fixture.overlapSlot(), fixture.room())))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("hard constraint violations");
            transactions.executeWithoutResult(status -> {
                assertThat(snapshots.findByScheduleIdOrderByIdAsc(fixture.schedule())).hasSize(2);
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM added_lessons WHERE schedule_id = ?",
                        Integer.class, fixture.schedule())).isZero();
                assertScheduleUnchanged(fixture.schedule(), score, explanation, revision);
            });

            assertThatThrownBy(() -> lessonService.updateLessonManually(fixture.secondSnapshot(),
                    update(fixture, fixture.overlapSlot())))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("hard constraint violations");
            transactions.executeWithoutResult(status -> {
                ScheduledLesson snapshot = snapshots.findById(fixture.secondSnapshot()).orElseThrow();
                assertThat(snapshot.getTimeslot().getId()).isEqualTo(fixture.secondSlot());
                assertThat(snapshot.getStatus()).isEqualTo(ScheduledLessonStatus.ASSIGNED);
                assertScheduleUnchanged(fixture.schedule(), score, explanation, revision);
            });

            cancellationService.cancelLesson(fixture.secondSnapshot(), new CancelLessonRequest("rollback"), admin);
            lessonService.updateLessonManually(fixture.secondSnapshot(), update(fixture, fixture.overlapSlot()));
            ScheduleMetadata cancelled = schedules.findById(fixture.schedule()).orElseThrow();
            String cancelledScore = cancelled.getSolverScore();
            String cancelledExplanation = cancelled.getScoreExplanation();
            long cancelledRevision = cancelled.getSolverRevision();

            assertThatThrownBy(() -> cancellationService.restoreLesson(fixture.secondSnapshot(), admin))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("hard constraint violations");
            transactions.executeWithoutResult(status -> {
                ScheduledLesson snapshot = snapshots.findById(fixture.secondSnapshot()).orElseThrow();
                assertThat(snapshot.isCancelled()).isTrue();
                assertThat(snapshot.getCancelledBy().getId()).isEqualTo(fixture.admin());
                assertThat(snapshot.getCancelledAt()).isNotNull();
                assertThat(snapshot.getCancelReason()).isEqualTo("rollback");
                assertThat(snapshot.getTimeslot().getId()).isEqualTo(fixture.overlapSlot());
                assertScheduleUnchanged(fixture.schedule(), cancelledScore, cancelledExplanation, cancelledRevision);
            });
        } finally {
            transactions.executeWithoutResult(status -> cleanup(fixture));
        }
    }

    @ParameterizedTest
    @EnumSource(SharedFact.class)
    // Opt in on MySQL: shared-fact mutations do not yet participate in the publication lock.
    @EnabledIfSystemProperty(named = "verify.shared-facts", matches = "true")
    void publicationCannotCommitAfterAnUncoordinatedConflictingFactChange(SharedFact fact) throws Exception {
        assertThat(jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<String>)
                connection -> connection.getMetaData().getDatabaseProductName())).isEqualTo("MySQL");
        Fixture fixture = transactions.execute(status -> createFixture());
        CountDownLatch evaluated = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            if (fact == SharedFact.GROUP_MEMBERSHIP) {
                transactions.executeWithoutResult(status -> {
                    Role studentRole = roles.findByName("STUDENT").orElseGet(() -> roles.save(new Role(null, "STUDENT")));
                    jdbc.update("INSERT INTO students (id) VALUES (?)", fixture.firstUser());
                    jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)",
                            fixture.firstUser(), studentRole.getId());
                });
            }
            Long availabilityId = transactions.execute(status -> availability.findByUserIdAndAvailabilityRole(
                    fixture.firstUser(), AvailabilityRole.TEACHER).get(0).getId());
            doAnswer(invocation -> {
                Object result = invocation.callRealMethod();
                evaluated.countDown();
                if (!release.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting to finish publication");
                }
                return result;
            }).when(evaluator).validateAndRecalculate(eq(fixture.schedule()), eq(true));
            Future<?> publishing = executor.submit(() -> metadataService.publish(fixture.schedule()));
            assertThat(evaluated.await(10, TimeUnit.SECONDS)).isTrue();
            Future<?> mutation = executor.submit(() -> {
                switch (fact) {
                    case AVAILABILITY -> weeklyAvailabilityService.updateSlot(availabilityId,
                            new WeeklyAvailabilityRequest(DayOfWeek.MONDAY, LocalTime.of(18, 0),
                                    LocalTime.of(21, 0), AvailabilityRole.TEACHER));
                    case TIMESLOT -> timeslotService.updateTimeslot(fixture.secondSlot(),
                            new TimeslotDTO(fixture.secondSlot(), DayOfWeek.MONDAY,
                                    LocalTime.of(13, 0), LocalTime.of(14, 0)));
                    case GROUP_MEMBERSHIP -> danceGroupService.enrollStudent(fixture.firstGroup(), fixture.firstUser());
                }
            });
            boolean mutationCommittedBeforePublication;
            try {
                mutation.get(2, TimeUnit.SECONDS);
                mutationCommittedBeforePublication = true;
            } catch (TimeoutException blocked) {
                mutationCommittedBeforePublication = false;
            }
            release.countDown();
            boolean published;
            try {
                publishing.get(10, TimeUnit.SECONDS);
                published = true;
            } catch (ExecutionException failed) {
                assertThat(failed.getCause()).isInstanceOf(ScheduleConflictException.class);
                published = false;
            }
            mutation.get(10, TimeUnit.SECONDS);
            if (mutationCommittedBeforePublication && published) {
                assertThat(HardSoftScore.parseScore(evaluator.explain(fixture.schedule()).totalScore()).hardScore())
                        .as("%s committed before publication; published score must still be valid", fact)
                        .isZero();
            }
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(15, TimeUnit.SECONDS)).isTrue();
            reset(evaluator);
            transactions.executeWithoutResult(status -> cleanup(fixture));
        }
    }

    private void assertScheduleUnchanged(Long id, String score, String explanation, long revision) {
        ScheduleMetadata schedule = schedules.findById(id).orElseThrow();
        assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.PUBLISHED);
        assertThat(schedule.getSolverScore()).isEqualTo(score);
        assertThat(schedule.getScoreExplanation()).isEqualTo(explanation);
        assertThat(schedule.getSolverRevision()).isEqualTo(revision);
    }

    private UpdateLessonRequest update(Fixture fixture, Long slot) {
        return new UpdateLessonRequest(fixture.secondUser(), fixture.secondGroup(), null, 60,
                false, false, true, slot, fixture.room());
    }

    private Fixture createFixture() {
        Role teacherRole = roles.findByName("TEACHER").orElseGet(() -> roles.save(new Role(null, "TEACHER")));
        Role adminRole = roles.findByName("ADMIN").orElseGet(() -> roles.save(new Role(null, "ADMIN")));
        Teacher first = teacher("rollback-first@example.test", teacherRole);
        Teacher second = teacher("rollback-second@example.test", teacherRole);
        AbstractUser admin = user("rollback-admin@example.test", adminRole);
        Timeslot firstSlot = timeslots.save(new Timeslot(DayOfWeek.MONDAY, LocalTime.of(12, 31), LocalTime.of(13, 31)));
        Timeslot overlapSlot = timeslots.save(new Timeslot(DayOfWeek.MONDAY, LocalTime.of(13, 1), LocalTime.of(14, 1)));
        Timeslot secondSlot = timeslots.save(new Timeslot(DayOfWeek.MONDAY, LocalTime.of(15, 31), LocalTime.of(16, 31)));
        Room room = rooms.save(new Room("Rollback integration room", 4, true));
        DanceGroup firstGroup = groups.save(new DanceGroup("Rollback first", null, DanceLevel.BEGINNER));
        DanceGroup secondGroup = groups.save(new DanceGroup("Rollback second", null, DanceLevel.BEGINNER));
        ScheduleMetadata schedule = schedules.save(ScheduleMetadata.builder().name("Rollback integration")
                .validFrom(LocalDate.of(2040, 1, 2)).validTo(LocalDate.of(2040, 1, 8))
                .status(ScheduleStatus.DRAFT).build());
        Lesson firstLesson = lessons.save(new Lesson(first, firstGroup, 60, false, true));
        Lesson secondLesson = lessons.save(new Lesson(second, secondGroup, 60, false, true));
        snapshot(schedule, firstLesson, firstSlot, room);
        ScheduledLesson secondSnapshot = snapshot(schedule, secondLesson, secondSlot, room);
        return new Fixture(schedule.getId(), first.getId(), second.getId(), admin.getId(), firstGroup.getId(),
                secondGroup.getId(), firstLesson.getId(), secondLesson.getId(), firstSlot.getId(),
                overlapSlot.getId(), secondSlot.getId(), room.getId(), secondSnapshot.getId());
    }

    private Teacher teacher(String email, Role role) {
        AbstractUser user = user(email, role);
        Teacher teacher = new Teacher();
        teacher.setUser(user);
        teacher = teachers.save(teacher);
        availability.save(new WeeklyAvailability(null, user, DayOfWeek.MONDAY,
                LocalTime.of(9, 0), LocalTime.of(21, 0), AvailabilityRole.TEACHER));
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

    private ScheduledLesson snapshot(ScheduleMetadata schedule, Lesson lesson, Timeslot slot, Room room) {
        ScheduledLesson snapshot = new ScheduledLesson();
        snapshot.setSchedule(schedule);
        snapshot.setLesson(lesson);
        snapshot.setTimeslot(slot);
        snapshot.setRoom(room);
        snapshot.setStatus(ScheduledLessonStatus.ASSIGNED);
        return snapshots.save(snapshot);
    }

    private void cleanup(Fixture fixture) {
        jdbc.update("DELETE FROM scheduled_lessons WHERE schedule_id = ?", fixture.schedule());
        jdbc.update("DELETE FROM added_lessons WHERE schedule_id = ?", fixture.schedule());
        jdbc.update("DELETE FROM lessons WHERE id IN (?, ?)", fixture.firstLesson(), fixture.secondLesson());
        jdbc.update("DELETE FROM schedules WHERE id = ?", fixture.schedule());
        jdbc.update("DELETE FROM weekly_availability WHERE user_id IN (?, ?)", fixture.firstUser(), fixture.secondUser());
        jdbc.update("DELETE FROM student_groups WHERE group_id IN (?, ?)", fixture.firstGroup(), fixture.secondGroup());
        jdbc.update("DELETE FROM user_roles WHERE user_id IN (?, ?, ?)",
                fixture.firstUser(), fixture.secondUser(), fixture.admin());
        jdbc.update("DELETE FROM students WHERE id = ?", fixture.firstUser());
        jdbc.update("DELETE FROM teachers WHERE id IN (?, ?)", fixture.firstUser(), fixture.secondUser());
        jdbc.update("DELETE FROM users WHERE id IN (?, ?, ?)", fixture.firstUser(), fixture.secondUser(), fixture.admin());
        jdbc.update("DELETE FROM dance_groups WHERE id IN (?, ?)", fixture.firstGroup(), fixture.secondGroup());
        jdbc.update("DELETE FROM timeslots WHERE id IN (?, ?, ?)",
                fixture.firstSlot(), fixture.overlapSlot(), fixture.secondSlot());
        jdbc.update("DELETE FROM rooms WHERE id = ?", fixture.room());
    }
}
