package com.timetable.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.timetable.backend.domain.dto.RegisterRequest;
import com.timetable.backend.domain.dto.UserRole;
import com.timetable.backend.domain.repository.DanceStyleRepository;
import com.timetable.backend.domain.model.DanceStyle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test: teacher registration with dance style selection.
 * Ensures that:
 * 1. Unauthenticated users can GET the list of dance styles (all fallback paths).
 * 2. Teacher registration with selected styles works correctly.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DanceStyleRegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DanceStyleRepository danceStyleRepository;

    private List<DanceStyle> savedStyles;

    @BeforeEach
    void setUp() {
        danceStyleRepository.deleteAll();
        savedStyles = danceStyleRepository.saveAll(List.of(
                new DanceStyle("Salsa"),
                new DanceStyle("Bachata"),
                new DanceStyle("Afro House")
        ));
    }

    @Test
    void getStylesList_UnauthenticatedViaApiDanceStyles_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/dance-styles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].name", anyOf(is("Salsa"), is("Bachata"), is("Afro House"))));
    }

    @Test
    void getStylesList_UnauthenticatedViaDictionariesDanceStyles_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/dictionaries/dance-styles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void getStylesList_UnauthenticatedViaDictionariesStyles_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/dictionaries/styles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.danceStyles", hasSize(3)));
    }

    @Test
    void teacherRegistration_WithValidStyles_ReturnsUserResponse() throws Exception {
        var request = new RegisterRequest(
                "teacher@example.com",
                "password123",
                "Jane Doe",
                LocalDate.of(1990, 5, 15),
                UserRole.TEACHER,
                null, // danceLevel - student-only
                null, // parentContact
                null, // desiredLessonsPerWeek
                "+1-555-0123", // phone
                List.of("Salsa", "Bachata"), // qualifiedStyleIds - names
                "Professional instructor with 10+ years of experience" // bio
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("teacher@example.com"))
                .andExpect(jsonPath("$.fullName").value("Jane Doe"))
                .andExpect(jsonPath("$.role").value("TEACHER"));
    }

    @Test
    void teacherRegistration_WithUnknownStyle_ReturnsBadRequest() throws Exception {
        var request = new RegisterRequest(
                "teacher2@example.com",
                "password123",
                "John Smith",
                LocalDate.of(1985, 3, 20),
                UserRole.TEACHER,
                null,
                null,
                null,
                "+1-555-0456",
                List.of("Salsa", "UnknownStyle"), // Invalid style
                "Bio"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void teacherRegistration_NoStyles_ReturnsBadRequest() throws Exception {
        var request = new RegisterRequest(
                "teacher3@example.com",
                "password123",
                "Bob Johnson",
                LocalDate.of(1988, 7, 10),
                UserRole.TEACHER,
                null,
                null,
                null,
                "+1-555-0789",
                List.of(), // Empty qualifiedStyleIds
                "Bio"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
