package com.timetable.backend.solver;

import ai.timefold.solver.test.api.score.stream.ConstraintVerifier;
import com.timetable.backend.config.SolverWeightsConfig;
import com.timetable.backend.domain.model.*;
import org.junit.jupiter.api.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Edge-case constraint tests for the Timefold Solver.
 *
 * <p>These tests extend the basic constraint coverage and demonstrate interesting
 * scheduling scenarios — cross-day boundaries, cascading constraint interactions, and
 * workload extremes — that make for compelling presentation material.</p>
 *
 * <p>Each test uses Timefold's {@link ConstraintVerifier} for isolated, deterministic
 * constraint checking without running the full solver engine.</p>
 *
 * <b>Categories covered:</b>
 * <ul>
 *   <li>Room Capacity — boundary values and mixed lesson types</li>
 *   <li>Teacher Workload — daily limit edge cases</li>
 *   <li>Teacher Gap Penalty — proportional idle-time penalty math</li>
 *   <li>Availability — cross-day availability precision</li>
 *   <li>Student Matching — subscription and double-booking edge cases</li>
 *   <li>Load Balancing — quadratic penalty math verification</li>
 * </ul>
 */
@DisplayName("🔬 Solver Constraint Edge-Case Tests")
@TestMethodOrder(MethodOrderer.DisplayName.class)
class SolverConstraintEdgeCaseTest {

    private ConstraintVerifier<DanceScheduleConstraintProvider, DanceSchedule> verifier;

    @BeforeEach
    void setUp() {
        verifier = ConstraintVerifier.build(
                new DanceScheduleConstraintProvider(new SolverWeightsConfig()),
                DanceSchedule.class,
                Lesson.class
        );
    }

    // ================================================================
    //  ROOM CAPACITY — Max 4 private lessons per timeslot
    // ================================================================

    @Test
    @DisplayName("🏠 Room capacity: Exactly 4 private lessons — boundary, no penalty")
    void roomCapacity_exactlyFourLessons_noPenalty() {
        Timeslot ts = timeslot(1L, DayOfWeek.MONDAY, "17:00", "18:00");

        verifier.verifyThat(DanceScheduleConstraintProvider::maxFourPrivateLessonsPerTimeslot)
                .given(
                        privateLesson(1L, teacher(1L, "T1"), ts),
                        privateLesson(2L, teacher(2L, "T2"), ts),
                        privateLesson(3L, teacher(3L, "T3"), ts),
                        privateLesson(4L, teacher(4L, "T4"), ts)
                )
                .penalizesBy(0);
    }

    @Test
    @DisplayName("🏠 Room capacity: 8 private lessons (2× overbooked) — penalty = 4")
    void roomCapacity_eightLessons_penaltyFour() {
        Timeslot ts = timeslot(1L, DayOfWeek.FRIDAY, "18:00", "19:00");

        verifier.verifyThat(DanceScheduleConstraintProvider::maxFourPrivateLessonsPerTimeslot)
                .given(
                        privateLesson(1L, teacher(1L, "T1"), ts),
                        privateLesson(2L, teacher(2L, "T2"), ts),
                        privateLesson(3L, teacher(3L, "T3"), ts),
                        privateLesson(4L, teacher(4L, "T4"), ts),
                        privateLesson(5L, teacher(5L, "T5"), ts),
                        privateLesson(6L, teacher(6L, "T6"), ts),
                        privateLesson(7L, teacher(7L, "T7"), ts),
                        privateLesson(8L, teacher(8L, "T8"), ts)
                )
                .penalizesBy(4); // 8 − 4 = 4 violations
    }

    @Test
    @DisplayName("🏠 Room capacity: Mix of private (3) and group (5) — only private count, no penalty")
    void roomCapacity_mixedLessonTypes_onlyPrivateCount() {
        Timeslot ts = timeslot(1L, DayOfWeek.WEDNESDAY, "16:00", "17:00");
        DanceGroup g1 = danceGroup(1L, "Salsa Beginners");
        DanceGroup g2 = danceGroup(2L, "Bachata Intermediate");

        verifier.verifyThat(DanceScheduleConstraintProvider::maxFourPrivateLessonsPerTimeslot)
                .given(
                        privateLesson(1L, teacher(1L, "T1"), ts),
                        privateLesson(2L, teacher(2L, "T2"), ts),
                        privateLesson(3L, teacher(3L, "T3"), ts),
                        groupLesson(4L, teacher(4L, "T4"), g1, ts),
                        groupLesson(5L, teacher(5L, "T5"), g2, ts)
                )
                .penalizesBy(0); // 3 private ≤ 4 — group lessons do NOT consume room capacity
    }

