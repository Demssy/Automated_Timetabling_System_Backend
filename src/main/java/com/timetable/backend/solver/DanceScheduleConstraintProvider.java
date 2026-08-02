package com.timetable.backend.solver;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import com.timetable.backend.config.SolverWeightsConfig;
import com.timetable.backend.domain.model.Lesson;
import com.timetable.backend.domain.model.ResourceUnavailability;
import com.timetable.backend.domain.model.Student;
import com.timetable.backend.domain.model.WeeklyAvailability;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Constraint provider for dance schedule optimization.
 * Defines hard and soft constraints for the Timefold Solver.
 *
 * <p><b>Important:</b> Because {@code Lesson.student} is annotated with
 * {@code @PlanningVariable(nullable = true)}, the standard {@code forEach(Lesson.class)}
 * excludes all entities where the nullable variable is {@code null}. Since group lessons
 * always have {@code student == null}, we use {@code forEachIncludingNullVars(Lesson.class)}
 * in every constraint so that both group and unmatched private lessons are included.
 * Each constraint already has its own null-safety filters.</p>
 *
 * <p><b>Room logic:</b> There is only one room. Room is NOT a planning variable.
 * The solver enforces: max 4 private lessons per timeslot, and no private lessons
 * during a group lesson timeslot.</p>
 *
 * Hard Constraints (must be satisfied):
 * - Max 4 private lessons per timeslot (single room capacity)
 * - No private lessons during group lesson timeslots
 * - Teacher conflict: A teacher cannot teach two lessons at the same time
 * - Max 6 lessons per teacher per day (physical workload limit)
 * - Teacher availability: Lessons cannot be scheduled when teacher is unavailable
 * - Student matching: subscription, availability, no double-booking
 * - Student lessons per day must not exceed their availability windows for that day
 *
 * Soft Constraints (optimized):
 * - Reward student assignment (matchmaking incentive)
 * - Minimize gaps: Penalize idle gaps > 15 min between teacher lessons on same day
 * - Prime time reward: Encourage scheduling during peak hours 16:00-21:00 (+10 per lesson)
 * - Load balancing: Distribute lessons fairly among teachers
 */
public class DanceScheduleConstraintProvider implements ConstraintProvider {

    /**
     * Gaps up to this many minutes are considered a normal break between lessons
     * and are NOT penalized. Only gaps exceeding this threshold represent real idle time.
     * Value is loaded from {@link SolverWeightsConfig#getTeacherGapThresholdMinutes()}.
     */
    private final long acceptableTeacherGapMinutes;
    private final SolverWeightsConfig weights;

    /**
     * No-arg constructor required by Timefold 1.6.0, which instantiates
     * {@code ConstraintProvider} via reflection ({@code newInstance()}) and does
     * NOT support Spring DI here. Delegates to the primary constructor using
     * the Spring-managed {@link SolverWeightsConfig#getInstance()} holder,
     * which is populated via {@code @PostConstruct} before the solver starts.
     *
     * <p>Falls back to a default instance with standard values when called
     * outside of a Spring context (e.g., unit tests).</p>
     */
    public DanceScheduleConstraintProvider() {
        this(SolverWeightsConfig.getInstance());
    }

    /**
     * Primary constructor. Used directly in tests:
     * {@code new DanceScheduleConstraintProvider(new SolverWeightsConfig())}.
     *
     * @param weights externalized constraint weights and thresholds
     */
    public DanceScheduleConstraintProvider(SolverWeightsConfig weights) {
        this.weights = weights;
        this.acceptableTeacherGapMinutes = weights.getTeacherGapThresholdMinutes();
    }

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
                // Hard constraints — single-room capacity
                maxFourPrivateLessonsPerTimeslot(constraintFactory),
                noPrivateDuringGroupLesson(constraintFactory),

                // Hard constraints — teacher
                teacherConflict(constraintFactory),
                maxTeacherLessonsPerDay(constraintFactory),

                // Teacher availability constraints
                teacherOutsideWeeklyAvailability(constraintFactory),
                teacherOneTimeUnavailability(constraintFactory),

