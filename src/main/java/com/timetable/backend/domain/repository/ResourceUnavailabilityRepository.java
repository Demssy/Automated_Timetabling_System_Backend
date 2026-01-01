package com.timetable.backend.domain.repository;

import com.timetable.backend.domain.model.ResourceUnavailability;
import com.timetable.backend.domain.model.Teacher;
import com.timetable.backend.domain.model.Timeslot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceUnavailabilityRepository extends JpaRepository<ResourceUnavailability, Long> {

    List<ResourceUnavailability> findByTeacher(Teacher teacher);

    List<ResourceUnavailability> findByTimeslot(Timeslot timeslot);
}

