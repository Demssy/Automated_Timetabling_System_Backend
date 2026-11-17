# Automated Timetabling System — Backend (`SE-60`)

A constraint-based weekly class scheduling backend for a single-hall dance studio, built with **Java 21**, **Spring Boot 3.5.8**, **Timefold Solver 1.6.0**, and **MySQL 8.4.7**[cite: 11, 12].

---

## 1. Overview & Architecture

The backend is implemented as a **modular monolith** running inside a single JVM process, combining REST API management, stateless JWT authentication, transactional schedule lifecycle controls, and an embedded heuristic constraint solver[cite: 11, 12]:

* **Dual-Mode Single-Hall Modeling:** A group class claims **100%** of the hall exclusively, whereas up to **four** private lessons may share the hall concurrently (**25%** capacity each)[cite: 11]. Collision detection evaluates half-open time intervals (`startA < endB && startB < endA`)[cite: 11].
* **Dual-Role Support:** A single user account (`users`) can hold both `TEACHER` and `STUDENT` roles with role-scoped weekly availability and dated unavailability exceptions while preventing overlapping teaching and learning commitments[cite: 11].
* **Heuristic Optimization:** Uses Timefold Solver (`Construction Heuristic` + `Entity Tabu Search`) with lexicographic `HardSoftScore` evaluation, configurable weights/presets, pinned group anchors (`@PlanningPin`), and stale-revision guards (`solverRevision`)[cite: 11].
* **Concurrency & Publication Guards:** Uses optimistic locking (`@Version`), pessimistic schedule-row locking, and a singleton database lock (`SchedulePublicationLock`) to serialize concurrent schedule publications[cite: 11].

### Package Structure (`src/main/java/com/timetable/backend`)
* `config/` — Security filter chain (`SecurityConfig`), default solver weights (`SolverWeightsConfig`), and template initializers[cite: 12].
* `controller/` — REST endpoints for authentication, users, teachers, students, groups, availability, lessons, schedules, solver execution, and runtime solver settings[cite: 12].
* `domain/` — JPA entities, DTOs, MapStruct mappers, repositories, and domain exceptions[cite: 12].
* `security/` — Stateless JWT generation/validation (`JwtService`, `JwtAuthenticationFilter`) and Argon2 password hashing[cite: 11, 12].
* `service/` — Transactional business logic, schedule lifecycle management, cancellation audit, score evaluation, and solution persistence[cite: 12].
* `solver/` — Timefold `@PlanningSolution` (`DanceSchedule`), constraint streams (`DanceScheduleConstraintProvider`), and asynchronous runtime registry (`SolverRuntimeRegistry`)[cite: 11, 12].

---

## 2. System Requirements & Dependencies

* **Java Development Kit (JDK):** Java **21** (configured via `java.version=21` in `pom.xml`)[cite: 11]
* **Build Tool:** Apache Maven (included via Maven Wrapper `./mvnw` / `.\mvnw.cmd`)[cite: 11, 12]
* **Database:** **MySQL 8.4.7** (or **Docker** + **Docker Compose** to run the provided MySQL container)[cite: 11]

---

## 3. Installation, Database Setup & Running

### Step 1: Configuration & Environment Variables
Default settings in `src/main/resources/application.properties` support the following environment variables[cite: 12, 15]:

| Variable | Default Value | Description |
| :--- | :--- | :--- |
| `MYSQL_IP` | `localhost` | Hostname or IP address of the MySQL 8.4.7 instance[cite: 15] |
| `TIMETABLE_API_PORT` | `8080` | HTTP port exposed by the Spring Boot server[cite: 15] |
| `JWT_SECRET` | *(dev base64 default)* | Base64-encoded HS256 signing key (override in production)[cite: 15] |

*(Note: Before deploying outside a local development environment, replace the development database credentials in `application.properties` and `docker-compose.yml` with secure environment-injected values and set a strong `JWT_SECRET`.)*[cite: 11, 12, 15]

### Step 2: Start MySQL Database
Start the MySQL 8.4.7 container on port `3306` using Docker Compose[cite: 11, 12]:

    docker compose up -d

> **Schema Evolution & Seed Data:** On application startup, **Flyway** automatically applies ordered migrations `V1` through `V26` from `src/main/resources/db/migration/` (creating all tables, indexes, default roles, default dance styles, the singleton publication lock row, and default solver settings)[cite: 11, 12]. If a `dump.sql` seed file is placed in the backend root directory before first container startup, Docker mounts it into `/docker-entrypoint-initdb.d/dump.sql`[cite: 11, 12].

### Step 3: Build and Run the Backend
Using the included Maven Wrapper[cite: 11]:

* **Linux / macOS:**

      ./mvnw clean package -DskipTests
      ./mvnw spring-boot:run

* **Windows (PowerShell / CMD):**

      .\mvnw.cmd clean package -DskipTests
      .\mvnw.cmd spring-boot:run

