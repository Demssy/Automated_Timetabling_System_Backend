package com.timetable.backend.controller;

import com.timetable.backend.domain.dto.DanceGroupDetailsDTO;
import com.timetable.backend.domain.dto.GroupStudentDTO;
import com.timetable.backend.service.DanceGroupService;
import com.timetable.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for the public-facing dance groups catalogue.
 *
 * <p>Base path: {@code /api/groups}</p>
 *
 * <ul>
 *   <li>{@code GET    /api/groups}              — all groups with schedule details</li>
 *   <li>{@code GET    /api/groups/my}            — "My Groups" tab (role-aware)</li>
 *   <li>{@code POST   /api/groups/{id}/enroll}   — enroll the current student</li>
 *   <li>{@code DELETE /api/groups/{id}/enroll}   — unenroll the current student</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@Slf4j
public class DanceGroupController {

    private final DanceGroupService danceGroupService;
    private final UserService userService;

    /**
     * Returns all dance groups with their weekly schedule.
     */
    @GetMapping
    public ResponseEntity<List<DanceGroupDetailsDTO>> getAllGroups(Authentication authentication) {
        Long userId = resolveCurrentUserId(authentication);
        return ResponseEntity.ok(danceGroupService.getAllGroupsWithDetails(userId));
    }

    /**
     * Returns groups relevant to the current user (role-aware).
     */
    @GetMapping("/my")
    public ResponseEntity<List<DanceGroupDetailsDTO>> getMyGroups(Authentication authentication) {
        Long userId = resolveCurrentUserId(authentication);
        String role = extractPrimaryRole(authentication);
        return ResponseEntity.ok(danceGroupService.getMyGroups(userId, role));
    }

    /**
     * Returns all students enrolled in the given dance group.
     * Accessible by any authenticated user (student, teacher, admin).
     */
    @GetMapping("/{id}/students")
    public ResponseEntity<List<GroupStudentDTO>> getStudents(@PathVariable Long id) {
        return ResponseEntity.ok(danceGroupService.getStudentsByGroupId(id));
    }

    /**
     * Enrolls a student in a group.
     * If {@code studentId} query param is provided (admin use-case), enrolls that student.
     * Otherwise, enrolls the currently authenticated user.
     */
    @PostMapping("/{groupId}/enroll")
    public ResponseEntity<Void> enroll(
            @PathVariable Long groupId,
            @RequestParam(required = false) Long studentId,
            Authentication authentication) {

        Long targetId = studentId != null ? studentId : resolveCurrentUserId(authentication);
        danceGroupService.enrollStudent(groupId, targetId);
        log.info("Student {} enrolled in group {}", targetId, groupId);
        return ResponseEntity.ok().build();
    }

    /**
     * Removes a student from a group.
     */
    @DeleteMapping("/{groupId}/enroll")
    public ResponseEntity<Void> unenroll(
            @PathVariable Long groupId,
            @RequestParam(required = false) Long studentId,
            Authentication authentication) {

        Long targetId = studentId != null ? studentId : resolveCurrentUserId(authentication);
        danceGroupService.unenrollStudent(groupId, targetId);
        log.info("Student {} unenrolled from group {}", targetId, groupId);
        return ResponseEntity.ok().build();
    }

    // ──────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────

    /** JWT subject = email. We look up the user to obtain their numeric id. */
    private Long resolveCurrentUserId(Authentication authentication) {
        String email = authentication.getName();
        return userService.getCurrentUserInfo(email).id();
    }

    private String extractPrimaryRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .findFirst()
            .orElse("ROLE_STUDENT");
    }
}