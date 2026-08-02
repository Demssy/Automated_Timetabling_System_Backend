package com.timetable.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.timetable.backend.config.SecurityConfig;
import com.timetable.backend.domain.dto.AuthenticationRequest;
import com.timetable.backend.domain.dto.RegisterRequest;
import com.timetable.backend.domain.dto.UserResponse;
import com.timetable.backend.domain.dto.UserRole;
import com.timetable.backend.security.JwtAuthenticationFilter;
import com.timetable.backend.security.JwtService;
import com.timetable.backend.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    // Helper: build a minimal valid STUDENT RegisterRequest
    private RegisterRequest studentRequest(String email, String password, String fullName, LocalDate birthDate) {
        return new RegisterRequest(email, password, fullName, birthDate,
                UserRole.STUDENT, null, null, null, null, null, null);
    }

    @Test
    void register_Success() throws Exception {
        RegisterRequest request = studentRequest(
                "student@test.com", "password123", "Student Name", LocalDate.of(2000, 1, 1));
        UserResponse userResponse = new UserResponse(1L, "student@test.com", "Student Name", "STUDENT", true, null, null);

        when(authService.register(any(RegisterRequest.class))).thenReturn(userResponse);
        when(jwtService.getExpirationMs()).thenReturn(3600000L);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("student@test.com"))
                .andExpect(jsonPath("$.fullName").value("Student Name"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    void login_Success() throws Exception {
        AuthenticationRequest request = new AuthenticationRequest("student@test.com", "password");
        UserResponse userResponse = new UserResponse(1L, "student@test.com", "Student Name", "STUDENT", true, null, null);

        when(authService.authenticate(request.email(), request.password())).thenReturn(userResponse);
        when(jwtService.getExpirationMs()).thenReturn(3600000L);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("student@test.com"))
                .andExpect(jsonPath("$.fullName").value("Student Name"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    void register_WithInvalidEmail_ShouldReturnBadRequest() throws Exception {
        RegisterRequest request = studentRequest(
                "invalid-email", "password123", "Student Name", LocalDate.of(2000, 1, 1));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_WithShortPassword_ShouldReturnBadRequest() throws Exception {
        RegisterRequest request = studentRequest(
                "student@test.com", "12345", "Student Name", LocalDate.of(2000, 1, 1));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_WithBlankFullName_ShouldReturnBadRequest() throws Exception {
        RegisterRequest request = studentRequest(
                "student@test.com", "password123", "", LocalDate.of(2000, 1, 1));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_WithFutureBirthDate_ShouldReturnBadRequest() throws Exception {
        RegisterRequest request = studentRequest(
                "student@test.com", "password123", "Student Name", LocalDate.now().plusDays(1));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_Teacher_ActiveImmediately_ShouldReturnOkWithCookie() throws Exception {
        var request = new RegisterRequest(
                "teacher@test.com", "password123", "Teacher Name", LocalDate.of(1990, 5, 20),
                UserRole.TEACHER, null, null, null, "+1555000000", List.of("SALSA"), "Bio text");
        var userResponse = new UserResponse(2L, "teacher@test.com", "Teacher Name", "TEACHER", true, null, null);

        when(authService.register(any(RegisterRequest.class))).thenReturn(userResponse);
        when(jwtService.getExpirationMs()).thenReturn(3600000L);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TEACHER"))
                .andExpect(jsonPath("$.isActive").value(true));
    }
}
