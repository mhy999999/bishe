-- MySQL dump 10.13  Distrib 8.0.39, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: bishe
-- ------------------------------------------------------
-- Server version	8.0.39

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
-- Table structure for table `ai_recycling_model`
--

DROP TABLE IF EXISTS `ai_recycling_model`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_recycling_model` (
  `id` int NOT NULL,
  `model_json` text COLLATE utf8mb4_general_ci,
  `train_samples` int DEFAULT '0',
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_recycling_model`
--

/*!40000 ALTER TABLE `ai_recycling_model` DISABLE KEYS */;
INSERT INTO `ai_recycling_model` VALUES (1,'{\"features\":[\"capacity\",\"voltage\",\"capacityVoltage\",\"chainTransferCount\",\"chainMaintenanceCount\",\"chainQualityInspectionCount\",\"chainSalesCount\",\"lastTransferDays\",\"lastMaintenanceDays\",\"lastQualityInspectionDays\",\"latestOcv\",\"latestAcr\",\"latestInsulationRes\",\"latestAirTightnessOk\"],\"std\":[1,1,1,1,1,1,1,1,1,1,1,1,1,1],\"trainedAt\":\"2026-01-06T16:10:03.231342900\",\"mean\":[0,0,0,0,0,0,0,0,0,0,0,0,0,0],\"weights\":[0,0,0,0.6,-5,-15,0,0,0,0,0,0,0,0,0],\"samples\":0}',0,'2026-01-06 16:10:03');
/*!40000 ALTER TABLE `ai_recycling_model` ENABLE KEYS */;

--
-- Table structure for table `battery_info`
--

DROP TABLE IF EXISTS `battery_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `battery_info` (
  `battery_id` varchar(64) NOT NULL COMMENT '电池ID',
  `batch_id` bigint DEFAULT NULL COMMENT '生产批次ID',
  `manufacturer` varchar(100) NOT NULL,
  `type_code` varchar(20) NOT NULL COMMENT '电池类型',
  `capacity` decimal(10,2) NOT NULL COMMENT '额定容量',
  `voltage` decimal(10,2) NOT NULL COMMENT '标称电压',
  `cathode_material` varchar(50) DEFAULT NULL COMMENT '正极材料',
  `produce_date` date NOT NULL COMMENT '生产日期',
  `create_time` datetime NOT NULL COMMENT '录入时间',
  `status` int NOT NULL COMMENT '当前状态 1:生产, 2:销售, 3:使用, 4:维修, 5:回收',
  PRIMARY KEY (`battery_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='电池主表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `battery_info`
--

/*!40000 ALTER TABLE `battery_info` DISABLE KEYS */;
INSERT INTO `battery_info` VALUES ('1',4,'1','NCM',1.00,1.00,NULL,'2025-12-27','2025-12-27 17:05:58',7),('3',4,'3','NCM',3.00,3.00,NULL,'2025-12-27','2025-12-27 17:28:41',4),('5',4,'1','NCM',1.00,1.00,NULL,'2025-12-27','2025-12-27 17:12:56',1),('BAT-202512-0001',NULL,'宁德时代','NCM',75.00,400.00,'NCM811','2025-11-15','2025-12-10 09:30:00',4),('BAT-202512-0002',NULL,'宁德时代','LFP',55.00,350.00,'LFP','2025-11-20','2025-12-11 10:15:00',4),('BAT-202512-0003',NULL,'比亚迪','LFP',60.00,360.00,'LFP','2025-11-22','2025-12-12 14:10:00',5),('BAT-202512-0004',NULL,'宁德时代','NCM',82.00,450.00,'NCA','2025-11-25','2025-12-14 16:45:00',4);
/*!40000 ALTER TABLE `battery_info` ENABLE KEYS */;

--
-- Table structure for table `battery_transfer_record`
--

DROP TABLE IF EXISTS `battery_transfer_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `battery_transfer_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `battery_id` varchar(64) NOT NULL COMMENT '电池ID',
  `from_owner` bigint NOT NULL COMMENT '转出方ID',
  `to_owner` bigint NOT NULL COMMENT '接收方ID',
  `action_type` varchar(20) NOT NULL COMMENT '动作类型',
  `create_time` datetime NOT NULL COMMENT '发生时间',
  `tx_hash` varchar(66) DEFAULT NULL COMMENT '交易哈希',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流转记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `battery_transfer_record`
--

/*!40000 ALTER TABLE `battery_transfer_record` DISABLE KEYS */;
INSERT INTO `battery_transfer_record` VALUES (1,'BAT-202512-0001',3,4,'SALE_TO_CUSTOMER','2025-12-18 09:25:00','0x33a3c549d0f0a8a6d49b534acf5a7ae4938c5990efe4c348ee0dc104edecbba2'),(2,'BAT-202512-0002',3,4,'SALE_TO_CUSTOMER','2025-12-18 09:40:00','0x2456bfe7853f411e3bda25077fcd671a9fd652a9a0225d0bcc8db7b4f31a4705'),(3,'BAT-202512-0003',3,9,'SALE_TO_CUSTOMER','2025-12-19 15:15:00','0xd3fa60ba2dfa5e60200e36998b9ba64ea33e7ad3996ece9ee8e5e1ce67aa1681'),(4,'BAT-202512-0004',3,10,'SALE_TO_CUSTOMER','2025-12-19 16:05:00','0x3696b805ced82737fbb8ea7b08cbd2d9d3ce79b20adba493d5340631749d8f3e'),(5,'1',9,10,'TEST','2025-12-30 00:57:12','0xe1b17ecdac0ab43ab152d8d3158328e4d01becf1240f6aec48037b8ee3073843'),(6,'1',9,10,'TEST2','2025-12-30 01:00:13','0xe7b90a06db2567480286329e1b75bf957a0fc3cc20fe765b572d2d29771f8c7e'),(7,'2',2,3,'TRANSFER','2026-01-01 02:43:31','0x024b54f2ee0c8fea2e32b307b5ab05c81cf0cde72ce8d1a7b81af9bfdc3c33c5'),(8,'3',2,3,'TRANSFER','2026-01-01 02:43:31','0xed8eb375133409cd2f164f02340d38f3e6ca38ffb9cd3303a90b6279d0d75b68'),(9,'5',2,3,'TRANSFER','2026-01-01 02:43:31','0x5081295ac1076638bd2f47d7fad41e6db26bdba516d3abe634bcb420445aba9a');
/*!40000 ALTER TABLE `battery_transfer_record` ENABLE KEYS */;

--
-- Table structure for table `chain_transaction`
--

