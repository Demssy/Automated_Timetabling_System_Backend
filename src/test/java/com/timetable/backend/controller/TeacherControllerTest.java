package com.timetable.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.timetable.backend.config.SecurityConfig;
import com.timetable.backend.domain.dto.CreateTeacherRequest;
import com.timetable.backend.domain.dto.TeacherResponse;
import com.timetable.backend.security.JwtAuthenticationFilter;
import com.timetable.backend.security.JwtService;
import com.timetable.backend.service.TeacherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TeacherController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class TeacherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TeacherService teacherService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "ADMIN")
    void createTeacher_Success() throws Exception {
        CreateTeacherRequest request = new CreateTeacherRequest(
                "teacher@test.com", "password", "John Doe", 5, "#FFFFFF", Set.of(1L)
        );
        TeacherResponse response = new TeacherResponse(
                1L, "teacher@test.com", "John Doe", 5, "#FFFFFF", Set.of()
        );

        when(teacherService.createTeacher(any(CreateTeacherRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("teacher@test.com"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void createTeacher_Forbidden() throws Exception {
        CreateTeacherRequest request = new CreateTeacherRequest(
                "teacher@test.com", "password", "John Doe", 5, "#FFFFFF", Set.of(1L)
        );

        mockMvc.perform(post("/api/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createTeacher_ValidationFailure_InvalidEmail() throws Exception {
        CreateTeacherRequest request = new CreateTeacherRequest(
                "invalid-email", "password", "John Doe", 5, "#FFFFFF", Set.of(1L)
        );

        mockMvc.perform(post("/api/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createTeacher_ValidationFailure_ShortPassword() throws Exception {
        CreateTeacherRequest request = new CreateTeacherRequest(
                "teacher@test.com", "123", "John Doe", 5, "#FFFFFF", Set.of(1L)
        );

        mockMvc.perform(post("/api/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createTeacher_ValidationFailure_EmptyName() throws Exception {
        CreateTeacherRequest request = new CreateTeacherRequest(
                "teacher@test.com", "password", "", 5, "#FFFFFF", Set.of(1L)
        );

        mockMvc.perform(post("/api/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createTeacher_ValidationFailure_InvalidColor() throws Exception {
        CreateTeacherRequest request = new CreateTeacherRequest(
                "teacher@test.com", "password", "John Doe", 5, "ZZZZZZ", Set.of(1L)
        );

        mockMvc.perform(post("/api/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
