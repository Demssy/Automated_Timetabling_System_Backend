package com.timetable.backend.solver;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import com.timetable.backend.domain.model.Lesson;
import com.timetable.backend.domain.model.ResourceUnavailability;
import com.timetable.backend.domain.model.WeeklyAvailability;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Constraint provider for dance schedule optimization.
 * Defines hard and soft constraints for the Timefold Solver.
 *
 * Hard Constraints (must be satisfied):
 * - Room conflict: Weighted Dual-Mode logic (Group=1.0, Private=0.25)
 * - Teacher conflict: A teacher cannot teach two lessons at the same time
 * - Teacher availability: Lessons cannot be scheduled when teacher is unavailable
 *
 * <p><b>Null-safety note:</b> Since {@code Lesson.danceGroup} can be {@code null}
 * for private lessons, all constraints that would access {@code lesson.getDanceGroup()}
 * must guard with {@code .filter(lesson -> lesson.getDanceGroup() != null)}.
 * Currently no constraint accesses {@code danceGroup} directly, so the solver
 * is already null-safe with respect to this field.</p>
 * - Minimize gaps: Minimize time gaps between lessons for the same teacher on the same day
 * - Prime time reward: Encourage scheduling lessons during peak hours (16:00-21:00)
 * - Load balancing: Distribute lessons fairly among teachers
 */
