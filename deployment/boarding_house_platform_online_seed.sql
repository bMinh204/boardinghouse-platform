-- MySQL dump 10.13  Distrib 8.0.45, for Win64 (x86_64)
--
-- Host: localhost    Database: boarding_house_platform
-- ------------------------------------------------------
-- Server version	8.4.8

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
-- Table structure for table `account_activation_token`
--

DROP TABLE IF EXISTS `account_activation_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `account_activation_token` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `expiry_date` datetime(6) NOT NULL,
  `token` varchar(255) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKbxyblk406b8vbumblyda9rht2` (`token`),
  UNIQUE KEY `UKkoajxv968tet95apni6u0nj7b` (`user_id`),
  CONSTRAINT `FKpdsifs4fdn575d43by6jiyk73` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `account_activation_token`
--

LOCK TABLES `account_activation_token` WRITE;
/*!40000 ALTER TABLE `account_activation_token` DISABLE KEYS */;
/*!40000 ALTER TABLE `account_activation_token` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `chat_messages`
--

DROP TABLE IF EXISTS `chat_messages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_messages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` varchar(2000) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `conversation_id` bigint NOT NULL,
  `sender_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKc8ljv426x8fj9tcywei40stu9` (`conversation_id`),
  KEY `FKgiqeap8ays4lf684x7m0r2729` (`sender_id`),
  CONSTRAINT `FKc8ljv426x8fj9tcywei40stu9` FOREIGN KEY (`conversation_id`) REFERENCES `conversations` (`id`),
  CONSTRAINT `FKgiqeap8ays4lf684x7m0r2729` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `chat_messages`
--

LOCK TABLES `chat_messages` WRITE;
/*!40000 ALTER TABLE `chat_messages` DISABLE KEYS */;
INSERT INTO `chat_messages` VALUES (1,'Em quan tam phong nay, cuoi tuan co the xem phong khong a?','2026-03-23 21:43:25.314956',1,5),(2,'Duoc em, chieu thu 7 chi co nha. Em mang can cuoc de xem hop dong mau.','2026-03-23 21:43:25.317082',1,2),(3,'Phong da co nguoi dat coc chua anh?','2026-03-23 21:43:25.318082',2,6),(4,'Phong da co nguoi thue, nhung anh con mot phong tuong tu sap trong.','2026-03-23 21:43:25.319083',2,3),(5,'ád','2026-04-02 10:36:31.592208',1,5),(6,'xin chào','2026-04-02 13:36:54.724477',3,8);
/*!40000 ALTER TABLE `chat_messages` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `conversations`
--

DROP TABLE IF EXISTS `conversations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `conversations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `landlord_id` bigint NOT NULL,
  `room_id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_conversation_room_tenant_landlord` (`room_id`,`tenant_id`,`landlord_id`),
  KEY `FKh9tml2eyrdux0t4kie4bprf5b` (`landlord_id`),
  KEY `FK76po99nsmyv9safxthw3haibn` (`tenant_id`),
  CONSTRAINT `FK44v3vuw3vnfeewdfag4miknr3` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`),
  CONSTRAINT `FK76po99nsmyv9safxthw3haibn` FOREIGN KEY (`tenant_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKh9tml2eyrdux0t4kie4bprf5b` FOREIGN KEY (`landlord_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `conversations`
--

LOCK TABLES `conversations` WRITE;
/*!40000 ALTER TABLE `conversations` DISABLE KEYS */;
INSERT INTO `conversations` VALUES (1,'2026-03-23 21:43:25.312442','2026-05-09 11:38:19.957685',2,1,5),(2,'2026-03-23 21:43:25.317082','2026-03-23 21:43:25.317082',3,3,6),(3,'2026-04-02 13:36:54.718475','2026-04-02 13:36:54.736825',2,1,8);
/*!40000 ALTER TABLE `conversations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `favorites`
--

DROP TABLE IF EXISTS `favorites`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `favorites` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `room_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_favorite_user_room` (`user_id`,`room_id`),
  UNIQUE KEY `UKckblwk3vn1b2yn4btnhcecmce` (`tenant_id`,`room_id`),
  KEY `FKberjnu7ygmw3btmkhf0chgh1f` (`room_id`),
  CONSTRAINT `FKberjnu7ygmw3btmkhf0chgh1f` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`),
  CONSTRAINT `FKbirvpkrcw5km95d9p20kumy6b` FOREIGN KEY (`tenant_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKk7du8b8ewipawnnpg76d55fus` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `favorites`
--

LOCK TABLES `favorites` WRITE;
/*!40000 ALTER TABLE `favorites` DISABLE KEYS */;
INSERT INTO `favorites` VALUES (7,NULL,5,5,5),(8,NULL,3,5,5);
/*!40000 ALTER TABLE `favorites` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `messages`
--

DROP TABLE IF EXISTS `messages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `messages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` varchar(1000) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `conversation_id` bigint NOT NULL,
  `sender_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKt492th6wsovh1nush5yl5jj8e` (`conversation_id`),
  KEY `FK4ui4nnwntodh6wjvck53dbk9m` (`sender_id`),
  CONSTRAINT `FK4ui4nnwntodh6wjvck53dbk9m` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKt492th6wsovh1nush5yl5jj8e` FOREIGN KEY (`conversation_id`) REFERENCES `conversations` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `messages`
--

LOCK TABLES `messages` WRITE;
/*!40000 ALTER TABLE `messages` DISABLE KEYS */;
INSERT INTO `messages` VALUES (4,'Codex verify chat detail','2026-05-07 15:27:43.044276',1,5),(5,'Realtime filter setup','2026-05-09 11:31:05.839836',1,5),(6,'Realtime filter message','2026-05-09 11:31:12.334498',1,5),(7,'Realtime negative setup','2026-05-09 11:31:44.959995',1,5),(8,'Realtime negative message','2026-05-09 11:31:49.056446',1,5),(9,'Std test conversation','2026-05-09 11:36:58.482537',1,5),(10,'Std tenant message','2026-05-09 11:36:58.589977',1,5),(11,'Std realtime setup','2026-05-09 11:37:32.755851',1,5),(12,'Std realtime message','2026-05-09 11:37:38.243407',1,5),(13,'Std realtime setup 2','2026-05-09 11:38:12.154281',1,5),(14,'Std realtime message 2','2026-05-09 11:38:19.947207',1,5);
/*!40000 ALTER TABLE `messages` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `password_reset_tokens`
--

DROP TABLE IF EXISTS `password_reset_tokens`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `password_reset_tokens` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `expiry_date` datetime(6) NOT NULL,
  `token` varchar(255) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK71lqwbwtklmljk3qlsugr1mig` (`token`),
  UNIQUE KEY `UKla2ts67g4oh2sreayswhox1i6` (`user_id`),
  CONSTRAINT `FKk3ndxg5xp6v7wd4gjyusp15gq` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `password_reset_tokens`
--

LOCK TABLES `password_reset_tokens` WRITE;
/*!40000 ALTER TABLE `password_reset_tokens` DISABLE KEYS */;
INSERT INTO `password_reset_tokens` VALUES (1,'2026-05-09 11:27:40.715269','7caccde6-621c-407b-b373-bcac6452e5fa',5);
/*!40000 ALTER TABLE `password_reset_tokens` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rental_requests`
--

DROP TABLE IF EXISTS `rental_requests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rental_requests` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `move_in_date` date NOT NULL,
  `note` varchar(1000) DEFAULT NULL,
  `status` enum('APPROVED','CANCELLED','PENDING','REJECTED') NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `landlord_id` bigint NOT NULL,
  `room_id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK5615tmh7qtcoa35l7tbyxyhti` (`landlord_id`),
  KEY `FK37fcw837567yells07xxio8fs` (`room_id`),
  KEY `FKjw7dk8lajsl8sod5t80c4ki4l` (`tenant_id`),
  CONSTRAINT `FK37fcw837567yells07xxio8fs` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`),
  CONSTRAINT `FK5615tmh7qtcoa35l7tbyxyhti` FOREIGN KEY (`landlord_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKjw7dk8lajsl8sod5t80c4ki4l` FOREIGN KEY (`tenant_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rental_requests`
--

LOCK TABLES `rental_requests` WRITE;
/*!40000 ALTER TABLE `rental_requests` DISABLE KEYS */;
INSERT INTO `rental_requests` VALUES (1,'2026-03-23 21:43:25.324083','2026-03-30','Muon vao o dau thang toi, uu tien hop dong 12 thang.','PENDING','2026-03-23 21:43:25.324083',2,1,5),(2,'2026-03-23 21:43:25.325962','2026-03-26','Can phong o ngay trong tuan nay.','APPROVED','2026-03-23 21:43:25.325962',3,3,6),(3,'2026-03-23 21:43:25.327100','2026-04-06','Muon giu phong sau khi sua xong.','REJECTED','2026-03-23 21:43:25.327100',4,5,7),(7,'2026-05-09 11:36:58.645338','2026-06-10','Std rental request','PENDING','2026-05-09 11:36:58.645338',2,1,5);
/*!40000 ALTER TABLE `rental_requests` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `room_amenities`
--

DROP TABLE IF EXISTS `room_amenities`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `room_amenities` (
  `room_id` bigint NOT NULL,
  `amenity` varchar(255) DEFAULT NULL,
  KEY `FKps6ofup9gxhn8juqvproxbaud` (`room_id`),
  CONSTRAINT `FKps6ofup9gxhn8juqvproxbaud` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `room_amenities`
--

LOCK TABLES `room_amenities` WRITE;
/*!40000 ALTER TABLE `room_amenities` DISABLE KEYS */;
INSERT INTO `room_amenities` VALUES (20,'chỗ để xe có mái che');
/*!40000 ALTER TABLE `room_amenities` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `room_images`
--

DROP TABLE IF EXISTS `room_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `room_images` (
  `room_id` bigint NOT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  KEY `FKtky1jnwoh1hv50m263p2vlt0y` (`room_id`),
  CONSTRAINT `FKtky1jnwoh1hv50m263p2vlt0y` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `room_images`
--

LOCK TABLES `room_images` WRITE;
/*!40000 ALTER TABLE `room_images` DISABLE KEYS */;
INSERT INTO `room_images` VALUES (20,'/uploads/65c2b07f-c446-4b84-8f05-445e73ab97b4.png');
/*!40000 ALTER TABLE `room_images` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `room_views`
--

DROP TABLE IF EXISTS `room_views`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `room_views` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `room_id` bigint NOT NULL,
  `viewer_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKm9h2o2o7kv6e4r67vgh05ig8d` (`room_id`),
  KEY `FK2c6cy0a6b7vytftltsisl8sxb` (`viewer_id`),
  CONSTRAINT `FK2c6cy0a6b7vytftltsisl8sxb` FOREIGN KEY (`viewer_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKm9h2o2o7kv6e4r67vgh05ig8d` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `room_views`
--

LOCK TABLES `room_views` WRITE;
/*!40000 ALTER TABLE `room_views` DISABLE KEYS */;
INSERT INTO `room_views` VALUES (1,'2026-03-23 21:43:25.306443',1,5),(2,'2026-03-23 21:43:25.308445',1,6),(3,'2026-03-23 21:43:25.309442',1,7),(4,'2026-03-23 21:43:25.309442',2,5),(5,'2026-03-23 21:43:25.310443',3,6),(6,'2026-03-23 21:43:25.311444',3,7),(7,'2026-03-23 21:43:25.312442',5,5),(8,'2026-03-23 21:58:46.809094',2,2),(9,'2026-03-23 22:18:01.994343',1,2),(10,'2026-03-23 22:46:24.511928',3,5),(11,'2026-03-23 22:46:30.007531',3,5),(12,'2026-03-23 22:46:31.517854',2,5),(13,'2026-03-23 22:47:15.269693',1,5),(14,'2026-04-02 08:27:34.721905',3,NULL),(18,'2026-04-02 13:36:43.510789',1,8);
/*!40000 ALTER TABLE `room_views` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rooms`
--

DROP TABLE IF EXISTS `rooms`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rooms` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `address` varchar(255) NOT NULL,
  `amenities` varchar(500) NOT NULL,
  `area_name` varchar(255) DEFAULT NULL,
  `available_from` date NOT NULL,
  `capacity` int NOT NULL,
  `contact_phone` varchar(255) DEFAULT NULL,
  `contract_note` varchar(1000) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(2000) NOT NULL,
  `featured_image` varchar(255) DEFAULT NULL,
  `image_gallery` longtext,
  `map_query` varchar(255) DEFAULT NULL,
  `moderation_note` varchar(500) DEFAULT NULL,
  `moderation_status` enum('APPROVED','PENDING','REJECTED') NOT NULL,
  `price` bigint DEFAULT NULL,
  `property_name` varchar(255) DEFAULT NULL,
  `size` double NOT NULL,
  `status` enum('AVAILABLE','EXPIRING_SOON','MAINTENANCE','OCCUPIED') NOT NULL,
  `title` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `owner_id` bigint NOT NULL,
  `contact_count` bigint DEFAULT NULL,
  `survey_average` double DEFAULT NULL,
  `survey_count` int DEFAULT NULL,
  `view_count` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKtnhxhxjvamaungwsm0q7e010` (`owner_id`),
  CONSTRAINT `FKtnhxhxjvamaungwsm0q7e010` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rooms`
--

LOCK TABLES `rooms` WRITE;
/*!40000 ALTER TABLE `rooms` DISABLE KEYS */;
INSERT INTO `rooms` VALUES (1,'So 8 ngo 215 Tan Thinh, Thai Nguyen','','Gan DH CNTT Thai Nguyen','2026-03-25',2,'0911111111','Co hop dong 6-12 thang, coc 1 thang.','2026-03-23 21:43:25.289569','Phong thong thoang, co cua so lon, phu hop sinh vien va nguoi di lam.','https://picsum.photos/seed/tro1/900/600','https://picsum.photos/seed/tro1a/900/600 | https://picsum.photos/seed/tro1b/900/600 | https://picsum.photos/seed/tro1c/900/600','ICTU Thai Nguyen',NULL,'APPROVED',1200000,'Sakura House',22,'AVAILABLE','Phong studio full noi that gan ICTU','2026-05-09 11:36:58.648304',2,3,NULL,NULL,2),(2,'So 8 ngo 215 Tan Thinh, Thai Nguyen','','Gan DH CNTT Thai Nguyen','2026-03-28',3,'0911111111','Uu tien hop dong 1 nam.','2026-03-23 21:43:25.295082','Phong co gac xep, khu dan cu yen tinh, gan cho va xe buyt.','https://picsum.photos/seed/tro2/900/600','https://picsum.photos/seed/tro2a/900/600 | https://picsum.photos/seed/tro2b/900/600 | https://picsum.photos/seed/tro2c/900/600','ICTU Thai Nguyen',NULL,'APPROVED',1500000,'Sakura House',28,'EXPIRING_SOON','Phong gac xep tiet kiem chi phi','2026-05-12 13:46:14.146205',2,NULL,NULL,NULL,3),(3,'To 14 Quyet Thang, Thai Nguyen','wifi | dieu hoa | bep rieng | camera','Quyet Thang','2026-04-02',2,'0922222222','Co mau hop dong dien tu.','2026-03-23 21:43:25.296595','Nha moi, co bep rieng va khu vuc hoc tap, phu hop cap doi hoac nguoi di lam.','https://picsum.photos/seed/tro3/900/600','https://picsum.photos/seed/tro3a/900/600 | https://picsum.photos/seed/tro3b/900/600 | https://picsum.photos/seed/tro3c/900/600','Thai Nguyen University',NULL,'APPROVED',2200000,'Hong Phuc Residence',30,'OCCUPIED','Phong dep co dieu hoa va bep rieng','2026-03-23 21:43:25.296595',3,NULL,NULL,NULL,NULL),(4,'To 14 Quyet Thang, Thai Nguyen','wifi | giu xe | ban hoc','Quyet Thang','2026-03-24',2,'0922222222','Coc 500k, hop dong linh hoat.','2026-03-23 21:43:25.298592','Gia mem, thu tuc nhanh, co ho tro xac nhan tam tru.','https://picsum.photos/seed/tro4/900/600','https://picsum.photos/seed/tro4a/900/600 | https://picsum.photos/seed/tro4b/900/600 | https://picsum.photos/seed/tro4c/900/600','Thai Nguyen University','Can bo sung thong tin noi dung hoac hinh anh.','REJECTED',1000000,'Hong Phuc Residence',18,'AVAILABLE','Phong gia re cho tan sinh vien','2026-03-23 22:48:10.237897',3,NULL,NULL,NULL,NULL),(5,'So 3 Duong Z115, Thai Nguyen','wifi | dieu hoa | nong lanh | giu xe | thang may','Gan DH CNTT Thai Nguyen','2026-04-04',2,'0933333333','Co chinh sach gia han hop dong online.','2026-03-23 21:43:25.299592','Phong cao cap, co khu sinh hoat chung, phu hop hoc vien cao hoc va nguoi di lam.','https://picsum.photos/seed/tro5/900/600','https://picsum.photos/seed/tro5a/900/600 | https://picsum.photos/seed/tro5b/900/600 | https://picsum.photos/seed/tro5c/900/600','ICTU Thai Nguyen',NULL,'APPROVED',2700000,'ICTU View',32,'MAINTENANCE','Phong cao cap view dep gan truong','2026-04-27 12:14:52.386648',4,NULL,NULL,NULL,1),(6,'Ngo 7 Tan Thinh, Thai Nguyen','wifi | giu xe | ban hoc | camera','Tan Thinh','2026-03-29',3,'0933333333','Can xac nhan thong tin chu tro.','2026-03-23 21:43:25.300592','Chi phi hop ly, cach truong 7 phut di xe may.','https://picsum.photos/seed/tro6/900/600','https://picsum.photos/seed/tro6a/900/600 | https://picsum.photos/seed/tro6b/900/600 | https://picsum.photos/seed/tro6c/900/600','Tan Thinh Thai Nguyen','Can cap nhat lai noi dung hinh anh.','REJECTED',900000,'Tan Thinh Garden',20,'AVAILABLE','Phong mini cho sinh vien o ghep','2026-03-23 21:43:25.300592',4,NULL,NULL,NULL,NULL),(20,'https://maps.app.goo.gl/NwiHvWaad6Jzjif4A','chỗ để xe có mái che','đại học khoa  học','2026-05-12',10,'0848041204','tốt','2026-05-12 13:32:38.789563','tốt','/uploads/65c2b07f-c446-4b84-8f05-445e73ab97b4.png',NULL,'đại học khoa học',NULL,'APPROVED',900000,'Sakura House',12,'AVAILABLE','trọ siêu rẻ , chỉ dưới 1 triệu','2026-05-12 13:33:20.343517',11,0,0,0,1);
/*!40000 ALTER TABLE `rooms` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `surveys`
--

DROP TABLE IF EXISTS `surveys`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `surveys` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cleanliness_rating` int NOT NULL,
  `comment` varchar(1000) DEFAULT NULL,
  `convenience_rating` int NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `security_rating` int NOT NULL,
  `room_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKnmhc2osmtjkufkwi4j47rvu42` (`room_id`),
  KEY `FKiydpdbdg90l5bl365gt67qgrn` (`user_id`),
  CONSTRAINT `FKiydpdbdg90l5bl365gt67qgrn` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKnmhc2osmtjkufkwi4j47rvu42` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `surveys`
--

LOCK TABLES `surveys` WRITE;
/*!40000 ALTER TABLE `surveys` DISABLE KEYS */;
INSERT INTO `surveys` VALUES (1,5,'Vi tri gan truong, chu tro phan hoi nhanh va phong sach.',5,'2026-03-23 21:43:25.320084',4,1,5),(2,4,'Noi that tot, gia hop ly so voi khu vuc.',4,'2026-03-23 21:43:25.321083',5,3,6),(3,4,'Khu nha tro yen tinh, co thang may va giu xe an toan.',5,'2026-03-23 21:43:25.322083',4,5,7);
/*!40000 ALTER TABLE `surveys` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `address` varchar(255) DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `full_name` varchar(255) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `role` enum('ADMIN','LANDLORD','TENANT') NOT NULL,
  `locked` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=54 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,1,'Thai Nguyen','admin@trototn.vn','Admin He Thong','$2a$10$GLsv5GAEvIL2GP5OYQ8qpOZ9imhrnCmrNdebA/PHki7DfLaMjhQuu','0900000000','ADMIN',0,'2026-04-03 11:46:34.450669'),(2,1,'Tan Thinh, Thai Nguyen','ha.chutro@trototn.vn','Nguyen Thu Ha','$2a$10$lIL/wWUMReWYR2HaiolHfebzRpQI/p7NQcV3rGOLQ6yYjYE6FRpwa','0911111111','LANDLORD',0,'2026-04-03 11:46:34.450669'),(3,1,'Quyet Thang, Thai Nguyen','viet.chutro@trototn.vn','Tran Quoc Viet','8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92','0922222222','LANDLORD',0,'2026-04-03 11:46:34.450669'),(4,1,'Gan ICTU, Thai Nguyen','lananh.chutro@trototn.vn','Pham Lan Anh','8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92','0933333333','LANDLORD',0,'2026-04-03 11:46:34.450669'),(5,1,'Thai Nguyen','khang.nguoithue@trototn.vn','Le Minh Khang','$2a$10$lhSscFzrSNCLqAF/emmw/eZ4Fp0hSAk2EMAD/fu5psoU7Kna12jIS','0944444444','TENANT',0,'2026-04-03 11:46:34.450669'),(6,1,'Bac Ninh','trang.nguoithue@trototn.vn','Do Thu Trang','8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92','0955555555','TENANT',0,'2026-04-03 11:46:34.450669'),(7,1,'Tuyen Quang','nam.nguoithue@trototn.vn','Vu Duc Nam','8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92','0966666666','TENANT',0,'2026-04-03 11:46:34.450669'),(8,1,'lạng sơn','minh@gmail.com','hoang binh minh','8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92','0848041204','ADMIN',0,'2026-04-03 11:46:34.450669'),(9,1,'Thai Nguyen','dtc225200837@ictu.edu.vn','Bình Minh Hoàng','$2a$10$T4v7zFdoJLnaARfbcL0HXOFULd/NUoKx2.q.1fR.hBgO8Po2ibsWa','8484041204','LANDLORD',0,'2026-04-03 04:55:47.341324'),(10,1,'sơn la','long12@gmail.com','long','$2a$10$1V.db21J2Z0UI3VuiL5Ncuon8vQMJtr46exkhpHdtuO4I23LAA9ha','1234567890','TENANT',0,'2026-04-28 15:39:12.290565'),(11,1,'hà giang','manh@gmail.com','manh','$2a$10$7SkhAmyEY910g04Xus3kPuZui8OwiW3e0PLBYw2X0GXqWmXZUzeAa','0848041204','LANDLORD',0,'2026-04-29 13:45:24.779418');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'boarding_house_platform'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-05-12 20:50:42
