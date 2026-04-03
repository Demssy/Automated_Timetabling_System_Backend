package com.timetable.backend.repository;

import com.timetable.backend.domain.model.Admin;
import com.timetable.backend.domain.model.DanceStyle;
import com.timetable.backend.domain.model.Role;
import com.timetable.backend.domain.model.Teacher;
import com.timetable.backend.domain.repository.DanceStyleRepository;
import com.timetable.backend.domain.repository.RoleRepository;
import com.timetable.backend.domain.repository.TeacherRepository;
import com.timetable.backend.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
public class DanceStyleRepositoryJpaTest {

    @Autowired
    DanceStyleRepository danceStyleRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    TeacherRepository teacherRepository;

    @Test
    void saveAndFindByNameAndAssociateWithTeacher() {
        DanceStyle salsa = danceStyleRepository.save(new DanceStyle("Salsa"));

        var found = danceStyleRepository.findByName("Salsa");
        assertThat(found).isPresent();

        // Teacher now uses composition: first persist an AbstractUser (Admin stub), then Teacher
        Role teacherRole = roleRepository.save(new Role(null, "TEACHER"));
        Admin userStub = new Admin();
        userStub.setEmail("t2@example.com");
        userStub.setPasswordHash("hashedpw");
        userStub.setFullName("T2");
        userStub.setRole(teacherRole);
        Admin savedUser = (Admin) userRepository.save(userStub);

        Teacher teacher = new Teacher();
        teacher.setUser(savedUser);
        teacher.setMaxDailyHours(5);
        teacher.setColorCode("#ffeecc");
        Teacher savedTeacher = teacherRepository.save(teacher);

        // associate teacher with dance style
        salsa.getTeachers().add(savedTeacher);
        danceStyleRepository.save(salsa);

        var reloaded = danceStyleRepository.findByName("Salsa").orElseThrow();
        assertThat(reloaded.getTeachers()).isNotEmpty();
    }
}
