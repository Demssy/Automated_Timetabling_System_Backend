# 🏗️ Анализ влияния рекомендаций на архитектуру проекта

**Дата:** 5 января 2026  
**Проект:** Automated Timetabling System Backend  
**Базовый отчет:** review1.md

---

## 📊 ОБЩИЙ ВЕРДИКТ

### ❓ Сильно ли изменится архитектура?

**ОТВЕТ: НЕТ, архитектура изменится НЕЗНАЧИТЕЛЬНО** ✅

**Почему:**
- ✅ Основные слои (Controller → Service → Repository) **остаются без изменений**
- ✅ Package structure **не меняется**
- ✅ DTOs, Mappers, Entities — **структура сохраняется**
- ✅ **95% кода** — это **локальные улучшения** (аннотации, поля, методы)
- ⚠️ Только **5% изменений** затрагивают структуру (новые файлы для интерфейсов и exceptions)

---

## 🎯 Классификация изменений

### 🟢 **Уровень 1: МИНИМАЛЬНЫЕ изменения (90% работы)**
> **Добавление аннотаций и полей в существующие классы**  
> **НЕ ТРЕБУЕТ** изменения архитектуры или структуры пакетов

#### 1.1. Добавление `@Version` в Entities (7 файлов)
**Тип изменения:** Добавление одного поля  
**Затронутые файлы:**
```
domain/model/
├── Teacher.java          (+1 поле @Version)
├── Student.java          (+1 поле @Version)
├── DanceGroup.java       (+1 поле @Version)
├── Lesson.java           (+1 поле @Version)
├── Room.java             (+1 поле @Version)
├── DanceStyle.java       (+1 поле @Version)
└── ScheduleMetadata.java (+1 поле @Version)
```

**Пример изменения:**
```java
// ДО
@Entity
public class Teacher extends AbstractUser {
    private int maxDailyHours;
    // ...
}

// ПОСЛЕ
@Entity
public class Teacher extends AbstractUser {
    @Version
    private Long version; // +1 строка
    
    private int maxDailyHours;
    // ...
}
```

**Влияние на архитектуру:** ❌ НИКАКОГО  
**Ломает ли существующий код:** ❌ НЕТ (JPA автоматически управляет полем)  
**Требует изменений в других слоях:** ❌ НЕТ  

---

#### 1.2. Добавление `fetch = FetchType.LAZY` (10+ мест)
**Тип изменения:** Изменение параметра аннотации  
**Затронутые файлы:**
```
domain/model/
├── Lesson.java              (4 аннотации @ManyToOne)
├── DanceGroup.java          (1 аннотация @ManyToOne)
├── ResourceUnavailability.java (2 аннотации @ManyToOne)
├── Teacher.java             (1 аннотация @ManyToMany)
├── DanceStyle.java          (1 аннотация @ManyToMany)
└── AbstractUser.java        (1 аннотация @ManyToOne для Role)
```

**Пример изменения:**
```java
// ДО
@ManyToOne
@JoinColumn(name = "teacher_id")
private Teacher teacher;

// ПОСЛЕ
@ManyToOne(fetch = FetchType.LAZY) // +18 символов
@JoinColumn(name = "teacher_id")
private Teacher teacher;
```

**Влияние на архитектуру:** ❌ НИКАКОГО  
**Ломает ли существующий код:** ⚠️ ВОЗМОЖНО (если есть toString() вне транзакций)  
**Требует изменений в других слоях:** ✅ ДА — нужно добавить `@EntityGraph` в repositories  

---

#### 1.3. Исправление `@ToString(exclude = {...})` (2 файла)
**Тип изменения:** Изменение параметра аннотации  
**Затронутые файлы:**
```
domain/model/
├── Teacher.java     (@ToString(callSuper = true) → @ToString(callSuper = true, exclude = {"danceStyles"}))
└── DanceStyle.java  (@ToString → @ToString(exclude = {"teachers"}))
```

