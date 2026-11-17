-- MySQL dump 10.13  Distrib 8.4.7, for Linux (x86_64)
--
-- Host: localhost    Database: timetable_db
-- ------------------------------------------------------
-- Server version	8.4.7

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `added_lessons`
--

DROP TABLE IF EXISTS `added_lessons`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `added_lessons` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `schedule_id` bigint NOT NULL,
  `teacher_id` bigint NOT NULL,
  `dance_group_id` bigint DEFAULT NULL,
  `student_id` bigint DEFAULT NULL,
  `timeslot_id` bigint DEFAULT NULL,
  `room_id` bigint DEFAULT NULL,
  `duration_minutes` int NOT NULL DEFAULT '60',
  `is_pinned` tinyint(1) NOT NULL DEFAULT '0',
  `is_private` tinyint(1) NOT NULL DEFAULT '0',
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `fk_added_lessons_group` (`dance_group_id`),
  KEY `fk_added_lessons_timeslot` (`timeslot_id`),
  KEY `fk_added_lessons_room` (`room_id`),
  KEY `idx_added_lessons_schedule` (`schedule_id`),
  KEY `idx_added_lessons_teacher` (`teacher_id`),
  KEY `idx_added_lessons_student` (`student_id`),
  CONSTRAINT `fk_added_lessons_group` FOREIGN KEY (`dance_group_id`) REFERENCES `dance_groups` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_added_lessons_room` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_added_lessons_schedule` FOREIGN KEY (`schedule_id`) REFERENCES `schedules` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_added_lessons_student` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_added_lessons_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `teachers` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_added_lessons_timeslot` FOREIGN KEY (`timeslot_id`) REFERENCES `timeslots` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `added_lessons`
--