    // ================================================================
    //  TEACHER DAILY WORKLOAD — max 6 lessons per day
    // ================================================================

    @Test
    @DisplayName("👩‍🏫 Teacher workload: Exactly 6 lessons/day — boundary, no penalty")
    void teacherWorkload_exactlySixLessons_noPenalty() {
        Teacher teacher = teacher(1L, "Ana Petrova");
        Timeslot[] slots = {
                timeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00"),
                timeslot(2L, DayOfWeek.MONDAY, "10:00", "11:00"),
                timeslot(3L, DayOfWeek.MONDAY, "11:00", "12:00"),
                timeslot(4L, DayOfWeek.MONDAY, "16:00", "17:00"),
                timeslot(5L, DayOfWeek.MONDAY, "17:00", "18:00"),
                timeslot(6L, DayOfWeek.MONDAY, "18:00", "19:00")
        };

        verifier.verifyThat(DanceScheduleConstraintProvider::maxTeacherLessonsPerDay)
                .given(
                        privateLesson(1L, teacher, slots[0]),
                        privateLesson(2L, teacher, slots[1]),
                        privateLesson(3L, teacher, slots[2]),
                        privateLesson(4L, teacher, slots[3]),
                        privateLesson(5L, teacher, slots[4]),
                        privateLesson(6L, teacher, slots[5])
                )
                .penalizesBy(0);
    }

    @Test
    @DisplayName("👩‍🏫 Teacher workload: 8 lessons/day — over by 2, penalty = 2")
    void teacherWorkload_eightLessonsDay_penaltyTwo() {
        Teacher teacher = teacher(1L, "Ana Petrova");
        // Create 8 unique timeslots on one day
        Lesson[] ls = new Lesson[8];
        for (int i = 0; i < 8; i++) {
            Timeslot ts = timeslot((long) (i + 1), DayOfWeek.TUESDAY,
                    String.format("%02d:00", 9 + i), String.format("%02d:00", 10 + i));
            ls[i] = privateLesson((long) (i + 1), teacher, ts);
        }

        verifier.verifyThat(DanceScheduleConstraintProvider::maxTeacherLessonsPerDay)
                .given(ls[0], ls[1], ls[2], ls[3], ls[4], ls[5], ls[6], ls[7])
                .penalizesBy(2); // 8 − 6 = 2
    }

