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
import com.timetable.backend.config.SolverWeightsConfig;
import com.timetable.backend.domain.dto.SolverSettingsValues;
import com.timetable.backend.domain.model.AbstractUser;
import com.timetable.backend.domain.model.AvailabilityRole;
import com.timetable.backend.domain.model.DanceGroup;
import com.timetable.backend.domain.model.Lesson;
import com.timetable.backend.domain.model.ResourceUnavailability;
import com.timetable.backend.domain.model.Role;
import com.timetable.backend.domain.model.Room;
import com.timetable.backend.domain.model.Student;
import com.timetable.backend.domain.model.Teacher;
import com.timetable.backend.domain.model.Timeslot;
import com.timetable.backend.domain.model.WeeklyAvailability;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Empirical benchmark suite comparing Timefold Solver (Construction Heuristic + Tabu Search)
 * against a deterministic Greedy First-Fit baseline across four realistic dance-school scenarios,
 * plus a sensitivity analysis of calibrated soft-constraint weight presets.
 */
class SolverBenchmarkEvaluationTest {

    private static final int STEP_LIMIT = 2500;
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final LocalTime PRIME_START = LocalTime.of(16, 0);
    private static final LocalTime PRIME_END = LocalTime.of(21, 0);
    private static final LocalDate SCHEDULE_START = LocalDate.of(2026, 11, 2);

    private record Dataset(
            String code,
            String label,
            int teachers,
            int students,
            int groups,
            int privateLessons,
            int startHour,
            int slotsPerDay,
            boolean dualRole,
            int teacherWeeklyTarget
    ) {}

    private record BenchmarkMetrics(
            String datasetCode,
            String datasetLabel,
            String algorithm,
            long runtimeMs,
            HardSoftScore score,
            int hardViolations,
            long assignedTotal,
            long totalLessons,
            double completenessPct,
            long assignedPrivate,
            long privateTemplates,
            long studentDemand,
            double primeLessonsPct,
            double primeRoomCapacityPct,
            long teacherIdleGapMinutes,
            double workloadStdDev
    ) {}

