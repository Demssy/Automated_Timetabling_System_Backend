# 🎵 Automated Timetabling System Backend

Automated timetable generation system for a dance school powered by **Timefold Solver**.

---

## 🚀 About the Project

**Automated Timetabling System** is a production-ready REST API for automatic generation of optimal dance school lesson schedules, taking into account multiple hard and soft constraints (teacher availability, room capacity, group levels, time gaps optimization, etc.).

### Key Features:
- ✅ **Automated timetabling** using Timefold Solver with Constraint Streams API
- ✅ **Advanced Dual-Mode logic** - parallel private lessons in the same room (weighted constraints)
- ✅ **Asynchronous solving** - non-blocking schedule optimization with status monitoring
- ✅ **JWT Authentication** - secure stateless authentication with HTTP-only cookies
- ✅ **Role-Based Access Control** - role separation (Admin, Teacher, Student)
- ✅ **RESTful API** - clean API design with DTOs and MapStruct mapping
- ✅ **Pinned lessons** - support for pre-assigned lessons
- ✅ **Resource unavailability** - teacher and room availability constraints

---

## 🛠️ Technology Stack

| Component | Technology | Version |
|-----------|-----------|--------|
| **Backend Framework** | Spring Boot | 3.5.8 |
| **Language** | Java | 21 |
| **Optimization Engine** | Timefold Solver | 1.6.0 |
| **Database** | MySQL | 8.0 |
| **Migrations** | Flyway | Core + MySQL |
| **Security** | Spring Security 6 + JWT | JJWT 0.11.5 |
| **Password Hashing** | Argon2 (BouncyCastle) | 1.78 |
| **Mapping** | MapStruct | 1.5.5.Final |
| **Validation** | Jakarta Validation + Hibernate Validator | 3.0.2 / 8.0.1 |
| **Test Database** | H2 | Runtime |
| **Build Tool** | Maven | Wrapper included |

---

## 📦 Project Structure

```
src/
├── main/
│   ├── java/com/timetable/backend/
│   │   ├── BackendApplication.java  # Main Spring Boot Application
│   │   ├── config/                  # Spring Configuration (SecurityConfig)
│   │   ├── controller/              # REST Controllers (5 controllers)
│   │   │   ├── AuthController.java
│   │   │   ├── DictionaryController.java
│   │   │   ├── SolverController.java
│   │   │   ├── TeacherController.java
│   │   │   └── UserController.java
│   │   ├── domain/
│   │   │   ├── dto/                 # Data Transfer Objects (16 DTOs)
│   │   │   ├── mapper/              # MapStruct Mappers (4 mappers)
│   │   │   ├── model/               # JPA Entities (14 entities)
│   │   │   │   ├── AbstractUser.java (JOINED inheritance)
│   │   │   │   ├── Student.java, Teacher.java, Admin.java
│   │   │   │   ├── Lesson.java (@PlanningEntity)
│   │   │   │   ├── DanceGroup.java, DanceStyle.java
│   │   │   │   ├── Timeslot.java, Room.java
│   │   │   │   ├── ResourceUnavailability.java
│   │   │   │   ├── ScheduleMetadata.java
│   │   │   │   └── Role.java, DanceLevel.java, ScheduleStatus.java
│   │   │   └── repository/          # Spring Data JPA Repositories (10 repos)
│   │   ├── security/                # JWT + Spring Security
│   │   │   ├── JwtService.java
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   └── JpaUserDetailsService.java
│   │   ├── service/                 # Business Logic (4 services)
│   │   │   ├── AuthService.java
│   │   │   ├── SolverService.java
│   │   │   ├── TeacherService.java
│   │   │   └── UserService.java
│   │   └── solver/                  # Timefold Solver
│   │       ├── DanceSchedule.java (@PlanningSolution)
│   │       └── DanceScheduleConstraintProvider.java
│   └── resources/
│       ├── application.properties   # Main config
│       ├── application-test.properties
│       └── db/migration/            # Flyway SQL Migrations (V1-V6)
│           ├── V1__init.sql         # User tables (JOINED inheritance)
│           ├── V2__dictionaries.sql # Dance styles, Rooms
│           ├── V3__solver_entities.sql # Timeslots, Groups, Lessons
│           ├── V4__create_schedules_table.sql
│           ├── V5__add_version_for_optimistic_locking.sql
│           └── V6__insert_default_roles.sql
└── test/                            # Comprehensive Test Suite (18+ tests)
    ├── controller/                  # Controller Tests (6 tests)
    ├── service/                     # Service Tests (3 tests)
    ├── security/                    # Security Tests (3 tests)
    ├── solver/                      # Solver Tests (2 tests)
    ├── repository/                  # Repository Tests (2 tests)
    └── domain/mapper/               # Mapper Tests (2 tests)
```

