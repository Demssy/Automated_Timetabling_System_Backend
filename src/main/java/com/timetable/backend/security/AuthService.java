package com.timetable.backend.security;

import com.timetable.backend.domain.model.Role;
import com.timetable.backend.domain.model.Student;
import com.timetable.backend.domain.model.AbstractUser;
import com.timetable.backend.domain.repository.RoleRepository;
import com.timetable.backend.domain.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, JwtService jwtService, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public String registerStudent(String email, String password, String fullName, LocalDate birthDate) {
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

