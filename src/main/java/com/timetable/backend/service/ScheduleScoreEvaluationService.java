package com.timetable.backend.service;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.timetable.backend.domain.dto.ConstraintViolationSummary;
import com.timetable.backend.domain.dto.ScoreExplanationResponse;
import com.timetable.backend.domain.exception.ScheduleConflictException;
import com.timetable.backend.domain.model.Lesson;
import com.timetable.backend.domain.model.ScheduleMetadata;
import com.timetable.backend.domain.model.ScheduledLesson;
import com.timetable.backend.domain.model.ScheduledLessonStatus;
import com.timetable.backend.domain.repository.ResourceUnavailabilityRepository;
import com.timetable.backend.domain.repository.ScheduleMetadataRepository;
import com.timetable.backend.domain.repository.ScheduledLessonRepository;
import com.timetable.backend.domain.repository.StudentRepository;
import com.timetable.backend.domain.repository.TeacherRepository;
import com.timetable.backend.domain.repository.TimeslotRepository;
import com.timetable.backend.domain.repository.WeeklyAvailabilityRepository;
import com.timetable.backend.solver.DanceSchedule;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

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
    private final SolutionManager<DanceSchedule, HardSoftScore> solutionManager;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public ScoreExplanationResponse explain(Long scheduleId) {
        return evaluate(loadSchedule(scheduleId));
    }

    @Transactional
    public HardSoftScore recalculate(Long scheduleId) {
        ScheduleMetadata schedule = loadSchedule(scheduleId);
        ScoreExplanationResponse result = evaluate(schedule);
        try {
            schedule.setScoreExplanation(objectMapper.writeValueAsString(result.violations()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize score explanation for schedule " + scheduleId, e);
        }
        schedule.setSolverScore(result.totalScore());
        scheduleRepository.save(schedule);
        return HardSoftScore.parseScore(result.totalScore());
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
        HardSoftScore score = recalculate(scheduleId);
        if (score.initScore() != 0 || score.hardScore() < 0) {
            throw new ScheduleConflictException(
                    "Cannot publish schedule with unresolved hard constraint violations (Score: " + score + ")");
        }
    }

    private ScheduleMetadata loadSchedule(Long scheduleId) {
        return scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("Schedule not found: " + scheduleId));
    }

    private ScoreExplanationResponse evaluate(ScheduleMetadata schedule) {
        List<Lesson> lessons = scheduledLessonRepository.findByScheduleIdOrderByIdAsc(schedule.getId()).stream()
                .filter(row -> !row.isCancelled() && row.getStatus() == ScheduledLessonStatus.ASSIGNED)
                .filter(row -> row.getTimeslot() != null && row.getRoom() != null)
                .filter(row -> !row.isSourcePrivate() || row.getStudent() != null)
                .map(this::toPlanningLesson)
                .toList();
        DanceSchedule solution = new DanceSchedule(
                schedule.getId(),
                timeslotRepository.findAll(),
                teacherRepository.findAll(),
                resourceUnavailabilityRepository.findAll(),
                weeklyAvailabilityRepository.findAll(),
                schedule.getValidFrom() == null ? List.of() : List.of(schedule.getValidFrom()),
                studentRepository.findAll(),
                lessons
        );
        var explanation = solutionManager.explain(solution);
        List<ConstraintViolationSummary> summaries = explanation.getConstraintMatchTotalMap().values().stream()
                .filter(total -> total.getScore().hardScore() != 0 || total.getScore().softScore() != 0)
                .map(total -> new ConstraintViolationSummary(total.getConstraintRef().constraintName(),
                        total.getScore().hardScore(), total.getScore().softScore(), total.getConstraintMatchCount()))
                .sorted(Comparator.comparingInt(ConstraintViolationSummary::hardScore)
                        .thenComparingInt(ConstraintViolationSummary::softScore))
                .toList();
        return new ScoreExplanationResponse(schedule.getId(), explanation.getScore().toString(), summaries);
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