public class DanceScheduleConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
                // Hard constraints
                roomConflict(constraintFactory),
                teacherConflict(constraintFactory),

                // NEW: Updated availability constraints
                teacherOutsideWeeklyAvailability(constraintFactory),
                teacherOneTimeUnavailability(constraintFactory),

                // Soft constraints
                minimizeTeacherGaps(constraintFactory),
                rewardPrimeTime(constraintFactory),
                balanceTeacherLoad(constraintFactory)
        };
    }

    /**
     * HARD CONSTRAINT 1: Room Conflict (Weighted Dual-Mode Logic)
     *
     * Implements EPIC 4 BE-15.2 specification:
     * - Group lesson occupies 100% of room capacity (weight = 1.0)
     * - Private lesson occupies 25% of room capacity (weight = 0.25)
     *
     * The constraint penalizes when total weight exceeds 1.0 in a room/timeslot.
     * Examples:
     * - 1 Group lesson = 1.0 (OK, fills room)
     * - 2 Group lessons = 2.0 (CONFLICT)
     * - 1 Group + 1 Private = 1.25 (CONFLICT)
     * - 4 Private lessons = 1.0 (OK, within capacity)
     * - 5 Private lessons = 1.25 (CONFLICT)
     *
     * @param constraintFactory the factory to create constraints
     * @return room conflict constraint
     */
    Constraint roomConflict(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Lesson.class)
                .filter(lesson -> lesson.getRoom() != null && lesson.getTimeslot() != null)
                .groupBy(Lesson::getRoom, Lesson::getTimeslot,
                         ConstraintCollectors.sum(this::getRoomOccupancyWeight))
                .filter((room, timeslot, totalWeight) -> totalWeight > 100) // 100 = 100%
                .penalize(HardSoftScore.ONE_HARD,
                         (room, timeslot, totalWeight) -> totalWeight - 100) // Penalize excess
                .asConstraint("Room conflict (Dual-Mode weighted)");
    }

    /**
     * Helper method to calculate room occupancy weight for a lesson.
     * - Group lesson (isPrivate=false): 100 (represents 100% capacity)
     * - Private lesson (isPrivate=true): 25 (represents 25% capacity)
     *
     * This allows up to 4 private lessons in the same room/timeslot,
     * but prevents mixing group with any other lesson.
     *
     * @param lesson the lesson to evaluate
     * @return occupancy weight (100 for group, 25 for private)
     */
    private int getRoomOccupancyWeight(Lesson lesson) {
        return lesson.isPrivate() ? 25 : 100;
    }

    /**
     * HARD CONSTRAINT 2: Teacher Conflict
     *
     * A teacher cannot teach two lessons at the same time.
     *
     * @param constraintFactory the factory to create constraints
     * @return teacher conflict constraint
     */
    Constraint teacherConflict(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Lesson.class)
                .join(Lesson.class,
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
     * HARD CONSTRAINT: Teacher Weekly Availability
     * Penalize if a lesson is scheduled outside the teacher's declared weekly available hours.
     */
    Constraint teacherOutsideWeeklyAvailability(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeslot() != null)
                // We penalize if there does NOT exist a weekly availability window that fully contains the lesson
                .ifNotExists(WeeklyAvailability.class,
                        // Match the teacher
                        Joiners.equal(Lesson::getTeacher, WeeklyAvailability::getUser),
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
                .forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeslot() != null)
                .join(ResourceUnavailability.class,
                        Joiners.equal(Lesson::getTeacher, ResourceUnavailability::getUser)
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
     * SOFT CONSTRAINT: Minimize Teacher Gaps
     *
     * Minimize time gaps between lessons for the same teacher on the same day.
     * It's better if lessons are consecutive (e.g., 09:00-10:00, 10:00-11:00)
     * rather than having gaps (e.g., 09:00-10:00, 12:00-13:00).
     *
     * The penalty is proportional to the gap duration in minutes.
     *
     * @param constraintFactory the factory to create constraints
     * @return minimize gaps constraint
     */
    /**
     * SOFT CONSTRAINT: Minimize Teacher Gaps
     */
    Constraint minimizeTeacherGaps(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Lesson.class)
                .join(Lesson.class,
                        // Different lessons
                        Joiners.lessThan(Lesson::getId),
                        // Same teacher
                        Joiners.equal(Lesson::getTeacher)

                )
                .filter((lesson1, lesson2) -> {

                    if (lesson1.getTimeslot() == null || lesson2.getTimeslot() == null) {
                        return false;
                    }

                    if (lesson1.getTimeslot().getDayOfWeek() != lesson2.getTimeslot().getDayOfWeek()) {
                        return false;
                    }

                    LocalTime end1 = lesson1.getTimeslot().getEndTime();
                    LocalTime start2 = lesson2.getTimeslot().getStartTime();
                    LocalTime end2 = lesson2.getTimeslot().getEndTime();
                    LocalTime start1 = lesson1.getTimeslot().getStartTime();

                    // 3. Gap exists if lesson1 ends before lesson2 starts (and vice versa)
                    return (end1.isBefore(start2) || end2.isBefore(start1));
                })
                .penalize(HardSoftScore.ONE_SOFT, (lesson1, lesson2) -> {
                    LocalTime end1 = lesson1.getTimeslot().getEndTime();
                    LocalTime start2 = lesson2.getTimeslot().getStartTime();
                    LocalTime end2 = lesson2.getTimeslot().getEndTime();
                    LocalTime start1 = lesson1.getTimeslot().getStartTime();

                    if (end1.isBefore(start2)) {
                        return (int) Duration.between(end1, start2).toMinutes();
                    } else {
                        return (int) Duration.between(end2, start1).toMinutes();
                    }
                })
                .asConstraint("Minimize teacher gaps");
    }

    /**
     * SOFT CONSTRAINT: Prime-Time Reward (EPIC 4 BE-16.2)
     *
     * Encourages scheduling lessons during peak hours (16:00-21:00).
     * This is when most students prefer to attend classes.
     *
     * Rewards each lesson scheduled in prime time with +1 soft score.
     *
     * @param constraintFactory the factory to create constraints
     * @return prime time reward constraint
     */
    Constraint rewardPrimeTime(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Lesson.class)
                .filter(lesson -> {
                    if (lesson.getTimeslot() == null) {
                        return false;
                    }
                    LocalTime start = lesson.getTimeslot().getStartTime();
                    // Prime time: 16:00 - 21:00
                    return !start.isBefore(LocalTime.of(16, 0)) &&
                           start.isBefore(LocalTime.of(21, 0));
                })
                .reward(HardSoftScore.ONE_SOFT)
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
                .forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeslot() != null)
                .groupBy(Lesson::getTeacher, ConstraintCollectors.count())
                .penalize(HardSoftScore.ONE_SOFT,
                         (teacher, count) -> count * count) // Square penalty for imbalance
                .asConstraint("Balance teacher workload");
    }
}
