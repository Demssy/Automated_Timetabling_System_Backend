# 🏗️ Архитектура разделения моделей (Planning Model Separation)

## 📊 Визуальная диаграмма

```
┌─────────────────────────────────────────────────────────────────────┐
│                        CLIENT REQUEST                                │
│                  POST /api/v1/solver/solve                          │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      CONTROLLER LAYER                                │
│                    SolverController.java                             │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                       SERVICE LAYER                                  │
│                    SolverService.java                                │
│                                                                       │
│  Step 1: Load JPA Entities from Database                            │
│  ┌────────────────────────────────────────┐                         │
│  │ lessonRepository.findAll()              │                         │
│  │ → List<Lesson> (JPA @Entity)           │                         │
│  │                                         │                         │
│  │ timeslotRepository.findAll()            │                         │
│  │ → List<Timeslot> (JPA @Entity)         │                         │
│  └────────────────────────────────────────┘                         │
│                     │                                                 │
│                     ▼                                                 │
│  Step 2: Convert to Planning Model (via Mapper)                     │
│  ┌────────────────────────────────────────┐                         │
│  │ PlanningModelMapper                     │                         │
│  │ .toPlanningSolution(lessons, ...)      │                         │
│  │                                         │                         │
│  │ JPA → Planning POJO conversion         │                         │
│  └────────────────────────────────────────┘                         │
│                     │                                                 │
│                     ▼                                                 │
│  Step 3: Start Solving                                               │
│  ┌────────────────────────────────────────┐                         │
│  │ solverManager.solve(TimetableSolution) │                         │
│  └────────────────────────────────────────┘                         │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      SOLVER LAYER                                    │
│                   Timefold Solver Engine                             │
│                                                                       │
│  Works ONLY with Planning Model (solver/domain)                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  TimetableSolution                                           │   │
│  │  ├── List<PlanningLesson>       (@PlanningEntity)           │   │
│  │  ├── List<PlanningTimeslot>     (Problem Fact)              │   │
│  │  ├── List<PlanningRoom>         (Problem Fact)              │   │
│  │  ├── List<PlanningTeacher>      (Problem Fact)              │   │
│  │  └── HardSoftScore score                                    │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                       │
│  Constraint Provider evaluates Planning POJOs:                       │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  DanceScheduleConstraintProvider.java                        │   │
│  │  - forEach(PlanningLesson.class)                            │   │
│  │  - No JPA, No LazyInitializationException                   │   │
│  │  - Fast cloning (lightweight objects)                       │   │
│  └─────────────────────────────────────────────────────────────┘   │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             ▼ (Solution found)
┌─────────────────────────────────────────────────────────────────────┐
│                       SERVICE LAYER                                  │
│                    SolverService.saveSolution()                      │
│                                                                       │
│  Step 4: Convert Planning Model back to JPA                         │
│  ┌────────────────────────────────────────┐                         │
│  │ PlanningModelMapper                     │                         │
│  │ .toPersistableLessons(solution)        │                         │
│  │                                         │                         │
│  │ Planning POJO → JPA updates            │                         │
│  └────────────────────────────────────────┘                         │
│                     │                                                 │
│                     ▼                                                 │
│  Step 5: Update JPA Entities in Database                            │
│  ┌────────────────────────────────────────┐                         │
│  │ lesson.setTimeslot(timeslot);          │                         │
│  │ lesson.setRoom(room);                  │                         │
│  │ lessonRepository.save(lesson);         │                         │
│  └────────────────────────────────────────┘                         │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 🗂️ Разделение ответственностей

### 📁 `domain/model/` — JPA Persistence Model

**Назначение:** Хранение данных в базе данных

**Технологии:**
- JPA/Hibernate
- `@Entity`, `@Table`, `@ManyToOne`, `@Version`

**Пример: Lesson.java**
```java
package com.timetable.backend.domain.model;

