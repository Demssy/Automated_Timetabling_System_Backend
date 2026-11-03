package com.timetable.backend.solver;

import ai.timefold.solver.core.api.score.ScoreExplanation;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.localsearch.decider.acceptor.LocalSearchAcceptorConfig;
import ai.timefold.solver.core.config.localsearch.decider.forager.LocalSearchForagerConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import benchmark.BenchmarkConstraintProvider;
import com.timetable.backend.domain.model.*;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SolverBenchmarkEvaluationTest {

    private static final int STEP_LIMIT = 2000;
    private static final Duration TIMEOUT = Duration.ofSeconds(60);
    private static final LocalTime PRIME_START = LocalTime.of(16, 0);
    private static final LocalTime PRIME_END = LocalTime.of(21, 0);

    private record Dataset(String name, int teachers, int students, int groups, int privateLessons,
                           int slotsPerDay, boolean dualRole) {}

    private record Result(String dataset, String algorithm, long millis, HardSoftScore score,
                          int hardMatches, int softPenaltyMatches, int softRewardMatches,
                          double completeness, double primeOccupied, double primeCapacity,
                          long assignedPrivate, long demand, String termination) {}

    @Test
    void compareTimefoldWithFirstFit() {
        SolverConfig config = new SolverConfig()
                .withEnvironmentMode(EnvironmentMode.REPRODUCIBLE)
                .withRandomSeed(42L)
                .withSolutionClass(DanceSchedule.class)
                .withEntityClasses(Lesson.class)
                .withConstraintProviderClass(BenchmarkConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig().withSpentLimit(TIMEOUT))
                .withPhases(new ConstructionHeuristicPhaseConfig(),
                        new LocalSearchPhaseConfig()
                                .withAcceptorConfig(new LocalSearchAcceptorConfig().withEntityTabuSize(5))
                                .withForagerConfig(new LocalSearchForagerConfig().withAcceptedCountLimit(20))
                                .withTerminationConfig(new TerminationConfig().withStepCountLimit(STEP_LIMIT)));
        SolverFactory<DanceSchedule> factory = SolverFactory.create(config);
        SolutionManager<DanceSchedule, HardSoftScore> manager = SolutionManager.create(factory);
        List<Dataset> datasets = List.of(
                new Dataset("A", 3, 15, 6, 14, 3, false),
                new Dataset("B", 4, 35, 12, 48, 5, true),
                new Dataset("C", 6, 60, 20, 80, 7, false));

        System.out.println("Dataset | Algorithm | ms | score | hard matches | soft penalty matches | "
                + "soft reward matches | completeness | prime occupied | prime capacity | demand | termination");
        for (Dataset dataset : datasets) {
            DanceSchedule baselineInput = buildProblem(dataset);
            DanceSchedule solverInput = buildProblem(dataset);
            assertIndependentEqualInputs(dataset, baselineInput, solverInput);

            long start = System.nanoTime();
            firstFit(baselineInput, manager);
            long baselineMillis = Duration.ofNanos(System.nanoTime() - start).toMillis();
            Result baseline = summarize(dataset.name(), "first-fit", baselineMillis, baselineInput, manager, "completed");
            print(baseline);

            Solver<DanceSchedule> solver = factory.buildSolver();
            start = System.nanoTime();
            DanceSchedule solved = solver.solve(solverInput);
            long solverMillis = Duration.ofNanos(System.nanoTime() - start).toMillis();
            String termination = solverMillis < TIMEOUT.toMillis()
                    ? "before timeout" : "timeout or startup overhead; not step-reproducible";
            Result timefold = summarize(dataset.name(), "Timefold", solverMillis, solved, manager, termination);
            print(timefold);

            if (!dataset.name().equals("C")) {
                assertThat(baseline.score().hardScore()).as("%s baseline hard score", dataset.name()).isZero();
                assertThat(timefold.score().hardScore()).as("%s solver hard score", dataset.name()).isZero();
                assertThat(baseline.score().isSolutionInitialized()).isTrue();
                assertThat(timefold.score().isSolutionInitialized()).isTrue();
                assertThat(timefold.completeness()).as("%s solver completeness", dataset.name()).isEqualTo(100.0);
                assertThat(baseline.completeness()).as("%s baseline completeness", dataset.name())
                        .isBetween(75.0, 85.0);
                assertThat(timefold.assignedPrivate()).as("%s assigned demand improvement", dataset.name())
                        .isGreaterThan(baseline.assignedPrivate());
                assertThat(timefold.primeCapacity() - baseline.primeCapacity())
                        .as("%s prime capacity improvement in percentage points", dataset.name())
                        .isGreaterThanOrEqualTo(4.0);
                assertThat(timefold.score().softScore() - baseline.score().softScore())
                        .as("%s soft score improvement", dataset.name()).isGreaterThanOrEqualTo(200);
            }
        }
    }

    @Test
    void metricsHandleEmptyAndPartiallyOccupiedPrimeTime() {
        Dataset empty = new Dataset("empty", 1, 0, 0, 0, 1, false);
        DanceSchedule schedule = buildProblem(empty);
        SolverFactory<DanceSchedule> factory = SolverFactory.create(new SolverConfig()
                .withSolutionClass(DanceSchedule.class)
                .withEntityClasses(Lesson.class)
                .withConstraintProviderClass(BenchmarkConstraintProvider.class));
        Result result = summarize("empty", "test", 0, schedule, SolutionManager.create(factory), "completed");
        assertThat(result.completeness()).isEqualTo(100.0);
        assertThat(result.primeOccupied()).isZero();
        assertThat(result.primeCapacity()).isZero();
        assertThat(result.assignedPrivate()).isZero();
        assertThat(result.demand()).isZero();
        assertThat(demandPercent(result)).isEqualTo(100.0);

        DanceSchedule occupied = buildProblem(new Dataset("occupied", 1, 1, 1, 1, 1, false));
        Lesson privateLesson = occupied.getLessonList().get(1);
        privateLesson.setTimeslot(occupied.getTimeslotList().get(0));
        privateLesson.setStudent(occupied.getStudentList().get(0));
        result = summarize("occupied", "test", 0, occupied, SolutionManager.create(factory), "completed");
        assertThat(result.completeness()).isEqualTo(100.0);
        assertThat(result.primeOccupied()).isEqualTo(4.0);
        assertThat(result.primeCapacity()).isEqualTo(4.0);
        assertThat(result.assignedPrivate()).isEqualTo(1);
        assertThat(result.demand()).isEqualTo(2);
        assertThat(demandPercent(result)).isEqualTo(50.0);

        occupied.getLessonList().getFirst().setTimeslot(occupied.getTimeslotList().get(1));
        result = summarize("occupied", "test", 0, occupied, SolutionManager.create(factory), "completed");
        assertThat(result.primeOccupied()).isEqualTo(8.0);
        assertThat(result.primeCapacity()).isEqualTo(5.0);
    }

    private static DanceSchedule buildProblem(Dataset dataset) {
        List<Timeslot> slots = new ArrayList<>();
        for (int day = 0; day < 5; day++) {
            for (int hour = 0; hour < dataset.slotsPerDay(); hour++) {
                LocalTime start = LocalTime.of(21 - dataset.slotsPerDay() + hour, 0);
                slots.add(new Timeslot((long) slots.size() + 1, DayOfWeek.of(day + 1), start, start.plusHours(1)));
            }
        }

        List<Student> students = new ArrayList<>();
        for (int i = 0; i < dataset.students(); i++) {
            Student student = new Student();
            student.setId(dataset.dualRole() && i == 0 ? 1L : 1000L + i);
            student.setFullName("Student " + i);
            student.setDesiredLessonsPerWeek(2);
            students.add(student);
        }
        List<Teacher> teachers = new ArrayList<>();
        for (int i = 0; i < dataset.teachers(); i++) {
            AbstractUser user = dataset.dualRole() && i == 0 ? students.get(0) : new AbstractUser();
            user.setId((long) i + 1);
            user.setFullName("Teacher " + i);
            if (dataset.dualRole() && i == 0) {
                user.setRole(new Role(1L, "TEACHER"));
                user.addRole(new Role(2L, "STUDENT"));
            }
            Teacher teacher = new Teacher();
            teacher.setId(user.getId());
            teacher.setUser(user);
            Set<Student> subscribers = new LinkedHashSet<>(students);
            subscribers.removeIf(student -> student.getId().equals(teacher.getId()));
            if (dataset.name().equals("A") || dataset.name().equals("B")) {
                // The first teacher can use these students, but the second teacher depends on them.
                int firstShared = dataset.dualRole() ? 1 : 0;
                int sharedCount = dataset.dualRole() ? 6 : 3;
                Set<Student> shared = new LinkedHashSet<>(
                        students.subList(firstShared, firstShared + sharedCount));
                if (i == 1) {
                    subscribers.retainAll(shared);
                } else if (i > 1) {
                    subscribers.removeAll(shared);
                }
            }
            teacher.setPrivateStudents(subscribers);
            teachers.add(teacher);
        }
        List<WeeklyAvailability> availability = new ArrayList<>();
        for (Teacher teacher : teachers) {
            addAvailability(availability, teacher.getUser(), AvailabilityRole.TEACHER);
        }
        for (Student student : students) {
            addAvailability(availability, student, AvailabilityRole.STUDENT);
        }
        Room room = new Room(1L, "Benchmark room", 4, true);
        List<Lesson> lessons = new ArrayList<>();
        for (int i = 0; i < dataset.groups(); i++) {
            DanceGroup group = new DanceGroup();
            group.setId((long) i + 1);
            group.setName("Group " + i);
            if (dataset.dualRole() && i == 1) {
                group.setEnrolledStudents(new LinkedHashSet<>(List.of(students.get(0))));
            }
            Lesson lesson = new Lesson(teachers.get(i % teachers.size()), group, 60, false, true);
            lesson.setId((long) lessons.size() + 1);
            lesson.setPinned(true);
            lesson.setTimeslot(slots.get((i * 2) % slots.size()));
            lesson.setRoom(room);
            lessons.add(lesson);
        }
        for (int i = 0; i < dataset.privateLessons(); i++) {
            Lesson lesson = new Lesson(teachers.get(i % teachers.size()), (Student) null, 60, true);
            lesson.setId((long) lessons.size() + 1);
            lesson.setRoom(room);
            lessons.add(lesson);
        }
        if (dataset.name().equals("A") || dataset.name().equals("B")) {
            lessons.subList(dataset.groups(), lessons.size())
                    .sort(Comparator.comparing(lesson -> lesson.getTeacher().getId()));
        }
        return new DanceSchedule(1L, slots, teachers, List.of(), availability,
                List.of(LocalDate.of(2026, 11, 2)), students, lessons);
    }

    private static void addAvailability(List<WeeklyAvailability> availability, AbstractUser user,
                                        AvailabilityRole role) {
        for (int day = 1; day <= 5; day++) {
            availability.add(new WeeklyAvailability((long) availability.size() + 1, user,
                    DayOfWeek.of(day), LocalTime.of(8, 0), LocalTime.of(22, 0), role));
        }
    }

    private static void assertIndependentEqualInputs(Dataset dataset, DanceSchedule first, DanceSchedule second) {
        assertThat(first).isNotSameAs(second);
        assertThat(first.getLessonList()).hasSize(dataset.groups() + dataset.privateLessons());
        assertThat(first.getTeacherList()).hasSize(dataset.teachers());
        assertThat(first.getStudentList()).hasSize(dataset.students());
        assertThat(first.getTimeslotList()).hasSize(dataset.slotsPerDay() * 5);
        assertThat(first.getLessonList().get(0)).isNotSameAs(second.getLessonList().get(0));
        assertThat(first.getLessonList().stream().map(Lesson::getId).toList())
                .isEqualTo(second.getLessonList().stream().map(Lesson::getId).toList());
    }

    private static void firstFit(DanceSchedule schedule, SolutionManager<DanceSchedule, HardSoftScore> manager) {
        for (Lesson lesson : schedule.getLessonList()) {
            if (!lesson.isPrivate()) {
                continue;
            }
            boolean assigned = false;
            for (Timeslot slot : schedule.getTimeslotList()) {
                if (!canPlace(schedule, lesson, slot)) {
                    continue;
                }
                lesson.setTimeslot(slot);
                for (Student student : lesson.getTeacher().getPrivateStudents()) {
                    if (!canAttend(schedule, student, slot)) {
                        continue;
                    }
                    lesson.setStudent(student);
                    if (manager.update(schedule).hardScore() == 0) {
                        assigned = true;
                        break;
                    }
                    lesson.setStudent(null);
                }
                if (assigned) {
                    break;
                }
                lesson.setTimeslot(null);
            }
            if (!assigned) {
                for (Timeslot slot : schedule.getTimeslotList()) {
                    if (canPlace(schedule, lesson, slot)) {
                        lesson.setTimeslot(slot);
                        if (manager.update(schedule).hardScore() == 0) {
                            break;
                        }
                        lesson.setTimeslot(null);
                    }
                }
            }
        }
    }

    private static boolean canPlace(DanceSchedule schedule, Lesson candidate, Timeslot slot) {
        int simultaneousPrivate = 0;
        int teacherDay = 0;
        for (Lesson other : schedule.getLessonList()) {
            if (other == candidate || other.getTimeslot() == null) {
                continue;
            }
            if (other.getTeacher().getId().equals(candidate.getTeacher().getId())
                    && other.getTimeslot().getDayOfWeek() == slot.getDayOfWeek()) {
                teacherDay++;
            }
            if (!overlaps(slot, other.getTimeslot())) {
                continue;
            }
            if (other.getTeacher().getId().equals(candidate.getTeacher().getId())
                    || !other.isPrivate()) {
                return false;
            }
            simultaneousPrivate++;
        }
        return teacherDay < 6 && simultaneousPrivate < 4;
    }

    private static boolean canAttend(DanceSchedule schedule, Student student, Timeslot slot) {
        int assigned = 0;
        for (Lesson other : schedule.getLessonList()) {
            if (other.getStudent() == student) {
                assigned++;
                if (other.getTimeslot() != null
                        && other.getTimeslot().getDayOfWeek() == slot.getDayOfWeek()) {
                    return false;
                }
            }
            if (other.getTimeslot() != null && overlaps(slot, other.getTimeslot())
                    && (other.getTeacher().getId().equals(student.getId())
                    || other.getDanceGroup() != null && other.getDanceGroup().getEnrolledStudents().contains(student))) {
                return false;
            }
        }
        return assigned < student.getDesiredLessonsPerWeek();
    }

    private static boolean overlaps(Timeslot first, Timeslot second) {
        return first.getDayOfWeek() == second.getDayOfWeek()
                && first.getStartTime().isBefore(second.getEndTime())
                && second.getStartTime().isBefore(first.getEndTime());
    }

    private static Result summarize(String dataset, String algorithm, long millis, DanceSchedule schedule,
                                    SolutionManager<DanceSchedule, HardSoftScore> manager, String termination) {
        ScoreExplanation<DanceSchedule, HardSoftScore> explanation = manager.explain(schedule);
        int hardMatches = 0;
        int softPenaltyMatches = 0;
        int softRewardMatches = 0;
        for (var total : explanation.getConstraintMatchTotalMap().values()) {
            for (var match : total.getConstraintMatchSet()) {
                if (match.getScore().hardScore() < 0) {
                    hardMatches++;
                }
                if (match.getScore().softScore() < 0) {
                    softPenaltyMatches++;
                } else if (match.getScore().softScore() > 0) {
                    softRewardMatches++;
                }
            }
            if (total.getScore().hardScore() < 0) {
                System.out.printf("%s | %s | hard constraint: %s (%s)%n",
                        dataset, algorithm, total.getConstraintRef(), total.getScore());
            }
        }

        List<Lesson> assigned = schedule.getLessonList().stream()
                .filter(SolverBenchmarkEvaluationTest::fullyAssigned).toList();
        double completeness = schedule.getLessonList().isEmpty() ? 100.0
                : 100.0 * assigned.size() / schedule.getLessonList().size();
        Set<DayOfWeek> days = new LinkedHashSet<>();
        schedule.getTimeslotList().forEach(slot -> days.add(slot.getDayOfWeek()));
        int occupied = 0;
        int capacityQuarters = 0;
        for (DayOfWeek day : days) {
            for (LocalTime start = PRIME_START; start.isBefore(PRIME_END); start = start.plusMinutes(30)) {
                LocalTime end = start.plusMinutes(30);
                int quarters = 0;
                for (Lesson lesson : assigned) {
                    Timeslot slot = lesson.getTimeslot();
                    if (slot.getDayOfWeek() == day && slot.getStartTime().isBefore(end)
                            && start.isBefore(slot.getEndTime())) {
                        quarters += lesson.isPrivate() ? 1 : 4;
                    }
                }
                if (quarters > 0) {
                    occupied++;
                }
                capacityQuarters += Math.min(4, quarters);
            }
        }
        int intervals = days.size() * 10;
        double primeOccupied = intervals == 0 ? 0 : 100.0 * occupied / intervals;
        double primeCapacity = intervals == 0 ? 0 : 100.0 * capacityQuarters / (intervals * 4);
        long assignedPrivate = assigned.stream().filter(Lesson::isPrivate).count();
        long demand = schedule.getStudentList().stream().map(Student::getDesiredLessonsPerWeek)
                .filter(value -> value != null).mapToLong(Integer::longValue).sum();
        return new Result(dataset, algorithm, millis, explanation.getScore(), hardMatches, softPenaltyMatches,
                softRewardMatches, completeness, primeOccupied, primeCapacity, assignedPrivate, demand, termination);
    }

    private static boolean fullyAssigned(Lesson lesson) {
        return lesson.getTimeslot() != null && lesson.getRoom() != null
                && (!lesson.isPrivate() || lesson.getStudent() != null);
    }

    private static double demandPercent(Result result) {
        return result.demand() == 0 ? 100.0 : Math.min(100.0, 100.0 * result.assignedPrivate() / result.demand());
    }

    private static void print(Result result) {
        System.out.printf(java.util.Locale.ROOT,
                "%s | %s | %d | %s | %d | %d | %d | %.1f%% | %.1f%% | %.1f%% | %d/%d (%.1f%%) | %s%n",
                result.dataset(), result.algorithm(), result.millis(), result.score(), result.hardMatches(),
                result.softPenaltyMatches(), result.softRewardMatches(), result.completeness(),
                result.primeOccupied(), result.primeCapacity(), result.assignedPrivate(), result.demand(),
                demandPercent(result), result.termination());
    }
}
