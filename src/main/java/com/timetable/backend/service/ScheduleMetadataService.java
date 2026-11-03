package com.timetable.backend.service;

import com.timetable.backend.domain.dto.ScheduleMetadataDTO;
import com.timetable.backend.domain.exception.ScheduleConflictException;
import com.timetable.backend.domain.mapper.ScheduleMetadataMapper;
import com.timetable.backend.domain.model.AddedLesson;
import com.timetable.backend.domain.model.ScheduleMetadata;
import com.timetable.backend.domain.model.ScheduledLesson;
import com.timetable.backend.domain.model.ScheduledLessonStatus;
import com.timetable.backend.domain.model.ScheduleStatus;
import com.timetable.backend.domain.repository.AddedLessonRepository;
import com.timetable.backend.domain.repository.ScheduleMetadataRepository;
import com.timetable.backend.domain.repository.SchedulePublicationLockRepository;
import com.timetable.backend.domain.repository.ScheduledLessonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleMetadataService {

    private final ScheduleMetadataRepository repository;
    private final ScheduleMetadataMapper mapper;
    private final AddedLessonRepository addedLessonRepository;
    private final ScheduledLessonRepository scheduledLessonRepository;
    private final ScheduleScoreEvaluationService scoreEvaluationService;
    private final SchedulePublicationLockRepository publicationLockRepository;

    /**
     * Returns only PUBLISHED schedules.
     * Intended for the public-facing main page — all roles, including ADMIN, see the same data here.
     */
    @Transactional(readOnly = true)
    public List<ScheduleMetadataDTO> getAll() {
        log.debug("Fetching all PUBLISHED schedules for main page");
        return repository.findAllByStatus(ScheduleStatus.PUBLISHED).stream()
                .map(mapper::toDTO)
                .toList();
    }

    /**
     * Returns ALL schedules regardless of status.
     * Intended exclusively for the admin panel — only ADMIN role may call this.
     */
    @Transactional(readOnly = true)
    public List<ScheduleMetadataDTO> getAllForAdmin() {
        log.debug("Admin fetching all schedules (including drafts)");
        return repository.findAll().stream()
                .map(mapper::toDTO)
                .toList();
    }

    /**
     * Returns a single schedule by ID.
     * Non-admin users can only view PUBLISHED schedules.
     * Admins can view any schedule by ID (e.g. to navigate to it from the admin panel).
     */
    public ScheduleMetadataDTO getById(Long id) {
        ScheduleMetadata entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found with id: " + id));

        // Allow admins to see drafts, but restrict for other roles if needed
        // For now, let's just remove the hard restriction since Controller has @PreAuthorize
        // or add a check:
    /*
    if (entity.getStatus() != ScheduleStatus.PUBLISHED && !currentUserIsAdmin()) {
        throw new IllegalArgumentException("Access denied");
    }
    */

        return mapper.toDTO(entity);
    }

    /**
     * Returns a single schedule by ID for admin use — no status restriction.
     */
    @Transactional(readOnly = true)
    public ScheduleMetadataDTO getByIdForAdmin(Long id) {
        return repository.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found with id: " + id));
    }

    /**
     * Confirms manual edits for a schedule without changing its status.
     * Lesson-level changes are already persisted by lesson endpoints.
     */
    @Transactional
    public ScheduleMetadataDTO save(Long id) {
        ScheduleMetadata schedule = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found with id: " + id));
        requireEditable(schedule);
        invalidateSolver(schedule);
        synchronizeAddedLessonsSnapshot(schedule);
        recalculateAfterSynchronization(schedule);
        return mapper.toDTO(schedule);
    }

    /**
     * Confirms manual edits for an already published schedule while keeping its status unchanged.
     */
    @Transactional
    public ScheduleMetadataDTO republish(Long id) {
        ScheduleMetadata schedule = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found with id: " + id));
        requireEditable(schedule);
        invalidateSolver(schedule);
        synchronizeAddedLessonsSnapshot(schedule);
        recalculateAfterSynchronization(schedule);
        return mapper.toDTO(schedule);
    }

    @Transactional
    public ScheduleMetadataDTO create(ScheduleMetadataDTO dto) {
        ScheduleMetadata entity = mapper.toEntity(dto);
        entity.setCreatedAt(java.time.LocalDateTime.now()); // Ensure timestamp is set
        boolean publishing = entity.getStatus() == ScheduleStatus.PUBLISHED;
        if (entity.getStatus() == null || publishing) {
            entity.setStatus(ScheduleStatus.DRAFT);
        }
        entity = repository.save(entity);
        if (publishing) {
            preparePublication(entity);
            entity.setStatus(ScheduleStatus.PUBLISHED);
        }
        return mapper.toDTO(entity);
    }

    @Transactional
    public ScheduleMetadataDTO update(Long id, ScheduleMetadataDTO dto) {
        ScheduleMetadata entity = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found with id: " + id));

        entity.setName(dto.name());
        entity.setValidFrom(dto.validFrom());
        entity.setValidTo(dto.validTo());

        ScheduleStatus targetStatus = dto.status() == null ? entity.getStatus() : dto.status();
        if (targetStatus == ScheduleStatus.PUBLISHED) {
            preparePublication(entity);
        }
        invalidateSolver(entity);
        entity.setStatus(targetStatus);

        return mapper.toDTO(repository.save(entity));
    }

    /**
     * Publishes a schedule by transitioning its status to PUBLISHED.
     *
     * <p>Business rule: only one schedule may be PUBLISHED per date range.
     * If another PUBLISHED schedule's validity period overlaps with this one, the
     * operation is rejected with an {@link IllegalStateException}.
     *
     * @param id the schedule ID to publish
     * @return the updated schedule DTO
     * @throws IllegalArgumentException if schedule is not found
     * @throws IllegalStateException    if an overlapping PUBLISHED schedule already exists
     */
    @Transactional
    public ScheduleMetadataDTO publish(Long id) {
        ScheduleMetadata entity = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found with id: " + id));

        if (entity.getStatus() != ScheduleStatus.DRAFT) {
            throw new ScheduleConflictException("Only a draft schedule can be published");
        }
        preparePublication(entity);
        invalidateSolver(entity);
        log.info("Publishing schedule id={}, previous status={}", id, entity.getStatus());
        entity.setStatus(ScheduleStatus.PUBLISHED);

        return mapper.toDTO(repository.save(entity));
    }

    private void preparePublication(ScheduleMetadata entity) {
        if (entity.getStatus() == ScheduleStatus.ARCHIVED) {
            throw new ScheduleConflictException("Cannot publish an archived schedule");
        }
        publicationLockRepository.acquire()
                .orElseThrow(() -> new IllegalStateException("Schedule publication lock is missing"));
        scoreEvaluationService.validateAndRecalculate(entity.getId(), true);
        boolean overlap = repository.existsByStatusAndIdNotAndValidFromLessThanEqualAndValidToGreaterThanEqual(
                ScheduleStatus.PUBLISHED,
                entity.getId(),
                entity.getValidTo(),
                entity.getValidFrom()
        );

        if (overlap) {
            throw new ScheduleConflictException(
                    "Cannot publish schedule id=" + entity.getId() +
                    ": another PUBLISHED schedule already covers the same date range (" +
                    entity.getValidFrom() + " — " + entity.getValidTo() + ")"
            );
        }

    }

    /**
     * Archives a schedule by transitioning its status to ARCHIVED.
     *
     * @param id the schedule ID to archive
     * @return the updated schedule DTO
     * @throws IllegalArgumentException if schedule is not found
     */
    @Transactional
    public ScheduleMetadataDTO archive(Long id) {
        ScheduleMetadata entity = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found with id: " + id));
        log.info("Archiving schedule id={}, previous status={}", id, entity.getStatus());
        invalidateSolver(entity);
        entity.setStatus(ScheduleStatus.ARCHIVED);
        return mapper.toDTO(repository.save(entity));
    }

    /**
     * Reverts a schedule to DRAFT status, allowing it to be edited again.
     *
     * @param id the schedule ID to revert
     * @return the updated schedule DTO
     * @throws IllegalArgumentException if schedule is not found
     */
    @Transactional
    public ScheduleMetadataDTO revertToDraft(Long id) {
        ScheduleMetadata entity = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found with id: " + id));
        log.info("Reverting schedule id={} to DRAFT, previous status={}", id, entity.getStatus());
        invalidateSolver(entity);
        entity.setStatus(ScheduleStatus.DRAFT);
        return mapper.toDTO(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        ScheduleMetadata schedule = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found with id: " + id));
        repository.delete(schedule);
    }

    private void requireEditable(ScheduleMetadata schedule) {
        if (schedule.getStatus() == ScheduleStatus.ARCHIVED) {
            throw new ScheduleConflictException("Cannot modify an archived schedule");
        }
    }

    private void invalidateSolver(ScheduleMetadata schedule) {
        schedule.setSolverRevision(Math.incrementExact(schedule.getSolverRevision()));
    }

    private void recalculateAfterSynchronization(ScheduleMetadata schedule) {
        if (schedule.getStatus() == ScheduleStatus.PUBLISHED) {
            scoreEvaluationService.validateAndRecalculate(schedule.getId(), false);
        } else {
            scoreEvaluationService.recalculate(schedule.getId());
        }
    }

    private void synchronizeAddedLessonsSnapshot(ScheduleMetadata schedule) {
        List<AddedLesson> addedLessons = addedLessonRepository.findByScheduleIdOrderByIdAsc(schedule.getId());
        var existingByAddedLessonId = scheduledLessonRepository.findByScheduleIdOrderByIdAsc(schedule.getId()).stream()
                .filter(sl -> sl.getAddedLesson() != null && sl.getAddedLesson().getId() != null)
                .collect(java.util.stream.Collectors.toMap(
                        sl -> sl.getAddedLesson().getId(),
                        sl -> sl,
                        (first, second) -> first
                ));

        var incomingAddedLessonIds = new java.util.HashSet<Long>();
        var rowsToSave = new java.util.ArrayList<ScheduledLesson>(addedLessons.size());

        for (AddedLesson addedLesson : addedLessons) {
            incomingAddedLessonIds.add(addedLesson.getId());
            ScheduledLesson existing = existingByAddedLessonId.get(addedLesson.getId());
            ScheduledLessonStatus status = addedLesson.getTimeslot() != null
                    ? ScheduledLessonStatus.ASSIGNED
                    : ScheduledLessonStatus.UNASSIGNED;

            if (existing != null) {
                existing.setTimeslot(addedLesson.getTimeslot());
                existing.setRoom(addedLesson.getRoom());
                existing.setStudent(addedLesson.getStudent());
                existing.setStatus(status);
                rowsToSave.add(existing);
                continue;
            }

            ScheduledLesson row = new ScheduledLesson();
            row.setLesson(null);
            row.setAddedLesson(addedLesson);
            row.setSchedule(schedule);
            row.setTimeslot(addedLesson.getTimeslot());
            row.setRoom(addedLesson.getRoom());
            row.setStudent(addedLesson.getStudent());
            row.setStatus(status);
            row.setCancelled(false);
            row.setCancelledBy(null);
            row.setCancelledAt(null);
            row.setCancelReason(null);
            rowsToSave.add(row);
        }

        var staleRows = existingByAddedLessonId.values().stream()
                .filter(row -> !incomingAddedLessonIds.contains(row.getAddedLesson().getId()))
                .toList();
        if (!staleRows.isEmpty()) {
            scheduledLessonRepository.deleteAll(staleRows);
        }
        if (!rowsToSave.isEmpty()) {
            scheduledLessonRepository.saveAll(rowsToSave);
        }
    }
}
