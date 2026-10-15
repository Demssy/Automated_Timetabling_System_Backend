# Automated Timetabling System for Dance School (Backend)

## 1. Project Overview

The **Automated Timetabling System for Dance School** backend provides a REST API for managing school resources and generating weekly lesson schedules. It uses constraint-based optimization to assign teachers, students, groups, rooms, private lesson templates, and timeslots while respecting recurring availability and one-time unavailability.

Core capabilities include role-based account management, administration of dance groups and dictionaries, weekly availability management, schedule generation, schedule publication, and cancellation or restoration of scheduled lessons. A teacher may also be registered as a student, and the scheduling model accounts for this dual role.

## 2. Technology Stack and Prerequisites

| Concern | Technology |
|---|---|
| Language and runtime | Java 21 (OpenJDK / Eclipse Temurin) |
| Framework | Spring Boot 3.x (project uses 3.5.8) |
| Optimization engine | Timefold Solver, Constraint Streams, `HardSoftScore` |
| Security | Spring Security 6, stateless JWT authentication (JJWT) |
| Persistence | Spring Data JPA, Hibernate, MySQL 8.x |
| Database migrations | Flyway, migrations `V1` through `V23` |
| Build tool | Maven Wrapper (`mvnw` / `mvnw.cmd`) |
| Tests | JUnit 5, Spring Boot Test, Timefold constraint testing, H2 |

Install **JDK 21** and **Docker**. Ensure ports **3306** (MySQL) and **8080** (HTTP API) are available. The included database container configuration supports MySQL 8.0; the repository's `Dockerfile` uses MySQL 8.4.

## 3. Architecture and Project Layout

```text
src/main/java/com/timetable/backend
├── BackendApplication.java
├── config        Security, solver weights, private lesson template initialization
├── controller    REST endpoints grouped by domain
├── domain
│   ├── model       JPA entities
│   ├── repository  Spring Data repositories
│   ├── dto         API request and response types
│   └── mapper      Entity and DTO mapping
├── security      JWT filter, token service, user details service
├── service       Business logic, solver orchestration and persistence
└── solver        Timefold planning solution and constraint provider
```

- **`com.timetable.backend.solver`** contains `DanceSchedule`, the Timefold planning solution, and `DanceScheduleConstraintProvider`. The planning entity is `Lesson` in `com.timetable.backend.domain.model` (it is also a JPA entity). `timeslot` and `student` are planning variables; a private lesson's student may be left unassigned when no valid match is available.
- **`com.timetable.backend.domain`** contains entities such as `AbstractUser`, `Teacher`, `Student`, `DanceGroup`, `Room`, `Timeslot`, `Lesson`, `WeeklyAvailability`, and `ResourceUnavailability`, as well as repositories, DTOs, and mappers.
- **`com.timetable.backend.service`** contains application logic. `SolverProblemLoaderService` assembles the planning problem from persisted data; `SolverService` manages solving; `SolutionPersistenceService` persists the result. `PrivateLessonTemplateInitializer` seeds private lesson templates. Other services handle users, availability, schedules, lessons, and related domain operations. Solver score explanations and unmet-student reports are available through the solver API.
- **`com.timetable.backend.controller`** exposes the REST API, grouped by authentication, schedules, lessons, availability, groups, dictionaries, and solver administration.
- **`com.timetable.backend.security`** implements JWT authentication. The filter reads bearer tokens, and the user details service loads the authenticated user. Sessions are stateless; authentication routes and selected public dance-style reads are open, while the remaining API requires authentication. Admin operations are role-protected.

### Planning model scope

Although rooms are managed as domain entities, **room is not a planning variable** in the current solver model. `Lesson.room` is pre-assigned, and the constraint provider documents a single-room scheduling assumption. Room occupancy is represented by the hard constraints limiting concurrent private lessons and prohibiting private lessons during a group lesson. The current solver does not implement a general room-assignment, room-style-suitability, or multi-room conflict constraint. Those capabilities would require extending the planning model and constraint provider.

