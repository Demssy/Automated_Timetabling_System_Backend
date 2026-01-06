# Timefold Architecture Refactoring - Planning Model Separation

## 🎯 Цель рефакторинга

Разделение **Persistence Model** (JPA) и **Planning Model** (Timefold) для устранения критических проблем производительности:

- ❌ **Было:** JPA entities с `@Entity` + `@PlanningEntity` → Heavy cloning, LazyInitializationException, N+1 queries
- ✅ **Стало:** Отдельные Planning POJOs → Zero Hibernate overhead, fast cloning, no DB access during solving

---

## 📁 Новая структура

### Planning Domain Package: `com.timetable.backend.solver.domain`

Чистые POJO классы для Timefold Solver (БЕЗ JPA):

1. **`PlanningLesson.java`** — Planning Entity (заменяет Lesson в solver)
   - `@PlanningEntity`, `@PlanningVariable`, `@PlanningId`, `@PlanningPin`
   - Содержит только необходимые для решения поля

2. **`TimetableSolution.java`** — Planning Solution (заменяет DanceSchedule в solver)
   - `@PlanningSolution`, `@PlanningScore`
   - Содержит списки Planning POJOs

3. **Planning Facts (Problem Facts):**
   - `PlanningTimeslot.java`
   - `PlanningRoom.java`
   - `PlanningTeacher.java`
   - `PlanningDanceGroup.java`
   - `PlanningResourceUnavailability.java`

### Mapper Package: `com.timetable.backend.solver.mapper`

**`PlanningModelMapper.java`** — конвертер между JPA ↔ Planning:

```java
// JPA → Planning (для передачи в Solver)
TimetableSolution toPlanningSolution(
    Long scheduleId,
    List<Lesson> lessons,
    List<Timeslot> timeslots,
    List<Room> rooms,
    List<Teacher> teachers,
    List<ResourceUnavailability> unavailabilities
)

// Planning → JPA updates (для сохранения результата)
List<LessonUpdate> toPersistableLessons(
    TimetableSolution solution,
    Map<Long, Timeslot> timeslotMap,
    Map<Long, Room> roomMap
)
```

---

## ✅ Что изменилось

### 1. JPA Entity `Lesson.java`

**Удалены Timefold аннотации:**
```diff
- import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
- import ai.timefold.solver.core.api.domain.entity.PlanningPin;
- import ai.timefold.solver.core.api.domain.lookup.PlanningId;
- import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

- @PlanningEntity
- @PlanningId
- @PlanningVariable(valueRangeProviderRefs = "timeslotRange")
- @PlanningVariable(valueRangeProviderRefs = "roomRange")
- @PlanningPin
```

Теперь это **чистый JPA entity** для persistence.

### 2. Constraint Provider `DanceScheduleConstraintProvider.java`

**Обновлены импорты и типы:**
```diff
- import com.timetable.backend.domain.model.Lesson;
- import com.timetable.backend.domain.model.ResourceUnavailability;
+ import com.timetable.backend.solver.domain.PlanningLesson;
+ import com.timetable.backend.solver.domain.PlanningResourceUnavailability;

- .forEach(Lesson.class)
+ .forEach(PlanningLesson.class)
```

Все constraints теперь работают с Planning POJOs.

### 3. Solver Configuration

**Требуется обновить `SolverService` для использования:**
- `TimetableSolution` вместо `DanceSchedule`
- `PlanningModelMapper` для конвертации данных

---

## 🚀 Преимущества новой архитектуры

| Аспект | До рефакторинга | После рефакторинга |
|--------|-----------------|-------------------|
| **Cloning** | Тяжёлое (Hibernate proxies) | Лёгкое (Pure POJOs) |
| **DB Access** | Риск N+1 queries в constraints | 0 запросов во время solving |
| **LazyInit** | Риск LazyInitializationException | Невозможен (нет lazy loading) |
| **Performance** | Медленно под нагрузкой | Быстро (lightweight objects) |
| **Separation of Concerns** | Смешаны JPA + Solver | Чистое разделение |

