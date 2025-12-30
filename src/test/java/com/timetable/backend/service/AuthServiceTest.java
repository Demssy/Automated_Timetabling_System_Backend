package com.timetable.backend.service;

import com.timetable.backend.domain.model.Role;
import com.timetable.backend.domain.model.Student;
import com.timetable.backend.domain.repository.RoleRepository;
import com.timetable.backend.domain.repository.UserRepository;
import com.timetable.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
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
    private JwtService jwtService;
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
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("jwt-token");

        String token = authService.registerStudent(email, password, fullName, birthDate);

        assertNotNull(token);
        assertEquals("jwt-token", token);
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
        UserDetails userDetails = mock(UserDetails.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");

        String token = authService.authenticate(email, password);

        assertEquals("jwt-token", token);
    }
}
