package com.timetable.backend.controller;

import com.timetable.backend.domain.dto.*;
import com.timetable.backend.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Central REST controller for all administrator operations.
 * Aggregates CRUD management for: Users, Teachers, Rooms, Timeslots, Lessons.
 * Also exposes Solver control endpoints.
 *
 * All endpoints require ADMIN role.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminController {

    private final UserService userService;
    private final TeacherService teacherService;
    private final RoomService roomService;
    private final DanceStyleService danceStyleService;
    private final TimeslotService timeslotService;
    private final DanceGroupService danceGroupService;
    private final LessonService lessonService;
    private final ScheduleMetadataService scheduleMetadataService;
    private final WeeklyAvailabilityService weeklyAvailabilityService;

    // =========================================================================
    // Schedule management  —  /api/admin/schedules
    // =========================================================================

    /**
     * Returns ALL schedules regardless of status (DRAFT, PUBLISHED, ARCHIVED).
     * This is the admin-panel view — only admins can see unpublished schedules.
     */
    @GetMapping("/schedules")
    public ResponseEntity<List<ScheduleMetadataDTO>> getAllSchedules() {
        return ResponseEntity.ok(scheduleMetadataService.getAllForAdmin());
    }

    /**
     * Returns a single schedule by ID with no status restriction.
     * Used in the admin panel to navigate to any schedule (including drafts).
     */
    @GetMapping("/schedules/{id}")
    public ResponseEntity<ScheduleMetadataDTO> getScheduleById(@PathVariable Long id) {
        return ResponseEntity.ok(scheduleMetadataService.getByIdForAdmin(id));
    }

    // =========================================================================
    // User management  —  /api/admin/users
    // =========================================================================

    /** Get all registered users. */
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * Search users by partial email for autocomplete input.
     * Example: GET /api/admin/users/search?email=john&limit=10
     */
    @GetMapping("/users/search")
    public ResponseEntity<List<UserResponse>> searchUsersByEmail(
            @RequestParam String email,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(userService.searchUsersByEmail(email, limit));
    }

    /** Get user by ID. */
    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    /** Update user details. */
    @PutMapping("/users/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @RequestBody @Valid UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    /**
     * Admin-only: bulk replace a user's weekly schedule + one-time exceptions.
     * Mirrors the self-service endpoint PUT /api/user/me/availability but for any user.
     *
     * @param id      target user ID
     * @param request contains weeklyAvailabilities and oneTimeUnavailabilities lists
     * @return updated UserResponse reflecting the new availability state
     */
    @PutMapping("/users/{id}/availability")
    public ResponseEntity<UserResponse> updateUserAvailability(
            @PathVariable Long id,
            @RequestBody @Valid UpdateAvailabilityRequest request) {
        return ResponseEntity.ok(userService.updateUserAvailability(id, request));
    }

    /** Delete a user. */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // Teacher management  —  /api/admin/teachers
    // =========================================================================

    /** Get all teachers. */
    @GetMapping("/teachers")
    public ResponseEntity<List<TeacherResponse>> getAllTeachers() {
        return ResponseEntity.ok(teacherService.getAllTeachers());
    }

    /** Get teacher by ID. */
    @GetMapping("/teachers/{id}")
    public ResponseEntity<TeacherResponse> getTeacherById(@PathVariable Long id) {
        return ResponseEntity.ok(teacherService.getTeacherById(id));
    }

    /**
     * Create a new teacher account.
     *
     * @param request email, password, fullName, maxDailyHours, colorCode, qualifiedStyleIds
     * @return 201 Created with teacher details
     */
    @PostMapping("/teachers")
    public ResponseEntity<TeacherResponse> createTeacher(@RequestBody @Valid CreateTeacherRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(teacherService.createTeacher(request));
    }

    /** Update teacher profile (fullName, maxDailyHours, colorCode, qualifiedStyleIds). */
    @PutMapping("/teachers/{id}")
    public ResponseEntity<TeacherResponse> updateTeacher(
            @PathVariable Long id,
            @RequestBody @Valid UpdateTeacherRequest request) {
        return ResponseEntity.ok(teacherService.updateTeacher(id, request));
    }

    /** Delete a teacher. */
    @DeleteMapping("/teachers/{id}")
    public ResponseEntity<Void> deleteTeacher(@PathVariable Long id) {
        teacherService.deleteTeacher(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // Room management  —  /api/admin/rooms
    // =========================================================================

    /** Get all rooms. */
    @GetMapping("/rooms")
    public ResponseEntity<List<RoomDTO>> getAllRooms() {
        return ResponseEntity.ok(roomService.getAllRooms());
    }

    /** Get room by ID. */
    @GetMapping("/rooms/{id}")
    public ResponseEntity<RoomDTO> getRoomById(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.getRoomById(id));
    }

    /**
     * Create a new room.
     *
     * @param dto name, capacity, allowsParallelPrivate
     * @return 201 Created with room details
     */
    @PostMapping("/rooms")
    public ResponseEntity<RoomDTO> createRoom(@RequestBody @Valid RoomDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roomService.createRoom(dto));
    }

    /** Update room details. */
    @PutMapping("/rooms/{id}")
    public ResponseEntity<RoomDTO> updateRoom(
            @PathVariable Long id,
            @RequestBody @Valid RoomDTO dto) {
        return ResponseEntity.ok(roomService.updateRoom(id, dto));
    }

    /** Delete a room. */
    @DeleteMapping("/rooms/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // DanceStyle management  —  /api/admin/styles
    // =========================================================================

    /** Get all dance styles. */
    @GetMapping("/styles")
    public ResponseEntity<List<DanceStyleDTO>> getAllStyles() {
        return ResponseEntity.ok(danceStyleService.getAllStyles());
    }

    /** Get dance style by ID. */
    @GetMapping("/styles/{id}")
    public ResponseEntity<DanceStyleDTO> getStyleById(@PathVariable Long id) {
        return ResponseEntity.ok(danceStyleService.getStyleById(id));
    }

    /** Create a new dance style. */
    @PostMapping("/styles")
    public ResponseEntity<DanceStyleDTO> createStyle(@RequestBody @Valid DanceStyleDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(danceStyleService.createStyle(dto));
    }

    /** Update dance style details. */
    @PutMapping("/styles/{id}")
    public ResponseEntity<DanceStyleDTO> updateStyle(
            @PathVariable Long id,
            @RequestBody @Valid DanceStyleDTO dto) {
        return ResponseEntity.ok(danceStyleService.updateStyle(id, dto));
    }

    /** Delete a dance style. */
    @DeleteMapping("/styles/{id}")
    public ResponseEntity<Void> deleteStyle(@PathVariable Long id) {
        danceStyleService.deleteStyle(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // Timeslot management  —  /api/admin/timeslots
    // =========================================================================

    /** Get all timeslots. */
    @GetMapping("/timeslots")
    public ResponseEntity<List<TimeslotDTO>> getAllTimeslots() {
        return ResponseEntity.ok(timeslotService.getAllTimeslots());
    }

    /** Get timeslot by ID. */
    @GetMapping("/timeslots/{id}")
    public ResponseEntity<TimeslotDTO> getTimeslotById(@PathVariable Long id) {
        return ResponseEntity.ok(timeslotService.getTimeslotById(id));
    }

    /**
     * Create a new timeslot.
     * Combination of (dayOfWeek + startTime + endTime) must be unique.
     *
     * @param dto dayOfWeek, startTime, endTime
     * @return 201 Created with timeslot details
     */
    @PostMapping("/timeslots")
    public ResponseEntity<TimeslotDTO> createTimeslot(@RequestBody @Valid TimeslotDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(timeslotService.createTimeslot(dto));
    }

    /** Update timeslot. */
    @PutMapping("/timeslots/{id}")
    public ResponseEntity<TimeslotDTO> updateTimeslot(
            @PathVariable Long id,
            @RequestBody @Valid TimeslotDTO dto) {
        return ResponseEntity.ok(timeslotService.updateTimeslot(id, dto));
    }

    /** Delete a timeslot. */
    @DeleteMapping("/timeslots/{id}")
    public ResponseEntity<Void> deleteTimeslot(@PathVariable Long id) {
        timeslotService.deleteTimeslot(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // DanceGroup management  —  /api/admin/groups
    // =========================================================================

    /** Get all dance groups. */
    @GetMapping("/groups")
    public ResponseEntity<List<DanceGroupDTO>> getAllGroups() {
        return ResponseEntity.ok(danceGroupService.getAllGroups());
    }

    /** Get dance group by ID. */
    @GetMapping("/groups/{id}")
    public ResponseEntity<DanceGroupDTO> getGroupById(@PathVariable Long id) {
        return ResponseEntity.ok(danceGroupService.getGroupById(id));
    }

    /** Create a new dance group. */
    @PostMapping("/groups")
    public ResponseEntity<DanceGroupDTO> createGroup(@RequestBody @Valid DanceGroupDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(danceGroupService.createGroup(dto));
    }

    /** Update dance group details. */
    @PutMapping("/groups/{id}")
    public ResponseEntity<DanceGroupDTO> updateGroup(
            @PathVariable Long id,
            @RequestBody @Valid DanceGroupDTO dto) {
        return ResponseEntity.ok(danceGroupService.updateGroup(id, dto));
    }

    /** Delete a dance group. */
    @DeleteMapping("/groups/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id) {
        danceGroupService.deleteGroup(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // Lesson management  —  /api/admin/lessons
    // =========================================================================

    /** Get all lessons (planning entities). */
    @GetMapping("/lessons")
    public ResponseEntity<List<ScheduledLessonDTO>> getAllLessons() {
        return ResponseEntity.ok(lessonService.getAllLessons());
    }

    /** Get lesson by ID. */
    @GetMapping("/lessons/{id}")
    public ResponseEntity<ScheduledLessonDTO> getLessonById(@PathVariable Long id) {
        return ResponseEntity.ok(lessonService.getLessonById(id));
    }

    /**
     * Create a new lesson (planning entity for the solver).
     * Leave timeslotId and roomId as null — the Solver will assign them.
     *
     * @param request teacherId, danceGroupId, durationMinutes, isPrivate, isPinned, timeslotId?, roomId?
     * @return 201 Created with lesson details
     */
    @PostMapping("/lessons")
    public ResponseEntity<ScheduledLessonDTO> createLesson(@RequestBody @Valid CreateLessonRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(lessonService.createLesson(request));
    }

    /**
     * Update lesson data — also used for manual drag-and-drop reschedule.
     *
     * <p>Uses {@link UpdateLessonRequest} instead of {@link CreateLessonRequest}
     * because {@code timeslotId} and {@code roomId} must be explicitly nullable
     * without triggering validation errors.
     *
     * <p>Returns 409 Conflict (via {@link GlobalExceptionHandler})
     * when the lesson is currently pinned and the request attempts to change
     * the timeslot or room.
     */
    @PutMapping("/lessons/{id}")
    public ResponseEntity<ScheduledLessonDTO> updateLesson(
            @PathVariable Long id,
            @RequestBody @Valid UpdateLessonRequest request) {
        return ResponseEntity.ok(lessonService.updateLessonManually(id, request));
    }

    /** Delete a lesson. */
    @DeleteMapping("/lessons/{id}")
    public ResponseEntity<Void> deleteLesson(@PathVariable Long id) {
        lessonService.deleteLesson(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // Weekly Availability management  —  /api/admin/availability
    // =========================================================================

    /**
     * Returns all users (TEACHER + STUDENT) with their full weekly availability.
     * Used to populate the admin availability dashboard table.
     * Delegates to {@link UserService#getAllUsers()} which already embeds weeklyAvailabilities.
     */
    @GetMapping("/availability/users")
    public ResponseEntity<List<UserResponse>> getAllUsersWithAvailability() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * Returns the weekly availability slots for a single user.
     *
     * @param userId target user ID
     * @return list of weekly availability slots
     */
    @GetMapping("/availability/users/{userId}")
    public ResponseEntity<List<WeeklyAvailabilityDTO>> getUserAvailability(@PathVariable Long userId) {
        return ResponseEntity.ok(weeklyAvailabilityService.getByUserId(userId));
    }

    /**
     * Creates a new weekly availability slot for the specified user.
     *
     * @param userId  target user ID
     * @param request dayOfWeek, startTime, endTime
     * @return 201 Created with the saved slot
     */
    @PostMapping("/availability/users/{userId}/weekly")
    public ResponseEntity<WeeklyAvailabilityDTO> createAvailabilitySlot(
            @PathVariable Long userId,
            @RequestBody @Valid WeeklyAvailabilityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(weeklyAvailabilityService.createSlot(userId, request));
    }

    /**
     * Updates an existing weekly availability slot.
     *
     * @param slotId  ID of the slot to update
     * @param request new dayOfWeek, startTime, endTime values
     * @return updated slot DTO
     */
    @PutMapping("/availability/weekly/{slotId}")
    public ResponseEntity<WeeklyAvailabilityDTO> updateAvailabilitySlot(
            @PathVariable Long slotId,
            @RequestBody @Valid WeeklyAvailabilityRequest request) {
        return ResponseEntity.ok(weeklyAvailabilityService.updateSlot(slotId, request));
    }

    /**
     * Deletes a weekly availability slot by ID.
     *
     * @param slotId ID of the slot to remove
     * @return 204 No Content
     */
    @DeleteMapping("/availability/weekly/{slotId}")
    public ResponseEntity<Void> deleteAvailabilitySlot(@PathVariable Long slotId) {
        weeklyAvailabilityService.deleteSlot(slotId);
        return ResponseEntity.noContent().build();
    }
}