    @Test
    void compareTimefoldWithGreedyFirstFitAcrossStudioScales() {
        SolverConfig config = new SolverConfig()
                .withEnvironmentMode(EnvironmentMode.REPRODUCIBLE)
                .withRandomSeed(42L)
                .withSolutionClass(DanceSchedule.class)
                .withEntityClasses(Lesson.class)
                .withConstraintProviderClass(BenchmarkConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig().withSpentLimit(TIMEOUT))
                .withPhases(
                        new ConstructionHeuristicPhaseConfig(),
                        new LocalSearchPhaseConfig()
                                .withAcceptorConfig(new LocalSearchAcceptorConfig().withEntityTabuSize(5))
                                .withForagerConfig(new LocalSearchForagerConfig().withAcceptedCountLimit(20))
                                .withTerminationConfig(new TerminationConfig().withStepCountLimit(STEP_LIMIT))
                );

        SolverFactory<DanceSchedule> factory = SolverFactory.create(config);
        SolutionManager<DanceSchedule, HardSoftScore> manager = SolutionManager.create(factory);

        List<Dataset> datasets = List.of(
                new Dataset("A", "Small Studio (3T / 15S / 21L)", 3, 15, 6, 15, 13, 8, false, 8),
                new Dataset("B", "Medium Studio + Dual-Role (5T / 32S / 48L)", 5, 32, 10, 38, 12, 9, true, 10),
                new Dataset("C", "Large Multi-Teacher Studio (8T / 55S / 80L)", 8, 55, 15, 65, 11, 10, true, 11),
                new Dataset("D", "Overload Stress Test (4T / 25S / 76L, Tight Room)", 4, 25, 16, 60, 13, 8, false, 15)
        );

        List<BenchmarkMetrics> allResults = new ArrayList<>();

        for (Dataset dataset : datasets) {
            DanceSchedule baselineInput = buildProblem(dataset);
            DanceSchedule solverInput = buildProblem(dataset);
            assertIndependentEqualInputs(dataset, baselineInput, solverInput);

            long t0 = System.nanoTime();
            firstFit(baselineInput);
            long baselineMs = Duration.ofNanos(System.nanoTime() - t0).toMillis();
            BenchmarkMetrics baseline = evaluate(dataset, "First-Fit", baselineMs, baselineInput, manager);
            allResults.add(baseline);

            Solver<DanceSchedule> solver = factory.buildSolver();
            t0 = System.nanoTime();
            DanceSchedule solved = solver.solve(solverInput);
            long solverMs = Duration.ofNanos(System.nanoTime() - t0).toMillis();
            BenchmarkMetrics timefold = evaluate(dataset, "Timefold", solverMs, solved, manager);
            allResults.add(timefold);
        }

        System.out.println("\n==================== BENCHMARK: GREEDY FIRST-FIT VS TIMEFOLD SOLVER ====================");
        System.out.println("Dataset | Algorithm | Time(ms) | Score           | Completeness | Private Matched | Demand Fulfilled | Prime Lessons | Prime Cap | Teacher Gaps | Load StdDev");
        for (BenchmarkMetrics row : allResults) {
            printRow(row);
        }

        for (int i = 0; i < allResults.size(); i += 2) {
            BenchmarkMetrics baseline = allResults.get(i);
            BenchmarkMetrics timefold = allResults.get(i + 1);

            assertThat(baseline.score().hardScore())
                    .as("%s First-Fit hard score", baseline.datasetCode())
                    .isZero();
            assertThat(timefold.score().hardScore())
                    .as("%s Timefold hard score", timefold.datasetCode())
                    .isZero();

            assertThat(timefold.assignedPrivate())
                    .as("%s Timefold matched private lessons vs First-Fit", timefold.datasetCode())
                    .isGreaterThanOrEqualTo(baseline.assignedPrivate());

            if (!timefold.datasetCode().equals("A")) {
                assertThat(timefold.assignedPrivate())
                        .as("%s Timefold matched private lessons strictly exceeds First-Fit", timefold.datasetCode())
                        .isGreaterThan(baseline.assignedPrivate());
            }

            assertThat(timefold.score().softScore())
                    .as("%s Timefold soft score vs First-Fit", timefold.datasetCode())
                    .isGreaterThan(baseline.score().softScore());
            assertThat(timefold.primeLessonsPct())
                    .as("%s Timefold prime-time share vs First-Fit", timefold.datasetCode())
                    .isGreaterThan(baseline.primeLessonsPct());
        }
    }

