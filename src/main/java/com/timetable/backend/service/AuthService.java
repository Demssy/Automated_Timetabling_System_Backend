package com.timetable.backend.service;

import com.timetable.backend.domain.dto.UserResponse;
import com.timetable.backend.domain.model.AbstractUser;
import com.timetable.backend.domain.model.Role;
import com.timetable.backend.domain.model.Student;
import com.timetable.backend.domain.repository.RoleRepository;
import com.timetable.backend.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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

    public UserResponse registerStudent(String email, String password, String fullName, LocalDate birthDate) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already in use");
        }
        Role studentRole = roleRepository.findByName("STUDENT").orElseGet(() -> roleRepository.save(new Role(null, "STUDENT")));
        Student student = new Student();
        student.setEmail(email);
        student.setPasswordHash(passwordEncoder.encode(password));
        student.setFullName(fullName);
        student.setRole(studentRole);
        student.setBirthDate(birthDate);
        userRepository.save(student);

        return new UserResponse(
            student.getId(),
            student.getEmail(),
            student.getFullName(),
            student.getRole().getName(),
            student.isActive()
        );
    }

    public UserResponse authenticate(String email, String password) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));

        AbstractUser user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getRole().getName(),
            user.isActive()
        );
    }
}