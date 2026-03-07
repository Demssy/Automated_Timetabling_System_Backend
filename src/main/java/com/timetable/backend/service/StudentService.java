package com.timetable.backend.service;

import com.timetable.backend.domain.dto.StudentDTO;
import com.timetable.backend.domain.mapper.StudentMapper;
import com.timetable.backend.domain.model.Role;
import com.timetable.backend.domain.model.Student;
import com.timetable.backend.domain.repository.RoleRepository;
import com.timetable.backend.domain.repository.StudentRepository;
import com.timetable.backend.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final StudentMapper studentMapper;

    @Transactional(readOnly = true)
    public List<StudentDTO> getAllStudents() {
        return studentRepository.findAll().stream()
                .map(studentMapper::toStudentDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentDTO getStudentById(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with id: " + id));
        return studentMapper.toStudentDTO(student);
    }

    @Transactional
    public StudentDTO createStudent(StudentDTO request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already in use");
        }

        Role studentRole = roleRepository.findByName("STUDENT")
                .orElseGet(() -> roleRepository.save(new Role(null, "STUDENT")));

        Student student = studentMapper.toStudent(request);
        if (request.password() != null) {
            student.setPasswordHash(passwordEncoder.encode(request.password()));
        } else {
            // Set default or throw error, here we could generate a temp password?
            // For now let's assume password is required for create, but handled in DTO request as optional to reuse dto
             throw new IllegalArgumentException("Password is required for new students");
        }
        student.setRole(studentRole);

        Student saved = studentRepository.save(student);
        return studentMapper.toStudentDTO(saved);
    }

    @Transactional
    public StudentDTO updateStudent(Long id, StudentDTO request) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with id: " + id));

        if (!student.getEmail().equals(request.email()) && userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already in use");
        }

        student.setEmail(request.email());
        student.setFullName(request.fullName());
        student.setBirthDate(request.birthDate());
        student.setDanceLevel(request.danceLevel());
        student.setParentContact(request.parentContact());

        if (request.password() != null && !request.password().isBlank()) {
            student.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        Student saved = studentRepository.save(student);
        return studentMapper.toStudentDTO(saved);
    }

    @Transactional
    public void deleteStudent(Long id) {
        if (!studentRepository.existsById(id)) {
            throw new IllegalArgumentException("Student not found with id: " + id);
        }
        studentRepository.deleteById(id);
    }
}