---

## 📝 Следующие шаги (TODO)

### 1. Обновить `SolverService.java`

```java
@Service
@RequiredArgsConstructor
public class SolverService {
    
    private final PlanningModelMapper planningMapper;
    private final SolverManager<TimetableSolution, Long> solverManager;
    
    public void solve(Long scheduleId) {
        // 1. Load JPA entities from DB
        var lessons = lessonRepository.findAll();
        var timeslots = timeslotRepository.findAll();
        var rooms = roomRepository.findAll();
        var teachers = teacherRepository.findAll();
        var unavailabilities = unavailabilityRepository.findAll();
        
        // 2. Convert to Planning Model
        TimetableSolution problem = planningMapper.toPlanningSolution(
            scheduleId, lessons, timeslots, rooms, teachers, unavailabilities
        );
        
        // 3. Solve (NO DB ACCESS during this phase)
        solverManager.solve(scheduleId, problem);
    }
    
    // BestSolutionChangedEvent listener
    public void onBestSolutionChanged(BestSolutionChangedEvent<TimetableSolution> event) {
        TimetableSolution solution = event.getNewBestSolution();
        
        // 4. Convert back to JPA updates
        var updates = planningMapper.toPersistableLessons(solution, ...);
        
        // 5. Persist to DB
        updates.forEach(update -> {
            Lesson lesson = lessonRepository.findById(update.lessonId()).orElseThrow();
            lesson.setTimeslot(timeslotRepository.findById(update.timeslotId()).orElse(null));
            lesson.setRoom(roomRepository.findById(update.roomId()).orElse(null));
        });
        lessonRepository.saveAll(...);
    }
}
```

### 2. Обновить `solverConfig.xml` (если есть)

```xml
<solver xmlns="https://timefold.ai/xsd/solver" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="https://timefold.ai/xsd/solver https://timefold.ai/xsd/solver/solver.xsd">
    <solutionClass>com.timetable.backend.solver.domain.TimetableSolution</solutionClass>
    <entityClass>com.timetable.backend.solver.domain.PlanningLesson</entityClass>
    <!-- ... -->
</solver>
```

### 3. Обновить тесты

- `DanceScheduleConstraintProviderTest` → использовать `PlanningLesson` вместо `Lesson`
- `SolverServiceTest` → мокать `PlanningModelMapper`

---

## ⚠️ Breaking Changes

Все места, где используется `DanceSchedule` с Solver, нужно обновить на `TimetableSolution`:

```diff
- SolverManager<DanceSchedule, Long> solverManager;
+ SolverManager<TimetableSolution, Long> solverManager;

- SolverJob<DanceSchedule, Long> solverJob;
+ SolverJob<TimetableSolution, Long> solverJob;
```

---

## 🔍 Проверка корректности

После полного внедрения:

1. **Компиляция:** `mvn clean compile` — должно пройти без ошибок
2. **Тесты constraints:** `DanceScheduleConstraintProviderTest` должны пройти
3. **Integration test:** запустить solve и убедиться, что нет DB-запросов во время solving
4. **Performance test:** сравнить скорость cloning (должно быть в разы быстрее)

---

## 📚 Документация

- [Timefold Best Practices - Domain Model Separation](https://docs.timefold.ai/timefold-solver/latest/optimization-algorithms/optimization-algorithms#domainModelingSeparation)
- [Avoiding LazyInitializationException](https://docs.timefold.ai/timefold-solver/latest/integration/integration#integratingWithJpa)

---

## ✅ Рефакторинг завершён для:

- ✅ Planning Domain POJOs созданы
- ✅ `PlanningModelMapper` реализован
- ✅ `DanceScheduleConstraintProvider` обновлён
- ✅ JPA `Lesson` entity очищен от Timefold аннотаций
- ✅ `SolverService` интегрирован с Planning Model
- ✅ `SolverController` обновлён для работы с TimetableSolution
- ✅ **BUILD SUCCESS** — проект компилируется

**Следующий этап:** Обновление тестов (требует отдельного PR/commit)