## 4. Mathematical Model and Solver Logic

Let \(L\) be the set of lessons, \(T\) the set of available timeslots, and \(S\) the set of students. The solver chooses a timeslot \(t(l) \in T\) for each lesson \(l \in L\), and an optional student \(s(l) \in S \cup \{\varnothing\}\) for private lessons. Group lessons do not receive a student assignment. Weekly availability, one-time unavailability, teacher and student relationships, schedule dates, and lesson data are problem facts.

Timefold evaluates each candidate schedule with a lexicographic `HardSoftScore` \((H, S)\). Hard-constraint violations reduce the hard score; soft penalties and rewards contribute to the soft score. Feasibility requires a hard score of zero. Feasible schedules are then ranked by soft score. The solver uses a default termination limit of 10 seconds and reproducible environment mode; the termination limit is configurable.

**Important scope distinction:** the implemented constraints below describe the current backend. The model does not currently contain a separate room variable or a soft constraint for explicit student preferred-time matching. Student time preferences are represented through availability; private-lesson assignment and prime-time use have explicit rewards.

### 4.1 Hard constraints (feasibility)

| Constraint | Implemented behavior |
|---|---|
| Teacher conflict | A teacher cannot teach two lessons in the same timeslot. |
| Concurrent private lesson limit | No more than four private lessons can be assigned to one timeslot (the single-room capacity assumption). |
| Group lesson occupancy | Private lessons cannot be scheduled during a group lesson. |
| Dual-role conflict | A teacher cannot teach while attending another lesson as a student at the same time. |
| Self-teaching prevention | A teacher cannot teach a group lesson in which they are enrolled as a student. |
| Teacher daily limit | A teacher cannot exceed the configured maximum number of lessons per day (default: six). |
| Teacher weekly availability | Lessons must fit within the teacher's weekly availability windows. |
| Teacher one-time unavailability | Lessons cannot conflict with a teacher's dated unavailability entries. |
| Teacher desired weekly maximum | A teacher cannot exceed `desiredLessonsPerWeek`, when configured. |
| Group lesson student assignment | Group lessons cannot have an assigned private-lesson student. |
| Teacher-student relationship | A private lesson's student must be subscribed to that teacher. |
| Student conflict | A student cannot be assigned to two lessons in the same timeslot. |
| Student daily private-lesson limit | A student can have at most one private lesson per day. |
| Student availability | Private lessons must fit the student's weekly availability and avoid one-time unavailability. |
| Student daily availability-window limit | A student's daily lesson count cannot exceed the number of that day's availability windows. |
| Student desired weekly maximum | A student cannot exceed `desiredLessonsPerWeek`, when configured. |

### 4.2 Soft constraints (quality optimization)

| Constraint | Effect |
|---|---|
| Private lesson assignment reward | Rewards assigning a student to a private lesson; an unassigned template remains possible when no valid match exists. |
| Teacher schedule gaps | Penalizes idle gaps between a teacher's lessons on the same day when they exceed the configured threshold (default: 15 minutes). |
| Teacher workload balance | Penalizes teacher workloads using a squared lesson-count penalty, encouraging more even distribution. |
| Prime-time utilization | Rewards lessons starting from 16:00 up to (but not including) 21:00. |
| Teacher desired weekly count | Penalizes teachers for falling short of their desired weekly lesson count. |
| Student desired weekly count | Penalizes students for falling short of their desired weekly lesson count. |

Weights and thresholds are configurable under `solver.weights.*` in `src/main/resources/application.properties` and `SolverWeightsConfig`. For example, the configured application properties reward private lesson assignment by 100 and prime-time use by 10. Constraint tests are in `src/test/java/com/timetable/backend/solver`.

## 5. Local Setup and Execution

### 5.1 Start MySQL with Docker

```bash
docker run --name timetabling-mysql -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=timetable_db -p 3306:3306 -d mysql:8.0
```

