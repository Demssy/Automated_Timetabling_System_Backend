package com.timetable.backend.solver;

import ai.timefold.solver.core.api.score.ScoreExplanation;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import com.timetable.backend.domain.dto.SolverSettingsValues;
import com.timetable.backend.domain.model.*;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class WeightCalibrationTest {

    @Test
    void verifySoftConstraintSharesAndPresetSensitivity() {
        SolverConfig baseConfig = new SolverConfig()
                .withEnvironmentMode(EnvironmentMode.REPRODUCIBLE)
                .withRandomSeed(42L)
                .withSolutionClass(DanceSchedule.class)
                .withEntityClasses(Lesson.class)
                .withConstraintProviderClass(DanceScheduleConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig().withSpentLimit(Duration.ofSeconds(5)));

        SolverRuntimeFactory factory = new SolverRuntimeFactory(baseConfig);

        // 1. Legacy weights (from screenshot) vs 2. Calibrated Balanced vs 3. Teacher Comfort vs 4. Prime-Time Focus
        Map<String, SolverSettingsValues> presets = new LinkedHashMap<>();
        presets.put("1_Legacy_Default", new SolverSettingsValues(
                5, 0, 6, 15, 1, 1, 1, 1, 1, 100, 10, 50, 30, 1, 1));
        presets.put("2_Calibrated_Balanced", new SolverSettingsValues(
                5, 0, 6, 15, 1, 1, 1, 1, 1, 60, 15, 35, 25, 8, 3));
        presets.put("3_Teacher_Comfort", new SolverSettingsValues(
                5, 0, 6, 15, 1, 1, 1, 1, 1, 60, 5, 30, 25, 25, 10));
        presets.put("4_PrimeTime_Revenue", new SolverSettingsValues(
                5, 0, 6, 15, 1, 1, 1, 1, 1, 60, 35, 35, 20, 3, 2));

        for (var entry : presets.entrySet()) {
            String presetName = entry.getKey();
            SolverSettingsValues settings = entry.getValue();

            DanceSchedule problem = buildCalibrationScenario();
            DanceSchedule solved = factory.create(settings).buildSolver().solve(problem);
            SolutionManager<DanceSchedule, HardSoftScore> manager = factory.solutionManager(settings);
            ScoreExplanation<DanceSchedule, HardSoftScore> explanation = manager.explain(solved);

            long totalAbsSoft = explanation.getConstraintMatchTotalMap().values().stream()
                    .mapToLong(t -> Math.abs(t.getScore().softScore()))
                    .sum();

            long primeLessons = solved.getLessonList().stream()
                    .filter(l -> l.getTimeslot() != null
                            && !l.getTimeslot().getStartTime().isBefore(LocalTime.of(16, 0))
                            && l.getTimeslot().getStartTime().isBefore(LocalTime.of(21, 0)))
                    .count();

            long totalGapMinutes = computeTotalTeacherGapMinutes(solved);

            System.out.printf(Locale.ROOT,
                    "%n=== Preset: %s | Score: %s | Prime lessons: %d | Total teacher gap: %d min ===%n",
                    presetName, explanation.getScore(), primeLessons, totalGapMinutes);

            explanation.getConstraintMatchTotalMap().values().stream()
                    .filter(t -> t.getScore().softScore() != 0)
                    .sorted((a, b) -> Integer.compare(
                    Math.abs(b.getScore().softScore()),
                    Math.abs(a.getScore().softScore())))
                    .forEach(t -> {
                        int s = t.getScore().softScore();
                        double share = totalAbsSoft == 0 ? 0.0 : 100.0 * Math.abs(s) / totalAbsSoft;
                        System.out.printf(Locale.ROOT, "  %-48s : %+6d soft (%5.1f%% of soft magnitude, %d matches)%n",
                                t.getConstraintRef().constraintName(), s, share, t.getConstraintMatchCount());
                    });

            assertThat(explanation.getScore().hardScore()).isZero();
        }
    }

    private static long computeTotalTeacherGapMinutes(DanceSchedule schedule) {
        long gaps = 0;
        for (Teacher teacher : schedule.getTeacherList()) {
            for (DayOfWeek day : DayOfWeek.values()) {
                List<Lesson> dayLessons = schedule.getLessonList().stream()
                        .filter(l -> l.getTimeslot() != null
                                && l.getTeacher().equals(teacher)
                                && l.getTimeslot().getDayOfWeek() == day)
                        .sorted(Comparator.comparing(l -> l.getTimeslot().getStartTime()))
                        .toList();
                for (int i = 1; i < dayLessons.size(); i++) {
                    long gap = Duration.between(
                            dayLessons.get(i - 1).getTimeslot().getEndTime(),
                            dayLessons.get(i).getTimeslot().getStartTime()).toMinutes();
                    if (gap > 15) {
                        gaps += gap;
                    }
                }
            }
        }
        return gaps;
    }

    private static DanceSchedule buildCalibrationScenario() {
        List<Timeslot> slots = new ArrayList<>();
        long slotId = 1L;
        for (DayOfWeek day : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY)) {
            for (int hour : List.of(12, 13, 14, 15, 16, 17, 18, 19)) {
                slots.add(new Timeslot(slotId++, day, LocalTime.of(hour, 0), LocalTime.of(hour + 1, 0)));
            }
        }

        List<Teacher> teachers = new ArrayList<>();
        List<Student> students = new ArrayList<>();
        List<WeeklyAvailability> avail = new ArrayList<>();
        long waId = 1L;

        for (int t = 0; t < 4; t++) {
            AbstractUser u = new AbstractUser();
            u.setId((long) t + 1);
            u.setFullName("Teacher " + (t + 1));
            Teacher teacher = new Teacher();
            teacher.setId(u.getId());
            teacher.setUser(u);
            teacher.setDesiredLessonsPerWeek(6);
            teacher.setPrivateStudents(new LinkedHashSet<>());
            teachers.add(teacher);

            for (DayOfWeek d : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY)) {
                avail.add(new WeeklyAvailability(waId++, u, d, LocalTime.of(12, 0), LocalTime.of(20, 0), AvailabilityRole.TEACHER));
            }
        }

        for (int s = 0; s < 12; s++) {
            Student st = new Student();
            st.setId(100L + s);
            st.setFullName("Student " + (s + 1));
            st.setDesiredLessonsPerWeek(2);
            students.add(st);

            teachers.get(s % 4).getPrivateStudents().add(st);
            teachers.get((s + 1) % 4).getPrivateStudents().add(st);

            for (DayOfWeek d : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY)) {
                avail.add(new WeeklyAvailability(waId++, st, d, LocalTime.of(12, 0), LocalTime.of(20, 0), AvailabilityRole.STUDENT));
            }
        }

        Room room = new Room(1L, "Main Hall", 4, true);
        List<Lesson> lessons = new ArrayList<>();
        long lessonId = 1L;

        // 4 pinned group lessons
        for (int g = 0; g < 4; g++) {
            DanceGroup group = new DanceGroup();
            group.setId((long) g + 1);
            group.setName("Group " + (g + 1));
            Lesson gl = new Lesson(teachers.get(g), group, 60, false, true);
            gl.setId(lessonId++);
            gl.setPinned(true);
            gl.setRoom(room);
            gl.setTimeslot(slots.get(g * 8)); // 12:00 on Mon, Tue, Wed, Thu
            lessons.add(gl);
        }

        // 20 unpinned private templates
        for (int p = 0; p < 20; p++) {
            Lesson pl = new Lesson(teachers.get(p % 4), (Student) null, 60, true);
            pl.setId(lessonId++);
            pl.setRoom(room);
            lessons.add(pl);
        }

        return new DanceSchedule(1L, slots, teachers, List.of(), avail,
                List.of(LocalDate.of(2026, 11, 2)), students, lessons);
    }
}