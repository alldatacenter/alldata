-- Licensed to the Apache Software Foundation (ASF) under one or more
-- contributor license agreements.  See the NOTICE file distributed with
-- this work for additional information regarding copyright ownership.
-- The ASF licenses this file to You under the Apache License, Version 2.0
-- (the "License"); you may not use this file except in compliance with
-- the License.  You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.

-- MySQL dump 10.13  Distrib 5.1.50, for apple-darwin10.3.0 (i386)
--
-- Host: localhost    Database: xa_db
-- ------------------------------------------------------
-- Server version	5.1.50

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `xa_access_audit`
--

DROP TABLE IF EXISTS `xa_access_audit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `xa_access_audit` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `added_by_id` bigint(20) DEFAULT NULL,
  `upd_by_id` bigint(20) DEFAULT NULL,
  `audit_type` int(11) NOT NULL DEFAULT '0',
  `access_result` int(11) DEFAULT '0',
  `access_type` varchar(255) DEFAULT NULL,
  `acl_enforcer` varchar(255) DEFAULT NULL,
  `agent_id` varchar(255) DEFAULT NULL,
  `client_ip` varchar(255) DEFAULT NULL,
  `client_type` varchar(255) DEFAULT NULL,
  `policy_id` bigint(20) DEFAULT '0',
  `repo_name` varchar(255) DEFAULT NULL,
  `repo_type` int(11) DEFAULT '0',
  `result_reason` varchar(255) DEFAULT NULL,
  `session_id` varchar(255) DEFAULT NULL,
  `event_time` datetime DEFAULT NULL,
  `request_user` varchar(255) DEFAULT NULL,
  `action` varchar(2000) DEFAULT NULL,
  `request_data` varchar(4000) DEFAULT NULL,
  `resource_path` varchar(4000) DEFAULT NULL,
  `resource_type` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `xa_access_audit_added_by_id` (`added_by_id`),
  KEY `xa_access_audit_upd_by_id` (`upd_by_id`),
  KEY `xa_access_audit_cr_time` (`create_time`),
  KEY `xa_access_audit_up_time` (`update_time`),
  KEY `xa_access_audit_event_time` (`event_time`)
)DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `xa_access_audit`
--

LOCK TABLES `xa_access_audit` WRITE;
/*!40000 ALTER TABLE `xa_access_audit` DISABLE KEYS */;
/*!40000 ALTER TABLE `xa_access_audit` ENABLE KEYS */;
UNLOCK TABLES;

-- Dump completed on 2014-05-25  0:07:27
