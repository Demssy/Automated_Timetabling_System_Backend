# Детальный Анализ EPIC 3: Solver MVP & Constraint Engine

## 📊 Общая Информация

### Название
**EPIC 3: Solver MVP & Constraint Engine (Движок оптимизации)**

### Цель Epic'а
Реализация **MVP (Minimum Viable Product)** движка планирования расписания на базе **Timefold Solver** (форк OptaPlanner). Это **ключевая функциональность** всего проекта - автоматическая генерация оптимального расписания с учётом ограничений.

### Архитектурный Паттерн
**Async Execution Pattern** - асинхронное выполнение через `SolverManager`

### Контекст
- **Технология**: Spring Boot 3 + Java 21 + Timefold Solver
- **API Contract**: Строго использовать DTOs, **НИКОГДА** не возвращать `@PlanningEntity` или `@PlanningSolution` напрямую

---

## 🎯 Функциональное Описание

### Что нужно реализовать:

1. **Конфигурацию Timefold Solver**
   - Настройка времени выполнения (60 секунд)
   - Выбор алгоритмов оптимизации (Late Acceptance / Tabu Search)

2. **Математические Ограничения (Constraints)**
   - **Hard Constraints** - жёсткие правила, которые НЕЛЬЗЯ нарушать
   - **Soft Constraints** - мягкие правила, желательные к соблюдению

3. **Асинхронный API**
   - Запуск процесса оптимизации в фоне
   - Мониторинг статуса решения
   - Сохранение результатов в БД

### Бизнес-Задача
Обеспечить **автоматическую генерацию расписания**, которое:
- ✅ Соблюдает физические ограничения (один учитель не может быть в двух местах одновременно)
- ✅ Учитывает доступность ресурсов (залы, учителя)
- ✅ Оптимизирует использование времени (минимизирует окна между уроками)

---

## 📋 Технические Задачи (Детальный Разбор)

### [BE-10] Конфигурация Timefold Solver ✅ ЗАВЕРШЕНО

#### Что было сделано:

**1. Добавлены зависимости в pom.xml**
```xml
<dependency>
    <groupId>ai.timefold.solver</groupId>
    <artifactId>timefold-solver-spring-boot-starter</artifactId>
    <version>1.6.0</version>
</dependency>

<dependency>
    <groupId>ai.timefold.solver</groupId>
    <artifactId>timefold-solver-test</artifactId>
    <version>1.6.0</version>
    <scope>test</scope>
</dependency>
```

**2. Создан класс DanceSchedule (@PlanningSolution)**
```java
@PlanningSolution
public class DanceSchedule {
    @PlanningId
    private Long id;
    
    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "timeslotRange")
    private List<Timeslot> timeslotList;
    
    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "roomRange")
    private List<Room> roomList;
    
    @ProblemFactCollectionProperty
    private List<Teacher> teacherList;
    
    @PlanningEntityCollectionProperty
    private List<Lesson> lessonList;
    
    @PlanningScore
    private HardSoftScore score;
}
```

**3. Создана Entity Lesson (@PlanningEntity)**
```java
@Entity
@PlanningEntity
public class Lesson {
    @Id
    @PlanningId
    private Long id;
    
    @ManyToOne
    private Teacher teacher;
    
    @ManyToOne
    private DanceGroup group;
    
    @PlanningVariable(valueRangeProviderRefs = "timeslotRange")
    @ManyToOne
    private Timeslot timeslot;
    
    @PlanningVariable(valueRangeProviderRefs = "roomRange")
    @ManyToOne
    private Room room;
    
    private int durationMinutes;
    
    @PlanningPin
    private boolean pinned;
    
    private boolean isPrivate;  // Для Dual-Mode логики
}
```

**4. Создана Entity Timeslot (@ProblemFact)**
```java
@Entity
public class Timeslot {
    @Id
    private Long id;
    
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
}
```

**5. Создана Entity DanceGroup**
```java
@Entity
public class DanceGroup {
    @Id
    private Long id;
    
    private String name;
    
    @ManyToOne
    private DanceStyle danceStyle;
    
    @Enumerated(EnumType.STRING)
    private DanceLevel danceLevel;
    
    private Integer minSize;
    private String targetAgeRange;
}
```

**6. Создана Entity ResourceUnavailability**
```java
@Entity
public class ResourceUnavailability {
    @Id
    private Long id;
    
    @ManyToOne
    private Teacher teacher;
    
    @ManyToOne
    private Timeslot timeslot;
    
    private String reason;
}
```

