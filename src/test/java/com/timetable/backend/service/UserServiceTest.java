package com.timetable.backend.service;

import com.timetable.backend.domain.dto.UserResponse;
import com.timetable.backend.domain.model.Role;
import com.timetable.backend.domain.model.Student;
import com.timetable.backend.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private Student testStudent;
    private Role studentRole;

    @BeforeEach
    void setUp() {
        studentRole = new Role(1L, "STUDENT");
        testStudent = new Student();
        testStudent.setId(10L);
        testStudent.setEmail("test@example.com");
        testStudent.setFullName("Test User");
        testStudent.setRole(studentRole);
        testStudent.setActive(true);
    }

    @Test
    @DisplayName("getCurrentUserInfo - should return user information when user exists")
    void getCurrentUserInfo_Success() {
        // Given
        when(userRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(testStudent));

        // When
        UserResponse result = userService.getCurrentUserInfo("test@example.com");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.email()).isEqualTo("test@example.com");
        assertThat(result.fullName()).isEqualTo("Test User");
        assertThat(result.role()).isEqualTo("STUDENT");
        assertThat(result.isActive()).isTrue();

        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("getCurrentUserInfo - should throw exception when user not found")
    void getCurrentUserInfo_UserNotFound() {
        // Given
        String nonExistentEmail = "nonexistent@example.com";
        when(userRepository.findByEmail(nonExistentEmail))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getCurrentUserInfo(nonExistentEmail))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessageContaining("User not found with email: " + nonExistentEmail);

        verify(userRepository).findByEmail(nonExistentEmail);
    }

    @Test
    @DisplayName("getCurrentUserInfo - should handle inactive users")
    void getCurrentUserInfo_InactiveUser() {
        // Given
        testStudent.setActive(false);
        when(userRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(testStudent));

        // When
        UserResponse result = userService.getCurrentUserInfo("test@example.com");

        // Then
        assertThat(result.isActive()).isFalse();
    }
}