**Пример изменения:**
```java
// ДО
@ToString(callSuper = true)
public class Teacher extends AbstractUser {
    @ManyToMany
    private Set<DanceStyle> danceStyles;
}

// ПОСЛЕ
@ToString(callSuper = true, exclude = {"danceStyles"}) // +29 символов
public class Teacher extends AbstractUser {
    @ManyToMany
    private Set<DanceStyle> danceStyles;
}
```

**Влияние на архитектуру:** ❌ НИКАКОГО  
**Ломает ли существующий код:** ❌ НЕТ (улучшает стабильность)  

---

#### 1.4. Добавление `@Transactional(readOnly = true)` на классах (3 файла)
**Тип изменения:** Добавление аннотации на класс  
**Затронутые файлы:**
```
service/
├── TeacherService.java  (+1 аннотация на классе, +1 на методе)
├── AuthService.java     (+1 аннотация на классе, +2 на методах)
└── SolverService.java   (+1 аннотация на классе)
```

**Пример изменения:**
```java
// ДО
@Service
@RequiredArgsConstructor
public class TeacherService {
    @Transactional
    public TeacherResponse createTeacher(...) { ... }
}

// ПОСЛЕ
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // +1 строка на классе
public class TeacherService {
    @Transactional // Переопределение для write-операций
    public TeacherResponse createTeacher(...) { ... }
}
```

**Влияние на архитектуру:** ❌ НИКАКОГО  
**Ломает ли существующий код:** ❌ НЕТ (performance improvement)  

---

#### 1.5. Добавление валидации в DTO (1 файл)
**Тип изменения:** Добавление аннотаций  
**Затронутые файлы:**
```
domain/dto/
└── AuthenticationRequest.java (+2 аннотации)
```

**Пример изменения:**
```java
// ДО
public record AuthenticationRequest(String email, String password) {}

// ПОСЛЕ
public record AuthenticationRequest(
    @NotBlank @Email String email,    // +2 аннотации
    @NotBlank String password         // +1 аннотация
) {}
```

**Влияние на архитектуру:** ❌ НИКАКОГО  
**Ломает ли существующий код:** ❌ НЕТ  

---

#### 1.6. Изменение URI с `/api/...` на `/api/v1/...` (4 контроллера)
**Тип изменения:** Изменение строки в аннотации  
**Затронутые файлы:**
```
controller/
├── TeacherController.java    (@RequestMapping("/api/teachers") → "/api/v1/teachers")
├── AuthController.java       (@RequestMapping("/api/auth") → "/api/v1/auth")
├── SolverController.java     (@RequestMapping("/api/solver") → "/api/v1/solver")
└── DictionaryController.java (@RequestMapping("/api/dictionaries") → "/api/v1/dictionaries")
```

**Влияние на архитектуру:** ❌ НИКАКОГО  
**Ломает ли существующий код:** ⚠️ ДА (фронтенд должен обновить URLs)  
**Альтернатива:** Настроить глобально через `application.properties`:
```properties
spring.mvc.servlet.path=/api/v1
```

---

### 🟡 **Уровень 2: СРЕДНИЕ изменения (8% работы)**
> **Создание новых классов без изменения существующих**  
> **Минимальное влияние** на архитектуру

#### 2.1. Создание Service Interfaces (3 новых файла)
**Тип изменения:** Извлечение интерфейса из класса  
**Новые файлы:**
```
service/
├── AuthService.java (интерфейс, ~10 строк)
├── TeacherService.java (интерфейс, ~8 строк)
└── SolverService.java (интерфейс, ~15 строк)
```

**Переименование:**
```
service/
├── AuthServiceImpl.java (было AuthService.java)
├── TeacherServiceImpl.java (было TeacherService.java)
└── SolverServiceImpl.java (было SolverService.java)
```

**Пример:**
```java
// service/AuthService.java (НОВЫЙ ФАЙЛ)
public interface AuthService {
    String registerStudent(String email, String password, String fullName, LocalDate birthDate);
    String authenticate(String email, String password);
}

// service/AuthServiceImpl.java (ПЕРЕИМЕНОВАН)
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    // ...existing code...
}
```

