package com.timetable.backend.service;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatch;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.timetable.backend.domain.dto.ConstraintMatchDetailDTO;
import com.timetable.backend.domain.dto.ConstraintViolationSummary;
import com.timetable.backend.domain.dto.ScoreExplanationResponse;
import com.timetable.backend.domain.dto.SolverSettingsValues;
import com.timetable.backend.domain.exception.ScheduleConflictException;
import com.timetable.backend.domain.model.Lesson;
import com.timetable.backend.domain.model.ResourceUnavailability;
import com.timetable.backend.domain.model.Room;
import com.timetable.backend.domain.model.ScheduleMetadata;
import com.timetable.backend.domain.model.ScheduleStatus;
import com.timetable.backend.domain.model.ScheduledLesson;
import com.timetable.backend.domain.model.ScheduledLessonStatus;
import com.timetable.backend.domain.model.Student;
import com.timetable.backend.domain.model.Teacher;
import com.timetable.backend.domain.model.Timeslot;
import com.timetable.backend.domain.repository.ResourceUnavailabilityRepository;
import com.timetable.backend.domain.repository.ScheduleMetadataRepository;
import com.timetable.backend.domain.repository.ScheduledLessonRepository;
import com.timetable.backend.domain.repository.StudentRepository;
import com.timetable.backend.domain.repository.TeacherRepository;
import com.timetable.backend.domain.repository.TimeslotRepository;
import com.timetable.backend.domain.repository.WeeklyAvailabilityRepository;
import com.timetable.backend.solver.DanceSchedule;
import com.timetable.backend.solver.SolverRuntimeFactory;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleScoreEvaluationService {

    private final ScheduleMetadataRepository scheduleRepository;
    private final ScheduledLessonRepository scheduledLessonRepository;
    private final TimeslotRepository timeslotRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final WeeklyAvailabilityRepository weeklyAvailabilityRepository;
    private final ResourceUnavailabilityRepository resourceUnavailabilityRepository;
    private final ObjectMapper objectMapper;
    private final SolverSettingsService settingsService;
    private final SolverRuntimeFactory runtimeFactory;

    @Transactional
    public ScoreExplanationResponse explain(Long scheduleId) {
        ScheduleMetadata schedule = loadSchedule(scheduleId);
        ScoreExplanationResponse response = evaluate(schedule, settingsService.current());

        // Keep stored score synchronized with live explanation for DRAFT schedules
        if (schedule.getStatus() == ScheduleStatus.DRAFT
                && !Objects.equals(schedule.getSolverScore(), response.totalScore())) {
            schedule.setSolverScore(response.totalScore());
            try {
                schedule.setScoreExplanation(objectMapper.writeValueAsString(response.violations()));
            } catch (JsonProcessingException ignored) {
                // Ignore serialization error during read/sync
            }
            scheduleRepository.save(schedule);
        }
        return response;
    }

    @Transactional
    public HardSoftScore recalculate(Long scheduleId) {
        return HardSoftScore.parseScore(storeEvaluation(loadSchedule(scheduleId), settingsService.current()).totalScore());
    }

    private ScoreExplanationResponse storeEvaluation(ScheduleMetadata schedule, SolverSettingsValues settings) {
        ScoreExplanationResponse result = evaluate(schedule, settings);
        if (result.violations().stream().anyMatch(violation ->
                violation.constraintName().equals("Teacher attends another lesson as student at the same time")
                        && violation.hardScore() < 0)) {
            throw new ScheduleConflictException("A teacher cannot teach and attend another lesson at the same time");
        }
        try {
            schedule.setScoreExplanation(objectMapper.writeValueAsString(result.violations()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize score explanation for schedule " + schedule.getId(), e);
        }
        schedule.setSolverScore(result.totalScore());
        scheduleRepository.save(schedule);
        return result;
    }

    @Transactional
    public void validateAndRecalculate(Long scheduleId, boolean requireAssignedLesson) {
        List<ScheduledLesson> active = scheduledLessonRepository.findByScheduleIdOrderByIdAsc(scheduleId).stream()
                .filter(row -> !row.isCancelled())
                .toList();
        long assigned = 0;
        for (ScheduledLesson row : active) {
            if (row.getLesson() == null && row.getAddedLesson() == null) {
                throw new IllegalStateException("Scheduled lesson has no source: " + row.getId());
            }
            if (row.getStatus() == ScheduledLessonStatus.ASSIGNED) {
                if (row.getTimeslot() == null || row.getRoom() == null
                        || (row.isSourcePrivate() && row.getStudent() == null)) {
                    throw new ScheduleConflictException("Scheduled lesson has an incomplete assignment: " + row.getId());
                }
                assigned++;
            } else if (row.getStatus() != ScheduledLessonStatus.UNASSIGNED
                    || !row.isSourcePrivate() || row.getStudent() != null) {
                throw new ScheduleConflictException("Scheduled lesson has an incomplete assignment: " + row.getId());
            }
        }
        if (requireAssignedLesson && assigned == 0) {
            throw new ScheduleConflictException("Cannot publish a schedule without assigned lessons");
        }
        SolverSettingsValues settings = settingsService.current();
        ScoreExplanationResponse result = storeEvaluation(loadSchedule(scheduleId), settings);
        HardSoftScore score = HardSoftScore.parseScore(result.totalScore());
        if (score.initScore() != 0 || score.hardScore() < -(long) settings.maxAllowedHardPenalty()) {
            throw new ScheduleConflictException(
                    "Cannot publish schedule with unresolved hard constraint violations (Score: " + score
                            + ", minimum hard score: -" + settings.maxAllowedHardPenalty() + "hard)");
        }
    }

    private ScheduleMetadata loadSchedule(Long scheduleId) {
        return scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("Schedule not found: " + scheduleId));
    }

    private ScoreExplanationResponse evaluate(ScheduleMetadata schedule, SolverSettingsValues settings) {
        List<Teacher> teachers = teacherRepository.findAll();
        Map<Long, Teacher> teacherById = teachers.stream()
                .collect(Collectors.toMap(Teacher::getId, t -> t));

        List<Lesson> lessons = scheduledLessonRepository.findByScheduleIdOrderByIdAsc(schedule.getId()).stream()
                .filter(row -> !row.isCancelled() && row.getStatus() == ScheduledLessonStatus.ASSIGNED)
                .filter(row -> row.getTimeslot() != null && row.getRoom() != null)
                .filter(row -> !row.isSourcePrivate() || row.getStudent() != null)
                .map(this::toPlanningLesson)
                .toList();

        DanceSchedule solution = new DanceSchedule(
                schedule.getId(),
                timeslotRepository.findAll(),
                teachers,
                resourceUnavailabilityRepository.findAll(),
                weeklyAvailabilityRepository.findAll(),
                schedule.getValidFrom() == null ? List.of() : List.of(schedule.getValidFrom()),
                studentRepository.findAll(),
                lessons
        );

        var explanation = runtimeFactory.solutionManager(settings).explain(solution);

        List<ConstraintViolationSummary> summaries = explanation.getConstraintMatchTotalMap().values().stream()
                .filter(total -> total.getScore().hardScore() != 0 || total.getScore().softScore() != 0)
                .map(total -> {
                    String constraintName = total.getConstraintRef().constraintName();
                    List<ConstraintMatchDetailDTO> details = total.getConstraintMatchSet().stream()
                            .map(match -> toMatchDetail(constraintName, match, teacherById))
                            .sorted(Comparator.comparingInt(ConstraintMatchDetailDTO::hardImpact)
                                    .thenComparingInt(ConstraintMatchDetailDTO::softImpact))
                            .toList();

                    return new ConstraintViolationSummary(
                            constraintName,
                            total.getScore().hardScore(),
                            total.getScore().softScore(),
                            total.getConstraintMatchCount(),
                            details
                    );
                })
                .sorted(Comparator.comparingInt(ConstraintViolationSummary::hardScore)
                        .thenComparingInt(ConstraintViolationSummary::softScore))
                .toList();

        return new ScoreExplanationResponse(schedule.getId(), explanation.getScore().toString(), summaries);
    }
    private ConstraintMatchDetailDTO toMatchDetail(
            String constraintName,
            ConstraintMatch<HardSoftScore> match,
            Map<Long, Teacher> teacherById
    ) {
        int hardImpact = match.getScore().hardScore();
        int softImpact = match.getScore().softScore();

        Set<String> involvedUsers = new LinkedHashSet<>();
        List<String> lessonDescriptions = new ArrayList<>();
        List<String> timeslotDescriptions = new ArrayList<>();
        List<String> extraNotes = new ArrayList<>();

        for (Object obj : match.getIndictedObjectList()) {
            if (obj == null) continue;

            if (obj instanceof Lesson lesson) {
                extractFromLesson(lesson, involvedUsers, lessonDescriptions, timeslotDescriptions);
            } else if (obj instanceof Collection<?> collection) {
                for (Object item : collection) {
                    if (item instanceof Lesson lesson) {
                        extractFromLesson(lesson, involvedUsers, lessonDescriptions, timeslotDescriptions);
                    }
                }
            } else if (obj instanceof Teacher teacher) {
                involvedUsers.add("Teacher: " + resolveTeacherName(teacher));
                if (teacher.getDesiredLessonsPerWeek() != null) {
                    extraNotes.add("Desired: " + teacher.getDesiredLessonsPerWeek() + " lessons/week");
                }
            } else if (obj instanceof Student student) {
                involvedUsers.add("Student: " + student.getFullName());
                if (student.getDesiredLessonsPerWeek() != null) {
                    extraNotes.add("Desired: " + student.getDesiredLessonsPerWeek() + " lessons/week");
                }
            } else if (obj instanceof ResourceUnavailability unavail) {
                if (unavail.getUser() != null) {
                    involvedUsers.add(unavail.getUser().getFullName());
                }
                timeslotDescriptions.add(unavail.getDate() + " " + unavail.getStartTime() + "–" + unavail.getEndTime());
            } else if (obj instanceof Integer count) {
                extraNotes.add("Assigned count: " + count);
            } else if (obj.getClass().isRecord()) {
                extractFromRecord(obj, teacherById, involvedUsers, timeslotDescriptions, extraNotes);
            }
        }

        String lessonInfo = lessonDescriptions.isEmpty() ? null : String.join(" | ", lessonDescriptions);
        String timeslotInfo = timeslotDescriptions.isEmpty() ? null : String.join(", ", new LinkedHashSet<>(timeslotDescriptions));

        String description = extraNotes.isEmpty()
                ? constraintName
                : constraintName + " (" + String.join(", ", extraNotes) + ")";

        return new ConstraintMatchDetailDTO(
                hardImpact,
                softImpact,
                description,
                new ArrayList<>(involvedUsers),
                lessonInfo,
                timeslotInfo
        );
    }

    private void extractFromLesson(
            Lesson lesson,
            Set<String> involvedUsers,
            List<String> lessonDescriptions,
            List<String> timeslotDescriptions
    ) {
        if (lesson.getTeacher() != null) {
            involvedUsers.add("Teacher: " + resolveTeacherName(lesson.getTeacher()));
        }
        if (lesson.isPrivate()) {
            if (lesson.getStudent() != null) {
                involvedUsers.add("Student: " + lesson.getStudent().getFullName());
            }
            String style = lesson.getDanceStyle() != null ? " (" + lesson.getDanceStyle().getName() + ")" : "";
            lessonDescriptions.add("Private lesson #" + lesson.getId() + style);
        } else {
            String groupName = lesson.getDanceGroup() != null ? lesson.getDanceGroup().getName() : "#" + lesson.getId();
            lessonDescriptions.add("Group: " + groupName);
        }

        Timeslot ts = lesson.getTimeslot();
        if (ts != null) {
            timeslotDescriptions.add(ts.getDayOfWeek() + " " + ts.getStartTime() + "–" + ts.getEndTime());
        }
    }

    private void extractFromRecord(
            Object recordObj,
            Map<Long, Teacher> teacherById,
            Set<String> involvedUsers,
            List<String> timeslotDescriptions,
            List<String> extraNotes
    ) {
        try {
            for (var component : recordObj.getClass().getRecordComponents()) {
                Object value = component.getAccessor().invoke(recordObj);
                if (value instanceof Teacher t) {
                    involvedUsers.add("Teacher: " + resolveTeacherName(t));
                } else if (value instanceof Student s) {
                    involvedUsers.add("Student: " + s.getFullName());
                } else if (value instanceof Long id && component.getName().toLowerCase().contains("teacher")) {
                    Teacher t = teacherById.get(id);
                    involvedUsers.add(t != null ? "Teacher: " + resolveTeacherName(t) : "Teacher ID: " + id);
                } else if (value instanceof DayOfWeek day) {
                    timeslotDescriptions.add(day.name());
                } else if (value instanceof Room room) {
                    extraNotes.add("Room: " + room.getName());
                } else if (value instanceof Integer count) {
                    extraNotes.add(component.getName() + ": " + count);
                }
            }
        } catch (Exception ignored) {
            // Safe fallback if reflection fails
        }
    }

    private String resolveTeacherName(Teacher teacher) {
        if (teacher == null) {
            return "Unknown";
        }
        if (teacher.getUser() != null && teacher.getUser().getFullName() != null) {
            return teacher.getUser().getFullName();
        }
        return "ID #" + teacher.getId();
    }
    private Lesson toPlanningLesson(ScheduledLesson snapshot) {
        if (snapshot.getId() == null || snapshot.getSourceTeacher() == null
                || (snapshot.getLesson() == null && snapshot.getAddedLesson() == null)) {
            throw new IllegalStateException("Invalid scheduled lesson snapshot: " + snapshot.getId());
        }
        Lesson lesson = new Lesson();
        lesson.setId(snapshot.getId());
        lesson.setTeacher(snapshot.getSourceTeacher());
        lesson.setDanceGroup(snapshot.getSourceDanceGroup());
        lesson.setDanceStyle(snapshot.getLesson() == null ? null : snapshot.getLesson().getDanceStyle());
        lesson.setDurationMinutes(snapshot.getSourceDurationMinutes());
        lesson.setPrivate(snapshot.isSourcePrivate());
        lesson.setPinned(true);
        lesson.setActive(snapshot.isSourceActive());
        lesson.setStudent(snapshot.getStudent());
        lesson.setTimeslot(snapshot.getTimeslot());
        lesson.setRoom(snapshot.getRoom());
        return lesson;
    }
}