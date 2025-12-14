package com.timetable.backend.repository;

import com.timetable.backend.domain.model.Room;
import com.timetable.backend.domain.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
public class RoomRepositoryJpaTest {

    @Autowired
    RoomRepository roomRepository;

    @Test
    void saveAndFindByName() {
        Room r = new Room(null, "Main Hall", 30, false);
        roomRepository.save(r);

        var found = roomRepository.findByName("Main Hall");
        assertThat(found).isPresent();
        assertThat(found.get().getCapacity()).isEqualTo(30);
    }
}

