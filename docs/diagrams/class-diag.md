    classDiagram
    %% --- API Layer ---
    namespace API_Layer {
        class LessonController {
            <<RestController>>
            +getLessons() List~LessonDTO~
            +createLesson(dto: LessonDTO) void
            +solveSchedule() String
        }
    }

    %% --- Service Layer ---
    namespace Service_Layer {
        class LessonService {
            <<Service>>
            +findAll() List~Lesson~
            +save(lesson: Lesson) Lesson
            +delete(id: Long) void
        }
        class SolverService {
            <<Service>>
            +solve(problemId: Long)
        }
    }

    %% --- Data Transfer Layer ---
    namespace DTO_Layer {
        class LessonDTO {
            <<DTO>>
            +id : Long
            +durationMinutes : int
            +pinned : boolean
            +teacherId : Long
            +teacherName : String
            +roomId : Long
            +roomName : String
            +timeslotId : Long
        }
        class LessonMapper {
            <<Interface>>
            +toDTO(entity: Lesson) LessonDTO
            +toEntity(dto: LessonDTO) Lesson
        }
    }

    %% --- Domain Persistence Layer ---
    namespace Timefold_Core {
        class DanceSchedule {
            <<PlanningSolution>>
            +score : HardSoftScore
            +solverStatus : SolverStatus
            +solve()
        }
        class Lesson {
            <<PlanningEntity>>
            +id : Long
            +pinned : boolean
            +durationMinutes : int
            +timeslot : Timeslot
            +room : Room
        }
        class Timeslot {
            <<ProblemFact>>
            +dayOfWeek : DayOfWeek
            +startTime : LocalTime
            +endTime : LocalTime
        }
        class Room {
            <<ProblemFact>>
            +name : String
            +capacity : int
            +allowsParallelPrivate : boolean
        }
    }

    namespace Users_IAM {
        class AbstractUser {
            <<MappedSuperclass>>
            +id : Long
            +email : String
            +passwordHash : String
            +fullName : String
            +role : Role
        }
        class Teacher {
            <<Entity>>
            +maxDailyHours : int
            +colorCode : String
        }
        class Student {
            <<Entity>>
            +birthDate : LocalDate
            +danceLevel : DanceLevel
        }
        class Admin {
            <<Entity>>
        }
    }

    namespace Business_Operations {
        class DanceGroup {
            <<ProblemFact>>
            +name : String
            +minSize : int
            +targetAge : String
        }
        class Booking {
            <<Entity>>
            +createdAt : LocalDateTime
            +status : BookingStatus
        }
        class ResourceUnavailability {
            <<ProblemFact>>
        }
    }

    %% --- Relationships ---
    LessonController ..> LessonService : uses
    LessonController ..> SolverService : uses
    LessonService --> Lesson : manages
    LessonService ..> LessonMapper : uses
    LessonMapper ..> Lesson : maps
    LessonMapper ..> LessonDTO : creates

    DanceSchedule "1" *-- "*" Lesson : contains
    DanceSchedule "1" *-- "*" Timeslot : contains
    DanceSchedule "1" *-- "*" Room : contains
    DanceSchedule "1" *-- "*" Teacher : contains

    Lesson "*" --> "0..1" Timeslot : assigned to
    Lesson "*" --> "0..1" Room : assigned to
    Lesson "*" --> "1" Teacher : taught by
    Lesson "*" --> "1" DanceGroup : defines content

    AbstractUser <|-- Teacher
    AbstractUser <|-- Student
    AbstractUser <|-- Admin

    Teacher "1" *-- "*" ResourceUnavailability : has
    Booking "*" --> "1" Lesson : reserves
    Booking "*" --> "1" Student : made by
