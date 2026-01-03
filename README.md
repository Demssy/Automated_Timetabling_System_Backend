# 🎵 Automated Timetabling System Backend

Automated timetable generation system for a dance school powered by **Timefold Solver**.

---

## 🚀 About the Project

**Automated Timetabling System** is a REST API for automatic generation of optimal dance school lesson schedules, taking into account multiple constraints (teacher availability, room capacity, group levels, etc.).

### Key Features:
- ✅ **Automated timetabling** using Timefold Solver
- ✅ **Dual-Mode logic** - parallel private lessons in the same room
- ✅ **JWT Authentication** - secure authentication
- ✅ **Role-Based Access Control** - role separation (Admin, Teacher, Student)
- ✅ **RESTful API** - clean API design with DTOs

---

## 🛠️ Technology Stack

| Component | Technology | Version |
|-----------|-----------|--------|
| **Backend Framework** | Spring Boot | 3.5.8 |
| **Language** | Java | 21 |
| **Optimization Engine** | Timefold Solver | 1.6.0 |
| **Database** | MySQL | 8.0 |
| **Migrations** | Flyway | - |
| **Security** | Spring Security + JWT | JJWT 0.11.5 |
| **Password Hashing** | Argon2 | - |
| **Mapping** | MapStruct | 1.5.5 |
| **Build Tool** | Maven | - |

---

## 📦 Project Structure

```
src/
├── main/
│   ├── java/com/timetable/backend/
│   │   ├── config/              # Spring Configuration
│   │   ├── controller/          # REST Controllers
│   │   ├── domain/
│   │   │   ├── dto/             # Data Transfer Objects
│   │   │   ├── mapper/          # MapStruct Mappers
│   │   │   ├── model/           # JPA Entities
│   │   │   └── repository/      # Spring Data JPA Repositories
│   │   ├── security/            # JWT + Spring Security
│   │   ├── service/             # Business Logic
│   │   └── solver/              # Timefold Solver (PlanningSolution, Constraints)
│   └── resources/
│       ├── db/migration/        # Flyway SQL Migrations
│       └── application.properties
└── test/                        # Unit and Integration Tests
```

---

## 📚 Documentation

**All technical documentation is located in the [`docs/`](./docs/) folder**

### Main Documents:
- 📖 [**Documentation Navigation**](./docs/README.md) - start here!
- 📊 [**Project Analysis**](./docs/analysis/project_analysis.md) - what's already implemented
- 📋 [**Task Reports**](./docs/reports/) - detailed reports for each task
- 🎯 [**EPIC 3 Plan**](docs/epic3/epic3_detailed_analysis.md) - detailed Solver implementation plan

---

## 🚦 Development Status

### ✅ Completed Epics:

#### EPIC 2: Domain Core & Security
- ✅ JPA Entities (User hierarchy with JOINED Inheritance)
- ✅ Spring Security 6 + JWT
- ✅ Flyway Migrations (V1, V2, V3)
- ✅ CRUD API for dictionaries (Rooms, Dance Styles)
- ✅ Teacher Management API
- ✅ Student Registration
- ✅ MapStruct Mappers
- ✅ Comprehensive Testing

### ✅ Recently Completed:

#### EPIC 3: Solver MVP & Constraint Engine (Completed)
- ✅ [BE-10] Timefold Solver Configuration - **COMPLETED**
  - ✅ Entities: Timeslot, DanceGroup, Lesson, ResourceUnavailability
  - ✅ PlanningSolution: DanceSchedule
  - ✅ Flyway Migration V3
  - ✅ Repositories
- ✅ [BE-11] DanceScheduleConstraintProvider - **COMPLETED**
- ✅ [BE-12] SolverService (asynchronous) - **COMPLETED**
- ✅ [BE-13] Unit tests for constraints - **COMPLETED**
- ✅ [BE-14] SolverController + REST API - **COMPLETED**

---

## 🏃 Quick Start

### Requirements:
- Java 21+
- MySQL 8.0
- Maven 3.8+

### Installation and Running:

```bash
# 1. Clone the repository
git clone <repository-url>
cd Automated_Timetabling_System_Backend

# 2. Set up MySQL database
mysql -u root -p
CREATE DATABASE timetable_db;

# 3. (Optional) Configure environment variables
export MYSQL_IP=localhost
export JWT_SECRET=your_secret_key_here

# 4. Build the project
./mvnw clean install

# 5. Run the application
./mvnw spring-boot:run
```

Application will be available at: `http://localhost:8080`

---

## 🔌 API Endpoints

### Authentication
- `POST /api/auth/register` - Student registration
- `POST /api/auth/login` - Login

### Dictionaries (ADMIN only)
- `GET /api/dictionaries/rooms` - List of rooms
- `POST /api/dictionaries/rooms` - Create room
- `GET /api/dictionaries/styles` - List of dance styles
- `POST /api/dictionaries/styles` - Create dance style

### Teachers (ADMIN only)
- `POST /api/teachers` - Create teacher

### Solver
- `POST /api/solver/solve/{scheduleId}` - Start optimization
- `GET /api/solver/status/{scheduleId}` - Solution status
- `POST /api/solver/terminate/{scheduleId}` - Stop solving

📖 **Full API documentation:** (Swagger UI will be added later)

---

## 🧪 Testing

```bash
# Run all tests
./mvnw test

# Run with coverage report
./mvnw test jacoco:report
```

**Current test coverage:** High (all main components covered)

---

## 🗃️ Database

### Flyway Migrations:
- **V1__init.sql** - User tables creation (users, teachers, students, admins, roles)
- **V2__dictionaries.sql** - Dictionaries (dance_styles, rooms, teacher_dance_style)
- **V3__solver_entities.sql** - Solver entities (timeslots, dance_groups, lessons, resource_unavailability)

### Total tables: 13

---

## 🏗️ Architecture

### Layered Architecture

```
Controller → Service → Repository → Database
     ↓          ↓
    DTO    Entity (Domain Model)
```

### Key Principles:
- **DTO Pattern** - never return entities directly
- **Constructor Injection** - via @RequiredArgsConstructor
- **Stateless API** - JWT without sessions
- **RBAC** - Role-Based Access Control

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

---

## 📖 Additional Resources

- [Timefold Solver Documentation](https://docs.timefold.ai/)
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [MapStruct Documentation](https://mapstruct.org/documentation/stable/reference/html/)

---

*Made with ❤️ for dance schools*