    @Test
    void evaluateCalibratedWeightPresetsTradeoffs() {
        Dataset mediumStudio = new Dataset("B", "Medium Studio + Dual-Role (5T / 32S / 48L)", 5, 32, 10, 38, 12, 9, true, 10);

        Map<String, SolverSettingsValues> presets = new LinkedHashMap<>();
        presets.put("1_Legacy_Unbalanced", new SolverSettingsValues(
                10, 0, 6, 15, 1, 1, 1, 1, 1, 100, 10, 50, 30, 1, 1));
        presets.put("2_Calibrated_Balanced", new SolverSettingsValues(
                10, 0, 6, 15, 1, 1, 1, 1, 1, 60, 15, 35, 25, 8, 3));
        presets.put("3_Teacher_Comfort", new SolverSettingsValues(
                10, 0, 6, 15, 1, 1, 1, 1, 1, 60, 5, 30, 25, 25, 4));
        presets.put("4_PrimeTime_Focus", new SolverSettingsValues(
                10, 0, 6, 15, 1, 1, 1, 1, 1, 60, 35, 35, 20, 3, 2));

        List<String> rows = new ArrayList<>();

        for (var entry : presets.entrySet()) {
            String presetName = entry.getKey();
            SolverSettingsValues settings = entry.getValue();

            SolverConfig config = new SolverConfig()
                    .withEnvironmentMode(EnvironmentMode.REPRODUCIBLE)
                    .withRandomSeed(42L)
                    .withSolutionClass(DanceSchedule.class)
                    .withEntityClasses(Lesson.class)
                    .withConstraintProviderClass(DanceScheduleConstraintProvider.class)
                    .withTerminationConfig(new TerminationConfig().withSpentLimit(TIMEOUT))
                    .withPhases(
                            new ConstructionHeuristicPhaseConfig(),
                            new LocalSearchPhaseConfig()
                                    .withAcceptorConfig(new LocalSearchAcceptorConfig().withEntityTabuSize(5))
                                    .withForagerConfig(new LocalSearchForagerConfig().withAcceptedCountLimit(20))
                                    .withTerminationConfig(new TerminationConfig().withStepCountLimit(STEP_LIMIT))
                    );

            SolverFactory<DanceSchedule> factory = SolverWeightsConfig.withFactoryWeights(
                    settings, () -> SolverFactory.create(config));
            SolutionManager<DanceSchedule, HardSoftScore> manager = SolutionManager.create(factory);

            DanceSchedule problem = buildProblem(mediumStudio);
            DanceSchedule solved = factory.buildSolver().solve(problem);
            BenchmarkMetrics metrics = evaluate(mediumStudio, presetName, 0L, solved, manager);
            ScoreExplanation<DanceSchedule, HardSoftScore> explanation = manager.explain(solved);

            long totalAbsSoft = explanation.getConstraintMatchTotalMap().values().stream()
                    .mapToLong(t -> Math.abs(t.getScore().softScore()))
                    .sum();
            long assignmentSoft = explanation.getConstraintMatchTotalMap().values().stream()
                    .filter(t -> t.getConstraintRef().constraintName().equals("Reward for assigning a student to a private lesson"))
                    .mapToLong(t -> Math.abs(t.getScore().softScore()))
                    .sum();
            double assignmentShare = totalAbsSoft == 0 ? 0.0 : 100.0 * assignmentSoft / totalAbsSoft;

            rows.add(String.format(Locale.ROOT,
                    "%-23s | %-15s | %2d/%-2d   | %14.1f%% | %18d | %11.2f | %17.1f%%",
                    presetName,
                    metrics.score(),
                    metrics.assignedPrivate(),
                    metrics.privateTemplates(),
                    metrics.primeLessonsPct(),
                    metrics.teacherIdleGapMinutes(),
                    metrics.workloadStdDev(),
                    assignmentShare));

            assertThat(metrics.score().hardScore()).isZero();
        }

        System.out.println("\n==================== WEIGHT PRESET SENSITIVITY ANALYSIS (DATASET B) ====================");
        System.out.println("Preset                  | Score           | Matched | Prime Lessons % | Teacher Gaps (min) | Load StdDev | Assignment Share %");
        rows.forEach(System.out::println);
    }

