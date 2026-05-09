package com.timetable.backend.solver;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import com.timetable.backend.domain.model.*;
import org.junit.jupiter.api.*;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Timefold Solver using a realistic dance school dataset.
 *
 * <p>These tests do NOT require a Spring context — they build the solver
 * programmatically via {@link SolverFactory}. The dataset simulates a real week:
 * 5 teachers, 15 students, 30 timeslots, 2 pinned group lessons and 15 private
 * lesson templates. The solver runs for up to {@value SOLVER_TIMEOUT_SECONDS}
 * seconds and must achieve 0 hard violations.</p>
 *
 * <p><b>Key metrics tracked for the presentation:</b>
 * <ul>
 *   <li>Solve time</li>
 *   <li>Hard violations (must be 0)</li>
 *   <li>Student assignment rate</li>
 *   <li>Prime-time slot utilization (16:00–21:00)</li>
 *   <li>Teacher workload distribution</li>
 * </ul>
 * </p>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("🚀 Solver Integration Tests — Realistic Dance School Benchmark")
class SolverIntegrationTest {

    private static final int SOLVER_TIMEOUT_SECONDS = 60;
    private static final LocalDate SCHEDULE_ANCHOR = LocalDate.of(2026, 5, 4); // Monday

    // ----------------------------------------------------------------
    // Shared state — built once per test class execution
    // ----------------------------------------------------------------
    private static SolverFactory<DanceSchedule> solverFactory;
    private static DanceSchedule solvedSchedule;

