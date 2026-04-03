package com.timetable.backend.service;

import com.timetable.backend.domain.dto.ResourceUnavailabilityDTO;
import com.timetable.backend.domain.dto.UserResponse;
import com.timetable.backend.domain.dto.WeeklyAvailabilityDTO;
import com.timetable.backend.domain.model.Role;
import com.timetable.backend.domain.model.Student;
import com.timetable.backend.domain.repository.ResourceUnavailabilityRepository;
import com.timetable.backend.domain.repository.RoleRepository;
import com.timetable.backend.domain.repository.UserRepository;
import com.timetable.backend.domain.repository.WeeklyAvailabilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    // Availability services — required by UserService constructor after refactor
    @Mock private WeeklyAvailabilityService weeklyAvailabilityService;
    @Mock private ResourceUnavailabilityService resourceUnavailabilityService;
    @Mock private WeeklyAvailabilityRepository weeklyAvailabilityRepository;
    @Mock private ResourceUnavailabilityRepository resourceUnavailabilityRepository;

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

        // Default stubs for availability — return empty lists unless overridden
        when(weeklyAvailabilityService.getByUserId(10L)).thenReturn(List.of());
        when(resourceUnavailabilityService.getByUserId(10L)).thenReturn(List.of());
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
        assertThat(result.weeklyAvailabilities()).isEmpty();
        assertThat(result.oneTimeUnavailabilities()).isEmpty();

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
    @DisplayName("getCurrentUserInfo - should return availability lists when populated")
    void getCurrentUserInfo_WithAvailability() {
        // Given
        WeeklyAvailabilityDTO weeklyDTO = new WeeklyAvailabilityDTO(
            1L, java.time.DayOfWeek.MONDAY,
            java.time.LocalTime.of(18, 0), java.time.LocalTime.of(20, 0)
        );
        ResourceUnavailabilityDTO exceptDTO = new ResourceUnavailabilityDTO(
            2L, java.time.LocalDate.of(2026, 12, 31),
            java.time.LocalTime.of(9, 0), java.time.LocalTime.of(18, 0), "Holiday"
        );

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testStudent));
        when(weeklyAvailabilityService.getByUserId(10L)).thenReturn(List.of(weeklyDTO));
        when(resourceUnavailabilityService.getByUserId(10L)).thenReturn(List.of(exceptDTO));

        // When
        UserResponse result = userService.getCurrentUserInfo("test@example.com");

        // Then
        assertThat(result.weeklyAvailabilities()).hasSize(1);
        assertThat(result.weeklyAvailabilities().get(0).dayOfWeek())
            .isEqualTo(java.time.DayOfWeek.MONDAY);
        assertThat(result.oneTimeUnavailabilities()).hasSize(1);
        assertThat(result.oneTimeUnavailabilities().get(0).reason()).isEqualTo("Holiday");
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

