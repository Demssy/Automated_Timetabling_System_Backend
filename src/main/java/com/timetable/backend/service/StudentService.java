package com.timetable.backend.service;

import com.timetable.backend.domain.dto.StudentResponse;
import com.timetable.backend.domain.exception.ResourceNotFoundException;
import com.timetable.backend.domain.mapper.StudentMapper;
import com.timetable.backend.domain.model.DanceLevel;
import com.timetable.backend.domain.model.Student;
import com.timetable.backend.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Implementation of student service.
 * Handles student CRUD operations and queries.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StudentService implements IStudentService {

    private final UserRepository userRepository;
    private final StudentMapper studentMapper;

    @Override
    public List<StudentResponse> getAllStudents() {
        log.info("Retrieving all students");

        return userRepository.findAll().stream()
                .filter(user -> user instanceof Student)
                .map(user -> (Student) user)
                .map(studentMapper::toStudentResponse)
                .toList();
    }

    @Override
    public StudentResponse getStudentById(Long id) {
        log.info("Retrieving student with id: {}", id);

        var user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", id));

        if (!(user instanceof Student student)) {
            throw new ResourceNotFoundException("User with id " + id + " is not a Student");
        }

        return studentMapper.toStudentResponse(student);
    }

    @Override
    @Transactional
    public StudentResponse updateStudent(Long id, String fullName, LocalDate birthDate,
                                          DanceLevel danceLevel, String parentContact) {
        log.info("Updating student with id: {}", id);

        var user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", id));

        if (!(user instanceof Student student)) {
            throw new ResourceNotFoundException("User with id " + id + " is not a Student");
        }

        // Update fields (only if provided)
        if (fullName != null) {
            student.setFullName(fullName);
        }
        if (birthDate != null) {
            student.setBirthDate(birthDate);
        }
        if (danceLevel != null) {
            student.setDanceLevel(danceLevel);
        }
        if (parentContact != null) {
            student.setParentContact(parentContact);
        }

        var updated = userRepository.save(student);
        log.info("Successfully updated student with id: {}", id);

        return studentMapper.toStudentResponse((Student) updated);
    }

    @Override
    @Transactional
    public void deleteStudent(Long id) {
        log.info("Deleting student with id: {}", id);

        var user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", id));

        if (!(user instanceof Student)) {
            throw new ResourceNotFoundException("User with id " + id + " is not a Student");
        }

        userRepository.deleteById(id);
        log.info("Successfully deleted student with id: {}", id);
    }

    @Override
    public List<StudentResponse> getStudentsByDanceLevel(DanceLevel danceLevel) {
        log.info("Retrieving students with dance level: {}", danceLevel);

        return userRepository.findAll().stream()
                .filter(user -> user instanceof Student)
                .map(user -> (Student) user)
                .filter(student -> danceLevel.equals(student.getDanceLevel()))
                .map(studentMapper::toStudentResponse)
                .toList();
    }
}

