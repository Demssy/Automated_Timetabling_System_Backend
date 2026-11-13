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
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SolverBenchmarkEvaluationTest {

    private static final int STEP_LIMIT = 20000;
    private static final int UNIMPROVED_STEP_LIMIT = 1500;
    private static final Duration OPERATIONAL_TIMEOUT = Duration.ofSeconds(10);
    private static final long[] SEEDS = {42L, 101L, 202L, 303L, 404L};

    private static final LocalTime PRIME_START = LocalTime.of(16, 0);
    private static final LocalTime PRIME_END = LocalTime.of(21, 0);
    private static final LocalDate SCHEDULE_ANCHOR = LocalDate.of(2026, 11, 2);

    private record Dataset(String name, int teachers, int students, int groups, int privateLessons,
                           int slotsPerDay, boolean dualRole) {}

    private record Result(String dataset, long seed, String algorithm, long millis, HardSoftScore score,
                          int hardMatches, int softPenaltyMatches, int softRewardMatches,
                          double completeness, double primeOccupied, double primeCapacity,
                          long assignedPrivate, long demand) {}

    @Test
    void compareTimefoldWithFirstFitAcrossMultipleSeeds() {
        List<Dataset> datasets = List.of(
                new Dataset("A", 3, 12, 6, 14, 4, false),
                new Dataset("B", 4, 28, 10, 50, 6, true),
                new Dataset("C", 6, 48, 16, 84, 8, true)
        );

        // Warm up JVM and Timefold Constraint Streams to avoid cold-JIT distortion
        SolverFactory<DanceSchedule> warmupFactory = buildFactory(42L, Duration.ofSeconds(1), 200, 100);
        warmupFactory.buildSolver().solve(buildProblem(datasets.get(0)));

        List<Result> allResults = new ArrayList<>();

        for (Dataset dataset : datasets) {
            for (long seed : SEEDS) {
                SolverFactory<DanceSchedule> factory = buildFactory(
                        seed, OPERATIONAL_TIMEOUT, STEP_LIMIT, UNIMPROVED_STEP_LIMIT);
                SolutionManager<DanceSchedule, HardSoftScore> manager = SolutionManager.create(factory);

                DanceSchedule baselineInput = buildProblem(dataset);
                DanceSchedule solverInput = buildProblem(dataset);
                assertIndependentEqualInputs(dataset, baselineInput, solverInput);

                // 1. First-Fit greedy baseline
                long start = System.nanoTime();
                firstFit(baselineInput, manager);
                long baselineMillis = Duration.ofNanos(System.nanoTime() - start).toMillis();
                allResults.add(summarize(dataset.name(), seed, "First-Fit", baselineMillis, baselineInput, manager));

                // 2. Timefold (Construction Heuristic + Entity Tabu Search)
                Solver<DanceSchedule> solver = factory.buildSolver();
                start = System.nanoTime();
                DanceSchedule solved = solver.solve(solverInput);
                long solverMillis = Duration.ofNanos(System.nanoTime() - start).toMillis();
                allResults.add(summarize(dataset.name(), seed, "Timefold", solverMillis, solved, manager));
            }
        }

        printAggregatedSummary(datasets, allResults);

        for (Dataset dataset : datasets) {
            List<Result> tfRuns = allResults.stream()
                    .filter(r -> r.dataset().equals(dataset.name()) && r.algorithm().equals("Timefold"))
                    .toList();
            List<Result> ffRuns = allResults.stream()
                    .filter(r -> r.dataset().equals(dataset.name()) && r.algorithm().equals("First-Fit"))
                    .toList();

            double meanTfHard = tfRuns.stream().mapToInt(r -> r.score().hardScore()).average().orElse(-1);
            double meanTfSoft = tfRuns.stream().mapToInt(r -> r.score().softScore()).average().orElse(0);
            double meanFfSoft = ffRuns.stream().mapToInt(r -> r.score().softScore()).average().orElse(0);
            double meanTfComp = tfRuns.stream().mapToDouble(Result::completeness).average().orElse(0);
            double meanFfComp = ffRuns.stream().mapToDouble(Result::completeness).average().orElse(0);
            double meanTfMillis = tfRuns.stream().mapToLong(Result::millis).average().orElse(99999);

            assertThat(meanTfHard).as("Dataset %s Timefold must achieve 0 hard violations", dataset.name()).isZero();
            assertThat(meanTfMillis).as("Dataset %s Timefold must finish under 10s operational limit", dataset.name())
                    .isLessThan(OPERATIONAL_TIMEOUT.toMillis());
            assertThat(meanTfComp).as("Dataset %s Timefold completeness must exceed First-Fit", dataset.name())
                    .isGreaterThan(meanFfComp);
            assertThat(meanTfSoft).as("Dataset %s Timefold soft score must exceed First-Fit", dataset.name())
                    .isGreaterThan(meanFfSoft);
        }
    }

    private static SolverFactory<DanceSchedule> buildFactory(long seed, Duration timeout,
                                                             int stepLimit, int unimprovedLimit) {
        SolverConfig config = new SolverConfig()
                .withEnvironmentMode(EnvironmentMode.REPRODUCIBLE)
                .withRandomSeed(seed)
                .withSolutionClass(DanceSchedule.class)
                .withEntityClasses(Lesson.class)
                .withConstraintProviderClass(BenchmarkConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig().withSpentLimit(timeout))
                .withPhases(
                        new ConstructionHeuristicPhaseConfig(),
                        new LocalSearchPhaseConfig()
                                .withAcceptorConfig(new LocalSearchAcceptorConfig().withEntityTabuSize(5))
                                .withForagerConfig(new LocalSearchForagerConfig().withAcceptedCountLimit(20))
                                .withTerminationConfig(new TerminationConfig()
                                        .withStepCountLimit(stepLimit)
                                        .withUnimprovedStepCountLimit(unimprovedLimit))
                );
        return SolverFactory.create(config);
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
            subscribers.removeIf(st -> st.getId().equals(teacher.getId()));

            // Shared student bottleneck: Teacher 1 depends on a small shared subset also accessible to Teacher 0
            int firstShared = dataset.dualRole() ? 1 : 0;
            int sharedCount = dataset.name().equals("A") ? 3 : (dataset.name().equals("B") ? 6 : 8);
            Set<Student> shared = new LinkedHashSet<>(students.subList(firstShared, firstShared + sharedCount));
            if (i == 1) {
                subscribers.retainAll(shared);
            } else if (i > 1) {
                subscribers.removeAll(shared);
            }

            teacher.setPrivateStudents(subscribers);
            teachers.add(teacher);
        }

        List<WeeklyAvailability> availability = new ArrayList<>();
        long waId = 1L;
        for (int tIdx = 0; tIdx < teachers.size(); tIdx++) {
            Teacher teacher = teachers.get(tIdx);
            for (int day = 1; day <= 5; day++) {
                LocalTime shiftStart = (day + tIdx) % 3 == 0 ? LocalTime.of(15, 0) : LocalTime.of(12, 0);
                availability.add(new WeeklyAvailability(waId++, teacher.getUser(),
                        DayOfWeek.of(day), shiftStart, LocalTime.of(21, 0), AvailabilityRole.TEACHER));
            }
        }
        for (int sIdx = 0; sIdx < students.size(); sIdx++) {
            Student student = students.get(sIdx);
            for (int day = 1; day <= 5; day++) {
                LocalTime stStart = (sIdx + day) % 2 == 0 ? LocalTime.of(14, 0) : LocalTime.of(16, 0);
                availability.add(new WeeklyAvailability(waId++, student,
                        DayOfWeek.of(day), stStart, LocalTime.of(21, 0), AvailabilityRole.STUDENT));
            }
        }

        // One-time absence on Wednesday 17:00-18:00 for Teacher 0
        List<ResourceUnavailability> unavailabilities = new ArrayList<>();
        ResourceUnavailability ru = new ResourceUnavailability();
        ru.setId(1L);
        ru.setUser(teachers.get(0).getUser());
        ru.setAvailabilityRole(AvailabilityRole.TEACHER);
        ru.setDate(SCHEDULE_ANCHOR.plusDays(2));
        ru.setStartTime(LocalTime.of(17, 0));
        ru.setEndTime(LocalTime.of(18, 0));
        ru.setReason("Competition rehearsal");
        unavailabilities.add(ru);

        Room room = new Room(1L, "Main Studio Hall", 4, true);
        List<Lesson> lessons = new ArrayList<>();
        Set<Integer> usedGroupSlots = new LinkedHashSet<>();

        for (int i = 0; i < dataset.groups(); i++) {
            DanceGroup group = new DanceGroup();
            group.setId((long) i + 1);
            group.setName("Group " + i);
            if (dataset.dualRole() && i == 1) {
                group.setEnrolledStudents(new LinkedHashSet<>(List.of(students.get(0))));
            }
            int tIdx = i % teachers.size();
            Teacher assignedTeacher = teachers.get(tIdx);

            // Pick a valid, non-conflicting slot strictly inside assignedTeacher's weekly shift & outside unavailability
            int chosenSlotIdx = -1;
            for (int step = 0; step < slots.size(); step++) {
                int candidateIdx = (i * 3 + step) % slots.size();
                if (usedGroupSlots.contains(candidateIdx)) {
                    continue;
                }
                Timeslot cand = slots.get(candidateIdx);
                int dayNum = cand.getDayOfWeek().getValue();
                LocalTime shiftStart = (dayNum + tIdx) % 3 == 0 ? LocalTime.of(15, 0) : LocalTime.of(12, 0);
                boolean insideShift = !cand.getStartTime().isBefore(shiftStart);
                boolean hitsAbsence = (tIdx == 0 && dayNum == 3 && cand.getStartTime().equals(LocalTime.of(17, 0)));
                // Also avoid dual-role overlap if Teacher 0 is enrolled in Group 1
                boolean dualConflict = dataset.dualRole() && (i == 1) && dayNum == 1 && cand.getStartTime().equals(slots.get(0).getStartTime());
                if (insideShift && !hitsAbsence && !dualConflict) {
                    chosenSlotIdx = candidateIdx;
                    break;
                }
            }
            usedGroupSlots.add(chosenSlotIdx);

            Lesson lesson = new Lesson(assignedTeacher, group, 60, false, true);
            lesson.setId((long) lessons.size() + 1);
            lesson.setPinned(true);
            lesson.setTimeslot(slots.get(chosenSlotIdx));
            lesson.setRoom(room);
            lessons.add(lesson);
        }

        for (int i = 0; i < dataset.privateLessons(); i++) {
            Lesson lesson = new Lesson(teachers.get(i % teachers.size()), (Student) null, 60, true);
            lesson.setId((long) lessons.size() + 1);
            lesson.setRoom(room);
            lessons.add(lesson);
        }

        lessons.subList(dataset.groups(), lessons.size())
                .sort(Comparator.comparing(lesson -> lesson.getTeacher().getId()));

        return new DanceSchedule(1L, slots, teachers, unavailabilities, availability,
                List.of(SCHEDULE_ANCHOR), students, lessons);
    }

    private static void assertIndependentEqualInputs(Dataset dataset, DanceSchedule first, DanceSchedule second) {
        assertThat(first).isNotSameAs(second);
        assertThat(first.getLessonList()).hasSize(dataset.groups() + dataset.privateLessons());
        assertThat(first.getTeacherList()).hasSize(dataset.teachers());
        assertThat(first.getStudentList()).hasSize(dataset.students());
        assertThat(first.getTimeslotList()).hasSize(dataset.slotsPerDay() * 5);
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
            if (other.getTeacher().getId().equals(candidate.getTeacher().getId()) || !other.isPrivate()) {
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
                    || (other.getDanceGroup() != null && other.getDanceGroup().getEnrolledStudents().contains(student)))) {
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

    private static Result summarize(String dataset, long seed, String algorithm, long millis, DanceSchedule schedule,
                                    SolutionManager<DanceSchedule, HardSoftScore> manager) {
        ScoreExplanation<DanceSchedule, HardSoftScore> explanation = manager.explain(schedule);
        int hardMatches = 0;
        int softPenaltyMatches = 0;
        int softRewardMatches = 0;
        for (var total : explanation.getConstraintMatchTotalMap().values()) {
            for (var match : total.getConstraintMatchSet()) {
                if (match.getScore().hardScore() < 0) hardMatches++;
                if (match.getScore().softScore() < 0) softPenaltyMatches++;
                else if (match.getScore().softScore() > 0) softRewardMatches++;
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
                if (quarters > 0) occupied++;
                capacityQuarters += Math.min(4, quarters);
            }
        }
        int intervals = days.size() * 10;
        double primeOccupied = intervals == 0 ? 0 : 100.0 * occupied / intervals;
        double primeCapacity = intervals == 0 ? 0 : 100.0 * capacityQuarters / (intervals * 4);
        long assignedPrivate = assigned.stream().filter(Lesson::isPrivate).count();
        long demand = schedule.getStudentList().stream()
                .map(Student::getDesiredLessonsPerWeek)
                .filter(v -> v != null)
                .mapToLong(Integer::longValue).sum();

        return new Result(dataset, seed, algorithm, millis, explanation.getScore(), hardMatches,
                softPenaltyMatches, softRewardMatches, completeness, primeOccupied, primeCapacity,
                assignedPrivate, demand);
    }

    private static boolean fullyAssigned(Lesson lesson) {
        return lesson.getTimeslot() != null && lesson.getRoom() != null
                && (!lesson.isPrivate() || lesson.getStudent() != null);
    }

    private static void printAggregatedSummary(List<Dataset> datasets, List<Result> results) {
        System.out.println();
        System.out.println("=== Multi-Seed Benchmark Summary (Mean +/- Std across " + SEEDS.length + " seeds) ===");
        String header = String.format(Locale.ROOT,
                "| %-7s | %-9s | %15s | %12s | %18s | %14s | %14s | %14s | %18s |",
                "Dataset", "Method", "Time (ms)", "Hard Score", "Soft Score",
                "Completeness", "Prime Occup.", "Prime Cap.", "Private / Demand");
        String sep = "-".repeat(header.length());
        System.out.println(sep);
        System.out.println(header);
        System.out.println(sep);

        for (Dataset ds : datasets) {
            for (String algo : List.of("First-Fit", "Timefold")) {
                List<Result> sub = results.stream()
                        .filter(r -> r.dataset().equals(ds.name()) && r.algorithm().equals(algo))
                        .toList();

                double meanMs = sub.stream().mapToLong(Result::millis).average().orElse(0);
                double stdMs = std(sub.stream().mapToDouble(Result::millis).toArray(), meanMs);

                double meanHard = sub.stream().mapToInt(r -> r.score().hardScore()).average().orElse(0);
                double meanSoft = sub.stream().mapToInt(r -> r.score().softScore()).average().orElse(0);
                double stdSoft = std(sub.stream().mapToDouble(r -> r.score().softScore()).toArray(), meanSoft);

                double meanComp = sub.stream().mapToDouble(Result::completeness).average().orElse(0);
                double meanOcc = sub.stream().mapToDouble(Result::primeOccupied).average().orElse(0);
                double meanCap = sub.stream().mapToDouble(Result::primeCapacity).average().orElse(0);
                double meanPriv = sub.stream().mapToLong(Result::assignedPrivate).average().orElse(0);
                long demand = sub.get(0).demand();

                System.out.println(String.format(Locale.ROOT,
                        "| %-7s | %-9s | %7.0f +/- %-4.0f | %12.1f | %9.0f +/- %-5.0f | %13.1f%% | %13.1f%% | %13.1f%% | %5.1f/%-3d (%4.1f%%) |",
                        ds.name(), algo, meanMs, stdMs, meanHard, meanSoft, stdSoft,
                        meanComp, meanOcc, meanCap, meanPriv, demand, 100.0 * meanPriv / demand));
            }
            System.out.println(sep);
        }
        System.out.println();
    }

    private static double std(double[] values, double mean) {
        if (values.length <= 1) return 0.0;
        double sumSq = 0.0;
        for (double v : values) {
            sumSq += (v - mean) * (v - mean);
        }
        return Math.sqrt(sumSq / (values.length - 1));
    }
}