**Влияние на архитектуру:** ⚠️ МИНИМАЛЬНОЕ  
**Ломает ли существующий код:** ⚠️ ВОЗМОЖНО  
- Controllers нужно обновить инъекции:
```java
// ДО
private final TeacherService teacherService;

// ПОСЛЕ (ОПЦИОНАЛЬНО - можно оставить как было)
private final TeacherService teacherService; // Будет внедрен TeacherServiceImpl
```

**Примечание:** Spring автоматически найдет `TeacherServiceImpl` для интерфейса `TeacherService`, так что **большинство кода не изменится**.

---

#### 2.2. Создание Custom Exceptions (3 новых файла)
**Тип изменения:** Добавление новых классов  
**Новые файлы:**
```
domain/exception/  (НОВЫЙ ПАКЕТ)
├── ResourceNotFoundException.java (~15 строк)
├── BusinessRuleViolationException.java (~15 строк)
└── OptimisticLockingException.java (~15 строк)
```

**Пример:**
```java
// domain/exception/ResourceNotFoundException.java
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String resourceName, Long id) {
        super(String.format("%s with id %d not found", resourceName, id));
    }
}
```

**Влияние на архитектуру:** ⚠️ МИНИМАЛЬНОЕ (новый пакет)  
**Ломает ли существующий код:** ❌ НЕТ  
**Требует изменений в существующих классах:** ✅ ДА  
- Services должны заменить `IllegalArgumentException`:
```java
// ДО
if (!userRepository.existsById(id)) {
    throw new IllegalArgumentException("User not found");
}

// ПОСЛЕ
if (!userRepository.existsById(id)) {
    throw new ResourceNotFoundException("User", id);
}
```

---

### 🔴 **Уровень 3: СУЩЕСТВЕННЫЕ изменения (2% работы)**
> **Создание нового архитектурного слоя**  
> **Среднее влияние** на архитектуру

#### 3.1. Создание Global Exception Handler (1 новый файл)
**Тип изменения:** Добавление нового архитектурного компонента  
**Новый файл:**
```
exception/  (НОВЫЙ ПАКЕТ на верхнем уровне)
└── GlobalExceptionHandler.java (~80 строк)
```

**Структура проекта ПОСЛЕ:**
```
com.timetable.backend/
├── BackendApplication.java
├── config/
├── controller/
├── domain/
├── exception/           ← НОВЫЙ ПАКЕТ
│   └── GlobalExceptionHandler.java
├── security/
├── service/
└── solver/
```

**Пример:**
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND, 
            ex.getMessage()
        );
    }
    
    @ExceptionHandler(BusinessRuleViolationException.class)
    public ProblemDetail handleBusinessRule(BusinessRuleViolationException ex) {
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, 
            ex.getMessage()
        );
    }
    
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLocking(OptimisticLockingFailureException ex) {
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            "Resource was modified by another user. Please refresh and try again."
        );
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
            .map(err -> err.getField() + ": " + err.getDefaultMessage())
            .collect(Collectors.joining(", "));
        
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Validation failed: " + errors
        );
    }
}
```

**Влияние на архитектуру:** ⚠️ СРЕДНЕЕ  
- **Добавляет новый слой** обработки ошибок
- **Централизует** логику, которая сейчас разбросана по контроллерам

**Ломает ли существующий код:** ❌ НЕТ  
**Улучшения:**
- ✅ Все контроллеры получат единообразные ответы
- ✅ Можно убрать `try-catch` из контроллеров
- ✅ Автоматическая обработка валидации

---

#### 3.2. Перенос логики из DictionaryController в DictionaryService (1 новый файл)
**Тип изменения:** Создание нового сервиса  
**Новый файл:**
```
service/
└── DictionaryService.java (~150 строк)
```

**Изменения в DictionaryController:**
```java
// ДО (логика в контроллере)
@PutMapping("/rooms/{id}")
public ResponseEntity<RoomDTO> updateRoom(@PathVariable Long id, @Valid @RequestBody RoomDTO updated) {
    return roomRepository.findById(id).map(r -> {
        r.setName(updated.name());
        r.setCapacity(updated.capacity());
        r.setAllowsParallelPrivate(updated.allowsParallelPrivate());
        roomRepository.save(r);
        return ResponseEntity.ok(dictionaryMapper.toRoomDTO(r));
    }).orElseGet(() -> ResponseEntity.notFound().build());
}

