# 🔍 Missing Components Matrix — Архитектурный Анализ

## 📊 Таблица компонентов (Entities vs Controller/Service/Repository)

| # | Entity | Controller | Service | Repository | Вердикт |
|---|--------|-----------|---------|-----------|---------|
| 1 | **Teacher** | ✅ TeacherController | ✅ ITeacherService<br>✅ TeacherService | ✅ TeacherRepository | ✅ **OK** — Полная архитектура |
| 2 | **Student** | ✅ StudentController | ✅ IStudentService<br>✅ StudentService | ❌ **MISSING** | ⚠️ **Проблема**: Student хранится через `UserRepository` (полиморфизм), но нет прямого доступа |
| 3 | **Admin** | ❌ MISSING | ❌ MISSING | ❌ **MISSING** | 🟡 **Возможно OK**: Админы управляются через `AuthService` + `UserRepository` |
| 4 | **AbstractUser** | ❌ N/A | ❌ N/A | ✅ UserRepository | 🟢 **OK** — Абстрактный класс, не требует отдельного API |
| 5 | **Role** | ❌ MISSING | ❌ MISSING | ✅ RoleRepository | 🟡 **Возможно OK**: Роли — справочник, управляются через миграции |
| 6 | **Room** | ❌ MISSING | ❌ MISSING | ✅ RoomRepository | 🔴 **СПРЯТАНО** в `DictionaryController` (анти-паттерн) |
| 7 | **DanceStyle** | ❌ MISSING | ❌ MISSING | ✅ DanceStyleRepository | 🔴 **СПРЯТАНО** в `DictionaryController` (анти-паттерн) |
| 8 | **DanceGroup** | ❌ MISSING | ❌ MISSING | ✅ DanceGroupRepository | 🔴 **КРИТИЧНО**: Бизнес-сущность без API! |
| 9 | **Lesson** | ❌ MISSING | ❌ MISSING | ✅ LessonRepository | 🔴 **КРИТИЧНО**: Основная сущность без API! |
| 10 | **Timeslot** | ❌ MISSING | ❌ MISSING | ✅ TimeslotRepository | 🟡 **Возможно OK**: Справочник, но CRUD нужен |
| 11 | **ResourceUnavailability** | ❌ MISSING | ❌ MISSING | ✅ ResourceUnavailabilityRepository | 🔴 **ПРОБЛЕМА**: Должен быть связан с TeacherController |
| 12 | **ScheduleMetadata** | ✅ SolverController<br>(частично) | ✅ ISolverService<br>✅ SolverService | ✅ ScheduleMetadataRepository | 🟡 **Неполно**: API только для солвера, нет CRUD для расписаний |
| 13 | **DanceLevel** (enum) | ❌ N/A | ❌ N/A | ❌ N/A | 🟢 **OK** — Enum, не требует отдельного хранения |
| 14 | **ScheduleStatus** (enum) | ❌ N/A | ❌ N/A | ❌ N/A | 🟢 **OK** — Enum, не требует отдельного хранения |

---

## 🧠 Гипотезы: Почему компоненты отсутствуют?

### 🔴 **Критичные проблемы (требуют немедленного исправления):**

#### 1. **Lesson** — нет API
**Проблема:**  
`Lesson` — это **ключевая бизнес-сущность** (занятия танцевальной школы), но у неё **нет контроллера и сервиса**.

**Гипотеза:**
- Возможно, предполагалось, что занятия управляются только через `SolverService` (автоматическая генерация расписания).
- Но это **нарушает Clean Architecture**: администратор должен иметь возможность:
    - Создавать занятия вручную (`POST /api/v1/lessons`)
    - Редактировать их (`PUT /api/v1/lessons/{id}`)
    - Удалять (`DELETE /api/v1/lessons/{id}`)
    - Просматривать (`GET /api/v1/lessons`)

**Рекомендация:**  
Создать `LessonController`, `ILessonService`, `LessonService` для полноценного CRUD.

---

#### 2. **DanceGroup** — нет API
**Проблема:**  
`DanceGroup` (группы студентов) — это **основная доменная сущность**, но у неё **нет публичного API**.

**Гипотеза:**
- Возможно, планировалось управление через `DictionaryController`, но туда **не добавили эндпоинты**.
- Либо предполагалось, что группы создаются только через SQL-миграции.

**Последствия:**
- Администратор **не может создать новую группу** через UI.
- Невозможно привязать студентов к группам через API.

**Рекомендация:**  
Вариант 1: Добавить CRUD в `DictionaryController` (быстро, но нарушает SRP).  
Вариант 2: Создать `DanceGroupController` + Service (правильно, но больше кода).

---

#### 3. **ResourceUnavailability** — висит без API
**Проблема:**  
`ResourceUnavailability` (недоступность учителя) — это **бизнес-логика**, но управляется только через Repository напрямую.

**Гипотеза:**
- Планировалось добавить это в `TeacherController` как вложенный ресурс:
  ```
  POST /api/v1/teachers/{teacherId}/unavailability
  GET /api/v1/teachers/{teacherId}/unavailability
  DELETE /api/v1/teachers/{teacherId}/unavailability/{id}
  ```
- Но **забыли реализовать**.

**Последствия:**
- Учитель **не может отметить** свой отпуск/больничный через UI.
- Солвер будет назначать уроки в неправильное время.

**Рекомендация:**  
Добавить методы в `ITeacherService` и `TeacherController` для управления недоступностью.

---

### 🟡 **Спорные решения (требуют ревью):**

#### 4. **Room и DanceStyle** — спрятаны в `DictionaryController`
**Текущая ситуация:**  
`DictionaryController` объединяет **два разных справочника** (Rooms и Dance Styles) в одном контроллере.

