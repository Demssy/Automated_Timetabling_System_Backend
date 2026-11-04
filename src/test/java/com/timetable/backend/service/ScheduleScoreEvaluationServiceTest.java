package com.timetable.backend.service;

import ai.timefold.solver.core.api.score.ScoreExplanation;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.solver.SolutionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.timetable.backend.domain.model.AddedLesson;
import com.timetable.backend.domain.model.Lesson;
import com.timetable.backend.domain.model.Room;
import com.timetable.backend.domain.model.ScheduleMetadata;
import com.timetable.backend.domain.model.ScheduledLesson;
import com.timetable.backend.domain.model.ScheduledLessonStatus;
import com.timetable.backend.domain.model.Teacher;
import com.timetable.backend.domain.model.Timeslot;
import com.timetable.backend.domain.model.Student;
import com.timetable.backend.domain.repository.ResourceUnavailabilityRepository;
import com.timetable.backend.domain.repository.ScheduleMetadataRepository;
import com.timetable.backend.domain.repository.ScheduledLessonRepository;
import com.timetable.backend.domain.repository.StudentRepository;
import com.timetable.backend.domain.repository.TeacherRepository;
import com.timetable.backend.domain.repository.TimeslotRepository;
import com.timetable.backend.domain.repository.WeeklyAvailabilityRepository;
import com.timetable.backend.solver.DanceSchedule;
import com.timetable.backend.solver.SolverRuntimeFactory;
import com.timetable.backend.domain.dto.SolverSettingsValues;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleScoreEvaluationServiceTest {
    @Mock private ScheduleMetadataRepository schedules;
    @Mock private ScheduledLessonRepository snapshots;
    @Mock private TimeslotRepository timeslots;
    @Mock private TeacherRepository teachers;
    @Mock private StudentRepository students;
    @Mock private WeeklyAvailabilityRepository weekly;
    @Mock private ResourceUnavailabilityRepository unavailability;
    @Mock private SolutionManager<DanceSchedule, HardSoftScore> solutionManager;
    @Mock private SolverRuntimeFactory runtimeFactory;
    @Mock private SolverSettingsService settingsService;
    @Mock private ScoreExplanation<DanceSchedule, HardSoftScore> explanation;
    @Mock private ConstraintMatchTotal<HardSoftScore> constraintTotal;
    private ScheduleScoreEvaluationService service;
    private ScheduleMetadata schedule;

    @BeforeEach
    void setUp() {
        service = new ScheduleScoreEvaluationService(schedules, snapshots, timeslots, teachers, students,
                weekly, unavailability, new ObjectMapper(), settingsService, runtimeFactory);
        lenient().when(settingsService.current()).thenReturn(new SolverSettingsValues(
                10, 0, 6, 15, 1, 1, 1, 1, 1, 100, 10, 50, 30, 1, 1));
        lenient().when(runtimeFactory.solutionManager(any())).thenReturn(solutionManager);
        schedule = new ScheduleMetadata();
        schedule.setId(1L);
        lenient().when(schedules.findById(1L)).thenReturn(Optional.of(schedule));
        lenient().when(solutionManager.explain(any())).thenReturn(explanation);
        lenient().when(explanation.getConstraintMatchTotalMap()).thenReturn(Map.of());
        lenient().when(explanation.getScore()).thenReturn(HardSoftScore.ZERO);
    }

    @Test
    void evaluatesEachEligibleSnapshotOnceWithoutMutatingSource() {
        Teacher teacher = new Teacher();
        Lesson source = new Lesson();
        source.setId(55L);
        source.setTeacher(teacher);
        source.setPrivate(false);
        Timeslot originalSlot = new Timeslot();
        source.setTimeslot(originalSlot);
        ScheduledLesson generated = snapshot(1L);
        generated.setLesson(source);

        AddedLesson added = new AddedLesson();
        added.setId(55L);
        added.setTeacher(teacher);
        added.setPrivate(false);
        ScheduledLesson manual = snapshot(2L);
        manual.setAddedLesson(added);
        ScheduledLesson cancelled = snapshot(3L);
        cancelled.setLesson(source);
        cancelled.setCancelled(true);
        ScheduledLesson incomplete = snapshot(4L);
        incomplete.setLesson(source);
        incomplete.setRoom(null);
        when(snapshots.findByScheduleIdOrderByIdAsc(1L))
                .thenReturn(List.of(generated, manual, cancelled, incomplete));

        service.recalculate(1L);

        ArgumentCaptor<DanceSchedule> captor = ArgumentCaptor.forClass(DanceSchedule.class);
        verify(solutionManager).explain(captor.capture());
        List<Lesson> evaluated = captor.getValue().getLessonList();
        assertThat(evaluated).extracting(Lesson::getId).containsExactly(1L, 2L);
        assertThat(evaluated.get(0)).isNotSameAs(source);
        assertThat(evaluated.get(0).getTimeslot()).isSameAs(generated.getTimeslot());
        assertThat(source.getTimeslot()).isSameAs(originalSlot);
        assertThat(schedule.getSolverScore()).isEqualTo("0hard/0soft");
        assertThat(schedule.getScoreExplanation()).isEqualTo("[]");
        verify(schedules).save(schedule);
    }

    @Test
    void explanationDoesNotWriteToSchedule() {
        when(snapshots.findByScheduleIdOrderByIdAsc(1L)).thenReturn(List.of());
        assertThat(service.explain(1L).totalScore()).isEqualTo("0hard/0soft");
        verify(schedules, never()).save(any());
        assertThat(schedule.getSolverScore()).isNull();
    }

    @Test
    void publicationRejectsEmptySchedule() {
        when(snapshots.findByScheduleIdOrderByIdAsc(1L)).thenReturn(List.of());
        assertThatThrownBy(() -> service.validateAndRecalculate(1L, true))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("without assigned lessons");
    }

    @Test
    void publicationRejectsIncompleteAssignments() {
        Lesson source = new Lesson();
        source.setPrivate(true);
        ScheduledLesson row = snapshot(1L);
        row.setLesson(source);
        when(snapshots.findByScheduleIdOrderByIdAsc(1L)).thenReturn(List.of(row));
        assertThatThrownBy(() -> service.validateAndRecalculate(1L, true))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("incomplete assignment");
    }

    @Test
    void publicationRejectsNegativeHardScore() {
        Lesson source = new Lesson();
        source.setTeacher(new Teacher());
        ScheduledLesson row = snapshot(1L);
        row.setLesson(source);
        when(snapshots.findByScheduleIdOrderByIdAsc(1L)).thenReturn(List.of(row));
        when(explanation.getScore()).thenReturn(HardSoftScore.ofHard(-1));
        assertThatThrownBy(() -> service.validateAndRecalculate(1L, true))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("hard constraint violations");
    }

    @Test
    void publicationAppliesConfiguredHardPenaltyThreshold() {
        Lesson source = new Lesson();
        source.setTeacher(new Teacher());
        ScheduledLesson row = snapshot(1L);
        row.setLesson(source);
        when(snapshots.findByScheduleIdOrderByIdAsc(1L)).thenReturn(List.of(row));
        when(settingsService.current()).thenReturn(new SolverSettingsValues(
                10, 15, 6, 15, 10, 1, 1, 1, 1, 100, 10, 50, 30, 1, 1));
        when(explanation.getScore()).thenReturn(HardSoftScore.ofHard(-13));
        service.validateAndRecalculate(1L, true);
        when(explanation.getScore()).thenReturn(HardSoftScore.ofHard(-16));
        assertThatThrownBy(() -> service.validateAndRecalculate(1L, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("-16hard")
                .hasMessageContaining("-15hard");
    }

    @Test
    void dualRoleOverlapIsRejectedEvenWithAnAllowance() {
        when(settingsService.current()).thenReturn(new SolverSettingsValues(
                10, 15, 6, 15, 1, 1, 1, 1, 1, 100, 10, 50, 30, 1, 1));
        when(snapshots.findByScheduleIdOrderByIdAsc(1L)).thenReturn(List.of());
        when(constraintTotal.getScore()).thenReturn(HardSoftScore.ofHard(-1));
        when(constraintTotal.getConstraintRef()).thenReturn(ConstraintRef.of("test",
                "Teacher attends another lesson as student at the same time"));
        when(explanation.getConstraintMatchTotalMap()).thenReturn(Map.of("dual-role", constraintTotal));
        assertThatThrownBy(() -> service.recalculate(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("teach and attend");
        verify(schedules, never()).save(any());
    }

    @Test
    void publicationIgnoresUnassignedPrivateTemplate() {
        Lesson group = new Lesson();
        group.setTeacher(new Teacher());
        ScheduledLesson assigned = snapshot(1L);
        assigned.setLesson(group);
        Lesson privateSource = new Lesson();
        privateSource.setPrivate(true);
        ScheduledLesson template = snapshot(2L);
        template.setLesson(privateSource);
        template.setStatus(ScheduledLessonStatus.UNASSIGNED);
        when(snapshots.findByScheduleIdOrderByIdAsc(1L)).thenReturn(List.of(assigned, template));
        service.validateAndRecalculate(1L, true);
        assertThat(schedule.getSolverScore()).isEqualTo("0hard/0soft");
    }

    @Test
    void publicationRejectsUnassignedPrivateLessonWithStudent() {
        Lesson source = new Lesson();
        source.setPrivate(true);
        ScheduledLesson row = snapshot(1L);
        row.setLesson(source);
        row.setStudent(new Student());
        row.setStatus(ScheduledLessonStatus.UNASSIGNED);
        when(snapshots.findByScheduleIdOrderByIdAsc(1L)).thenReturn(List.of(row));
        assertThatThrownBy(() -> service.validateAndRecalculate(1L, true))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("incomplete assignment");
    }

    @Test
    void publicationRejectsUninitializedScore() {
        Lesson source = new Lesson();
        source.setTeacher(new Teacher());
        ScheduledLesson row = snapshot(1L);
        row.setLesson(source);
        when(snapshots.findByScheduleIdOrderByIdAsc(1L)).thenReturn(List.of(row));
        when(explanation.getScore()).thenReturn(HardSoftScore.ofUninitialized(-1, 0, 0));
        assertThatThrownBy(() -> service.validateAndRecalculate(1L, true))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("hard constraint violations");
    }

    @Test
    void serializationFailureIsNotStoredAsEmptyExplanation() throws Exception {
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        when(failingMapper.writeValueAsString(any())).thenThrow(new JsonMappingException(null, "broken"));
        service = new ScheduleScoreEvaluationService(schedules, snapshots, timeslots, teachers, students,
                weekly, unavailability, failingMapper, settingsService, runtimeFactory);
        when(snapshots.findByScheduleIdOrderByIdAsc(1L)).thenReturn(List.of());
        assertThatThrownBy(() -> service.recalculate(1L))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("serialize");
        verify(schedules, never()).save(any());
        assertThat(schedule.getScoreExplanation()).isNull();
    }

    private ScheduledLesson snapshot(long id) {
        ScheduledLesson row = new ScheduledLesson();
        row.setId(id);
        row.setSchedule(schedule);
        row.setTimeslot(new Timeslot());
        row.setRoom(new Room());
        row.setStatus(ScheduledLessonStatus.ASSIGNED);
        return row;
    }
}
