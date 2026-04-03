package com.timetable.backend.controller;

import com.timetable.backend.config.SecurityConfig;
import com.timetable.backend.domain.dto.UserResponse;
import com.timetable.backend.security.JwtAuthenticationFilter;
import com.timetable.backend.security.JwtService;
import com.timetable.backend.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@DisplayName("UserController Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "student@example.com", roles = {"STUDENT"})
    @DisplayName("GET /api/user/me - should return current user information")
    void getCurrentUser_Success() throws Exception {
        // Given
        UserResponse mockResponse = new UserResponse(
            1L,
            "student@example.com",
            "John Doe",
            "STUDENT",
            true,
            null,
            null
        );

        when(userService.getCurrentUserInfo("student@example.com"))
            .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/api/user/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.email").value("student@example.com"))
            .andExpect(jsonPath("$.fullName").value("John Doe"))
            .andExpect(jsonPath("$.role").value("STUDENT"))
            .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    @DisplayName("GET /api/user/me - should return 403 when not authenticated")
    void getCurrentUser_Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/user/me"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "teacher@example.com", roles = {"TEACHER"})
    @DisplayName("GET /api/user/me - should work for teacher role")
    void getCurrentUser_AsTeacher() throws Exception {
        // Given
        UserResponse mockResponse = new UserResponse(
            2L,
            "teacher@example.com",
            "Jane Smith",
            "TEACHER",
            true,
            null,
            null
        );

        when(userService.getCurrentUserInfo("teacher@example.com"))
            .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/api/user/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("TEACHER"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("GET /api/user/me - should work for admin role")
    void getCurrentUser_AsAdmin() throws Exception {
        // Given
        UserResponse mockResponse = new UserResponse(
            3L,
            "admin@example.com",
            "Admin User",
            "ADMIN",
            true,
            null,
            null
        );

        when(userService.getCurrentUserInfo("admin@example.com"))
            .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/api/user/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("ADMIN"));
    }
}

