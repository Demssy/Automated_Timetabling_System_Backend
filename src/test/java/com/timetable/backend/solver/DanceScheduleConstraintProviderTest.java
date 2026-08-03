package com.timetable.backend.solver;

import ai.timefold.solver.test.api.score.stream.ConstraintVerifier;
import com.timetable.backend.config.SolverWeightsConfig;
import com.timetable.backend.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Unit tests for DanceScheduleConstraintProvider.
 * Uses Timefold's ConstraintVerifier to test each constraint in isolation.
 *
 * Room is NOT a planning variable — all room-related parameters have been removed.
 * New constraints: maxFourPrivateLessonsPerTimeslot, noPrivateDuringGroupLesson.
 */
class DanceScheduleConstraintProviderTest {

    private ConstraintVerifier<DanceScheduleConstraintProvider, DanceSchedule> constraintVerifier;

    @BeforeEach
    void setUp() {
        constraintVerifier = ConstraintVerifier.build(
            new DanceScheduleConstraintProvider(new SolverWeightsConfig()),
            DanceSchedule.class,
            Lesson.class
        );
    }

    // ==================== HARD CONSTRAINT: Max 4 Private Lessons Per Timeslot ====================

    @Test
    @DisplayName("Max 4 private per timeslot: Four private lessons — no penalty")
    void noPenalty_fourPrivateLessonsInTimeslot() {
        Timeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        Teacher teacher1 = createTeacher(1L, "Teacher 1");
        Teacher teacher2 = createTeacher(2L, "Teacher 2");
        Teacher teacher3 = createTeacher(3L, "Teacher 3");
        Teacher teacher4 = createTeacher(4L, "Teacher 4");

        Lesson lesson1 = createLesson(1L, teacher1, null, timeslot, true, false);
        Lesson lesson2 = createLesson(2L, teacher2, null, timeslot, true, false);
        Lesson lesson3 = createLesson(3L, teacher3, null, timeslot, true, false);
        Lesson lesson4 = createLesson(4L, teacher4, null, timeslot, true, false);

        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::maxFourPrivateLessonsPerTimeslot)
            .given(lesson1, lesson2, lesson3, lesson4)
            .penalizesBy(0);
    }

    @Test
    @DisplayName("Max 4 private per timeslot: Five private lessons — penalty 1")
    void penalty_fivePrivateLessonsInTimeslot() {
        Timeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        Teacher teacher1 = createTeacher(1L, "Teacher 1");
        Teacher teacher2 = createTeacher(2L, "Teacher 2");
        Teacher teacher3 = createTeacher(3L, "Teacher 3");
        Teacher teacher4 = createTeacher(4L, "Teacher 4");
        Teacher teacher5 = createTeacher(5L, "Teacher 5");

        Lesson lesson1 = createLesson(1L, teacher1, null, timeslot, true, false);
        Lesson lesson2 = createLesson(2L, teacher2, null, timeslot, true, false);
        Lesson lesson3 = createLesson(3L, teacher3, null, timeslot, true, false);
        Lesson lesson4 = createLesson(4L, teacher4, null, timeslot, true, false);
        Lesson lesson5 = createLesson(5L, teacher5, null, timeslot, true, false);

        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::maxFourPrivateLessonsPerTimeslot)
            .given(lesson1, lesson2, lesson3, lesson4, lesson5)
            .penalizesBy(1);
    }

    @Test
    @DisplayName("Max 4 private per timeslot: Six private lessons — penalty 2")
    void penalty_sixPrivateLessonsInTimeslot() {
        Timeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");

        Lesson lesson1 = createLesson(1L, createTeacher(1L, "T1"), null, timeslot, true, false);
        Lesson lesson2 = createLesson(2L, createTeacher(2L, "T2"), null, timeslot, true, false);
        Lesson lesson3 = createLesson(3L, createTeacher(3L, "T3"), null, timeslot, true, false);
        Lesson lesson4 = createLesson(4L, createTeacher(4L, "T4"), null, timeslot, true, false);
        Lesson lesson5 = createLesson(5L, createTeacher(5L, "T5"), null, timeslot, true, false);
        Lesson lesson6 = createLesson(6L, createTeacher(6L, "T6"), null, timeslot, true, false);

        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::maxFourPrivateLessonsPerTimeslot)
            .given(lesson1, lesson2, lesson3, lesson4, lesson5, lesson6)
            .penalizesBy(2);
    }

    @Test
    @DisplayName("Max 4 private per timeslot: Three private lessons — no penalty")
    void noPenalty_threePrivateLessonsInTimeslot() {
        Timeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        Teacher teacher1 = createTeacher(1L, "Teacher 1");
        Teacher teacher2 = createTeacher(2L, "Teacher 2");
        Teacher teacher3 = createTeacher(3L, "Teacher 3");

        Lesson lesson1 = createLesson(1L, teacher1, null, timeslot, true, false);
        Lesson lesson2 = createLesson(2L, teacher2, null, timeslot, true, false);
        Lesson lesson3 = createLesson(3L, teacher3, null, timeslot, true, false);

        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::maxFourPrivateLessonsPerTimeslot)
            .given(lesson1, lesson2, lesson3)
            .penalizesBy(0);
    }

    @Test
    @DisplayName("Max 4 private per timeslot: Group lessons do NOT count")
    void noPenalty_groupLessonsDoNotCount() {
        Timeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        DanceGroup group1 = createDanceGroup(1L, "Group 1");
        DanceGroup group2 = createDanceGroup(2L, "Group 2");

        Lesson lesson1 = createLesson(1L, createTeacher(1L, "T1"), group1, timeslot, false, true);
        Lesson lesson2 = createLesson(2L, createTeacher(2L, "T2"), group2, timeslot, false, true);

        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::maxFourPrivateLessonsPerTimeslot)
            .given(lesson1, lesson2)
            .penalizesBy(0);
    }

    // ==================== HARD CONSTRAINT: No Private During Group Lesson ====================

    @Test
    @DisplayName("No private during group: Penalty when private overlaps group timeslot")
    void penalty_privateDuringGroupLesson() {
        Timeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        DanceGroup group = createDanceGroup(1L, "Beginners");

        Lesson groupLesson = createLesson(1L, createTeacher(1L, "T1"), group, timeslot, false, true);
        Lesson privateLesson = createLesson(2L, createTeacher(2L, "T2"), null, timeslot, true, false);

        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::noPrivateDuringGroupLesson)
            .given(groupLesson, privateLesson)
            .penalizesBy(1);
    }

    @Test
    @DisplayName("No private during group: No penalty when private is at different timeslot")
    void noPenalty_privateDifferentTimeslotFromGroup() {
        Timeslot timeslot1 = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        Timeslot timeslot2 = createTimeslot(2L, DayOfWeek.MONDAY, "10:00", "11:00");
        DanceGroup group = createDanceGroup(1L, "Beginners");

        Lesson groupLesson = createLesson(1L, createTeacher(1L, "T1"), group, timeslot1, false, true);
        Lesson privateLesson = createLesson(2L, createTeacher(2L, "T2"), null, timeslot2, true, false);

        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::noPrivateDuringGroupLesson)
            .given(groupLesson, privateLesson)
            .penalizesBy(0);
    }

    @Test
    @DisplayName("No private during group: Multiple private lessons during group — penalty per private")
    void penalty_multiplePrivateDuringGroup() {
        Timeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        DanceGroup group = createDanceGroup(1L, "Beginners");

        Lesson groupLesson = createLesson(1L, createTeacher(1L, "T1"), group, timeslot, false, true);
        Lesson private1 = createLesson(2L, createTeacher(2L, "T2"), null, timeslot, true, false);
        Lesson private2 = createLesson(3L, createTeacher(3L, "T3"), null, timeslot, true, false);

        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::noPrivateDuringGroupLesson)
            .given(groupLesson, private1, private2)
            .penalizesBy(2);
    }

    @Test
    @DisplayName("No private during group: No penalty when only private lessons")
    void noPenalty_onlyPrivateLessons() {
        Timeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");

        Lesson private1 = createLesson(1L, createTeacher(1L, "T1"), null, timeslot, true, false);
        Lesson private2 = createLesson(2L, createTeacher(2L, "T2"), null, timeslot, true, false);

        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::noPrivateDuringGroupLesson)
            .given(private1, private2)
            .penalizesBy(0);
    }

    // ==================== HARD CONSTRAINT: Teacher Conflict ====================

    @Test
    @DisplayName("Teacher conflict: Teacher cannot teach two lessons simultaneously")
    void penaltyForTeacherConflict() {
        Teacher teacher = createTeacher(1L, "John Doe");
        Timeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        DanceGroup group1 = createDanceGroup(1L, "Group 1");
        DanceGroup group2 = createDanceGroup(2L, "Group 2");

        Lesson lesson1 = createLesson(1L, teacher, group1, timeslot, false, true);
        Lesson lesson2 = createLesson(2L, teacher, group2, timeslot, false, true);

        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::teacherConflict)
            .given(lesson1, lesson2, teacher, timeslot)
            .penalizesBy(1);
    }

    @Test
    @DisplayName("Teacher conflict: No penalty for different timeslots")
    void noPenaltyForTeacherConflict_differentTimeslots() {
        Teacher teacher = createTeacher(1L, "John Doe");
        Timeslot timeslot1 = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        Timeslot timeslot2 = createTimeslot(2L, DayOfWeek.MONDAY, "10:00", "11:00");
        DanceGroup group1 = createDanceGroup(1L, "Group 1");
        DanceGroup group2 = createDanceGroup(2L, "Group 2");

        Lesson lesson1 = createLesson(1L, teacher, group1, timeslot1, false, true);
        Lesson lesson2 = createLesson(2L, teacher, group2, timeslot2, false, true);

        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::teacherConflict)
            .given(lesson1, lesson2, teacher, timeslot1, timeslot2)
            .penalizesBy(0);
    }

    @Test
    @DisplayName("Teacher conflict: No penalty for different teachers")
    void noPenaltyForTeacherConflict_differentTeachers() {
        Teacher teacher1 = createTeacher(1L, "John Doe");
        Teacher teacher2 = createTeacher(2L, "Jane Smith");
        Timeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        DanceGroup group1 = createDanceGroup(1L, "Group 1");
        DanceGroup group2 = createDanceGroup(2L, "Group 2");

        Lesson lesson1 = createLesson(1L, teacher1, group1, timeslot, false, true);
        Lesson lesson2 = createLesson(2L, teacher2, group2, timeslot, false, true);

        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::teacherConflict)
            .given(lesson1, lesson2, teacher1, teacher2, timeslot)
            .penalizesBy(0);
    }

    // ==================== HARD CONSTRAINT: Teacher Availability ====================

    private static final LocalDate SCHEDULE_ANCHOR = LocalDate.of(2024, 1, 1);

    @Test
    @DisplayName("Teacher one-time unavailability: Penalty when lesson overlaps unavailable period")
    void penaltyForTeacherUnavailability() {
        Teacher teacher = createTeacher(1L, "John Doe");
        Timeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        DanceGroup group = createDanceGroup(1L, "Group 1");

        Lesson lesson = createLesson(1L, teacher, group, timeslot, false, true);
        ResourceUnavailability unavailability = createUnavailability(1L, teacher, timeslot, "Vacation");

        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::teacherOneTimeUnavailability)
            .given(lesson, unavailability, SCHEDULE_ANCHOR)
            .penalizesBy(1);
    }

    @Test
    @DisplayName("Teacher one-time unavailability: No penalty when no unavailability record exists")
    void noPenaltyForTeacherAvailability_teacherAvailable() {
        Teacher teacher = createTeacher(1L, "John Doe");
        Timeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        DanceGroup group = createDanceGroup(1L, "Group 1");

        Lesson lesson = createLesson(1L, teacher, group, timeslot, false, true);

        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::teacherOneTimeUnavailability)
            .given(lesson, SCHEDULE_ANCHOR)
            .penalizesBy(0);
    }

    @Test
    @DisplayName("Teacher one-time unavailability: No penalty when lesson is at a different time")
    void noPenaltyForTeacherAvailability_differentTimeslot() {
        Teacher teacher = createTeacher(1L, "John Doe");
        Timeslot unavailableSlot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        Timeslot lessonSlot = createTimeslot(2L, DayOfWeek.MONDAY, "10:00", "11:00");
        DanceGroup group = createDanceGroup(1L, "Group 1");

        Lesson lesson = createLesson(1L, teacher, group, lessonSlot, false, true);
        ResourceUnavailability unavailability = createUnavailability(1L, teacher, unavailableSlot, "Meeting");

        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::teacherOneTimeUnavailability)
            .given(lesson, unavailability, SCHEDULE_ANCHOR)
            .penalizesBy(0);
    }

    // ==================== SOFT CONSTRAINT: Minimize Teacher Gaps ====================

    @Test
    @DisplayName("Minimize gaps: Penalty proportional to gap duration (formula: (gapMinutes/60)+1)")
    void penaltyForTeacherGaps_proportionalToGapDuration() {
        Teacher teacher = createTeacher(1L, "John Doe");
        Timeslot timeslot1 = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        Timeslot timeslot2 = createTimeslot(2L, DayOfWeek.MONDAY, "12:00", "13:00");
        DanceGroup group1 = createDanceGroup(1L, "Group 1");
        DanceGroup group2 = createDanceGroup(2L, "Group 2");

        Lesson lesson1 = createLesson(1L, teacher, group1, timeslot1, false, true);
        Lesson lesson2 = createLesson(2L, teacher, group2, timeslot2, false, true);

        // Gap = 120 min  →  penalty = (120/60)+1 = 3
        // Formula: (gapMinutes / 60) + 1  — penalizes each full hour of idle time plus a base cost.
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::minimizeTeacherGaps)
            .given(lesson1, lesson2, teacher, timeslot1, timeslot2)
            .penalizesBy(3);
    }

    @Test
    @DisplayName("Minimize gaps: No penalty for consecutive lessons")
    void noPenaltyForTeacherGaps_consecutiveLessons() {
        Teacher teacher = createTeacher(1L, "John Doe");
        Timeslot timeslot1 = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        Timeslot timeslot2 = createTimeslot(2L, DayOfWeek.MONDAY, "10:00", "11:00");
        DanceGroup group1 = createDanceGroup(1L, "Group 1");
        DanceGroup group2 = createDanceGroup(2L, "Group 2");

        Lesson lesson1 = createLesson(1L, teacher, group1, timeslot1, false, true);
        Lesson lesson2 = createLesson(2L, teacher, group2, timeslot2, false, true);

        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::minimizeTeacherGaps)
            .given(lesson1, lesson2, teacher, timeslot1, timeslot2)
            .penalizesBy(0);
    }

    @Test
    @DisplayName("Minimize gaps: No penalty for different days")
    void noPenaltyForTeacherGaps_differentDays() {
        Teacher teacher = createTeacher(1L, "John Doe");
        Timeslot timeslot1 = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        Timeslot timeslot2 = createTimeslot(2L, DayOfWeek.TUESDAY, "09:00", "10:00");
        DanceGroup group1 = createDanceGroup(1L, "Group 1");
        DanceGroup group2 = createDanceGroup(2L, "Group 2");

        Lesson lesson1 = createLesson(1L, teacher, group1, timeslot1, false, true);
        Lesson lesson2 = createLesson(2L, teacher, group2, timeslot2, false, true);

        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::minimizeTeacherGaps)
            .given(lesson1, lesson2, teacher, timeslot1, timeslot2)
            .penalizesBy(0);
    }

    @Test
    @DisplayName("Minimize gaps: No penalty for different teachers")
    void noPenaltyForTeacherGaps_differentTeachers() {
        Teacher teacher1 = createTeacher(1L, "John Doe");
        Teacher teacher2 = createTeacher(2L, "Jane Smith");
        Timeslot timeslot1 = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        Timeslot timeslot2 = createTimeslot(2L, DayOfWeek.MONDAY, "12:00", "13:00");
        DanceGroup group1 = createDanceGroup(1L, "Group 1");
        DanceGroup group2 = createDanceGroup(2L, "Group 2");

        Lesson lesson1 = createLesson(1L, teacher1, group1, timeslot1, false, true);
        Lesson lesson2 = createLesson(2L, teacher2, group2, timeslot2, false, true);

        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::minimizeTeacherGaps)
            .given(lesson1, lesson2, teacher1, teacher2, timeslot1, timeslot2)
            .penalizesBy(0);
    }

    // ==================== SOFT CONSTRAINT: Prime-Time Reward ====================

    @Test
    @DisplayName("Prime-Time: Reward for lesson during prime hours")
    void rewardForPrimeTime_lessonAt18() {
        Teacher teacher = createTeacher(1L, "John Doe");
        Timeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, "18:00", "19:00");
        DanceGroup group = createDanceGroup(1L, "Group 1");

        Lesson lesson = createLesson(1L, teacher, group, timeslot, false, true);

        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::rewardPrimeTime)
            .given(lesson, teacher, timeslot, group)
            .rewardsWith(1);
    }

    @Test
    @DisplayName("Prime-Time: Reward for lesson at 16:00")
    void rewardForPrimeTime_lessonAt16() {
        Teacher teacher = createTeacher(1L, "John Doe");
        Timeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, "16:00", "17:00");
        DanceGroup group = createDanceGroup(1L, "Group 1");

        Lesson lesson = createLesson(1L, teacher, group, timeslot, false, true);

        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::rewardPrimeTime)
            .given(lesson, teacher, timeslot, group)
            .rewardsWith(1);
    }

    @Test
    @DisplayName("Prime-Time: No reward for lesson before prime time")
    void noRewardForPrimeTime_lessonAt15() {
        Teacher teacher = createTeacher(1L, "John Doe");
        Timeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, "15:00", "16:00");
        DanceGroup group = createDanceGroup(1L, "Group 1");

        Lesson lesson = createLesson(1L, teacher, group, timeslot, false, true);

        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::rewardPrimeTime)
            .given(lesson, teacher, timeslot, group)
            .rewardsWith(0);
    }

    @Test
    @DisplayName("Prime-Time: No reward for lesson after prime time")
    void noRewardForPrimeTime_lessonAt21() {
        Teacher teacher = createTeacher(1L, "John Doe");
        Timeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, "21:00", "22:00");
        DanceGroup group = createDanceGroup(1L, "Group 1");

        Lesson lesson = createLesson(1L, teacher, group, timeslot, false, true);

        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::rewardPrimeTime)
            .given(lesson, teacher, timeslot, group)
            .rewardsWith(0);
    }

    // ==================== SOFT CONSTRAINT: Load Balancing ====================

    @Test
    @DisplayName("Load Balancing: Penalty for teacher with many lessons")
    void penaltyForLoadBalance_manyLessons() {
        Teacher teacher = createTeacher(1L, "John Doe");
        Timeslot timeslot1 = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        Timeslot timeslot2 = createTimeslot(2L, DayOfWeek.MONDAY, "10:00", "11:00");
        Timeslot timeslot3 = createTimeslot(3L, DayOfWeek.MONDAY, "11:00", "12:00");
        DanceGroup group1 = createDanceGroup(1L, "Group 1");
        DanceGroup group2 = createDanceGroup(2L, "Group 2");
        DanceGroup group3 = createDanceGroup(3L, "Group 3");

        Lesson lesson1 = createLesson(1L, teacher, group1, timeslot1, false, true);
        Lesson lesson2 = createLesson(2L, teacher, group2, timeslot2, false, true);
        Lesson lesson3 = createLesson(3L, teacher, group3, timeslot3, false, true);

        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::balanceTeacherLoad)
            .given(lesson1, lesson2, lesson3, teacher, timeslot1, timeslot2, timeslot3)
            .penalizesBy(9);
    }

    @Test
    @DisplayName("Load Balancing: Lower penalty for balanced distribution")
    void lowerPenaltyForLoadBalance_balanced() {
        Teacher teacher1 = createTeacher(1L, "Teacher 1");
        Teacher teacher2 = createTeacher(2L, "Teacher 2");
        Timeslot timeslot1 = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        Timeslot timeslot2 = createTimeslot(2L, DayOfWeek.MONDAY, "10:00", "11:00");
        DanceGroup group1 = createDanceGroup(1L, "Group 1");
        DanceGroup group2 = createDanceGroup(2L, "Group 2");

        Lesson lesson1 = createLesson(1L, teacher1, group1, timeslot1, false, true);
        Lesson lesson2 = createLesson(2L, teacher2, group2, timeslot2, false, true);

        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::balanceTeacherLoad)
            .given(lesson1, lesson2, teacher1, teacher2, timeslot1, timeslot2)
            .penalizesBy(2);
    }

    // ==================== STUDENT MATCHING CONSTRAINTS ====================

    // --- groupLessonCannotHaveStudent ---

    @Test
    @DisplayName("Group lesson cannot have student: Penalty when group lesson has student assigned")
    void penaltyForGroupLessonWithStudent() {
        Teacher teacher = createTeacher(1L, "John Doe");
        Timeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        DanceGroup group = createDanceGroup(1L, "Beginners");
        Student student = createStudent(100L, "Alice");

        Lesson lesson = createLesson(1L, teacher, group, timeslot, false, true);
        lesson.setStudent(student); // illegally assigned to a group lesson

        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::groupLessonCannotHaveStudent)
            .given(lesson)
            .penalizesBy(1);
    }

    @Test
    @DisplayName("Group lesson cannot have student: No penalty when group lesson has no student")
    void noPenaltyForGroupLessonWithoutStudent() {
        Teacher teacher = createTeacher(1L, "John Doe");
        Timeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        DanceGroup group = createDanceGroup(1L, "Beginners");

        Lesson lesson = createLesson(1L, teacher, group, timeslot, false, true);
        // student is null — correct for group lessons

        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::groupLessonCannotHaveStudent)
            .given(lesson)
            .penalizesBy(0);
    }

    @Test
    @DisplayName("Group lesson cannot have student: No penalty for private lesson with student")
    void noPenaltyForPrivateLessonWithStudent_groupConstraint() {
        Teacher teacher = createTeacher(1L, "John Doe");
        Timeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        Student student = createStudent(100L, "Alice");

        Lesson lesson = createLesson(1L, teacher, null, timeslot, true, false);
        lesson.setStudent(student); // valid for private lesson

        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::groupLessonCannotHaveStudent)
            .given(lesson)
            .penalizesBy(0);
    }

    // --- studentMustBeSubscribedToTeacher ---

    @Test
    @DisplayName("Student must be subscribed: Penalty when student is not in teacher's pool")
    void penaltyForStudentNotSubscribed() {
        Student student = createStudent(100L, "Alice");
        Teacher teacher = createTeacher(1L, "John Doe");
        teacher.setPrivateStudents(new HashSet<>());

        Timeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");

        Lesson lesson = createLesson(1L, teacher, null, timeslot, true, false);
        lesson.setStudent(student);

        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::studentMustBeSubscribedToTeacher)
            .given(lesson)
            .penalizesBy(1);
    }

    @Test
    @DisplayName("Student must be subscribed: No penalty when student IS in teacher's pool")
    void noPenaltyForStudentSubscribed() {
        Student student = createStudent(100L, "Alice");
        Teacher teacher = createTeacher(1L, "John Doe");
        teacher.setPrivateStudents(Set.of(student));

        Timeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");

        Lesson lesson = createLesson(1L, teacher, null, timeslot, true, false);
        lesson.setStudent(student);

        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::studentMustBeSubscribedToTeacher)
            .given(lesson)
            .penalizesBy(0);
    }

    // --- studentConflict ---

    @Test
    @DisplayName("Student conflict: Penalty when same student in two lessons at same time")
    void penaltyForStudentDoubleBooking() {
        Student student = createStudent(100L, "Alice");
        Teacher teacher1 = createTeacher(1L, "John Doe");
        Teacher teacher2 = createTeacher(2L, "Jane Smith");
        Timeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");

        Lesson lesson1 = createLesson(1L, teacher1, null, timeslot, true, false);
        lesson1.setStudent(student);
        Lesson lesson2 = createLesson(2L, teacher2, null, timeslot, true, false);
        lesson2.setStudent(student);

        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::studentConflict)
            .given(lesson1, lesson2)
            .penalizesBy(1);
    }

    @Test
    @DisplayName("Student conflict: No penalty when same student in different timeslots")
    void noPenaltyForStudentDifferentTimeslots() {
        Student student = createStudent(100L, "Alice");
        Teacher teacher1 = createTeacher(1L, "John Doe");
        Teacher teacher2 = createTeacher(2L, "Jane Smith");
        Timeslot timeslot1 = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        Timeslot timeslot2 = createTimeslot(2L, DayOfWeek.MONDAY, "10:00", "11:00");

        Lesson lesson1 = createLesson(1L, teacher1, null, timeslot1, true, false);
        lesson1.setStudent(student);
        Lesson lesson2 = createLesson(2L, teacher2, null, timeslot2, true, false);
        lesson2.setStudent(student);

        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::studentConflict)
            .given(lesson1, lesson2)
            .penalizesBy(0);
    }

    @Test
    @DisplayName("Student conflict: No penalty for different students same timeslot")
    void noPenaltyForDifferentStudentsSameTimeslot() {
        Student student1 = createStudent(100L, "Alice");
        Student student2 = createStudent(101L, "Bob");
        Teacher teacher = createTeacher(1L, "John Doe");
        Timeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");

        Lesson lesson1 = createLesson(1L, teacher, null, timeslot, true, false);
        lesson1.setStudent(student1);
        Lesson lesson2 = createLesson(2L, teacher, null, timeslot, true, false);
        lesson2.setStudent(student2);

        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::studentConflict)
            .given(lesson1, lesson2)
            .penalizesBy(0);
    }

    // --- studentMaxOnePrivateLessonPerDay ---

    @Test
    @DisplayName("Student max one private lesson/day: Penalty when same student has two private lessons on same day")
    void penaltyForStudentTwoPrivateLessonsOnSameDay() {
        Student student = createStudent(100L, "Alice");
        Teacher teacher1 = createTeacher(1L, "John Doe");
        Teacher teacher2 = createTeacher(2L, "Jane Smith");
        Timeslot timeslot1 = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        Timeslot timeslot2 = createTimeslot(2L, DayOfWeek.MONDAY, "10:00", "11:00");

        Lesson lesson1 = createLesson(1L, teacher1, null, timeslot1, true, false);
        lesson1.setStudent(student);
        Lesson lesson2 = createLesson(2L, teacher2, null, timeslot2, true, false);
        lesson2.setStudent(student);

        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::studentMaxOnePrivateLessonPerDay)
            .given(lesson1, lesson2)
            .penalizesBy(1);
    }

    @Test
    @DisplayName("Student max one private lesson/day: No penalty when same student lessons are on different days")
    void noPenaltyForStudentPrivateLessonsOnDifferentDays() {
        Student student = createStudent(100L, "Alice");
        Teacher teacher1 = createTeacher(1L, "John Doe");
        Teacher teacher2 = createTeacher(2L, "Jane Smith");
        Timeslot mondayTimeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        Timeslot tuesdayTimeslot = createTimeslot(2L, DayOfWeek.TUESDAY, "09:00", "10:00");

        Lesson mondayLesson = createLesson(1L, teacher1, null, mondayTimeslot, true, false);
        mondayLesson.setStudent(student);
        Lesson tuesdayLesson = createLesson(2L, teacher2, null, tuesdayTimeslot, true, false);
        tuesdayLesson.setStudent(student);

        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::studentMaxOnePrivateLessonPerDay)
            .given(mondayLesson, tuesdayLesson)
            .penalizesBy(0);
    }

    @Test
    @DisplayName("Student max one private lesson/day: No penalty for different students on same day")
    void noPenaltyForDifferentStudentsPrivateLessonsOnSameDay() {
        Student student1 = createStudent(100L, "Alice");
        Student student2 = createStudent(101L, "Bob");
        Teacher teacher1 = createTeacher(1L, "John Doe");
        Teacher teacher2 = createTeacher(2L, "Jane Smith");
        Timeslot timeslot1 = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        Timeslot timeslot2 = createTimeslot(2L, DayOfWeek.MONDAY, "10:00", "11:00");

        Lesson lesson1 = createLesson(1L, teacher1, null, timeslot1, true, false);
        lesson1.setStudent(student1);
        Lesson lesson2 = createLesson(2L, teacher2, null, timeslot2, true, false);
        lesson2.setStudent(student2);

        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::studentMaxOnePrivateLessonPerDay)
            .given(lesson1, lesson2)
            .penalizesBy(0);
    }

    // --- studentOutsideWeeklyAvailability ---

    @Test
    @DisplayName("Student weekly availability: Penalty when no matching availability window")
    void penaltyForStudentOutsideWeeklyAvailability() {
        Student student = createStudent(100L, "Alice");
        Teacher teacher = createTeacher(1L, "John Doe");
        Timeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");

        Lesson lesson = createLesson(1L, teacher, null, timeslot, true, false);
        lesson.setStudent(student);

        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::studentOutsideWeeklyAvailability)
            .given(lesson)
            .penalizesBy(1);
    }

    @Test
    @DisplayName("Student weekly availability: No penalty when availability covers the lesson")
    void noPenaltyForStudentInsideWeeklyAvailability() {
        Student student = createStudent(100L, "Alice");
        Teacher teacher = createTeacher(1L, "John Doe");
        Timeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");

        Lesson lesson = createLesson(1L, teacher, null, timeslot, true, false);
        lesson.setStudent(student);

        WeeklyAvailability availability = createWeeklyAvailability(1L, student,
                DayOfWeek.MONDAY, "08:00", "12:00");

        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::studentOutsideWeeklyAvailability)
            .given(lesson, availability)
            .penalizesBy(0);
    }

    @Test
    @DisplayName("Student weekly availability: Penalty when availability is on wrong day")
    void penaltyForStudentAvailabilityWrongDay() {
        Student student = createStudent(100L, "Alice");
        Teacher teacher = createTeacher(1L, "John Doe");
        Timeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");

        Lesson lesson = createLesson(1L, teacher, null, timeslot, true, false);
        lesson.setStudent(student);

        WeeklyAvailability availability = createWeeklyAvailability(1L, student,
                DayOfWeek.TUESDAY, "08:00", "12:00");

        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::studentOutsideWeeklyAvailability)
            .given(lesson, availability)
            .penalizesBy(1);
    }

    // --- studentOneTimeUnavailability ---

    @Test
    @DisplayName("Student one-time unavailability: Penalty when lesson overlaps")
    void penaltyForStudentOneTimeUnavailability() {
        Student student = createStudent(100L, "Alice");
        Teacher teacher = createTeacher(1L, "John Doe");
        Timeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");

        Lesson lesson = createLesson(1L, teacher, null, timeslot, true, false);
        lesson.setStudent(student);

        ResourceUnavailability unavailability = createStudentUnavailability(1L, student, timeslot, "Dentist");

        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::studentOneTimeUnavailability)
            .given(lesson, unavailability, SCHEDULE_ANCHOR)
            .penalizesBy(1);
    }

    @Test
    @DisplayName("Student one-time unavailability: No penalty when no overlap")
    void noPenaltyForStudentOneTimeUnavailability_noOverlap() {
        Student student = createStudent(100L, "Alice");
        Teacher teacher = createTeacher(1L, "John Doe");
        Timeslot lessonSlot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        Timeslot unavailSlot = createTimeslot(2L, DayOfWeek.MONDAY, "10:00", "11:00");

        Lesson lesson = createLesson(1L, teacher, null, lessonSlot, true, false);
        lesson.setStudent(student);

        ResourceUnavailability unavailability = createStudentUnavailability(1L, student, unavailSlot, "Dentist");

        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::studentOneTimeUnavailability)
            .given(lesson, unavailability, SCHEDULE_ANCHOR)
            .penalizesBy(0);
    }

    // --- rewardStudentAssignment ---

    @Test
    @DisplayName("Reward student assignment: Reward when private lesson has student")
    void rewardForStudentAssignment() {
        Student student = createStudent(100L, "Alice");
        Teacher teacher = createTeacher(1L, "John Doe");
        Timeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");

        Lesson lesson = createLesson(1L, teacher, null, timeslot, true, false);
        lesson.setStudent(student);

        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::rewardStudentAssignment)
            .given(lesson)
            .rewardsWith(1);
    }

    @Test
    @DisplayName("Reward student assignment: No reward when private lesson has no student")
    void noRewardForUnassignedTemplate() {
        Teacher teacher = createTeacher(1L, "John Doe");
        Timeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");

        Lesson lesson = createLesson(1L, teacher, null, timeslot, true, false);

        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::rewardStudentAssignment)
            .given(lesson)
            .rewardsWith(0);
    }

    @Test
    @DisplayName("Reward student assignment: No reward for group lesson")
    void noRewardForGroupLesson() {
        Teacher teacher = createTeacher(1L, "John Doe");
        Timeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        DanceGroup group = createDanceGroup(1L, "Beginners");

        Lesson lesson = createLesson(1L, teacher, group, timeslot, false, true);

        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::rewardStudentAssignment)
            .given(lesson)
            .rewardsWith(0);
    }

    // ==================== Test Data Builders ====================

    private Timeslot createTimeslot(Long id, DayOfWeek dayOfWeek, String startTime, String endTime) {
        Timeslot timeslot = new Timeslot();
        timeslot.setId(id);
        timeslot.setDayOfWeek(dayOfWeek);
        timeslot.setStartTime(LocalTime.parse(startTime));
        timeslot.setEndTime(LocalTime.parse(endTime));
        return timeslot;
    }

    private Teacher createTeacher(Long id, String fullName) {
        AbstractUser user = new AbstractUser() {};
        user.setId(id);
        user.setEmail(fullName.toLowerCase().replace(" ", ".") + "@example.com");
        user.setPasswordHash("hashedPassword");
        user.setFullName(fullName);

        Teacher teacher = new Teacher();
        teacher.setId(id);
        teacher.setUser(user);
        teacher.setMaxDailyHours(8);
        teacher.setColorCode("#FF5733");
        return teacher;
    }

    private DanceGroup createDanceGroup(Long id, String name) {
        DanceGroup group = new DanceGroup();
        group.setId(id);
        group.setName(name);
        return group;
    }

    /**
     * Creates a Lesson without Room (room is no longer a planning variable).
     */
    private Lesson createLesson(Long id, Teacher teacher, DanceGroup group,
                               Timeslot timeslot,
                               boolean isPrivate, boolean isPinned) {
        Lesson lesson = new Lesson();
        lesson.setId(id);
        lesson.setTeacher(teacher);
        lesson.setDanceGroup(group);
        lesson.setTimeslot(timeslot);
        lesson.setDurationMinutes(60);
        lesson.setPrivate(isPrivate);
        lesson.setPinned(isPinned);
        return lesson;
    }

    private ResourceUnavailability createUnavailability(Long id, Teacher teacher,
                                                       Timeslot timeslot, String reason) {
        ResourceUnavailability unavailability = new ResourceUnavailability();
        unavailability.setId(id);
        unavailability.setUser(teacher.getUser());
        int shift = timeslot.getDayOfWeek().getValue() - SCHEDULE_ANCHOR.getDayOfWeek().getValue();
        if (shift < 0) shift += 7;
        unavailability.setDate(SCHEDULE_ANCHOR.plusDays(shift));
        unavailability.setStartTime(timeslot.getStartTime());
        unavailability.setEndTime(timeslot.getEndTime());
        unavailability.setReason(reason);
        return unavailability;
    }

    private Student createStudent(Long id, String fullName) {
        Student student = new Student();
        student.setId(id);
        student.setEmail(fullName.toLowerCase().replace(" ", ".") + "@student.example.com");
        student.setPasswordHash("hashedPassword");
        student.setFullName(fullName);
        return student;
    }

    private WeeklyAvailability createWeeklyAvailability(Long id, AbstractUser user,
                                                         DayOfWeek dayOfWeek, String startTime, String endTime) {
        WeeklyAvailability availability = new WeeklyAvailability();
        availability.setId(id);
        availability.setUser(user);
        availability.setDayOfWeek(dayOfWeek);
        availability.setStartTime(LocalTime.parse(startTime));
        availability.setEndTime(LocalTime.parse(endTime));
        return availability;
    }

    private ResourceUnavailability createStudentUnavailability(Long id, Student student,
                                                                Timeslot timeslot, String reason) {
        ResourceUnavailability unavailability = new ResourceUnavailability();
        unavailability.setId(id);
        unavailability.setUser(student);
        int shift = timeslot.getDayOfWeek().getValue() - SCHEDULE_ANCHOR.getDayOfWeek().getValue();
        if (shift < 0) shift += 7;
        unavailability.setDate(SCHEDULE_ANCHOR.plusDays(shift));
        unavailability.setStartTime(timeslot.getStartTime());
        unavailability.setEndTime(timeslot.getEndTime());
        unavailability.setReason(reason);
        return unavailability;
    }
}