// ПОСЛЕ (логика в сервисе)
@PutMapping("/rooms/{id}")
public ResponseEntity<RoomDTO> updateRoom(@PathVariable Long id, @Valid @RequestBody RoomDTO updated) {
    return ResponseEntity.ok(dictionaryService.updateRoom(id, updated));
}
```

**Влияние на архитектуру:** ⚠️ МИНИМАЛЬНОЕ  
- Добавляет сервис, которого раньше не было (сейчас контроллер обращается напрямую к repository)
- Улучшает соответствие Clean Architecture

---

## 📈 Визуализация изменений

### ТЕКУЩАЯ АРХИТЕКТУРА
```
┌─────────────────────────────────────────────────┐
│              Controllers (4 шт)                 │
│  ┌─────────┐  ┌──────┐  ┌────────┐  ┌──────┐  │
│  │ Teacher │  │ Auth │  │ Solver │  │ Dict │  │
│  └────┬────┘  └───┬──┘  └───┬────┘  └──┬───┘  │
│       │           │          │           │       │
└───────┼───────────┼──────────┼───────────┼──────┘
        │           │          │           │
        ▼           ▼          ▼           ▼
┌───────────────────────────────────────────────────┐
│           Services (3 шт + Security)              │
│  ┌─────────────┐  ┌──────────┐  ┌─────────────┐ │
│  │   Teacher   │  │   Auth   │  │   Solver    │ │
│  │  Service    │  │ Service  │  │  Service    │ │
│  └──────┬──────┘  └────┬─────┘  └──────┬──────┘ │
│         │              │                │         │
│         │       ┌──────┴──────┐        │         │
│         │       │  Security   │        │         │
│         │       │  (JWT, etc) │        │         │
│         │       └─────────────┘        │         │
└─────────┼────────────────────────────────────────┘
          │                               │
          ▼                               ▼
┌─────────────────────────────────────────────────┐
│          Repositories (10 шт)                   │
│  ┌────────┐  ┌────┐  ┌──────┐  ┌────────┐     │
│  │Teacher │  │User│  │Lesson│  │  Room  │ ... │
│  └───┬────┘  └─┬──┘  └───┬──┘  └───┬────┘     │
└──────┼─────────┼─────────┼─────────┼───────────┘
       │         │         │         │
       ▼         ▼         ▼         ▼
┌─────────────────────────────────────────────────┐
│              JPA Entities (10 шт)               │
│  Teacher, Student, Lesson, Room, etc.           │
└─────────────────────────────────────────────────┘
```

### АРХИТЕКТУРА ПОСЛЕ РЕФАКТОРИНГА
```
┌─────────────────────────────────────────────────┐
│              Controllers (4 шт)                 │
│  ┌─────────┐  ┌──────┐  ┌────────┐  ┌──────┐  │
│  │ Teacher │  │ Auth │  │ Solver │  │ Dict │  │
│  └────┬────┘  └───┬──┘  └───┬────┘  └──┬───┘  │
│       │           │          │           │       │
└───────┼───────────┼──────────┼───────────┼──────┘
        │           │          │           │
┌───────┼───────────┼──────────┼───────────┼──────┐
│       │ ┌─────────▼──────────────────┐   │      │
│       │ │ GlobalExceptionHandler     │◄──┘      │
│       │ │  (@RestControllerAdvice)   │          │
│       │ │  - ProblemDetail responses │          │
│       │ │  - Custom exceptions       │          │
│       │ └────────────────────────────┘          │
│       │           Exception Layer                │
└───────┼──────────────────────────────────────────┘
        │           │          │           │
        ▼           ▼          ▼           ▼