@Entity
@Table(name = "lessons")
public class Lesson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Version
    private Long version; // Optimistic Locking
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timeslot_id")
    private Timeslot timeslot;
    
    // ... JPA magic
}
```

**Используется для:**
- ✅ CRUD операции через Repository
- ✅ Хранение в MySQL
- ✅ Транзакции и Optimistic Locking
- ❌ **НЕ используется** в Timefold Solver!

---

### 📁 `solver/domain/` — Timefold Planning Model

**Назначение:** Оптимизация расписания (solving)

**Технологии:**
- Timefold Solver
- `@PlanningEntity`, `@PlanningVariable`, `@PlanningSolution`

**Пример: PlanningLesson.java**
```java
package com.timetable.backend.solver.domain;

@PlanningEntity
public class PlanningLesson {
    @PlanningId
    private Long id;
    
    // NO @Version, NO @Entity, NO JPA!
    
    private PlanningTeacher teacher; // Simple reference, not @ManyToOne
    
    @PlanningVariable(valueRangeProviderRefs = "timeslotRange")
    private PlanningTimeslot timeslot; // Will be assigned by solver
    
    @PlanningVariable(valueRangeProviderRefs = "roomRange")
    private PlanningRoom room;
    
    // Pure POJO - fast cloning, no Hibernate
}
```

**Используется для:**
- ✅ Timefold Solver optimization
- ✅ Constraint evaluation
- ✅ Fast cloning during search
- ❌ **НЕ сохраняется** в БД напрямую!

---

### 📁 `solver/mapper/` — Conversion Layer

**Назначение:** Конвертация между моделями

**Пример: PlanningModelMapper.java**
```java
package com.timetable.backend.solver.mapper;

@Component
public class PlanningModelMapper {
    
    // JPA → Planning (before solving)
    public TimetableSolution toPlanningSolution(
        List<Lesson> lessons,          // JPA
        List<Timeslot> timeslots,      // JPA
        ...
    ) {
        var planningLessons = lessons.stream()
            .map(this::toPlanningLesson)  // Convert each Lesson → PlanningLesson
            .toList();
        
        return new TimetableSolution(..., planningLessons);
    }
    
    // Planning → JPA updates (after solving)
    public List<LessonUpdate> toPersistableLessons(
        TimetableSolution solution     // Planning Model
    ) {
        return solution.getLessonList().stream()
            .map(planningLesson -> new LessonUpdate(
                planningLesson.getId(),
                planningLesson.getTimeslot().getId(),
                planningLesson.getRoom().getId()
            ))
            .toList();
    }
}
```

---

## 🔄 Data Flow (Полный цикл)

### 1️⃣ Loading (JPA → Planning)

```
Database (MySQL)
    │
    ├─► lessonRepository.findAll()
    │   └─► List<Lesson> (JPA entities with @Entity, @ManyToOne, etc.)
    │
    └─► PlanningModelMapper.toPlanningSolution()
        └─► TimetableSolution {
              lessonList: List<PlanningLesson> (Pure POJOs)
              timeslotList: List<PlanningTimeslot>
              roomList: List<PlanningRoom>
            }
```

**Преобразования:**
- `Lesson` (JPA) → `PlanningLesson` (POJO)
- `Timeslot` (JPA) → `PlanningTimeslot` (POJO)
- `Room` (JPA) → `PlanningRoom` (POJO)

**Зачем?**
- Unproxy Hibernate proxies
- Remove JPA baggage
- Create lightweight cloneable objects

---

### 2️⃣ Solving (Planning Model ONLY)

```
Timefold Solver Engine
    │
    ├─► Clone TimetableSolution (millions of times)
    │   └─► FAST! (POJOs, no JPA overhead)
    │
    ├─► Evaluate Constraints
    │   └─► DanceScheduleConstraintProvider.forEach(PlanningLesson.class)
    │       └─► No LazyInitializationException!
    │
    └─► Assign variables
        └─► planningLesson.setTimeslot(...)
            planningLesson.setRoom(...)
