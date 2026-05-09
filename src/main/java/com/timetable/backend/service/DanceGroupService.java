package com.timetable.backend.service;

import com.timetable.backend.domain.dto.DanceGroupDTO;
import com.timetable.backend.domain.dto.DanceGroupDetailsDTO;
import com.timetable.backend.domain.dto.GroupScheduleSlotDTO;
import com.timetable.backend.domain.mapper.DictionaryMapper;
import com.timetable.backend.domain.model.*;
import com.timetable.backend.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Service for managing DanceGroup entities and student enrollment.
 */
@Service
@RequiredArgsConstructor
public class DanceGroupService {

    private final DanceGroupRepository danceGroupRepository;
    private final DanceStyleRepository danceStyleRepository;
    private final DictionaryMapper dictionaryMapper;
    private final LessonRepository lessonRepository;
    private final StudentRepository studentRepository;

    // ──────────────────────────────────────────────────────────────────
    // Basic CRUD (used by Admin panel — unchanged)
    // ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<DanceGroupDTO> getAllGroups() {
        return danceGroupRepository.findAll().stream()
            .map(dictionaryMapper::toDanceGroupDTO)
            .toList();
    }

    @Transactional(readOnly = true)
    public DanceGroupDTO getGroupById(Long id) {
        DanceGroup group = danceGroupRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("DanceGroup not found with id: " + id));
        return dictionaryMapper.toDanceGroupDTO(group);
    }

    @Transactional
    public DanceGroupDTO createGroup(DanceGroupDTO dto) {
        DanceGroup group = dictionaryMapper.toDanceGroup(dto);
        group.setDanceStyle(resolveDanceStyle(dto.danceStyleId()));
        DanceGroup saved = danceGroupRepository.save(group);
        return dictionaryMapper.toDanceGroupDTO(saved);
    }

    @Transactional
    public DanceGroupDTO updateGroup(Long id, DanceGroupDTO dto) {
        DanceGroup group = danceGroupRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("DanceGroup not found with id: " + id));

        group.setName(dto.name());
        group.setDanceLevel(dto.danceLevel());
        group.setMinSize(dto.minSize());
        group.setTargetAgeRange(dto.targetAgeRange());

        if (dto.danceStyleId() != null) {
            Long currentStyleId = group.getDanceStyle() != null ? group.getDanceStyle().getId() : null;
            if (!dto.danceStyleId().equals(currentStyleId)) {
                group.setDanceStyle(resolveDanceStyle(dto.danceStyleId()));
            }
        }

        DanceGroup saved = danceGroupRepository.save(group);
        return dictionaryMapper.toDanceGroupDTO(saved);
    }

    @Transactional
    public void deleteGroup(Long id) {
        if (!danceGroupRepository.existsById(id)) {
            throw new IllegalArgumentException("DanceGroup not found with id: " + id);
        }
        danceGroupRepository.deleteById(id);
    }

    // ──────────────────────────────────────────────────────────────────
    // Public Groups page — all groups with schedule details
    // ──────────────────────────────────────────────────────────────────

    /**
     * Returns all dance groups with their weekly schedule and enrollment state.
     *
     * @param currentUserId the authenticated user's id (used to mark isEnrolledByCurrentUser)
     */
    @Transactional(readOnly = true)
    public List<DanceGroupDetailsDTO> getAllGroupsWithDetails(Long currentUserId) {
        return danceGroupRepository.findAll().stream()
            .map(group -> buildDetailsDTO(group, currentUserId))
            .sorted(Comparator.comparing(DanceGroupDetailsDTO::name))
            .toList();
    }

    /**
     * Returns groups for the "My Groups" tab, filtered by role:
     * - STUDENT → groups the student is enrolled in
     * - TEACHER → groups the teacher is assigned to (via lessons)
     * - ADMIN   → all groups (same as getAllGroupsWithDetails)
     */
    @Transactional(readOnly = true)
    public List<DanceGroupDetailsDTO> getMyGroups(Long userId, String roleName) {
        return switch (roleName.toUpperCase()) {
            case "ROLE_STUDENT" -> getGroupsForStudent(userId);
            case "ROLE_TEACHER" -> getGroupsForTeacher(userId);
            default             -> getAllGroupsWithDetails(userId);
        };
    }

    // ──────────────────────────────────────────────────────────────────
    // Enrollment
    // ──────────────────────────────────────────────────────────────────

    /**
     * Enrolls a student in a dance group.
     */
    @Transactional
    public void enrollStudent(Long groupId, Long studentId) {
        DanceGroup group = danceGroupRepository.findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("DanceGroup not found: " + groupId));
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        if (group.getEnrolledStudents().contains(student)) {
            throw new IllegalStateException("Student is already enrolled in this group");
        }

        group.getEnrolledStudents().add(student);
        danceGroupRepository.save(group);
    }

    /**
     * Removes a student from a dance group.
     */
    @Transactional
    public void unenrollStudent(Long groupId, Long studentId) {
        DanceGroup group = danceGroupRepository.findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("DanceGroup not found: " + groupId));
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        if (!group.getEnrolledStudents().remove(student)) {
            throw new IllegalStateException("Student is not enrolled in this group");
        }

        danceGroupRepository.save(group);
    }

    // ──────────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────────

    private List<DanceGroupDetailsDTO> getGroupsForStudent(Long studentId) {
        return danceGroupRepository.findByEnrolledStudentId(studentId).stream()
            .map(group -> buildDetailsDTO(group, studentId))
            .sorted(Comparator.comparing(DanceGroupDetailsDTO::name))
            .toList();
    }

    private List<DanceGroupDetailsDTO> getGroupsForTeacher(Long teacherId) {
        List<Long> groupIds = lessonRepository.findActiveGroupLessonsByTeacherId(teacherId)
            .stream()
            .filter(l -> l.getDanceGroup() != null)
            .map(l -> l.getDanceGroup().getId())
            .distinct()
            .toList();

        return groupIds.stream()
            .map(id -> danceGroupRepository.findById(id).orElseThrow())
            .map(group -> buildDetailsDTO(group, teacherId))
            .sorted(Comparator.comparing(DanceGroupDetailsDTO::name))
            .toList();
    }

    /**
     * Builds a {@link DanceGroupDetailsDTO} for a group, including its weekly timeslots
     * derived from active group lessons that already have a timeslot assigned (pinned lessons).
     */
    private DanceGroupDetailsDTO buildDetailsDTO(DanceGroup group, Long currentUserId) {
        List<GroupScheduleSlotDTO> schedule = lessonRepository
            .findByDanceGroupIdAndIsPrivateFalseAndIsActiveTrue(group.getId())
            .stream()
            .filter(lesson -> lesson.getTimeslot() != null)
            .map(lesson -> new GroupScheduleSlotDTO(
                lesson.getTimeslot().getDayOfWeek(),
                lesson.getTimeslot().getStartTime(),
                lesson.getTimeslot().getEndTime(),
                lesson.getTeacher().getUser().getFullName()
            ))
            .sorted(Comparator.comparing(GroupScheduleSlotDTO::dayOfWeek)
                .thenComparing(GroupScheduleSlotDTO::startTime))
            .toList();

        boolean isEnrolled = group.getEnrolledStudents().stream()
            .anyMatch(s -> s.getId().equals(currentUserId));

        return new DanceGroupDetailsDTO(
            group.getId(),
            group.getName(),
            group.getDanceStyle() != null ? group.getDanceStyle().getName() : null,
            group.getDanceLevel(),
            group.getTargetAgeRange(),
            group.getMinSize(),
            schedule,
            group.getEnrolledStudents().size(),
            isEnrolled
        );
    }

    private DanceStyle resolveDanceStyle(Long danceStyleId) {
        if (danceStyleId == null) {
            throw new IllegalArgumentException("Dance Style ID is required");
        }
        return danceStyleRepository.findById(danceStyleId)
            .orElseThrow(() -> new IllegalArgumentException("Dance Style not found with id: " + danceStyleId));
    }
}