┌───────────────────────────────────────────────────┐
│         Services (4 шт + Interfaces)              │
│  ┌─────────────┐  ┌──────────┐  ┌─────────────┐ │
│  │   Teacher   │  │   Auth   │  │   Solver    │ │
│  │  Service ⓘ  │  │ Service ⓘ│  │  Service ⓘ  │ │ ⓘ = Interface
│  └──────┬──────┘  └────┬─────┘  └──────┬──────┘ │
│         │              │                │         │
│  ┌──────┴──────┐       │         ┌──────┴──────┐ │
│  │ Dictionary  │       │         │   NEW!      │ │
│  │  Service ⓘ  │ ◄─────┘         └─────────────┘ │
│  └─────────────┘                                  │
└─────────┼────────────────────────────────────────┘
          │                               │
          ▼                               ▼
┌─────────────────────────────────────────────────┐
│          Repositories (10 шт)                   │
│  ┌────────┐  ┌────┐  ┌──────┐  ┌────────┐     │
│  │Teacher │  │User│  │Lesson│  │  Room  │ ... │
│  └───┬────┘  └─┬──┘  └───┬──┘  └───┬────┘     │
└──────┼─────────┼─────────┼─────────┼───────────┘
       │         │         │         │
       ▼         ▼         ▼         ▼