    private static DanceSchedule buildProblem(Dataset dataset) {
        List<Timeslot> slots = new ArrayList<>();
        for (int day = 1; day <= 5; day++) {
            for (int hour = 0; hour < dataset.slotsPerDay(); hour++) {
                LocalTime start = LocalTime.of(dataset.startHour() + hour, 0);
                slots.add(new Timeslot((long) slots.size() + 1, DayOfWeek.of(day), start, start.plusHours(1)));
            }
        }

        List<Student> students = new ArrayList<>();
        for (int i = 0; i < dataset.students(); i++) {
            Student student = new Student();
            boolean isDualUser = dataset.dualRole() && i < 2 && i < dataset.teachers();
            student.setId(isDualUser ? (long) i + 1 : 1000L + i);
            student.setFullName(isDualUser ? "DualUser " + (i + 1) : "Student " + (i + 1));
            student.setDesiredLessonsPerWeek(2);
            students.add(student);
        }

        List<Teacher> teachers = new ArrayList<>();
        int broadTeacherCount = Math.max(1, dataset.teachers() / 2);

        for (int i = 0; i < dataset.teachers(); i++) {
            boolean isDualUser = dataset.dualRole() && i < 2 && i < dataset.students();
            AbstractUser user = isDualUser ? students.get(i) : new AbstractUser();
            user.setId((long) i + 1);
            user.setFullName(isDualUser ? "DualUser " + (i + 1) : "Teacher " + (i + 1));
            if (isDualUser) {
                user.setRole(new Role(1L, "TEACHER"));
                user.addRole(new Role(2L, "STUDENT"));
            }

            Teacher teacher = new Teacher();
            teacher.setId(user.getId());
            teacher.setUser(user);
            teacher.setMaxDailyHours(6);
            teacher.setDesiredLessonsPerWeek(dataset.teacherWeeklyTarget());

            // Broad teachers (first half) can teach a wide student pool, whereas specialist
            // teachers (second half) depend on smaller overlapping student cohorts.
            // Greedy First-Fit assigns broad teachers first and starves specialist teachers.
            Set<Student> subscribers = new LinkedHashSet<>();
            if (i < broadTeacherCount) {
                int broadLimit = Math.min(students.size(), Math.max(10, (int) Math.ceil(students.size() * 0.75)));
                for (int k = 0; k < broadLimit; k++) {
                    Student candidate = students.get(k);
                    if (!candidate.getId().equals(teacher.getId())) {
                        subscribers.add(candidate);
                    }
                }
            } else {
                int specialistIndex = i - broadTeacherCount;
                int startIdx = Math.min(students.size() - 1, 2 + specialistIndex * 2);
                int nicheSize = Math.min(6, students.size() - startIdx);
                for (int k = 0; k < nicheSize; k++) {
                    Student candidate = students.get(startIdx + k);
                    if (!candidate.getId().equals(teacher.getId())) {
                        subscribers.add(candidate);
                    }
                }
            }
            teacher.setPrivateStudents(subscribers);
            teachers.add(teacher);
        }

        List<WeeklyAvailability> availability = new ArrayList<>();
        long availId = 1L;
        for (Teacher teacher : teachers) {
            for (int day = 1; day <= 5; day++) {
                availability.add(new WeeklyAvailability(
                        availId++, teacher.getUser(), DayOfWeek.of(day),
                        LocalTime.of(dataset.startHour(), 0),
                        LocalTime.of(dataset.startHour() + dataset.slotsPerDay(), 0),
                        AvailabilityRole.TEACHER));
            }
        }

        // Heterogeneous student availability windows:
        // Cohort 0: full day, Cohort 1: afternoon/evening (15:00+), Cohort 2: prime-time only (16:00+)
        for (int i = 0; i < students.size(); i++) {
            Student student = students.get(i);
            int cohort = i % 3;
            LocalTime windowStart = switch (cohort) {
                case 0 -> LocalTime.of(dataset.startHour(), 0);
                case 1 -> LocalTime.of(Math.max(dataset.startHour(), 15), 0);
                default -> LocalTime.of(Math.max(dataset.startHour(), 16), 0);
            };
            LocalTime windowEnd = LocalTime.of(dataset.startHour() + dataset.slotsPerDay(), 0);
            for (int day = 1; day <= 5; day++) {
                if (cohort == 2 && day == 3) {
                    continue; // Evening-only students unavailable on Wednesday
                }
                availability.add(new WeeklyAvailability(
                        availId++, student, DayOfWeek.of(day),
                        windowStart, windowEnd, AvailabilityRole.STUDENT));
            }
        }

        // Add realistic one-time unavailability exceptions for medium & large studios
        List<ResourceUnavailability> exceptions = new ArrayList<>();
        if (dataset.code().equals("B") || dataset.code().equals("C")) {
            exceptions.add(new ResourceUnavailability(
                    1L, teachers.get(0).getUser(), SCHEDULE_START.plusDays(1),
                    LocalTime.of(16, 0), LocalTime.of(18, 0), "External workshop", AvailabilityRole.TEACHER));
            exceptions.add(new ResourceUnavailability(
                    2L, students.get(2), SCHEDULE_START.plusDays(3),
                    LocalTime.of(17, 0), LocalTime.of(19, 0), "Family event", AvailabilityRole.STUDENT));
        }

        Room room = new Room(1L, "Main Studio Hall", 4, true);
        List<Lesson> lessons = new ArrayList<>();

        // Pinned group lessons distributed across prime & afternoon hours, blocking the single room
        for (int i = 0; i < dataset.groups(); i++) {
            DanceGroup group = new DanceGroup();
            group.setId((long) i + 1);
            group.setName("Group " + (i + 1));
            Teacher assignedTeacher = teachers.get(i % teachers.size());

            // Enroll dual-role users in groups taught by OTHER teachers (never their own group)
            if (dataset.dualRole() && (i == 2 || i == 3)) {
                int dualStudentIdx = i - 2;
                if (dualStudentIdx < students.size()
                        && !assignedTeacher.getId().equals(students.get(dualStudentIdx).getId())) {
                    group.setEnrolledStudents(new LinkedHashSet<>(List.of(students.get(dualStudentIdx))));
                }
            }

            int dayIndex = i % 5;
            int slotOffsetInDay = Math.min(dataset.slotsPerDay() - 1, Math.max(0, dataset.slotsPerDay() - 4 + (i / 5)));
            Timeslot groupSlot = slots.get(dayIndex * dataset.slotsPerDay() + slotOffsetInDay);

            Lesson lesson = new Lesson(assignedTeacher, group, 60, false, true);
            lesson.setId((long) lessons.size() + 1);
            lesson.setPinned(true);
            lesson.setTimeslot(groupSlot);
            lesson.setRoom(room);
            lessons.add(lesson);
        }

        // Unpinned private templates grouped by teacher (matching DB load order)
        for (int i = 0; i < dataset.privateLessons(); i++) {
            Teacher teacher = teachers.get(i % teachers.size());
            Lesson lesson = new Lesson(teacher, (Student) null, 60, true);
            lesson.setId((long) lessons.size() + 1);
            lesson.setRoom(room);
            lessons.add(lesson);
        }
        lessons.subList(dataset.groups(), lessons.size())
                .sort(Comparator.comparing((Lesson l) -> l.getTeacher().getId()).thenComparing(Lesson::getId));

        return new DanceSchedule(
                1L, slots, teachers, exceptions, availability,
                List.of(SCHEDULE_START), students, lessons
        );
    }

