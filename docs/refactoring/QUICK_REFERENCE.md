# 📋 Быстрая шпаргалка: solver/domain и solver/mapper

## ❓ Вопрос
**Почему в папке solver теперь находятся domain и mapper?**

## ✅ Короткий ответ
Это **архитектурный паттерн разделения моделей**:
- `domain/model/` → для **базы данных** (JPA)
- `solver/domain/` → для **оптимизации** (Timefold)
- `solver/mapper/` → **конвертер** между ними

## 🎯 Зачем это нужно?

### Проблема (если смешивать JPA + Timefold)
```java
@Entity          // JPA для БД
@PlanningEntity  // Timefold для solver
public class Lesson {
    @ManyToOne              // Hibernate proxy
    @PlanningVariable       // Solver пытается работать с proxy
    private Timeslot slot;  // 💥 LazyInitializationException!
}
```

### Решение (разделить модели)
```java
// domain/model/Lesson.java (для БД)
@Entity
public class Lesson {
    @ManyToOne(fetch = FetchType.LAZY)
    private Timeslot slot;
}

// solver/domain/PlanningLesson.java (для оптимизации)
@PlanningEntity
public class PlanningLesson {
    private PlanningTimeslot slot; // Простой POJO, не JPA
}
```

## 🔄 Как это работает?

```
1. Загрузка из БД
   Database → JPA Lesson → PlanningModelMapper → PlanningLesson

2. Оптимизация
   Timefold Solver работает только с PlanningLesson (быстро, нет JPA)

3. Сохранение в БД
   PlanningLesson → PlanningModelMapper → обновление JPA Lesson → Database
```

## 📁 Структура

```
solver/
├── domain/                          # Planning Model (Pure POJOs)
│   ├── PlanningLesson.java         # @PlanningEntity (БЕЗ @Entity)
│   ├── TimetableSolution.java      # @PlanningSolution
│   ├── PlanningTimeslot.java       # Problem Fact
│   └── ...
├── mapper/                          # Конвертер JPA ↔ Planning
│   └── PlanningModelMapper.java    # toPlanningSolution(), toPersistableLessons()
└── DanceScheduleConstraintProvider.java # Работает с PlanningLesson
```

## ✅ Преимущества

| Что улучшилось | Как |
|----------------|-----|
| **Производительность** | Solver клонирует легкие POJO, а не тяжелые JPA entities |
| **Стабильность** | Нет LazyInitializationException (все данные в памяти) |
| **Чистота** | JPA для БД, Timefold для оптимизации (разные задачи) |
| **Масштабируемость** | Solver не обращается к БД во время работы |

## 🎓 Для комиссии (1 предложение)

> "Я применил архитектурный паттерн Separated Planning Model для изоляции Timefold Solver от JPA, что устраняет Hibernate proxy overhead и LazyInitializationException во время оптимизации."

## 📚 Документация

- `docs/refactoring/WHY_SOLVER_DOMAIN_MAPPER.md` — подробное объяснение
- `docs/refactoring/ARCHITECTURE_DIAGRAM.md` — визуальная диаграмма
- `docs/refactoring/REFACTORING_PLANNING_MODEL.md` — детали рефакторинга

## 💡 Нужно ли что-то менять?

**НЕТ!** ❌

Эта архитектура:
- ✅ Правильная (best practice)
- ✅ Оптимальная (решает проблемы производительности)
- ✅ Совместима с нашими критичными исправлениями (Этап 1-2)

**Оставляем как есть!**

