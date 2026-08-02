package com.timetable.backend.service;

import com.timetable.backend.domain.dto.RegisterRequest;
import com.timetable.backend.domain.dto.UserResponse;
import com.timetable.backend.domain.model.AbstractUser;
import com.timetable.backend.domain.model.DanceStyle;
import com.timetable.backend.domain.model.Role;
import com.timetable.backend.domain.model.Student;
import com.timetable.backend.domain.model.Teacher;
import com.timetable.backend.domain.repository.DanceStyleRepository;
import com.timetable.backend.domain.repository.RoleRepository;
import com.timetable.backend.domain.repository.TeacherRepository;
import com.timetable.backend.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TeacherRepository teacherRepository;
    private final DanceStyleRepository danceStyleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    // Lazy delegation — no circular dependency: AuthService → UserService → repositories
    private final UserService userService;

    /**
     * Registers a new user with the given role.
     *
     * <ul>
     *   <li>ADMIN role is rejected with {@link IllegalArgumentException} (HTTP 400).</li>
     *   <li>STUDENT: account is active immediately; dance-specific fields are persisted.</li>
     *   <li>TEACHER: account starts <em>inactive</em> (pending admin approval);
     *       phone, bio and dance specializations are persisted on the Teacher profile.</li>
     * </ul>
     *
     * @param request validated registration payload
     * @return {@link UserResponse} for the newly created user
     * @throws IllegalArgumentException if email already in use, role is ADMIN, or
     *                                  TEACHER registration lacks at least one specialization
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already in use");
        }

        return switch (request.role()) {
            case ADMIN -> throw new IllegalArgumentException(
                    "ADMIN accounts cannot be created via public registration");
            case STUDENT -> registerStudent(request);
            case TEACHER -> registerTeacher(request);
        };
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private UserResponse registerStudent(RegisterRequest request) {
        Role studentRole = resolveRole("STUDENT");

        var student = new Student();
        student.setEmail(request.email());
        student.setPasswordHash(passwordEncoder.encode(request.password()));
        student.setFullName(request.fullName());
        student.setRole(studentRole);
        student.setBirthDate(request.birthDate());
        student.setActive(true);
        student.setDanceLevel(request.danceLevel());
        student.setParentContact(request.parentContact());
        student.setDesiredLessonsPerWeek(request.desiredLessonsPerWeek());

        userRepository.save(student);
        log.info("Registered new STUDENT: {}", request.email());

        return userService.getCurrentUserInfo(request.email());
    }

    /**
     * Creates a Teacher account.
     * The base {@link AbstractUser} record (in the {@code users} table) is created first;
     * then a {@link Teacher} profile (in the {@code teachers} table) is linked via @MapsId.
     * The account is active immediately — the teacher can log in right after registration.
     */
    private UserResponse registerTeacher(RegisterRequest request) {
        if (request.qualifiedStyleIds() == null || request.qualifiedStyleIds().isEmpty()) {
            throw new IllegalArgumentException("At least one specialization is required for TEACHER registration");
        }

        Role teacherRole = resolveRole("TEACHER");

        // 1. Persist the base user row (AbstractUser is non-abstract — plain users table row)
        var baseUser = new AbstractUser();
        baseUser.setEmail(request.email());
        baseUser.setPasswordHash(passwordEncoder.encode(request.password()));
        baseUser.setFullName(request.fullName());
        baseUser.setRole(teacherRole);
        baseUser.setActive(true);

        AbstractUser savedUser = userRepository.saveAndFlush(baseUser);

        // 2. Resolve dance style entities by ID or name
        Set<DanceStyle> styles = resolveDanceStylesByIdOrName(request.qualifiedStyleIds());

        // 3. Create the Teacher profile linked to the base user
        var teacher = new Teacher();
        teacher.setUser(savedUser);
        teacher.setPhone(request.phone());
        teacher.setBio(request.bio());
        teacher.setDanceStyles(styles);

        teacherRepository.save(teacher);
        log.info("Registered new TEACHER (pending approval): {}", request.email());

        return userService.getCurrentUserInfo(request.email());
    }

    private Role resolveRole(String name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(new Role(null, name)));
    }

    /**
     * Looks up {@link DanceStyle} entities by ID or name.
     * Supports mixed lists: [1L, "SALSA", 2L, "BACHATA"]
     * Unknown IDs/names cause an {@link IllegalArgumentException} (HTTP 400).
     */
    private Set<DanceStyle> resolveDanceStylesByIdOrName(List<?> identifiers) {
        var styles = new HashSet<DanceStyle>();
        for (Object id : identifiers) {
            DanceStyle style = null;

            if (id instanceof Number num) {
                // ID path: Long, Integer, etc.
                Long styleId = num.longValue();
                style = danceStyleRepository.findById(styleId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Unknown dance style with ID: " + styleId));
            } else if (id instanceof String name) {
                // Name path: String
                style = danceStyleRepository.findByName(name)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Unknown dance style: " + name));
            } else {
                throw new IllegalArgumentException(
                        "Invalid style identifier type: " + (id != null ? id.getClass().getName() : "null"));
            }

            styles.add(style);
        }
        return styles;
    }

    public UserResponse authenticate(String email, String password) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));

        // Load the full profile including actual availability lists from DB
        return userService.getCurrentUserInfo(email);
    }
}