    /**
     * Deterministic Greedy First-Fit baseline:
     * Iterates over unpinned private templates in teacher order and assigns the first
     * hard-feasible (timeslot, student) pair without backtracking or global soft optimization.
     */
    private static void firstFit(DanceSchedule schedule) {
        for (Lesson lesson : schedule.getLessonList()) {
            if (!lesson.isPrivate() || lesson.isPinned()) {
                continue;
            }
            for (Timeslot slot : schedule.getTimeslotList()) {
                if (!canPlaceTeacherAndRoom(schedule, lesson, slot)) {
                    continue;
                }
                Student matchedStudent = null;
                for (Student student : lesson.getTeacher().getPrivateStudents()) {
                    if (canStudentAttend(schedule, lesson, student, slot)) {
                        matchedStudent = student;
                        break;
                    }
                }
                if (matchedStudent != null) {
                    lesson.setTimeslot(slot);
                    lesson.setStudent(matchedStudent);
                    break;
                }
            }
        }
    }

    private static boolean canPlaceTeacherAndRoom(DanceSchedule schedule, Lesson candidate, Timeslot slot) {
        Teacher teacher = candidate.getTeacher();
        if (!isWithinWeeklyAvailability(schedule, teacher.getUser(), AvailabilityRole.TEACHER, slot)) {
            return false;
        }
        if (hasOneTimeUnavailability(schedule, teacher.getUser(), AvailabilityRole.TEACHER, slot)) {
            return false;
        }

        int simultaneousPrivate = 0;
        int teacherDayCount = 0;
        int teacherWeekCount = 0;

        for (Lesson other : schedule.getLessonList()) {
            if (other == candidate || !isActivelyScheduled(other)) {
                continue;
            }
            if (other.getTeacher().getId().equals(teacher.getId())) {
                teacherWeekCount++;
                if (other.getTimeslot().getDayOfWeek() == slot.getDayOfWeek()) {
                    teacherDayCount++;
                }
            }
            if (overlaps(slot, other.getTimeslot())) {
                if (other.getTeacher().getId().equals(teacher.getId())) {
                    return false;
                }
                if (!other.isPrivate()) {
                    return false; // Group lesson occupies entire room
                }
                simultaneousPrivate++;
            }
            // Dual-role check: teacher attending another lesson as student at this time
            if (overlaps(slot, other.getTimeslot()) && attendsAsStudent(other, teacher.getId())) {
                return false;
            }
        }

        int dailyCap = teacher.getMaxDailyHours() > 0 ? Math.min(teacher.getMaxDailyHours(), 6) : 6;
        if (teacherDayCount >= dailyCap) {
            return false;
        }
        if (teacher.getDesiredLessonsPerWeek() != null && teacherWeekCount >= teacher.getDesiredLessonsPerWeek()) {
            return false;
        }
        return simultaneousPrivate < 4;
    }

