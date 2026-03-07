package com.timetable.backend.service;

import com.timetable.backend.domain.dto.CreateTeacherRequest;
import com.timetable.backend.domain.dto.TeacherResponse;
import com.timetable.backend.domain.dto.UpdateTeacherRequest;
import com.timetable.backend.domain.mapper.TeacherMapper;
import com.timetable.backend.domain.model.AbstractUser;
import com.timetable.backend.domain.model.DanceStyle;
import com.timetable.backend.domain.model.Role;
import com.timetable.backend.domain.model.Teacher;
import com.timetable.backend.domain.repository.DanceStyleRepository;
import com.timetable.backend.domain.repository.RoleRepository;
import com.timetable.backend.domain.repository.TeacherRepository;
import com.timetable.backend.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
    private final TeacherMapper teacherMapper;

    @Transactional(readOnly = true)
    public List<TeacherResponse> getAllTeachers() {
        return teacherRepository.findAll().stream()
                .map(teacherMapper::toTeacherResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TeacherResponse getTeacherById(Long id) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found with id: " + id));
        return teacherMapper.toTeacherResponse(teacher);
    }

    @Transactional
    public TeacherResponse updateTeacher(Long id, UpdateTeacherRequest request) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found with id: " + id));

        // fullName lives in the AbstractUser record — update it there
        if (request.fullName() != null && teacher.getUser() != null) {
            teacher.getUser().setFullName(request.fullName());
            userRepository.save(teacher.getUser());
        }

        teacher.setMaxDailyHours(request.maxDailyHours());
        teacher.setColorCode(request.colorCode());

        if (request.qualifiedStyleIds() != null) {
            Set<Long> requestedStyleIds = new HashSet<>(request.qualifiedStyleIds());
            List<DanceStyle> styles = danceStyleRepository.findAllById(requestedStyleIds);
            if (styles.size() != requestedStyleIds.size()) {
                throw new IllegalArgumentException("One or more DanceStyle IDs not found");
            }
            teacher.setDanceStyles(new HashSet<>(styles));
        }

        return teacherMapper.toTeacherResponse(teacherRepository.save(teacher));
    }

    @Transactional
    public void deleteTeacher(Long id) {
        if (!teacherRepository.existsById(id)) {
            throw new IllegalArgumentException("Teacher not found with id: " + id);
        }
        teacherRepository.deleteById(id);
    }

    /**
     * Promotes an existing user to a Teacher.
     * <p>
     * Since {@link Teacher} is now a standalone entity (not extending {@link AbstractUser}),
     * promotion works as follows:
     * <ol>
     *   <li>Load the existing {@link AbstractUser} by {@code userId}.</li>
     *   <li>Ensure the user is not already promoted (no Teacher record with the same id).</li>
     *   <li>Update the user's role to {@code TEACHER} in the {@code users} table.</li>
     *   <li>Create a new {@link Teacher} row with {@code id = userId} and the provided settings.</li>
     * </ol>
     *
     * @param request DTO with userId, maxDailyHours, colorCode, qualifiedStyleIds.
     * @return the created {@link TeacherResponse}.
     * @throws IllegalArgumentException if the user is not found, is already a Teacher,
     *                                  or any dance style ID is invalid.
     */
    @Transactional
    public TeacherResponse createTeacher(CreateTeacherRequest request) {
        // 1. Verify the source user exists
        AbstractUser user = userRepository.findById(request.userId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "User not found with id: " + request.userId()));

        // 2. Guard: prevent double-promotion
        if (teacherRepository.existsById(request.userId())) {
            throw new IllegalArgumentException(
                    "User with id " + request.userId() + " is already a Teacher");
        }

        // 3. Resolve (or create) the TEACHER role and update the user's role
        Role teacherRole = roleRepository.findByName("TEACHER")
                .orElseGet(() -> roleRepository.save(new Role(null, "TEACHER")));
        user.setRole(teacherRole);
        // Use the returned managed instance to avoid a detached-entity conflict on merge
        AbstractUser managedUser = userRepository.saveAndFlush(user);

        // 4. Build the Teacher record — do NOT call setId() manually when using @MapsId,
        //    Hibernate derives the PK from the associated user automatically.
        Teacher teacher = new Teacher();
        teacher.setUser(managedUser);
        teacher.setMaxDailyHours(
                request.maxDailyHours() != null ? request.maxDailyHours() : 8);
        teacher.setColorCode(
                request.colorCode() != null ? request.colorCode() : "#000000");

        // 5. Resolve and assign dance styles
        if (request.qualifiedStyleIds() != null && !request.qualifiedStyleIds().isEmpty()) {
            Set<Long> requestedStyleIds = new HashSet<>(request.qualifiedStyleIds());
            List<DanceStyle> styles = danceStyleRepository.findAllById(requestedStyleIds);
            if (styles.size() != requestedStyleIds.size()) {
                throw new IllegalArgumentException("One or more DanceStyle IDs not found");
            }
            teacher.setDanceStyles(new HashSet<>(styles));
        }

        return teacherMapper.toTeacherResponse(teacherRepository.save(teacher));
    }
}