**7. Настроен application.properties**
```properties
timefold.solver.termination.spent-limit=60s
timefold.solver.environment-mode=REPRODUCIBLE
```

**8. Создана Flyway миграция V3**
```sql
CREATE TABLE timeslots (...);
CREATE TABLE dance_groups (...);
CREATE TABLE lessons (...);
CREATE TABLE resource_unavailability (...);
```

**9. Созданы Repositories**
- TimeslotRepository
- DanceGroupRepository
- LessonRepository
- ResourceUnavailabilityRepository

---

### [BE-11] Реализация Constraints (ConstraintProvider) ⏳ СЛЕДУЮЩАЯ ЗАДАЧА

#### Что нужно создать:
**Класс `DanceScheduleConstraintProvider`** в пакете `solver`

#### Структура класса:
```java
package com.timetable.backend.solver;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;

public class DanceScheduleConstraintProvider implements ConstraintProvider {
    
    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
            // Hard constraints
            roomConflict(constraintFactory),
            teacherConflict(constraintFactory),
            teacherAvailability(constraintFactory),
            
            // Soft constraints
            minimizeGaps(constraintFactory)
        };
    }
    
    // Реализация каждого constraint...
}
```

#### HARD CONSTRAINT 1: Room Conflict

**Описание**: Два урока не могут быть в одной комнате в одно время

**Особенность "Dual-Mode"**: 
- Если оба урока **PRIVATE** (частные) И комната позволяет параллельные частные уроки (`allowsParallelPrivate=true`), то конфликта нет
- В противном случае - штраф

**Реализация**:
```java
Constraint roomConflict(ConstraintFactory constraintFactory) {
    return constraintFactory
        .forEach(Lesson.class)
        .join(Lesson.class,
            // Разные уроки
            Joiners.lessThan(Lesson::getId),
            // Одна и та же комната
            Joiners.equal(Lesson::getRoom),
            // Одно и то же время
            Joiners.equal(Lesson::getTimeslot)
        )
        // Фильтр: если оба частных и комната позволяет - пропускаем
        .filter((lesson1, lesson2) -> {
            Room room = lesson1.getRoom();
            boolean bothPrivate = lesson1.isPrivate() && lesson2.isPrivate();
            return !(bothPrivate && room.isAllowsParallelPrivate());
        })
        .penalize(HardSoftScore.ONE_HARD)
        .asConstraint("Room conflict");
}
```

#### HARD CONSTRAINT 2: Teacher Conflict

**Описание**: Учитель не может вести два урока одновременно

**Реализация**:
```java
Constraint teacherConflict(ConstraintFactory constraintFactory) {
    return constraintFactory
        .forEach(Lesson.class)
        .join(Lesson.class,
            Joiners.lessThan(Lesson::getId),
            Joiners.equal(Lesson::getTeacher),  // Один учитель
            Joiners.equal(Lesson::getTimeslot)  // Одно время
        )
        .penalize(HardSoftScore.ONE_HARD)
        .asConstraint("Teacher conflict");
}
```

#### HARD CONSTRAINT 3: Teacher Availability

**Описание**: Урок не может быть назначен на время, когда учитель недоступен

**Реализация constraint**:
```java
Constraint teacherAvailability(ConstraintFactory constraintFactory) {
    return constraintFactory
        .forEach(Lesson.class)
        .join(ResourceUnavailability.class,
            Joiners.equal(Lesson::getTeacher, ResourceUnavailability::getTeacher),
            Joiners.equal(Lesson::getTimeslot, ResourceUnavailability::getTimeslot)
        )
        .penalize(HardSoftScore.ONE_HARD)
        .asConstraint("Teacher unavailability");
}
```

#### SOFT CONSTRAINT: Minimize Gaps

**Описание**: Минимизировать окна (разрывы) между уроками одного учителя в один день

**Идея**: Лучше если уроки идут подряд (09:00-10:00, 10:00-11:00), чем с разрывами (09:00-10:00, 12:00-13:00)