    @BeforeAll
    static void buildDatasetAndSolve() {
        solverFactory = buildSolverFactory();
        DanceSchedule problemDataset = buildRealisticProblem();

        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║       DANCE SCHOOL SOLVER — INTEGRATION TEST SUITE               ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.printf( "║  Dataset  : %d teachers | %d students | %d lessons | %d timeslots  ║%n",
                countTeachers(problemDataset), countStudents(problemDataset),
                problemDataset.getLessonList().size(), problemDataset.getTimeslotList().size());
        System.out.printf( "║  Private  : %d lessons to assign | Group (pinned): %d lessons       ║%n",
                countPrivate(problemDataset), countGroup(problemDataset));
        System.out.printf( "║  Timeout  : %d seconds                                            ║%n",
                SOLVER_TIMEOUT_SECONDS);
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.println("║  ⏳ Solver is running...                                          ║");

        long start = System.currentTimeMillis();
        Solver<DanceSchedule> solver = solverFactory.buildSolver();
        solvedSchedule = solver.solve(problemDataset);
        long elapsedMs = System.currentTimeMillis() - start;

        HardSoftScore score = solvedSchedule.getScore();
        long assignedStudents = solvedSchedule.getLessonList().stream()
                .filter(l -> l.isPrivate() && l.getStudent() != null)
                .count();
        long totalPrivate = countPrivate(solvedSchedule);
        long primeTimeCount = solvedSchedule.getLessonList().stream()
                .filter(SolverIntegrationTest::isInPrimeTime)
                .count();
        long totalScheduled = solvedSchedule.getLessonList().stream()
                .filter(l -> l.getTimeslot() != null)
                .count();

        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.printf( "║  ✅ Solved in: %-5d ms (~%.1f sec)                               ║%n",
                elapsedMs, elapsedMs / 1000.0);
        System.out.printf( "║  📊 Score    : %d hard / %+d soft                              ║%n",
                score.hardScore(), score.softScore());
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.printf( "║  🎓 Students assigned  : %2d / %2d (%.0f%%)                            ║%n",
                assignedStudents, totalPrivate, 100.0 * assignedStudents / totalPrivate);
        System.out.printf( "║  🌟 Prime-time lessons : %2d / %2d (%.0f%%)                            ║%n",
                primeTimeCount, totalScheduled, 100.0 * primeTimeCount / totalScheduled);
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        printTeacherWorkload(solvedSchedule);
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    // ================================================================
    //  TEST 1 — Zero hard violations
    // ================================================================

    @Test
    @Order(1)
    @DisplayName("✅ T1: Solver achieves ZERO hard violations on a 15-lesson realistic dataset")
    void solver_achievesZeroHardViolations() {
        HardSoftScore score = solvedSchedule.getScore();

        assertThat(score.hardScore())
                .as("Solver must satisfy ALL hard constraints — zero hard violations expected")
                .isEqualTo(0);
    }

    // ================================================================
    //  TEST 2 — Positive soft score (solver actually optimized)
    // ================================================================

    @Test
    @Order(2)
    @DisplayName("📈 T2: Solver produces a POSITIVE soft score (optimization confirmed)")
    void solver_producesPositiveSoftScore() {
        HardSoftScore score = solvedSchedule.getScore();

        assertThat(score.softScore())
                .as("Soft score must be positive — solver must have assigned students and utilized prime time")
                .isGreaterThan(0);
    }

    // ================================================================
    //  TEST 3 — All 15 students are assigned to private lessons
    // ================================================================

    @Test
    @Order(3)
    @DisplayName("🎓 T3: All 15 students are matched to private lesson slots")
    void solver_assignsAllStudents_toPrivateLessons() {
        long assigned = solvedSchedule.getLessonList().stream()
                .filter(l -> l.isPrivate() && l.getStudent() != null)
                .count();

        long total = countPrivate(solvedSchedule);

        assertThat(assigned)
                .as("Every private lesson must have a student assigned when constraints allow")
                .isEqualTo(total);
    }

    // ================================================================
    //  TEST 4 — No teacher double-booked in the solution
    // ================================================================

    @Test
    @Order(4)
    @DisplayName("👩‍🏫 T4: No teacher teaches two lessons at the same time (conflict-free)")
    void solver_ensuresNoTeacherConflict_inFinalSolution() {
        List<Lesson> scheduled = solvedSchedule.getLessonList().stream()
                .filter(l -> l.getTimeslot() != null)
                .toList();

        for (Lesson a : scheduled) {
            for (Lesson b : scheduled) {
                if (a.getId() < b.getId()
                        && a.getTeacher().equals(b.getTeacher())
                        && a.getTimeslot().equals(b.getTimeslot())) {
                    Assertions.fail(String.format(
                            "Teacher conflict detected! Teacher [%s] has TWO lessons at [%s %s-%s]: lesson#%d and lesson#%d",
                            a.getTeacher().getId(),
                            a.getTimeslot().getDayOfWeek(),
                            a.getTimeslot().getStartTime(),
                            a.getTimeslot().getEndTime(),
                            a.getId(), b.getId()));
                }
            }
        }
    }

    // ================================================================
    //  TEST 5 — Private lessons respect student availability (Mon/Wed/Fri 16-20)
    // ================================================================

    @Test
    @Order(5)
    @DisplayName("📅 T5: All private lessons fall within student weekly availability windows")
    void solver_respectsStudentWeeklyAvailability() {
        Set<DayOfWeek> allowedDays = Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
        LocalTime windowStart = LocalTime.of(16, 0);
        LocalTime windowEnd   = LocalTime.of(20, 0);

        List<Lesson> violations = solvedSchedule.getLessonList().stream()
                .filter(l -> l.isPrivate() && l.getStudent() != null && l.getTimeslot() != null)
                .filter(l -> !allowedDays.contains(l.getTimeslot().getDayOfWeek())
                        || l.getTimeslot().getStartTime().isBefore(windowStart)
                        || l.getTimeslot().getEndTime().isAfter(windowEnd))
                .toList();

        assertThat(violations)
                .as("Private lessons must be within student availability: Mon/Wed/Fri 16:00–20:00")
                .isEmpty();
    }

    // ================================================================
    //  TEST 6 — No private lessons overlap with group lesson timeslots
    // ================================================================

    @Test
    @Order(6)
    @DisplayName("🚫 T6: No private lessons are scheduled during group lesson timeslots")
    void solver_ensuresNoPrivateDuringGroupLesson() {
        Set<Timeslot> groupTimeslots = solvedSchedule.getLessonList().stream()
                .filter(l -> !l.isPrivate() && l.getTimeslot() != null)
                .map(Lesson::getTimeslot)
                .collect(Collectors.toSet());

        List<Lesson> conflicts = solvedSchedule.getLessonList().stream()
                .filter(l -> l.isPrivate() && l.getTimeslot() != null)
                .filter(l -> groupTimeslots.contains(l.getTimeslot()))
                .toList();

        assertThat(conflicts)
                .as("The room is fully occupied by group lessons — no private lessons allowed in those timeslots")
                .isEmpty();
    }

    // ================================================================
    //  TEST 7 — At most 4 private lessons per timeslot (room capacity)
    // ================================================================

    @Test
    @Order(7)
    @DisplayName("🏠 T7: Room capacity respected — at most 4 private lessons per timeslot")
    void solver_respectsRoomCapacity_maxFourPrivatePerTimeslot() {
        Map<Timeslot, Long> privatePerTimeslot = solvedSchedule.getLessonList().stream()
                .filter(l -> l.isPrivate() && l.getTimeslot() != null)
                .collect(Collectors.groupingBy(Lesson::getTimeslot, Collectors.counting()));

        privatePerTimeslot.forEach((timeslot, count) ->
                assertThat(count)
                        .as("Timeslot [%s %s-%s] must have at most 4 private lessons (room capacity)",
                                timeslot.getDayOfWeek(), timeslot.getStartTime(), timeslot.getEndTime())
                        .isLessThanOrEqualTo(4L)
        );
    }

    // ================================================================
    //  TEST 8 — Solver improves score compared to a random (violated) assignment
    // ================================================================

    @Test
    @Order(8)
    @DisplayName("⚡ T8: Solver score is strictly better than a naive violated assignment")
    void solver_outperformsNaiveAssignment() {
        // Build the same problem but manually assign all lessons to the SAME timeslot
        // → massive teacher conflicts + room overflow → terrible score
        DanceSchedule naiveProblem = buildRealisticProblem();
        Timeslot firstTimeslot = naiveProblem.getTimeslotList().getFirst();

        naiveProblem.getLessonList().forEach(lesson -> {
            if (!lesson.isPinned()) {
                lesson.setTimeslot(firstTimeslot);
            }
        });

        SolutionManager<DanceSchedule, HardSoftScore> solutionManager =
                SolutionManager.create(solverFactory);
        HardSoftScore naiveScore = solutionManager.update(naiveProblem);

        System.out.printf("%n  📉 Naive Assignment Score : %d hard / %+d soft%n",
                naiveScore.hardScore(), naiveScore.softScore());
        System.out.printf("  📈 Solver Solution Score  : %d hard / %+d soft%n%n",
                solvedSchedule.getScore().hardScore(), solvedSchedule.getScore().softScore());

        assertThat(solvedSchedule.getScore().hardScore())
                .as("Solver hard score MUST be better than the naive assignment")
                .isGreaterThan(naiveScore.hardScore());
    }

    // ================================================================
    //  TEST 9 — Students are NOT double-booked (no student conflict)
    // ================================================================

    @Test
    @Order(9)
    @DisplayName("🎯 T9: No student is scheduled for two lessons at the same time")
    void solver_ensuresNoStudentDoubleBooking() {
        List<Lesson> assigned = solvedSchedule.getLessonList().stream()
                .filter(l -> l.isPrivate() && l.getStudent() != null && l.getTimeslot() != null)
                .toList();

        for (Lesson a : assigned) {
            for (Lesson b : assigned) {
                if (a.getId() < b.getId()
                        && Objects.equals(a.getStudent(), b.getStudent())
                        && Objects.equals(a.getTimeslot(), b.getTimeslot())) {
                    Assertions.fail(String.format(
                            "Student [%s] is double-booked at [%s %s-%s]",
                            a.getStudent().getFullName(),
                            a.getTimeslot().getDayOfWeek(),
                            a.getTimeslot().getStartTime(),
                            a.getTimeslot().getEndTime()));
                }
            }
        }
    }

    // ================================================================
    //  TEST 10 — Every student is matched to their subscribed teacher only
    // ================================================================

    @Test
    @Order(10)
    @DisplayName("🔗 T10: Every student is only matched to their subscribed teacher")
    void solver_onlyMatchesStudentsToSubscribedTeacher() {
        List<Lesson> violations = solvedSchedule.getLessonList().stream()
                .filter(l -> l.isPrivate() && l.getStudent() != null)
                .filter(l -> !l.getTeacher().getPrivateStudents().contains(l.getStudent()))
                .toList();

        assertThat(violations)
                .as("Students can only attend private lessons with teachers they are subscribed to")
                .isEmpty();
    }

    // ================================================================
    //  PROBLEM BUILDER
    // ================================================================

    /**
     * Builds a realistic daily dataset for a dance school.
     *
     * <p>Structure:
     * <ul>
     *   <li>5 teachers (Ana, Boris, Clara, David, Elena)</li>
     *   <li>15 students — 3 per teacher, each student subscribed only to one teacher</li>
     *   <li>30 timeslots — Mon–Fri, 3 morning (9–12) + 3 evening (16–19) = 6/day × 5 days</li>
     *   <li>2 group lessons (pinned): Mon 9:00 and Wed 9:00 — these block the room</li>
     *   <li>15 private lessons — 3 per teacher, timeslot=null (solver assigns them)</li>
     *   <li>Teacher availability: Mon–Fri 09:00–21:00 (all timeslots covered)</li>
     *   <li>Student availability: Mon, Wed, Fri 16:00–20:00 (prime-time only)</li>
     * </ul>
     * </p>
     *
     * <p>Expected solver behavior:
     * <ul>
     *   <li>All private lessons → placed in Mon/Wed/Fri 16:00–19:00 (student constraint forces this)</li>
     *   <li>0 hard violations</li>
     *   <li>Soft score driven by prime-time reward and load balancing</li>
     * </ul>
     * </p>
     */
    private static DanceSchedule buildRealisticProblem() {
        // --- Timeslots: Mon–Fri × (9:00, 10:00, 11:00, 16:00, 17:00, 18:00) ---
        List<Timeslot> timeslots = new ArrayList<>();
        long tsId = 1L;
        LocalTime[] starts = {
                LocalTime.of(9, 0),  LocalTime.of(10, 0), LocalTime.of(11, 0),
                LocalTime.of(16, 0), LocalTime.of(17, 0), LocalTime.of(18, 0)
        };
        for (DayOfWeek day : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)) {
            for (LocalTime start : starts) {
                Timeslot ts = new Timeslot();
                ts.setId(tsId++);
                ts.setDayOfWeek(day);
                ts.setStartTime(start);
                ts.setEndTime(start.plusHours(1));
                timeslots.add(ts);
            }
        }

        // --- Teachers ---
        String[] teacherNames = {"Ana Petrova", "Boris Ivanov", "Clara Morozova", "David Kim", "Elena Sokolova"};
        String[] colors       = {"#E53935", "#1E88E5", "#43A047", "#FB8C00", "#8E24AA"};
        List<Teacher> teachers = new ArrayList<>();
        for (int i = 0; i < teacherNames.length; i++) {
            teachers.add(buildTeacher((long) (i + 1), teacherNames[i], colors[i]));
        }

        // --- Students: 3 per teacher ---
        List<Student> allStudents = new ArrayList<>();
        long studentId = 100L;
        String[] firstNames = {"Alice", "Bob", "Cara", "Dan", "Eva", "Frank",
                "Grace", "Henry", "Iris", "Jake", "Kate", "Leo", "Mia", "Nick", "Olivia"};
        int nameIdx = 0;
        for (Teacher teacher : teachers) {
            Set<Student> pool = new HashSet<>();
            for (int j = 0; j < 3; j++) {
                Student s = buildStudent(studentId++, firstNames[nameIdx++]);
                allStudents.add(s);
                pool.add(s);
            }
            teacher.setPrivateStudents(pool);
        }

        // --- Weekly Availability — Teachers: Mon–Fri 09:00–21:00 ---
        List<WeeklyAvailability> weeklyAvailabilities = new ArrayList<>();
        long waId = 1L;
        for (Teacher teacher : teachers) {
            for (DayOfWeek day : DayOfWeek.values()) {
                if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) continue;
                WeeklyAvailability wa = new WeeklyAvailability();
                wa.setId(waId++);
                wa.setUser(teacher.getUser());
                wa.setDayOfWeek(day);
                wa.setStartTime(LocalTime.of(9, 0));
                wa.setEndTime(LocalTime.of(21, 0));
                weeklyAvailabilities.add(wa);
            }
        }