```

**Ключевые преимущества:**
- ✅ No Hibernate proxies
- ✅ No database access
- ✅ No LazyInitializationException
- ✅ Fast cloning (lightweight POJOs)

---

### 3️⃣ Saving (Planning → JPA)

```
TimetableSolution (solved)
    │
    └─► PlanningModelMapper.toPersistableLessons()
        └─► List<LessonUpdate> {
              lessonId: 1,
              timeslotId: 5,
              roomId: 3
            }
        │
        └─► Apply to JPA entities:
            lesson = lessonRepository.findById(1)
            lesson.setTimeslot(timeslotMap.get(5))
            lesson.setRoom(roomMap.get(3))
            │
            └─► Database (MySQL)
                UPDATE lessons SET timeslot_id=5, room_id=3 WHERE id=1
```

**Только обновляем:**
- `timeslot_id` (Planning Variable)
- `room_id` (Planning Variable)

**НЕ обновляем:**
- `teacher_id` (Problem Fact, не меняется)
- `dance_group_id` (Problem Fact, не меняется)

---

## 🎯 Почему это важно?

### Проблемы старого подхода (JPA + Timefold в одном классе):

```java
// BAD: Mixing JPA and Planning
@Entity
@PlanningEntity
public class Lesson {
    @ManyToOne
    @PlanningVariable
    private Timeslot timeslot; // Hibernate proxy!
}
```

**Что происходит во время solving:**

1. **Solver клонирует Lesson** → клонирует Hibernate proxy
2. **Constraint обращается к** `lesson.getTimeslot().getStartTime()`
3. **Hibernate пытается загрузить** Timeslot из БД
4. **💥 LazyInitializationException!** (вне сессии)

**Альтернатива: EAGER loading**
```java
@ManyToOne(fetch = FetchType.EAGER) // Fix?
```

**Новая проблема:**
- ❌ N+1 queries при загрузке
- ❌ Тяжелые объекты (загружаются все relationships)
- ❌ Медленное клонирование

---

### Решение: Separated Planning Model

```java
// GOOD: Separate models
// JPA (domain/model/)
@Entity
public class Lesson {
    @ManyToOne(fetch = FetchType.LAZY) // Optimized for database
    private Timeslot timeslot;
}

// Planning (solver/domain/)
@PlanningEntity
public class PlanningLesson {
    private PlanningTimeslot timeslot; // Simple reference, no JPA
}
```

**Что происходит во время solving:**

1. **Mapper загружает** все данные из БД (один раз)
2. **Mapper конвертирует** JPA → Planning POJOs (все в памяти)
3. **Solver клонирует** PlanningLesson → быстро (POJO)
4. **Constraint обращается** к `planningLesson.getTimeslot().getStartTime()`
5. **✅ Работает!** (нет JPA, нет LazyInit, все в памяти)

---

## 📚 Итоговое сравнение

| Характеристика | JPA Model | Planning Model |
|----------------|-----------|----------------|
| **Пакет** | `domain/model` | `solver/domain` |
| **Аннотации** | `@Entity`, `@ManyToOne` | `@PlanningEntity`, `@PlanningVariable` |
| **Назначение** | Persistence (БД) | Optimization (Solver) |
| **Технология** | JPA/Hibernate | Timefold Solver |
| **Relationships** | Hibernate proxies, LAZY | Simple references, in-memory |
| **Cloning** | Медленное (JPA overhead) | Быстрое (POJO) |
| **Database access** | Да (через EntityManager) | Нет (все в памяти) |
| **LazyInitException** | Возможна | Невозможна |
| **Используется в** | Controllers, Services, Repositories | Solver, ConstraintProvider |

---

## ✅ Вывод

**Папки `solver/domain` и `solver/mapper` — это правильная архитектурная практика!**

Они реализуют **Separation of Concerns**:
- 📊 **Persistence** → `domain/model` (JPA)
- 🧩 **Optimization** → `solver/domain` (Timefold)
- 🔄 **Conversion** → `solver/mapper` (Glue code)

Это **рекомендованный подход** для production-приложений с Timefold/OptaPlanner.

**Не нужно ничего менять** — архитектура правильная! ✅