    @Test
    @DisplayName("👩‍🏫 Teacher workload: 7 lessons split across 2 days (4 Mon + 3 Tue) — no penalty")
    void teacherWorkload_sevenLessonsAcrossTwoDays_noPenalty() {
        Teacher teacher = teacher(1L, "Boris Ivanov");

        verifier.verifyThat(DanceScheduleConstraintProvider::maxTeacherLessonsPerDay)
                .given(
                        privateLesson(1L, teacher, timeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00")),
                        privateLesson(2L, teacher, timeslot(2L, DayOfWeek.MONDAY, "10:00", "11:00")),
                        privateLesson(3L, teacher, timeslot(3L, DayOfWeek.MONDAY, "11:00", "12:00")),
                        privateLesson(4L, teacher, timeslot(4L, DayOfWeek.MONDAY, "16:00", "17:00")),
                        privateLesson(5L, teacher, timeslot(5L, DayOfWeek.TUESDAY, "09:00", "10:00")),
                        privateLesson(6L, teacher, timeslot(6L, DayOfWeek.TUESDAY, "10:00", "11:00")),
                        privateLesson(7L, teacher, timeslot(7L, DayOfWeek.TUESDAY, "11:00", "12:00"))
                )
                .penalizesBy(0); // 4 on Mon ≤ 6, 3 on Tue ≤ 6 — both OK
    }

    // ================================================================
    //  TEACHER GAP PENALTY — idle-time between lessons on same day
    // ================================================================

    @Test
    @DisplayName("⏰ Teacher gap: 2-hour gap (120 min) between lessons — penalty formula: (120/60)+1 = 3")
    void teacherGap_twoHourGap_penaltyThree() {
        Teacher teacher = teacher(1L, "Clara Morozova");
        Timeslot ts1 = timeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        Timeslot ts2 = timeslot(2L, DayOfWeek.MONDAY, "12:00", "13:00");
        DanceGroup g1 = danceGroup(1L, "Group A");
        DanceGroup g2 = danceGroup(2L, "Group B");

        verifier.verifyThat(DanceScheduleConstraintProvider::minimizeTeacherGaps)
                .given(
                        groupLesson(1L, teacher, g1, ts1),
                        groupLesson(2L, teacher, g2, ts2)
                )
                .penalizesBy(3); // gap = 120 min → (120/60)+1 = 3
    }

    @Test
    @DisplayName("⏰ Teacher gap: 1-hour gap (60 min) — penalty formula: (60/60)+1 = 2")
    void teacherGap_oneHourGap_penaltyTwo() {
        Teacher teacher = teacher(1L, "David Kim");
        Timeslot ts1 = timeslot(1L, DayOfWeek.WEDNESDAY, "09:00", "10:00");
        Timeslot ts2 = timeslot(2L, DayOfWeek.WEDNESDAY, "11:00", "12:00");
        DanceGroup g1 = danceGroup(1L, "Group C");
        DanceGroup g2 = danceGroup(2L, "Group D");

        verifier.verifyThat(DanceScheduleConstraintProvider::minimizeTeacherGaps)
                .given(
                        groupLesson(1L, teacher, g1, ts1),
                        groupLesson(2L, teacher, g2, ts2)
                )
                .penalizesBy(2); // gap = 60 min → (60/60)+1 = 2
    }

    @Test
    @DisplayName("⏰ Teacher gap: 15-min acceptable break — NO penalty (within threshold)")
    void teacherGap_fifteenMinuteBreak_noPenalty() {
        Teacher teacher = teacher(1L, "Elena Sokolova");
        Timeslot ts1 = timeslot(1L, DayOfWeek.FRIDAY, "09:00", "10:00");
        Timeslot ts2 = timeslot(2L, DayOfWeek.FRIDAY, "10:15", "11:15");
        DanceGroup g1 = danceGroup(1L, "Group E");
        DanceGroup g2 = danceGroup(2L, "Group F");

        verifier.verifyThat(DanceScheduleConstraintProvider::minimizeTeacherGaps)
                .given(
                        groupLesson(1L, teacher, g1, ts1),
                        groupLesson(2L, teacher, g2, ts2)
                )
                .penalizesBy(0); // 15-min break = acceptable threshold, no penalty
    }

    @Test
    @DisplayName("⏰ Teacher gap: 3 lessons — solver finds correct pair-wise gap penalty")
    void teacherGap_threeLessons_sumsPenalties() {
        Teacher teacher = teacher(1L, "Ana Petrova");
        Timeslot ts1 = timeslot(1L, DayOfWeek.THURSDAY, "09:00", "10:00");
        Timeslot ts2 = timeslot(2L, DayOfWeek.THURSDAY, "12:00", "13:00"); // +120 min gap
        Timeslot ts3 = timeslot(3L, DayOfWeek.THURSDAY, "15:00", "16:00"); // +120 min gap
        DanceGroup g1 = danceGroup(1L, "G1");
        DanceGroup g2 = danceGroup(2L, "G2");
        DanceGroup g3 = danceGroup(3L, "G3");

        verifier.verifyThat(DanceScheduleConstraintProvider::minimizeTeacherGaps)
                .given(
                        groupLesson(1L, teacher, g1, ts1),
                        groupLesson(2L, teacher, g2, ts2),
                        groupLesson(3L, teacher, g3, ts3)
                )
                // pair (ts1,ts2): gap=120 → (120/60)+1=3
                // pair (ts1,ts3): gap=300 → (300/60)+1=6
                // pair (ts2,ts3): gap=120 → (120/60)+1=3
                // total = 3+6+3 = 12
                .penalizesBy(12);
    }

    // ================================================================
    //  PRIME-TIME — Reward for lessons in 16:00–21:00 window
    // ================================================================

    @Test
    @DisplayName("🌟 Prime-time: Exactly at boundary 16:00 — rewarded (1 match)")
    void primeTime_exactBoundaryStart_rewarded() {
        Teacher teacher = teacher(1L, "T1");
        Timeslot ts = timeslot(1L, DayOfWeek.MONDAY, "16:00", "17:00");
        DanceGroup group = danceGroup(1L, "Salsa");

        // ConstraintVerifier.rewardsWith(N) verifies N constraint MATCHES (not the soft score value).
        // 1 lesson in prime time → 1 match → multiplied by weight (10) at runtime.
        verifier.verifyThat(DanceScheduleConstraintProvider::rewardPrimeTime)
                .given(groupLesson(1L, teacher, group, ts))
                .rewardsWith(1);
    }

    @Test
    @DisplayName("🌟 Prime-time: Exactly at 20:00 — rewarded (last prime hour, 1 match)")
    void primeTime_latePrimeSlot_20_00_rewarded() {
        Teacher teacher = teacher(1L, "T1");
        Timeslot ts = timeslot(1L, DayOfWeek.FRIDAY, "20:00", "21:00");
        DanceGroup group = danceGroup(1L, "Tango Advanced");

        verifier.verifyThat(DanceScheduleConstraintProvider::rewardPrimeTime)
                .given(groupLesson(1L, teacher, group, ts))
                .rewardsWith(1);
    }

    @Test
    @DisplayName("🌟 Prime-time: Morning slot 09:00 — NOT rewarded (0 matches)")
    void primeTime_morningSlot_noReward() {
        Teacher teacher = teacher(1L, "T1");
        Timeslot ts = timeslot(1L, DayOfWeek.TUESDAY, "09:00", "10:00");
        DanceGroup group = danceGroup(1L, "Beginner Morning");

        verifier.verifyThat(DanceScheduleConstraintProvider::rewardPrimeTime)
                .given(groupLesson(1L, teacher, group, ts))
                .rewardsWith(0);
    }

    @Test
    @DisplayName("🌟 Prime-time: 5 lessons all in prime time — 5 constraint matches rewarded")
    void primeTime_fiveLessonsAllPrime_fiveMatchesRewarded() {
        Teacher t1 = teacher(1L, "T1");
        Teacher t2 = teacher(2L, "T2");
        Teacher t3 = teacher(3L, "T3");
        Teacher t4 = teacher(4L, "T4");
        Teacher t5 = teacher(5L, "T5");
        DanceGroup g = danceGroup(1L, "G");

        // 5 lessons in prime time → 5 matches → each contributes +10 soft at runtime
        verifier.verifyThat(DanceScheduleConstraintProvider::rewardPrimeTime)
                .given(
                        groupLesson(1L, t1, g, timeslot(1L, DayOfWeek.MONDAY,    "16:00", "17:00")),
                        groupLesson(2L, t2, g, timeslot(2L, DayOfWeek.TUESDAY,   "17:00", "18:00")),
                        groupLesson(3L, t3, g, timeslot(3L, DayOfWeek.WEDNESDAY, "18:00", "19:00")),
                        groupLesson(4L, t4, g, timeslot(4L, DayOfWeek.THURSDAY,  "19:00", "20:00")),
                        groupLesson(5L, t5, g, timeslot(5L, DayOfWeek.FRIDAY,    "20:00", "21:00"))
                )
                .rewardsWith(5); // 5 matches × weight(10) = +50 soft in actual schedule
    }

    // ================================================================
    //  LOAD BALANCING — Quadratic penalty verification
    // ================================================================

    @Test
    @DisplayName("⚖️ Load balancing: 1 teacher × 1 lesson — penalty = 1²= 1")
    void loadBalance_oneTeacherOneLesson_penaltyOne() {
        Teacher teacher = teacher(1L, "Solo Teacher");
        Timeslot ts = timeslot(1L, DayOfWeek.MONDAY, "17:00", "18:00");

        verifier.verifyThat(DanceScheduleConstraintProvider::balanceTeacherLoad)
                .given(privateLesson(1L, teacher, ts))
                .penalizesBy(1); // 1² = 1
    }

    @Test
    @DisplayName("⚖️ Load balancing: 1 teacher × 5 lessons — penalty = 5² = 25")
    void loadBalance_oneTeacherFiveLessons_penaltyTwentyFive() {
        Teacher teacher = teacher(1L, "Ana Petrova");

        verifier.verifyThat(DanceScheduleConstraintProvider::balanceTeacherLoad)
                .given(
                        privateLesson(1L, teacher, timeslot(1L, DayOfWeek.MONDAY,    "16:00", "17:00")),
                        privateLesson(2L, teacher, timeslot(2L, DayOfWeek.MONDAY,    "17:00", "18:00")),
                        privateLesson(3L, teacher, timeslot(3L, DayOfWeek.TUESDAY,   "16:00", "17:00")),
                        privateLesson(4L, teacher, timeslot(4L, DayOfWeek.WEDNESDAY, "16:00", "17:00")),
                        privateLesson(5L, teacher, timeslot(5L, DayOfWeek.THURSDAY,  "16:00", "17:00"))
                )
                .penalizesBy(25); // 5² = 25
    }

    @Test
    @DisplayName("⚖️ Load balancing: 2 teachers × 3 lessons each — penalty = 3² + 3² = 18 (balanced)")
    void loadBalance_twoTeachersBalanced_penaltyEighteen() {
        Teacher t1 = teacher(1L, "Teacher A");
        Teacher t2 = teacher(2L, "Teacher B");

        verifier.verifyThat(DanceScheduleConstraintProvider::balanceTeacherLoad)
                .given(
                        privateLesson(1L, t1, timeslot(1L, DayOfWeek.MONDAY, "16:00", "17:00")),
                        privateLesson(2L, t1, timeslot(2L, DayOfWeek.TUESDAY, "16:00", "17:00")),
                        privateLesson(3L, t1, timeslot(3L, DayOfWeek.WEDNESDAY, "16:00", "17:00")),
                        privateLesson(4L, t2, timeslot(4L, DayOfWeek.MONDAY, "17:00", "18:00")),
                        privateLesson(5L, t2, timeslot(5L, DayOfWeek.TUESDAY, "17:00", "18:00")),
                        privateLesson(6L, t2, timeslot(6L, DayOfWeek.WEDNESDAY, "17:00", "18:00"))
                )
                .penalizesBy(18); // 3² + 3² = 9 + 9 = 18
    }

    @Test
    @DisplayName("⚖️ Load balancing: 2 teachers — 1 vs 5 lessons — penalty = 1 + 25 = 26 (imbalanced!)")
    void loadBalance_twoTeachersImbalanced_penaltyTwentySix() {
        Teacher t1 = teacher(1L, "Overloaded Teacher");
        Teacher t2 = teacher(2L, "Underused Teacher");

        verifier.verifyThat(DanceScheduleConstraintProvider::balanceTeacherLoad)
                .given(
                        privateLesson(1L, t1, timeslot(1L, DayOfWeek.MONDAY,    "16:00", "17:00")),
                        privateLesson(2L, t1, timeslot(2L, DayOfWeek.MONDAY,    "17:00", "18:00")),
                        privateLesson(3L, t1, timeslot(3L, DayOfWeek.TUESDAY,   "16:00", "17:00")),
                        privateLesson(4L, t1, timeslot(4L, DayOfWeek.TUESDAY,   "17:00", "18:00")),
                        privateLesson(5L, t1, timeslot(5L, DayOfWeek.WEDNESDAY, "16:00", "17:00")),
                        privateLesson(6L, t2, timeslot(6L, DayOfWeek.FRIDAY,    "16:00", "17:00"))
                )
                .penalizesBy(26); // 5² + 1² = 25 + 1 = 26
    }

    // ================================================================
    //  STUDENT — Complex matching scenarios
    // ================================================================

    @Test
    @DisplayName("🎓 Student matching: 3 students, each with their own teacher — no conflicts")
    void studentMatching_threeStudentsThreeTeachers_noConflicts() {
        Timeslot ts = timeslot(1L, DayOfWeek.WEDNESDAY, "17:00", "18:00");

        Student s1 = student(101L, "Alice");
        Student s2 = student(102L, "Bob");
        Student s3 = student(103L, "Cara");

        Teacher t1 = teacherWithStudents(1L, "T1", s1);
        Teacher t2 = teacherWithStudents(2L, "T2", s2);
        Teacher t3 = teacherWithStudents(3L, "T3", s3);

        Lesson l1 = privateLessonWithStudent(1L, t1, ts, s1);
        Lesson l2 = privateLessonWithStudent(2L, t2, ts, s2);
        Lesson l3 = privateLessonWithStudent(3L, t3, ts, s3);

        verifier.verifyThat(DanceScheduleConstraintProvider::studentConflict)
                .given(l1, l2, l3)
                .penalizesBy(0);
    }

    @Test
    @DisplayName("🎓 Student matching: Same student at same time with two different teachers — conflict!")
    void studentMatching_sameStudentTwoTeachers_conflict() {
        Timeslot ts = timeslot(1L, DayOfWeek.MONDAY, "18:00", "19:00");
        Student alice = student(101L, "Alice");
        Teacher t1 = teacher(1L, "Teacher 1");
        Teacher t2 = teacher(2L, "Teacher 2");

        Lesson l1 = privateLessonWithStudent(1L, t1, ts, alice);
        Lesson l2 = privateLessonWithStudent(2L, t2, ts, alice);

        verifier.verifyThat(DanceScheduleConstraintProvider::studentConflict)
                .given(l1, l2)
                .penalizesBy(1);
    }

    @Test
    @DisplayName("🔗 Student subscription: Student at unsubscribed teacher — hard penalty")
    void studentSubscription_unsubscribedTeacher_penaltyOne() {
        Teacher teacher = teacher(1L, "Strict Teacher");
        teacher.setPrivateStudents(new HashSet<>()); // no students registered

        Student gate_crasher = student(999L, "Uninvited Student");
        Timeslot ts = timeslot(1L, DayOfWeek.TUESDAY, "19:00", "20:00");

        Lesson lesson = privateLessonWithStudent(1L, teacher, ts, gate_crasher);

        verifier.verifyThat(DanceScheduleConstraintProvider::studentMustBeSubscribedToTeacher)
                .given(lesson)
                .penalizesBy(1);
    }

    @Test
    @DisplayName("🔗 Student subscription: Student with TWO subscribed teachers — each valid")
    void studentSubscription_studentSubscribedToTwoTeachers_bothValid() {
        Student alex = student(200L, "Alex");

        Teacher t1 = teacherWithStudents(1L, "Teacher One", alex);
        Teacher t2 = teacherWithStudents(2L, "Teacher Two", alex);

        Timeslot ts1 = timeslot(1L, DayOfWeek.MONDAY,  "17:00", "18:00");
        Timeslot ts2 = timeslot(2L, DayOfWeek.TUESDAY, "17:00", "18:00");

        verifier.verifyThat(DanceScheduleConstraintProvider::studentMustBeSubscribedToTeacher)
                .given(
                        privateLessonWithStudent(1L, t1, ts1, alex),
                        privateLessonWithStudent(2L, t2, ts2, alex)
                )
                .penalizesBy(0); // Both teachers have Alex in their pool — valid
    }

    // ================================================================
    //  TEACHER AVAILABILITY — Precision checks
    // ================================================================

    @Test
    @DisplayName("🗓️ Teacher availability: Lesson fits EXACTLY into availability window — no penalty")
    void teacherAvailability_lessonFitsExactly_noPenalty() {
        Teacher teacher = teacher(1L, "Ana");
        Timeslot ts = timeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        DanceGroup group = danceGroup(1L, "Group");
        Lesson lesson = groupLesson(1L, teacher, group, ts);

        WeeklyAvailability availability = weeklyAvailability(1L, teacher.getUser(),
                DayOfWeek.MONDAY, "09:00", "10:00");

        verifier.verifyThat(DanceScheduleConstraintProvider::teacherOutsideWeeklyAvailability)
                .given(lesson, availability)
                .penalizesBy(0);
    }

    @Test
    @DisplayName("🗓️ Teacher availability: Lesson EXTENDS beyond availability window — penalty")
    void teacherAvailability_lessonExtendsBeyondWindow_penalty() {
        Teacher teacher = teacher(1L, "Boris");
        Timeslot ts = timeslot(1L, DayOfWeek.FRIDAY, "18:00", "19:00");
        DanceGroup group = danceGroup(1L, "Group");
        Lesson lesson = groupLesson(1L, teacher, group, ts);

        // Window ends at 18:30 — lesson ends at 19:00 (exceeds window by 30min)
        WeeklyAvailability availability = weeklyAvailability(1L, teacher.getUser(),
                DayOfWeek.FRIDAY, "16:00", "18:30");

        verifier.verifyThat(DanceScheduleConstraintProvider::teacherOutsideWeeklyAvailability)
                .given(lesson, availability)
                .penalizesBy(1);
    }

    @Test
    @DisplayName("🗓️ Teacher availability: Lesson on WRONG day — penalty even if time matches")
    void teacherAvailability_wrongDay_penalty() {
        Teacher teacher = teacher(1L, "Clara");
        Timeslot ts = timeslot(1L, DayOfWeek.SATURDAY, "10:00", "11:00"); // Saturday!
        DanceGroup group = danceGroup(1L, "Weekend Group");
        Lesson lesson = groupLesson(1L, teacher, group, ts);

        // Teacher only available Mon–Fri
        WeeklyAvailability availability = weeklyAvailability(1L, teacher.getUser(),
                DayOfWeek.FRIDAY, "09:00", "21:00"); // different day

        verifier.verifyThat(DanceScheduleConstraintProvider::teacherOutsideWeeklyAvailability)
                .given(lesson, availability)
                .penalizesBy(1);
    }

    // ================================================================
    //  TEST DATA BUILDERS (private DSL methods)
    // ================================================================

    private Timeslot timeslot(Long id, DayOfWeek day, String start, String end) {
        Timeslot ts = new Timeslot();
        ts.setId(id);
        ts.setDayOfWeek(day);
        ts.setStartTime(LocalTime.parse(start));
        ts.setEndTime(LocalTime.parse(end));
        return ts;
    }

    private Teacher teacher(Long id, String name) {
        AbstractUser user = new AbstractUser() {};
        user.setId(id);
        user.setEmail(name.toLowerCase().replace(" ", ".") + "@school.com");
        user.setPasswordHash("$argon2id$placeholder");
        user.setFullName(name);

        Teacher teacher = new Teacher();
        teacher.setId(id);
        teacher.setUser(user);
        teacher.setMaxDailyHours(8);
        teacher.setColorCode("#000000");
        teacher.setPrivateStudents(new HashSet<>());
        return teacher;
    }

    private Teacher teacherWithStudents(Long id, String name, Student... students) {
        Teacher teacher = teacher(id, name);
        teacher.setPrivateStudents(new HashSet<>(Set.of(students)));
        return teacher;
    }

    private Student student(Long id, String name) {
        Student student = new Student();
        student.setId(id);
        student.setEmail(name.toLowerCase() + "@student.com");
        student.setPasswordHash("$argon2id$placeholder");
        student.setFullName(name);
        return student;
    }

    private DanceGroup danceGroup(Long id, String name) {
        DanceGroup group = new DanceGroup();
        group.setId(id);
        group.setName(name);
        return group;
    }

    private Lesson privateLesson(Long id, Teacher teacher, Timeslot timeslot) {
        Lesson lesson = new Lesson();
        lesson.setId(id);
        lesson.setTeacher(teacher);
        lesson.setTimeslot(timeslot);
        lesson.setDurationMinutes(60);
        lesson.setPrivate(true);
        lesson.setPinned(false);
        return lesson;
    }

    private Lesson privateLessonWithStudent(Long id, Teacher teacher, Timeslot timeslot, Student student) {
        Lesson lesson = privateLesson(id, teacher, timeslot);
        lesson.setStudent(student);
        return lesson;
    }

    private Lesson groupLesson(Long id, Teacher teacher, DanceGroup group, Timeslot timeslot) {
        Lesson lesson = new Lesson();
        lesson.setId(id);
        lesson.setTeacher(teacher);
        lesson.setDanceGroup(group);
        lesson.setTimeslot(timeslot);
        lesson.setDurationMinutes(60);
        lesson.setPrivate(false);
        lesson.setPinned(true);
        return lesson;
    }

    private WeeklyAvailability weeklyAvailability(Long id, AbstractUser user, // NOSONAR: id varies per caller
                                                   DayOfWeek day, String start, String end) {
        WeeklyAvailability wa = new WeeklyAvailability();
        wa.setId(id);
        wa.setUser(user);
        wa.setDayOfWeek(day);
        wa.setStartTime(LocalTime.parse(start));
        wa.setEndTime(LocalTime.parse(end));
        return wa;
    }
}