Check that the container is running with `docker ps`.

### 5.2 Restore an optional SQL dump

Place `dump.sql` in the repository root (or adjust the path in the command).

**Windows PowerShell**

```powershell
Get-Content dump.sql | Where-Object { $_ -notmatch '^mysqldump:' } | docker exec -i timetabling-mysql mysql -u root -proot timetable_db
```

**Linux / macOS**

```bash
docker exec -i timetabling-mysql mysql -u root -proot timetable_db < dump.sql
```

### 5.3 Configure the application

The defaults in `src/main/resources/application.properties` match the Docker command above:

```properties
spring.datasource.url=jdbc:mysql://${MYSQL_IP:localhost}:3306/timetable_db
spring.datasource.username=root
spring.datasource.password=root
```

Flyway applies migrations `V1`–`V23` at startup. The following environment variables can override application defaults:

| Variable | Default | Purpose |
|---|---|---|
| `MYSQL_IP` | `localhost` | MySQL host used in the JDBC URL |
| `TIMETABLE_API_PORT` | `8080` | Backend HTTP port |
| `JWT_SECRET` | Development-only key | Base64-encoded JWT signing secret |

The checked-in credentials and JWT key are for local development only. Use a strong secret and non-default database credentials outside a local development environment.

### 5.4 Launch the backend

**Windows PowerShell**

```powershell
.\mvnw spring-boot:run
```

**Linux / macOS**

```bash
./mvnw spring-boot:run
```

The API listens at `http://localhost:8080` by default.

### 5.5 Run tests

```powershell
.\mvnw clean test
```

The test profile uses an in-memory H2 database and disables Flyway, so tests do not require the local MySQL container.

## 6. Key REST API Endpoints

All endpoints other than the public authentication and selected public dance-style reads require a bearer JWT. Solver administration endpoints require an admin account.

| Domain | Method | Endpoint | Purpose |
|---|---|---|---|
| Authentication | `POST` | `/api/auth/register` | Register an account |
| Authentication | `POST` | `/api/auth/login` | Authenticate and obtain a JWT |
| Solver (admin) | `POST` | `/api/admin/solver/solve/{scheduleId}` | Start solving a schedule |
| Solver (admin) | `GET` | `/api/admin/solver/status/{scheduleId}` | Get solver job status |
| Solver (admin) | `POST` | `/api/admin/solver/stop/{scheduleId}` | Stop a solver job |
| Solver (admin) | `GET` | `/api/admin/solver/solution/{scheduleId}` | Retrieve the solution |
| Solver (admin) | `GET` | `/api/admin/solver/score-explanation/{scheduleId}` | Explain score matches and violations |
| Solver (admin) | `GET` | `/api/admin/solver/unmet-students/{scheduleId}` | Retrieve unmet student demand |
| Schedules | `GET` | `/api/schedules` | List schedules |
| Schedules | `GET` | `/api/schedules/{id}` | Retrieve schedule metadata |
| Weekly availability | `GET` | `/api/availability/weekly` | Read weekly availability |
| Weekly availability | `POST` | `/api/availability/weekly` | Create weekly availability |

**Endpoint naming note:** the solver routes currently use `/api/admin/solver/...` and a `{scheduleId}` path parameter. The codebase does not currently expose `/api/solver/solve`, `/api/solver/status/{jobId}`, `/api/solver/stop/{jobId}`, `/api/schedules/{id}/score-explanation`, `/api/schedules/{id}/analytics`, or `/api/availability/weekly` as a solver or analytics route. Schedule score explanations are served under `/api/admin/solver/score-explanation/{scheduleId}`; no schedule analytics endpoint is implemented.

## References

- [Timefold Solver documentation](https://docs.timefold.ai/)
- [Spring Boot documentation](https://docs.spring.io/spring-boot/)
- [Flyway documentation](https://documentation.red-gate.com/flyway)
