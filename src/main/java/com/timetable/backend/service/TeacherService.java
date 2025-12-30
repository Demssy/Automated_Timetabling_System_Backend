package com.timetable.backend.service;

import com.timetable.backend.domain.dto.CreateTeacherRequest;
import com.timetable.backend.domain.dto.TeacherResponse;
import com.timetable.backend.domain.mapper.TeacherMapper;
import com.timetable.backend.domain.model.DanceStyle;
import com.timetable.backend.domain.model.Role;
import com.timetable.backend.domain.model.Teacher;
import com.timetable.backend.domain.repository.DanceStyleRepository;
import com.timetable.backend.domain.repository.RoleRepository;
import com.timetable.backend.domain.repository.TeacherRepository;
import com.timetable.backend.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DanceStyleRepository danceStyleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TeacherMapper teacherMapper;

    /**
     * Creates a new Teacher entity based on the provided request.
     * <p>
     * This method performs the following steps:
     * <ul>
     *     <li>Validates that the email is not already in use.</li>
     *     <li>Retrieves or creates the "TEACHER" role.</li>
     *     <li>Maps the request DTO to a Teacher entity.</li>
     *     <li>Encodes the password.</li>
     *     <li>Validates and associates the teacher with the specified dance styles.</li>
     * </ul>
     *
     * @param request the DTO containing teacher details (email, password, name, styles).
     * @return the created TeacherResponse DTO.
     * @throws IllegalArgumentException if a user with the given email already exists or if any dance style ID is invalid.
     */
    @Transactional
    public TeacherResponse createTeacher(CreateTeacherRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already in use");
        }

        Role teacherRole = roleRepository.findByName("TEACHER")
                .orElseGet(() -> roleRepository.save(new Role(null, "TEACHER")));

        Teacher teacher = teacherMapper.toTeacher(request);
        teacher.setPasswordHash(passwordEncoder.encode(request.password()));
        teacher.setRole(teacherRole);

        if (request.qualifiedStyleIds() != null && !request.qualifiedStyleIds().isEmpty()) {
            // Use a Set to ensure unique IDs from the request
            Set<Long> requestedStyleIds = new HashSet<>(request.qualifiedStyleIds());
            List<DanceStyle> styles = danceStyleRepository.findAllById(requestedStyleIds);

            if (styles.size() != requestedStyleIds.size()) {
                throw new IllegalArgumentException("One or more DanceStyle IDs not found");
            }
            teacher.setDanceStyles(new HashSet<>(styles));
        }

        Teacher saved = teacherRepository.save(teacher);
        return teacherMapper.toTeacherResponse(saved);
    }
}