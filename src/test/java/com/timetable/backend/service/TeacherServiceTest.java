package com.timetable.backend.service;

import com.timetable.backend.domain.dto.CreateTeacherRequest;
import com.timetable.backend.domain.dto.TeacherResponse;
import com.timetable.backend.domain.mapper.StudentMapper;
import com.timetable.backend.domain.mapper.TeacherMapper;
import com.timetable.backend.domain.model.AbstractUser;
import com.timetable.backend.domain.model.DanceStyle;
import com.timetable.backend.domain.model.Role;
import com.timetable.backend.domain.model.Teacher;
import com.timetable.backend.domain.repository.DanceStyleRepository;
import com.timetable.backend.domain.repository.RoleRepository;
import com.timetable.backend.domain.repository.TeacherRepository;
import com.timetable.backend.domain.repository.UserRepository;
import com.timetable.backend.domain.repository.WeeklyAvailabilityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private WeeklyAvailabilityRepository weeklyAvailabilityRepository;
    @Mock
    private TeacherMapper teacherMapper;
    @Mock
    private StudentMapper studentMapper;

    @InjectMocks
    private TeacherService teacherService;

    @Test
    void shouldCreateTeacherSuccessfully() {
        // Arrange: CreateTeacherRequest now promotes an existing user to Teacher by userId
        CreateTeacherRequest request = new CreateTeacherRequest(1L, 5, "#FFFFFF", Set.of(1L));

        Role teacherRole = new Role(2L, "TEACHER");
        DanceStyle style = new DanceStyle("Salsa");
        style.setId(1L);

        AbstractUser existingUser = mock(AbstractUser.class);

        Teacher savedTeacher = new Teacher();
        savedTeacher.setId(1L);
        savedTeacher.setUser(existingUser);

        TeacherResponse response = new TeacherResponse(1L, "teacher@test.com", "John Doe", 5, "#FFFFFF", Set.of());

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(teacherRepository.existsById(1L)).thenReturn(false);
        when(roleRepository.findByName("TEACHER")).thenReturn(Optional.of(teacherRole));
        when(userRepository.saveAndFlush(existingUser)).thenReturn(existingUser);
        when(danceStyleRepository.findAllById(any())).thenReturn(List.of(style));
        when(teacherRepository.save(any(Teacher.class))).thenReturn(savedTeacher);
        when(teacherMapper.toTeacherResponse(savedTeacher)).thenReturn(response);

        // Act
        TeacherResponse result = teacherService.createTeacher(request);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.id());
        verify(teacherRepository).save(any(Teacher.class));
        verify(existingUser).setRole(teacherRole);
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenUserNotFound() {
        // Arrange: userId 99 does not exist
        CreateTeacherRequest request = new CreateTeacherRequest(99L, 5, "#FFFFFF", Set.of());
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> teacherService.createTeacher(request));
        verify(teacherRepository, never()).save(any());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenUserAlreadyIsTeacher() {
        // Arrange: userId 1 exists in users table but is already promoted to Teacher
        CreateTeacherRequest request = new CreateTeacherRequest(1L, 5, "#FFFFFF", Set.of());
        AbstractUser existingUser = mock(AbstractUser.class);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(teacherRepository.existsById(1L)).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> teacherService.createTeacher(request));
        verify(teacherRepository, never()).save(any());
    }
}
