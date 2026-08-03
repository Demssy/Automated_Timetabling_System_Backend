package com.timetable.backend.service;
import com.timetable.backend.domain.dto.CreateTeacherRequest;
import com.timetable.backend.domain.dto.StudentAvailabilityResponse;
import com.timetable.backend.domain.dto.StudentResponse;
import com.timetable.backend.domain.dto.TeacherResponse;
import com.timetable.backend.domain.dto.UpdateTeacherRequest;
import com.timetable.backend.domain.dto.WeeklyAvailabilityDTO;
import com.timetable.backend.domain.mapper.StudentMapper;
import com.timetable.backend.domain.mapper.TeacherMapper;
import com.timetable.backend.domain.model.*;
import com.timetable.backend.domain.repository.DanceStyleRepository;
import com.timetable.backend.domain.repository.LessonRepository;
import com.timetable.backend.domain.repository.RoomRepository;
import com.timetable.backend.domain.repository.RoleRepository;
import com.timetable.backend.domain.repository.TeacherRepository;
import com.timetable.backend.domain.repository.UserRepository;
import com.timetable.backend.domain.repository.WeeklyAvailabilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.ArrayList;
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
    private final LessonRepository lessonRepository;
    private final WeeklyAvailabilityRepository weeklyAvailabilityRepository;
    private final TeacherMapper teacherMapper;
    private final StudentMapper studentMapper;
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
        if (request.fullName() != null && teacher.getUser() != null) {
            teacher.getUser().setFullName(request.fullName());
            userRepository.save(teacher.getUser());
        }
        teacher.setMaxDailyHours(request.maxDailyHours());
        teacher.setColorCode(request.colorCode());
        teacher.setDesiredLessonsPerWeek(request.desiredLessonsPerWeek());
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
    @Transactional
    public TeacherResponse createTeacher(CreateTeacherRequest request) {
        AbstractUser user = userRepository.findById(request.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + request.userId()));
        if (teacherRepository.existsById(request.userId())) {
            throw new IllegalArgumentException("User with id " + request.userId() + " is already a Teacher");
        }
        Role teacherRole = roleRepository.findByName("TEACHER")
                .orElseGet(() -> roleRepository.save(new Role(null, "TEACHER")));
        user.setRole(teacherRole);
        AbstractUser managedUser = userRepository.saveAndFlush(user);
        Teacher teacher = new Teacher();
        teacher.setUser(managedUser);
        teacher.setMaxDailyHours(request.maxDailyHours() != null ? request.maxDailyHours() : 8);
        teacher.setColorCode(request.colorCode() != null ? request.colorCode() : "#000000");
        if (request.qualifiedStyleIds() != null && !request.qualifiedStyleIds().isEmpty()) {
            Set<Long> requestedStyleIds = new HashSet<>(request.qualifiedStyleIds());
            List<DanceStyle> styles = danceStyleRepository.findAllById(requestedStyleIds);
            if (styles.size() != requestedStyleIds.size()) {
                throw new IllegalArgumentException("One or more DanceStyle IDs not found");
            }
            teacher.setDanceStyles(new HashSet<>(styles));
        }
        Teacher savedTeacher = teacherRepository.save(teacher);
        

        List<Lesson> defaultLessons = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            Lesson lesson = new Lesson();
            lesson.setTeacher(savedTeacher);
            lesson.setDurationMinutes(60);
            lesson.setPrivate(true);
            lesson.setActive(true);
            lesson.setPinned(false);
            lesson.setRoom(null);
            lesson.setTimeslot(null);
            lesson.setStudent(null);
            defaultLessons.add(lesson);
        }
        lessonRepository.saveAllAndFlush(defaultLessons);

        return teacherMapper.toTeacherResponse(savedTeacher);
    }
    @Transactional(readOnly = true)
    public List<StudentResponse> getMyStudents(String teacherEmail) {
        Teacher teacher = resolveTeacherByEmail(teacherEmail);
        return teacher.getPrivateStudents().stream()
                .map(studentMapper::toStudentResponse)
                .toList();
    }
    @Transactional(readOnly = true)
    public List<StudentResponse> getStudentsForTeacher(Long teacherId) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found with id: " + teacherId));
        return teacher.getPrivateStudents().stream()
                .map(studentMapper::toStudentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TeacherResponse getMyProfile(String email) {
        Teacher teacher = resolveTeacherByEmail(email);
        return teacherMapper.toTeacherResponse(teacher);
    }

    @Transactional
    public TeacherResponse updateMyProfile(String email, UpdateTeacherRequest request) {
        Teacher teacher = resolveTeacherByEmail(email);
        if (request.fullName() != null && teacher.getUser() != null) {
            teacher.getUser().setFullName(request.fullName());
            userRepository.save(teacher.getUser());
        }
        if (request.maxDailyHours() != null) {
            teacher.setMaxDailyHours(request.maxDailyHours());
        }
        teacher.setDesiredLessonsPerWeek(request.desiredLessonsPerWeek());
        if (request.colorCode() != null) {
            teacher.setColorCode(request.colorCode());
        }
        if (request.qualifiedStyleIds() != null) {
            var requestedStyleIds = new HashSet<>(request.qualifiedStyleIds());
            var styles = danceStyleRepository.findAllById(requestedStyleIds);
            if (styles.size() != requestedStyleIds.size()) {
                throw new IllegalArgumentException("One or more DanceStyle IDs not found");
            }
            teacher.setDanceStyles(new HashSet<>(styles));
        }
        return teacherMapper.toTeacherResponse(teacherRepository.save(teacher));
    }

    private Teacher resolveTeacherByEmail(String email) {
        AbstractUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
        return teacherRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Teacher profile not found for: " + email));
    }

    /**
     * Returns the weekly availability of a student, accessible only to the teacher
     * who has that student in their private lesson pool.
     *
     * @param teacherEmail email of the authenticated teacher (from JWT principal)
     * @param studentId    ID of the student whose availability is requested
     * @return StudentAvailabilityResponse with list of WeeklyAvailabilityDTO
     * @throws ResponseStatusException 403 if student is not in the teacher's pool
     * @throws ResponseStatusException 404 if the student does not exist
     */
    @Transactional(readOnly = true)
    public StudentAvailabilityResponse getStudentAvailability(String teacherEmail, Long studentId) {
        Teacher teacher = resolveTeacherByEmail(teacherEmail);

        // Security check: ensure studentId belongs to this teacher's private pool
        if (!teacherRepository.isStudentOfTeacher(teacher.getId(), studentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Student with id " + studentId + " is not in your private lesson pool");
        }

        // Verify student exists (repository query returns empty list if not, but explicit check is cleaner)
        if (!userRepository.existsById(studentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found with id: " + studentId);
        }

        List<WeeklyAvailabilityDTO> dtos = weeklyAvailabilityRepository.findByUserId(studentId)
                .stream()
                .map(w -> new WeeklyAvailabilityDTO(
                        w.getId(),
                        w.getDayOfWeek(),
                        w.getStartTime(),
                        w.getEndTime()))
                .toList();

        return new StudentAvailabilityResponse(dtos);
    }
}