**Реализация** (упрощённая):
```java
Constraint minimizeGaps(ConstraintFactory constraintFactory) {
    return constraintFactory
        .forEach(Lesson.class)
        .join(Lesson.class,
            Joiners.equal(Lesson::getTeacher),  // Один учитель
            Joiners.equal(lesson -> lesson.getTimeslot().getDayOfWeek())  // Один день
        )
        .filter((lesson1, lesson2) -> {
            // Проверяем, есть ли разрыв между уроками
            LocalTime end1 = lesson1.getTimeslot().getEndTime();
            LocalTime start2 = lesson2.getTimeslot().getStartTime();
            return end1.isBefore(start2) && 
                   Duration.between(end1, start2).toMinutes() > 0;
        })
        .penalize(HardSoftScore.ONE_SOFT, (lesson1, lesson2) -> {
            // Штраф = количество минут разрыва
            LocalTime end1 = lesson1.getTimeslot().getEndTime();
            LocalTime start2 = lesson2.getTimeslot().getStartTime();
            return (int) Duration.between(end1, start2).toMinutes();
        })
        .asConstraint("Minimize teacher gaps");
}
```

#### Итоговая Структура Constraints:
- ✅ **3 Hard Constraints** (HardScore должен быть 0 для валидного решения)
- ✅ **1+ Soft Constraints** (SoftScore - чем больше, тем лучше)

---

### [BE-12] Solver Service & Manager (Async Logic) ⏳ СЛЕДУЮЩАЯ

#### Что нужно создать:
**Класс `SolverService`** в пакете `service`

#### Основные Компоненты:

**1. Внедрение SolverManager**:
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class SolverService {
    
    private final SolverManager<DanceSchedule, Long> solverManager;
    
    // Repositories для загрузки данных
    private final LessonRepository lessonRepository;
    private final TimeslotRepository timeslotRepository;
    private final RoomRepository roomRepository;
    private final TeacherRepository teacherRepository;
}
```

**2. Метод `solve(Long scheduleId)`**:
```java
public void solve(Long scheduleId) {
    log.info("Starting solve for schedule ID: {}", scheduleId);
    
    // 1. Загрузить проблему из БД
    DanceSchedule problem = loadProblem(scheduleId);
    
    // 2. Запустить Solver асинхронно
    solverManager.solveBuilder()
        .withProblemId(scheduleId)
        .withProblemFinder(id -> problem)
        .withBestSolutionConsumer(this::saveSolution)
        .withExceptionHandler((problemId, exception) -> {
            log.error("Error solving schedule {}", problemId, exception);
        })
        .run();  // НЕблокирующий вызов!
    
    log.info("Solver started for schedule ID: {}", scheduleId);
}
```

**3. Метод загрузки проблемы**:
```java
private DanceSchedule loadProblem(Long scheduleId) {
    DanceSchedule schedule = new DanceSchedule();
    schedule.setId(scheduleId);
    
    schedule.setTimeslotList(timeslotRepository.findAll());
    schedule.setRoomList(roomRepository.findAll());
    schedule.setTeacherList(teacherRepository.findAll());
    
    List<Lesson> lessons = lessonRepository.findAll();
    
    // Очищаем planning variables для незафиксированных уроков
    lessons.forEach(lesson -> {
        if (!lesson.isPinned()) {
            lesson.setTimeslot(null);
            lesson.setRoom(null);
        }
    });
    
    schedule.setLessonList(lessons);
    
    return schedule;
}
```

**4. Метод сохранения решения**:
```java
@Transactional
private void saveSolution(DanceSchedule solution) {
    log.info("Saving solution for schedule ID: {}, score: {}", 
             solution.getId(), solution.getScore());
    
    for (Lesson lesson : solution.getLessonList()) {
        lessonRepository.save(lesson);
    }
}
```

**5. Метод проверки статуса**:
```java
public SolverStatus getSolverStatus(Long scheduleId) {
    return solverManager.getSolverStatus(scheduleId);
}
```

**6. Метод остановки решения**:
```java
public void terminateEarly(Long scheduleId) {
    solverManager.terminateEarly(scheduleId);
    log.info("Solver terminated early for schedule ID: {}", scheduleId);
}
```

---

### [BE-13] Unit Tests for Constraints ⏳ СЛЕДУЮЩАЯ

#### Создать Тестовый Класс:
**`DanceScheduleConstraintProviderTest`** в `src/test/java/.../solver/`

```java
@SpringBootTest
class DanceScheduleConstraintProviderTest {
    
    private ConstraintVerifier<DanceScheduleConstraintProvider, DanceSchedule> constraintVerifier;
    
