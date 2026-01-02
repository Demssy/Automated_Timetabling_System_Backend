package com.timetable.backend.solver;

import ai.timefold.solver.test.api.score.stream.ConstraintVerifier;
import com.timetable.backend.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Unit tests for DanceScheduleConstraintProvider.
 * Uses Timefold's ConstraintVerifier to test each constraint in isolation.
 */
class DanceScheduleConstraintProviderTest {

    private ConstraintVerifier<DanceScheduleConstraintProvider, DanceSchedule> constraintVerifier;

    @BeforeEach
    void setUp() {
        constraintVerifier = ConstraintVerifier.build(
            new DanceScheduleConstraintProvider(),
            DanceSchedule.class,
            Lesson.class
        );
    }

    // ==================== HARD CONSTRAINT 1: Room Conflict ====================

    @Test
    @DisplayName("Room conflict (weighted): Two group lessons exceed capacity")
    void penaltyForRoomConflict_twoGroupLessons() {
        // Given: Two group lessons (100 + 100 = 200, exceeds 100)
        Room room = createRoom(1L, "Studio A", 20, false);
        Timeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        Teacher teacher1 = createTeacher(1L, "John Doe");
        Teacher teacher2 = createTeacher(2L, "Jane Smith");
        DanceGroup group1 = createDanceGroup(1L, "Beginners Salsa");
        DanceGroup group2 = createDanceGroup(2L, "Intermediate Bachata");

        Lesson lesson1 = createLesson(1L, teacher1, group1, timeslot, room, false, false);
        Lesson lesson2 = createLesson(2L, teacher2, group2, timeslot, room, false, false);

        // When/Then: Should penalize with 100 HARD (200 - 100 = 100 excess)
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::roomConflict)
            .given(lesson1, lesson2, room, timeslot)
            .penalizesBy(100);
    }

    @Test
    @DisplayName("Room conflict (weighted): Four private lessons within capacity")
    void noPenaltyForRoomConflict_fourPrivateLessons() {
        // Given: Four private lessons (25 * 4 = 100, exactly at capacity)
        Room room = createRoom(1L, "Studio B", 15, true);
        Timeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        Teacher teacher1 = createTeacher(1L, "Teacher 1");
        Teacher teacher2 = createTeacher(2L, "Teacher 2");
        Teacher teacher3 = createTeacher(3L, "Teacher 3");
        Teacher teacher4 = createTeacher(4L, "Teacher 4");
        DanceGroup group1 = createDanceGroup(1L, "Private 1");
        DanceGroup group2 = createDanceGroup(2L, "Private 2");
        DanceGroup group3 = createDanceGroup(3L, "Private 3");
        DanceGroup group4 = createDanceGroup(4L, "Private 4");

        Lesson lesson1 = createLesson(1L, teacher1, group1, timeslot, room, true, false);
        Lesson lesson2 = createLesson(2L, teacher2, group2, timeslot, room, true, false);
        Lesson lesson3 = createLesson(3L, teacher3, group3, timeslot, room, true, false);
        Lesson lesson4 = createLesson(4L, teacher4, group4, timeslot, room, true, false);

        // When/Then: No penalty (exactly at 100% capacity)
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::roomConflict)
            .given(lesson1, lesson2, lesson3, lesson4, room, timeslot)
            .penalizesBy(0);
    }

    @Test
    @DisplayName("Room conflict (weighted): Five private lessons exceed capacity")
    void penaltyForRoomConflict_fivePrivateLessons() {
        // Given: Five private lessons (25 * 5 = 125, exceeds 100)
        Room room = createRoom(1L, "Studio B", 15, true);
        Timeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        Teacher teacher1 = createTeacher(1L, "Teacher 1");
        Teacher teacher2 = createTeacher(2L, "Teacher 2");
        Teacher teacher3 = createTeacher(3L, "Teacher 3");
        Teacher teacher4 = createTeacher(4L, "Teacher 4");
        Teacher teacher5 = createTeacher(5L, "Teacher 5");

        Lesson lesson1 = createLesson(1L, teacher1, null, timeslot, room, true, false);
        Lesson lesson2 = createLesson(2L, teacher2, null, timeslot, room, true, false);
        Lesson lesson3 = createLesson(3L, teacher3, null, timeslot, room, true, false);
        Lesson lesson4 = createLesson(4L, teacher4, null, timeslot, room, true, false);
        Lesson lesson5 = createLesson(5L, teacher5, null, timeslot, room, true, false);

        // When/Then: Should penalize with 25 HARD (125 - 100 = 25 excess)
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::roomConflict)
            .given(lesson1, lesson2, lesson3, lesson4, lesson5, room, timeslot)
            .penalizesBy(25);
    }

    @Test
    @DisplayName("Room conflict (weighted): Group + Private exceeds capacity")
    void penaltyForRoomConflict_groupPlusPrivate() {
        // Given: One group (100) + one private (25) = 125, exceeds 100
        Room room = createRoom(1L, "Studio C", 15, true);
        Timeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        Teacher teacher1 = createTeacher(1L, "John Doe");
        Teacher teacher2 = createTeacher(2L, "Jane Smith");
        DanceGroup group1 = createDanceGroup(1L, "Private");
        DanceGroup group2 = createDanceGroup(2L, "Group");

        Lesson lesson1 = createLesson(1L, teacher1, group1, timeslot, room, true, false);  // private = 25
        Lesson lesson2 = createLesson(2L, teacher2, group2, timeslot, room, false, false); // group = 100

        // When/Then: Should penalize with 25 HARD (125 - 100 = 25 excess)
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::roomConflict)
            .given(lesson1, lesson2, room, timeslot)
            .penalizesBy(25);
    }

    @Test
    @DisplayName("Room conflict (weighted): Three private lessons within capacity")
    void noPenaltyForRoomConflict_threePrivateLessons() {
        // Given: Three private lessons (25 * 3 = 75, under 100)
        Room room = createRoom(1L, "Studio B", 15, true);
        Timeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        Teacher teacher1 = createTeacher(1L, "Teacher 1");
        Teacher teacher2 = createTeacher(2L, "Teacher 2");
        Teacher teacher3 = createTeacher(3L, "Teacher 3");
        DanceGroup group1 = createDanceGroup(1L, "Private 1");
        DanceGroup group2 = createDanceGroup(2L, "Private 2");
        DanceGroup group3 = createDanceGroup(3L, "Private 3");

        Lesson lesson1 = createLesson(1L, teacher1, group1, timeslot, room, true, false);
        Lesson lesson2 = createLesson(2L, teacher2, group2, timeslot, room, true, false);
        Lesson lesson3 = createLesson(3L, teacher3, group3, timeslot, room, true, false);

        // When/Then: No penalty (75 < 100)
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::roomConflict)
            .given(lesson1, lesson2, lesson3, room, timeslot)
            .penalizesBy(0);
    }

    // ==================== HARD CONSTRAINT 2: Teacher Conflict ====================

    @Test
    @DisplayName("Teacher conflict: Teacher cannot teach two lessons simultaneously")
    void penaltyForTeacherConflict() {
        // Given: Same teacher, same timeslot, different rooms
        Teacher teacher = createTeacher(1L, "John Doe");
        Room room1 = createRoom(1L, "Studio A", 20, false);
        Room room2 = createRoom(2L, "Studio B", 15, false);
        Timeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        DanceGroup group1 = createDanceGroup(1L, "Group 1");
        DanceGroup group2 = createDanceGroup(2L, "Group 2");

        Lesson lesson1 = createLesson(1L, teacher, group1, timeslot, room1, false, false);
        Lesson lesson2 = createLesson(2L, teacher, group2, timeslot, room2, false, false);

        // When/Then: Should penalize with 1 HARD
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::teacherConflict)
            .given(lesson1, lesson2, teacher, timeslot, room1, room2)
            .penalizesBy(1);
    }

    @Test
    @DisplayName("Teacher conflict: No penalty for different timeslots")
    void noPenaltyForTeacherConflict_differentTimeslots() {
        // Given: Same teacher, different timeslots
        Teacher teacher = createTeacher(1L, "John Doe");
        Room room1 = createRoom(1L, "Studio A", 20, false);
        Room room2 = createRoom(2L, "Studio B", 15, false);
        Timeslot timeslot1 = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        Timeslot timeslot2 = createTimeslot(2L, DayOfWeek.MONDAY, "10:00", "11:00");
        DanceGroup group1 = createDanceGroup(1L, "Group 1");
        DanceGroup group2 = createDanceGroup(2L, "Group 2");

        Lesson lesson1 = createLesson(1L, teacher, group1, timeslot1, room1, false, false);
        Lesson lesson2 = createLesson(2L, teacher, group2, timeslot2, room2, false, false);

        // When/Then: No conflict (different times)
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::teacherConflict)
            .given(lesson1, lesson2, teacher, timeslot1, timeslot2, room1, room2)
            .penalizesBy(0);
    }

    @Test
    @DisplayName("Teacher conflict: No penalty for different teachers")
    void noPenaltyForTeacherConflict_differentTeachers() {
        // Given: Different teachers, same timeslot
        Teacher teacher1 = createTeacher(1L, "John Doe");
        Teacher teacher2 = createTeacher(2L, "Jane Smith");
        Room room = createRoom(1L, "Studio A", 20, false);
        Timeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        DanceGroup group1 = createDanceGroup(1L, "Group 1");
        DanceGroup group2 = createDanceGroup(2L, "Group 2");

        Lesson lesson1 = createLesson(1L, teacher1, group1, timeslot, room, false, false);
        Lesson lesson2 = createLesson(2L, teacher2, group2, timeslot, room, false, false);

        // When/Then: No teacher conflict (different teachers)
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::teacherConflict)
            .given(lesson1, lesson2, teacher1, teacher2, timeslot, room)
            .penalizesBy(0);
    }

    // ==================== HARD CONSTRAINT 3: Teacher Availability ====================

    @Test
    @DisplayName("Teacher availability: Penalty when lesson scheduled during unavailable time")
    void penaltyForTeacherUnavailability() {
        // Given: Teacher unavailable on Monday 9:00-10:00
        Teacher teacher = createTeacher(1L, "John Doe");
        Timeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        Room room = createRoom(1L, "Studio A", 20, false);
        DanceGroup group = createDanceGroup(1L, "Group 1");

        Lesson lesson = createLesson(1L, teacher, group, timeslot, room, false, false);
        ResourceUnavailability unavailability = createUnavailability(1L, teacher, timeslot, "Vacation");

        // When/Then: Should penalize with 1 HARD
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::teacherAvailability)
            .given(lesson, unavailability, teacher, timeslot, room)
            .penalizesBy(1);
    }

    @Test
    @DisplayName("Teacher availability: No penalty when teacher is available")
    void noPenaltyForTeacherAvailability_teacherAvailable() {
        // Given: Teacher available (no unavailability record)
        Teacher teacher = createTeacher(1L, "John Doe");
        Timeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        Room room = createRoom(1L, "Studio A", 20, false);
        DanceGroup group = createDanceGroup(1L, "Group 1");

        Lesson lesson = createLesson(1L, teacher, group, timeslot, room, false, false);

        // When/Then: No penalty (teacher is available)
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::teacherAvailability)
            .given(lesson, teacher, timeslot, room)
            .penalizesBy(0);
    }

    @Test
    @DisplayName("Teacher availability: No penalty for different timeslot")
    void noPenaltyForTeacherAvailability_differentTimeslot() {
        // Given: Teacher unavailable at 9:00, but lesson is at 10:00
        Teacher teacher = createTeacher(1L, "John Doe");
        Timeslot unavailableSlot = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        Timeslot lessonSlot = createTimeslot(2L, DayOfWeek.MONDAY, "10:00", "11:00");
        Room room = createRoom(1L, "Studio A", 20, false);
        DanceGroup group = createDanceGroup(1L, "Group 1");

        Lesson lesson = createLesson(1L, teacher, group, lessonSlot, room, false, false);
        ResourceUnavailability unavailability = createUnavailability(1L, teacher, unavailableSlot, "Meeting");

        // When/Then: No penalty (different timeslot)
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::teacherAvailability)
            .given(lesson, unavailability, teacher, unavailableSlot, lessonSlot, room)
            .penalizesBy(0);
    }

    // ==================== SOFT CONSTRAINT: Minimize Teacher Gaps ====================

    @Test
    @DisplayName("Minimize gaps: Penalty proportional to gap duration")
    void penaltyForTeacherGaps_proportionalToGapDuration() {
        // Given: Teacher has lessons at 9:00-10:00 and 12:00-13:00 (2 hours = 120 min gap)
        Teacher teacher = createTeacher(1L, "John Doe");
        Room room1 = createRoom(1L, "Studio A", 20, false);
        Room room2 = createRoom(2L, "Studio B", 15, false);
        Timeslot timeslot1 = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        Timeslot timeslot2 = createTimeslot(2L, DayOfWeek.MONDAY, "12:00", "13:00");
        DanceGroup group1 = createDanceGroup(1L, "Group 1");
        DanceGroup group2 = createDanceGroup(2L, "Group 2");

        Lesson lesson1 = createLesson(1L, teacher, group1, timeslot1, room1, false, false);
        Lesson lesson2 = createLesson(2L, teacher, group2, timeslot2, room2, false, false);

        // When/Then: Should penalize with 120 SOFT (120 minutes gap)
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::minimizeTeacherGaps)
            .given(lesson1, lesson2, teacher, timeslot1, timeslot2, room1, room2)
            .penalizesBy(120);
    }

    @Test
    @DisplayName("Minimize gaps: No penalty for consecutive lessons")
    void noPenaltyForTeacherGaps_consecutiveLessons() {
        // Given: Teacher has consecutive lessons (9:00-10:00, 10:00-11:00)
        Teacher teacher = createTeacher(1L, "John Doe");
        Room room1 = createRoom(1L, "Studio A", 20, false);
        Room room2 = createRoom(2L, "Studio B", 15, false);
        Timeslot timeslot1 = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        Timeslot timeslot2 = createTimeslot(2L, DayOfWeek.MONDAY, "10:00", "11:00");
        DanceGroup group1 = createDanceGroup(1L, "Group 1");
        DanceGroup group2 = createDanceGroup(2L, "Group 2");

        Lesson lesson1 = createLesson(1L, teacher, group1, timeslot1, room1, false, false);
        Lesson lesson2 = createLesson(2L, teacher, group2, timeslot2, room2, false, false);

        // When/Then: No penalty (no gap)
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::minimizeTeacherGaps)
            .given(lesson1, lesson2, teacher, timeslot1, timeslot2, room1, room2)
            .penalizesBy(0);
    }

    @Test
    @DisplayName("Minimize gaps: No penalty for different days")
    void noPenaltyForTeacherGaps_differentDays() {
        // Given: Teacher has lessons on different days
        Teacher teacher = createTeacher(1L, "John Doe");
        Room room1 = createRoom(1L, "Studio A", 20, false);
        Room room2 = createRoom(2L, "Studio B", 15, false);
        Timeslot timeslot1 = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        Timeslot timeslot2 = createTimeslot(2L, DayOfWeek.TUESDAY, "09:00", "10:00");
        DanceGroup group1 = createDanceGroup(1L, "Group 1");
        DanceGroup group2 = createDanceGroup(2L, "Group 2");

        Lesson lesson1 = createLesson(1L, teacher, group1, timeslot1, room1, false, false);
        Lesson lesson2 = createLesson(2L, teacher, group2, timeslot2, room2, false, false);

        // When/Then: No penalty (different days)
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::minimizeTeacherGaps)
            .given(lesson1, lesson2, teacher, timeslot1, timeslot2, room1, room2)
            .penalizesBy(0);
    }

    @Test
    @DisplayName("Minimize gaps: No penalty for different teachers")
    void noPenaltyForTeacherGaps_differentTeachers() {
        // Given: Different teachers on same day
        Teacher teacher1 = createTeacher(1L, "John Doe");
        Teacher teacher2 = createTeacher(2L, "Jane Smith");
        Room room = createRoom(1L, "Studio A", 20, false);
        Timeslot timeslot1 = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        Timeslot timeslot2 = createTimeslot(2L, DayOfWeek.MONDAY, "12:00", "13:00");
        DanceGroup group1 = createDanceGroup(1L, "Group 1");
        DanceGroup group2 = createDanceGroup(2L, "Group 2");

        Lesson lesson1 = createLesson(1L, teacher1, group1, timeslot1, room, false, false);
        Lesson lesson2 = createLesson(2L, teacher2, group2, timeslot2, room, false, false);

        // When/Then: No penalty (different teachers)
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::minimizeTeacherGaps)
            .given(lesson1, lesson2, teacher1, teacher2, timeslot1, timeslot2, room)
            .penalizesBy(0);
    }

    // ==================== SOFT CONSTRAINT: Prime-Time Reward ====================

    @Test
    @DisplayName("Prime-Time: Reward for lesson during prime hours")
    void rewardForPrimeTime_lessonAt18() {
        // Given: Lesson at 18:00 (within 16:00-21:00 prime time)
        Teacher teacher = createTeacher(1L, "John Doe");
        Room room = createRoom(1L, "Studio A", 20, false);
        Timeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, "18:00", "19:00");
        DanceGroup group = createDanceGroup(1L, "Group 1");

        Lesson lesson = createLesson(1L, teacher, group, timeslot, room, false, false);

        // When/Then: Should reward with 1 SOFT
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::rewardPrimeTime)
            .given(lesson, teacher, room, timeslot, group)
            .rewardsWith(1);
    }

    @Test
    @DisplayName("Prime-Time: Reward for lesson at 16:00")
    void rewardForPrimeTime_lessonAt16() {
        // Given: Lesson at 16:00 (start of prime time)
        Teacher teacher = createTeacher(1L, "John Doe");
        Room room = createRoom(1L, "Studio A", 20, false);
        Timeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, "16:00", "17:00");
        DanceGroup group = createDanceGroup(1L, "Group 1");

        Lesson lesson = createLesson(1L, teacher, group, timeslot, room, false, false);

        // When/Then: Should reward with 1 SOFT
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::rewardPrimeTime)
            .given(lesson, teacher, room, timeslot, group)
            .rewardsWith(1);
    }

    @Test
    @DisplayName("Prime-Time: No reward for lesson before prime time")
    void noRewardForPrimeTime_lessonAt15() {
        // Given: Lesson at 15:00 (before prime time)
        Teacher teacher = createTeacher(1L, "John Doe");
        Room room = createRoom(1L, "Studio A", 20, false);
        Timeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, "15:00", "16:00");
        DanceGroup group = createDanceGroup(1L, "Group 1");

        Lesson lesson = createLesson(1L, teacher, group, timeslot, room, false, false);

        // When/Then: No reward
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::rewardPrimeTime)
            .given(lesson, teacher, room, timeslot, group)
            .rewardsWith(0);
    }

    @Test
    @DisplayName("Prime-Time: No reward for lesson after prime time")
    void noRewardForPrimeTime_lessonAt21() {
        // Given: Lesson at 21:00 (after prime time)
        Teacher teacher = createTeacher(1L, "John Doe");
        Room room = createRoom(1L, "Studio A", 20, false);
        Timeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, "21:00", "22:00");
        DanceGroup group = createDanceGroup(1L, "Group 1");

        Lesson lesson = createLesson(1L, teacher, group, timeslot, room, false, false);

        // When/Then: No reward
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::rewardPrimeTime)
            .given(lesson, teacher, room, timeslot, group)
            .rewardsWith(0);
    }

    // ==================== SOFT CONSTRAINT: Load Balancing ====================

    @Test
    @DisplayName("Load Balancing: Penalty for teacher with many lessons")
    void penaltyForLoadBalance_manyLessons() {
        // Given: One teacher with 3 lessons (penalty = 9 for one teacher: 3*3)
        Teacher teacher = createTeacher(1L, "John Doe");
        Room room = createRoom(1L, "Studio A", 20, false);
        Timeslot timeslot1 = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        Timeslot timeslot2 = createTimeslot(2L, DayOfWeek.MONDAY, "10:00", "11:00");
        Timeslot timeslot3 = createTimeslot(3L, DayOfWeek.MONDAY, "11:00", "12:00");
        DanceGroup group1 = createDanceGroup(1L, "Group 1");
        DanceGroup group2 = createDanceGroup(2L, "Group 2");
        DanceGroup group3 = createDanceGroup(3L, "Group 3");

        Lesson lesson1 = createLesson(1L, teacher, group1, timeslot1, room, false, false);
        Lesson lesson2 = createLesson(2L, teacher, group2, timeslot2, room, false, false);
        Lesson lesson3 = createLesson(3L, teacher, group3, timeslot3, room, false, false);

        // When/Then: Should penalize with 9 SOFT (3 * 3 = 9)
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::balanceTeacherLoad)
            .given(lesson1, lesson2, lesson3, teacher, room, timeslot1, timeslot2, timeslot3)
            .penalizesBy(9);
    }

    @Test
    @DisplayName("Load Balancing: Lower penalty for balanced distribution")
    void lowerPenaltyForLoadBalance_balanced() {
        // Given: Two teachers with 1 lesson each (total penalty = 2: 1*1 + 1*1)
        Teacher teacher1 = createTeacher(1L, "Teacher 1");
        Teacher teacher2 = createTeacher(2L, "Teacher 2");
        Room room = createRoom(1L, "Studio A", 20, false);
        Timeslot timeslot1 = createTimeslot(1L, DayOfWeek.MONDAY, "09:00", "10:00");
        Timeslot timeslot2 = createTimeslot(2L, DayOfWeek.MONDAY, "10:00", "11:00");
        DanceGroup group1 = createDanceGroup(1L, "Group 1");
        DanceGroup group2 = createDanceGroup(2L, "Group 2");

        Lesson lesson1 = createLesson(1L, teacher1, group1, timeslot1, room, false, false);
        Lesson lesson2 = createLesson(2L, teacher2, group2, timeslot2, room, false, false);

        // When/Then: Should penalize with 2 SOFT (1*1 + 1*1 = 2)
        // This is LESS than having one teacher with 2 lessons (2*2 = 4)
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::balanceTeacherLoad)
            .given(lesson1, lesson2, teacher1, teacher2, room, timeslot1, timeslot2)
            .penalizesBy(2);
    }

    // ==================== Test Data Builders ====================

    private Room createRoom(Long id, String name, int capacity, boolean allowsParallelPrivate) {
        Room room = new Room();
        room.setId(id);
        room.setName(name);
        room.setCapacity(capacity);
        room.setAllowsParallelPrivate(allowsParallelPrivate);
        return room;
    }

    private Timeslot createTimeslot(Long id, DayOfWeek dayOfWeek, String startTime, String endTime) {
        Timeslot timeslot = new Timeslot();
        timeslot.setId(id);
        timeslot.setDayOfWeek(dayOfWeek);
        timeslot.setStartTime(LocalTime.parse(startTime));
        timeslot.setEndTime(LocalTime.parse(endTime));
        return timeslot;
    }

    private Teacher createTeacher(Long id, String fullName) {
        Teacher teacher = new Teacher();
        teacher.setId(id);
        teacher.setFullName(fullName);
        teacher.setEmail(fullName.toLowerCase().replace(" ", ".") + "@example.com");
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

    private Lesson createLesson(Long id, Teacher teacher, DanceGroup group,
                               Timeslot timeslot, Room room,
                               boolean isPrivate, boolean isPinned) {
        Lesson lesson = new Lesson();
        lesson.setId(id);
        lesson.setTeacher(teacher);
        lesson.setDanceGroup(group);
        lesson.setTimeslot(timeslot);
        lesson.setRoom(room);
        lesson.setDurationMinutes(60);
        lesson.setPrivate(isPrivate);
        lesson.setPinned(isPinned);
        return lesson;
    }

    private ResourceUnavailability createUnavailability(Long id, Teacher teacher,
                                                       Timeslot timeslot, String reason) {
        ResourceUnavailability unavailability = new ResourceUnavailability();
        unavailability.setId(id);
        unavailability.setTeacher(teacher);
        unavailability.setTimeslot(timeslot);
        unavailability.setReason(reason);
        return unavailability;
    }
}