        // --- Weekly Availability — Students: Mon, Wed, Fri 16:00–20:00 ---
        for (Student student : allStudents) {
            for (DayOfWeek day : List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)) {
                WeeklyAvailability wa = new WeeklyAvailability();
                wa.setId(waId++);
                wa.setUser(student);
                wa.setDayOfWeek(day);
                wa.setStartTime(LocalTime.of(16, 0));
                wa.setEndTime(LocalTime.of(20, 0));
                weeklyAvailabilities.add(wa);
            }
        }

        // --- Group Lessons (pinned) ---
        // Room is blocked at these slots: no private lessons allowed there
        Timeslot mon9  = findTimeslot(timeslots, DayOfWeek.MONDAY,    LocalTime.of(9, 0));
        Timeslot wed9  = findTimeslot(timeslots, DayOfWeek.WEDNESDAY, LocalTime.of(9, 0));

        DanceGroup beginners     = buildDanceGroup(1L, "Beginners");
        DanceGroup intermediates = buildDanceGroup(2L, "Intermediates");

        List<Lesson> lessons = new ArrayList<>();
        long lessonId = 1L;

        Lesson groupLesson1 = buildGroupLesson(lessonId++, teachers.get(0), beginners, mon9);
        Lesson groupLesson2 = buildGroupLesson(lessonId++, teachers.get(1), intermediates, wed9);
        lessons.add(groupLesson1);
        lessons.add(groupLesson2);

        // --- Private Lessons: 3 per teacher, timeslot=null (solver assigns) ---
        for (Teacher teacher : teachers) {
            for (int j = 0; j < 3; j++) {
                Lesson privateLesson = new Lesson();
                privateLesson.setId(lessonId++);
                privateLesson.setTeacher(teacher);
                privateLesson.setDurationMinutes(60);
                privateLesson.setPrivate(true);
                privateLesson.setPinned(false);
                privateLesson.setTimeslot(null); // solver will assign
                privateLesson.setStudent(null);  // solver will assign
                lessons.add(privateLesson);
            }
        }

        return new DanceSchedule(
                1L,
                timeslots,
                teachers,
                List.of(), // no one-time unavailabilities
                weeklyAvailabilities,
                List.of(SCHEDULE_ANCHOR),
                allStudents,
                lessons
        );
    }

    // ================================================================
    //  ENTITY BUILDERS
    // ================================================================

    private static Teacher buildTeacher(Long id, String fullName, String color) {
        AbstractUser user = new AbstractUser() {};
        user.setId(id);
        user.setEmail(fullName.toLowerCase().replace(" ", ".") + "@school.com");
        user.setPasswordHash("$argon2id$v=19$m=65536,t=3,p=4$placeholder");
        user.setFullName(fullName);

        Teacher teacher = new Teacher();
        teacher.setId(id);
        teacher.setUser(user);
        teacher.setMaxDailyHours(8);
        teacher.setColorCode(color);
        teacher.setPrivateStudents(new HashSet<>());
        return teacher;
    }

    private static Student buildStudent(Long id, String fullName) {
        Student student = new Student();
        student.setId(id);
        student.setEmail(fullName.toLowerCase() + "@student.com");
        student.setPasswordHash("$argon2id$v=19$m=65536,t=3,p=4$placeholder");
        student.setFullName(fullName);
        return student;
    }

    private static DanceGroup buildDanceGroup(Long id, String name) {
        DanceGroup group = new DanceGroup();
        group.setId(id);
        group.setName(name);
        return group;
    }

    private static Lesson buildGroupLesson(Long id, Teacher teacher, DanceGroup group, Timeslot timeslot) {
        Lesson lesson = new Lesson();
        lesson.setId(id);
        lesson.setTeacher(teacher);
        lesson.setDanceGroup(group);
        lesson.setTimeslot(timeslot);
        lesson.setDurationMinutes(60);
        lesson.setPrivate(false);
        lesson.setPinned(true); // group lessons are always pinned
        lesson.setStudent(null);
        return lesson;
    }

    // ================================================================
    //  SOLVER FACTORY
    // ================================================================

    private static SolverFactory<DanceSchedule> buildSolverFactory() {
        SolverConfig config = new SolverConfig()
                .withSolutionClass(DanceSchedule.class)
                .withEntityClasses(Lesson.class)
                .withConstraintProviderClass(DanceScheduleConstraintProvider.class)
                .withTerminationConfig(
                        new TerminationConfig().withSpentLimit(Duration.ofSeconds(SOLVER_TIMEOUT_SECONDS))
                );
        return SolverFactory.create(config);
    }

    // ================================================================
    //  UTILITIES
    // ================================================================

    private static Timeslot findTimeslot(List<Timeslot> timeslots, DayOfWeek day, LocalTime start) {
        return timeslots.stream()
                .filter(t -> t.getDayOfWeek() == day && t.getStartTime().equals(start))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Timeslot not found: " + day + " " + start));
    }

    private static boolean isInPrimeTime(Lesson lesson) {
        if (lesson.getTimeslot() == null) return false;
        LocalTime start = lesson.getTimeslot().getStartTime();
        return !start.isBefore(LocalTime.of(16, 0)) && start.isBefore(LocalTime.of(21, 0));
    }

    private static long countTeachers(DanceSchedule schedule) {
        return schedule.getTeacherList().size();
    }

    private static long countStudents(DanceSchedule schedule) {
        return schedule.getStudentList().size();
    }

    private static long countPrivate(DanceSchedule schedule) {
        return schedule.getLessonList().stream().filter(Lesson::isPrivate).count();
    }

    private static long countGroup(DanceSchedule schedule) {
        return schedule.getLessonList().stream().filter(l -> !l.isPrivate()).count();
    }

    private static void printTeacherWorkload(DanceSchedule schedule) {
        Map<String, Long> workload = schedule.getLessonList().stream()
                .filter(l -> l.getTimeslot() != null)
                .collect(Collectors.groupingBy(
                        l -> l.getTeacher().getUser().getFullName(),
                        Collectors.counting()
                ));

        System.out.println("║  📋 Teacher Workload:                                             ║");
        workload.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(e -> System.out.printf(
                        "║     %-20s → %2d lesson(s)                          ║%n",
                        e.getKey(), e.getValue()));
    }
}