    @BeforeEach
    void setUp() {
        constraintVerifier = ConstraintVerifier.build(
            new DanceScheduleConstraintProvider(),
            DanceSchedule.class,
            Lesson.class
        );
    }
    
    @Test
    void penaltyForRoomConflict() {
        // Given: Два урока в одной комнате в одно время
        Room room = new Room(1L, "Hall A", 20, false);
        Timeslot timeslot = new Timeslot(1L, DayOfWeek.MONDAY, 
                                         LocalTime.of(9, 0), LocalTime.of(10, 0));
        
        Lesson lesson1 = new Lesson(...);
        Lesson lesson2 = new Lesson(...);
        
        // When/Then: Должен быть штраф 1 HARD
        constraintVerifier.verifyThat(DanceScheduleConstraintProvider::roomConflict)
            .given(lesson1, lesson2)
            .penalizesBy(1);
    }
    
    @Test
    void noPenaltyForDualModePrivateLessons() {
        // Given: Два частных урока в комнате с allowsParallelPrivate=true
        // When/Then: НЕ должно быть штрафа
    }
    
    @Test
    void penaltyForTeacherConflict() {
        // Given: Учитель ведёт два урока одновременно
        // When/Then: Должен быть штраф 1 HARD
    }
}
```

---

### [BE-14] Solver Controller & DTO ⏳ СЛЕДУЮЩАЯ

#### Создать Controller:
**`SolverController`** в пакете `controller`

```java
@RestController
@RequestMapping("/api/solver")
@RequiredArgsConstructor
@Slf4j
public class SolverController {
    
    private final SolverService solverService;
    
    @PostMapping("/solve/{scheduleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SolveResponse> solve(@PathVariable Long scheduleId) {
        SolverStatus currentStatus = solverService.getSolverStatus(scheduleId);
        if (currentStatus == SolverStatus.SOLVING_ACTIVE) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new SolveResponse(null, "Solver already running", currentStatus));
        }
        
        solverService.solve(scheduleId);
        
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(new SolveResponse(
                scheduleId.toString(),
                "Solving started",
                SolverStatus.SOLVING_ACTIVE
            ));
    }
    
    @GetMapping("/status/{scheduleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SolverStatusResponse> getStatus(@PathVariable Long scheduleId) {
        SolverStatus status = solverService.getSolverStatus(scheduleId);
        return ResponseEntity.ok(new SolverStatusResponse(scheduleId, status, "..."));
    }
    
    @PostMapping("/terminate/{scheduleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> terminate(@PathVariable Long scheduleId) {
        solverService.terminateEarly(scheduleId);
        return ResponseEntity.ok().build();
    }
}
```

#### DTOs:

**SolveResponse.java**:
```java
public record SolveResponse(
    String jobId,
    String message,
    SolverStatus status
) {}
```

**SolverStatusResponse.java**:
```java
public record SolverStatusResponse(
    Long scheduleId,
    SolverStatus status,
    String message
) {}
```

#### API Contract:

**POST /api/solver/solve/{scheduleId}**
- Response: `202 Accepted` + JSON: `{"jobId": "123", "message": "Solving started", "status": "SOLVING_ACTIVE"}`
- Error: `409 Conflict` если solver уже запущен

**GET /api/solver/status/{scheduleId}**
- Response: `200 OK` + JSON: `{"scheduleId": 123, "status": "NOT_SOLVING", "message": "Idle"}`

**POST /api/solver/terminate/{scheduleId}**
- Response: `200 OK`

---

## 👤 User Stories & Acceptance Criteria

### Story 1: Избежание конфликтов

**Как** Администратор,  
**Я хочу**, чтобы система автоматически проверяла расписание на физические конфликты (двойная запись зала, учителя),  
**Чтобы** исключить человеческие ошибки при планировании.

#### Acceptance Criteria:
✅ **Сценарий 1**: Конфликтные данные
- **Given**: В БД есть 3 урока, назначенные в один слот в одну комнату
- **When**: Запускаем solver (`POST /api/solver/solve/1`)
- **Then**: 
  - Solver разносит уроки по разным временным слотам или комнатам
  - Итоговый **Hard Score = 0** (нет нарушений жёстких правил)

✅ **Сценарий 2**: Dual-Mode
- **Given**: 2 частных урока в комнате с `allowsParallelPrivate=true`
- **When**: Solver работает
- **Then**: Эти уроки МОГУТ быть в одном timeslot в одной room

✅ **Сценарий 3**: Недоступность учителя
- **Given**: Учитель недоступен в понедельник 9:00-10:00
- **When**: Solver работает
- **Then**: Ни один урок этого учителя не назначен на понедельник 9:00-10:00

---

### Story 2: Запуск оптимизации

**Как** Администратор,  
**Я хочу** запускать процесс пересчёта расписания одной кнопкой,  
**Чтобы** не ждать 60 секунд перед заблокированным экраном.

#### Acceptance Criteria:
✅ **Сценарий 1**: Асинхронный запуск
- **When**: Отправляет `POST /api/solver/solve/1`
- **Then**: API возвращает **HTTP 202 Accepted** мгновенно (< 100ms)

✅ **Сценарий 2**: Автосохранение результата
- **When**: Прошло 60 секунд
- **Then**: Результаты автоматически сохранены в БД

---

## 📊 Архитектурная Диаграмма Solver Flow

```
┌──────────────────────────────────────────────────────────────┐
│  1. Admin нажимает "Generate Schedule"                       │
│     → POST /api/solver/solve/1                               │
└────────────┬─────────────────────────────────────────────────┘
             │
             ▼
