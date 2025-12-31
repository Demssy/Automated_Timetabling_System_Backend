package com.timetable.backend.domain.mapper;

import com.timetable.backend.domain.dto.CreateTeacherRequest;
import com.timetable.backend.domain.dto.TeacherResponse;
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
        Teacher teacher = new Teacher();
        teacher.setId(1L);
        teacher.setEmail("teacher@test.com");
        teacher.setFullName("John Doe");
        teacher.setMaxDailyHours(5);
        teacher.setColorCode("#FFFFFF");

        DanceStyle style = new DanceStyle("Salsa");
        style.setId(10L);
        teacher.setDanceStyles(Set.of(style));

        TeacherResponse response = teacherMapper.toTeacherResponse(teacher);

        assertNotNull(response);
        assertEquals(teacher.getId(), response.id());
        assertEquals(teacher.getEmail(), response.email());
        assertEquals(teacher.getFullName(), response.fullName());
        assertEquals(teacher.getMaxDailyHours(), response.maxDailyHours());
        assertEquals(teacher.getColorCode(), response.colorCode());
        assertEquals(1, response.qualifiedStyles().size());
        assertEquals("Salsa", response.qualifiedStyles().iterator().next().name());
    }

    @Test
    void toTeacher() {
        CreateTeacherRequest request = new CreateTeacherRequest(
                "teacher@test.com", "password", "John Doe", 5, "#FFFFFF", Set.of(10L)
        );

        Teacher teacher = teacherMapper.toTeacher(request);

        assertNotNull(teacher);
        assertEquals(request.email(), teacher.getEmail());
        assertEquals(request.fullName(), teacher.getFullName());
        assertEquals(request.maxDailyHours(), teacher.getMaxDailyHours());
        assertEquals(request.colorCode(), teacher.getColorCode());
        // danceStyles are ignored in mapping as per annotation, handled in service
    }
}