Once started, the REST API is available at: **`http://localhost:8080`**[cite: 11, 15].

---

## 4. Demo Accounts & Core API Workflows

### Creating / Using Demo Accounts
* If initializing the database with the provided `dump.sql` seed script, sign in using the pre-configured demo credentials listed in the seed data header[cite: 11, 12].
* If starting from a clean Flyway-migrated database[cite: 11]:
  1. Register `STUDENT` or `TEACHER` accounts via `POST /api/auth/register` (public registration intentionally blocks self-registration with the `ADMIN` role)[cite: 11].
  2. To create an administrator account, assign the `ADMIN` role (`role_id` matching `ADMIN` in `V6__insert_default_roles.sql`) in the `users` / `user_roles` tables[cite: 11, 12].

### Core Operational Workflows (REST API)

1. **Authentication (`AuthController`)**[cite: 11, 12]
   * `POST /api/auth/register` — Register a `STUDENT` or `TEACHER` account (teachers require at least one dance style specialization)[cite: 11].
   * `POST /api/auth/login` — Authenticate with email and password; returns `UserResponse` and sets an HTTP-only `jwt` cookie (`1 hour` TTL; `Authorization: Bearer <token>` is also supported)[cite: 11, 15].

2. **Master Data & Availability Setup**
   * `GET /api/dance-styles`, `GET /api/dictionaries/*` — Retrieve dance styles, rooms, and discrete timeslots[cite: 12].
   * `GET / POST /api/availability/weekly?role=TEACHER|STUDENT` — Configure role-scoped recurring weekly availability intervals[cite: 11].
   * `GET / POST /api/unavailability` — Register dated one-time absences[cite: 11, 12].
   * `POST /api/lessons` — Create recurring group lessons (automatically pinned during solving) or private lesson templates[cite: 11].

3. **Asynchronous Timetable Optimization (`SolverController` & `SolverSettingsController`)**[cite: 11, 12]
   * `GET / PUT /api/v1/solver/settings` — View or customize solver time limit (`5–300s`, default `10s`), constraint weights, daily limits, and named presets[cite: 11, 15].
   * `POST /api/schedules` — Create a `DRAFT` schedule with a `[validFrom, validTo]` date window[cite: 11].
   * `POST /api/admin/solver/solve/{scheduleId}` — Launch an asynchronous Timefold optimization run (returns `202 Accepted`)[cite: 11].
   * `GET /api/admin/solver/status/{scheduleId}` — Poll solver status (`SOLVING_SCHEDULED`, `SOLVING_ACTIVE`, `NOT_SOLVING`)[cite: 11].
   * `POST /api/admin/solver/stop/{scheduleId}` — Request early termination while preserving the best solution found so far[cite: 11].

4. **Review, Manual Edits & Guarded Publication**
   * `GET /api/admin/solver/solution/{scheduleId}` — Retrieve materialized assignments (`ScheduledLessonDTO`) and `HardSoftScore`[cite: 11].
   * `GET /api/admin/solver/score-explanation/{scheduleId}` & `/unmet-students/{scheduleId}` — Inspect per-constraint score breakdown and unfulfilled student demand[cite: 11].
   * `PATCH /api/scheduled-lessons/{id}/cancel` & `/restore` — Cancel a specific lesson occurrence (recording actor, timestamp, and reason) or restore it as an admin[cite: 11].
   * `PATCH /api/schedules/{id}/publish` — Validate mandatory hard constraints, check for overlapping published schedules under `SchedulePublicationLock`, and transition status to `PUBLISHED`[cite: 11].

---

## 5. Automated Testing & Reproducing Experiments

The test suite uses JUnit 5, Spring Boot Test, Timefold Solver Test, and an in-memory H2 database configured in MySQL compatibility mode (`src/main/resources/application-test.properties`)[cite: 11, 12].

### Run All 227 Automated Tests

    ./mvnw test

*(On Windows: `.\mvnw.cmd test`)*[cite: 11]

Expected summary:

    Tests run: 227, Failures: 0, Errors: 0, Skipped: 0
    BUILD SUCCESS

### Reproduce Empirical Benchmarks & Weight Sensitivity Experiments
To reproduce the synthetic benchmark comparison (**Greedy First-Fit vs. Timefold Solver** across Scenarios `A–D` with `EnvironmentMode.REPRODUCIBLE` and `seed = 42`) and the 4-preset weight calibration experiment[cite: 11]:

    ./mvnw test -Dtest=SolverBenchmarkEvaluationTest,WeightCalibrationTest

### Generate JaCoCo Code Coverage Report

    ./mvnw test jacoco:report

The HTML coverage report is generated at `target/site/jacoco/index.html` (core solver package `com.timetable.backend.solver` achieves **94%** instruction coverage)[cite: 12].