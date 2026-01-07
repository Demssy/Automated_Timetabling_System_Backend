package com.timetable.backend.controller;

import com.timetable.backend.domain.model.AbstractUser;
import com.timetable.backend.domain.model.Role;
import com.timetable.backend.domain.model.Student;
import com.timetable.backend.domain.repository.RoleRepository;
import com.timetable.backend.domain.repository.UserRepository;
import com.timetable.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for UserController.
 * Tests the /api/user/me endpoint with real database and security context.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("UserController Integration Tests")
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JwtService jwtService;

    private String studentToken;
    private Student testStudent;

    @BeforeEach
    void setUp() {
        // Create and save role
        Role studentRole = roleRepository.findByName("STUDENT")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("STUDENT");
                    return roleRepository.save(role);
                });

        testStudent = new Student();
        testStudent.setEmail("integration.test@example.com");
        testStudent.setPasswordHash("$2a$10$dummyhash"); // BCrypt hash
        testStudent.setFullName("Integration Test User");
        testStudent.setRole(studentRole);
        testStudent.setActive(true);
        testStudent.setBirthDate(LocalDate.of(2000, 1, 1));

        AbstractUser savedUser = userRepository.save(testStudent);
        testStudent.setId(savedUser.getId());

        // Generate JWT token
        UserDetails userDetails = User.builder()
                .username(testStudent.getEmail())
                .password(testStudent.getPasswordHash())
                .authorities("ROLE_STUDENT")
                .build();

        studentToken = jwtService.generateToken(userDetails);
    }

    @Test
    @DisplayName("GET /api/user/me - should return current user info with valid JWT")
    void getCurrentUser_WithValidToken_ReturnsUserInfo() throws Exception {
        mockMvc.perform(get("/api/user/me")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testStudent.getId()))
                .andExpect(jsonPath("$.email").value("integration.test@example.com"))
                .andExpect(jsonPath("$.fullName").value("Integration Test User"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    @DisplayName("GET /api/user/me - should return 403 without JWT token")
    void getCurrentUser_WithoutToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/user/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/user/me - should return 403 with invalid JWT token")
    void getCurrentUser_WithInvalidToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/user/me")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isForbidden());
    }
}

