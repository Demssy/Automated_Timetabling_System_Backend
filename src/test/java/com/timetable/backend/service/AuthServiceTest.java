package com.timetable.backend.service;

import com.timetable.backend.domain.dto.UserResponse;
import com.timetable.backend.domain.model.Role;
import com.timetable.backend.domain.model.Student;
import com.timetable.backend.domain.repository.RoleRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerStudent_Success() {
        String email = "student@test.com";
        String password = "password";
        String fullName = "Student Name";
        LocalDate birthDate = LocalDate.of(2000, 1, 1);

        Role studentRole = new Role(1L, "STUDENT");
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(roleRepository.findByName("STUDENT")).thenReturn(Optional.of(studentRole));
        when(passwordEncoder.encode(password)).thenReturn("encoded");

        UserResponse response = authService.registerStudent(email, password, fullName, birthDate);

        assertNotNull(response);
        assertEquals(email, response.email());
        assertEquals(fullName, response.fullName());
        assertEquals("STUDENT", response.role());
        verify(userRepository).save(any(Student.class));
    }

    @Test
    void registerStudent_EmailExists() {
        String email = "student@test.com";
        when(userRepository.existsByEmail(email)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
            authService.registerStudent(email, "password", "Name", LocalDate.now())
        );
        verify(userRepository, never()).save(any());
    }

    @Test
    void authenticate_Success() {
        String email = "user@test.com";
        String password = "password";
        Authentication auth = mock(Authentication.class);

        Student student = new Student();
        student.setId(1L);
        student.setEmail(email);
        student.setFullName("Test User");
        student.setRole(new Role(1L, "STUDENT"));

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(student));

        UserResponse response = authService.authenticate(email, password);

        assertNotNull(response);
        assertEquals(email, response.email());
        assertEquals("Test User", response.fullName());
        assertEquals("STUDENT", response.role());
    }
}
