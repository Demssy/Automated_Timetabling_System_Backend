package com.timetable.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.timetable.backend.domain.dto.SolveResponse;
import com.timetable.backend.domain.dto.SolverStatusResponse;
import com.timetable.backend.domain.model.*;
import com.timetable.backend.domain.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for Solver E2E workflow.
 * Tests the complete flow: solve → status → verify results.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@AutoConfigureMockMvc
class SolverControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private TimeslotRepository timeslotRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private DanceGroupRepository danceGroupRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private DanceStyleRepository danceStyleRepository;

    @Autowired
    private ResourceUnavailabilityRepository resourceUnavailabilityRepository;

    private static Long testScheduleId;

    @BeforeEach
    void setUp() {
        // Clean up previous test data
        lessonRepository.deleteAll();
        resourceUnavailabilityRepository.deleteAll();
        danceGroupRepository.deleteAll();
        teacherRepository.deleteAll();
        danceStyleRepository.deleteAll();
        timeslotRepository.deleteAll();
        roomRepository.deleteAll();

        // Create test data
        createTestData();
    }

    @Test
    @Order(1)
    @DisplayName("E2E: Complete solver workflow")
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    void testCompleteSolverWorkflow() throws Exception {
        testScheduleId = System.currentTimeMillis();

        // Step 1: Start solving
        MvcResult solveResult = mockMvc.perform(post("/api/solver/solve")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted())
                .andReturn();

        String solveJson = solveResult.getResponse().getContentAsString();
        SolveResponse solveResponse = objectMapper.readValue(solveJson, SolveResponse.class);

        assertThat(solveResponse).isNotNull();
        assertThat(solveResponse.scheduleId()).isNotNull();

        Long scheduleId = solveResponse.scheduleId();

        // Step 2: Check status (should be SOLVING_ACTIVE or SOLVING_SCHEDULED)
        MvcResult statusResult = mockMvc.perform(get("/api/solver/status/" + scheduleId))
                .andExpect(status().isOk())
                .andReturn();

        String statusJson = statusResult.getResponse().getContentAsString();
        SolverStatusResponse statusResponse = objectMapper.readValue(statusJson, SolverStatusResponse.class);
        assertThat(statusResponse).isNotNull();

        // Step 3: Wait for solver to complete and assign lessons
        // Poll database for up to 10 seconds
        long assignedCount = 0;
        for (int i = 0; i < 20; i++) {
            Thread.sleep(500);
            var lessons = lessonRepository.findAll();
            assignedCount = lessons.stream()
                .filter(lesson -> lesson.getTimeslot() != null && lesson.getRoom() != null)
                .count();

            if (assignedCount > 0) {
                break;
            }
        }

        // Step 4: Check final status
        mockMvc.perform(get("/api/solver/status/" + scheduleId))
                .andExpect(status().isOk());

        // Step 5: Verify assignments
        assertThat(assignedCount).isGreaterThan(0);
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/solver/solve - requires ADMIN role")
    void testSolveRequiresAdminRole() throws Exception {
        // Without authentication, should get 401 or 403
        mockMvc.perform(post("/api/solver/solve"))
                .andExpect(status().isForbidden()); // Spring Security defaults to 403 for missing auth in some configs, or 401.
                // Actually without user it might be 401, but let's check.
                // If @WithMockUser is missing, it is anonymous user.
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/solver/status/{id} - returns status")
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    void testGetSolverStatus() throws Exception {
        Long scheduleId = System.currentTimeMillis();

        MvcResult result = mockMvc.perform(get("/api/solver/status/" + scheduleId))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        SolverStatusResponse response = objectMapper.readValue(json, SolverStatusResponse.class);

        assertThat(response).isNotNull();
        assertThat(response.scheduleId()).isEqualTo(scheduleId);
    }

    @Test
    @Order(4)
    @DisplayName("POST /api/solver/terminate/{id} - terminates solver")
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    void testTerminateSolver() throws Exception {
        // Start solver first
        MvcResult solveResult = mockMvc.perform(post("/api/solver/solve"))
                .andExpect(status().isAccepted())
                .andReturn();

        String solveJson = solveResult.getResponse().getContentAsString();
        SolveResponse solveResponse = objectMapper.readValue(solveJson, SolveResponse.class);
        Long scheduleId = solveResponse.scheduleId();

        // Give it a moment to start
        Thread.sleep(1000);

        // Terminate
        mockMvc.perform(post("/api/solver/terminate/" + scheduleId))
                .andExpect(status().isOk()); // Or BAD_REQUEST if already finished
    }

    /**
     * Creates minimal test data for solver to work with.
     */
    private void createTestData() {
        // Create timeslots
        createTimeslot(DayOfWeek.MONDAY, "09:00", "10:00");
        createTimeslot(DayOfWeek.MONDAY, "10:00", "11:00");
        createTimeslot(DayOfWeek.MONDAY, "11:00", "12:00");
        createTimeslot(DayOfWeek.TUESDAY, "09:00", "10:00");
        createTimeslot(DayOfWeek.TUESDAY, "10:00", "11:00");

        // Create rooms
        createRoom("Studio A", 20, false);
        createRoom("Studio B", 15, true); // Dual-mode enabled

        // Create role and dance style
        Role teacherRole = roleRepository.findByName("TEACHER")
            .orElseGet(() -> {
                Role role = new Role();
                role.setName("TEACHER");
                return roleRepository.save(role);
            });

        DanceStyle salsa = new DanceStyle();
        salsa.setName("Salsa");
        salsa = danceStyleRepository.save(salsa);

        // Create teacher
        Teacher teacher = new Teacher();
        teacher.setEmail("teacher1@test.com");
        teacher.setPasswordHash("hashed_password");
        teacher.setFullName("Test Teacher");
        teacher.setRole(teacherRole);
        teacher.setMaxDailyHours(8);
        teacher.setColorCode("#FF5733");
        teacher = teacherRepository.save(teacher);

        // Create dance group
        DanceGroup group = new DanceGroup();
        group.setName("Beginners Salsa");
        group.setDanceStyle(salsa);
        group.setDanceLevel(DanceLevel.BEGINNER);
        group = danceGroupRepository.save(group);

        // Create lessons (unassigned - solver will assign them)
        for (int i = 0; i < 3; i++) {
            Lesson lesson = new Lesson();
            lesson.setTeacher(teacher);
            lesson.setDanceGroup(group);
            lesson.setDurationMinutes(60);
            lesson.setPrivate(false);
            lesson.setPinned(false);
            // timeslot and room are null - solver will assign
            lessonRepository.save(lesson);
        }
    }

    private void createTimeslot(DayOfWeek day, String start, String end) {
        Timeslot slot = new Timeslot();
        slot.setDayOfWeek(day);
        slot.setStartTime(LocalTime.parse(start));
        slot.setEndTime(LocalTime.parse(end));
        timeslotRepository.save(slot);
    }

    private void createRoom(String name, int capacity, boolean allowsParallel) {
        Room room = new Room();
        room.setName(name);
        room.setCapacity(capacity);
        room.setAllowsParallelPrivate(allowsParallel);
        roomRepository.save(room);
    }
}

