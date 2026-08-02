package com.timetable.backend.service;

import com.timetable.backend.domain.dto.RegisterRequest;
import com.timetable.backend.domain.dto.UserResponse;
import com.timetable.backend.domain.dto.UserRole;
import com.timetable.backend.domain.model.DanceStyle;
import com.timetable.backend.domain.model.Role;
import com.timetable.backend.domain.model.Student;
import com.timetable.backend.domain.repository.DanceStyleRepository;
import com.timetable.backend.domain.repository.RoleRepository;
import com.timetable.backend.domain.repository.TeacherRepository;
import com.timetable.backend.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private DanceStyleRepository danceStyleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserService userService;

    @InjectMocks
    private AuthService authService;

    /** Helper: build a minimal STUDENT RegisterRequest. */
    private RegisterRequest studentRequest(String email, String password, String fullName, LocalDate birthDate) {
        return new RegisterRequest(email, password, fullName, birthDate,
                UserRole.STUDENT, null, null, null, null, null, null);
    }

    @Test
    void register_Student_Success() {
        String email = "student@test.com";
        String password = "password";
        String fullName = "Student Name";
        LocalDate birthDate = LocalDate.of(2000, 1, 1);

        Role studentRole = new Role(1L, "STUDENT");
        UserResponse expectedResponse = new UserResponse(1L, email, fullName, "STUDENT", true, List.of(), List.of());

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(roleRepository.findByName("STUDENT")).thenReturn(Optional.of(studentRole));
        when(passwordEncoder.encode(password)).thenReturn("encoded");
        when(userService.getCurrentUserInfo(email)).thenReturn(expectedResponse);

        UserResponse response = authService.register(studentRequest(email, password, fullName, birthDate));

        assertNotNull(response);
        assertEquals(email, response.email());
        assertEquals(fullName, response.fullName());
        assertEquals("STUDENT", response.role());
        verify(userRepository).save(any(Student.class));
        verify(userService).getCurrentUserInfo(email);
    }

    @Test
    void register_EmailExists_ThrowsException() {
        String email = "student@test.com";
        when(userRepository.existsByEmail(email)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                authService.register(studentRequest(email, "password", "Name", LocalDate.now().minusDays(1))));
        verify(userRepository, never()).save(any());
        verify(userService, never()).getCurrentUserInfo(any());
    }

    @Test
    void register_AdminRole_ThrowsException() {
        var request = new RegisterRequest(
                "admin@test.com", "password123", "Admin", LocalDate.of(1990, 1, 1),
                UserRole.ADMIN, null, null, null, null, null, null);

        when(userRepository.existsByEmail("admin@test.com")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_Teacher_MissingSpecialization_ThrowsException() {
        var request = new RegisterRequest(
                "teacher@test.com", "password123", "Teacher", LocalDate.of(1985, 3, 10),
                UserRole.TEACHER, null, null, null, null, null, null);  // no specialization

        when(userRepository.existsByEmail("teacher@test.com")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_Teacher_UnknownDanceStyle_ThrowsException() {
        var request = new RegisterRequest(
                "teacher@test.com", "password123", "Teacher", LocalDate.of(1985, 3, 10),
                UserRole.TEACHER, null, null, null, "+1234567", List.of("UNKNOWN_STYLE"), "bio");

        Role teacherRole = new Role(2L, "TEACHER");
        when(userRepository.existsByEmail("teacher@test.com")).thenReturn(false);
        when(roleRepository.findByName("TEACHER")).thenReturn(Optional.of(teacherRole));
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        when(danceStyleRepository.findByName("UNKNOWN_STYLE")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
    }

    @Test
    void register_Teacher_Success() {
        var email = "teacher@test.com";
        var request = new RegisterRequest(
                email, "password123", "Teacher Name", LocalDate.of(1985, 3, 10),
                UserRole.TEACHER, null, null, null, "+1234567", List.of("SALSA"), "bio");

        Role teacherRole = new Role(2L, "TEACHER");
        DanceStyle salsa = new DanceStyle("SALSA");
        UserResponse expectedResponse = new UserResponse(2L, email, "Teacher Name", "TEACHER", true, List.of(), List.of());

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(roleRepository.findByName("TEACHER")).thenReturn(Optional.of(teacherRole));
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        when(danceStyleRepository.findByName("SALSA")).thenReturn(Optional.of(salsa));
        when(userService.getCurrentUserInfo(email)).thenReturn(expectedResponse);

        UserResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("TEACHER", response.role());
        assertTrue(response.isActive());
        verify(teacherRepository).save(any());
        verify(userService).getCurrentUserInfo(email);
    }

    @Test
    void authenticate_Success() {
        String email = "user@test.com";
        String password = "password";
        Authentication auth = mock(Authentication.class);
        UserResponse expectedResponse = new UserResponse(1L, email, "Test User", "STUDENT", true, List.of(), List.of());

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(userService.getCurrentUserInfo(email)).thenReturn(expectedResponse);

        UserResponse response = authService.authenticate(email, password);

        assertNotNull(response);
        assertEquals(email, response.email());
        assertEquals("Test User", response.fullName());
        assertEquals("STUDENT", response.role());
        verify(userService).getCurrentUserInfo(email);
        verify(userRepository, never()).findByEmail(email);
    }
}
