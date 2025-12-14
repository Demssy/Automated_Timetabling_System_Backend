package com.timetable.backend.security;

import com.timetable.backend.domain.model.*;
import com.timetable.backend.domain.repository.RoleRepository;
import com.timetable.backend.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
public class UserRepositoryJpaTest {

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    UserRepository userRepository;

    @Test
    void saveAndFindDifferentUserTypes() {
        Role adminRole = roleRepository.save(new Role(null, "ADMIN"));
        Role teacherRole = roleRepository.save(new Role(null, "TEACHER"));
        Role studentRole = roleRepository.save(new Role(null, "STUDENT"));

        Admin admin = new Admin(null, "admin@example.com", "hashedpw", "Admin User", adminRole, true, true);
        Teacher teacher = new Teacher(null, "teacher@example.com", "hashedpw", "Teacher User", teacherRole, true, 6, "#112233");
        Student student = new Student(null, "student@example.com", "hashedpw", "Student User", studentRole, true, LocalDate.of(2010, 1, 1), DanceLevel.BEGINNER, "+123456789");

        userRepository.save(admin);
        userRepository.save(teacher);
        userRepository.save(student);

        assertThat(userRepository.findByEmail("admin@example.com")).isPresent().get().isInstanceOf(Admin.class);
        assertThat(userRepository.findByEmail("teacher@example.com")).isPresent().get().isInstanceOf(Teacher.class);
        assertThat(userRepository.findByEmail("student@example.com")).isPresent().get().isInstanceOf(Student.class);

        var foundStudent = userRepository.findByEmail("student@example.com").orElseThrow();
        assertThat(((Student) foundStudent).getDanceLevel()).isEqualTo(DanceLevel.BEGINNER);
    }
}
