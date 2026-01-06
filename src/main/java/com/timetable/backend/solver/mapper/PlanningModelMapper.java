package com.timetable.backend.solver.mapper;

import com.timetable.backend.domain.model.*;
import com.timetable.backend.solver.domain.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Mapper between JPA Persistence Model and Timefold Planning Model.
 *
 * Responsibilities:
 * 1. Convert JPA entities to lightweight Planning POJOs (for solver input)
 * 2. Map solved Planning POJOs back to JPA entities (for persistence)
 * 3. Unproxy Hibernate entities to avoid LazyInitializationException
 *
 * Performance Benefits:
 * - Eliminates Hibernate proxy overhead during solving
 * - Prevents N+1 queries in constraint evaluation
 * - Enables fast object cloning (no JPA baggage)
 */
@Component
public class PlanningModelMapper {

    /**
     * Converts JPA domain model to Planning model for Timefold Solver.
     *
     * @param scheduleId the schedule identifier
     * @param lessons JPA Lesson entities (eagerly loaded with relations)
     * @param timeslots available timeslots
     * @param rooms available rooms
     * @param teachers available teachers
     * @param unavailabilities teacher unavailability constraints
     * @return TimetableSolution ready for solving
     */
    public TimetableSolution toPlanningSolution(
            Long scheduleId,
            List<Lesson> lessons,
            List<Timeslot> timeslots,
            List<Room> rooms,
            List<Teacher> teachers,
            List<ResourceUnavailability> unavailabilities) {

        // Map facts (these become immutable references during solving)
        var planningTimeslots = timeslots.stream()
                .map(this::toPlanningTimeslot)
                .toList();

        var planningRooms = rooms.stream()
                .map(this::toPlanningRoom)
                .toList();

        var planningTeachers = teachers.stream()
                .map(this::toPlanningTeacher)
                .toList();

        var planningUnavailabilities = unavailabilities.stream()
                .map(this::toPlanningUnavailability)
                .toList();

        // Map planning entities (these will have variables assigned)
        var planningLessons = lessons.stream()
                .map(this::toPlanningLesson)
                .toList();

        return new TimetableSolution(
                scheduleId,
                planningTimeslots,
                planningRooms,
                planningTeachers,
                planningUnavailabilities,
                planningLessons
        );
    }

    /**
     * Maps solved Planning model back to JPA entities for persistence.
     *
     * IMPORTANT: This method only updates timeslot and room assignments.
     * The returned Lesson objects are DETACHED and need to be merged with
     * EntityManager or retrieved fresh from DB before updating.
     *
     * @param solution solved TimetableSolution from Timefold
     * @param timeslotMap map of ID -> JPA Timeslot (for lookup)
     * @param roomMap map of ID -> JPA Room (for lookup)
     * @return list of Lesson updates (id, timeslotId, roomId)
     */
    public List<LessonUpdate> toPersistableLessons(
            TimetableSolution solution,
            Map<Long, Timeslot> timeslotMap,
            Map<Long, Room> roomMap) {

        return solution.getLessonList().stream()
                .map(planningLesson -> {
                    Long timeslotId = planningLesson.getTimeslot() != null
                            ? planningLesson.getTimeslot().getId()
                            : null;
                    Long roomId = planningLesson.getRoom() != null
                            ? planningLesson.getRoom().getId()
                            : null;

                    return new LessonUpdate(
                            planningLesson.getId(),
                            timeslotId,
                            roomId
                    );
                })
                .toList();
    }

    // ========== Private Mapping Methods ==========

    private PlanningLesson toPlanningLesson(Lesson lesson) {
        var planningLesson = new PlanningLesson();
        planningLesson.setId(lesson.getId());
        planningLesson.setDurationMinutes(lesson.getDurationMinutes());
        planningLesson.setPinned(lesson.isPinned());
        planningLesson.setPrivate(lesson.isPrivate());

        // Map teacher (unproxy Hibernate entity)
        if (lesson.getTeacher() != null) {
            planningLesson.setTeacher(toPlanningTeacher(lesson.getTeacher()));
        }

        // Map dance group (unproxy)
        if (lesson.getDanceGroup() != null) {
            planningLesson.setDanceGroup(toPlanningDanceGroup(lesson.getDanceGroup()));
        }

        // Map current assignments (may be null for unassigned lessons)
        if (lesson.getTimeslot() != null) {
            planningLesson.setTimeslot(toPlanningTimeslot(lesson.getTimeslot()));
        }

        if (lesson.getRoom() != null) {
            planningLesson.setRoom(toPlanningRoom(lesson.getRoom()));
        }

        return planningLesson;
    }

    private PlanningTimeslot toPlanningTimeslot(Timeslot timeslot) {
        return new PlanningTimeslot(
                timeslot.getId(),
                timeslot.getDayOfWeek(),
                timeslot.getStartTime(),
                timeslot.getEndTime()
        );
    }

    private PlanningRoom toPlanningRoom(Room room) {
        return new PlanningRoom(
                room.getId(),
                room.getName(),
                room.getCapacity(),
                room.isAllowsParallelPrivate()
        );
    }

    private PlanningTeacher toPlanningTeacher(Teacher teacher) {
        return new PlanningTeacher(
                teacher.getId(),
                teacher.getFullName(),
                teacher.getEmail(),
                teacher.getMaxDailyHours(),
                teacher.getColorCode()
        );
    }

    private PlanningDanceGroup toPlanningDanceGroup(DanceGroup danceGroup) {
        Long danceStyleId = danceGroup.getDanceStyle() != null
                ? danceGroup.getDanceStyle().getId()
                : null;

        return new PlanningDanceGroup(
                danceGroup.getId(),
                danceGroup.getName(),
                danceStyleId,
                danceGroup.getDanceLevel(),
                danceGroup.getMinSize()
        );
    }

    private PlanningResourceUnavailability toPlanningUnavailability(ResourceUnavailability unavailability) {
        return new PlanningResourceUnavailability(
                unavailability.getId(),
                unavailability.getTeacher().getId(),
                unavailability.getTimeslot().getId(),
                unavailability.getReason()
        );
    }

    /**
     * DTO for lesson updates after solving.
     * Contains only the fields that need to be persisted.
     */
    public record LessonUpdate(
            Long lessonId,
            Long timeslotId,
            Long roomId
    ) {}
}