                // Student matching constraints
                groupLessonCannotHaveStudent(constraintFactory),
                studentMustBeSubscribedToTeacher(constraintFactory),
                studentConflict(constraintFactory),
                studentOutsideWeeklyAvailability(constraintFactory),
                studentOneTimeUnavailability(constraintFactory),
                studentLessonsPerDayMatchAvailabilityWindows(constraintFactory),

                // Desired lessons per week — hard cap + soft nudge
                studentDoNotExceedDesiredLessonsPerWeek(constraintFactory),
                teacherDoNotExceedDesiredLessonsPerWeek(constraintFactory),

                // Soft constraints
                rewardStudentAssignment(constraintFactory),
                studentMeetDesiredLessonsPerWeek(constraintFactory),
                teacherMeetDesiredLessonsPerWeek(constraintFactory),
                minimizeTeacherGaps(constraintFactory),
                rewardPrimeTime(constraintFactory),
                balanceTeacherLoad(constraintFactory)
        };
    }

    /**
     * HARD CONSTRAINT: Max 4 Private Lessons Per Timeslot
     *
     * Since there is only one room, at most 4 private lessons can happen simultaneously.
     * Group by timeslot → count private lessons → penalize when count exceeds 4.
     *
     * @param constraintFactory the factory to create constraints
     * @return max private lessons per timeslot constraint
     */
    Constraint maxFourPrivateLessonsPerTimeslot(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachIncludingNullVars(Lesson.class)
                .filter(lesson -> lesson.isPrivate() && lesson.getTimeslot() != null)
                .groupBy(Lesson::getTimeslot, ConstraintCollectors.count())
                .filter((timeslot, count) -> count > 4)
                .penalize(HardSoftScore.ONE_HARD,
                         (timeslot, count) -> count - 4)
                .asConstraint("Max 4 private lessons per timeslot");
    }

    /**
     * HARD CONSTRAINT: No Private Lessons During Group Lesson
     *
     * A group lesson occupies the entire room. No private lesson may be scheduled
     * in the same timeslot as any group lesson.
     *
     * @param constraintFactory the factory to create constraints
     * @return no private during group constraint
     */
    Constraint noPrivateDuringGroupLesson(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachIncludingNullVars(Lesson.class)
                .filter(lesson -> lesson.isPrivate() && lesson.getTimeslot() != null)
                .join(constraintFactory.forEachIncludingNullVars(Lesson.class),
                        Joiners.equal(Lesson::getTimeslot),
                        Joiners.filtering((privateLsn, groupLsn) ->
                                !groupLsn.isPrivate() && groupLsn.getTimeslot() != null)
                )
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("No private lessons during group lesson");
    }

    /**
     * HARD CONSTRAINT 2: Teacher Conflict
     *
     * A teacher cannot teach two lessons at the same time.
     *
     * <p><b>Null-safety:</b> Both streams explicitly filter {@code timeslot != null}.
     * Without this, two unassigned private lessons from the same teacher (both timeslot=null)
     * would match via {@code Joiners.equal(Lesson::getTimeslot)} because {@code null == null},
     * producing a <em>spurious hard violation</em> that severely misleads the solver.</p>
     *
     * @param constraintFactory the factory to create constraints
     * @return teacher conflict constraint
     */
    Constraint teacherConflict(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachIncludingNullVars(Lesson.class)
                .filter(lesson -> lesson.getTimeslot() != null)
                .join(constraintFactory.forEachIncludingNullVars(Lesson.class)
                                .filter(lesson -> lesson.getTimeslot() != null),
                        // Different lessons
                        Joiners.lessThan(Lesson::getId),
                        // Same teacher
                        Joiners.equal(Lesson::getTeacher),
                        // Same timeslot
                        Joiners.equal(Lesson::getTimeslot)
                )
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Teacher conflict");
    }

    /**
     * HARD CONSTRAINT: Max 6 Lessons Per Teacher Per Day
     *
     * A teacher cannot physically conduct more than 6 lessons in a single day.
     * 6 × 60 min lesson + 5 × 15 min break = 6h 15min — a realistic daily maximum.
     *
     * <p>Uses a local composite-key record {@code TeacherDay(teacherId, day)} to group
     * by (teacher, dayOfWeek) with a single-key {@code groupBy}, avoiding the
     * 3-argument groupBy API that has known issues in Timefold 1.6.0.</p>
     */
    Constraint maxTeacherLessonsPerDay(ConstraintFactory constraintFactory) {
        record TeacherDay(Long teacherId, DayOfWeek day) {}

        return constraintFactory
                .forEachIncludingNullVars(Lesson.class)
                .filter(lesson -> lesson.getTimeslot() != null)
                .groupBy(
                    lesson -> new TeacherDay(
                        lesson.getTeacher().getId(),
                        lesson.getTimeslot().getDayOfWeek()
                    ),
                    ConstraintCollectors.count()
                )
                .filter((key, count) -> count > weights.getMaxTeacherLessonsPerDay())
                .penalize(HardSoftScore.ONE_HARD, (key, count) -> count - weights.getMaxTeacherLessonsPerDay())
                .asConstraint("Max 6 lessons per teacher per day");
    }

    /**
     * HARD CONSTRAINT: Teacher Weekly Availability
     */
    Constraint teacherOutsideWeeklyAvailability(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachIncludingNullVars(Lesson.class)
                .filter(lesson -> lesson.getTimeslot() != null)
                // We penalize if there does NOT exist a weekly availability window that fully contains the lesson
                .ifNotExists(WeeklyAvailability.class,
                        // Match the teacher by comparing their shared AbstractUser
                        Joiners.equal(lesson -> lesson.getTeacher().getUser(), WeeklyAvailability::getUser),
                        // Match the day of the week
                        Joiners.equal(lesson -> lesson.getTimeslot().getDayOfWeek(), WeeklyAvailability::getDayOfWeek),
                        // Ensure the availability window fully covers the timeslot
                        Joiners.filtering((lesson, availability) ->
                                !lesson.getTimeslot().getStartTime().isBefore(availability.getStartTime()) &&
                                        !lesson.getTimeslot().getEndTime().isAfter(availability.getEndTime())
                        )
                )
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Teacher scheduled outside weekly availability");
    }
    /**
     * HARD CONSTRAINT: Teacher One-Time Unavailability (Exceptions)
     * Penalize if a lesson overlaps with a specific requested day off.
     */
    Constraint teacherOneTimeUnavailability(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachIncludingNullVars(Lesson.class)
                .filter(lesson -> lesson.getTimeslot() != null)
                .join(ResourceUnavailability.class,
                        Joiners.equal(lesson -> lesson.getTeacher().getUser(), ResourceUnavailability::getUser)
                )
                .join(LocalDate.class)
                .filter((lesson, unavail, scheduleStartDate) -> {
                    LocalDate lessonDate = mapDayOfWeekToDate(scheduleStartDate, lesson.getTimeslot().getDayOfWeek());
                    if (!lessonDate.equals(unavail.getDate())) {
                        return false;
                    }

                    // Time overlap logic: StartA < EndB AND EndA > StartB
                    return lesson.getTimeslot().getStartTime().isBefore(unavail.getEndTime()) &&
                            lesson.getTimeslot().getEndTime().isAfter(unavail.getStartTime());
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Teacher one-time unavailability conflict");
    }

    private LocalDate mapDayOfWeekToDate(LocalDate scheduleStartDate, DayOfWeek dayOfWeek) {
        int shift = dayOfWeek.getValue() - scheduleStartDate.getDayOfWeek().getValue();
        if (shift < 0) {
            shift += 7;
        }
        return scheduleStartDate.plusDays(shift);
    }
    /**
     * HARD CONSTRAINT: Group Lesson Cannot Have Student
     * A group lesson (isPrivate=false) must never have a student assigned.
     */
    Constraint groupLessonCannotHaveStudent(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachIncludingNullVars(Lesson.class)
                .filter(lesson -> !lesson.isPrivate() && lesson.getStudent() != null)
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Group lesson cannot have a student assigned");
    }

    /**
     * HARD CONSTRAINT: Student Must Be Subscribed To Teacher
     * A student can only attend a private lesson if they are linked to that teacher.
     */
    Constraint studentMustBeSubscribedToTeacher(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachIncludingNullVars(Lesson.class)
                .filter(lesson -> lesson.isPrivate() && lesson.getStudent() != null)
                .filter(lesson -> !lesson.getTeacher().getPrivateStudents().contains(lesson.getStudent()))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Student is not subscribed to this teacher");
    }

    /**
     * HARD CONSTRAINT: Student Conflict (Double Booking)
     * A student cannot attend two lessons at the same time.
     */
    Constraint studentConflict(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachIncludingNullVars(Lesson.class)
                .filter(lesson -> lesson.getStudent() != null && lesson.getTimeslot() != null)
                .join(constraintFactory.forEachIncludingNullVars(Lesson.class),
                        Joiners.lessThan(Lesson::getId),
                        Joiners.equal(Lesson::getStudent),
                        Joiners.equal(Lesson::getTimeslot)
                )
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Student conflict (double booking)");
    }

    /**
     * HARD CONSTRAINT: Student Outside Weekly Availability
     * The lesson timeslot must fall within the student's declared weekly availability windows.
     */
    Constraint studentOutsideWeeklyAvailability(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachIncludingNullVars(Lesson.class)
                .filter(lesson -> lesson.isPrivate() && lesson.getStudent() != null && lesson.getTimeslot() != null)
                .ifNotExists(WeeklyAvailability.class,
                        Joiners.equal(Lesson::getStudent, WeeklyAvailability::getUser),
                        Joiners.equal(lesson -> lesson.getTimeslot().getDayOfWeek(), WeeklyAvailability::getDayOfWeek),
                        Joiners.filtering((lesson, availability) ->
                                !lesson.getTimeslot().getStartTime().isBefore(availability.getStartTime()) &&
                                !lesson.getTimeslot().getEndTime().isAfter(availability.getEndTime())
                        )
                )
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Student scheduled outside weekly availability");
    }

    /**
     * HARD CONSTRAINT: Student One-Time Unavailability
     * The lesson must not overlap with a student's one-time ResourceUnavailability exception.
     */
    Constraint studentOneTimeUnavailability(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachIncludingNullVars(Lesson.class)
                .filter(lesson -> lesson.isPrivate() && lesson.getStudent() != null && lesson.getTimeslot() != null)
                .join(ResourceUnavailability.class,
                        Joiners.equal(Lesson::getStudent, ResourceUnavailability::getUser)
                )
                .join(LocalDate.class)
                .filter((lesson, unavail, scheduleStartDate) -> {
                    LocalDate lessonDate = mapDayOfWeekToDate(scheduleStartDate, lesson.getTimeslot().getDayOfWeek());
                    if (!lessonDate.equals(unavail.getDate())) return false;
                    return lesson.getTimeslot().getStartTime().isBefore(unavail.getEndTime()) &&
                           lesson.getTimeslot().getEndTime().isAfter(unavail.getStartTime());
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Student one-time unavailability conflict");
    }

    /**
     * SOFT CONSTRAINT: Reward Student Assignment
     * Because allowsUnassigned=true, the solver could score 0 Hard by leaving all private
     * lessons unmatched. This reward forces the solver to maximize filled private slots.
     */
    Constraint rewardStudentAssignment(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachIncludingNullVars(Lesson.class)
                .filter(lesson -> lesson.isPrivate() && lesson.getStudent() != null)
                .reward(HardSoftScore.ofSoft(weights.getRewardStudentAssignment()))
                .asConstraint("Reward for assigning a student to a private lesson");
    }

    /**
     * SOFT CONSTRAINT: Minimize Teacher Gaps
     *
     * Penalizes idle gaps between a teacher's lessons on the same day,
     * but ONLY when the gap exceeds {@value "ACCEPTABLE_TEACHER_GAP_MINUTES"} minutes.
     *
     * <p>A gap of 10–15 minutes is a normal break the teacher needs between students.
     * Only gaps representing real idle time (e.g. 2-hour hole mid-day) are penalized.</p>
     *
     * <p>Penalty: {@code (gapMinutes / 60) + 1} for gaps exceeding the threshold.</p>
     */
    Constraint minimizeTeacherGaps(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachIncludingNullVars(Lesson.class)
                .filter(lesson -> lesson.getTimeslot() != null)
                .join(constraintFactory.forEachIncludingNullVars(Lesson.class)
                                .filter(lesson -> lesson.getTimeslot() != null),
                        Joiners.lessThan(Lesson::getId),
                        Joiners.equal(Lesson::getTeacher),
                        Joiners.equal(lesson -> lesson.getTimeslot().getDayOfWeek())
                )
                .filter((lesson1, lesson2) -> {
                    LocalTime end1   = lesson1.getTimeslot().getEndTime();
                    LocalTime start2 = lesson2.getTimeslot().getStartTime();
                    LocalTime end2   = lesson2.getTimeslot().getEndTime();
                    LocalTime start1 = lesson1.getTimeslot().getStartTime();

                    long gapMinutes;
                    if (end1.isBefore(start2)) {
                        gapMinutes = Duration.between(end1, start2).toMinutes();
                    } else if (end2.isBefore(start1)) {
                        gapMinutes = Duration.between(end2, start1).toMinutes();
                    } else {
                        return false; // adjacent or overlapping — no gap to penalize
                    }
                    // Accept short breaks; penalize only real idle time
                    return gapMinutes > acceptableTeacherGapMinutes;
                })
                .penalize(HardSoftScore.ONE_SOFT, (lesson1, lesson2) -> {
                    LocalTime end1   = lesson1.getTimeslot().getEndTime();
                    LocalTime start2 = lesson2.getTimeslot().getStartTime();
                    LocalTime end2   = lesson2.getTimeslot().getEndTime();
                    LocalTime start1 = lesson1.getTimeslot().getStartTime();

                    long gapMinutes = end1.isBefore(start2)
                            ? Duration.between(end1, start2).toMinutes()
                            : Duration.between(end2, start1).toMinutes();

                    // Example: 120 min gap → (120/60)+1 = 3 soft penalty
                    return (int) (gapMinutes / 60) + 1;
                })
                .asConstraint("Minimize teacher gaps");
    }

    /**
     * SOFT CONSTRAINT: Prime-Time Reward
     *
     * Encourages scheduling lessons during peak hours (16:00–21:00).
     *
     * <p>Reward is +10 soft per lesson. Previously +1 was negligible (1% of the +100
     * assignment reward). At +10 it represents 10% and meaningfully steers the solver
     * toward evening slots preferred by most students.</p>
     */
    Constraint rewardPrimeTime(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachIncludingNullVars(Lesson.class)
                .filter(lesson -> {
                    if (lesson.getTimeslot() == null) {
                        return false;
                    }
                    LocalTime start = lesson.getTimeslot().getStartTime();
                    // Prime time: 16:00 - 21:00
                    return !start.isBefore(LocalTime.of(16, 0)) &&
                           start.isBefore(LocalTime.of(21, 0));
                })
                .reward(HardSoftScore.ofSoft(weights.getRewardPrimeTime()))
                .asConstraint("Reward prime time usage");
    }

    /**
     * SOFT CONSTRAINT: Load Balancing (EPIC 4 BE-16.3)
     *
     * Distributes lessons fairly among teachers to ensure workload balance.
     * Uses variance-based penalty: the more lessons a teacher has, the higher the penalty.
     *
     * This prevents scenarios where one teacher has 10 lessons while another has only 2.
     * The squared penalty ensures that large imbalances are heavily discouraged.
     *
     * @param constraintFactory the factory to create constraints
     * @return load balancing constraint
     */
    Constraint balanceTeacherLoad(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachIncludingNullVars(Lesson.class)
                .filter(lesson -> lesson.getTimeslot() != null)
                .groupBy(Lesson::getTeacher, ConstraintCollectors.count())
                .penalize(HardSoftScore.ONE_SOFT,
                         (teacher, count) -> count * count) // Square penalty for imbalance
                .asConstraint("Balance teacher workload");
    }


    /**
     * HARD CONSTRAINT: Student Lessons Per Day Must Not Exceed Availability Windows
     *
     * <p>A student's availability windows represent distinct time slots they are willing
     * to attend on a given day:
     * <ul>
     *   <li>1 window on Monday  → max 1 lesson on Monday (hard)</li>
     *   <li>2 windows on Monday → max 2 lessons on Monday (hard)</li>
     * </ul>
     * </p>
     *
     * <p><b>Algorithm (two-phase groupBy + join):</b>
     * <ol>
     *   <li>Group private lessons by {@code (student, dayOfWeek)} → {@code lessonCount}.</li>
     *   <li>Join the result with {@code WeeklyAvailability} on same {@code (student, day)}
     *       to enumerate each availability window as a separate row.</li>
     *   <li>Re-group by {@code (student, day, lessonCount)} counting how many availability
     *       windows were joined ({@code windowCount}).</li>
     *   <li>Penalize by {@code lessonCount - windowCount} whenever lessons exceed windows.</li>
     * </ol>
     * </p>
     *
     * <p><b>Edge case — 0 windows on a day:</b> the join produces zero rows, so the
     * re-grouped stream has no entry for that student+day. This case is already caught
     * by {@code studentOutsideWeeklyAvailability} which requires every lesson to fall
     * inside at least one window.</p>
     */
    Constraint studentLessonsPerDayMatchAvailabilityWindows(ConstraintFactory constraintFactory) {
        record StudentDay(Student student, DayOfWeek day) {}
        record StudentDayWithLessonCount(Student student, DayOfWeek day, int lessonCount) {}

        return constraintFactory
                // Phase 1: count private lessons per (student, day)
                .forEachIncludingNullVars(Lesson.class)
                .filter(lesson -> lesson.isPrivate()
                        && lesson.getStudent() != null
                        && lesson.getTimeslot() != null)
                .groupBy(
                        lesson -> new StudentDay(
                                lesson.getStudent(),
                                lesson.getTimeslot().getDayOfWeek()),
                        ConstraintCollectors.count()
                )
                // BiConstraintStream<StudentDay, Integer lessonCount>

                // Phase 2: join with WeeklyAvailability to enumerate windows for (student, day)
                .join(WeeklyAvailability.class,
                        Joiners.equal((key, count) -> key.student(), WeeklyAvailability::getUser),
                        Joiners.equal((key, count) -> key.day(), WeeklyAvailability::getDayOfWeek)
                )
                // TriConstraintStream<StudentDay, Integer lessonCount, WeeklyAvailability>

                // Phase 3: re-group by (student, day, lessonCount), counting availability rows
                .groupBy(
                        (key, lessonCount, avail) ->
                                new StudentDayWithLessonCount(key.student(), key.day(), lessonCount),
                        ConstraintCollectors.countTri()
                )
                // BiConstraintStream<StudentDayWithLessonCount, Integer windowCount>

                // Phase 4: penalize every lesson beyond the window count
                .filter((sdwc, windowCount) -> sdwc.lessonCount() > windowCount)
                .penalize(HardSoftScore.ONE_HARD,
                        (sdwc, windowCount) -> sdwc.lessonCount() - windowCount)
                .asConstraint("Student lessons per day must not exceed availability windows");
    }

    /**
     * HARD CONSTRAINT: Student Must Not Exceed Desired Lessons Per Week
     *
     * <p>If a student has set {@code desiredLessonsPerWeek}, the solver must not
     * assign more private lessons to them than that value across the whole week.
     * Students who have not set this field ({@code null}) are not affected.</p>
     *
     * <p>Penalizes by 1 hard per excess lesson beyond the desired count.</p>
     */
    Constraint studentDoNotExceedDesiredLessonsPerWeek(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachIncludingNullVars(Lesson.class)
                .filter(lesson -> lesson.isPrivate()
                        && lesson.getStudent() != null
                        && lesson.getTimeslot() != null)
                .groupBy(Lesson::getStudent, ConstraintCollectors.count())
                .filter((student, count) ->
                        student.getDesiredLessonsPerWeek() != null
                        && count > student.getDesiredLessonsPerWeek())
                .penalize(HardSoftScore.ONE_HARD,
                        (student, count) -> count - student.getDesiredLessonsPerWeek())
                .asConstraint("Student must not exceed desired lessons per week");
    }

    /**
     * SOFT CONSTRAINT: Student Should Meet Desired Lessons Per Week
     *
     * <p>Penalizes each lesson the student is missing compared to their desired weekly count.
     * Only fires when a student has at least one lesson assigned but fewer than desired —
     * the zero-assigned case is already covered by {@link #rewardStudentAssignment}.
     * Only applies when {@code desiredLessonsPerWeek} is set ({@code != null}).</p>
     *
     * <p>Weight: {@link SolverWeightsConfig#getPenaltyStudentUnderDesiredLessons()} per
     * missing lesson (default 50 — half of the assignment reward, so the solver always
     * gains net by assigning but is also nudged toward filling all desired slots).</p>
     */
    Constraint studentMeetDesiredLessonsPerWeek(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachIncludingNullVars(Lesson.class)
                .filter(lesson -> lesson.isPrivate()
                        && lesson.getStudent() != null
                        && lesson.getTimeslot() != null)
                .groupBy(Lesson::getStudent, ConstraintCollectors.count())
                .filter((student, count) ->
                        student.getDesiredLessonsPerWeek() != null
                        && count < student.getDesiredLessonsPerWeek())
                .penalize(HardSoftScore.ofSoft(weights.getPenaltyStudentUnderDesiredLessons()),
                        (student, count) -> student.getDesiredLessonsPerWeek() - count)
                .asConstraint("Student should meet desired lessons per week");
    }

    /**
     * HARD CONSTRAINT: Teacher Must Not Exceed Desired Lessons Per Week
     *
     * <p>If a teacher has set {@code desiredLessonsPerWeek}, the total number of
     * lessons (private + group) assigned to them must not exceed that value.
     * Teachers who have not set this field ({@code null}) are not affected.</p>
     *
     * <p>Penalizes by 1 hard per lesson beyond the desired weekly count.</p>
     */
    Constraint teacherDoNotExceedDesiredLessonsPerWeek(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachIncludingNullVars(Lesson.class)
                .filter(lesson -> lesson.getTimeslot() != null)
                .groupBy(Lesson::getTeacher, ConstraintCollectors.count())
                .filter((teacher, count) ->
                        teacher.getDesiredLessonsPerWeek() != null
                        && count > teacher.getDesiredLessonsPerWeek())
                .penalize(HardSoftScore.ONE_HARD,
                        (teacher, count) -> count - teacher.getDesiredLessonsPerWeek())
                .asConstraint("Teacher must not exceed desired lessons per week");
    }

    /**
     * SOFT CONSTRAINT: Teacher Should Meet Desired Lessons Per Week
     *
     * <p>Penalizes each lesson the teacher is missing compared to their desired weekly count.
     * Only applies when {@code desiredLessonsPerWeek} is set ({@code != null}).</p>
     *
     * <p>Weight: {@link SolverWeightsConfig#getPenaltyTeacherUnderDesiredLessons()} per
     * missing lesson (default 30).</p>
     */
    Constraint teacherMeetDesiredLessonsPerWeek(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachIncludingNullVars(Lesson.class)
                .filter(lesson -> lesson.getTimeslot() != null)
                .groupBy(Lesson::getTeacher, ConstraintCollectors.count())
                .filter((teacher, count) ->
                        teacher.getDesiredLessonsPerWeek() != null
                        && count < teacher.getDesiredLessonsPerWeek())
                .penalize(HardSoftScore.ofSoft(weights.getPenaltyTeacherUnderDesiredLessons()),
                        (teacher, count) -> teacher.getDesiredLessonsPerWeek() - count)
                .asConstraint("Teacher should meet desired lessons per week");
    }
}
