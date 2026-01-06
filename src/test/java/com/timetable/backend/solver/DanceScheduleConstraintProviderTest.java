package com.timetable.backend.solver;

import ai.timefold.solver.test.api.score.stream.ConstraintVerifier;
import com.timetable.backend.domain.model.DanceLevel;
import com.timetable.backend.solver.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Unit tests for DanceScheduleConstraintProvider.
 * Uses Timefold's ConstraintVerifier to test each constraint in isolation.
 *
 * REFACTORED: Now uses Planning POJOs instead of JPA entities.
 */
class DanceScheduleConstraintProviderTest {

    private ConstraintVerifier<DanceScheduleConstraintProvider, TimetableSolution> constraintVerifier;

    @BeforeEach
    void setUp() {
        constraintVerifier = ConstraintVerifier.build(
            new DanceScheduleConstraintProvider(),
            TimetableSolution.class,
            PlanningLesson.class
        );
    }

    // ==================== HARD CONSTRAINT 1: PlanningRoom Conflict ====================

    @Test
    @DisplayName("PlanningRoom conflict (weighted): Two group lessons exceed capacity")
    void penaltyForRoomConflict_twoGroupLessons() {
        // Given: Two group lessons (100 + 100 = 200, exceeds 100)
        PlanningRoom PlanningRoom = createRoom(1L, "Studio A", 20, false);
        PlanningTimeslot PlanningTimeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        PlanningTeacher teacher1 = createTeacher(1L, "John Doe");
        PlanningTeacher teacher2 = createTeacher(2L, "Jane Smith");
        PlanningDanceGroup group1 = createDanceGroup(1L, "Beginners Salsa");
        PlanningDanceGroup group2 = createDanceGroup(2L, "Intermediate Bachata");

        PlanningLesson lesson1 = createLesson(1L, teacher1, group1, PlanningTimeslot, PlanningRoom, false, false);
        PlanningLesson lesson2 = createLesson(2L, teacher2, group2, PlanningTimeslot, PlanningRoom, false, false);

        // When/Then: Should penalize with 100 HARD (200 - 100 = 100 excess)
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::roomConflict)
            .given(lesson1, lesson2, PlanningRoom, PlanningTimeslot)
            .penalizesBy(100);
    }

    @Test
    @DisplayName("PlanningRoom conflict (weighted): Four private lessons within capacity")
    void noPenaltyForRoomConflict_fourPrivateLessons() {
        // Given: Four private lessons (25 * 4 = 100, exactly at capacity)
        PlanningRoom PlanningRoom = createRoom(1L, "Studio B", 15, true);
        PlanningTimeslot PlanningTimeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        PlanningTeacher teacher1 = createTeacher(1L, "PlanningTeacher 1");
        PlanningTeacher teacher2 = createTeacher(2L, "PlanningTeacher 2");
        PlanningTeacher teacher3 = createTeacher(3L, "PlanningTeacher 3");
        PlanningTeacher teacher4 = createTeacher(4L, "PlanningTeacher 4");
        PlanningDanceGroup group1 = createDanceGroup(1L, "Private 1");
        PlanningDanceGroup group2 = createDanceGroup(2L, "Private 2");
        PlanningDanceGroup group3 = createDanceGroup(3L, "Private 3");
        PlanningDanceGroup group4 = createDanceGroup(4L, "Private 4");

        PlanningLesson lesson1 = createLesson(1L, teacher1, group1, PlanningTimeslot, PlanningRoom, true, false);
        PlanningLesson lesson2 = createLesson(2L, teacher2, group2, PlanningTimeslot, PlanningRoom, true, false);
        PlanningLesson lesson3 = createLesson(3L, teacher3, group3, PlanningTimeslot, PlanningRoom, true, false);
        PlanningLesson lesson4 = createLesson(4L, teacher4, group4, PlanningTimeslot, PlanningRoom, true, false);

        // When/Then: No penalty (exactly at 100% capacity)
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::roomConflict)
            .given(lesson1, lesson2, lesson3, lesson4, PlanningRoom, PlanningTimeslot)
            .penalizesBy(0);
    }

    @Test
    @DisplayName("PlanningRoom conflict (weighted): Five private lessons exceed capacity")
    void penaltyForRoomConflict_fivePrivateLessons() {
        // Given: Five private lessons (25 * 5 = 125, exceeds 100)
        PlanningRoom PlanningRoom = createRoom(1L, "Studio B", 15, true);
        PlanningTimeslot PlanningTimeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        PlanningTeacher teacher1 = createTeacher(1L, "PlanningTeacher 1");
        PlanningTeacher teacher2 = createTeacher(2L, "PlanningTeacher 2");
        PlanningTeacher teacher3 = createTeacher(3L, "PlanningTeacher 3");
        PlanningTeacher teacher4 = createTeacher(4L, "PlanningTeacher 4");
        PlanningTeacher teacher5 = createTeacher(5L, "PlanningTeacher 5");

        PlanningLesson lesson1 = createLesson(1L, teacher1, null, PlanningTimeslot, PlanningRoom, true, false);
        PlanningLesson lesson2 = createLesson(2L, teacher2, null, PlanningTimeslot, PlanningRoom, true, false);
        PlanningLesson lesson3 = createLesson(3L, teacher3, null, PlanningTimeslot, PlanningRoom, true, false);
        PlanningLesson lesson4 = createLesson(4L, teacher4, null, PlanningTimeslot, PlanningRoom, true, false);
        PlanningLesson lesson5 = createLesson(5L, teacher5, null, PlanningTimeslot, PlanningRoom, true, false);

        // When/Then: Should penalize with 25 HARD (125 - 100 = 25 excess)
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::roomConflict)
            .given(lesson1, lesson2, lesson3, lesson4, lesson5, PlanningRoom, PlanningTimeslot)
            .penalizesBy(25);
    }

    @Test
    @DisplayName("PlanningRoom conflict (weighted): Group + Private exceeds capacity")
    void penaltyForRoomConflict_groupPlusPrivate() {
        // Given: One group (100) + one private (25) = 125, exceeds 100
        PlanningRoom PlanningRoom = createRoom(1L, "Studio C", 15, true);
        PlanningTimeslot PlanningTimeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        PlanningTeacher teacher1 = createTeacher(1L, "John Doe");
        PlanningTeacher teacher2 = createTeacher(2L, "Jane Smith");
        PlanningDanceGroup group1 = createDanceGroup(1L, "Private");
        PlanningDanceGroup group2 = createDanceGroup(2L, "Group");

        PlanningLesson lesson1 = createLesson(1L, teacher1, group1, PlanningTimeslot, PlanningRoom, true, false);  // private = 25
        PlanningLesson lesson2 = createLesson(2L, teacher2, group2, PlanningTimeslot, PlanningRoom, false, false); // group = 100

        // When/Then: Should penalize with 25 HARD (125 - 100 = 25 excess)
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::roomConflict)
            .given(lesson1, lesson2, PlanningRoom, PlanningTimeslot)
            .penalizesBy(25);
    }

    @Test
    @DisplayName("PlanningRoom conflict (weighted): Three private lessons within capacity")
    void noPenaltyForRoomConflict_threePrivateLessons() {
        // Given: Three private lessons (25 * 3 = 75, under 100)
        PlanningRoom PlanningRoom = createRoom(1L, "Studio B", 15, true);
        PlanningTimeslot PlanningTimeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        PlanningTeacher teacher1 = createTeacher(1L, "PlanningTeacher 1");
        PlanningTeacher teacher2 = createTeacher(2L, "PlanningTeacher 2");
        PlanningTeacher teacher3 = createTeacher(3L, "PlanningTeacher 3");
        PlanningDanceGroup group1 = createDanceGroup(1L, "Private 1");
        PlanningDanceGroup group2 = createDanceGroup(2L, "Private 2");
        PlanningDanceGroup group3 = createDanceGroup(3L, "Private 3");

        PlanningLesson lesson1 = createLesson(1L, teacher1, group1, PlanningTimeslot, PlanningRoom, true, false);
        PlanningLesson lesson2 = createLesson(2L, teacher2, group2, PlanningTimeslot, PlanningRoom, true, false);
        PlanningLesson lesson3 = createLesson(3L, teacher3, group3, PlanningTimeslot, PlanningRoom, true, false);

        // When/Then: No penalty (75 < 100)
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::roomConflict)
            .given(lesson1, lesson2, lesson3, PlanningRoom, PlanningTimeslot)
            .penalizesBy(0);
    }

    // ==================== HARD CONSTRAINT 2: PlanningTeacher Conflict ====================

    @Test
    @DisplayName("PlanningTeacher conflict: PlanningTeacher cannot teach two lessons simultaneously")
    void penaltyForTeacherConflict() {
        // Given: Same PlanningTeacher, same PlanningTimeslot, different rooms
        PlanningTeacher PlanningTeacher = createTeacher(1L, "John Doe");
        PlanningRoom room1 = createRoom(1L, "Studio A", 20, false);
        PlanningRoom room2 = createRoom(2L, "Studio B", 15, false);
        PlanningTimeslot PlanningTimeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        PlanningDanceGroup group1 = createDanceGroup(1L, "Group 1");
        PlanningDanceGroup group2 = createDanceGroup(2L, "Group 2");

        PlanningLesson lesson1 = createLesson(1L, PlanningTeacher, group1, PlanningTimeslot, room1, false, false);
        PlanningLesson lesson2 = createLesson(2L, PlanningTeacher, group2, PlanningTimeslot, room2, false, false);

        // When/Then: Should penalize with 1 HARD
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::teacherConflict)
            .given(lesson1, lesson2, PlanningTeacher, PlanningTimeslot, room1, room2)
            .penalizesBy(1);
    }

    @Test
    @DisplayName("PlanningTeacher conflict: No penalty for different timeslots")
    void noPenaltyForTeacherConflict_differentTimeslots() {
        // Given: Same PlanningTeacher, different timeslots
        PlanningTeacher PlanningTeacher = createTeacher(1L, "John Doe");
        PlanningRoom room1 = createRoom(1L, "Studio A", 20, false);
        PlanningRoom room2 = createRoom(2L, "Studio B", 15, false);
        PlanningTimeslot timeslot1 = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        PlanningTimeslot timeslot2 = createTimeslot(2L, DayOfWeek.MONDAY, "10:00", "11:00");
        PlanningDanceGroup group1 = createDanceGroup(1L, "Group 1");
        PlanningDanceGroup group2 = createDanceGroup(2L, "Group 2");

        PlanningLesson lesson1 = createLesson(1L, PlanningTeacher, group1, timeslot1, room1, false, false);
        PlanningLesson lesson2 = createLesson(2L, PlanningTeacher, group2, timeslot2, room2, false, false);

        // When/Then: No conflict (different times)
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::teacherConflict)
            .given(lesson1, lesson2, PlanningTeacher, timeslot1, timeslot2, room1, room2)
            .penalizesBy(0);
    }

    @Test
    @DisplayName("PlanningTeacher conflict: No penalty for different teachers")
    void noPenaltyForTeacherConflict_differentTeachers() {
        // Given: Different teachers, same PlanningTimeslot
        PlanningTeacher teacher1 = createTeacher(1L, "John Doe");
        PlanningTeacher teacher2 = createTeacher(2L, "Jane Smith");
        PlanningRoom PlanningRoom = createRoom(1L, "Studio A", 20, false);
        PlanningTimeslot PlanningTimeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        PlanningDanceGroup group1 = createDanceGroup(1L, "Group 1");
        PlanningDanceGroup group2 = createDanceGroup(2L, "Group 2");

        PlanningLesson lesson1 = createLesson(1L, teacher1, group1, PlanningTimeslot, PlanningRoom, false, false);
        PlanningLesson lesson2 = createLesson(2L, teacher2, group2, PlanningTimeslot, PlanningRoom, false, false);

        // When/Then: No PlanningTeacher conflict (different teachers)
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::teacherConflict)
            .given(lesson1, lesson2, teacher1, teacher2, PlanningTimeslot, PlanningRoom)
            .penalizesBy(0);
    }

    // ==================== HARD CONSTRAINT 3: PlanningTeacher Availability ====================

    @Test
    @DisplayName("PlanningTeacher availability: Penalty when PlanningLesson scheduled during unavailable time")
    void penaltyForTeacherUnavailability() {
        // Given: PlanningTeacher unavailable on Monday 9:00-10:00
        PlanningTeacher PlanningTeacher = createTeacher(1L, "John Doe");
        PlanningTimeslot PlanningTimeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        PlanningRoom PlanningRoom = createRoom(1L, "Studio A", 20, false);
        PlanningDanceGroup group = createDanceGroup(1L, "Group 1");

        PlanningLesson PlanningLesson = createLesson(1L, PlanningTeacher, group, PlanningTimeslot, PlanningRoom, false, false);
        PlanningResourceUnavailability unavailability = createUnavailability(1L, PlanningTeacher.getId(), PlanningTimeslot.getId(), "Vacation");

        // When/Then: Should penalize with 1 HARD
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::teacherAvailability)
            .given(PlanningLesson, unavailability)
            .penalizesBy(1);
    }

    @Test
    @DisplayName("PlanningTeacher availability: No penalty when PlanningTeacher is available")
    void noPenaltyForTeacherAvailability_teacherAvailable() {
        // Given: PlanningTeacher available (no unavailability record)
        PlanningTeacher PlanningTeacher = createTeacher(1L, "John Doe");
        PlanningTimeslot PlanningTimeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        PlanningRoom PlanningRoom = createRoom(1L, "Studio A", 20, false);
        PlanningDanceGroup group = createDanceGroup(1L, "Group 1");

        PlanningLesson PlanningLesson = createLesson(1L, PlanningTeacher, group, PlanningTimeslot, PlanningRoom, false, false);

        // When/Then: No penalty (PlanningTeacher is available)
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::teacherAvailability)
            .given(PlanningLesson)
            .penalizesBy(0);
    }

    @Test
    @DisplayName("PlanningTeacher availability: No penalty for different PlanningTimeslot")
    void noPenaltyForTeacherAvailability_differentTimeslot() {
        // Given: PlanningTeacher unavailable at 9:00, but PlanningLesson is at 10:00
        PlanningTeacher PlanningTeacher = createTeacher(1L, "John Doe");
        PlanningTimeslot unavailableSlot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        PlanningTimeslot lessonSlot = createTimeslot(2L, DayOfWeek.MONDAY, "10:00", "11:00");
        PlanningRoom PlanningRoom = createRoom(1L, "Studio A", 20, false);
        PlanningDanceGroup group = createDanceGroup(1L, "Group 1");

        PlanningLesson PlanningLesson = createLesson(1L, PlanningTeacher, group, lessonSlot, PlanningRoom, false, false);
        PlanningResourceUnavailability unavailability = createUnavailability(1L, PlanningTeacher.getId(), unavailableSlot.getId(), "Meeting");

        // When/Then: No penalty (different PlanningTimeslot)
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::teacherAvailability)
            .given(PlanningLesson, unavailability)
            .penalizesBy(0);
    }

    // ==================== SOFT CONSTRAINT: Minimize PlanningTeacher Gaps ====================

    @Test
    @DisplayName("Minimize gaps: Penalty proportional to gap duration")
    void penaltyForTeacherGaps_proportionalToGapDuration() {
        // Given: PlanningTeacher has lessons at 9:00-10:00 and 12:00-13:00 (2 hours = 120 min gap)
        PlanningTeacher PlanningTeacher = createTeacher(1L, "John Doe");
        PlanningRoom room1 = createRoom(1L, "Studio A", 20, false);
        PlanningRoom room2 = createRoom(2L, "Studio B", 15, false);
        PlanningTimeslot timeslot1 = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        PlanningTimeslot timeslot2 = createTimeslot(2L, DayOfWeek.MONDAY, "12:00", "13:00");
        PlanningDanceGroup group1 = createDanceGroup(1L, "Group 1");
        PlanningDanceGroup group2 = createDanceGroup(2L, "Group 2");

        PlanningLesson lesson1 = createLesson(1L, PlanningTeacher, group1, timeslot1, room1, false, false);
        PlanningLesson lesson2 = createLesson(2L, PlanningTeacher, group2, timeslot2, room2, false, false);

        // When/Then: Should penalize with 120 SOFT (120 minutes gap)
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::minimizeTeacherGaps)
            .given(lesson1, lesson2, PlanningTeacher, timeslot1, timeslot2, room1, room2)
            .penalizesBy(120);
    }

    @Test
    @DisplayName("Minimize gaps: No penalty for consecutive lessons")
    void noPenaltyForTeacherGaps_consecutiveLessons() {
        // Given: PlanningTeacher has consecutive lessons (9:00-10:00, 10:00-11:00)
        PlanningTeacher PlanningTeacher = createTeacher(1L, "John Doe");
        PlanningRoom room1 = createRoom(1L, "Studio A", 20, false);
        PlanningRoom room2 = createRoom(2L, "Studio B", 15, false);
        PlanningTimeslot timeslot1 = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        PlanningTimeslot timeslot2 = createTimeslot(2L, DayOfWeek.MONDAY, "10:00", "11:00");
        PlanningDanceGroup group1 = createDanceGroup(1L, "Group 1");
        PlanningDanceGroup group2 = createDanceGroup(2L, "Group 2");

        PlanningLesson lesson1 = createLesson(1L, PlanningTeacher, group1, timeslot1, room1, false, false);
        PlanningLesson lesson2 = createLesson(2L, PlanningTeacher, group2, timeslot2, room2, false, false);

        // When/Then: No penalty (no gap)
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::minimizeTeacherGaps)
            .given(lesson1, lesson2, PlanningTeacher, timeslot1, timeslot2, room1, room2)
            .penalizesBy(0);
    }

    @Test
    @DisplayName("Minimize gaps: No penalty for different days")
    void noPenaltyForTeacherGaps_differentDays() {
        // Given: PlanningTeacher has lessons on different days
        PlanningTeacher PlanningTeacher = createTeacher(1L, "John Doe");
        PlanningRoom room1 = createRoom(1L, "Studio A", 20, false);
        PlanningRoom room2 = createRoom(2L, "Studio B", 15, false);
        PlanningTimeslot timeslot1 = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        PlanningTimeslot timeslot2 = createTimeslot(2L, DayOfWeek.TUESDAY, "09:00", "10:00");
        PlanningDanceGroup group1 = createDanceGroup(1L, "Group 1");
        PlanningDanceGroup group2 = createDanceGroup(2L, "Group 2");

        PlanningLesson lesson1 = createLesson(1L, PlanningTeacher, group1, timeslot1, room1, false, false);
        PlanningLesson lesson2 = createLesson(2L, PlanningTeacher, group2, timeslot2, room2, false, false);

        // When/Then: No penalty (different days)
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::minimizeTeacherGaps)
            .given(lesson1, lesson2, PlanningTeacher, timeslot1, timeslot2, room1, room2)
            .penalizesBy(0);
    }

    @Test
    @DisplayName("Minimize gaps: No penalty for different teachers")
    void noPenaltyForTeacherGaps_differentTeachers() {
        // Given: Different teachers on same day
        PlanningTeacher teacher1 = createTeacher(1L, "John Doe");
        PlanningTeacher teacher2 = createTeacher(2L, "Jane Smith");
        PlanningRoom PlanningRoom = createRoom(1L, "Studio A", 20, false);
        PlanningTimeslot timeslot1 = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        PlanningTimeslot timeslot2 = createTimeslot(2L, DayOfWeek.MONDAY, "12:00", "13:00");
        PlanningDanceGroup group1 = createDanceGroup(1L, "Group 1");
        PlanningDanceGroup group2 = createDanceGroup(2L, "Group 2");

        PlanningLesson lesson1 = createLesson(1L, teacher1, group1, timeslot1, PlanningRoom, false, false);
        PlanningLesson lesson2 = createLesson(2L, teacher2, group2, timeslot2, PlanningRoom, false, false);

        // When/Then: No penalty (different teachers)
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::minimizeTeacherGaps)
            .given(lesson1, lesson2, teacher1, teacher2, timeslot1, timeslot2, PlanningRoom)
            .penalizesBy(0);
    }

    // ==================== SOFT CONSTRAINT: Prime-Time Reward ====================

    @Test
    @DisplayName("Prime-Time: Reward for PlanningLesson during prime hours")
    void rewardForPrimeTime_lessonAt18() {
        // Given: PlanningLesson at 18:00 (within 16:00-21:00 prime time)
        PlanningTeacher PlanningTeacher = createTeacher(1L, "John Doe");
        PlanningRoom PlanningRoom = createRoom(1L, "Studio A", 20, false);
        PlanningTimeslot PlanningTimeslot = createTimeslot(1L, DayOfWeek.MONDAY, "18:00", "19:00");
        PlanningDanceGroup group = createDanceGroup(1L, "Group 1");

        PlanningLesson PlanningLesson = createLesson(1L, PlanningTeacher, group, PlanningTimeslot, PlanningRoom, false, false);

        // When/Then: Should reward with 1 SOFT
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::rewardPrimeTime)
            .given(PlanningLesson, PlanningTeacher, PlanningRoom, PlanningTimeslot, group)
            .rewardsWith(1);
    }

    @Test
    @DisplayName("Prime-Time: Reward for PlanningLesson at 16:00")
    void rewardForPrimeTime_lessonAt16() {
        // Given: PlanningLesson at 16:00 (start of prime time)
        PlanningTeacher PlanningTeacher = createTeacher(1L, "John Doe");
        PlanningRoom PlanningRoom = createRoom(1L, "Studio A", 20, false);
        PlanningTimeslot PlanningTimeslot = createTimeslot(1L, DayOfWeek.MONDAY, "16:00", "17:00");
        PlanningDanceGroup group = createDanceGroup(1L, "Group 1");

        PlanningLesson PlanningLesson = createLesson(1L, PlanningTeacher, group, PlanningTimeslot, PlanningRoom, false, false);

        // When/Then: Should reward with 1 SOFT
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::rewardPrimeTime)
            .given(PlanningLesson, PlanningTeacher, PlanningRoom, PlanningTimeslot, group)
            .rewardsWith(1);
    }

    @Test
    @DisplayName("Prime-Time: No reward for PlanningLesson before prime time")
    void noRewardForPrimeTime_lessonAt15() {
        // Given: PlanningLesson at 15:00 (before prime time)
        PlanningTeacher PlanningTeacher = createTeacher(1L, "John Doe");
        PlanningRoom PlanningRoom = createRoom(1L, "Studio A", 20, false);
        PlanningTimeslot PlanningTimeslot = createTimeslot(1L, DayOfWeek.MONDAY, "15:00", "16:00");
        PlanningDanceGroup group = createDanceGroup(1L, "Group 1");

        PlanningLesson PlanningLesson = createLesson(1L, PlanningTeacher, group, PlanningTimeslot, PlanningRoom, false, false);

        // When/Then: No reward
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::rewardPrimeTime)
            .given(PlanningLesson, PlanningTeacher, PlanningRoom, PlanningTimeslot, group)
            .rewardsWith(0);
    }

    @Test
    @DisplayName("Prime-Time: No reward for PlanningLesson after prime time")
    void noRewardForPrimeTime_lessonAt21() {
        // Given: PlanningLesson at 21:00 (after prime time)
        PlanningTeacher PlanningTeacher = createTeacher(1L, "John Doe");
        PlanningRoom PlanningRoom = createRoom(1L, "Studio A", 20, false);
        PlanningTimeslot PlanningTimeslot = createTimeslot(1L, DayOfWeek.MONDAY, "21:00", "22:00");
        PlanningDanceGroup group = createDanceGroup(1L, "Group 1");

        PlanningLesson PlanningLesson = createLesson(1L, PlanningTeacher, group, PlanningTimeslot, PlanningRoom, false, false);

        // When/Then: No reward
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::rewardPrimeTime)
            .given(PlanningLesson, PlanningTeacher, PlanningRoom, PlanningTimeslot, group)
            .rewardsWith(0);
    }

    // ==================== SOFT CONSTRAINT: Load Balancing ====================

    @Test
    @DisplayName("Load Balancing: Penalty for PlanningTeacher with many lessons")
    void penaltyForLoadBalance_manyLessons() {
        // Given: One PlanningTeacher with 3 lessons (penalty = 9 for one PlanningTeacher: 3*3)
        PlanningTeacher PlanningTeacher = createTeacher(1L, "John Doe");
        PlanningRoom PlanningRoom = createRoom(1L, "Studio A", 20, false);
        PlanningTimeslot timeslot1 = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        PlanningTimeslot timeslot2 = createTimeslot(2L, DayOfWeek.MONDAY, "10:00", "11:00");
        PlanningTimeslot timeslot3 = createTimeslot(3L, DayOfWeek.MONDAY, "11:00", "12:00");
        PlanningDanceGroup group1 = createDanceGroup(1L, "Group 1");
        PlanningDanceGroup group2 = createDanceGroup(2L, "Group 2");
        PlanningDanceGroup group3 = createDanceGroup(3L, "Group 3");

        PlanningLesson lesson1 = createLesson(1L, PlanningTeacher, group1, timeslot1, PlanningRoom, false, false);
        PlanningLesson lesson2 = createLesson(2L, PlanningTeacher, group2, timeslot2, PlanningRoom, false, false);
        PlanningLesson lesson3 = createLesson(3L, PlanningTeacher, group3, timeslot3, PlanningRoom, false, false);

        // When/Then: Should penalize with 9 SOFT (3 * 3 = 9)
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::balanceTeacherLoad)
            .given(lesson1, lesson2, lesson3, PlanningTeacher, PlanningRoom, timeslot1, timeslot2, timeslot3)
            .penalizesBy(9);
    }

    @Test
    @DisplayName("Load Balancing: Lower penalty for balanced distribution")
    void lowerPenaltyForLoadBalance_balanced() {
        // Given: Two teachers with 1 PlanningLesson each (total penalty = 2: 1*1 + 1*1)
        PlanningTeacher teacher1 = createTeacher(1L, "PlanningTeacher 1");
        PlanningTeacher teacher2 = createTeacher(2L, "PlanningTeacher 2");
        PlanningRoom PlanningRoom = createRoom(1L, "Studio A", 20, false);
        PlanningTimeslot timeslot1 = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        PlanningTimeslot timeslot2 = createTimeslot(2L, DayOfWeek.MONDAY, "10:00", "11:00");
        PlanningDanceGroup group1 = createDanceGroup(1L, "Group 1");
        PlanningDanceGroup group2 = createDanceGroup(2L, "Group 2");

        PlanningLesson lesson1 = createLesson(1L, teacher1, group1, timeslot1, PlanningRoom, false, false);
        PlanningLesson lesson2 = createLesson(2L, teacher2, group2, timeslot2, PlanningRoom, false, false);

        // When/Then: Should penalize with 2 SOFT (1*1 + 1*1 = 2)
        // This is LESS than having one PlanningTeacher with 2 lessons (2*2 = 4)
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::balanceTeacherLoad)
            .given(lesson1, lesson2, teacher1, teacher2, PlanningRoom, timeslot1, timeslot2)
            .penalizesBy(2);
    }

    // ==================== Test Data Builders (Planning POJOs) ====================

    private PlanningRoom createRoom(Long id, String name, int capacity, boolean allowsParallelPrivate) {
        return new PlanningRoom(id, name, capacity, allowsParallelPrivate);
    }

    private PlanningTimeslot createTimeslot(Long id, DayOfWeek dayOfWeek, String startTime, String endTime) {
        return new PlanningTimeslot(
            id,
            dayOfWeek,
            LocalTime.parse(startTime),
            LocalTime.parse(endTime)
        );
    }

    private PlanningTeacher createTeacher(Long id, String fullName) {
        return new PlanningTeacher(
            id,
            fullName,
            fullName.toLowerCase().replace(" ", ".") + "@example.com",
            8,
            "#FF5733"
        );
    }

    private PlanningDanceGroup createDanceGroup(Long id, String name) {
        return new PlanningDanceGroup(id, name, null, DanceLevel.BEGINNER, 5);
    }

    private PlanningLesson createLesson(Long id, PlanningTeacher PlanningTeacher, PlanningDanceGroup group,
                                       PlanningTimeslot PlanningTimeslot, PlanningRoom PlanningRoom,
                                       boolean isPrivate, boolean isPinned) {
        PlanningLesson PlanningLesson = new PlanningLesson();
        PlanningLesson.setId(id);
        PlanningLesson.setTeacher(PlanningTeacher);
        PlanningLesson.setDanceGroup(group);
        PlanningLesson.setTimeslot(PlanningTimeslot);
        PlanningLesson.setRoom(PlanningRoom);
        PlanningLesson.setDurationMinutes(60);
        PlanningLesson.setPrivate(isPrivate);
        PlanningLesson.setPinned(isPinned);
        return PlanningLesson;
    }

    private PlanningResourceUnavailability createUnavailability(Long id, Long teacherId,
                                                                Long timeslotId, String reason) {
        return new PlanningResourceUnavailability(id, teacherId, timeslotId, reason);
    }
}

