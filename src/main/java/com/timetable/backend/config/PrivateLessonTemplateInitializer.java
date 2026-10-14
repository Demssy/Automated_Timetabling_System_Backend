package com.timetable.backend.config;

import com.timetable.backend.domain.repository.TeacherRepository;
import com.timetable.backend.service.LessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PrivateLessonTemplateInitializer implements ApplicationRunner {

    private static final int TEMPLATES_PER_TEACHER = 5;

    private final TeacherRepository teacherRepository;
    private final LessonService lessonService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        teacherRepository.findAll()
                .forEach(teacher -> lessonService.createDefaultPrivateLessonTemplates(teacher, TEMPLATES_PER_TEACHER));
    }
}
