# 📁 Почему в папке solver появились domain и mapper?

**Дата:** 6 января 2026  
**Контекст:** Рефакторинг архитектуры Timefold Solver

---

## 🎯 Краткий ответ

Папки `solver/domain` и `solver/mapper` появились в результате **архитектурного рефакторинга**, который был сделан **ДО наших критичных исправлений**.

Это **правильное архитектурное решение**, которое разделяет:
- **JPA Persistence Model** (для базы данных) → `domain/model`
- **Timefold Planning Model** (для оптимизации) → `solver/domain`

---

## 📖 Подробное объяснение

### Проблема, которую это решает

**Изначально** в проекте была **плохая практика** (anti-pattern):

```java
// domain/model/Lesson.java
@Entity // JPA аннотация для БД
@PlanningEntity // Timefold аннотация для solver
public class Lesson {
    @Id
    @PlanningId
    private Long id;
    
    @ManyToOne
    @PlanningVariable // Смешивание JPA и Timefold
    private Timeslot timeslot;
}
```

**Проблемы этого подхода:**
1. ❌ **Hibernate proxies** во время solving → медленное клонирование объектов
2. ❌ **LazyInitializationException** при обращении к lazy-полям в constraints
3. ❌ **N+1 queries** из-за доступа к relationships в solver
4. ❌ **Тяжелый JPA багаж** (EntityManager, persistence context) не нужен для оптимизации

---

## 🏗️ Новая архитектура (после рефакторинга)

### Структура папок:

```
solver/
├── DanceSchedule.java                  # Старый класс (deprecated, но остался)
├── DanceScheduleConstraintProvider.java # Constraint Provider (обновлен)
├── domain/                             # 🆕 PLANNING MODEL (Pure POJOs)
│   ├── PlanningLesson.java            # Planning Entity (БЕЗ JPA)
│   ├── TimetableSolution.java         # Planning Solution
│   ├── PlanningTimeslot.java          # Problem Fact
│   ├── PlanningRoom.java              # Problem Fact
│   ├── PlanningTeacher.java           # Problem Fact
│   ├── PlanningDanceGroup.java        # Problem Fact
│   └── PlanningResourceUnavailability.java
└── mapper/                             # 🆕 КОНВЕРТЕР JPA ↔ Planning
    └── PlanningModelMapper.java       # Маппинг между моделями
```

---

## 🔄 Как это работает

### Шаг 1: Загрузка данных из БД (JPA)

```java
// SolverService.java
List<Lesson> lessons = lessonRepository.findAll(); // JPA entities
List<Timeslot> timeslots = timeslotRepository.findAll();
List<Room> rooms = roomRepository.findAll();
```

### Шаг 2: Конвертация в Planning Model

```java
// PlanningModelMapper.java
TimetableSolution solution = planningMapper.toPlanningSolution(
    scheduleId,
    lessons,      // JPA Lesson → PlanningLesson
    timeslots,    // JPA Timeslot → PlanningTimeslot
    rooms,        // JPA Room → PlanningRoom
    teachers,
    unavailabilities
);
```

**Результат:** Чистые POJO объекты БЕЗ JPA/Hibernate:

```java
// solver/domain/PlanningLesson.java
@PlanningEntity // ТОЛЬКО Timefold аннотации
@Getter
@Setter
@NoArgsConstructor
public class PlanningLesson { // БЕЗ @Entity!
    @PlanningId
    private Long id;
    
    private PlanningTeacher teacher; // НЕ @ManyToOne, простая ссылка
    
    @PlanningVariable(valueRangeProviderRefs = "timeslotRange")
    private PlanningTimeslot timeslot; // Lightweight POJO
    
    @PlanningVariable(valueRangeProviderRefs = "roomRange")
    private PlanningRoom room;
}
```

### Шаг 3: Solving (Timefold работает с Planning Model)

```java
// Solver видит только Planning POJOs
solverManager.solve(solution); // TimetableSolution с PlanningLesson
```

**Преимущества:**
- ✅ **Быстрое клонирование** (нет JPA proxy)
- ✅ **Нет LazyInitializationException** (все поля загружены)
- ✅ **Нет доступа к БД** во время solving (все данные в памяти)

### Шаг 4: Сохранение результата обратно в БД

```java
// PlanningModelMapper.java
var updates = planningMapper.toPersistableLessons(
    solution,      // TimetableSolution с решением
    timeslotMap,   // Lookup таблица JPA Timeslot
    roomMap        // Lookup таблица JPA Room
);

// Применяем к JPA entities
updates.forEach(update -> {
    Lesson lesson = lessonRepository.findById(update.lessonId()).get();
    lesson.setTimeslot(timeslotMap.get(update.timeslotId()));
    lesson.setRoom(roomMap.get(update.roomId()));
});
```

---

## 📊 Сравнение ДО и ПОСЛЕ

