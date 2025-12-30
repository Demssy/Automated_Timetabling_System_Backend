package com.timetable.backend.service;

import com.timetable.backend.domain.model.Role;
import com.timetable.backend.domain.model.Student;
import com.timetable.backend.domain.repository.RoleRepository;
import com.timetable.backend.domain.repository.UserRepository;
import com.timetable.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public String registerStudent(String email, String password, String fullName, LocalDate birthDate) {
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
        UserDetails ud = new org.springframework.security.core.userdetails.User(student.getEmail(), student.getPasswordHash(), java.util.Collections.emptyList());
        return jwtService.generateToken(ud);
    }

    public String authenticate(String email, String password) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
        UserDetails ud = (UserDetails) authentication.getPrincipal();
        return jwtService.generateToken(ud);
    }
}