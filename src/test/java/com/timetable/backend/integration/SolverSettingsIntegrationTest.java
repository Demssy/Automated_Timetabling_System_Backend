package com.timetable.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.timetable.backend.domain.dto.SolverSettingsPresetRequest;
import com.timetable.backend.domain.dto.SolverSettingsValues;
import com.timetable.backend.domain.model.ScheduleMetadata;
import com.timetable.backend.domain.model.ScheduleStatus;
import com.timetable.backend.domain.model.AbstractUser;
import com.timetable.backend.domain.model.AvailabilityRole;
import com.timetable.backend.domain.model.DanceGroup;
import com.timetable.backend.domain.model.DanceLevel;
import com.timetable.backend.domain.model.Lesson;
import com.timetable.backend.domain.model.Role;
import com.timetable.backend.domain.model.Room;
import com.timetable.backend.domain.model.ScheduledLesson;
import com.timetable.backend.domain.model.ScheduledLessonStatus;
import com.timetable.backend.domain.model.Teacher;
import com.timetable.backend.domain.model.Student;
import com.timetable.backend.domain.model.Timeslot;
import com.timetable.backend.domain.model.WeeklyAvailability;
import com.timetable.backend.domain.repository.ScheduleMetadataRepository;
import com.timetable.backend.domain.repository.DanceGroupRepository;
import com.timetable.backend.domain.repository.LessonRepository;
import com.timetable.backend.domain.repository.RoleRepository;
import com.timetable.backend.domain.repository.RoomRepository;
import com.timetable.backend.domain.repository.ScheduledLessonRepository;
import com.timetable.backend.domain.repository.TeacherRepository;
import com.timetable.backend.domain.repository.StudentRepository;
import com.timetable.backend.domain.repository.TimeslotRepository;
import com.timetable.backend.domain.repository.UserRepository;
import com.timetable.backend.domain.repository.WeeklyAvailabilityRepository;
import com.timetable.backend.service.ScheduleMetadataService;
import com.timetable.backend.service.ScheduleScoreEvaluationService;
import com.timetable.backend.service.SolverSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SolverSettingsIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private SolverSettingsService settingsService;
    @Autowired private ScheduleMetadataRepository schedules;
    @Autowired private ScheduleScoreEvaluationService evaluator;
    @Autowired private ScheduleMetadataService metadataService;
    @Autowired private UserRepository users;
    @Autowired private RoleRepository roles;
    @Autowired private TeacherRepository teachers;
    @Autowired private StudentRepository students;
    @Autowired private WeeklyAvailabilityRepository availability;
    @Autowired private TimeslotRepository timeslots;
    @Autowired private RoomRepository rooms;
    @Autowired private DanceGroupRepository groups;
    @Autowired private LessonRepository lessons;
    @Autowired private ScheduledLessonRepository snapshots;
    @PersistenceContext private EntityManager entityManager;

    @BeforeEach
    void reset() {
        settingsService.reset();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void defaultsPresetsActivationAndResetDoNotRecalculateSchedules() throws Exception {
        ScheduleMetadata schedule = ScheduleMetadata.builder().name("Settings draft")
                .validFrom(LocalDate.of(2040, 1, 2)).validTo(LocalDate.of(2040, 1, 8))
                .status(ScheduleStatus.DRAFT).solverScore("0hard/123soft").build();
        schedule = schedules.saveAndFlush(schedule);
        SolverSettingsValues values = values(10, 5);
        mvc.perform(get("/api/v1/solver/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxAllowedHardPenalty").value(0))
                .andExpect(jsonPath("$.defaults.primeTimeLessonReward").value(10));

        String body = objectMapper.writeValueAsString(new SolverSettingsPresetRequest("Competition", values));
        String response = mvc.perform(post("/api/v1/solver/settings/presets")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(response).get("id").asLong();
        mvc.perform(post("/api/v1/solver/settings/presets/{id}/activate", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.physicalConflictHardWeight").value(10))
                .andExpect(jsonPath("$.maxAllowedHardPenalty").value(5))
                .andExpect(jsonPath("$.activePresetId").value(id));
        entityManager.flush();
        entityManager.clear();
        assertThat(settingsService.current()).isEqualTo(values);
        assertThat(schedules.findById(schedule.getId()).orElseThrow().getSolverScore()).isEqualTo("0hard/123soft");
        mvc.perform(post("/api/v1/solver/settings/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.physicalConflictHardWeight").value(1))
                .andExpect(jsonPath("$.maxAllowedHardPenalty").value(0));
        mvc.perform(get("/api/v1/solver/settings/presets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Competition"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void editingAnActivePresetUpdatesSettingsAndDeletingItKeepsTheValues() throws Exception {
        String created = mvc.perform(post("/api/v1/solver/settings/presets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SolverSettingsPresetRequest("First", values(1, 0)))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(created).get("id").asLong();
        mvc.perform(post("/api/v1/solver/settings/presets/{id}/activate", id)).andExpect(status().isOk());
        mvc.perform(put("/api/v1/solver/settings/presets/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SolverSettingsPresetRequest("Updated", values(10, 5)))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Updated"));
        assertThat(settingsService.current()).isEqualTo(values(10, 5));
        mvc.perform(delete("/api/v1/solver/settings/presets/{id}", id)).andExpect(status().isNoContent());
        entityManager.flush();
        entityManager.clear();
        assertThat(settingsService.current()).isEqualTo(values(10, 5));
        mvc.perform(get("/api/v1/solver/settings")).andExpect(jsonPath("$.activePresetId").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void invalidWeightsAreRejected() throws Exception {
        mvc.perform(put("/api/v1/solver/settings").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(values(0, -1))))
                .andExpect(status().isBadRequest());
        ObjectNode settings = objectMapper.valueToTree(values(1, 0));
        String[] softFields = {"privateLessonAssignmentReward", "primeTimeLessonReward",
                "studentUnfulfilledLessonPenalty", "teacherUnfulfilledLessonPenalty",
                "teacherGapPenaltyMultiplier", "teacherWorkloadFairnessMultiplier"};
        for (String field : softFields) {
            settings.put(field, 100);
        }
        mvc.perform(put("/api/v1/solver/settings").contentType(MediaType.APPLICATION_JSON)
                        .content(settings.toString()))
                .andExpect(status().isOk());
        for (String field : softFields) {
            settings.put(field, 101);
            mvc.perform(put("/api/v1/solver/settings").contentType(MediaType.APPLICATION_JSON)
                            .content(settings.toString()))
                    .andExpect(status().isBadRequest());
            settings.put(field, 100);
        }
        assertThat(settingsService.current().maxAllowedHardPenalty()).isZero();
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void settingsAreForbiddenForNonAdmins() throws Exception {
        mvc.perform(get("/api/v1/solver/settings")).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/solver/settings").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(values(1, 0))))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/solver/settings/reset")).andExpect(status().isForbidden());
    }

    @Test
    void selectedWeightsTakeEffectAtNextEvaluationAndBlockPublication() {
        ScheduleMetadata schedule = overlappingGroups();
        assertThat(evaluator.recalculate(schedule.getId()).hardScore()).isEqualTo(-1);
        settingsService.update(values(10, 5));
        assertThat(schedules.findById(schedule.getId()).orElseThrow().getSolverScore()).contains("-1hard");
        assertThat(evaluator.recalculate(schedule.getId()).hardScore()).isEqualTo(-10);
        assertThatThrownBy(() -> metadataService.publish(schedule.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("-10hard")
                .hasMessageContaining("-5hard");
    }

    @Test
    void thresholdAllowsPublishingWhenScoreIsWithinLimit() {
        ScheduleMetadata schedule = overlappingGroups();
        settingsService.update(values(1, 5));
        assertThat(metadataService.publish(schedule.getId()).status()).isEqualTo(ScheduleStatus.PUBLISHED);
        assertThat(schedules.findById(schedule.getId()).orElseThrow().getSolverScore()).contains("-1hard");
        settingsService.update(values(10, 5));
        assertThat(schedules.findById(schedule.getId()).orElseThrow().getSolverScore()).contains("-1hard");
        assertThat(evaluator.explain(schedule.getId()).totalScore()).contains("-10hard");
        assertThat(schedules.findById(schedule.getId()).orElseThrow().getSolverScore()).contains("-1hard");
    }

    @Test
    void dailyPrivatePenaltyCanBeDisabledAndAllowedByPublicationThreshold() {
        ScheduleMetadata schedule = twoPrivateLessons();
        settingsService.update(values(10, 5));
        assertThat(evaluator.recalculate(schedule.getId()).hardScore()).isEqualTo(-1);
        SolverSettingsValues disabled = new SolverSettingsValues(
                10, 5, 6, 15, 10, 1, 1, 0, 1, 100, 10, 50, 30, 1, 1);
        settingsService.update(disabled);
        assertThat(schedules.findById(schedule.getId()).orElseThrow().getSolverScore()).contains("-1hard");
        assertThat(evaluator.recalculate(schedule.getId()).hardScore()).isZero();
        settingsService.update(values(10, 5));
        assertThat(metadataService.publish(schedule.getId()).status()).isEqualTo(ScheduleStatus.PUBLISHED);
        assertThat(schedules.findById(schedule.getId()).orElseThrow().getSolverScore()).contains("-1hard");
    }

    private ScheduleMetadata twoPrivateLessons() {
        Role teacherRole = roles.findByName("TEACHER").orElseGet(() -> roles.save(new Role(null, "TEACHER")));
        Role studentRole = roles.findByName("STUDENT").orElseGet(() -> roles.save(new Role(null, "STUDENT")));
        Teacher teacher = teacher("settings-private-teacher@example.test", teacherRole);
        Student student = new Student();
        student.setEmail("settings-private-student@example.test");
        student.setFullName("Settings student");
        student.setPasswordHash("test-password-hash");
        student.setRole(studentRole);
        student = students.save(student);
        teacher.getPrivateStudents().add(student);
        teachers.save(teacher);
        Timeslot first = timeslots.save(new Timeslot(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(11, 0)));
        Timeslot second = timeslots.save(new Timeslot(DayOfWeek.MONDAY, LocalTime.of(12, 0), LocalTime.of(13, 0)));
        availability.save(new WeeklyAvailability(null, student, DayOfWeek.MONDAY, LocalTime.of(10, 0),
                LocalTime.of(11, 0), AvailabilityRole.STUDENT));
        availability.save(new WeeklyAvailability(null, student, DayOfWeek.MONDAY, LocalTime.of(12, 0),
                LocalTime.of(13, 0), AvailabilityRole.STUDENT));
        Room room = rooms.save(new Room("Settings private room", 4, true));
        ScheduleMetadata schedule = schedules.save(ScheduleMetadata.builder().name("Settings private")
                .validFrom(LocalDate.of(2040, 1, 2)).validTo(LocalDate.of(2040, 1, 8))
                .status(ScheduleStatus.DRAFT).build());
        Lesson firstLesson = lessons.save(new Lesson(teacher, null, 60, true, false));
        Lesson secondLesson = lessons.save(new Lesson(teacher, null, 60, true, false));
        snapshot(schedule, firstLesson, first, room, student);
        snapshot(schedule, secondLesson, second, room, student);
        return schedule;
    }

    private ScheduleMetadata overlappingGroups() {
        Role role = roles.findByName("TEACHER").orElseGet(() -> roles.save(new Role(null, "TEACHER")));
        Teacher first = teacher("settings-first@example.test", role);
        Teacher second = teacher("settings-second@example.test", role);
        Timeslot slot = timeslots.save(new Timeslot(DayOfWeek.MONDAY, LocalTime.of(17, 0), LocalTime.of(18, 0)));
        Room room = rooms.save(new Room("Settings room", 4, true));
        DanceGroup firstGroup = groups.save(new DanceGroup("Settings first", null, DanceLevel.BEGINNER));
        DanceGroup secondGroup = groups.save(new DanceGroup("Settings second", null, DanceLevel.BEGINNER));
        ScheduleMetadata schedule = schedules.save(ScheduleMetadata.builder().name("Settings integration")
                .validFrom(LocalDate.of(2040, 1, 2)).validTo(LocalDate.of(2040, 1, 8))
                .status(ScheduleStatus.DRAFT).build());
        snapshot(schedule, lessons.save(new Lesson(first, firstGroup, 60, false, true)), slot, room);
        snapshot(schedule, lessons.save(new Lesson(second, secondGroup, 60, false, true)), slot, room);
        return schedule;
    }

    private Teacher teacher(String email, Role role) {
        AbstractUser user = new AbstractUser();
        user.setEmail(email);
        user.setFullName(email);
        user.setPasswordHash("test-password-hash");
        user.setRole(role);
        user = users.save(user);
        Teacher teacher = new Teacher();
        teacher.setUser(user);
        teacher = teachers.save(teacher);
        availability.save(new WeeklyAvailability(null, user, DayOfWeek.MONDAY, LocalTime.of(9, 0),
                LocalTime.of(21, 0), AvailabilityRole.TEACHER));
        return teacher;
    }

    private void snapshot(ScheduleMetadata schedule, Lesson lesson, Timeslot slot, Room room) {
        snapshot(schedule, lesson, slot, room, null);
    }

    private void snapshot(ScheduleMetadata schedule, Lesson lesson, Timeslot slot, Room room, Student student) {
        ScheduledLesson row = new ScheduledLesson();
        row.setSchedule(schedule);
        row.setLesson(lesson);
        row.setTimeslot(slot);
        row.setRoom(room);
        row.setStudent(student);
        row.setStatus(ScheduledLessonStatus.ASSIGNED);
        snapshots.save(row);
    }

    private SolverSettingsValues values(int physical, int threshold) {
        return new SolverSettingsValues(10, threshold, 6, 15, physical, 1, 1, 1, 1, 100, 10, 50, 30, 1, 1);
    }
}
