Я предоставлю тебе списки файлов из основных слоев моего Spring Boot проекта.
Твоя задача — провести перекрестный анализ и найти сущности, у которых отсутствуют необходимые архитектурные компоненты (Контроллер, Сервис или Репозиторий).

Вот списки файлов:

**1. Models (Entities):**
[Вставь список: Student.java, Teacher.java, Lesson.java...]

**2. Controllers:**
[Вставь список: TeacherController.java, DictionaryController.java...]

**3. Services:**
[Вставь список: TeacherService.java, AuthService.java...]

**Задание:**
Составь таблицу "Missing Components Matrix".
Для каждой Model-сущности проверь наличие соответствующего Controller и Service.
Если компонент отсутствует, отметь это крестиком ❌.

Пример ожидаемого вывода:
| Entity | Controller | Service | Repository | Вердикт |
|---|---|---|---|---|
| Teacher | ✅ TeacherController | ✅ TeacherService | ✅ TeacherRepo | Ok |
| Student | ❌ MISSING | ❌ MISSING | ✅ StudentRepo | **Подозрительно: нет API для студентов** |
| Room | ❌ MISSING | ❌ MISSING | ✅ RoomRepo | Возможно, спрятано в DictionaryController? |

После таблицы напиши свои гипотезы: почему эти файлы могут отсутствовать? Это ошибка или архитектурное решение (например, Read-only справочник)?