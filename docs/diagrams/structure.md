    graph TD
    %% --- Styles ---
    classDef folder fill:#FFD700,stroke:#333,stroke-width:2px; %% Gold for folders
    classDef java fill:#90EE90,stroke:#333,stroke-width:1px;   %% LightGreen for Java
    classDef config fill:#ADD8E6,stroke:#333,stroke-width:1px; %% LightBlue for Config/Res
    classDef root fill:#FFA500,stroke:#333,stroke-width:2px,color:white; %% Orange for Root

    %%Root
    Root[backend/]:::root
    
    %% Main Branches
    Src[src/main/java/.../dancescheduler/]:::folder
    Resources[src/main/resources/]:::folder
    Test[src/test/]:::folder
    RootFiles[Root Files]:::folder

    Root --> Src
    Root --> Resources
    Root --> Test
    Root --> RootFiles

    %% --- CONFIG PACKAGE ---
    subgraph ConfigPkg [config]
        direction TB
        OpenApi[OpenApiConfig.java]:::java
        SecConf[SecurityConfig.java]:::java
        WebConf[WebConfig.java]:::java
    end
    Src --> ConfigPkg

    %% --- CONTROLLER PACKAGE ---
    subgraph CtrlPkg [controller]
        direction TB
        AuthCtrl[AuthController.java]:::java
        LessonCtrl[LessonController.java]:::java
        SolverCtrl[SolverController.java]:::java
        AnalyticsCtrl[AnalyticsController.java]:::java
    end
    Src --> CtrlPkg

    %% --- DOMAIN PACKAGE ---
    subgraph DomainPkg [domain]
        direction TB
        
        subgraph ModelPkg [model]
            AbsUser[AbstractUser.java]:::java
            Teacher[Teacher.java]:::java
            Student[Student.java]:::java
            Lesson[Lesson.java]:::java
            Timeslot[Timeslot.java]:::java
            Room[Room.java]:::java
        end

        subgraph DtoPkg [dto]
            AuthReq[AuthRequest.java]:::java
            LessonDTO[LessonDTO.java]:::java
            TeacherDTO[TeacherDTO.java]:::java
            SchedResp[ScheduleResponse.java]:::java
        end
        
        subgraph MapperPkg [mapper]
            LMapper[LessonMapper.java]:::java
        end

        RepoPkg[repository/]:::folder
    end
    Src --> DomainPkg

    %% --- SECURITY PACKAGE ---
    subgraph SecPkg [security]
        direction TB
        JwtSvc[JwtService.java]:::java
        JwtFilter[JwtAuthenticationFilter.java]:::java
    end
    Src --> SecPkg

    %% --- SERVICE PACKAGE ---
    subgraph SvcPkg [service]
        direction TB
        LessonSvc[LessonService.java]:::java
        ValidSvc[ValidationService.java]:::java
        RecSvc[RecommendationService.java]:::java
        BookSvc[BookingService.java]:::java
    end
    Src --> SvcPkg

    %% --- SOLVER PACKAGE ---
    subgraph SolverPkg [solver]
        direction TB
        DSched[DanceSchedule.java]:::java
        Constraint[DanceScheduleConstraintProvider.java]:::java
    end
    Src --> SolverPkg

    %% --- RESOURCES ---
    AppYml[application.yml]:::config
    Resources --> AppYml
    
    subgraph Migrations [db/migration]
        V1[V1__init_schema.sql]:::config
        V2[V2__create_users.sql]:::config
    end
    Resources --> Migrations

    %% --- ROOT FILES ---
    Dock[Dockerfile]:::config
    Pom[pom.xml]:::config
    RootFiles --> Dock
    RootFiles --> Pom