    private static boolean canStudentAttend(DanceSchedule schedule, Lesson candidate, Student student, Timeslot slot) {
        if (candidate.getTeacher().getId().equals(student.getId())) {
            return false;
        }
        if (!isWithinWeeklyAvailability(schedule, student, AvailabilityRole.STUDENT, slot)) {
            return false;
        }
        if (hasOneTimeUnavailability(schedule, student, AvailabilityRole.STUDENT, slot)) {
            return false;
        }

        int studentWeekCount = 0;
        for (Lesson other : schedule.getLessonList()) {
            if (!isActivelyScheduled(other)) {
                continue;
            }
            if (other.isPrivate() && other.getStudent() != null && other.getStudent().getId().equals(student.getId())) {
                studentWeekCount++;
                if (other.getTimeslot().getDayOfWeek() == slot.getDayOfWeek()) {
                    return false; // Max 1 private lesson per day
                }
            }
            if (overlaps(slot, other.getTimeslot())) {
                if (attendsAsStudent(other, student.getId())) {
                    return false;
                }
                if (other.getTeacher().getId().equals(student.getId())) {
                    return false; // Dual-role user teaching at this time
                }
            }
        }

        return student.getDesiredLessonsPerWeek() == null || studentWeekCount < student.getDesiredLessonsPerWeek();
    }

    private static boolean isWithinWeeklyAvailability(
            DanceSchedule schedule, AbstractUser user, AvailabilityRole role, Timeslot slot) {
        return schedule.getWeeklyAvailabilityList().stream().anyMatch(wa ->
                wa.getUser().getId().equals(user.getId())
                        && wa.getAvailabilityRole() == role
                        && wa.getDayOfWeek() == slot.getDayOfWeek()
                        && !slot.getStartTime().isBefore(wa.getStartTime())
                        && !slot.getEndTime().isAfter(wa.getEndTime()));
    }