DROP TABLE IF EXISTS `chain_transaction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chain_transaction` (
  `tx_hash` varchar(66) NOT NULL COMMENT '交易哈希',
  `block_height` bigint DEFAULT NULL,
  `contract_addr` varchar(42) DEFAULT NULL,
  `method_name` varchar(50) NOT NULL COMMENT '调用方法',
  `params` text COMMENT '调用参数',
  `status` int NOT NULL COMMENT '交易状态 0:成功, 1:失败',
  `create_time` datetime NOT NULL COMMENT '上链时间',
  `chain_id` bigint DEFAULT NULL COMMENT '链ID',
  `from_address` varchar(42) DEFAULT NULL COMMENT '发起地址',
  `error_message` text COMMENT '错误信息',
  PRIMARY KEY (`tx_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='上链日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `chain_transaction`
--

/*!40000 ALTER TABLE `chain_transaction` DISABLE KEYS */;
INSERT INTO `chain_transaction` VALUES ('0x024b54f2ee0c8fea2e32b307b5ab05c81cf0cde72ce8d1a7b81af9bfdc3c33c5',900001,'0x0000000000000000000000000000000000000000','recordTransfer','{\"batteryId\":\"2\",\"from\":2,\"to\":3,\"action\":\"TRANSFER\"}',1,'2026-01-01 02:43:20',1,'0x1111111111111111111111111111111111111111',NULL),('0x0cd8f1676569106fa268039f96d736fa0ebc9571ba5b12d9c4e915617822adc8',10570,'0x0000000000000000000000000000000000000000','recordSales','{\"status\": 1, \"salesId\": 3, \"batteryId\": \"BAT-202512-0003\", \"buyerName\": \"李四\", \"salesDate\": \"2025-12-19 15:10:00\", \"salesPrice\": 42999.00, \"materialUrl\": null, \"salesPerson\": \"tesla_sh\", \"auditOpinion\": null, \"materialDesc\": null}',1,'2025-12-28 03:05:42',NULL,NULL,NULL),('0x0d5efd2fc5f8c555d575f0f2d37a68842d48a4827fc2f9a4d55e72477db8747a',NULL,'0x5FbDB2315678afecb367f032d93F642f64180aa3','uploadRecyclingPhoto','{\"batteryId\":\"1\",\"appraisalId\":3,\"time\":1767715525340,\"operator\":\"管理员\",\"url\":\"/files/recycling/photos/3/569b0c38ae1d46888c5d099473b4c548_image - 2025-12-11T143021.261.png\"}',1,'2026-01-07 00:05:26',NULL,'0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266','Failed to connect to /127.0.0.1:8545'),('0x0feb328265e06ec40d8587ecd00443e58c4182bf42b2097454c6f32eb1412af8',NULL,'0x5FbDB2315678afecb367f032d93F642f64180aa3','uploadRecyclingPhoto','{\"batteryId\":\"1\",\"appraisalId\":3,\"time\":1767714889793,\"operator\":\"管理员\",\"url\":\"/files/recycling/photos/3/ba20db9ecb0c4709a32f4e05ecdc90d1_d4c31bd0260dac01a76f46ec17883b02_3912538206445877688.png\"}',1,'2026-01-06 23:54:50',NULL,'0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266','Failed to connect to /127.0.0.1:8545'),('0x148159b5e5fe3525e509a67b12f5e2b95740db6cbc4c652e0bdcab7b87087e50',900301,'0x0000000000000000000000000000000000000000','recordRecycling','{\"batteryId\":\"1\",\"recyclerId\":6,\"suggestion\":\"梯次利用\",\"residualValue\":19999.00}',1,'2026-01-04 02:44:31',1,'0x4444444444444444444444444444444444444444',NULL),('0x2179ebabb1fd8eb04152a48564cd1b68a065e918503acea90ed0c1dd3a45710f',900301,'0x0000000000000000000000000000000000000000','recordRecycling','{\"batteryId\":\"5\",\"recyclerId\":6,\"suggestion\":\"梯次利用\",\"residualValue\":19999.00}',1,'2026-01-04 02:44:31',1,'0x4444444444444444444444444444444444444444',NULL),('0x2240452c78029b35b7b4c0e4ef72db737e1acce62f5c2fdb95ce34c559803241',NULL,'0x5FbDB2315678afecb367f032d93F642f64180aa3','saveRecyclingValuation','{\"preliminaryValue\":49.48,\"batteryId\":\"1\",\"appraisalId\":3,\"time\":1767715531655,\"operator\":\"管理员\",\"valuationBasis\":\"auto; operator=管理员; time=2026-01-07T00:05:31.649697200; batteryId=1; maintenanceCount=1; transferCount=2; batteryStatus=5\"}',1,'2026-01-07 00:05:32',NULL,'0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266','Failed to connect to /127.0.0.1:8545'),('0x2456bfe7853f411e3bda25077fcd671a9fd652a9a0225d0bcc8db7b4f31a4705',11002,'0x63638cc75fdc477f99e6efe3adad0f12','recordTransfer','{\"id\":null,\"batteryId\":\"BAT-202512-0002\",\"fromOwner\":3,\"toOwner\":4,\"actionType\":\"SALE_TO_CUSTOMER\",\"createTime\":\"2025-12-18T09:40:00\"}',1,'2025-12-18 09:40:00',NULL,NULL,NULL),('0x2495d3e1718b96f4f9c218e080bc68487c7e72123d6ab9167e3a6b1c249bf5c3',900301,'0x0000000000000000000000000000000000000000','recordRecycling','{\"batteryId\":\"3\",\"recyclerId\":6,\"suggestion\":\"梯次利用\",\"residualValue\":19999.00}',1,'2026-01-04 02:44:31',1,'0x4444444444444444444444444444444444444444',NULL),('0x25b12772cc17bfda0605c3a3351d88ea1cbd8add2f5a2183f2c413ff9013c823',900201,'0x0000000000000000000000000000000000000000','recordMaintenance','{\"batteryId\":\"2\",\"faultType\":\"电压异常\",\"maintainer\":\"管理员\"}',1,'2026-01-03 02:44:05',1,'0x3333333333333333333333333333333333333333',NULL),('0x285e993e43a1adfc966c2a6aafaf56d2164156e0707e695d2de7ff6db8ca631c',900301,'0x0000000000000000000000000000000000000000','recordRecycling','{\"batteryId\":\"BAT-202512-0001\",\"recyclerId\":6,\"suggestion\":\"梯次利用\",\"residualValue\":19999.00}',1,'2026-01-04 02:44:31',1,'0x4444444444444444444444444444444444444444',NULL),('0x2ec1768de9cf67633202f53b4f71dfd71f76fec36518d3bb614d30b4269d047a',NULL,'0x5FbDB2315678afecb367f032d93F642f64180aa3','recordMaintenance','{\"batteryId\":\"BAT-202512-0004\",\"faultType\":\"adsfasfas\",\"description\":\"asdfas\",\"maintainer\":\"管理员\",\"createTime\":1767630839916,\"status\":0,\"issueMaterialDesc\":\"adsfasd\",\"issueMaterialUrl\":\"[\\\"/files/maintenance/issue/b2776b8292904299981e18e29a28afee_cedd27cd74fda13c80bd1726eca6058d.mp4\\\"]\"}',1,'2026-01-06 00:34:00',NULL,'0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266','Failed to connect to /127.0.0.1:8545'),('0x339bcfc1a525946dc5ee6625e9b6075a81031c3a712edb686724221554daaa22',NULL,'0x5FbDB2315678afecb367f032d93F642f64180aa3','bindVehicle','{\"vin\":\"12345678900987654\",\"batteryId\":\"2\",\"brand\":\"byd\",\"model\":\"byd1型\",\"plateNo\":\"12345\",\"ownerId\":1,\"bindTime\":1767704447737}',1,'2026-01-06 21:00:48',NULL,'0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266','Failed to connect to /127.0.0.1:8545'),('0x33a3c549d0f0a8a6d49b534acf5a7ae4938c5990efe4c348ee0dc104edecbba2',11001,'0x63638cc75fdc477f99e6efe3adad0f12','recordTransfer','{\"id\":null,\"batteryId\":\"BAT-202512-0001\",\"fromOwner\":3,\"toOwner\":4,\"actionType\":\"SALE_TO_CUSTOMER\",\"createTime\":\"2025-12-18T09:25:00\"}',1,'2025-12-18 09:25:00',NULL,NULL,NULL),('0x33f40f3e1ec0b6a856dae736c3db759486f7dccab3b69969c9e9917a61924f4d',10493,'0x0000000000000000000000000000000000000000','recordSales','{\"status\": 1, \"salesId\": 1, \"batteryId\": \"BAT-202512-0001\", \"buyerName\": \"张三\", \"salesDate\": \"2025-12-18 09:20:00\", \"salesPrice\": 49999.00, \"materialUrl\": null, \"salesPerson\": \"tesla_sh\", \"auditOpinion\": null, \"materialDesc\": null}',1,'2025-12-28 03:05:42',NULL,NULL,NULL),('0x340e4a3047754bfce6e615f8b386d1174600eeb60320fed689bd2786a4bdb4d9',NULL,'0x5FbDB2315678afecb367f032d93F642f64180aa3','uploadRecyclingPhoto','{\"batteryId\":\"1\",\"appraisalId\":3,\"time\":1767714890112,\"operator\":\"管理员\",\"url\":\"/files/recycling/photos/3/7a9757d53dd54c7cbf2737cbfdedfed0_image - 2025-12-11T143019.102.png\"}',1,'2026-01-06 23:54:50',NULL,'0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266','Failed to connect to /127.0.0.1:8545'),('0x354e0e9ebd33e957932e76e83912f3c00ebb101eef279d3d5ff6761bdf48c13f',NULL,'0x5FbDB2315678afecb367f032d93F642f64180aa3','uploadRecyclingPhoto','{\"time\":1767645035364,\"appraisalId\":4,\"batteryId\":\"2\",\"operator\":\"管理员\",\"url\":\"/files/recycling/photos/4/71469c1235b340bfad4a2ea242a0ef0e_image - 2025-12-11T143021.261.png\"}',1,'2026-01-06 04:30:35',NULL,'0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266','Failed to connect to /127.0.0.1:8545'),('0x3696b805ced82737fbb8ea7b08cbd2d9d3ce79b20adba493d5340631749d8f3e',11004,'0x63638cc75fdc477f99e6efe3adad0f12','recordTransfer','{\"id\":null,\"batteryId\":\"BAT-202512-0004\",\"fromOwner\":3,\"toOwner\":10,\"actionType\":\"SALE_TO_CUSTOMER\",\"createTime\":\"2025-12-19T16:05:00\"}',1,'2025-12-19 16:05:00',NULL,NULL,NULL),('0x38a2213a15c98e660f07cbf2bb666e49379f547142251ea3427ef1eadebf77a5',NULL,'0x5FbDB2315678afecb367f032d93F642f64180aa3','saveRecyclingValuation','{\"preliminaryValue\":49.48,\"batteryId\":\"1\",\"appraisalId\":3,\"time\":1767715529755,\"operator\":\"管理员\",\"valuationBasis\":\"auto; operator=管理员; time=2026-01-07T00:05:29.748414900; batteryId=1; maintenanceCount=1; transferCount=2; batteryStatus=5\"}',1,'2026-01-07 00:05:30',NULL,'0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266','Failed to connect to /127.0.0.1:8545'),('0x443d6ceb9aa96e3610f16dc952674c27802a4b5f338c82187a8340abae0ba776',900201,'0x0000000000000000000000000000000000000000','recordMaintenance','{\"batteryId\":\"BAT-202512-0002\",\"faultType\":\"电压异常\",\"maintainer\":\"管理员\"}',1,'2026-01-03 02:44:05',1,'0x3333333333333333333333333333333333333333',NULL),('0x4816308f3cad80e61009cc6336dc0663631d492bb869688c3c0a48c0645f83d0',10791,'0x63638cc75fdc477f99e6efe3adad0f12','recordQualityInspection','{\"batteryId\":\"2\",\"ocv\":12,\"acr\":12,\"insulationRes\":12,\"airTightness\":\"PASS\",\"inspector\":\"管理员\",\"checkTime\":1766748167023,\"status\":0}',1,'2025-12-27 03:22:55',NULL,NULL,NULL),('0x4980e4a733e9eb7496158d3e26c09a213b294ccba665567bae9f1765c251ccb4',10384,'0x63638cc75fdc477f99e6efe3adad0f12','recordQualityInspection','{\"batteryId\":\"3\",\"ocv\":3,\"acr\":3,\"insulationRes\":3,\"airTightness\":\"PASS\",\"inspector\":\"管理员\",\"checkTime\":1766748237524,\"status\":0}',1,'2025-12-27 03:24:02',NULL,NULL,NULL),('0x4f960f994d65b1f3a753eb840017790fb8ad6ac4cf2b4971e58141acc1b54b5a',900201,'0x0000000000000000000000000000000000000000','recordMaintenance','{\"batteryId\":\"3\",\"faultType\":\"电压异常\",\"maintainer\":\"管理员\"}',1,'2026-01-03 02:44:05',1,'0x3333333333333333333333333333333333333333',NULL),('0x5081295ac1076638bd2f47d7fad41e6db26bdba516d3abe634bcb420445aba9a',900001,'0x0000000000000000000000000000000000000000','recordTransfer','{\"batteryId\":\"5\",\"from\":2,\"to\":3,\"action\":\"TRANSFER\"}',1,'2026-01-01 02:43:20',1,'0x1111111111111111111111111111111111111111',NULL),('0x53f7e5a3631053318f3e32326f86373a7de1605990f7feb1bf2053dc9029d4bf',NULL,'0x5FbDB2315678afecb367f032d93F642f64180aa3','auditRecyclingApply','{\"appraisalId\":4,\"recycleNo\":\"RCY202601060429148937\",\"auditOpinion\":\"123\",\"auditor\":\"管理员\",\"batteryId\":\"2\",\"auditTime\":1767644954894,\"status\":1}',1,'2026-01-06 04:29:16',NULL,'0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266','Failed to connect to /127.0.0.1:8545'),('0x55a9af09d06cf5cbfb7899c98cee1db3165d4946a870d593e0f456c1fa8dd728',10234,'0xb44daf90fd3a419aaee152801630617a','recordQualityInspection','{\"batteryId\":\"4\",\"ocv\":4,\"acr\":4,\"insulationRes\":4,\"airTightness\":\"PASS\",\"inspector\":\"管理员\",\"checkTime\":1766796991218,\"status\":0}',1,'2025-12-27 16:56:35',NULL,NULL,NULL),('0x58d4cde6b8bd1aa2ec81ef2de7a380875da020f5b4684a183d139d3eabeea39a',12003,'0x63638cc75fdc477f99e6efe3adad0f12','bindVehicle','{\"vehicleId\":null,\"vin\":\"LC0CE4CB1P0000003\",\"batteryId\":\"BAT-202512-0003\",\"brand\":\"比亚迪\",\"model\":\"汉 EV\",\"plateNo\":\"粤B88888\",\"ownerId\":9,\"bindTime\":\"2025-12-21T09:10:00\"}',1,'2025-12-21 09:10:00',NULL,NULL,NULL),('0x5f2f2c5e19de3785e0782964e2bea6d977efc4202f754e4b1e1454167737ed19',NULL,'0x5FbDB2315678afecb367f032d93F642f64180aa3','saveRecyclingValuation','{\"preliminaryValue\":49.48,\"batteryId\":\"1\",\"appraisalId\":3,\"time\":1767715532633,\"operator\":\"管理员\",\"valuationBasis\":\"auto; operator=管理员; time=2026-01-07T00:05:32.627144600; batteryId=1; maintenanceCount=1; transferCount=2; batteryStatus=5\"}',1,'2026-01-07 00:05:33',NULL,'0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266','Failed to connect to /127.0.0.1:8545'),('0x6024da8ae27c30577f5707914edf01c0d32e4e7a17b4ca2a49888d85a828b70c',NULL,'0x5FbDB2315678afecb367f032d93F642f64180aa3','saveRecyclingValuation','{\"preliminaryValue\":49.48,\"batteryId\":\"1\",\"appraisalId\":3,\"time\":1767715528810,\"operator\":\"管理员\",\"valuationBasis\":\"auto; operator=管理员; time=2026-01-07T00:05:28.794215800; batteryId=1; maintenanceCount=1; transferCount=2; batteryStatus=5\"}',1,'2026-01-07 00:05:29',NULL,'0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266','Failed to connect to /127.0.0.1:8545'),('0x640d99392b2cc558f846cba9b23ddf14d509cfa2b5022510a444f8e315f934fb',NULL,'0x5FbDB2315678afecb367f032d93F642f64180aa3','uploadRecyclingPhoto','{\"time\":1767645035451,\"appraisalId\":4,\"batteryId\":\"2\",\"operator\":\"管理员\",\"url\":\"/files/recycling/photos/4/72fce93e30574a7f9b5232fb1c15766c_image - 2025-12-11T143022.063.png\"}',1,'2026-01-06 04:30:35',NULL,'0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266','Failed to connect to /127.0.0.1:8545'),('0x65cef0f18294af7fa4a961781d470ec90756c8596d7c054dbc32f23cc298c7bc',NULL,'0x5FbDB2315678afecb367f032d93F642f64180aa3','auditRecyclingApply','{\"auditTime\":1767714878152,\"batteryId\":\"BAT-202512-0003\",\"auditor\":\"管理员\",\"appraisalId\":2,\"recycleNo\":\"RCY202601062354388755\",\"status\":1}',1,'2026-01-06 23:54:38',NULL,'0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266','Failed to connect to /127.0.0.1:8545'),('0x6789db5095cc2a780967bc999e6437a1618545b1e78f693c7e05e57ccb5afe93',900301,'0x0000000000000000000000000000000000000000','recordRecycling','{\"batteryId\":\"BAT-202512-0004\",\"recyclerId\":6,\"suggestion\":\"梯次利用\",\"residualValue\":19999.00}',1,'2026-01-04 02:44:31',1,'0x4444444444444444444444444444444444444444',NULL),('0x695c6c36c6a474f52abea8d7c9425fe197d436497a6f91a3178b460c6e8a70a1',10397,'0x0000000000000000000000000000000000000000','recordSales','{\"status\": 1, \"salesId\": 5, \"batteryId\": \"3\", \"buyerName\": \"test\", \"salesDate\": \"2025-12-28 01:03:26\", \"salesPrice\": 12.00, \"materialUrl\": \"[\\\"/files/sales/26b6acc0ece74b9aa4e396d8bb036af0_main.txt\\\"]\", \"salesPerson\": \"管理员\", \"auditOpinion\": \"\", \"materialDesc\": \"我是测试\"}',1,'2025-12-28 03:05:42',NULL,NULL,NULL),('0x6973a2cbadd8cef41ff971b4b3027606bd4e80380384020624392e35bbef1efd',NULL,'0x5FbDB2315678afecb367f032d93F642f64180aa3','generateRecyclingReceipt','{\"batteryInfo\":{\"batteryId\":\"BAT-202512-0003\",\"manufacturer\":\"比亚迪\",\"typeCode\":\"LFP\",\"capacity\":60,\"voltage\":360,\"cathodeMaterial\":\"LFP\",\"produceDate\":1763740800000,\"createTime\":1765519800000,\"status\":4}}',1,'2026-01-06 03:08:40',NULL,'0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266','Failed to connect to /127.0.0.1:8545'),('0x6a5623259faf7361931d6eca05ee51b1161ff4902ebf056cb24046ea526b1c09',900201,'0x0000000000000000000000000000000000000000','recordMaintenance','{\"batteryId\":\"BAT-202512-0004\",\"faultType\":\"电压异常\",\"maintainer\":\"管理员\"}',1,'2026-01-03 02:44:05',1,'0x3333333333333333333333333333333333333333',NULL),('0x6dc927cf74d45ccbdf59fb4f76d30824c22e1f355db1e4102aef26387a81e2f2',NULL,'0x5FbDB2315678afecb367f032d93F642f64180aa3','saveRecyclingValuation','{\"preliminaryValue\":49.48,\"batteryId\":\"1\",\"appraisalId\":3,\"time\":1767715531001,\"operator\":\"管理员\",\"valuationBasis\":\"auto; operator=管理员; time=2026-01-07T00:05:30.995283100; batteryId=1; maintenanceCount=1; transferCount=2; batteryStatus=5\"}',1,'2026-01-07 00:05:31',NULL,'0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266','Failed to connect to /127.0.0.1:8545'),('0x707d7b18cba2c9a1e980e6006aa5df2ec082b0137e4ca08d0924cc15dc6f5969',NULL,'0x5FbDB2315678afecb367f032d93F642f64180aa3','recordMaintenance','{\"batteryId\":\"BAT-202512-0003\",\"stationId\":1,\"faultType\":\"ddd\",\"description\":\"ddd\",\"maintainer\":\"管理员\",\"createTime\":1767632242969,\"status\":0,\"issueMaterialDesc\":\"ddd\",\"issueMaterialUrl\":\"[\\\"/files/maintenance/issue/990a789903834731994b965d122a7bf9_cedd27cd74fda13c80bd1726eca6058d.mp4\\\"]\"}',1,'2026-01-06 00:57:24',NULL,'0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266','Failed to connect to /127.0.0.1:8545'),('0x73b583f5f542b01e6c189220f3cee6bf42780368fbf394862a076a950a030737',NULL,'0x5FbDB2315678afecb367f032d93F642f64180aa3','recordMaintenance','{\"batteryId\":\"BAT-202512-0004\",\"faultType\":\"adsfasfas\",\"description\":\"asdfas\",\"maintainer\":\"管理员\",\"createTime\":1767630789651,\"status\":0,\"issueMaterialDesc\":\"adsfasd\",\"issueMaterialUrl\":\"[\\\"/files/maintenance/issue/8b2f69c61a304c8ea905019b6ca8ca01_601数学分析.doc\\\"]\"}',1,'2026-01-06 00:33:11',NULL,'0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266','Failed to connect to /127.0.0.1:8545'),('0x7605d7664014410f84f993173241926924116c138dd7c2bb07e50a62f06da6d6',NULL,'0x5FbDB2315678afecb367f032d93F642f64180aa3','auditRecyclingApply','{\"auditOpinion\":\"ok\",\"auditTime\":1767714021691,\"batteryId\":\"1\",\"auditor\":\"管理员\",\"appraisalId\":3,\"recycleNo\":\"RCY202601062340215575\",\"status\":1}',1,'2026-01-06 23:40:22',NULL,'0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266','Failed to connect to /127.0.0.1:8545'),('0x78829c13c6143285dd69a73e1d24c75a4a7ea6cdbf6547a4eebff1884860fb1a',NULL,'0x5FbDB2315678afecb367f032d93F642f64180aa3','uploadRecyclingPhoto','{\"batteryId\":\"1\",\"appraisalId\":3,\"time\":1767714890136,\"operator\":\"管理员\",\"url\":\"/files/recycling/photos/3/d868f58b89e14e32a525c8e04055acf1_image - 2025-12-11T143019.844.png\"}',1,'2026-01-06 23:54:50',NULL,'0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266','Failed to connect to /127.0.0.1:8545'),('0x7ec14bd9e41a39313d0d0fa9ad84d23c1f9b189304cbd59d235c30b156341ebd',NULL,'0x5FbDB2315678afecb367f032d93F642f64180aa3','confirmRecyclingPrice','{\"preliminaryValue\":49.48,\"priceReviewer\":\"管理员\",\"batteryId\":\"1\",\"appraisalId\":3,\"time\":1767715559397,\"finalValue\":50,\"recycleNo\":\"RCY202601062340215575\"}',1,'2026-01-07 00:05:59',NULL,'0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266','Failed to connect to /127.0.0.1:8545'),('0x91ce2bc0556f0810ecc7370535dd0b320549f290750679d192784936123aed4d',NULL,'0x5FbDB2315678afecb367f032d93F642f64180aa3','uploadRecyclingReport','{\"batteryId\":\"1\",\"appraisalId\":3,\"time\":1767714898800,\"operator\":\"管理员\",\"url\":\"/files/recycling/reports/3/4206228aff7b4dd781e12b4e20d3a0da_cedd27cd74fda13c80bd1726eca6058d.mp4\"}',1,'2026-01-06 23:54:59',NULL,'0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266','Failed to connect to /127.0.0.1:8545'),('0x943fd26e8162b37e57e33f69d1680dd7ddea677f27fe775fd87a90fa5e056514',NULL,'0x5FbDB2315678afecb367f032d93F642f64180aa3','confirmRecyclingPrice','{\"priceReviewer\":\"管理员\",\"finalValue\":123,\"time\":1767709755955,\"appraisalId\":4,\"recycleNo\":\"RCY202601060429148937\",\"preliminaryValue\":0,\"batteryId\":\"2\"}',1,'2026-01-06 22:29:17',NULL,'0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266','Failed to connect to /127.0.0.1:8545'),('0x97f14e1ed670f66cbe3d54632d79c38f1d0f9ce7d483f26fd9f3d55603bc293f',NULL,'0x5FbDB2315678afecb367f032d93F642f64180aa3','calcRecyclingValuation','{\"time\":1767646132967,\"valuationBasis\":\"ai_linear_regression; features=[capacity, voltage, capacityVoltage, maintenanceCount, batteryStatus]; capacity=2.00, voltage=2.00, capacityVoltage=4.0000, maintenanceCount=1, batteryStatus=5; samples=0\",\"appraisalId\":4,\"preliminaryValue\":0,\"operator\":\"管理员\",\"batteryId\":\"2\"}',1,'2026-01-06 04:48:54',NULL,'0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266','Failed to connect to /127.0.0.1:8545'),('0x9b75ef7658e60b8870242d0309e5fc2e71dc8a092fe85d83973d912d262fb612',NULL,'0x5FbDB2315678afecb367f032d93F642f64180aa3','uploadRecyclingPhoto','{\"time\":1767645035406,\"appraisalId\":4,\"batteryId\":\"2\",\"operator\":\"管理员\",\"url\":\"/files/recycling/photos/4/e31badb0be8f4d7bbaca6962e4e14177_image - 2025-12-11T143022.813.png\"}',1,'2026-01-06 04:30:35',NULL,'0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266','Failed to connect to /127.0.0.1:8545'),('0x9b9e786d52d98f1730e7e49a09d39971f7d3dc325f5ae8bc86e082339ff82858',10946,'0x10397cc8bff542e5a235bd58e8b6816b','recordQualityInspection','{\"batteryId\":\"1\",\"ocv\":3,\"acr\":3,\"insulationRes\":3,\"airTightness\":\"PASS\",\"inspector\":\"管理员\",\"checkTime\":1766797925988,\"status\":0}',1,'2025-12-27 17:12:10',NULL,NULL,NULL),('0x9cea8c4710cf652d6b2c552ebc92cf8939a1556652a98b4cf4d791117f066db9',12001,'0x63638cc75fdc477f99e6efe3adad0f12','bindVehicle','{\"vehicleId\":null,\"vin\":\"LRW3E7EK5NC000001\",\"batteryId\":\"BAT-202512-0001\",\"brand\":\"特斯拉\",\"model\":\"Model 3\",\"plateNo\":\"沪A12345\",\"ownerId\":4,\"bindTime\":\"2025-12-20T10:00:00\"}',1,'2025-12-20 10:00:00',NULL,NULL,NULL),('0x9fa1444ef34128c4f85b484cbd21a67bc8d349c0c8cdd0e9e266c7ff6f084b9e',NULL,'0x5FbDB2315678afecb367f032d93F642f64180aa3','uploadRecyclingPhoto','{\"time\":1767645001863,\"appraisalId\":4,\"batteryId\":\"2\",\"operator\":\"管理员\",\"url\":\"/files/recycling/photos/4/a6d4d0b4d46549beb6ea2b13ec6b494f_image - 2025-12-11T143021.261.png\"}',1,'2026-01-06 04:30:02',NULL,'0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266','Failed to connect to /127.0.0.1:8545'),('0xa124012c4136f9d3f0e875b851a99a7f51c8bfd7fab61e665969c498fbfa696c',900301,'0x0000000000000000000000000000000000000000','recordRecycling','{\"batteryId\":\"2\",\"recyclerId\":6,\"suggestion\":\"梯次利用\",\"residualValue\":19999.00}',1,'2026-01-04 02:44:31',1,'0x4444444444444444444444444444444444444444',NULL),('0xa70cca3aaf291918645e0c4daeb79906e08c3cd310aede0e8da7ec2fe70e5ae3',NULL,'0x5FbDB2315678afecb367f032d93F642f64180aa3','uploadRecyclingPhoto','{\"time\":1767645001961,\"appraisalId\":4,\"batteryId\":\"2\",\"operator\":\"管理员\",\"url\":\"/files/recycling/photos/4/36ecfe14739a4f0cb21b7a8c73f6e25d_image - 2025-12-11T143023.608.png\"}',1,'2026-01-06 04:30:02',NULL,'0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266','Failed to connect to /127.0.0.1:8545'),('0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',123456,'0x0000000000000000000000000000000000000000','recordRecycling','{\"batteryId\":\"BAT-202512-0003\",\"recycler\":\"brunp\",\"suggestion\":\"梯次利用\",\"residualValue\":18888.50}',0,'2026-01-05 12:00:00',1,'0x1111111111111111111111111111111111111111',NULL),('0xaf22b01dafb8cca080505d7db82bb2adeabfbfca8fcdab942595b6fa3dec2f87',NULL,'0x5FbDB2315678afecb367f032d93F642f64180aa3','calcRecyclingValuation','{\"operator\":\"管理员\",\"preliminaryValue\":0,\"appraisalId\":4,\"valuationBasis\":\"ai_linear_regression; features=[capacity, voltage, capacityVoltage, maintenanceCount, batteryStatus]; capacity=2.00, voltage=2.00, capacityVoltage=4.0000, maintenanceCount=1, batteryStatus=5; samples=0\",\"time\":1767684070288,\"batteryId\":\"2\"}',1,'2026-01-06 15:21:11',NULL,'0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266','Failed to connect to /127.0.0.1:8545'),('0xb0e6cf89e4955f07bac3099ad769f4fed0135940f462f0214540170cfec3f53a',10466,'0x63638cc75fdc477f99e6efe3adad0f12','recordQualityInspection','{\"batteryId\":\"1\",\"inspector\":\"管理员\",\"checkTime\":1766747207876,\"status\":0}',1,'2025-12-27 03:06:52',NULL,NULL,NULL),('0xb2978242177eb61ff34ee3c87d4844ba13441a27a0d469c89fe80ab5feda6f2e',NULL,'0x5FbDB2315678afecb367f032d93F642f64180aa3','recordQualityInspection','{\"batteryId\":\"5\",\"airTightness\":\"PASS\",\"inspector\":\"管理员\",\"checkTime\":1767673627957,\"status\":0}',1,'2026-01-06 20:27:11',NULL,'0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266','Failed to connect to /127.0.0.1:8545'),('0xba46a778a91a11fe91d6d68d67f845b4e932076d960563886a6d4b4efaf2ffa3',NULL,'0x5FbDB2315678afecb367f032d93F642f64180aa3','uploadRecyclingReport','{\"time\":1767644985814,\"appraisalId\":4,\"batteryId\":\"2\",\"operator\":\"管理员\",\"url\":\"/files/recycling/reports/4/a19c551bb1f24e1b92f946fd11d2d830_image - 2025-12-11T143021.261.png\"}',1,'2026-01-06 04:29:46',NULL,'0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266','Failed to connect to /127.0.0.1:8545'),('0xbedd02214ec3b766986bd61173f75eaf6afa4e6454526dbf650e96347b9faffe',NULL,'0x5FbDB2315678afecb367f032d93F642f64180aa3','calcRecyclingValuation','{\"operator\":\"管理员\",\"preliminaryValue\":0,\"appraisalId\":4,\"valuationBasis\":\"capacity*voltage*0.60 - maintenanceCount*20; capacity=2.00, voltage=2.00, maintenanceCount=1\",\"time\":1767645037232,\"batteryId\":\"2\"}',1,'2026-01-06 04:30:37',NULL,'0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266','Failed to connect to /127.0.0.1:8545'),('0xbf204a47baf4b06e164afa445cf370e618d841e59f686209e09713f938dc745d',10421,'0x0000000000000000000000000000000000000000','recordSales','{\"status\": 1, \"salesId\": 4, \"batteryId\": \"BAT-202512-0004\", \"buyerName\": \"王五\", \"salesDate\": \"2025-12-19 16:00:00\", \"salesPrice\": 56999.00, \"materialUrl\": null, \"salesPerson\": \"tesla_sh\", \"auditOpinion\": null, \"materialDesc\": null}',1,'2025-12-28 03:05:42',NULL,NULL,NULL),('0xc02f7de7bed8a8b1a71bcc145531e393881240fb3756fbd1f613f01766029758',NULL,'0x5FbDB2315678afecb367f032d93F642f64180aa3','recordMaintenance','{\"batteryId\":\"BAT-202512-0004\",\"faultType\":\"adsfasfas\",\"description\":\"asdfas\",\"maintainer\":\"管理员\",\"createTime\":1767630824545,\"status\":0,\"issueMaterialDesc\":\"adsfasd\",\"issueMaterialUrl\":\"[\\\"/files/maintenance/issue/b2776b8292904299981e18e29a28afee_cedd27cd74fda13c80bd1726eca6058d.mp4\\\"]\"}',1,'2026-01-06 00:33:45',NULL,'0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266','Failed to connect to /127.0.0.1:8545'),('0xc095adaa726a9cd1ea683f95b4c96d76b7ac93dfd98d472e9496b277dd4134df',900301,'0x0000000000000000000000000000000000000000','recordRecycling','{\"batteryId\":\"BAT-202512-0002\",\"recyclerId\":6,\"suggestion\":\"梯次利用\",\"residualValue\":19999.00}',1,'2026-01-04 02:44:31',1,'0x4444444444444444444444444444444444444444',NULL),('0xc33828ab517f2acbe8e66d09d374a99492d1a3ef9cf1dd2d73445e00f3e57d24',NULL,'0x5FbDB2315678afecb367f032d93F642f64180aa3','generateRecyclingReceipt','{\"auditOpinion\":\"123\",\"auditTime\":1767644955000,\"auditor\":\"管理员\",\"recycleNo\":\"RCY202601060429148937\",\"batteryInfo\":{\"batteryId\":\"2\",\"batchId\":4,\"manufacturer\":\"2\",\"typeCode\":\"NCM\",\"capacity\":2,\"voltage\":2,\"produceDate\":1766764800000,\"createTime\":1766827721000,\"status\":5}}',1,'2026-01-06 04:29:24',NULL,'0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266','Failed to connect to /127.0.0.1:8545'),('0xcab4c760734ec1fabcd0cb50966358356758d44632c5e4fc76e17fc78a3d81b4',900201,'0x0000000000000000000000000000000000000000','recordMaintenance','{\"batteryId\":\"1\",\"faultType\":\"电压异常\",\"maintainer\":\"管理员\"}',1,'2026-01-03 02:44:05',1,'0x3333333333333333333333333333333333333333',NULL),('0xcb2eb3fead0d43d2248339637a58d76711ebce4911f6faa21023d33ba777f56a',NULL,'0x5FbDB2315678afecb367f032d93F642f64180aa3','uploadRecyclingPhoto','{\"time\":1767645035508,\"appraisalId\":4,\"batteryId\":\"2\",\"operator\":\"管理员\",\"url\":\"/files/recycling/photos/4/b52b84aa421344b2821c5305a1e90fcd_image - 2025-12-11T143023.608.png\"}',1,'2026-01-06 04:30:36',NULL,'0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266','Failed to connect to /127.0.0.1:8545'),('0xcb85b914b115e80fcffb829fc86e4b198beff89d7ee097c8ec3b7b672f58f2c9',12004,'0x63638cc75fdc477f99e6efe3adad0f12','bindVehicle','{\"vehicleId\":null,\"vin\":\"LJ1EAZKB0N0000004\",\"batteryId\":\"BAT-202512-0004\",\"brand\":\"蔚来\",\"model\":\"ES6\",\"plateNo\":\"京A66666\",\"ownerId\":10,\"bindTime\":\"2025-12-21T11:30:00\"}',1,'2025-12-21 11:30:00',NULL,NULL,NULL),('0xce7c6b6f02a174eb57d834ec4166473d4421fd9df2254a354ef9d51dc5f8594e',900201,'0x0000000000000000000000000000000000000000','recordMaintenance','{\"batteryId\":\"BAT-202512-0001\",\"faultType\":\"电压异常\",\"maintainer\":\"管理员\"}',1,'2026-01-03 02:44:05',1,'0x3333333333333333333333333333333333333333',NULL),('0xcefe40c2646fdc13f6796ebc3d84a6209c4715d67b76fd2cb9aecc75f76d9900',NULL,'0x5FbDB2315678afecb367f032d93F642f64180aa3','uploadRecyclingPhoto','{\"time\":1767645001913,\"appraisalId\":4,\"batteryId\":\"2\",\"operator\":\"管理员\",\"url\":\"/files/recycling/photos/4/67cd0f586e5f4e48a4f0160c7124e387_image - 2025-12-11T143022.813.png\"}',1,'2026-01-06 04:30:02',NULL,'0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266','Failed to connect to /127.0.0.1:8545'),('0xcffc9845f96ba05a48e1701e631795ad39a4cb25bc54148de16a16d35568d03c',NULL,'0x5FbDB2315678afecb367f032d93F642f64180aa3','uploadRecyclingPhoto','{\"time\":1767644978351,\"appraisalId\":4,\"batteryId\":\"2\",\"operator\":\"管理员\",\"url\":\"/files/recycling/photos/4/cbde858185d243b5805633d405f49f2a_d4c31bd0260dac01a76f46ec17883b02_3912538206445877688.png\"}',1,'2026-01-06 04:29:38',NULL,'0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266','Failed to connect to /127.0.0.1:8545'),('0xd3fa60ba2dfa5e60200e36998b9ba64ea33e7ad3996ece9ee8e5e1ce67aa1681',11003,'0x63638cc75fdc477f99e6efe3adad0f12','recordTransfer','{\"id\":null,\"batteryId\":\"BAT-202512-0003\",\"fromOwner\":3,\"toOwner\":9,\"actionType\":\"SALE_TO_CUSTOMER\",\"createTime\":\"2025-12-19T15:15:00\"}',1,'2025-12-19 15:15:00',NULL,NULL,NULL),('0xd4c9db53036031233d7307a85ae64c4d39309f10c7b5d722bb982e52e1961702',NULL,'0x5FbDB2315678afecb367f032d93F642f64180aa3','saveRecyclingValuation','{\"preliminaryValue\":49.48,\"batteryId\":\"1\",\"appraisalId\":3,\"time\":1767715527262,\"operator\":\"管理员\",\"valuationBasis\":\"auto; operator=管理员; time=2026-01-07T00:05:27.252985600; batteryId=1; maintenanceCount=1; transferCount=2; batteryStatus=5\"}',1,'2026-01-07 00:05:27',NULL,'0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266','Failed to connect to /127.0.0.1:8545'),('0xd56df77d94b81bdd2acd54387635c2a0a3fa8ebc403543f331ad2c35494096d6',NULL,'0x5FbDB2315678afecb367f032d93F642f64180aa3','saveRecyclingValuation','{\"preliminaryValue\":49.48,\"batteryId\":\"1\",\"appraisalId\":3,\"time\":1767715530425,\"operator\":\"管理员\",\"valuationBasis\":\"auto; operator=管理员; time=2026-01-07T00:05:30.418456900; batteryId=1; maintenanceCount=1; transferCount=2; batteryStatus=5\"}',1,'2026-01-07 00:05:30',NULL,'0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266','Failed to connect to /127.0.0.1:8545'),('0xd606a88b61cd2a5e7ea675d1ec57d9348c918bba8608df3defa3507d1e7e0df9',NULL,'0x5FbDB2315678afecb367f032d93F642f64180aa3','calcRecyclingValuation','{\"batteryId\":\"2\",\"time\":1767687003312,\"valuationBasis\":\"ai_linear_regression_chain_dataset; features=[capacity, voltage, capacityVoltage, chainTransferCount, chainMaintenanceCount, chainQualityInspectionCount, chainSalesCount, lastTransferDays, lastMaintenanceDays, lastQualityInspectionDays, latestOcv, latestAcr, latestInsulationRes, latestAirTightnessOk]; capacity=2.00, voltage=2.00, capacityVoltage=4.0000; chain.transferCount=1, chain.maintenanceCount=1, chain.qualityInspectionCount=1, chain.salesCount=1, chain.lastTransferDays=5, chain.lastMaintenanceDays=3, chain.lastQualityInspectionDays=10, chain.latestOcv=12.0, chain.latestAcr=12.0, chain.latestInsulationRes=12.0, chain.latestAirTightnessOk=1; samples=0\",\"appraisalId\":4,\"preliminaryValue\":0,\"operator\":\"recycler_test\"}',1,'2026-01-06 16:10:04',NULL,'0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266','Failed to connect to /127.0.0.1:8545'),('0xd644124959e9e08d96b878212a6ab03fe3b5d07e6b7f019cd3b0b7c853538f5c',900101,'0x0000000000000000000000000000000000000000','recordSales','{\"batteryId\":\"2\",\"buyer\":\"张三\",\"price\":39999.00}',1,'2026-01-02 02:43:43',1,'0x2222222222222222222222222222222222222222',NULL),('0xd82f8d0a7bc0d89cf3aeb3a7a5f252c28950e48b1bfa1017aacf2c8252c8dea2',900201,'0x0000000000000000000000000000000000000000','recordMaintenance','{\"batteryId\":\"5\",\"faultType\":\"电压异常\",\"maintainer\":\"管理员\"}',1,'2026-01-03 02:44:05',1,'0x3333333333333333333333333333333333333333',NULL),('0xda8f348556859fd8b444d49d6c16fd3b6b2d4e490f44a395a9175131b357ff0c',900101,'0x0000000000000000000000000000000000000000','recordSales','{\"batteryId\":\"5\",\"buyer\":\"张三\",\"price\":39999.00}',1,'2026-01-02 02:43:43',1,'0x2222222222222222222222222222222222222222',NULL),('0xde798406ec325a2e932725f35888773438cb671ecf607d0d8fa7fc81c4330fd2',10194,'0x63638cc75fdc477f99e6efe3adad0f12','recordQualityInspection','{\"batteryId\":\"1\",\"inspector\":\"管理员\",\"checkTime\":1766747332583,\"status\":0}',1,'2025-12-27 03:08:56',NULL,NULL,NULL),('0xdffb458ea2eded34ea5ca376d62eca23984936f7bf584b21f20e11ad02e4c046',10764,'0x3b27bb9221084687abcb2a3ed71eb7d3','recordSales','{\"salesId\":8,\"batteryId\":\"1\",\"buyerName\":\"1231\",\"salesPrice\":12313,\"salesDate\":1766863752000,\"salesPerson\":\"管理员\",\"status\":1,\"auditOpinion\":\"\",\"materialDesc\":\"1323123\",\"materialUrl\":\"[]\"}',1,'2025-12-28 03:29:17',NULL,NULL,NULL),('0xe1b17ecdac0ab43ab152d8d3158328e4d01becf1240f6aec48037b8ee3073843',NULL,'0x5FbDB2315678afecb367f032d93F642f64180aa3','recordTransfer','{\"batteryId\":\"1\",\"fromOwner\":9,\"toOwner\":10,\"actionType\":\"TEST\",\"createTime\":1767027431590}',1,'2025-12-30 00:57:12',31337,NULL,'Odd number of digits at line 1 column 101'),('0xe45a714926e5ea4bdff9011999910014c210549f7f4f87ee7f1014afc24d5a1e',10809,'0x0000000000000000000000000000000000000000','recordSales','{\"status\": 1, \"salesId\": 2, \"batteryId\": \"BAT-202512-0002\", \"buyerName\": \"张三\", \"salesDate\": \"2025-12-18 09:35:00\", \"salesPrice\": 45999.00, \"materialUrl\": null, \"salesPerson\": \"tesla_sh\", \"auditOpinion\": null, \"materialDesc\": null}',1,'2025-12-28 03:05:42',NULL,NULL,NULL),('0xe58e50041f340b9e1d7d619e8fd1fe7011a8458badfb907854f9abc33a4dcde8',12002,'0x63638cc75fdc477f99e6efe3adad0f12','bindVehicle','{\"vehicleId\":null,\"vin\":\"LRWYGCEK7PC000002\",\"batteryId\":\"BAT-202512-0002\",\"brand\":\"特斯拉\",\"model\":\"Model Y\",\"plateNo\":\"沪B54321\",\"ownerId\":4,\"bindTime\":\"2025-12-20T10:15:00\"}',1,'2025-12-20 10:15:00',NULL,NULL,NULL),('0xe7b90a06db2567480286329e1b75bf957a0fc3cc20fe765b572d2d29771f8c7e',2,'0x5FbDB2315678afecb367f032d93F642f64180aa3','recordTransfer','{\"batteryId\":\"1\",\"fromOwner\":9,\"toOwner\":10,\"actionType\":\"TEST2\",\"createTime\":1767027613068}',0,'2025-12-30 01:00:14',31337,'0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266',NULL),('0xec4617a7bd743118292eccbcb5b065287e9b5dfcb96d1cf8dcab3534b67b5662',NULL,'0x5FbDB2315678afecb367f032d93F642f64180aa3','uploadRecyclingPhoto','{\"time\":1767645001828,\"appraisalId\":4,\"batteryId\":\"2\",\"operator\":\"管理员\",\"url\":\"/files/recycling/photos/4/fc95d93d38f84f72bc9a6469a6b88a4b_image - 2025-12-11T143022.063.png\"}',1,'2026-01-06 04:30:02',NULL,'0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266','Failed to connect to /127.0.0.1:8545'),('0xed8eb375133409cd2f164f02340d38f3e6ca38ffb9cd3303a90b6279d0d75b68',900001,'0x0000000000000000000000000000000000000000','recordTransfer','{\"batteryId\":\"3\",\"from\":2,\"to\":3,\"action\":\"TRANSFER\"}',1,'2026-01-01 02:43:20',1,'0x1111111111111111111111111111111111111111',NULL),('0xf7d892174b105e18c09fb718c63433a6d52898b7865f9f6fb6b2a680c9e6aadd',NULL,'0x5FbDB2315678afecb367f032d93F642f64180aa3','bindVehicle','{\"vin\":\"12312312312312312\",\"batteryId\":\"5\",\"bindTime\":1767703678394}',1,'2026-01-06 20:47:58',NULL,'0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266','Failed to connect to /127.0.0.1:8545'),('0xfbe2bf789972f199ff55946a1ff40d8bcf11672652bc9456dc7a4e480b41b7e4',10505,'0xbc794b0f39944409a8c1c3acee9c5355','recordQualityInspection','{\"batteryId\":\"3\",\"ocv\":12312,\"acr\":123123,\"insulationRes\":123213,\"airTightness\":\"PASS\",\"inspector\":\"管理员\",\"checkTime\":1766805334764,\"status\":0}',1,'2025-12-27 19:15:40',NULL,NULL,NULL);
/*!40000 ALTER TABLE `chain_transaction` ENABLE KEYS */;

--
-- Table structure for table `maintenance_record`
--

DROP TABLE IF EXISTS `maintenance_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `maintenance_record` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `battery_id` varchar(64) NOT NULL COMMENT '电池ID',
  `station_id` bigint NOT NULL COMMENT '维修站ID',
  `fault_type` varchar(50) NOT NULL COMMENT '故障类型',
  `description` varchar(500) DEFAULT NULL COMMENT '故障描述',
  `solution` varchar(500) DEFAULT NULL COMMENT '处理措施',
  `replace_parts` varchar(200) DEFAULT NULL COMMENT '更换配件',
  `maintainer` varchar(50) NOT NULL COMMENT '维修人员',
  `create_time` datetime NOT NULL COMMENT '维修时间',
  `tx_hash` varchar(66) DEFAULT NULL COMMENT '交易哈希',
  `status` int DEFAULT '0' COMMENT '状态: 0-待审核, 1-已通过, 2-已驳回',
  `audit_opinion` varchar(500) DEFAULT NULL,
  `issue_material_desc` varchar(500) DEFAULT NULL COMMENT '故障材料说明',
  `issue_material_url` text COMMENT '故障材料文件URL(可为JSON数组)',
  `completion_material_desc` varchar(500) DEFAULT NULL COMMENT '完工材料说明',
  `completion_material_url` text COMMENT '完工材料文件URL(可为JSON数组)',
  `complete_time` datetime DEFAULT NULL,
  `auditor` varchar(50) DEFAULT NULL COMMENT '审核人',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  PRIMARY KEY (`record_id`)
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='维修记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `maintenance_record`
--

/*!40000 ALTER TABLE `maintenance_record` DISABLE KEYS */;
INSERT INTO `maintenance_record` VALUES (19,'BAT-202512-0003',1,'ddd','ddd','123',NULL,'管理员','2026-01-06 00:57:23','0x707d7b18cba2c9a1e980e6006aa5df2ec082b0137e4ca08d0924cc15dc6f5969',3,'','ddd','[\"/files/maintenance/issue/990a789903834731994b965d122a7bf9_cedd27cd74fda13c80bd1726eca6058d.mp4\"]','123','[\"/files/maintenance/completion/6eec79ecbc5042d0b71716e19ca031d6_d4c31bd0260dac01a76f46ec17883b02_3912538206445877688.png\"]','2026-01-06 00:59:07','管理员','2026-01-06 00:58:56'),(20,'TEST-MAINT-1767634219012',1,'TEST','TEST',NULL,NULL,'tester','2026-01-06 01:30:19',NULL,1,'ok','TEST','[]',NULL,NULL,NULL,'auditor1','2026-01-06 01:30:19'),(21,'TEST-MAINT-1767635359528',1,'TEST','TEST',NULL,NULL,'tester','2026-01-06 01:49:19',NULL,1,'ok','TEST','[]',NULL,NULL,NULL,'auditor1','2026-01-06 01:49:20'),(22,'TEST-MAINT-1767635536979',1,'TEST','TEST',NULL,NULL,'tester','2026-01-06 01:52:17',NULL,1,'ok','TEST','[]',NULL,NULL,NULL,'auditor1','2026-01-06 01:52:17'),(23,'1',1,'电压异常','示例：充放电过程中电压波动偏大','复位BMS并进行质检复测','无','管理员','2026-01-03 02:44:17','0xcab4c760734ec1fabcd0cb50966358356758d44632c5e4fc76e17fc78a3d81b4',3,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(24,'2',1,'电压异常','示例：充放电过程中电压波动偏大','复位BMS并进行质检复测','无','管理员','2026-01-03 02:44:17','0x25b12772cc17bfda0605c3a3351d88ea1cbd8add2f5a2183f2c413ff9013c823',3,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(25,'3',1,'电压异常','示例：充放电过程中电压波动偏大','复位BMS并进行质检复测','无','管理员','2026-01-03 02:44:17','0x4f960f994d65b1f3a753eb840017790fb8ad6ac4cf2b4971e58141acc1b54b5a',3,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(26,'5',1,'电压异常','示例：充放电过程中电压波动偏大','复位BMS并进行质检复测','无','管理员','2026-01-03 02:44:17','0xd82f8d0a7bc0d89cf3aeb3a7a5f252c28950e48b1bfa1017aacf2c8252c8dea2',3,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(27,'BAT-202512-0001',1,'电压异常','示例：充放电过程中电压波动偏大','复位BMS并进行质检复测','无','管理员','2026-01-03 02:44:17','0xce7c6b6f02a174eb57d834ec4166473d4421fd9df2254a354ef9d51dc5f8594e',3,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(28,'BAT-202512-0002',1,'电压异常','示例：充放电过程中电压波动偏大','复位BMS并进行质检复测','无','管理员','2026-01-03 02:44:17','0x443d6ceb9aa96e3610f16dc952674c27802a4b5f338c82187a8340abae0ba776',3,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(29,'BAT-202512-0004',1,'电压异常','示例：充放电过程中电压波动偏大','复位BMS并进行质检复测','无','管理员','2026-01-03 02:44:17','0x6a5623259faf7361931d6eca05ee51b1161ff4902ebf056cb24046ea526b1c09',3,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(30,'TEST-MAINT-1767654671457',1,'TEST','TEST',NULL,NULL,'tester','2026-01-06 07:11:11',NULL,1,'ok','TEST','[]',NULL,NULL,NULL,'auditor1','2026-01-06 07:11:12');
/*!40000 ALTER TABLE `maintenance_record` ENABLE KEYS */;

--
-- Table structure for table `production_batch`
--

DROP TABLE IF EXISTS `production_batch`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `production_batch` (
  `batch_id` bigint NOT NULL AUTO_INCREMENT COMMENT '批次ID',
  `batch_no` varchar(50) NOT NULL COMMENT '批次编号',
  `manufacturer_id` bigint NOT NULL COMMENT '生产商ID',
  `produce_date` date NOT NULL COMMENT '生产日期',
  `quantity` int NOT NULL COMMENT '计划生产数量',
  PRIMARY KEY (`batch_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='生产批次表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `production_batch`
--

/*!40000 ALTER TABLE `production_batch` DISABLE KEYS */;
INSERT INTO `production_batch` VALUES (4,'123321',1,'2025-12-27',1);
/*!40000 ALTER TABLE `production_batch` ENABLE KEYS */;

--
-- Table structure for table `production_batch_record`
--

DROP TABLE IF EXISTS `production_batch_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `production_batch_record` (
  `record_id` bigint NOT NULL AUTO_INCREMENT,
  `batch_id` bigint NOT NULL COMMENT '批次ID',
  `batch_no` varchar(64) DEFAULT NULL COMMENT '批次号',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `operator` varchar(64) DEFAULT NULL COMMENT '操作人',
  `status` int DEFAULT '0' COMMENT '状态',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`record_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='生产批次记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `production_batch_record`
--

/*!40000 ALTER TABLE `production_batch_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `production_batch_record` ENABLE KEYS */;

--
-- Table structure for table `quality_inspection`
--

DROP TABLE IF EXISTS `quality_inspection`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `quality_inspection` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `battery_id` varchar(64) NOT NULL COMMENT '电池ID',
  `ocv` decimal(10,4) DEFAULT NULL,
  `acr` decimal(10,4) DEFAULT NULL,
  `insulation_res` decimal(10,2) DEFAULT NULL,
  `air_tightness` varchar(20) DEFAULT NULL,
  `inspector` varchar(50) NOT NULL COMMENT '质检员',
  `check_time` datetime NOT NULL COMMENT '检测时间',
  `data_hash` varchar(66) NOT NULL COMMENT '数据哈希',
  `status` int DEFAULT '0' COMMENT '状态: 0-待审核, 1-已通过, 2-已驳回',
  `audit_opinion` varchar(255) DEFAULT NULL COMMENT '审核意见',
  `auditor` varchar(50) DEFAULT NULL COMMENT '审核人',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='出厂质检表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `quality_inspection`
--

/*!40000 ALTER TABLE `quality_inspection` DISABLE KEYS */;
INSERT INTO `quality_inspection` VALUES (4,'1',NULL,NULL,NULL,NULL,'管理员','2025-12-26 19:06:48','0xb0e6cf89e4955f07bac3099ad769f4fed0135940f462f0214540170cfec3f53a',1,'','admin','2025-12-27 03:09:05'),(5,'1',NULL,NULL,NULL,NULL,'管理员','2025-12-26 19:08:53','0xde798406ec325a2e932725f35888773438cb671ecf607d0d8fa7fc81c4330fd2',1,'','admin','2025-12-27 03:09:04'),(6,'2',12.0000,12.0000,12.00,'PASS','管理员','2025-12-26 19:22:47','0x4816308f3cad80e61009cc6336dc0663631d492bb869688c3c0a48c0645f83d0',1,'','admin','2025-12-27 03:23:27'),(7,'3',3.0000,3.0000,3.00,'PASS','管理员','2025-12-26 19:23:58','0x4980e4a733e9eb7496158d3e26c09a213b294ccba665567bae9f1765c251ccb4',1,'','admin','2025-12-27 03:25:39'),(8,'4',4.0000,4.0000,4.00,'PASS','管理员','2025-12-27 08:56:31','0x55a9af09d06cf5cbfb7899c98cee1db3165d4946a870d593e0f456c1fa8dd728',1,'','admin','2025-12-27 16:56:40'),(9,'1',3.0000,3.0000,3.00,'PASS','管理员','2025-12-27 09:12:06','0x9b9e786d52d98f1730e7e49a09d39971f7d3dc325f5ae8bc86e082339ff82858',1,'','admin','2025-12-27 17:12:14'),(10,'3',12312.0000,123123.0000,123213.00,'PASS','管理员','2025-12-27 11:15:35','0xfbe2bf789972f199ff55946a1ff40d8bcf11672652bc9456dc7a4e480b41b7e4',1,'','admin','2025-12-27 19:15:58'),(11,'5',NULL,NULL,NULL,'PASS','管理员','2026-01-06 12:27:08','0xb2978242177eb61ff34ee3c87d4844ba13441a27a0d469c89fe80ab5feda6f2e',1,'','admin','2026-01-06 20:27:20');
/*!40000 ALTER TABLE `quality_inspection` ENABLE KEYS */;

--
-- Table structure for table `recycling_appraisal`
--

DROP TABLE IF EXISTS `recycling_appraisal`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recycling_appraisal` (
  `appraisal_id` bigint NOT NULL AUTO_INCREMENT COMMENT '评估ID',
  `battery_id` varchar(64) NOT NULL COMMENT '电池ID',
  `recycler_id` bigint NOT NULL COMMENT '回收商ID',
  `appearance` varchar(200) DEFAULT NULL COMMENT '外观检查',
  `residual_value` decimal(10,2) DEFAULT NULL COMMENT '估算残值',
  `suggestion` varchar(20) NOT NULL COMMENT '处置建议',
  `appraiser` varchar(50) NOT NULL COMMENT '评估人',
  `create_time` datetime NOT NULL COMMENT '评估时间',
  `data_hash` varchar(66) NOT NULL COMMENT '数据哈希',
  `status` int DEFAULT '0',
  `apply_reason` varchar(500) DEFAULT NULL,
  `apply_user` varchar(50) DEFAULT NULL,
  `apply_time` datetime DEFAULT NULL,
  `snapshot_voltage` decimal(10,2) DEFAULT NULL,
  `snapshot_capacity` decimal(10,2) DEFAULT NULL,
  `snapshot_battery_status` int DEFAULT NULL,
  `recycle_no` varchar(64) DEFAULT NULL,
  `photo_urls` text,
  `performance_report_url` text,
  `maintenance_history_json` longtext,
  `valuation_basis` longtext,
  `preliminary_value` decimal(10,2) DEFAULT NULL,
  `final_value` decimal(10,2) DEFAULT NULL,
  `price_reviewer` varchar(50) DEFAULT NULL,
  `recycle_time` datetime DEFAULT NULL,
  `receipt_hash` varchar(66) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`appraisal_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='回收评估表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `recycling_appraisal`
--

/*!40000 ALTER TABLE `recycling_appraisal` DISABLE KEYS */;
INSERT INTO `recycling_appraisal` VALUES (2,'BAT-202512-0003',6,'外观轻微划痕，壳体无鼓包，接口正常',18888.50,'梯次利用','邦普回收员','2026-01-05 12:00:00','0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',1,NULL,NULL,NULL,NULL,NULL,NULL,'RCY202601062354388755',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'0x6973a2cbadd8cef41ff971b4b3027606bd4e80380384020624392e35bbef1efd','2026-01-06 23:54:38'),(3,'1',6,'外观完好，少量擦痕(示例)',49.48,'梯次利用','邦普回收员','2026-01-04 02:44:40','0x148159b5e5fe3525e509a67b12f5e2b95740db6cbc4c652e0bdcab7b87087e50',3,NULL,NULL,NULL,NULL,NULL,NULL,'RCY202601062340215575','[\"/files/recycling/photos/3/ba20db9ecb0c4709a32f4e05ecdc90d1_d4c31bd0260dac01a76f46ec17883b02_3912538206445877688.png\",\"/files/recycling/photos/3/d868f58b89e14e32a525c8e04055acf1_image - 2025-12-11T143019.844.png\",\"/files/recycling/photos/3/569b0c38ae1d46888c5d099473b4c548_image - 2025-12-11T143021.261.png\"]','/files/recycling/reports/3/4206228aff7b4dd781e12b4e20d3a0da_cedd27cd74fda13c80bd1726eca6058d.mp4','[{\"recordId\":23,\"batteryId\":\"1\",\"stationId\":1,\"faultType\":\"电压异常\",\"description\":\"示例：充放电过程中电压波动偏大\",\"solution\":\"复位BMS并进行质检复测\",\"replaceParts\":\"无\",\"maintainer\":\"管理员\",\"createTime\":1767379457000,\"txHash\":\"0xcab4c760734ec1fabcd0cb50966358356758d44632c5e4fc76e17fc78a3d81b4\",\"status\":3}]','auto; operator=管理员; time=2026-01-07T00:05:32.627144600; batteryId=1; maintenanceCount=1; transferCount=2; batteryStatus=5',49.48,50.00,'管理员','2026-01-07 00:05:59','0x7ec14bd9e41a39313d0d0fa9ad84d23c1f9b189304cbd59d235c30b156341ebd','2026-01-07 00:05:59'),(4,'2',6,'外观完好，少量擦痕(示例)',0.00,'梯次利用','邦普回收员','2026-01-04 02:44:40','0xa124012c4136f9d3f0e875b851a99a7f51c8bfd7fab61e665969c498fbfa696c',3,NULL,NULL,NULL,NULL,NULL,NULL,'RCY202601060429148937','[\"/files/recycling/photos/4/cbde858185d243b5805633d405f49f2a_d4c31bd0260dac01a76f46ec17883b02_3912538206445877688.png\",\"/files/recycling/photos/4/36ecfe14739a4f0cb21b7a8c73f6e25d_image - 2025-12-11T143023.608.png\",\"/files/recycling/photos/4/b52b84aa421344b2821c5305a1e90fcd_image - 2025-12-11T143023.608.png\"]','/files/recycling/reports/4/a19c551bb1f24e1b92f946fd11d2d830_image - 2025-12-11T143021.261.png','[{\"recordId\":24,\"batteryId\":\"2\",\"stationId\":1,\"faultType\":\"电压异常\",\"description\":\"示例：充放电过程中电压波动偏大\",\"solution\":\"复位BMS并进行质检复测\",\"replaceParts\":\"无\",\"maintainer\":\"管理员\",\"createTime\":1767379457000,\"txHash\":\"0x25b12772cc17bfda0605c3a3351d88ea1cbd8add2f5a2183f2c413ff9013c823\",\"status\":3}]','ai_linear_regression_chain_dataset; features=[capacity, voltage, capacityVoltage, chainTransferCount, chainMaintenanceCount, chainQualityInspectionCount, chainSalesCount, lastTransferDays, lastMaintenanceDays, lastQualityInspectionDays, latestOcv, latestAcr, latestInsulationRes, latestAirTightnessOk]; capacity=2.00, voltage=2.00, capacityVoltage=4.0000; chain.transferCount=1, chain.maintenanceCount=1, chain.qualityInspectionCount=1, chain.salesCount=1, chain.lastTransferDays=5, chain.lastMaintenanceDays=3, chain.lastQualityInspectionDays=10, chain.latestOcv=12.0, chain.latestAcr=12.0, chain.latestInsulationRes=12.0, chain.latestAirTightnessOk=1; samples=0',0.00,123.00,'管理员','2026-01-06 22:29:16','0x943fd26e8162b37e57e33f69d1680dd7ddea677f27fe775fd87a90fa5e056514','2026-01-06 22:29:16'),(5,'3',6,'外观完好，少量擦痕(示例)',19999.00,'梯次利用','邦普回收员','2026-01-04 02:44:40','0x2495d3e1718b96f4f9c218e080bc68487c7e72123d6ab9167e3a6b1c249bf5c3',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(6,'5',6,'外观完好，少量擦痕(示例)',19999.00,'梯次利用','邦普回收员','2026-01-04 02:44:40','0x2179ebabb1fd8eb04152a48564cd1b68a065e918503acea90ed0c1dd3a45710f',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(7,'BAT-202512-0001',6,'外观完好，少量擦痕(示例)',19999.00,'梯次利用','邦普回收员','2026-01-04 02:44:40','0x285e993e43a1adfc966c2a6aafaf56d2164156e0707e695d2de7ff6db8ca631c',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(8,'BAT-202512-0002',6,'外观完好，少量擦痕(示例)',19999.00,'梯次利用','邦普回收员','2026-01-04 02:44:40','0xc095adaa726a9cd1ea683f95b4c96d76b7ac93dfd98d472e9496b277dd4134df',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(9,'BAT-202512-0004',6,'外观完好，少量擦痕(示例)',19999.00,'梯次利用','邦普回收员','2026-01-04 02:44:40','0x6789db5095cc2a780967bc999e6437a1618545b1e78f693c7e05e57ccb5afe93',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `recycling_appraisal` ENABLE KEYS */;

--
-- Table structure for table `sales_record`
--

DROP TABLE IF EXISTS `sales_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sales_record` (
  `sales_id` bigint NOT NULL AUTO_INCREMENT,
  `battery_id` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
  `buyer_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `sales_price` decimal(10,2) DEFAULT NULL,
  `sales_date` datetime DEFAULT NULL,
  `sales_person` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `status` int DEFAULT '0' COMMENT '状态: 0-待审核, 1-已通过, 2-已驳回',
  `audit_opinion` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `material_desc` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '材料说明',
  `material_url` text COLLATE utf8mb4_general_ci COMMENT '材料文件URL(可为JSON数组)',
  `tx_hash` varchar(66) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '交易哈希',
  PRIMARY KEY (`sales_id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sales_record`
--

/*!40000 ALTER TABLE `sales_record` DISABLE KEYS */;
INSERT INTO `sales_record` VALUES (1,'BAT-202512-0001','张三',49999.00,'2025-12-18 09:20:00','tesla_sh',1,NULL,NULL,NULL,'0x33f40f3e1ec0b6a856dae736c3db759486f7dccab3b69969c9e9917a61924f4d'),(2,'BAT-202512-0002','张三',45999.00,'2025-12-18 09:35:00','tesla_sh',1,NULL,NULL,NULL,'0xe45a714926e5ea4bdff9011999910014c210549f7f4f87ee7f1014afc24d5a1e'),(3,'BAT-202512-0003','李四',42999.00,'2025-12-19 15:10:00','tesla_sh',1,NULL,NULL,NULL,'0x0cd8f1676569106fa268039f96d736fa0ebc9571ba5b12d9c4e915617822adc8'),(4,'BAT-202512-0004','王五',56999.00,'2025-12-19 16:00:00','tesla_sh',1,NULL,NULL,NULL,'0xbf204a47baf4b06e164afa445cf370e618d841e59f686209e09713f938dc745d'),(5,'3','test',12.00,'2025-12-28 01:03:26','管理员',1,'','我是测试','[\"/files/sales/26b6acc0ece74b9aa4e396d8bb036af0_main.txt\"]','0x695c6c36c6a474f52abea8d7c9425fe197d436497a6f91a3178b460c6e8a70a1'),(6,'1','123',123.00,'2025-12-28 01:37:00','管理员',3,'','12312312','[\"/files/sales/b9fe8071c42447f49b5a6f95c29f8ef6_main.txt\",\"/files/sales/97d261e2baa745bbb41c5d7f9d49f7fd_881高等代数.doc\"]',NULL),(7,'1','12313',1231313.00,'2025-12-28 02:42:59','管理员',3,'','213123','[\"/files/sales/09ef83ae0e524b99876a2801ed33bffe_cedd27cd74fda13c80bd1726eca6058d.mp4\",\"/files/sales/680402293ed94c988ec275132d19ede8_d4c31bd0260dac01a76f46ec17883b02_3912538206445877688.png\",\"/files/sales/7e6cd79cfd554089aa65e9d442f122d2_image - 2025-12-11T143019.102.png\"]',NULL),(8,'1','1231',12313.00,'2025-12-28 03:29:12','管理员',1,'','1323123','[]','0xdffb458ea2eded34ea5ca376d62eca23984936f7bf584b21f20e11ad02e4c046'),(9,'2','张三',39999.00,'2026-01-02 02:43:52','tesla_sh',1,'示例数据自动生成','销售材料(示例)',NULL,'0xd644124959e9e08d96b878212a6ab03fe3b5d07e6b7f019cd3b0b7c853538f5c'),(10,'5','张三',39999.00,'2026-01-02 02:43:52','tesla_sh',1,'示例数据自动生成','销售材料(示例)',NULL,'0xda8f348556859fd8b444d49d6c16fd3b6b2d4e490f44a395a9175131b357ff0c');
/*!40000 ALTER TABLE `sales_record` ENABLE KEYS */;

--
-- Table structure for table `sys_audit`
--

DROP TABLE IF EXISTS `sys_audit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_audit` (
  `audit_id` bigint NOT NULL AUTO_INCREMENT,
  `business_type` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '业务类型: QUALITY, MAINTENANCE, SALES',
  `business_id` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '业务ID',
  `apply_user` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '申请人',
  `apply_time` datetime DEFAULT NULL COMMENT '申请时间',
  `status` int DEFAULT '0' COMMENT '状态: 0-待审核, 1-已通过, 2-已驳回',
  `auditor` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审核人',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `audit_opinion` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审核意见',
  PRIMARY KEY (`audit_id`)
) ENGINE=InnoDB AUTO_INCREMENT=62 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_audit`
--

/*!40000 ALTER TABLE `sys_audit` DISABLE KEYS */;
INSERT INTO `sys_audit` VALUES (4,'QUALITY','4','管理员','2025-12-27 03:06:52',1,'admin','2025-12-27 03:09:05',''),(5,'QUALITY','5','管理员','2025-12-27 03:08:56',1,'admin','2025-12-27 03:09:04',''),(6,'QUALITY','6','管理员','2025-12-27 03:22:55',1,'admin','2025-12-27 03:23:27',''),(7,'QUALITY','7','管理员','2025-12-27 03:24:02',1,'admin','2025-12-27 03:25:39',''),(8,'QUALITY','8','管理员','2025-12-27 16:56:35',1,'admin','2025-12-27 16:56:40',''),(9,'QUALITY','9','管理员','2025-12-27 17:12:10',1,'admin','2025-12-27 17:12:14',''),(10,'QUALITY','10','管理员','2025-12-27 19:15:40',1,'admin','2025-12-27 19:15:58',''),(11,'SALES','5','管理员','2025-12-28 01:03:26',2,'管理员','2025-12-28 01:07:53',''),(12,'SALES','5','管理员','2025-12-28 01:25:09',2,'管理员','2025-12-28 01:26:15','我是测试'),(13,'SALES','5','管理员','2025-12-28 01:26:25',2,'管理员','2025-12-28 01:36:17',''),(14,'SALES','5','管理员','2025-12-28 01:36:27',1,'管理员','2025-12-28 01:36:32',''),(15,'SALES','6','管理员','2025-12-28 01:37:00',2,'管理员','2025-12-28 01:38:02',''),(16,'SALES','6','管理员','2025-12-28 01:57:04',2,'管理员','2025-12-28 02:03:19',''),(17,'SALES','6','管理员','2025-12-28 02:03:48',2,'管理员','2025-12-28 02:23:50',''),(18,'SALES','7','管理员','2025-12-28 02:42:59',2,'管理员','2025-12-28 02:43:21',''),(50,'SALES','8','管理员','2025-12-28 03:29:12',1,'管理员','2025-12-28 03:29:17',''),(51,'RECYCLING','BAT-202512-0001','管理员','2025-12-28 04:32:22',0,NULL,NULL,NULL),(56,'RECYCLING','4','system','2026-01-06 04:28:58',1,'管理员','2026-01-06 04:29:15','123'),(57,'MAINTENANCE','30','tester','2026-01-06 07:11:11',1,'auditor1','2026-01-06 07:11:12','ok'),(58,'QUALITY','11','管理员','2026-01-06 20:27:12',1,'admin','2026-01-06 20:27:20',''),(59,'RECYCLING','3','system','2026-01-06 23:12:27',1,'管理员','2026-01-06 23:40:22','ok'),(60,'RECYCLING','5','system','2026-01-06 23:18:55',0,NULL,NULL,NULL),(61,'RECYCLING','2','system','2026-01-06 23:54:38',1,'管理员','2026-01-06 23:54:38',NULL);
/*!40000 ALTER TABLE `sys_audit` ENABLE KEYS */;

--
-- Table structure for table `sys_dept`
--

DROP TABLE IF EXISTS `sys_dept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dept` (
  `dept_id` bigint NOT NULL AUTO_INCREMENT COMMENT '部门ID',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父部门ID',
  `dept_name` varchar(50) NOT NULL COMMENT '部门名称',
  `order_num` int DEFAULT '0' COMMENT '显示顺序',
  `leader` varchar(20) DEFAULT NULL COMMENT '负责人',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '部门状态 0:正常, 1:停用',
  PRIMARY KEY (`dept_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='部门/机构表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_dept`
--

/*!40000 ALTER TABLE `sys_dept` DISABLE KEYS */;
INSERT INTO `sys_dept` VALUES (1,0,'平台总部',1,NULL,NULL,'0'),(2,0,'宁德时代',2,NULL,NULL,'0'),(3,0,'特斯拉上海4S店',3,NULL,NULL,'0'),(4,0,'个人车主组',4,NULL,NULL,'0'),(5,0,'阳光专业维修站',5,NULL,NULL,'0'),(6,0,'邦普循环回收',6,NULL,NULL,'0');
/*!40000 ALTER TABLE `sys_dept` ENABLE KEYS */;

--
-- Table structure for table `sys_dict`
--

DROP TABLE IF EXISTS `sys_dict`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict` (
  `dict_code` bigint NOT NULL AUTO_INCREMENT COMMENT '字典编码',
  `dict_label` varchar(100) NOT NULL COMMENT '字典标签',
  `dict_value` varchar(100) NOT NULL COMMENT '字典键值',
  `dict_type` varchar(100) NOT NULL COMMENT '字典类型',
  PRIMARY KEY (`dict_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据字典表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_dict`
--

/*!40000 ALTER TABLE `sys_dict` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_dict` ENABLE KEYS */;

--
-- Table structure for table `sys_menu`
--

DROP TABLE IF EXISTS `sys_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_menu` (
  `menu_id` bigint NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `menu_name` varchar(50) NOT NULL COMMENT '菜单名称',
  `parent_id` bigint NOT NULL COMMENT '父菜单ID',
  `order_num` int DEFAULT '0' COMMENT '显示顺序',
  `path` varchar(200) DEFAULT NULL COMMENT '路由地址',
  `component` varchar(255) DEFAULT NULL COMMENT '组件路径',
  `perms` varchar(100) DEFAULT NULL COMMENT '权限标识',
  `menu_type` char(1) NOT NULL COMMENT '菜单类型 M:目录, C:菜单, F:按钮',
  PRIMARY KEY (`menu_id`)
) ENGINE=InnoDB AUTO_INCREMENT=52 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜单权限表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_menu`
--

/*!40000 ALTER TABLE `sys_menu` DISABLE KEYS */;
INSERT INTO `sys_menu` VALUES (1,'系统管理',0,1,'/system',NULL,'system:list','M'),(2,'用户管理',1,1,'user','system/user/index','system:user:list','C'),(3,'角色管理',1,2,'role','system/role/index','system:role:list','C'),(4,'菜单管理',1,3,'menu','system/menu/index','system:menu:list','C'),(5,'部门管理',1,4,'dept','system/dept/index','system:dept:list','C'),(10,'电池管理',0,2,'/battery',NULL,'battery:list','M'),(11,'电池信息',10,1,'info','battery/info/index','battery:info:list','C'),(12,'生产批次',10,2,'batch','battery/batch/index','battery:batch:list','C'),(13,'出厂质检',10,3,'quality','battery/quality/index','battery:quality:list','C'),(20,'销售管理',0,3,'/sales',NULL,'sales:list','M'),(21,'车电绑定',20,1,'binding','sales/binding/index','sales:binding:list','C'),(22,'流转记录',20,2,'transfer','sales/transfer/index','sales:transfer:list','C'),(30,'维修保养',0,4,'/maintenance',NULL,'maintenance:list','M'),(31,'健康监测',30,1,'monitor','maintenance/monitor/index','maintenance:monitor:list','C'),(32,'维修记录',30,2,'record','maintenance/record/index','maintenance:record:list','C'),(33,'故障报警',30,3,'alarm','maintenance/alarm/index','maintenance:alarm:list','C'),(40,'回收利用',0,5,'/recycling',NULL,'recycling:list','M'),(41,'回收评估',40,1,'appraisal','recycling/appraisal/index','recycling:appraisal:list','C'),(50,'溯源查询',0,6,'/trace','trace/index','trace:query','C');
/*!40000 ALTER TABLE `sys_menu` ENABLE KEYS */;

--
-- Table structure for table `sys_role`
--

DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role` (
  `role_id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` varchar(50) NOT NULL COMMENT '角色名称',
  `role_key` varchar(50) NOT NULL COMMENT '角色权限字符串',
  `status` int NOT NULL DEFAULT '0' COMMENT '角色状态 0:正常, 1:停用',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role`
--

/*!40000 ALTER TABLE `sys_role` DISABLE KEYS */;
INSERT INTO `sys_role` VALUES (1,'超级管理员','admin',0,'2025-12-26 20:01:08'),(2,'生产商','manufacturer',0,'2025-12-26 20:01:08'),(3,'经销商','dealer',0,'2025-12-26 20:01:08'),(4,'车主','owner',0,'2025-12-26 20:01:08'),(5,'维修站','maintenance',0,'2025-12-26 20:01:08'),(6,'回收商','recycler',0,'2025-12-26 20:01:08');
/*!40000 ALTER TABLE `sys_role` ENABLE KEYS */;

--
-- Table structure for table `sys_role_menu`
--

DROP TABLE IF EXISTS `sys_role_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_menu` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`role_id`,`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色菜单关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role_menu`
--

/*!40000 ALTER TABLE `sys_role_menu` DISABLE KEYS */;
INSERT INTO `sys_role_menu` VALUES (1,1),(1,2),(1,3),(1,4),(1,5),(1,10),(1,11),(1,12),(1,13),(1,20),(1,21),(1,22),(1,30),(1,31),(1,32),(1,33),(1,40),(1,41),(1,50),(2,10),(2,11),(2,12),(2,13),(2,50),(3,20),(3,21),(3,22),(3,50),(4,50),(5,30),(5,31),(5,32),(5,33),(5,50),(6,40),(6,41),(6,50);
/*!40000 ALTER TABLE `sys_role_menu` ENABLE KEYS */;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `dept_id` bigint NOT NULL COMMENT '部门ID',
  `username` varchar(50) NOT NULL COMMENT '用户账号',
  `password` varchar(128) DEFAULT NULL,
  `nickname` varchar(50) DEFAULT NULL COMMENT '用户昵称',
  `email` varchar(50) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号码',
  `status` int NOT NULL DEFAULT '0' COMMENT '帐号状态 0:正常, 1:停用',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `chain_account` varchar(42) DEFAULT NULL COMMENT '链上账户地址',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user`
--

/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
INSERT INTO `sys_user` VALUES (1,1,'admin','sha256$e6af666f062740f2aa78bcc900e20d20$f674cb8f25cba980acf6abca1815f17b86ee08efd7ed5839fb811e2a9b1bae3e','管理员',NULL,NULL,0,'2025-12-26 19:46:10','2026-01-06 23:40:22',NULL),(2,2,'catl','sha256$8a718c6b407f433d9637c44605fb76e4$f23d7399e75671f50a31ca3169dc200f4f86263b7196cdc751ed215ff463ae11','宁德时代操作员',NULL,NULL,0,'2025-12-26 20:01:20','2026-01-06 23:47:03',NULL),(3,3,'tesla_sh','123456','特斯拉上海操作员',NULL,NULL,0,'2025-12-26 20:01:20','2025-12-28 00:01:28',NULL),(4,4,'user01','123456','张三',NULL,NULL,0,'2025-12-26 20:01:20','2026-01-06 02:45:24',NULL),(5,5,'fix_station','123456','李师傅',NULL,NULL,0,'2025-12-26 20:01:20','2025-12-28 00:01:49',NULL),(6,6,'brunp','123456','邦普回收员',NULL,NULL,0,'2025-12-26 20:01:20','2025-12-27 19:16:53',NULL),(9,4,'user02','123456','李四','lisi@example.com','13800000002',0,'2025-12-27 23:19:09','2025-12-30 00:48:08',NULL),(10,4,'user03','123456','王五','wangwu@example.com','13800000003',0,'2025-12-27 23:19:14','2025-12-27 23:19:14',NULL),(11,6,'recycler_test','123456','recycler_test',NULL,NULL,0,'2026-01-06 15:52:22','2026-01-06 15:53:30',NULL);
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;

--
-- Table structure for table `sys_user_role`
--

DROP TABLE IF EXISTS `sys_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_role` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user_role`
--

/*!40000 ALTER TABLE `sys_user_role` DISABLE KEYS */;
INSERT INTO `sys_user_role` VALUES (1,1),(2,2),(3,3),(4,4),(5,5),(6,6),(9,4),(10,4),(11,6);
/*!40000 ALTER TABLE `sys_user_role` ENABLE KEYS */;

--
-- Table structure for table `vehicle_info`
--

DROP TABLE IF EXISTS `vehicle_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vehicle_info` (
  `vehicle_id` bigint NOT NULL AUTO_INCREMENT COMMENT '车辆ID',
  `vin` varchar(17) NOT NULL COMMENT '车架号(VIN)',
  `battery_id` varchar(64) NOT NULL COMMENT '电池ID',
  `brand` varchar(50) NOT NULL COMMENT '品牌',
  `model` varchar(50) NOT NULL COMMENT '车型',
  `plate_no` varchar(20) DEFAULT NULL COMMENT '车牌号',
  `owner_id` bigint NOT NULL COMMENT '车主ID',
  `bind_time` datetime NOT NULL COMMENT '绑定时间',
  PRIMARY KEY (`vehicle_id`),
  UNIQUE KEY `uk_vin` (`vin`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `vehicle_info`
--

/*!40000 ALTER TABLE `vehicle_info` DISABLE KEYS */;
INSERT INTO `vehicle_info` VALUES (1,'LRW3E7EK5NC000001','BAT-202512-0001','特斯拉','Model 3','沪A12345',4,'2025-12-20 10:00:00'),(2,'LRWYGCEK7PC000002','BAT-202512-0002','特斯拉','Model Y','沪B54321',4,'2025-12-20 10:15:00'),(3,'LC0CE4CB1P0000003','BAT-202512-0003','比亚迪','汉 EV','粤B88888',9,'2025-12-21 09:10:00'),(4,'LJ1EAZKB0N0000004','BAT-202512-0004','蔚来','ES6','京A66666',10,'2025-12-21 11:30:00'),(5,'12345678900987654','2','byd','byd1型','12345',1,'2026-01-06 21:00:48');
/*!40000 ALTER TABLE `vehicle_info` ENABLE KEYS */;

--
-- Dumping routines for database 'bishe'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-01-07  0:22:16
