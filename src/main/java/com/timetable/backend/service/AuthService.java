package com.timetable.backend.service;

import com.timetable.backend.domain.dto.UserResponse;
import com.timetable.backend.domain.model.Role;
import com.timetable.backend.domain.model.Student;
import com.timetable.backend.domain.repository.RoleRepository;
import com.timetable.backend.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    // Lazy delegation — no circular dependency: AuthService → UserService → repositories
    private final UserService userService;

    public UserResponse registerStudent(String email, String password, String fullName, LocalDate birthDate) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already in use");
        }
        Role studentRole = roleRepository.findByName("STUDENT")
                .orElseGet(() -> roleRepository.save(new Role(null, "STUDENT")));
        Student student = new Student();
        student.setEmail(email);
        student.setPasswordHash(passwordEncoder.encode(password));
        student.setFullName(fullName);
        student.setRole(studentRole);
        student.setBirthDate(birthDate);
        userRepository.save(student);

        // Delegate to UserService so the response always reflects the full profile
        // (availability lists will be empty for a new user, but the pattern is consistent)
        return userService.getCurrentUserInfo(email);
    }

    public UserResponse authenticate(String email, String password) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));

        // Load the full profile including actual availability lists from DB
        return userService.getCurrentUserInfo(email);
    }
}