    private static boolean hasOneTimeUnavailability(
            DanceSchedule schedule, AbstractUser user, AvailabilityRole role, Timeslot slot) {
        LocalDate anchor = schedule.getScheduleDateAnchorList().get(0);
        int shift = slot.getDayOfWeek().getValue() - anchor.getDayOfWeek().getValue();
        if (shift < 0) {
            shift += 7;
        }
        LocalDate lessonDate = anchor.plusDays(shift);

        return schedule.getResourceUnavailabilityList().stream().anyMatch(ru ->
                ru.getUser().getId().equals(user.getId())
                        && ru.getAvailabilityRole() == role
                        && ru.getDate().equals(lessonDate)
                        && slot.getStartTime().isBefore(ru.getEndTime())
                        && ru.getStartTime().isBefore(slot.getEndTime()));
    }

    private static boolean attendsAsStudent(Lesson lesson, Long userId) {
        if (lesson.getStudent() != null && userId.equals(lesson.getStudent().getId())) {
            return true;
        }
        return lesson.getDanceGroup() != null
                && lesson.getDanceGroup().getEnrolledStudents() != null
                && lesson.getDanceGroup().getEnrolledStudents().stream()
                .anyMatch(s -> userId.equals(s.getId()));
    }

    private static boolean isActivelyScheduled(Lesson lesson) {
        return lesson.getTimeslot() != null && (!lesson.isPrivate() || lesson.getStudent() != null);
    }

    private static boolean overlaps(Timeslot first, Timeslot second) {
        return first.getDayOfWeek() == second.getDayOfWeek()
                && first.getStartTime().isBefore(second.getEndTime())
                && second.getStartTime().isBefore(first.getEndTime());
    }

    private static BenchmarkMetrics evaluate(
            Dataset dataset,
            String algorithm,
            long runtimeMs,
            DanceSchedule schedule,
            SolutionManager<DanceSchedule, HardSoftScore> manager
    ) {
        ScoreExplanation<DanceSchedule, HardSoftScore> explanation = manager.explain(schedule);
        int hardViolations = 0;
        for (var total : explanation.getConstraintMatchTotalMap().values()) {
            for (var match : total.getConstraintMatchSet()) {
                if (match.getScore().hardScore() < 0) {
                    hardViolations++;
                }
            }
        }

        List<Lesson> scheduled = schedule.getLessonList().stream()
                .filter(SolverBenchmarkEvaluationTest::isActivelyScheduled)
                .toList();
        long totalLessons = schedule.getLessonList().size();
        double completenessPct = totalLessons == 0 ? 100.0 : 100.0 * scheduled.size() / totalLessons;

        long assignedPrivate = scheduled.stream().filter(Lesson::isPrivate).count();
        long studentDemand = schedule.getStudentList().stream()
                .map(Student::getDesiredLessonsPerWeek)
                .filter(v -> v != null)
                .mapToLong(Integer::longValue)
                .sum();

        long primeScheduledCount = scheduled.stream()
                .filter(l -> !l.getTimeslot().getStartTime().isBefore(PRIME_START)
                        && l.getTimeslot().getStartTime().isBefore(PRIME_END))
                .count();
        double primeLessonsPct = scheduled.isEmpty() ? 0.0 : 100.0 * primeScheduledCount / scheduled.size();

        Set<DayOfWeek> days = new LinkedHashSet<>();
        schedule.getTimeslotList().forEach(s -> days.add(s.getDayOfWeek()));
        int capacityQuarters = 0;
        for (DayOfWeek day : days) {
            for (LocalTime start = PRIME_START; start.isBefore(PRIME_END); start = start.plusMinutes(30)) {
                LocalTime end = start.plusMinutes(30);
                int quarters = 0;
                for (Lesson lesson : scheduled) {
                    Timeslot ts = lesson.getTimeslot();
                    if (ts.getDayOfWeek() == day && ts.getStartTime().isBefore(end) && start.isBefore(ts.getEndTime())) {
                        quarters += lesson.isPrivate() ? 1 : 4;
                    }
                }
                capacityQuarters += Math.min(4, quarters);
            }
        }
        int totalPrimeHalfHours = days.size() * 10;
        double primeRoomCapacityPct = totalPrimeHalfHours == 0
                ? 0.0
                : 100.0 * capacityQuarters / (totalPrimeHalfHours * 4.0);

        long teacherIdleGapMinutes = computeTotalTeacherIdleGapMinutes(schedule, scheduled);
        double workloadStdDev = computeTeacherWorkloadStdDev(schedule, scheduled);

        return new BenchmarkMetrics(
                dataset.code(),
                dataset.label(),
                algorithm,
                runtimeMs,
                explanation.getScore(),
                hardViolations,
                scheduled.size(),
                totalLessons,
                completenessPct,
                assignedPrivate,
                dataset.privateLessons(),
                studentDemand,
                primeLessonsPct,
                primeRoomCapacityPct,
                teacherIdleGapMinutes,
                workloadStdDev
        );
    }

