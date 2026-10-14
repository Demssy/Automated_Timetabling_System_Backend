package com.timetable.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.timetable.backend.domain.dto.RegisterRequest;
import com.timetable.backend.domain.dto.UserRole;
import com.timetable.backend.domain.model.AbstractUser;
import com.timetable.backend.domain.model.AvailabilityRole;
import com.timetable.backend.domain.model.DanceLevel;
import com.timetable.backend.domain.model.DanceStyle;
import com.timetable.backend.domain.model.Student;
import com.timetable.backend.domain.repository.DanceStyleRepository;
import com.timetable.backend.domain.repository.StudentRepository;
import com.timetable.backend.domain.repository.TeacherRepository;
import com.timetable.backend.domain.repository.UserRepository;
import com.timetable.backend.service.WeeklyAvailabilityService;
import com.timetable.backend.domain.dto.WeeklyAvailabilityDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Integration test: a teacher registers as a student at the same time (dual-role account). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DualRoleRegistrationIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private DanceStyleRepository danceStyleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private WeeklyAvailabilityService weeklyAvailabilityService;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Test
    void teacherAlsoStudent_getsBothRolesAndProfiles() throws Exception {
        DanceStyle salsa = danceStyleRepository.save(new DanceStyle("DualRoleSalsa"));

        var request = new RegisterRequest(
                "dual.integration@example.com", "password123", "Dual Role", LocalDate.of(1991, 4, 12),
                UserRole.TEACHER, null, null, 2, "+1-555-0100", List.of(salsa.getId()), "Bio",
                true, DanceLevel.INTERMEDIATE, Set.of(salsa.getId()));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TEACHER"))
                .andExpect(jsonPath("$.roles", hasItems("ROLE_TEACHER", "ROLE_STUDENT")));

        AbstractUser user = userRepository.findByEmail("dual.integration@example.com").orElseThrow();
        assertTrue(user.hasRole("TEACHER"));
        assertTrue(user.hasRole("STUDENT"));
        assertTrue(teacherRepository.existsById(user.getId()));

        Student student = studentRepository.findById(user.getId()).orElseThrow();
        assertEquals(DanceLevel.INTERMEDIATE, student.getDanceLevel());
        assertEquals(2, student.getDesiredLessonsPerWeek());
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM student_dance_style WHERE student_id = ?", Integer.class, user.getId()));

        // Availability is stored separately per role
        weeklyAvailabilityService.updateUserSchedule(user.getId(), AvailabilityRole.TEACHER,
                List.of(new WeeklyAvailabilityDTO(null, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0))));
        weeklyAvailabilityService.updateUserSchedule(user.getId(), AvailabilityRole.STUDENT,
                List.of(new WeeklyAvailabilityDTO(null, DayOfWeek.TUESDAY, LocalTime.of(18, 0), LocalTime.of(20, 0))));

        assertEquals(DayOfWeek.MONDAY,
                weeklyAvailabilityService.getByUserId(user.getId(), AvailabilityRole.TEACHER).get(0).dayOfWeek());
        assertEquals(DayOfWeek.TUESDAY,
                weeklyAvailabilityService.getByUserId(user.getId(), AvailabilityRole.STUDENT).get(0).dayOfWeek());
    }

    @Test
    void plainTeacher_cannotDefineStudentAvailability() throws Exception {
        DanceStyle style = danceStyleRepository.save(new DanceStyle("PlainTeacherStyle"));
        var request = new RegisterRequest(
                "plain.teacher@example.com", "password123", "Plain Teacher", LocalDate.of(1988, 1, 5),
                UserRole.TEACHER, null, null, null, "+1-555-0200", List.of(style.getId()), "Bio");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", hasItems("ROLE_TEACHER")));

        Long userId = userRepository.findByEmail("plain.teacher@example.com").orElseThrow().getId();
        assertThrows(IllegalArgumentException.class, () -> weeklyAvailabilityService.updateUserSchedule(
                userId, AvailabilityRole.STUDENT, List.of()));
    }
}