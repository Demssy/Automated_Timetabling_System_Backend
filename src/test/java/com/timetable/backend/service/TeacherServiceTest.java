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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherServiceTest {

    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private DanceStyleRepository danceStyleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TeacherMapper teacherMapper;

    @InjectMocks
    private TeacherService teacherService;

    @Test
    void shouldCreateTeacherSuccessfully() {
        // Arrange
        CreateTeacherRequest request = new CreateTeacherRequest(
                "teacher@test.com", "password", "John Doe", 5, "#FFFFFF", Set.of(1L)
        );

        Role teacherRole = new Role(2L, "TEACHER");
        DanceStyle style = new DanceStyle("Salsa");
        style.setId(1L);
        Teacher teacher = new Teacher();
        teacher.setEmail(request.email());

        Teacher savedTeacher = new Teacher();
        savedTeacher.setId(10L);
        savedTeacher.setEmail(request.email());

        TeacherResponse response = new TeacherResponse(10L, "teacher@test.com", "John Doe", 5, "#FFFFFF", Set.of());

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(roleRepository.findByName("TEACHER")).thenReturn(Optional.of(teacherRole));
        when(teacherMapper.toTeacher(request)).thenReturn(teacher);
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPwd");
        when(danceStyleRepository.findAllById(request.qualifiedStyleIds())).thenReturn(List.of(style));
        when(teacherRepository.save(teacher)).thenReturn(savedTeacher);
        when(teacherMapper.toTeacherResponse(savedTeacher)).thenReturn(response);

        // Act
        TeacherResponse result = teacherService.createTeacher(request);

        // Assert
        assertNotNull(result);
        assertEquals(10L, result.id());
        verify(teacherRepository).save(teacher);

        assertEquals(teacherRole, teacher.getRole());
        assertEquals("encodedPwd", teacher.getPasswordHash());
        assertNotNull(teacher.getDanceStyles());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenEmailAlreadyExists() {
        CreateTeacherRequest request = new CreateTeacherRequest(
                "teacher@test.com", "password", "John Doe", 5, "#FFFFFF", Set.of()
        );
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> teacherService.createTeacher(request));
        verify(teacherRepository, never()).save(any());
    }
}

