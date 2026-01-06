# Prompt: Refactor Services & Repositories (Performance & Clean Arch)

**Role:** You are a Senior Java Architect specializing in Spring Boot 3 and JPA Performance.

**Context:**
I am conducting a code review and found three major issues that need immediate fixing:
1.  **N+1 Query Problem:** `LessonRepository` and `TeacherRepository` trigger too many SQL queries when fetching lists, which kills performance during the Solver initialization.
2.  **Architecture Violation:** Services (`AuthService`, `TeacherService`, `SolverService`) are concrete classes. They must be behind interfaces to satisfy dependency inversion principles.
3.  **Legacy Syntax:** The code uses verbose variable declarations instead of Java 21 `var` and pattern matching.

**Files to Modify:**
1.  `src/main/java/com/timetable/backend/domain/repository/LessonRepository.java`
2.  `src/main/java/com/timetable/backend/domain/repository/TeacherRepository.java`
3.  `src/main/java/com/timetable/backend/service/AuthService.java`
4.  `src/main/java/com/timetable/backend/service/TeacherService.java`
5.  `src/main/java/com/timetable/backend/service/SolverService.java`

**Action Plan & Requirements:**

### 1. Fix N+1 in Repositories (Performance)
* **Modify `LessonRepository.java`:**
    * Override `findAll()` and add `@EntityGraph(attributePaths = {"teacher", "danceGroup", "timeslot", "room"})`.
    * This ensures all related entities are fetched in a single query.
* **Modify `TeacherRepository.java`:**
    * Override `findAll()` and add `@EntityGraph(attributePaths = {"danceStyles"})`.

### 2. Extract Service Interfaces (Clean Architecture)
* For each service, extract an interface (e.g., `AuthUseCase`, `TeacherUseCase`, `SolverUseCase`) containing the public business methods.
* The implementation classes should implement these interfaces.
* *Note:* You can keep the implementation in the same package for now, or move interfaces to `service.api` if you prefer, but ensure the code compiles.

### 3. Refactor Implementation Logic (Java 21 & Clean Code)
* **General:** Replace explicit types with `var` inside methods to reduce visual noise.
* **`AuthService.java`:**
    * Use `var` for `studentRole`, `student`, `userDetails`.
    * In `authenticate()`, use `instanceof` pattern matching for `UserDetails`.
* **`SolverService.java`:**
    * Simplify `loadProblem(Long scheduleId)`. Since we fixed N+1 in the repository, we **NO LONGER** need complex `@Transactional` splitting or `loadProblemInternal` methods.
    * The method can simply call the repositories (which now fetch eagerly via EntityGraph) and map the result.
    * Ensure `solve()` uses the repository results efficiently.

**Output:**
Please generate the full corrected code for the 5 files mentioned above.