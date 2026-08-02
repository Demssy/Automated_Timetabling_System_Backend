# Automated Timetabling System Backend

A Spring Boot REST API for a dance-school timetabling system. It uses constraint satisfaction and optimization with [Timefold Solver](https://timefold.ai/) (the successor to OptaPlanner) to generate high-quality lesson schedules while respecting operational, teacher, and student constraints.

## ✨ Core Features

- **Constraint-based schedule generation** using Timefold Constraint Streams with hard and soft scoring.
- **Availability-aware planning** that respects recurring weekly availability and one-time resource unavailability for teachers and students.
- **Single-room scheduling model**: group lessons are pinned; private lessons are assigned a timeslot and an eligible student, with a maximum of four concurrent private lessons and no private lessons during a group lesson.
- **Schedule quality optimization** for student assignment, prime-time use, compact teacher schedules, balanced workloads, and desired weekly lesson counts.
- **Asynchronous solving** with start, stop, status polling, solution snapshots, score explanations, and unmet-student reporting.
- **Management modules** for users, teachers, students, lessons, schedules, dance groups, dance styles, rooms, timeslots, and unavailability.
- **JWT authentication and role-based access control** for `ADMIN`, `TEACHER`, and `STUDENT` roles. Tokens are delivered in an HTTP-only `jwt` cookie.
- **Versioned database schema** managed by Flyway migrations.

## 🛠️ Tech Stack

| Area | Technology |
|---|---|
| Language | Java 21 |
| Application framework | Spring Boot 3.5.8 |
| Scheduling and constraints | Timefold Solver 1.6.0 / Constraint Streams (OptaPlanner successor) |
| Persistence | Spring Data JPA and Hibernate |
| Database | MySQL 8.0+ |
| Schema migrations | Flyway SQL migrations |
| Security | Spring Security, JJWT 0.11.5, Argon2 password hashing |
| API validation and mapping | Jakarta Validation, MapStruct, Lombok |
| Build tool | Maven Wrapper (`mvnw`, `mvnw.cmd`) |
| Test database | H2 |
| Containerization | Docker with MySQL |

## 🏗️ Project Structure

```text
src/
├── main/
│   ├── java/com/timetable/backend/
│   │   ├── config/              # Security and solver-weight configuration
│   │   ├── controller/          # REST controllers and exception handling
│   │   ├── domain/
│   │   │   ├── dto/             # API request and response contracts
│   │   │   ├── mapper/          # MapStruct DTO mappers
│   │   │   ├── model/           # JPA entities and solver planning entities
│   │   │   └── repository/      # Spring Data JPA repositories
│   │   ├── security/            # JWT service, filter, and user-details service
│   │   ├── service/             # Business logic and solver orchestration
│   │   └── solver/              # Planning solution and constraint provider
│   └── resources/
│       ├── application.properties
│       ├── application-test.properties
│       └── db/migration/        # Flyway SQL migrations
└── test/                        # Controller, service, integration, and solver tests
```

### Scheduling Architecture

- `Lesson` is both a JPA entity and a Timefold planning entity.
- `DanceSchedule` is the Timefold planning solution. Its problem facts include timeslots, students, teachers, weekly availability, and one-time unavailability.
- `Lesson.timeslot` and nullable `Lesson.student` are planning variables. `Room` is not a planning variable because the application models one studio room.
- Group lessons are pinned and retain their preassigned time. The solver schedules private lesson templates.
- Solver output is persisted as `ScheduledLesson` snapshots, separate from the input lesson plan.

## 🚀 Getting Started

### Prerequisites

- Java 21 (Java 17+ may work, but the project is built and configured for Java 21)
- MySQL 8.0+ or Docker
- Maven 3.9+ (optional; the Maven Wrapper is included)

### 1. Start MySQL

Create a local database and use credentials that match your configuration:

```sql
CREATE DATABASE timetable_db;
```

Alternatively, run MySQL with Docker:

```bash
docker run --name timetable-mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=timetable_db \
  -p 3306:3306 \
  -d mysql:8.4.7
```

The repository's `Dockerfile` is also based on the MySQL image and exposes port `3306`.

### 2. Configure the Application

The default development settings are in `src/main/resources/application.properties`. The following environment variables override the most common settings:

| Variable | Default | Purpose |
|---|---|---|
| `MYSQL_IP` | `localhost` | MySQL host used in the JDBC URL |
| `TIMETABLE_API_PORT` | `8080` | HTTP port for the API |
| `JWT_SECRET` | Development-only Base64 key | Base64-encoded HS256 signing key |

PowerShell example:

```powershell
$env:MYSQL_IP = "localhost"
$env:TIMETABLE_API_PORT = "8080"
$env:JWT_SECRET = "<base64-encoded-production-secret>"
```

For production, set a strong unique `JWT_SECRET`, use a non-root database account, configure `application.security.cookie.secure=true` behind HTTPS, and restrict CORS origins in `SecurityConfig`.

### 3. Run Database Migrations

Flyway is enabled by default. It automatically applies the versioned SQL scripts in `src/main/resources/db/migration` when the application starts.

Ensure that `timetable_db` exists and the configured database user can create and alter tables; then start the application. Do not manually run migrations that Flyway has already recorded.

### 4. Build and Run

**Windows (PowerShell):**

```powershell
.\mvnw.cmd clean install
.\mvnw.cmd spring-boot:run
```

**macOS/Linux:**

```bash
./mvnw clean install
./mvnw spring-boot:run
```

The API is available at `http://localhost:8080` unless `TIMETABLE_API_PORT` is set.

### 5. Run Tests

The test profile uses an in-memory H2 database and disables MySQL-specific Flyway migrations.

```powershell
.\mvnw.cmd test
```

```bash
./mvnw test
```

Run the solver constraint tests only:

```powershell
.\mvnw.cmd test -Dtest=DanceScheduleConstraintProviderTest
```

## 🔐 Authentication and Authorization

`POST /api/auth/register` and `POST /api/auth/login` return user data and set an HTTP-only `jwt` cookie. The browser sends that cookie on subsequent requests. `GET` or `POST /api/auth/logout` clears it.

All non-public endpoints require authentication. Controller methods apply role checks where applicable:

| Role | Primary capabilities |
|---|---|
| `ADMIN` | Manage dictionaries, users, schedules, lessons, and solver runs |
| `TEACHER` | View and update own profile, view their student pool and schedule |
| `STUDENT` | Manage preferred teachers, availability, group membership, and own schedule |

Public read access is available for dance-style lists and the active published schedule. Configure HTTPS, cookie security, and CORS restrictions before production deployment.

## 📡 API Endpoints Overview

All endpoints are prefixed with the application host, for example `http://localhost:8080`. This is an overview; request and response fields are defined by the DTOs in `domain/dto`.

| Area | Base path | Main operations | Access |
|---|---|---|---|
| Authentication | `/api/auth` | `POST /register`, `POST /login`, `GET|POST /logout` | Public |
| Current user | `/api/user` | `GET /me`, `PUT /me/availability` | Authenticated |
| Teachers | `/api/teachers` | CRUD, `GET /me/profile`, `PUT /me/profile`, student pool and availability | Admin for CRUD; teacher self-service |
| Students | `/api/students` | CRUD, preferred-teacher management, `GET|PUT /me/profile` | Admin for CRUD; student self-service |
| Lessons | `/api/lessons` | CRUD, `PUT /{id}/toggle-active`, `GET /active-schedule`, `GET /my-schedule` | Admin for management; role-based schedule reads |
| Schedules | `/api/schedules` | CRUD, `PATCH /{id}/publish`, `/archive`, `/draft` | Admin for changes |
| Solver | `/api/admin/solver` | `POST /solve/{scheduleId}`, `GET /status/{scheduleId}`, `POST /stop/{scheduleId}`, `GET /solution/{scheduleId}`, score and unmet-student reports | Admin, except solution reads also allow teachers and students |
| Dance groups | `/api/groups` | List groups, list relevant groups, list members, enroll and unenroll | Authenticated |
| Dance-group administration | `/api/dictionaries/groups` | CRUD dance-group definitions | Admin |
| Dance styles | `/api/dance-styles` | Public reads; admin CRUD | Public reads; admin writes |
| Rooms | `/api/dictionaries/rooms` | CRUD room definitions | Admin |
| Timeslots | `/api/dictionaries/timeslots` | CRUD timeslot definitions | Admin |
| One-time unavailability | `/api/unavailability` | Get or replace user-specific availability exceptions | Authenticated reads; admin writes |

The legacy-compatible dictionary style routes `/api/dictionaries/styles` and `/api/dictionaries/dance-styles` are also available. The solver API is intentionally located at `/api/admin/solver`, not `/api/solver`.

## 🧠 Constraint Model

The solver uses `HardSoftScore`: hard constraints represent invalid schedules; soft constraints guide optimization among valid schedules.

### Hard Constraints

- At most four private lessons may occupy one timeslot.
- A private lesson cannot occupy a timeslot that contains a group lesson.
- A teacher or student cannot be double-booked.
- A teacher may not exceed the configured daily lesson limit.
- Teacher and student weekly availability must contain the assigned lesson.
- Teacher and student one-time unavailability must be respected.
- A private lesson can only be assigned to a student subscribed to its teacher.
- A group lesson cannot receive an individual student assignment.
- Student and teacher desired weekly lesson limits are enforced.

### Soft Constraints

- Reward successful student-to-private-lesson assignments.
- Prefer prime-time lessons.
- Minimize meaningful gaps between a teacher's lessons.
- Balance teacher workloads.
- Encourage meeting desired weekly lesson counts.

Constraint weights and thresholds are externalized under the `solver.weights.*` properties. The default Timefold termination limit is `10s`; configure it with `timefold.solver.termination.spent-limit`.

## 🗃️ Database and Migrations

Flyway migrations are SQL files named `V{version}__{description}.sql`. They define the user hierarchy, scheduling domain, availability, schedule snapshots, and later schema enhancements.

When evolving the persistence model:

1. Add a new, sequential Flyway migration under `src/main/resources/db/migration`.
2. Do not modify migrations already applied to shared environments.
3. Start the application and let Flyway validate and apply the migration.

## 📚 Useful References

- [Timefold Solver documentation](https://docs.timefold.ai/)
- [Spring Boot documentation](https://docs.spring.io/spring-boot/)
- [Spring Security documentation](https://docs.spring.io/spring-security/reference/)
- [Flyway documentation](https://documentation.red-gate.com/flyway)