**Проблемы:**
- **Нарушение SRP** (Single Responsibility Principle): один контроллер отвечает за две сущности.
- **Сложность расширения**: если добавятся сложные операции (например, "найти все залы с вместимостью > 20"), логика будет смешана.
- **Отсутствие Service Layer**: логика **напрямую вызывает Repository** из Controller (нарушение Clean Architecture).

**Почему так сделано:**  
Скорее всего, это **компромисс для ускорения разработки**:
- "Это же простые справочники, зачем им отдельные сервисы?"
- Но по мере роста проекта это станет узким местом.

**Рекомендация:**  
**Рефакторинг в 2 этапа:**
1. Создать `IRoomService`, `RoomService`, `IDanceStyleService`, `DanceStyleService` — вынести логику из контроллера.
2. (Опционально) Разделить на `RoomController` и `DanceStyleController`, удалить `DictionaryController`.

---

#### 5. **Student** — Repository через полиморфизм
**Текущая ситуация:**  
`Student`, `Teacher`, `Admin` наследуются от `AbstractUser`.  
Все хранятся через `UserRepository`, но **нет** отдельного `StudentRepository`.

**Почему так работает:**  
JPA Joined Inheritance (`@Inheritance(strategy = InheritanceType.JOINED)`) позволяет получать конкретные типы:
```java
List<Student> students = userRepository.findAll().stream()
    .filter(u -> u instanceof Student)
    .map(u -> (Student) u)
    .toList();
```

Но это **неэффективно** (загружает все типы пользователей, фильтрует в памяти).

**Проблема:**  
`StudentService` существует, но **не может эффективно работать** без прямого доступа к таблице `students`.

**Рекомендация:**  
Создать `StudentRepository extends JpaRepository<Student, Long>`:
```java
public interface StudentRepository extends JpaRepository<Student, Long> {
    List<Student> findByDanceLevel(DanceLevel level);
    List<Student> findByBirthDateAfter(LocalDate date);
}
```

Это позволит JPA генерировать оптимизированные запросы напрямую к таблице `students`.

---

### 🟢 **Архитектурно корректные отсутствия:**

#### 6. **Role** — нет API (это OK)
**Почему нет Controller/Service:**  
`Role` — это **системный справочник**, который:
- Создаётся через Flyway-миграции (`V2__dictionaries.sql`).
- **Не изменяется** во время работы приложения (роли жёстко зашиты: `ROLE_ADMIN`, `ROLE_TEACHER`, `ROLE_STUDENT`).

**Вердикт:**  
✅ **Правильное решение** — роли не должны редактироваться через API (это security-риск).

---

#### 7. **Admin** — нет отдельного API (это спорно)
**Текущая ситуация:**  
Админы создаются через `AuthService.register(...)`, но **нет отдельного CRUD**.

**Почему так:**
- Админы — это **привилегированные пользователи**, их количество мало (1-5 человек).
- Создание админа — это **редкая операция**, возможно, выполняется через SQL напрямую.

**Проблема:**  
Если потребуется:
- Изменить `hasBillingAccess` у админа
- Деактивировать админа (`isActive = false`)

То придётся **либо писать SQL вручную**, либо добавлять API.

**Рекомендация:**  
На данном этапе — **OK**, но в будущем стоит добавить `AdminController` для управления администраторами.

---

#### 8. **Timeslot** — нет API (но нужен)
**Текущая ситуация:**  
`Timeslot` (таймслоты: "Понедельник, 10:00-11:00") — это **справочник**, но **нет CRUD**.

**Почему это проблема:**
- Администратор должен иметь возможность добавлять новые таймслоты (например, "Суббота, 18:00-19:00").
- Сейчас это возможно **только через SQL**.

**Рекомендация:**  
Добавить CRUD в `DictionaryController` или создать отдельный `TimeslotController`.

---

## 📋 Итоговый Вердикт

### 🔴 **Критичные пробелы (MUST FIX):**
1. **Lesson** — нужен LessonController + Service
2. **DanceGroup** — нужен DanceGroupController + Service (или добавить в Dictionary)
3. **ResourceUnavailability** — добавить в TeacherController как вложенный ресурс

### 🟡 **Требует рефакторинга:**
4. **DictionaryController** — вынести логику в RoomService + DanceStyleService
5. **StudentRepository** — создать отдельный репозиторий для эффективных запросов
6. **Timeslot** — добавить CRUD для управления таймслотами

### 🟢 **Архитектурно корректно:**
7. **Role** — справочник, не требует API ✅
8. **Admin** — редкая сущность, управляется через AuthService ✅
9. **AbstractUser** — абстрактный класс, не требует отдельного API ✅
10. **DanceLevel/ScheduleStatus** — enum-ы, не требуют хранения ✅

---

## 🛠️ Рекомендации по приоритетам

### Высокий приоритет (блокирует работу):
1. Создать **LessonController + ILessonService + LessonService**
2. Создать **DanceGroupController + Service** (или добавить в Dictionary)
3. Добавить управление `ResourceUnavailability` в `TeacherController`

### Средний приоритет (техдолг):
4. Рефакторинг `DictionaryController` → вынести сервисы
5. Создать `StudentRepository` для оптимизации запросов

### Низкий приоритет (не критично):
6. Добавить `AdminController` для управления администраторами
7. Добавить CRUD для `Timeslot`

---

**Вывод:**  
Ваш проект имеет **хорошую базовую архитектуру** (есть слои, интерфейсы сервисов, DTO), но **пропущены критичные API** для управления основными доменными сущностями.  
Это типично для **раннего этапа разработки**, когда акцент был на солвере, а CRUD откладывали "на потом".

Теперь пришло время это исправить! 🚀
