package com.timetable.backend.domain.repository;

import com.timetable.backend.domain.model.Timeslot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Optional;

@Repository
public interface TimeslotRepository extends JpaRepository<Timeslot, Long> {

    Optional<Timeslot> findByDayOfWeekAndStartTimeAndEndTime(
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
    );
}
