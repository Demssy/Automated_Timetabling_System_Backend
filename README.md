# 🎵 Automated Timetabling System Backend

Система автоматического составления расписания для танцевальной школы на базе **Timefold Solver**.

---

## 🚀 О Проекте

**Automated Timetabling System** - это REST API для автоматической генерации оптимального расписания уроков танцевальной школы с учётом множества ограничений (доступность учителей, вместимость залов, уровни групп и т.д.).

### Ключевые Возможности:
- ✅ **Автоматическое составление расписания** с помощью Timefold Solver
- ✅ **Dual-Mode логика** - возможность параллельных частных уроков в одном зале
- ✅ **JWT Authentication** - безопасная аутентификация
- ✅ **Role-Based Access Control** - разграничение прав (Admin, Teacher, Student)
- ✅ **RESTful API** - чистый API дизайн с DTOs

---

## 🛠️ Технологический Стек

| Компонент | Технология | Версия |
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

## 📦 Структура Проекта

```
src/
├── main/
│   ├── java/com/timetable/backend/
│   │   ├── config/              # Конфигурация Spring
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
│       ├── db/migration/        # Flyway SQL миграции
│       └── application.properties
└── test/                        # Unit и Integration тесты
```

---

## 📚 Документация

**Вся техническая документация находится в папке [`docs/`](./docs/)**

### Основные Документы:
- 📖 [**Навигация по документации**](./docs/README.md) - начни отсюда!
- 📊 [**Анализ проекта**](./docs/analysis/project_analysis.md) - что уже реализовано
- 📋 [**Отчёты о задачах**](./docs/reports/) - детальные отчёты по каждой задаче
- 🎯 [**EPIC 3 План**](./docs/epic3/epic3_detailed_analysis.md) - детальный план реализации Solver

---

## 🚦 Статус Разработки

### ✅ Завершённые Epic'и:

#### EPIC 2: Domain Core & Security
- ✅ JPA Entities (User hierarchy с JOINED Inheritance)
- ✅ Spring Security 6 + JWT
- ✅ Flyway Migrations (V1, V2, V3)
- ✅ CRUD API для справочников (Rooms, Dance Styles)
- ✅ Teacher Management API
- ✅ Student Registration
- ✅ MapStruct Mappers
- ✅ Comprehensive Testing

### 🔄 В Процессе:

#### EPIC 3: Solver MVP & Constraint Engine (50% готово)
- ✅ [BE-10] Конфигурация Timefold Solver - **ЗАВЕРШЕНО**
  - ✅ Entity: Timeslot, DanceGroup, Lesson, ResourceUnavailability
  - ✅ PlanningSolution: DanceSchedule
  - ✅ Flyway Migration V3
  - ✅ Repositories
- ⏳ [BE-11] DanceScheduleConstraintProvider - **В очереди**
- ⏳ [BE-12] SolverService (асинхронный) - **В очереди**
- ⏳ [BE-13] Unit-тесты для constraints - **В очереди**
- ⏳ [BE-14] SolverController + REST API - **В очереди**

---

## 🏃 Быстрый Старт

### Требования:
- Java 21+
- MySQL 8.0
- Maven 3.8+

### Установка и Запуск:

```bash
# 1. Клонировать репозиторий
git clone <repository-url>
cd Automated_Timetabling_System_Backend

# 2. Настроить базу данных MySQL
mysql -u root -p
CREATE DATABASE timetable_db;

# 3. (Опционально) Настроить переменные окружения
export MYSQL_IP=localhost
export JWT_SECRET=your_secret_key_here

# 4. Собрать проект
./mvnw clean install

# 5. Запустить приложение
./mvnw spring-boot:run
```

Приложение будет доступно по адресу: `http://localhost:8080`

---

## 🔌 API Endpoints

### Authentication
- `POST /api/auth/register` - Регистрация студента
- `POST /api/auth/login` - Вход в систему

### Dictionaries (ADMIN only)
- `GET /api/dictionaries/rooms` - Список залов
- `POST /api/dictionaries/rooms` - Создать зал
- `GET /api/dictionaries/styles` - Список стилей танцев
- `POST /api/dictionaries/styles` - Создать стиль

### Teachers (ADMIN only)
- `POST /api/teachers` - Создать учителя

### Solver (В разработке)
- `POST /api/solver/solve/{scheduleId}` - Запустить оптимизацию
- `GET /api/solver/status/{scheduleId}` - Статус решения
- `POST /api/solver/terminate/{scheduleId}` - Остановить решение

📖 **Полная API документация:** (Swagger UI будет добавлен позже)

---

## 🧪 Тестирование

```bash
# Запуск всех тестов
./mvnw test

# Запуск с отчётом о покрытии
./mvnw test jacoco:report
```

**Текущее покрытие тестами:** Высокое (все основные компоненты)

---

## 🗃️ База Данных

### Flyway Миграции:
- **V1__init.sql** - Создание таблиц пользователей (users, teachers, students, admins, roles)
- **V2__dictionaries.sql** - Справочники (dance_styles, rooms, teacher_dance_style)
- **V3__solver_entities.sql** - Сущности для Solver (timeslots, dance_groups, lessons, resource_unavailability)

### Всего таблиц: 13

---

## 🏗️ Архитектура

### Layered Architecture (Слоистая архитектура)

```
Controller → Service → Repository → Database
     ↓          ↓
    DTO    Entity (Domain Model)
```

### Ключевые принципы:
- **DTO Pattern** - никогда не возвращать Entity напрямую
- **Constructor Injection** - через @RequiredArgsConstructor
- **Stateless API** - JWT без сессий
- **RBAC** - Role-Based Access Control

---

## 🔐 Безопасность

- **Argon2** для хеширования паролей (самый безопасный алгоритм)
- **JWT** с подписью HS256
- **HTTP-only cookies** для токенов
- **CORS** настроен
- **Role-Based Access Control** (ADMIN, TEACHER, STUDENT)

⚠️ **Важно для production:**
- Изменить `jwt.secret` на криптостойкий ключ
- Включить HTTPS (`cookieSecure=true`)
- Настроить конкретные CORS origins

---

## 🤝 Contributing

Проект находится в активной разработке. Contribution guidelines будут добавлены позже.

---

## 📝 Лицензия

(Будет добавлена)

---

## 👥 Контакты

**Проект:** Automated Timetabling System Backend  
**Версия:** 0.0.1-SNAPSHOT  
**Дата старта:** 31 декабря 2025  

---

## 📖 Дополнительные Ресурсы

- [Timefold Solver Documentation](https://docs.timefold.ai/)
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [MapStruct Documentation](https://mapstruct.org/documentation/stable/reference/html/)

---

*Сделано с ❤️ для танцевальных школ*

