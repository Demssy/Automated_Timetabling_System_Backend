package com.timetable.backend.repository;

import com.timetable.backend.domain.model.DanceStyle;
import com.timetable.backend.domain.model.Role;
import com.timetable.backend.domain.model.Teacher;
import com.timetable.backend.domain.repository.DanceStyleRepository;
import com.timetable.backend.domain.repository.RoleRepository;
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

    @Test
    void saveAndFindByNameAndAssociateWithTeacher() {
        DanceStyle salsa = danceStyleRepository.save(new DanceStyle("Salsa"));

        var found = danceStyleRepository.findByName("Salsa");
        assertThat(found).isPresent();

        Role teacherRole = roleRepository.save(new Role(null, "TEACHER"));
        Teacher teacher = new Teacher(null, "t2@example.com", "hashedpw", "T2", teacherRole, true, 5, "#ffeecc");
        userRepository.save(teacher);

        // associate
        salsa.getTeachers().add(teacher);
        danceStyleRepository.save(salsa);

        var reloaded = danceStyleRepository.findByName("Salsa").orElseThrow();
        assertThat(reloaded.getTeachers()).isNotEmpty();
    }
}