| Аспект | ДО (JPA + Timefold в одном классе) | ПОСЛЕ (Разделение моделей) |
|--------|-------------------------------------|----------------------------|
| **Класс для solver** | `Lesson.java` (@Entity + @PlanningEntity) | `PlanningLesson.java` (только @PlanningEntity) |
| **Hibernate proxies** | ❌ Присутствуют во время solving | ✅ Отсутствуют (POJO) |
| **LazyInitializationException** | ❌ Возможны при доступе к lazy-полям | ✅ Невозможны (все данные в памяти) |
| **Производительность клонирования** | ❌ Медленная (JPA overhead) | ✅ Быстрая (lightweight POJO) |
| **N+1 queries** | ❌ Риск при доступе к relationships | ✅ Нет доступа к БД |
| **Separation of Concerns** | ❌ Persistence и Planning смешаны | ✅ Четкое разделение |

---

## 🗂️ Назначение каждой папки

### `solver/domain/` — Planning Model (Timefold)

**Содержит:** Pure POJO классы для Timefold Solver

**Ответственность:**
- Представление проблемы оптимизации
- Хранение планируемых переменных (timeslot, room)
- Быстрое клонирование и копирование состояния

**Аннотации:** Только Timefold (`@PlanningEntity`, `@PlanningVariable`, `@PlanningSolution`)

**Нет JPA:** Нет `@Entity`, `@ManyToOne`, `@Column` и т.д.

**Примеры:**
- `PlanningLesson.java` — занятие с планируемым timeslot и room
- `TimetableSolution.java` — решение задачи расписания
- `PlanningTimeslot.java` — временной слот (Problem Fact)

---

### `solver/mapper/` — Конвертер между моделями

**Содержит:** `PlanningModelMapper.java` — Spring Component для маппинга

**Ответственность:**
1. **JPA → Planning:** Конвертация перед solving
   - Загружает данные из БД (JPA entities)
   - Создает легковесные POJO (Planning model)
   - "Unproxies" Hibernate объекты

2. **Planning → JPA:** Конвертация после solving
   - Извлекает решения из Planning model
   - Обновляет JPA entities
   - Сохраняет в БД

**Методы:**
```java
// JPA → Planning
TimetableSolution toPlanningSolution(...)

// Planning → JPA updates
List<LessonUpdate> toPersistableLessons(...)
```

---

## 🎓 Для защиты диплома

### Что сказать комиссии:

**"Я применил архитектурный паттерн разделения Persistence Model и Planning Model"**

**Обоснование:**
1. **Performance** — Timefold клонирует объекты миллионы раз. JPA entities тяжелые (proxies, lazy loading).
2. **Separation of Concerns** — База данных и оптимизация — разные ответственности.
3. **Best Practice** — Рекомендуется в официальной документации Timefold для больших проектов.

**Показать:**
```java
// domain/model/Lesson.java — для БД
@Entity
public class Lesson {
    @ManyToOne(fetch = FetchType.LAZY)
    private Teacher teacher;
}

// solver/domain/PlanningLesson.java — для оптимизации
@PlanningEntity
public class PlanningLesson {
    private PlanningTeacher teacher; // Простая ссылка, не JPA
}
```

---

## 📝 Связь с нашими рефакторингами

### Этап 1 (Критичные исправления):

Мы добавили `FetchType.LAZY` в JPA entities:

```java
// domain/model/Lesson.java
@ManyToOne(fetch = FetchType.LAZY) // Этап 1
private Teacher teacher;
```

**Это НЕ влияет** на Planning Model:

```java
// solver/domain/PlanningLesson.java
private PlanningTeacher teacher; // Уже POJO, не JPA
```

**Вывод:** Наши изменения улучшают JPA entities, а Planning Model уже был оптимален (POJO).

---

## ✅ Итог

### Почему эти папки существуют?

**Короткий ответ:**
> "Разделение моделей для устранения Hibernate overhead во время solving"

**Технический ответ:**
> "Применен архитектурный паттерн Separated Planning Model для изоляции Timefold Solver от JPA persistence layer, что устраняет Hibernate proxies, LazyInitializationException и N+1 queries во время оптимизации."

### Это правильно?

✅ **ДА!** Это **best practice** для Timefold/OptaPlanner в production-приложениях.

### Нужно ли что-то менять?

❌ **НЕТ!** Эта архитектура правильная и должна остаться.

Наши критичные исправления (Этап 1-2) **дополняют** эту архитектуру, а не заменяют её.

---

## 📚 Дополнительные ресурсы

Документация в проекте:
- `docs/refactoring/REFACTORING_PLANNING_MODEL.md` — детальное описание рефакторинга
- `docs/refactoring/EAGER_LOADING_EXAMPLE.java` — примеры проблем с EAGER loading

**Вывод:** Это часть предыдущего рефакторинга производительности, который был выполнен **до** наших критичных исправлений. Архитектура правильная и не требует изменений.