---

## 📚 Documentation

This README serves as the main documentation for the project.

### Additional Resources:
- 🔧 [Copilot Instructions](/.github/copilot-instructions.md) - Backend coding guidelines
- 🌐 [Global Instructions](/global-copilot-instructions) - System architecture overview
- 📖 [Timefold Solver Docs](https://docs.timefold.ai/) - Official Timefold documentation
- 🍃 [Spring Boot Docs](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- 🗺️ [MapStruct Docs](https://mapstruct.org/documentation/stable/reference/html/)

---

## 🚦 Development Status

### ✅ Completed Features:

#### EPIC 1: Project Foundation
- ✅ Java 21 + Spring Boot 3.5.8 setup
- ✅ MySQL 8.0 integration
- ✅ Flyway migrations configured
- ✅ Maven build configuration with annotation processors

#### EPIC 2: Domain Core & Security
- ✅ **JPA Entities** (14 entities with JOINED Inheritance for User hierarchy)
  - AbstractUser → Student, Teacher, Admin
  - Role, DanceStyle, Room, Timeslot, DanceGroup
  - Lesson (with @PlanningEntity), ResourceUnavailability
  - ScheduleMetadata, DanceLevel enum, ScheduleStatus enum
- ✅ **Spring Security 6 + JWT**
  - Stateless authentication with HTTP-only cookies
  - JwtService with HS256 signing
  - JwtAuthenticationFilter for token validation
  - JpaUserDetailsService for user loading
- ✅ **Password Security**
  - Argon2 password hashing (BouncyCastle 1.78)
- ✅ **Flyway Migrations** (V1-V6)
  - V1: User tables with JOINED inheritance
  - V2: Dictionaries (dance_styles, rooms, teacher_dance_style)
  - V3: Solver entities (timeslots, dance_groups, lessons, resource_unavailability)
  - V4: Schedule metadata table
  - V5: Optimistic locking (@Version)
  - V6: Default roles insertion
- ✅ **REST API Controllers** (5 controllers)
  - AuthController (register, login, logout)
  - DictionaryController (rooms, dance styles CRUD)
  - TeacherController (teacher management)
  - UserController (user profile management)
  - SolverController (optimization endpoints)
- ✅ **MapStruct Mappers** (4 mappers)
  - DictionaryMapper, TeacherMapper, StudentMapper, LessonMapper
- ✅ **Spring Data JPA Repositories** (10 repositories)
- ✅ **Comprehensive Testing** (18+ test classes)

#### EPIC 3: Solver MVP & Constraint Engine ✅ COMPLETED
- ✅ **[BE-10] Timefold Solver Configuration**
  - DanceSchedule (@PlanningSolution) with all problem facts
  - Lesson (@PlanningEntity) with timeslot and room planning variables
  - @PlanningPin support for fixed lessons
  - Timefold Solver Spring Boot Starter integration
- ✅ **[BE-11] DanceScheduleConstraintProvider**
  - **Hard Constraints:**
    - Room conflict with Dual-Mode logic (Group: 1.0, Private: 0.25)
    - Teacher conflict (no double-booking)
    - Teacher availability (respects ResourceUnavailability)
  - **Soft Constraints:**
    - Minimize teacher gaps (optimizes teacher schedule compactness)
    - Prime time reward (16:00-21:00 bonus)
    - Load balancing (fair lesson distribution)
- ✅ **[BE-12] SolverService**
  - Asynchronous solving with SolverManager
  - Problem loading from database
  - Solution persistence
  - Status monitoring (SOLVING_ACTIVE, NOT_SOLVING, etc.)
- ✅ **[BE-13] Unit tests for constraints**
  - DanceScheduleConstraintProviderTest
  - SolverLoadIntegrationTest
- ✅ **[BE-14] SolverController + REST API**
  - POST /api/solver/solve/{scheduleId}
  - GET /api/solver/status/{scheduleId}
  - POST /api/solver/terminate/{scheduleId}
  - GET /api/solver/solution/{scheduleId}

### 🎯 Current State:
**The system is fully functional with a complete MVP implementation of the Timefold Solver.**
All core features for automatic timetable generation are implemented and tested.

---

## 🏃 Quick Start

### Requirements:
- Java 21+
- MySQL 8.0
- Maven 3.8+ (or use included Maven wrapper)

### Installation and Running:

```powershell
# 1. Clone the repository
git clone <repository-url>
cd Automated_Timetabling_System_Backend

# 2. Set up MySQL database
mysql -u root -p
# In MySQL console:
# CREATE DATABASE timetable_db;
# EXIT;

# 3. (Optional) Configure environment variables
# For PowerShell:
$env:MYSQL_IP="localhost"
$env:JWT_SECRET="your_secret_key_here"

# 4. Build the project
.\mvnw.cmd clean install

# 5. Run the application
.\mvnw.cmd spring-boot:run
```

Application will be available at: `http://localhost:8080`

### Configuration:
The application uses the following default configurations (see `application.properties`):
- **Database:** `jdbc:mysql://localhost:3306/timetable_db`
- **Username:** `root` / **Password:** `root`
- **Server Port:** `8080`
- **JWT Expiration:** `3600000ms` (1 hour)
- **Solver Termination:** `60s` (configurable)

---

## 🔌 API Endpoints

### Authentication (`/api/auth`)
- `POST /api/auth/register` - Register new student
  - Body: `{ "email", "password", "fullName", "birthDate" }`
  - Returns: User object + Sets JWT cookie
- `POST /api/auth/login` - Login
  - Body: `{ "email", "password" }`
  - Returns: User object + Sets JWT cookie
- `POST /api/auth/logout` - Logout (clears JWT cookie)

### Dictionaries (`/api/dictionaries`) - ADMIN only
- `GET /api/dictionaries/rooms` - List all rooms
- `POST /api/dictionaries/rooms` - Create new room
- `GET /api/dictionaries/rooms/{id}` - Get room by ID
- `PUT /api/dictionaries/rooms/{id}` - Update room
- `DELETE /api/dictionaries/rooms/{id}` - Delete room
- `GET /api/dictionaries/styles` - List all dance styles
- `POST /api/dictionaries/styles` - Create new dance style
- `GET /api/dictionaries/styles/{id}` - Get dance style by ID
- `PUT /api/dictionaries/styles/{id}` - Update dance style
- `DELETE /api/dictionaries/styles/{id}` - Delete dance style

### Teachers (`/api/teachers`) - ADMIN only
- `POST /api/teachers` - Create new teacher
- `GET /api/teachers/{id}` - Get teacher details
- `PUT /api/teachers/{id}` - Update teacher
- `DELETE /api/teachers/{id}` - Delete teacher

### Users (`/api/users`)
- `GET /api/users/me` - Get current user profile
- `PUT /api/users/me` - Update current user profile

### Solver (`/api/solver`)
- `POST /api/solver/solve/{scheduleId}` - Start schedule optimization
  - Starts asynchronous solving process
- `GET /api/solver/status/{scheduleId}` - Get solver status
  - Returns: `{ "solverStatus": "NOT_SOLVING" | "SOLVING_ACTIVE" | "SOLVING_SCHEDULED" }`
- `POST /api/solver/terminate/{scheduleId}` - Stop solving process
- `GET /api/solver/solution/{scheduleId}` - Get optimized solution
  - Returns: Complete schedule with assigned timeslots and rooms + score

📖 **API Authentication:** All endpoints (except `/api/auth/register` and `/api/auth/login`) require JWT authentication via HTTP-only cookie.

---

## 🧪 Testing

```powershell
# Run all tests
.\mvnw.cmd test

# Run specific test class
.\mvnw.cmd test -Dtest=DanceScheduleConstraintProviderTest

# Run with coverage report (if jacoco is configured)
.\mvnw.cmd test jacoco:report
```

**Current test coverage:** High (all main components covered)

### Test Suite (18+ test classes):
- **Controller Tests (6):** AuthController, DictionaryController, SolverController, TeacherController, UserController (2)
- **Service Tests (3):** AuthService, TeacherService, UserService
- **Security Tests (3):** JwtService, JwtAuthenticationFilter, UserRepository
- **Solver Tests (2):** DanceScheduleConstraintProvider, SolverLoadIntegration
- **Repository Tests (2):** DanceStyleRepository, RoomRepository
- **Mapper Tests (2):** DictionaryMapper, TeacherMapper

---

## 🗃️ Database

### Flyway Migrations:
- **V1__init.sql** - User tables creation (users, teachers, students, admins, roles)
- **V2__dictionaries.sql** - Dictionaries (dance_styles, rooms, teacher_dance_style)
- **V3__solver_entities.sql** - Solver entities (timeslots, dance_groups, lessons, resource_unavailability)
- **V4__create_schedules_table.sql** - Schedule metadata table
- **V5__add_version_for_optimistic_locking.sql** - Optimistic locking support
- **V6__insert_default_roles.sql** - Default roles (STUDENT, TEACHER, ADMIN)

### Database Schema:
- **Total tables:** 12 (users, students, teachers, admins, roles, dance_styles, rooms, timeslots, dance_groups, lessons, resource_unavailability, schedule_metadata)
- **Inheritance Strategy:** JOINED (for User hierarchy)
- **Optimistic Locking:** @Version annotation on all entities

---

## 🎯 Solver Optimization Details

### Hard Constraints (Must be satisfied):
1. **Room Conflict Prevention** - Implements Dual-Mode logic:
   - Group lessons occupy 100% of room capacity (weight = 1.0)
   - Private lessons occupy 25% of room capacity (weight = 0.25)
   - Up to 4 private lessons can run in parallel in the same room at the same time
2. **Teacher Conflict Prevention** - A teacher cannot teach multiple lessons simultaneously
3. **Teacher Availability** - Lessons must respect teacher unavailability periods (via ResourceUnavailability)

### Soft Constraints (Optimized for quality):
1. **Minimize Teacher Gaps** - Reduces idle time between lessons for the same teacher on the same day
2. **Prime Time Reward** - Encourages scheduling during peak hours (16:00-21:00)
3. **Load Balancing** - Distributes lessons fairly among teachers to avoid overloading

### Planning Variables:
- **Timeslot** - When the lesson takes place (day + time)
- **Room** - Where the lesson takes place

### Supported Features:
- **Pinned Lessons** - Pre-assigned lessons that won't be changed by the solver
- **Resource Unavailability** - Teacher and room availability constraints
- **Asynchronous Solving** - Non-blocking optimization with real-time status monitoring

---

## 🏗️ Architecture

### Layered Architecture

```
Controller → Service → Repository → Database
     ↓          ↓
    DTO    Entity (Domain Model)
```

### Key Components:

#### 1. Domain Model (14 JPA Entities)
- **User Hierarchy (JOINED Inheritance):**
  - `AbstractUser` → `Student`, `Teacher`, `Admin`
  - `Role` - User roles (STUDENT, TEACHER, ADMIN)
- **Solver Entities:**
  - `Lesson` (@PlanningEntity) - The main entity to be scheduled
  - `Timeslot` - Time slot definition (day, start time, end time)
  - `Room` - Physical room with capacity
  - `DanceGroup` - Student group definition
  - `DanceStyle` - Dance style (e.g., Ballet, Hip-Hop)
  - `ResourceUnavailability` - Teacher/room unavailability periods
  - `ScheduleMetadata` - Schedule metadata and status
- **Enums:**
  - `DanceLevel` (BEGINNER, ELEMENTARY, INTERMEDIATE, ADVANCED, PROFESSIONAL)
  - `ScheduleStatus` (DRAFT, SOLVING, SOLVED, PUBLISHED)

#### 2. Security Layer
- **JwtService** - JWT token generation and validation (HS256)
- **JwtAuthenticationFilter** - Extracts and validates JWT from HTTP-only cookies
- **JpaUserDetailsService** - Loads user details from database
- **SecurityConfig** - Spring Security configuration with CORS and stateless sessions
- **Password Encoding** - Argon2 hashing for maximum security

#### 3. Solver Components
- **DanceSchedule** (@PlanningSolution) - The complete planning problem
- **DanceScheduleConstraintProvider** - Defines all optimization constraints
- **SolverService** - Manages asynchronous solving with SolverManager
- **SolverController** - REST API for solver operations

#### 4. Data Transfer Layer
- **16 DTOs** - Clean separation between API and domain models
- **4 MapStruct Mappers** - Automatic DTO ↔ Entity conversion
- **Validation** - Jakarta Validation API with Hibernate Validator

### Key Principles:
- **DTO Pattern** - Controllers never return entities directly
- **Constructor Injection** - Via @RequiredArgsConstructor (no @Autowired)
- **Stateless API** - JWT without sessions
- **RBAC** - Role-Based Access Control via @PreAuthorize
- **Optimistic Locking** - @Version on all entities to prevent concurrent updates
- **Clean Code** - Lombok annotations reduce boilerplate

---

## 🔐 Security

- **Argon2** for password hashing (most secure algorithm)
- **JWT** with HS256 signature
- **HTTP-only cookies** for tokens
- **CORS** configured
- **Role-Based Access Control** (ADMIN, TEACHER, STUDENT)

⚠️ **Important for production:**
- Change `jwt.secret` to a cryptographically strong key
- Enable HTTPS (`cookieSecure=true`)
- Configure specific CORS origins

---

## 🤝 Contributing

This project is under active development. Contribution guidelines will be added later.

---

## 📝 License

(To be added)

---

## 👥 Contact

**Project:** Automated Timetabling System Backend  
**Version:** 0.0.1-SNAPSHOT  
**Start Date:** December 31, 2025  
**Last Updated:** January 8, 2026  

---

## 📖 Additional Resources

- [Timefold Solver Documentation](https://docs.timefold.ai/)
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [MapStruct Documentation](https://mapstruct.org/documentation/stable/reference/html/)

---

*Made with ❤️ for dance schools*

