package com.timetable.backend.domain.mapper;

import com.timetable.backend.domain.dto.TeacherResponse;
import com.timetable.backend.domain.model.AbstractUser;
import com.timetable.backend.domain.model.DanceStyle;
import com.timetable.backend.domain.model.Teacher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TeacherMapperTest {

    private final TeacherMapper teacherMapper = Mappers.getMapper(TeacherMapper.class);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(teacherMapper, "dictionaryMapper", Mappers.getMapper(DictionaryMapper.class));
    }

    @Test
    void shouldMapTeacherToTeacherResponse_whenGivenValidTeacher() {
        // Teacher now uses composition — email and fullName live on the nested AbstractUser
        AbstractUser user = new AbstractUser() {};
        user.setId(1L);
        user.setEmail("teacher@test.com");
        user.setPasswordHash("hashedPassword");
        user.setFullName("John Doe");

        Teacher teacher = new Teacher();
        teacher.setId(1L);
        teacher.setUser(user);
        teacher.setMaxDailyHours(5);
        teacher.setColorCode("#FFFFFF");

        DanceStyle style = new DanceStyle("Salsa");
        style.setId(10L);
        teacher.setDanceStyles(Set.of(style));

        TeacherResponse response = teacherMapper.toTeacherResponse(teacher);

        assertNotNull(response);
        assertEquals(teacher.getId(),             response.id());
        assertEquals(user.getEmail(),             response.email());
        assertEquals(user.getFullName(),          response.fullName());
        assertEquals(teacher.getMaxDailyHours(),  response.maxDailyHours());
        assertEquals(teacher.getColorCode(),      response.colorCode());
        assertEquals(1, response.qualifiedStyles().size());
        assertEquals("Salsa", response.qualifiedStyles().iterator().next().name());
    }
}