    private static long computeTotalTeacherIdleGapMinutes(DanceSchedule schedule, List<Lesson> scheduled) {
        long totalGapMinutes = 0;
        for (Teacher teacher : schedule.getTeacherList()) {
            for (DayOfWeek day : DayOfWeek.values()) {
                List<Lesson> dayLessons = scheduled.stream()
                        .filter(l -> l.getTeacher().getId().equals(teacher.getId())
                                && l.getTimeslot().getDayOfWeek() == day)
                        .sorted(Comparator.comparing(l -> l.getTimeslot().getStartTime()))
                        .toList();
                for (int i = 1; i < dayLessons.size(); i++) {
                    long gap = Duration.between(
                            dayLessons.get(i - 1).getTimeslot().getEndTime(),
                            dayLessons.get(i).getTimeslot().getStartTime()).toMinutes();
                    if (gap > 15) {
                        totalGapMinutes += gap;
                    }
                }
            }
        }
        return totalGapMinutes;
    }

    private static double computeTeacherWorkloadStdDev(DanceSchedule schedule, List<Lesson> scheduled) {
        int n = schedule.getTeacherList().size();
        if (n == 0) {
            return 0.0;
        }
        double[] counts = new double[n];
        double sum = 0.0;
        for (int i = 0; i < n; i++) {
            long teacherId = schedule.getTeacherList().get(i).getId();
            counts[i] = scheduled.stream().filter(l -> l.getTeacher().getId().equals(teacherId)).count();
            sum += counts[i];
        }
        double mean = sum / n;
        double variance = 0.0;
        for (double c : counts) {
            variance += (c - mean) * (c - mean);
        }
        return Math.sqrt(variance / n);
    }

    private static void assertIndependentEqualInputs(Dataset dataset, DanceSchedule first, DanceSchedule second) {
        assertThat(first).isNotSameAs(second);
        assertThat(first.getLessonList()).hasSize(dataset.groups() + dataset.privateLessons());
        assertThat(first.getTeacherList()).hasSize(dataset.teachers());
        assertThat(first.getStudentList()).hasSize(dataset.students());
        assertThat(first.getTimeslotList()).hasSize(dataset.slotsPerDay() * 5);
    }

    private static void printRow(BenchmarkMetrics m) {
        double demandPct = m.studentDemand() == 0 ? 100.0 : 100.0 * m.assignedPrivate() / m.studentDemand();
        System.out.printf(Locale.ROOT,
                "%-7s | %-9s | %8d | %-15s | %11.1f%% | %2d/%-12d | %15.1f%% | %12.1f%% | %8.1f%% | %9d min | %11.2f%n",
                m.datasetCode(),
                m.algorithm(),
                m.runtimeMs(),
                m.score(),
                m.completenessPct(),
                m.assignedPrivate(),
                m.privateTemplates(),
                demandPct,
                m.primeLessonsPct(),
                m.primeRoomCapacityPct(),
                m.teacherIdleGapMinutes(),
                m.workloadStdDev());
    }
}