┌─────────────────────────────────────────────────┐
│         JPA Entities (10 шт) + @Version         │
│  Teacher, Student, Lesson, Room, etc.           │
│  + Optimistic Locking                           │
│  + FetchType.LAZY                               │
│  + @ToString(exclude)                           │
└─────────────────────────────────────────────────┘
```

**Ключевые отличия:**
- 🆕 **Добавлен слой Exception Handling** (GlobalExceptionHandler)
- 🆕 **Добавлен DictionaryService** (было прямое обращение к repository)
- 🔄 **Service Interfaces** (опционально, не меняет структуру)
- ⚡ **Entities усилены** (@Version, LAZY, @ToString exclude)

---

## 📊 Сравнительная таблица изменений

| Компонент | Текущее состояние | После рефакторинга | Тип изменения |
|-----------|-------------------|-------------------|---------------|
| **Entities (10 шт)** | Без @Version, EAGER, @ToString | +@Version, LAZY, exclude | 🟢 Локальное |
| **Services (3 шт)** | Прямые классы | +Interfaces (опц.) | 🟡 Структурное |
| **Controllers (4 шт)** | `/api/...`, логика в Dictionary | `/api/v1/...`, чистые | 🟢 Локальное |
| **Exception Handling** | ❌ Отсутствует | ✅ GlobalExceptionHandler | 🔴 Новый слой |
| **Custom Exceptions** | ❌ Отсутствуют | ✅ 3 класса | 🟡 Новые классы |
| **DTOs (15 шт)** | Без изменений | +1 валидация | 🟢 Локальное |
| **Repositories (10 шт)** | Без изменений | Без изменений* | ✅ Без изменений |
| **Mappers (4 шт)** | Без изменений | Без изменений | ✅ Без изменений |

\* Возможно добавление `@EntityGraph` для оптимизации LAZY loading

---

## 🎯 Резюме влияния на архитектуру

### ✅ ЧТО **НЕ МЕНЯЕТСЯ**:
1. ✅ **Основные слои** (Controller → Service → Repository)
2. ✅ **Package structure** (domain, controller, service, repository, solver)
3. ✅ **DTOs и Mappers** (почти без изменений)
4. ✅ **Security layer** (без изменений)
5. ✅ **Solver logic** (Timefold, Constraint Streams — без изменений)
6. ✅ **Database schema** (Flyway миграции — без изменений)

### ⚠️ ЧТО **ДОБАВЛЯЕТСЯ**:
1. 🆕 **Exception Handling Layer** (GlobalExceptionHandler + custom exceptions)
2. 🆕 **DictionaryService** (новый сервис)
3. 🆕 **Service Interfaces** (опционально)

### 🔄 ЧТО **УЛУЧШАЕТСЯ**:
1. ⚡ **Entities** (Optimistic Locking, LAZY loading, безопасный toString)
2. ⚡ **Transaction Management** (читаемые по умолчанию, писать явно)
3. ⚡ **API Versioning** (URI с `/v1/`)

---

## 📉 Оценка сложности миграции

| Этап | Сложность | Время | Риск поломки |
|------|-----------|-------|--------------|
| **Этап 1: Критичные исправления** | 🟢 Низкая | 1-2 дня | 🟢 Минимальный |
| └─ Добавление @Version | 🟢 Очень низкая | 30 мин | ✅ Нет |
| └─ FetchType.LAZY | 🟡 Средняя | 1 час | ⚠️ Возможны LazyInit ошибки |
| └─ @ToString(exclude) | 🟢 Низкая | 15 мин | ✅ Нет |
| └─ GlobalExceptionHandler | 🟡 Средняя | 2-3 часа | ✅ Нет (улучшает) |
| **Этап 2: Средние улучшения** | 🟡 Средняя | 2-3 дня | 🟡 Средний |
| └─ Service Interfaces | 🟡 Средняя | 1 день | ⚠️ Требует обновления DI |
| └─ @Transactional | 🟢 Низкая | 1 час | ✅ Нет |
| └─ URI Versioning | 🟢 Низкая | 30 мин | ⚠️ Фронтенд должен обновиться |
| **Этап 3: Полировка** | 🟢 Низкая | 1 день | 🟢 Минимальный |
| └─ DictionaryService | 🟡 Средняя | 3-4 часа | ✅ Нет |
| └─ Validation в DTO | 🟢 Низкая | 15 мин | ✅ Нет |

**ИТОГО:** 4-6 дней работы, риск поломки **НИЗКИЙ**

---

## 🎓 Рекомендации для дипломного проекта

### ✅ **ОБЯЗАТЕЛЬНО СДЕЛАТЬ:**
1. ✅ **Этап 1** (критичные исправления) — **MUST HAVE** для Production-ready кода
   - Покажет комиссии знание Optimistic Locking
   - Демонстрирует понимание N+1 проблем
   - Глобальная обработка ошибок — признак зрелого проекта

### ⚠️ **ЖЕЛАТЕЛЬНО СДЕЛАТЬ:**
2. ⚠️ **Этап 2.1** (Service Interfaces) — **NICE TO HAVE**
   - Показывает знание SOLID (Dependency Inversion)
   - Улучшает тестируемость
   - НО: можно отложить, если мало времени

3. ✅ **Этап 2.2-2.3** (@Transactional, URI Versioning) — **РЕКОМЕНДУЕТСЯ**
   - Низкая сложность, высокая ценность
   - Показывает внимание к деталям

### 🟢 **ОПЦИОНАЛЬНО:**
4. 🟢 **Этап 3** (полировка) — **OPTIONAL**
   - Делать, если есть время
   - Не критично для защиты

---

## 💡 Итоговый вердикт

### ❓ Сильно ли изменится архитектура?

# **НЕТ, изменения будут МИНИМАЛЬНЫМИ** ✅

**Аргументы:**
- 📊 **90% изменений** — это добавление аннотаций и полей
- 🏗️ **5% изменений** — новые файлы (interfaces, exceptions)
- 🔧 **5% изменений** — рефакторинг логики (DictionaryController → Service)
- ✅ **Основная архитектура сохраняется** (слои, пакеты, flow)
- ⚡ **Улучшения локальные**, не требуют переписывания системы

**Сравнение:**
```
Текущий проект: 📦 90%  готов к Production
После рефакторинга: 📦 100% Production-ready + Enterprise-grade
```

**Для комиссии это будет выглядеть как:**
> "Студент не только реализовал функционал, но и **применил Enterprise best practices**: Optimistic Locking для конкурентности, LAZY loading для производительности, RFC 7807 для стандартизированных ошибок, и Service Interfaces для тестируемости."

---

## 🚀 План действий

### Если времени мало (3-4 дня):
1. ✅ Этап 1: Критичные исправления (обязательно)
2. ✅ @Transactional на классах (30 минут)
3. ⚠️ URI Versioning через `application.properties` (5 минут)

### Если времени достаточно (5-7 дней):
1. ✅ Этап 1: Критичные исправления
2. ✅ Этап 2: Средние улучшения
3. ✅ Этап 3: Полировка (если останется время)

---

**ВЫВОД:** Рефакторинг **не ломает архитектуру**, а **усиливает её** соответствие Enterprise-стандартам. Это **эволюция**, а не **революция**.

