package com.timetable.backend.solver;

import ai.timefold.solver.core.api.score.ScoreExplanation;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.api.solver.SolverStatus;
import com.timetable.backend.domain.dto.SolverSettingsValues;
import com.timetable.backend.domain.model.AbstractUser;
import com.timetable.backend.domain.model.DanceGroup;
import com.timetable.backend.domain.model.Lesson;
import com.timetable.backend.domain.model.Student;
import com.timetable.backend.domain.model.Teacher;
import com.timetable.backend.domain.model.Timeslot;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class SolverRuntimeFactoryTest {
    private final SolverRuntimeFactory factory = new SolverRuntimeFactory(new SolverConfig()
            .withSolutionClass(DanceSchedule.class)
            .withEntityClasses(Lesson.class)
            .withConstraintProviderClass(DanceScheduleConstraintProvider.class));

    @Test
    void factoriesCaptureDifferentHardAndSoftWeightsWithoutChangingExistingFactories() {
        Teacher first = teacher(1L);
        Teacher second = teacher(2L);
        Timeslot slot = slot(1L, 17);
        Lesson group1 = group(1L, first, slot);
        Lesson group2 = group(2L, second, slot);
        DanceSchedule schedule = schedule(List.of(first, second), List.of(), List.of(group1, group2), List.of(slot));

        var oldManager = factory.solutionManager(settings(1, 1, 10));
        var newManager = factory.solutionManager(settings(10, 1, 20));
        assertThat(score(oldManager.explain(schedule), "Room conflict")).isEqualTo(HardSoftScore.ofHard(-1));
        assertThat(score(newManager.explain(schedule), "Room conflict")).isEqualTo(HardSoftScore.ofHard(-10));
        assertThat(score(oldManager.explain(schedule), "Reward prime time usage")).isEqualTo(HardSoftScore.ofSoft(20));
        assertThat(score(newManager.explain(schedule), "Reward prime time usage")).isEqualTo(HardSoftScore.ofSoft(40));
        assertThat(score(oldManager.explain(schedule), "Room conflict")).isEqualTo(HardSoftScore.ofHard(-1));
    }

    @Test
    void zeroDailyPrivateWeightRemovesOnlyThatPenalty() {
        Student student = new Student();
        student.setId(5L);
        Teacher teacher = teacher(1L);
        teacher.setPrivateStudents(new HashSet<>(Set.of(student)));
        Timeslot firstSlot = slot(1L, 10);
        Timeslot secondSlot = slot(2L, 12);
        Lesson first = individual(1L, teacher, student, firstSlot);
        Lesson second = individual(2L, teacher, student, secondSlot);
        DanceSchedule schedule = schedule(List.of(teacher), List.of(student), List.of(first, second),
                List.of(firstSlot, secondSlot));
        var enabled = factory.solutionManager(settings(1, 1, 10)).explain(schedule);
        var disabled = factory.solutionManager(settings(1, 0, 10)).explain(schedule);
        assertThat(score(enabled, "Student max one private lesson per day")).isEqualTo(HardSoftScore.ofHard(-1));
        assertThat(score(disabled, "Student max one private lesson per day")).isEqualTo(HardSoftScore.ZERO);
        assertThat(disabled.getScore().hardScore() - enabled.getScore().hardScore()).isEqualTo(1);
    }

    @Test
    void solverStopsAtConfiguredTimeLimit() {
        SolverSettingsValues settings = new SolverSettingsValues(
                5, 0, 6, 15, 1, 1, 1, 1, 1, 100, 10, 50, 30, 1, 1);
        long start = System.nanoTime();
        factory.create(settings).buildSolver().solve(problem(3L));
        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);
        assertThat(elapsed).isGreaterThanOrEqualTo(Duration.ofSeconds(4));
        assertThat(elapsed).isLessThan(Duration.ofSeconds(15));
    }

    @Test
    void runningJobsKeepTheirSnapshotWhenSettingsChange() throws Exception {
        SolverRuntimeRegistry registry = new SolverRuntimeRegistry(factory);
        CountDownLatch oldResult = new CountDownLatch(1);
        CountDownLatch newResult = new CountDownLatch(1);
        AtomicReference<DanceSchedule> oldSolution = new AtomicReference<>();
        AtomicReference<DanceSchedule> newSolution = new AtomicReference<>();
        SolverSettingsValues oldSettings = settings(1, 1, 10);
        SolverSettingsValues newSettings = settings(10, 1, 10);
        try {
            registry.solve(1L, oldSettings, id -> problem(id), solution -> {
                oldSolution.set(solution);
                oldResult.countDown();
            });
            assertThat(oldResult.await(10, TimeUnit.SECONDS)).isTrue();
            registry.solve(2L, newSettings, id -> problem(id), solution -> {
                newSolution.set(solution);
                newResult.countDown();
            });
            assertThat(newResult.await(10, TimeUnit.SECONDS)).isTrue();
            assertThat(registry.status(1L)).isNotEqualTo(SolverStatus.NOT_SOLVING);
            assertThat(score(factory.solutionManager(oldSettings).explain(oldSolution.get()), "Room conflict"))
                    .isEqualTo(HardSoftScore.ofHard(-1));
            assertThat(score(factory.solutionManager(newSettings).explain(newSolution.get()), "Room conflict"))
                    .isEqualTo(HardSoftScore.ofHard(-10));
        } finally {
            registry.close();
        }
    }

    private DanceSchedule problem(long id) {
        Teacher first = teacher(1L);
        Teacher second = teacher(2L);
        Teacher third = teacher(3L);
        Student student = new Student();
        student.setId(5L);
        third.setPrivateStudents(new HashSet<>(Set.of(student)));
        Timeslot slot = slot(1L, 17);
        Timeslot later = slot(2L, 19);
        Lesson group1 = group(1L, first, slot);
        Lesson group2 = group(2L, second, slot);
        Lesson flexible = individual(3L, third, null, null);
        return new DanceSchedule(id, List.of(slot, later), List.of(first, second, third),
                List.of(), List.of(), List.of(), List.of(student), List.of(group1, group2, flexible));
    }

    private HardSoftScore score(ScoreExplanation<DanceSchedule, HardSoftScore> explanation, String name) {
        return explanation.getConstraintMatchTotalMap().values().stream()
                .filter(total -> total.getConstraintRef().constraintName().equals(name))
                .map(total -> total.getScore()).findFirst().orElse(HardSoftScore.ZERO);
    }

    private SolverSettingsValues settings(int physical, int daily, int prime) {
        return new SolverSettingsValues(10, 0, 6, 15, physical, 1, 1, daily, 1, 100, prime, 50, 30, 1, 1);
    }

    private Teacher teacher(long id) {
        AbstractUser user = new AbstractUser();
        user.setId(id);
        Teacher teacher = new Teacher();
        teacher.setId(id);
        teacher.setUser(user);
        teacher.setPrivateStudents(new HashSet<>());
        return teacher;
    }

    private Timeslot slot(long id, int hour) {
        Timeslot timeslot = new Timeslot();
        timeslot.setId(id);
        timeslot.setDayOfWeek(DayOfWeek.MONDAY);
        timeslot.setStartTime(LocalTime.of(hour, 0));
        timeslot.setEndTime(LocalTime.of(hour + 1, 0));
        return timeslot;
    }

    private Lesson group(long id, Teacher teacher, Timeslot timeslot) {
        DanceGroup group = new DanceGroup();
        group.setId(id);
        Lesson lesson = individual(id, teacher, null, timeslot);
        lesson.setPrivate(false);
        lesson.setDanceGroup(group);
        lesson.setPinned(true);
        return lesson;
    }

    private Lesson individual(long id, Teacher teacher, Student student, Timeslot timeslot) {
        Lesson lesson = new Lesson();
        lesson.setId(id);
        lesson.setTeacher(teacher);
        lesson.setStudent(student);
        lesson.setTimeslot(timeslot);
        lesson.setPrivate(true);
        return lesson;
    }

    private DanceSchedule schedule(List<Teacher> teachers, List<Student> students,
            List<Lesson> lessons, List<Timeslot> timeslots) {
        return new DanceSchedule(1L, timeslots, teachers, List.of(), List.of(), List.of(), students, lessons);
    }
}