┌──────────────────────────────────────────────────────────────┐
│  2. SolverController                                         │
│     - Проверяет статус (не запущен ли уже)                   │
│     - Вызывает solverService.solve(1)                        │
│     - Возвращает 202 Accepted + jobId                        │
└────────────┬─────────────────────────────────────────────────┘
             │
             ▼
┌──────────────────────────────────────────────────────────────┐
│  3. SolverService.solve(1)                                   │
│     - Загружает данные из БД                                 │
│     - Создаёт DanceSchedule problem                          │
│     - Запускает solverManager.solveBuilder().run()           │
│     → НЕБЛОКИРУЮЩИЙ вызов!                                   │
└────────────┬─────────────────────────────────────────────────┘
             │
             ▼
┌──────────────────────────────────────────────────────────────┐
│  4. SolverManager (Timefold)                                 │
│     - Запускает отдельный поток                              │
│     - Применяет DanceScheduleConstraintProvider              │
│     - Оптимизирует в течение 60 секунд                       │
│     - Вызывает callback (withBestSolutionConsumer)           │
└────────────┬─────────────────────────────────────────────────┘
             │
             ▼
┌──────────────────────────────────────────────────────────────┐
│  5. SolverService.saveSolution(solution)                     │
│     - Сохраняет обновлённые lessons в БД                     │
│     - Логирует результат                                     │
└──────────────────────────────────────────────────────────────┘
```

---

## ✅ Чек-лист Реализации EPIC 3

### Подготовка (Setup)
- [x] Добавить зависимости Timefold в pom.xml
- [x] Создать Flyway миграцию V3
- [x] Настроить application.properties

### Domain Model
- [x] Создать Entity Timeslot
- [x] Создать Entity DanceGroup
- [x] Создать Entity Lesson (@PlanningEntity)
- [x] Создать Entity ResourceUnavailability

### Solver Core
- [x] Создать класс DanceSchedule (@PlanningSolution)
- [ ] Создать DanceScheduleConstraintProvider
- [ ] Создать SolverService

### API Layer
- [ ] Создать SolverController
- [ ] Создать DTOs: SolveResponse, SolverStatusResponse

### Testing
- [ ] Написать DanceScheduleConstraintProviderTest
- [ ] Интеграционный тест для SolverController

---

## 🚨 Критические Моменты

### 1. **Dual-Mode Logic**
Два частных урока МОГУТ быть в одной комнате одновременно, если `room.allowsParallelPrivate=true`

### 2. **Pinned Lessons**
Solver не должен трогать уроки с `pinned=true`

### 3. **Асинхронность**
`solverManager.solveBuilder().run()` НЕ блокирует поток

### 4. **Score Calculation**
- **HardScore < 0** = решение невалидно
- **HardScore = 0** = решение валидно
- **SoftScore** = качество решения

---

## 🎯 Итоговая Оценка

**EPIC 3 - это ЯДРО всего проекта**.

**Сложность**: Высокая  
**Приоритет**: Критический  
**Прогресс**: 25% (BE-10 завершена)  
**Следующий шаг**: BE-11 (ConstraintProvider)

---

*Дата анализа: 31 декабря 2025*  
*Статус: В активной разработке*

