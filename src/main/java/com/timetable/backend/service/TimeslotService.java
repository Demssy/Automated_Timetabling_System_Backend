package com.timetable.backend.service;

import com.timetable.backend.domain.dto.TimeslotDTO;
import com.timetable.backend.domain.mapper.DictionaryMapper;
import com.timetable.backend.domain.model.Timeslot;
import com.timetable.backend.domain.repository.TimeslotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing Timeslot entities.
 * Timeslots are Problem Facts in the Timefold Solver — they define the
 * available time slots for scheduling lessons across the week.
 */
@Service
@RequiredArgsConstructor
public class TimeslotService {

    private final TimeslotRepository timeslotRepository;
    private final DictionaryMapper dictionaryMapper;

    /**
     * Returns all timeslots, sorted implicitly by DB order.
     *
     * @return list of all TimeslotDTOs
     */
    @Transactional(readOnly = true)
    public List<TimeslotDTO> getAllTimeslots() {
        return timeslotRepository.findAll().stream()
                .map(dictionaryMapper::toTimeslotDTO)
                .toList();
    }

    /**
     * Returns a timeslot by ID.
     *
     * @param id timeslot identifier
     * @return TimeslotDTO
     * @throws IllegalArgumentException if timeslot not found
     */
    @Transactional(readOnly = true)
    public TimeslotDTO getTimeslotById(Long id) {
        Timeslot timeslot = timeslotRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Timeslot not found with id: " + id));
        return dictionaryMapper.toTimeslotDTO(timeslot);
    }

    /**
     * Creates a new timeslot.
     * Checks for uniqueness by (dayOfWeek, startTime, endTime).
     *
     * @param dto timeslot data
     * @return created TimeslotDTO
     * @throws IllegalArgumentException if a timeslot with the same slot already exists
     */
    @Transactional
    public TimeslotDTO createTimeslot(TimeslotDTO dto) {
        timeslotRepository
                .findByDayOfWeekAndStartTimeAndEndTime(dto.dayOfWeek(), dto.startTime(), dto.endTime())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "Timeslot already exists: " + dto.dayOfWeek() + " " + dto.startTime() + "-" + dto.endTime()
                    );
                });

        Timeslot saved = timeslotRepository.save(dictionaryMapper.toTimeslot(dto));
        return dictionaryMapper.toTimeslotDTO(saved);
    }

    /**
     * Updates an existing timeslot.
     *
     * @param id  timeslot identifier
     * @param dto updated timeslot data
     * @return updated TimeslotDTO
     * @throws IllegalArgumentException if timeslot not found
     */
    @Transactional
    public TimeslotDTO updateTimeslot(Long id, TimeslotDTO dto) {
        Timeslot timeslot = timeslotRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Timeslot not found with id: " + id));

        timeslot.setDayOfWeek(dto.dayOfWeek());
        timeslot.setStartTime(dto.startTime());
        timeslot.setEndTime(dto.endTime());

        return dictionaryMapper.toTimeslotDTO(timeslotRepository.save(timeslot));
    }

    /**
     * Deletes a timeslot by ID.
     *
     * @param id timeslot identifier
     * @throws IllegalArgumentException if timeslot not found
     */
    @Transactional
    public void deleteTimeslot(Long id) {
        if (!timeslotRepository.existsById(id)) {
            throw new IllegalArgumentException("Timeslot not found with id: " + id);
        }
        timeslotRepository.deleteById(id);
    }
}
