package com.timetable.backend.solver;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import com.timetable.backend.domain.model.Lesson;
import com.timetable.backend.domain.model.ResourceUnavailability;

import java.time.Duration;
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
 * Soft Constraints (should be optimized):
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
            teacherAvailability(constraintFactory),

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
     * HARD CONSTRAINT 3: Teacher Availability
     *
     * A lesson cannot be scheduled when the teacher is unavailable.
     * This checks against the ResourceUnavailability table.
     *
     * @param constraintFactory the factory to create constraints
     * @return teacher availability constraint
     */
    Constraint teacherAvailability(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Lesson.class)
                .join(ResourceUnavailability.class,
                        // Same teacher
                        Joiners.equal(Lesson::getTeacher, ResourceUnavailability::getTeacher),
                        // Same timeslot
                        Joiners.equal(Lesson::getTimeslot, ResourceUnavailability::getTimeslot)
                )
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Teacher unavailability");
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
    Constraint minimizeTeacherGaps(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Lesson.class)
                .join(Lesson.class,
                        // Different lessons
                        Joiners.lessThan(Lesson::getId),
                        // Same teacher
                        Joiners.equal(Lesson::getTeacher),
                        // Same day of week
                        Joiners.equal(lesson -> lesson.getTimeslot() != null ?
                                lesson.getTimeslot().getDayOfWeek() : null)
                )
                .filter((lesson1, lesson2) -> {
                    // Only consider lessons that have timeslots assigned
                    if (lesson1.getTimeslot() == null || lesson2.getTimeslot() == null) {
                        return false;
                    }

                    // Check if there's a gap between the lessons
                    LocalTime end1 = lesson1.getTimeslot().getEndTime();
                    LocalTime start2 = lesson2.getTimeslot().getStartTime();
                    LocalTime end2 = lesson2.getTimeslot().getEndTime();
                    LocalTime start1 = lesson1.getTimeslot().getStartTime();

                    // Gap exists if lesson1 ends before lesson2 starts (and vice versa)
                    boolean gapExists = (end1.isBefore(start2) || end2.isBefore(start1));

                    return gapExists;
                })
                .penalize(HardSoftScore.ONE_SOFT, (lesson1, lesson2) -> {
                    // Calculate gap duration in minutes
                    LocalTime end1 = lesson1.getTimeslot().getEndTime();
                    LocalTime start2 = lesson2.getTimeslot().getStartTime();
                    LocalTime end2 = lesson2.getTimeslot().getEndTime();
                    LocalTime start1 = lesson1.getTimeslot().getStartTime();

                    // Calculate the actual gap (the one that exists)
                    if (end1.isBefore(start2)) {
                        // lesson1 is before lesson2
                        return (int) Duration.between(end1, start2).toMinutes();
                    } else {
                        // lesson2 is before lesson1
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