LOCK TABLES `added_lessons` WRITE;
/*!40000 ALTER TABLE `added_lessons` DISABLE KEYS */;
/*!40000 ALTER TABLE `added_lessons` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `admins`
--

DROP TABLE IF EXISTS `admins`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admins` (
  `id` bigint NOT NULL,
  `has_billing_access` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_admins_user` FOREIGN KEY (`id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `admins`
--

LOCK TABLES `admins` WRITE;
/*!40000 ALTER TABLE `admins` DISABLE KEYS */;
/*!40000 ALTER TABLE `admins` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dance_groups`
--

DROP TABLE IF EXISTS `dance_groups`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dance_groups` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `dance_style_id` bigint DEFAULT NULL,
  `dance_level` varchar(50) DEFAULT NULL,
  `min_size` int DEFAULT NULL,
  `target_age_range` varchar(100) DEFAULT NULL,
  `version` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `fk_dancegroup_style` (`dance_style_id`),
  CONSTRAINT `fk_dancegroup_style` FOREIGN KEY (`dance_style_id`) REFERENCES `dance_styles` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dance_groups`
--

LOCK TABLES `dance_groups` WRITE;
/*!40000 ALTER TABLE `dance_groups` DISABLE KEYS */;
INSERT INTO `dance_groups` VALUES (1,'Contemporary Beginners with Maya',5,'BEGINNER',1,'16-25',3),(2,'Hip-Hop Intermediate withAlex',4,'INTERMEDIATE',1,'30-40',4);
/*!40000 ALTER TABLE `dance_groups` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dance_styles`
--

DROP TABLE IF EXISTS `dance_styles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dance_styles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dance_styles`
--

LOCK TABLES `dance_styles` WRITE;
/*!40000 ALTER TABLE `dance_styles` DISABLE KEYS */;
INSERT INTO `dance_styles` VALUES (2,'Bachata'),(7,'Ballroom'),(5,'Contemporary'),(4,'Hip Hop'),(6,'Jazz Funk'),(3,'Kizomba'),(8,'Latin'),(1,'Salsa');
/*!40000 ALTER TABLE `dance_styles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flyway_schema_history`
--

DROP TABLE IF EXISTS `flyway_schema_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flyway_schema_history` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) DEFAULT NULL,
  `description` varchar(200) NOT NULL,
  `type` varchar(20) NOT NULL,
  `script` varchar(1000) NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flyway_schema_history`
--

LOCK TABLES `flyway_schema_history` WRITE;
/*!40000 ALTER TABLE `flyway_schema_history` DISABLE KEYS */;
INSERT INTO `flyway_schema_history` VALUES (1,'1','init','SQL','V1__init.sql',1031083567,'timetable_user','2026-11-16 23:31:42',191,1),(2,'2','dictionaries','SQL','V2__dictionaries.sql',-1706262744,'timetable_user','2026-11-16 23:31:42',103,1),(3,'3','solver entities','SQL','V3__solver_entities.sql',1219355446,'timetable_user','2026-11-16 23:31:42',246,1),(4,'4','create schedules table','SQL','V4__create_schedules_table.sql',-1112051812,'timetable_user','2026-11-16 23:31:42',89,1),(5,'5','add version for optimistic locking','SQL','V5__add_version_for_optimistic_locking.sql',-631110924,'timetable_user','2026-11-16 23:31:42',239,1),(6,'6','insert default roles','SQL','V6__insert_default_roles.sql',-606340517,'timetable_user','2026-11-16 23:31:42',5,1),(7,'7','insert default dance styles','SQL','V7__insert_default_dance_styles.sql',-2143332687,'timetable_user','2026-11-16 23:31:42',9,1),(8,'8','create scheduled lessons table','SQL','V8__create_scheduled_lessons_table.sql',718752045,'timetable_user','2026-11-16 23:31:43',127,1),(9,'9','scheduled lessons status and constraints','SQL','V9__scheduled_lessons_status_and_constraints.sql',138838794,'timetable_user','2026-11-16 23:31:43',126,1),(10,'10','add is active to lessons','SQL','V10__add_is_active_to_lessons.sql',451095045,'timetable_user','2026-11-16 23:31:43',94,1),(11,'11','refactor user availability','SQL','V11__refactor_user_availability.sql',778240773,'timetable_user','2026-11-16 23:31:43',86,1),(12,'12','add availability indices','SQL','V12__add_availability_indices.sql',-2086236325,'timetable_user','2026-11-16 23:31:43',47,1),(13,'13','add student to lessons','SQL','V13__add_student_to_lessons.sql',-2050208040,'timetable_user','2026-11-16 23:31:43',206,1),(14,'14','teacher students relation','SQL','V14__teacher_students_relation.sql',1088645218,'timetable_user','2026-11-16 23:31:43',69,1),(15,'15','add student to scheduled lessons','SQL','V15__add_student_to_scheduled_lessons.sql',1561060068,'timetable_user','2026-11-16 23:31:43',121,1),(16,'16','add score explanation','SQL','V16__add_score_explanation.sql',-940184933,'timetable_user','2026-11-16 23:31:43',69,1),(17,'17','student group enrollment','SQL','V17__student_group_enrollment.sql',-1649753038,'timetable_user','2026-11-16 23:31:44',69,1),(18,'18','add cancellation to scheduled lessons','SQL','V18__add_cancellation_to_scheduled_lessons.sql',1075412969,'timetable_user','2026-11-16 23:31:44',138,1),(19,'19','add desired lessons per week','SQL','V19__add_desired_lessons_per_week.sql',980761071,'timetable_user','2026-11-16 23:31:44',99,1),(20,'20','add teacher contact fields','SQL','V20__add_teacher_contact_fields.sql',-1998330667,'timetable_user','2026-11-16 23:31:44',102,1),(21,'21','create added lessons and extend scheduled lessons','SQL','V21__create_added_lessons_and_extend_scheduled_lessons.sql',-2128279864,'timetable_user','2026-11-16 23:31:44',321,1),(22,'22','support dual role teacher student','SQL','V22__support_dual_role_teacher_student.sql',-1684945670,'timetable_user','2026-11-16 23:31:45',242,1),(23,'23','add dance style to lessons','SQL','V23__add_dance_style_to_lessons.sql',-2027585328,'timetable_user','2026-11-16 23:31:45',188,1),(24,'24','add schedule solver revision','SQL','V24__add_schedule_solver_revision.sql',-777133619,'timetable_user','2026-11-16 23:31:45',67,1),(25,'25','create schedule publication lock','SQL','V25__create_schedule_publication_lock.sql',-1713953910,'timetable_user','2026-11-16 23:31:45',26,1),(26,'26','create solver settings','SQL','V26__create_solver_settings.sql',303261977,'timetable_user','2026-11-16 23:31:45',61,1);
/*!40000 ALTER TABLE `flyway_schema_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `lessons`
--

DROP TABLE IF EXISTS `lessons`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lessons` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `teacher_id` bigint NOT NULL,
  `dance_group_id` bigint DEFAULT NULL,
  `timeslot_id` bigint DEFAULT NULL,
  `room_id` bigint DEFAULT NULL,
  `duration_minutes` int NOT NULL DEFAULT '60',
  `is_pinned` tinyint(1) NOT NULL DEFAULT '0',
  `is_private` tinyint(1) NOT NULL DEFAULT '0',
  `version` int NOT NULL DEFAULT '0',
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `student_id` bigint DEFAULT NULL,
  `dance_style_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_lesson_group` (`dance_group_id`),
  KEY `idx_lessons_teacher` (`teacher_id`),
  KEY `idx_lessons_timeslot` (`timeslot_id`),
  KEY `idx_lessons_room` (`room_id`),
  KEY `idx_lessons_is_active` (`is_active`),
  KEY `idx_lessons_student` (`student_id`),
  KEY `fk_lesson_dance_style` (`dance_style_id`),
  CONSTRAINT `fk_lesson_dance_style` FOREIGN KEY (`dance_style_id`) REFERENCES `dance_styles` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_lesson_group` FOREIGN KEY (`dance_group_id`) REFERENCES `dance_groups` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_lesson_room` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_lesson_student` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_lesson_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `teachers` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_lesson_timeslot` FOREIGN KEY (`timeslot_id`) REFERENCES `timeslots` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `lessons`
--

LOCK TABLES `lessons` WRITE;
/*!40000 ALTER TABLE `lessons` DISABLE KEYS */;
INSERT INTO `lessons` VALUES (1,2,NULL,NULL,1,60,0,1,1,1,NULL,5),(2,2,NULL,NULL,1,60,0,1,1,1,NULL,5),(3,2,NULL,NULL,1,60,0,1,1,1,NULL,5),(6,3,NULL,NULL,1,60,0,1,1,1,NULL,1),(7,3,NULL,NULL,1,60,0,1,3,1,NULL,1),(11,4,NULL,NULL,1,60,0,1,1,1,NULL,8),(12,4,NULL,NULL,1,60,0,1,1,1,NULL,8),(13,4,NULL,NULL,1,60,0,1,1,1,NULL,8),(16,2,1,27,1,60,1,0,2,1,NULL,NULL),(17,3,2,52,1,60,1,0,1,1,NULL,NULL),(19,3,NULL,NULL,1,60,0,1,0,1,NULL,1);
/*!40000 ALTER TABLE `lessons` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `resource_unavailability`
--

DROP TABLE IF EXISTS `resource_unavailability`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `resource_unavailability` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `unavailability_date` date NOT NULL,
  `start_time` time NOT NULL,
  `end_time` time NOT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `availability_role` varchar(20) NOT NULL DEFAULT 'TEACHER',
  PRIMARY KEY (`id`),
  KEY `idx_resource_unavailability_user_id` (`user_id`),
  KEY `idx_ru_user_role_date` (`user_id`,`availability_role`,`unavailability_date`),
  CONSTRAINT `fk_ru_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `resource_unavailability`
--

LOCK TABLES `resource_unavailability` WRITE;
/*!40000 ALTER TABLE `resource_unavailability` DISABLE KEYS */;
/*!40000 ALTER TABLE `resource_unavailability` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `roles`
--

DROP TABLE IF EXISTS `roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `roles`
--

LOCK TABLES `roles` WRITE;
/*!40000 ALTER TABLE `roles` DISABLE KEYS */;
INSERT INTO `roles` VALUES (3,'ADMIN'),(1,'STUDENT'),(2,'TEACHER');
/*!40000 ALTER TABLE `roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rooms`
--

DROP TABLE IF EXISTS `rooms`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rooms` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `capacity` int NOT NULL,
  `allows_parallel_private` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rooms`
--

LOCK TABLES `rooms` WRITE;
/*!40000 ALTER TABLE `rooms` DISABLE KEYS */;
INSERT INTO `rooms` VALUES (1,'Studio',999,1);
/*!40000 ALTER TABLE `rooms` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `schedule_publication_lock`
--

DROP TABLE IF EXISTS `schedule_publication_lock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `schedule_publication_lock` (
  `id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `schedule_publication_lock`
--

LOCK TABLES `schedule_publication_lock` WRITE;
/*!40000 ALTER TABLE `schedule_publication_lock` DISABLE KEYS */;
INSERT INTO `schedule_publication_lock` VALUES (1);
/*!40000 ALTER TABLE `schedule_publication_lock` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `scheduled_lessons`
--

DROP TABLE IF EXISTS `scheduled_lessons`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `scheduled_lessons` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `lesson_id` bigint DEFAULT NULL,
  `schedule_id` bigint NOT NULL,
  `timeslot_id` bigint DEFAULT NULL,
  `status` enum('ASSIGNED','UNASSIGNED') NOT NULL,
  `room_id` bigint DEFAULT NULL,
  `student_id` bigint DEFAULT NULL,
  `is_cancelled` tinyint(1) NOT NULL DEFAULT '0',
  `cancelled_by` bigint DEFAULT NULL,
  `cancelled_at` datetime(6) DEFAULT NULL,
  `cancel_reason` varchar(255) DEFAULT NULL,
  `added_lesson_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scheduled_lessons_schedule_lesson` (`schedule_id`,`lesson_id`),
  UNIQUE KEY `uk_scheduled_lessons_schedule_added_lesson` (`schedule_id`,`added_lesson_id`),
  KEY `idx_scheduled_lessons_schedule` (`schedule_id`),
  KEY `idx_scheduled_lessons_lesson` (`lesson_id`),
  KEY `idx_scheduled_lessons_timeslot` (`timeslot_id`),
  KEY `idx_scheduled_lessons_room` (`room_id`),
  KEY `idx_scheduled_lessons_status` (`status`),
  KEY `idx_scheduled_lessons_student` (`student_id`),
  KEY `fk_scheduled_lessons_cancelled_by` (`cancelled_by`),
  KEY `idx_scheduled_lessons_cancelled` (`is_cancelled`),
  KEY `idx_scheduled_lessons_added_lesson` (`added_lesson_id`),
  CONSTRAINT `fk_scheduled_lessons_added_lesson` FOREIGN KEY (`added_lesson_id`) REFERENCES `added_lessons` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_scheduled_lessons_cancelled_by` FOREIGN KEY (`cancelled_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_scheduled_lessons_lesson` FOREIGN KEY (`lesson_id`) REFERENCES `lessons` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_scheduled_lessons_room` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`),
  CONSTRAINT `fk_scheduled_lessons_schedule` FOREIGN KEY (`schedule_id`) REFERENCES `schedules` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_scheduled_lessons_student` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_scheduled_lessons_timeslot` FOREIGN KEY (`timeslot_id`) REFERENCES `timeslots` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `scheduled_lessons`
--

LOCK TABLES `scheduled_lessons` WRITE;
/*!40000 ALTER TABLE `scheduled_lessons` DISABLE KEYS */;
INSERT INTO `scheduled_lessons` VALUES (1,1,1,26,'ASSIGNED',1,5,0,NULL,NULL,NULL,NULL),(2,2,1,50,'ASSIGNED',1,6,0,NULL,NULL,NULL,NULL),(3,6,1,28,'ASSIGNED',1,4,0,NULL,NULL,NULL,NULL),(4,7,1,53,'ASSIGNED',1,4,0,NULL,NULL,NULL,NULL),(5,11,1,39,'ASSIGNED',1,5,0,NULL,NULL,NULL,NULL),(6,16,1,27,'ASSIGNED',1,NULL,0,NULL,NULL,NULL,NULL),(7,17,1,52,'ASSIGNED',1,NULL,0,NULL,NULL,NULL,NULL),(8,1,2,26,'ASSIGNED',1,5,0,NULL,NULL,NULL,NULL),(9,2,2,50,'ASSIGNED',1,6,0,NULL,NULL,NULL,NULL),(10,6,2,28,'ASSIGNED',1,4,0,NULL,NULL,NULL,NULL),(11,7,2,53,'ASSIGNED',1,4,0,NULL,NULL,NULL,NULL),(12,11,2,39,'ASSIGNED',1,5,0,NULL,NULL,NULL,NULL),(13,16,2,27,'ASSIGNED',1,NULL,0,NULL,NULL,NULL,NULL),(14,17,2,52,'ASSIGNED',1,NULL,0,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `scheduled_lessons` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `schedules`
--

DROP TABLE IF EXISTS `schedules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `schedules` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Human-readable schedule name (e.g., "Fall 2025", "Draft 1")',
  `valid_from` date NOT NULL COMMENT 'First date this schedule is valid/active',
  `valid_to` date NOT NULL COMMENT 'Last date this schedule is valid/active',
  `created_at` datetime NOT NULL COMMENT 'Timestamp when schedule was created',
  `status` enum('DRAFT','PUBLISHED','ARCHIVED') COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Lifecycle status of the schedule',
  `solver_score` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Timefold Solver score (e.g., "0hard/-200soft")',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Optional notes or description',
  `score_explanation` text COLLATE utf8mb4_unicode_ci,
  `solver_revision` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_schedules_status` (`status`),
  KEY `idx_schedules_dates` (`valid_from`,`valid_to`),
  KEY `idx_schedules_created_at` (`created_at` DESC),
  CONSTRAINT `chk_valid_dates` CHECK ((`valid_to` >= `valid_from`))
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Stores metadata for schedule versions, allowing version control and history tracking';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `schedules`
--

LOCK TABLES `schedules` WRITE;
/*!40000 ALTER TABLE `schedules` DISABLE KEYS */;
INSERT INTO `schedules` VALUES (1,'Week of 15 Nov 2026','2026-11-15','2026-11-21','2026-11-17 20:30:22','PUBLISHED','0hard/698soft',NULL,'[{\"constraintName\":\"Balance teacher workload\",\"hardScore\":0,\"softScore\":-57,\"matchCount\":3,\"details\":[{\"hardImpact\":0,\"softImpact\":-27,\"description\":\"Balance teacher workload (Desired: 3 lessons/week, Assigned count: 3)\",\"involvedUsers\":[\"Teacher: Maya Cohen\"],\"lessonInfo\":null,\"timeslotInfo\":null},{\"hardImpact\":0,\"softImpact\":-27,\"description\":\"Balance teacher workload (Desired: 3 lessons/week, Assigned count: 3)\",\"involvedUsers\":[\"Teacher: Alex Levi\"],\"lessonInfo\":null,\"timeslotInfo\":null},{\"hardImpact\":0,\"softImpact\":-3,\"description\":\"Balance teacher workload (Desired: 3 lessons/week, Assigned count: 1)\",\"involvedUsers\":[\"Teacher: Sarah Mizrahi\"],\"lessonInfo\":null,\"timeslotInfo\":null}]},{\"constraintName\":\"Reward prime time usage\",\"hardScore\":0,\"softScore\":105,\"matchCount\":7,\"details\":[{\"hardImpact\":0,\"softImpact\":15,\"description\":\"Reward prime time usage\",\"involvedUsers\":[\"Teacher: Maya Cohen\",\"Student: Noa Katz\"],\"lessonInfo\":\"Private lesson #1 (Contemporary)\",\"timeslotInfo\":\"MONDAY 16:00ΓÇô17:00\"},{\"hardImpact\":0,\"softImpact\":15,\"description\":\"Reward prime time usage\",\"involvedUsers\":[\"Teacher: Maya Cohen\",\"Student: Daniel Golan\"],\"lessonInfo\":\"Private lesson #2 (Contemporary)\",\"timeslotInfo\":\"WEDNESDAY 16:00ΓÇô17:00\"},{\"hardImpact\":0,\"softImpact\":15,\"description\":\"Reward prime time usage\",\"involvedUsers\":[\"Teacher: Alex Levi\",\"Student: Sarah Mizrahi\"],\"lessonInfo\":\"Private lesson #3 (Salsa)\",\"timeslotInfo\":\"MONDAY 18:00ΓÇô19:00\"},{\"hardImpact\":0,\"softImpact\":15,\"description\":\"Reward prime time usage\",\"involvedUsers\":[\"Teacher: Alex Levi\",\"Student: Sarah Mizrahi\"],\"lessonInfo\":\"Private lesson #4 (Salsa)\",\"timeslotInfo\":\"WEDNESDAY 19:00ΓÇô20:00\"},{\"hardImpact\":0,\"softImpact\":15,\"description\":\"Reward prime time usage\",\"involvedUsers\":[\"Teacher: Sarah Mizrahi\",\"Student: Noa Katz\"],\"lessonInfo\":\"Private lesson #5 (Latin)\",\"timeslotInfo\":\"TUESDAY 17:00ΓÇô18:00\"},{\"hardImpact\":0,\"softImpact\":15,\"description\":\"Reward prime time usage\",\"involvedUsers\":[\"Teacher: Maya Cohen\"],\"lessonInfo\":\"Group: Contemporary Beginners with Maya\",\"timeslotInfo\":\"MONDAY 17:00ΓÇô18:00\"},{\"hardImpact\":0,\"softImpact\":15,\"description\":\"Reward prime time usage\",\"involvedUsers\":[\"Teacher: Alex Levi\"],\"lessonInfo\":\"Group: Hip-Hop Intermediate withAlex\",\"timeslotInfo\":\"WEDNESDAY 18:00ΓÇô19:00\"}]},{\"constraintName\":\"Student should meet desired lessons per week\",\"hardScore\":0,\"softScore\":175,\"matchCount\":3,\"details\":[{\"hardImpact\":0,\"softImpact\":35,\"description\":\"Student should meet desired lessons per week (Desired: 3 lessons/week, Assigned count: 1)\",\"involvedUsers\":[\"Student: Daniel Golan\"],\"lessonInfo\":null,\"timeslotInfo\":null},{\"hardImpact\":0,\"softImpact\":70,\"description\":\"Student should meet desired lessons per week (Desired: 3 lessons/week, Assigned count: 2)\",\"involvedUsers\":[\"Student: Noa Katz\"],\"lessonInfo\":null,\"timeslotInfo\":null},{\"hardImpact\":0,\"softImpact\":70,\"description\":\"Student should meet desired lessons per week (Desired: 3 lessons/week, Assigned count: 2)\",\"involvedUsers\":[\"Student: Sarah Mizrahi\"],\"lessonInfo\":null,\"timeslotInfo\":null}]},{\"constraintName\":\"Teacher should meet desired lessons per week\",\"hardScore\":0,\"softScore\":175,\"matchCount\":3,\"details\":[{\"hardImpact\":0,\"softImpact\":25,\"description\":\"Teacher should meet desired lessons per week (Desired: 3 lessons/week, Assigned count: 1)\",\"involvedUsers\":[\"Teacher: Sarah Mizrahi\"],\"lessonInfo\":null,\"timeslotInfo\":null},{\"hardImpact\":0,\"softImpact\":75,\"description\":\"Teacher should meet desired lessons per week (Desired: 3 lessons/week, Assigned count: 3)\",\"involvedUsers\":[\"Teacher: Maya Cohen\"],\"lessonInfo\":null,\"timeslotInfo\":null},{\"hardImpact\":0,\"softImpact\":75,\"description\":\"Teacher should meet desired lessons per week (Desired: 3 lessons/week, Assigned count: 3)\",\"involvedUsers\":[\"Teacher: Alex Levi\"],\"lessonInfo\":null,\"timeslotInfo\":null}]},{\"constraintName\":\"Reward for assigning a student to a private lesson\",\"hardScore\":0,\"softScore\":300,\"matchCount\":5,\"details\":[{\"hardImpact\":0,\"softImpact\":60,\"description\":\"Reward for assigning a student to a private lesson\",\"involvedUsers\":[\"Teacher: Maya Cohen\",\"Student: Noa Katz\"],\"lessonInfo\":\"Private lesson #1 (Contemporary)\",\"timeslotInfo\":\"MONDAY 16:00ΓÇô17:00\"},{\"hardImpact\":0,\"softImpact\":60,\"description\":\"Reward for assigning a student to a private lesson\",\"involvedUsers\":[\"Teacher: Maya Cohen\",\"Student: Daniel Golan\"],\"lessonInfo\":\"Private lesson #2 (Contemporary)\",\"timeslotInfo\":\"WEDNESDAY 16:00ΓÇô17:00\"},{\"hardImpact\":0,\"softImpact\":60,\"description\":\"Reward for assigning a student to a private lesson\",\"involvedUsers\":[\"Teacher: Alex Levi\",\"Student: Sarah Mizrahi\"],\"lessonInfo\":\"Private lesson #3 (Salsa)\",\"timeslotInfo\":\"MONDAY 18:00ΓÇô19:00\"},{\"hardImpact\":0,\"softImpact\":60,\"description\":\"Reward for assigning a student to a private lesson\",\"involvedUsers\":[\"Teacher: Alex Levi\",\"Student: Sarah Mizrahi\"],\"lessonInfo\":\"Private lesson #4 (Salsa)\",\"timeslotInfo\":\"WEDNESDAY 19:00ΓÇô20:00\"},{\"hardImpact\":0,\"softImpact\":60,\"description\":\"Reward for assigning a student to a private lesson\",\"involvedUsers\":[\"Teacher: Sarah Mizrahi\",\"Student: Noa Katz\"],\"lessonInfo\":\"Private lesson #5 (Latin)\",\"timeslotInfo\":\"TUESDAY 17:00ΓÇô18:00\"}]}]',2),(2,'Week of 22 Nov 2026','2026-11-22','2026-11-28','2026-11-17 20:32:32','DRAFT','0hard/698soft',NULL,'[{\"constraintName\":\"Balance teacher workload\",\"hardScore\":0,\"softScore\":-57,\"matchCount\":3,\"details\":[{\"hardImpact\":0,\"softImpact\":-27,\"description\":\"Balance teacher workload (Desired: 3 lessons/week, Assigned count: 3)\",\"involvedUsers\":[\"Teacher: Maya Cohen\"],\"lessonInfo\":null,\"timeslotInfo\":null},{\"hardImpact\":0,\"softImpact\":-27,\"description\":\"Balance teacher workload (Desired: 3 lessons/week, Assigned count: 3)\",\"involvedUsers\":[\"Teacher: Alex Levi\"],\"lessonInfo\":null,\"timeslotInfo\":null},{\"hardImpact\":0,\"softImpact\":-3,\"description\":\"Balance teacher workload (Desired: 3 lessons/week, Assigned count: 1)\",\"involvedUsers\":[\"Teacher: Sarah Mizrahi\"],\"lessonInfo\":null,\"timeslotInfo\":null}]},{\"constraintName\":\"Reward prime time usage\",\"hardScore\":0,\"softScore\":105,\"matchCount\":7,\"details\":[{\"hardImpact\":0,\"softImpact\":15,\"description\":\"Reward prime time usage\",\"involvedUsers\":[\"Teacher: Maya Cohen\",\"Student: Noa Katz\"],\"lessonInfo\":\"Private lesson #8 (Contemporary)\",\"timeslotInfo\":\"MONDAY 16:00ΓÇô17:00\"},{\"hardImpact\":0,\"softImpact\":15,\"description\":\"Reward prime time usage\",\"involvedUsers\":[\"Teacher: Maya Cohen\",\"Student: Daniel Golan\"],\"lessonInfo\":\"Private lesson #9 (Contemporary)\",\"timeslotInfo\":\"WEDNESDAY 16:00ΓÇô17:00\"},{\"hardImpact\":0,\"softImpact\":15,\"description\":\"Reward prime time usage\",\"involvedUsers\":[\"Teacher: Alex Levi\",\"Student: Sarah Mizrahi\"],\"lessonInfo\":\"Private lesson #10 (Salsa)\",\"timeslotInfo\":\"MONDAY 18:00ΓÇô19:00\"},{\"hardImpact\":0,\"softImpact\":15,\"description\":\"Reward prime time usage\",\"involvedUsers\":[\"Teacher: Alex Levi\",\"Student: Sarah Mizrahi\"],\"lessonInfo\":\"Private lesson #11 (Salsa)\",\"timeslotInfo\":\"WEDNESDAY 19:00ΓÇô20:00\"},{\"hardImpact\":0,\"softImpact\":15,\"description\":\"Reward prime time usage\",\"involvedUsers\":[\"Teacher: Sarah Mizrahi\",\"Student: Noa Katz\"],\"lessonInfo\":\"Private lesson #12 (Latin)\",\"timeslotInfo\":\"TUESDAY 17:00ΓÇô18:00\"},{\"hardImpact\":0,\"softImpact\":15,\"description\":\"Reward prime time usage\",\"involvedUsers\":[\"Teacher: Maya Cohen\"],\"lessonInfo\":\"Group: Contemporary Beginners with Maya\",\"timeslotInfo\":\"MONDAY 17:00ΓÇô18:00\"},{\"hardImpact\":0,\"softImpact\":15,\"description\":\"Reward prime time usage\",\"involvedUsers\":[\"Teacher: Alex Levi\"],\"lessonInfo\":\"Group: Hip-Hop Intermediate withAlex\",\"timeslotInfo\":\"WEDNESDAY 18:00ΓÇô19:00\"}]},{\"constraintName\":\"Student should meet desired lessons per week\",\"hardScore\":0,\"softScore\":175,\"matchCount\":3,\"details\":[{\"hardImpact\":0,\"softImpact\":35,\"description\":\"Student should meet desired lessons per week (Desired: 3 lessons/week, Assigned count: 1)\",\"involvedUsers\":[\"Student: Daniel Golan\"],\"lessonInfo\":null,\"timeslotInfo\":null},{\"hardImpact\":0,\"softImpact\":70,\"description\":\"Student should meet desired lessons per week (Desired: 3 lessons/week, Assigned count: 2)\",\"involvedUsers\":[\"Student: Noa Katz\"],\"lessonInfo\":null,\"timeslotInfo\":null},{\"hardImpact\":0,\"softImpact\":70,\"description\":\"Student should meet desired lessons per week (Desired: 3 lessons/week, Assigned count: 2)\",\"involvedUsers\":[\"Student: Sarah Mizrahi\"],\"lessonInfo\":null,\"timeslotInfo\":null}]},{\"constraintName\":\"Teacher should meet desired lessons per week\",\"hardScore\":0,\"softScore\":175,\"matchCount\":3,\"details\":[{\"hardImpact\":0,\"softImpact\":25,\"description\":\"Teacher should meet desired lessons per week (Desired: 3 lessons/week, Assigned count: 1)\",\"involvedUsers\":[\"Teacher: Sarah Mizrahi\"],\"lessonInfo\":null,\"timeslotInfo\":null},{\"hardImpact\":0,\"softImpact\":75,\"description\":\"Teacher should meet desired lessons per week (Desired: 3 lessons/week, Assigned count: 3)\",\"involvedUsers\":[\"Teacher: Maya Cohen\"],\"lessonInfo\":null,\"timeslotInfo\":null},{\"hardImpact\":0,\"softImpact\":75,\"description\":\"Teacher should meet desired lessons per week (Desired: 3 lessons/week, Assigned count: 3)\",\"involvedUsers\":[\"Teacher: Alex Levi\"],\"lessonInfo\":null,\"timeslotInfo\":null}]},{\"constraintName\":\"Reward for assigning a student to a private lesson\",\"hardScore\":0,\"softScore\":300,\"matchCount\":5,\"details\":[{\"hardImpact\":0,\"softImpact\":60,\"description\":\"Reward for assigning a student to a private lesson\",\"involvedUsers\":[\"Teacher: Maya Cohen\",\"Student: Noa Katz\"],\"lessonInfo\":\"Private lesson #8 (Contemporary)\",\"timeslotInfo\":\"MONDAY 16:00ΓÇô17:00\"},{\"hardImpact\":0,\"softImpact\":60,\"description\":\"Reward for assigning a student to a private lesson\",\"involvedUsers\":[\"Teacher: Maya Cohen\",\"Student: Daniel Golan\"],\"lessonInfo\":\"Private lesson #9 (Contemporary)\",\"timeslotInfo\":\"WEDNESDAY 16:00ΓÇô17:00\"},{\"hardImpact\":0,\"softImpact\":60,\"description\":\"Reward for assigning a student to a private lesson\",\"involvedUsers\":[\"Teacher: Alex Levi\",\"Student: Sarah Mizrahi\"],\"lessonInfo\":\"Private lesson #10 (Salsa)\",\"timeslotInfo\":\"MONDAY 18:00ΓÇô19:00\"},{\"hardImpact\":0,\"softImpact\":60,\"description\":\"Reward for assigning a student to a private lesson\",\"involvedUsers\":[\"Teacher: Alex Levi\",\"Student: Sarah Mizrahi\"],\"lessonInfo\":\"Private lesson #11 (Salsa)\",\"timeslotInfo\":\"WEDNESDAY 19:00ΓÇô20:00\"},{\"hardImpact\":0,\"softImpact\":60,\"description\":\"Reward for assigning a student to a private lesson\",\"involvedUsers\":[\"Teacher: Sarah Mizrahi\",\"Student: Noa Katz\"],\"lessonInfo\":\"Private lesson #12 (Latin)\",\"timeslotInfo\":\"TUESDAY 17:00ΓÇô18:00\"}]}]',1);
/*!40000 ALTER TABLE `schedules` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `solver_settings_preset`
--

DROP TABLE IF EXISTS `solver_settings_preset`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `solver_settings_preset` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(120) NOT NULL,
  `settings_json` text NOT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_solver_settings_preset_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `solver_settings_preset`
--

LOCK TABLES `solver_settings_preset` WRITE;
/*!40000 ALTER TABLE `solver_settings_preset` DISABLE KEYS */;
/*!40000 ALTER TABLE `solver_settings_preset` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `solver_settings_state`
--

DROP TABLE IF EXISTS `solver_settings_state`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `solver_settings_state` (
  `id` bigint NOT NULL,
  `settings_json` text,
  `active_preset_id` bigint DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `fk_solver_settings_active_preset` (`active_preset_id`),
  CONSTRAINT `fk_solver_settings_active_preset` FOREIGN KEY (`active_preset_id`) REFERENCES `solver_settings_preset` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `solver_settings_state`
--

LOCK TABLES `solver_settings_state` WRITE;
/*!40000 ALTER TABLE `solver_settings_state` DISABLE KEYS */;
INSERT INTO `solver_settings_state` VALUES (1,NULL,NULL,0);
/*!40000 ALTER TABLE `solver_settings_state` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `student_dance_style`
--

DROP TABLE IF EXISTS `student_dance_style`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_dance_style` (
  `student_id` bigint NOT NULL,
  `dance_style_id` bigint NOT NULL,
  PRIMARY KEY (`student_id`,`dance_style_id`),
  KEY `fk_sds_style` (`dance_style_id`),
  CONSTRAINT `fk_sds_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_sds_style` FOREIGN KEY (`dance_style_id`) REFERENCES `dance_styles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student_dance_style`
--

LOCK TABLES `student_dance_style` WRITE;
/*!40000 ALTER TABLE `student_dance_style` DISABLE KEYS */;
INSERT INTO `student_dance_style` VALUES (4,8);
/*!40000 ALTER TABLE `student_dance_style` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `student_groups`
--

DROP TABLE IF EXISTS `student_groups`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_groups` (
  `student_id` bigint NOT NULL,
  `group_id` bigint NOT NULL,
  PRIMARY KEY (`student_id`,`group_id`),
  KEY `idx_sg_student` (`student_id`),
  KEY `idx_sg_group` (`group_id`),
  CONSTRAINT `fk_sg_group` FOREIGN KEY (`group_id`) REFERENCES `dance_groups` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_sg_student` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student_groups`
--

LOCK TABLES `student_groups` WRITE;
/*!40000 ALTER TABLE `student_groups` DISABLE KEYS */;
INSERT INTO `student_groups` VALUES (4,2),(5,1),(6,1),(6,2),(7,1),(7,2);
/*!40000 ALTER TABLE `student_groups` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `students`
--

DROP TABLE IF EXISTS `students`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `students` (
  `id` bigint NOT NULL,
  `birth_date` date DEFAULT NULL,
  `dance_level` varchar(50) DEFAULT NULL,
  `parent_contact` varchar(255) DEFAULT NULL,
  `desired_lessons_per_week` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_students_user` FOREIGN KEY (`id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `students`
--

LOCK TABLES `students` WRITE;
/*!40000 ALTER TABLE `students` DISABLE KEYS */;
INSERT INTO `students` VALUES (4,'2026-11-10','ELEMENTARY','99999999',3),(5,'2026-11-03','ADVANCED','123',3),(6,'2026-11-01','BEGINNER','123',3),(7,'2026-11-06','PRE_INTERMEDIATE','123',3);
/*!40000 ALTER TABLE `students` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `teacher_dance_style`
--

DROP TABLE IF EXISTS `teacher_dance_style`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teacher_dance_style` (
  `dance_style_id` bigint NOT NULL,
  `teacher_id` bigint NOT NULL,
  PRIMARY KEY (`dance_style_id`,`teacher_id`),
  KEY `fk_tds_teacher` (`teacher_id`),
  CONSTRAINT `fk_tds_dancestyle` FOREIGN KEY (`dance_style_id`) REFERENCES `dance_styles` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_tds_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `teachers` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `teacher_dance_style`
--

LOCK TABLES `teacher_dance_style` WRITE;
/*!40000 ALTER TABLE `teacher_dance_style` DISABLE KEYS */;
INSERT INTO `teacher_dance_style` VALUES (5,2),(7,2),(1,3),(4,3),(8,4);
/*!40000 ALTER TABLE `teacher_dance_style` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `teacher_students`
--

DROP TABLE IF EXISTS `teacher_students`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teacher_students` (
  `teacher_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  PRIMARY KEY (`teacher_id`,`student_id`),
  KEY `idx_ts_teacher_id` (`teacher_id`),
  KEY `idx_ts_student_id` (`student_id`),
  CONSTRAINT `fk_ts_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ts_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `teachers` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `teacher_students`
--

LOCK TABLES `teacher_students` WRITE;
/*!40000 ALTER TABLE `teacher_students` DISABLE KEYS */;
INSERT INTO `teacher_students` VALUES (2,5),(2,6),(3,4),(3,7),(4,5);
/*!40000 ALTER TABLE `teacher_students` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `teachers`
--

DROP TABLE IF EXISTS `teachers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teachers` (
  `id` bigint NOT NULL,
  `max_daily_hours` int DEFAULT NULL,
  `color_code` varchar(24) DEFAULT NULL,
  `desired_lessons_per_week` int DEFAULT NULL,
  `phone` varchar(50) DEFAULT NULL,
  `bio` text,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_teachers_user` FOREIGN KEY (`id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `teachers`
--

LOCK TABLES `teachers` WRITE;
/*!40000 ALTER TABLE `teachers` DISABLE KEYS */;
INSERT INTO `teachers` VALUES (2,8,'#AE1CF2',3,'123',NULL),(3,8,'#FA9E00',3,NULL,NULL),(4,8,'#1DED2A',3,NULL,NULL);
/*!40000 ALTER TABLE `teachers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `timeslots`
--

DROP TABLE IF EXISTS `timeslots`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `timeslots` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `day_of_week` varchar(10) NOT NULL,
  `start_time` time NOT NULL,
  `end_time` time NOT NULL,
  `version` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_timeslot` (`day_of_week`,`start_time`,`end_time`)
) ENGINE=InnoDB AUTO_INCREMENT=145 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `timeslots`
--

LOCK TABLES `timeslots` WRITE;
/*!40000 ALTER TABLE `timeslots` DISABLE KEYS */;
INSERT INTO `timeslots` VALUES (18,'MONDAY','08:00:00','09:00:00',0),(19,'MONDAY','09:00:00','10:00:00',0),(20,'MONDAY','10:00:00','11:00:00',0),(21,'MONDAY','11:00:00','12:00:00',0),(22,'MONDAY','12:00:00','13:00:00',0),(23,'MONDAY','13:00:00','14:00:00',0),(24,'MONDAY','14:00:00','15:00:00',0),(25,'MONDAY','15:00:00','16:00:00',0),(26,'MONDAY','16:00:00','17:00:00',0),(27,'MONDAY','17:00:00','18:00:00',0),(28,'MONDAY','18:00:00','19:00:00',0),(29,'MONDAY','19:00:00','20:00:00',0),(30,'TUESDAY','08:00:00','09:00:00',0),(31,'TUESDAY','09:00:00','10:00:00',0),(32,'TUESDAY','10:00:00','11:00:00',0),(33,'TUESDAY','11:00:00','12:00:00',0),(34,'TUESDAY','12:00:00','13:00:00',0),(35,'TUESDAY','13:00:00','14:00:00',0),(36,'TUESDAY','14:00:00','15:00:00',0),(37,'TUESDAY','15:00:00','16:00:00',0),(38,'TUESDAY','16:00:00','17:00:00',0),(39,'TUESDAY','17:00:00','18:00:00',0),(40,'TUESDAY','18:00:00','19:00:00',0),(41,'TUESDAY','19:00:00','20:00:00',0),(42,'WEDNESDAY','08:00:00','09:00:00',0),(43,'WEDNESDAY','09:00:00','10:00:00',0),(44,'WEDNESDAY','10:00:00','11:00:00',0),(45,'WEDNESDAY','11:00:00','12:00:00',0),(46,'WEDNESDAY','12:00:00','13:00:00',0),(47,'WEDNESDAY','13:00:00','14:00:00',0),(48,'WEDNESDAY','14:00:00','15:00:00',0),(49,'WEDNESDAY','15:00:00','16:00:00',0),(50,'WEDNESDAY','16:00:00','17:00:00',0),(51,'WEDNESDAY','17:00:00','18:00:00',0),(52,'WEDNESDAY','18:00:00','19:00:00',0),(53,'WEDNESDAY','19:00:00','20:00:00',0),(54,'THURSDAY','08:00:00','09:00:00',0),(55,'THURSDAY','09:00:00','10:00:00',0),(56,'THURSDAY','10:00:00','11:00:00',0),(57,'THURSDAY','11:00:00','12:00:00',0),(58,'THURSDAY','12:00:00','13:00:00',0),(59,'THURSDAY','13:00:00','14:00:00',0),(60,'THURSDAY','14:00:00','15:00:00',0),(61,'THURSDAY','15:00:00','16:00:00',0),(62,'THURSDAY','16:00:00','17:00:00',0),(63,'THURSDAY','17:00:00','18:00:00',0),(64,'THURSDAY','18:00:00','19:00:00',0),(65,'THURSDAY','19:00:00','20:00:00',0),(66,'FRIDAY','08:00:00','09:00:00',0),(67,'FRIDAY','09:00:00','10:00:00',0),(68,'FRIDAY','10:00:00','11:00:00',0),(69,'FRIDAY','11:00:00','12:00:00',0),(70,'FRIDAY','12:00:00','13:00:00',0),(71,'FRIDAY','13:00:00','14:00:00',0),(72,'FRIDAY','14:00:00','15:00:00',0),(73,'FRIDAY','15:00:00','16:00:00',0),(74,'FRIDAY','16:00:00','17:00:00',0),(75,'FRIDAY','17:00:00','18:00:00',0),(76,'FRIDAY','18:00:00','19:00:00',0),(77,'FRIDAY','19:00:00','20:00:00',0),(78,'SATURDAY','08:00:00','09:00:00',0),(79,'SATURDAY','09:00:00','10:00:00',0),(80,'SATURDAY','10:00:00','11:00:00',0),(81,'SATURDAY','11:00:00','12:00:00',0),(82,'SATURDAY','12:00:00','13:00:00',0),(83,'SATURDAY','13:00:00','14:00:00',0),(84,'SATURDAY','14:00:00','15:00:00',0),(85,'SATURDAY','15:00:00','16:00:00',0),(86,'SATURDAY','16:00:00','17:00:00',0),(87,'SATURDAY','17:00:00','18:00:00',0),(88,'SATURDAY','18:00:00','19:00:00',0),(89,'SATURDAY','19:00:00','20:00:00',0),(90,'SUNDAY','08:00:00','09:00:00',0),(91,'SUNDAY','09:00:00','10:00:00',0),(92,'SUNDAY','10:00:00','11:00:00',0),(93,'SUNDAY','11:00:00','12:00:00',0),(94,'SUNDAY','12:00:00','13:00:00',0),(95,'SUNDAY','13:00:00','14:00:00',0),(96,'SUNDAY','14:00:00','15:00:00',0),(97,'SUNDAY','15:00:00','16:00:00',0),(98,'SUNDAY','16:00:00','17:00:00',0),(99,'SUNDAY','17:00:00','18:00:00',0),(100,'SUNDAY','18:00:00','19:00:00',0),(101,'SUNDAY','19:00:00','20:00:00',0);
/*!40000 ALTER TABLE `timeslots` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_roles`
--

DROP TABLE IF EXISTS `user_roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_roles` (
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`user_id`,`role_id`),
  KEY `idx_user_roles_role_id` (`role_id`),
  CONSTRAINT `fk_ur_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`),
  CONSTRAINT `fk_ur_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_roles`
--

LOCK TABLES `user_roles` WRITE;
/*!40000 ALTER TABLE `user_roles` DISABLE KEYS */;
INSERT INTO `user_roles` VALUES (1,1),(4,1),(5,1),(6,1),(7,1),(2,2),(3,2),(4,2);
/*!40000 ALTER TABLE `user_roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(255) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `full_name` varchar(255) DEFAULT NULL,
  `role_id` bigint NOT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`),
  KEY `idx_users_email` (`email`),
  KEY `idx_users_role_id` (`role_id`),
  CONSTRAINT `fk_users_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'admin@gmail.com','$argon2id$v=19$m=16384,t=2,p=1$5gBWBUgo0srPb3qsID3Q8g$w4v+xOfQJiQvPQhe+G2rrmwLpRByuNUS045eXk+Y/C4','admin',3,1),(2,'maya.teacher@studio.com','$argon2id$v=19$m=16384,t=2,p=1$qxTqYB+AN4lh1N84xKfy3A$1oQOh7H/koZRI3aZV3jW9dFXTxzOA0h8YvekzJKUK0Q','Maya Cohen',2,1),(3,'alex.teacher@studio.com','$argon2id$v=19$m=16384,t=2,p=1$qaWql1o/vqJK61tBT1QI9Q$ceXvkJwpBNi2AwQXUrqbJHKC3pZmQ0JJGGTSdIBTa3I','Alex Levi',2,1),(4,'sarah.dual@studio.com','$argon2id$v=19$m=16384,t=2,p=1$dZpS/fiZNxbcZJuRcSjVag$byDfRF84AxDhNj7p/odzZcXjdYTK7r1FG5miiayz4Pc','Sarah Mizrahi',2,1),(5,'noa.student@studio.com','$argon2id$v=19$m=16384,t=2,p=1$Lgp0kvKR/1cpsu1CNcRvzw$+AeZLDO0U06eaeJTryBy0E4b6rhsrleRCdGTX2Kmt28','Noa Katz',1,1),(6,'daniel.student@studio.com','$argon2id$v=19$m=16384,t=2,p=1$DSMLCop0og1c0s8vfM5aXg$Zu1BVaGBsTnpb9O7pOGk2KrB0VFeIWR1UDtJETT4Qbs','Daniel Golan',1,1),(7,'liam.student@studio.com','$argon2id$v=19$m=16384,t=2,p=1$VJOD4kfR+ADD6kpKPh7DPA$U2mBSUbmYHT+D3yXZXgBirbvPzHaHI0akzhQd9kSOHI','Liam Ben-David',1,1);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `weekly_availability`
--

DROP TABLE IF EXISTS `weekly_availability`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `weekly_availability` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `day_of_week` varchar(20) NOT NULL,
  `start_time` time NOT NULL,
  `end_time` time NOT NULL,
  `availability_role` varchar(20) NOT NULL DEFAULT 'TEACHER',
  PRIMARY KEY (`id`),
  KEY `idx_weekly_availability_user_id` (`user_id`),
  KEY `idx_wa_user_role_day` (`user_id`,`availability_role`,`day_of_week`),
  CONSTRAINT `fk_wa_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `weekly_availability`
--

LOCK TABLES `weekly_availability` WRITE;
/*!40000 ALTER TABLE `weekly_availability` DISABLE KEYS */;
INSERT INTO `weekly_availability` VALUES (1,2,'MONDAY','16:00:00','20:00:00','TEACHER'),(2,2,'WEDNESDAY','16:00:00','19:00:00','TEACHER'),(3,3,'MONDAY','16:00:00','20:00:00','TEACHER'),(4,3,'WEDNESDAY','17:00:00','20:00:00','TEACHER'),(5,4,'TUESDAY','17:00:00','19:00:00','TEACHER'),(6,4,'MONDAY','18:00:00','20:00:00','STUDENT'),(7,4,'WEDNESDAY','18:00:00','20:00:00','STUDENT'),(8,7,'MONDAY','16:00:00','19:00:00','STUDENT'),(9,7,'WEDNESDAY','17:00:00','20:00:00','STUDENT'),(10,6,'MONDAY','17:00:00','20:00:00','STUDENT'),(11,6,'WEDNESDAY','16:00:00','19:00:00','STUDENT'),(12,5,'MONDAY','16:00:00','20:00:00','STUDENT'),(13,5,'TUESDAY','17:00:00','19:00:00','STUDENT');
/*!40000 ALTER TABLE `weekly_availability` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-17 18:36:53
