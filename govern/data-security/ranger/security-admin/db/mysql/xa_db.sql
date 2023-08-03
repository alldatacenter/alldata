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
-- Current Database: `xa_db`
--

/*!40000 DROP DATABASE IF EXISTS `xa_db`*/;

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `xa_db` /*!40100 DEFAULT CHARACTER SET latin1 */;

USE `xa_db`;

--
-- Temporary table structure for view `vx_trx_log`
--

DROP TABLE IF EXISTS `vx_trx_log`;
/*!50001 DROP VIEW IF EXISTS `vx_trx_log`*/;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
/*!50001 CREATE TABLE `vx_trx_log` (
  `id` bigint(20),
  `create_time` datetime,
  `update_time` datetime,
  `added_by_id` bigint(20),
  `upd_by_id` bigint(20),
  `class_type` int(11),
  `object_id` bigint(20),
  `parent_object_id` bigint(20),
  `parent_object_class_type` int(11),
  `attr_name` varchar(255),
  `parent_object_name` varchar(1024),
  `object_name` varchar(1024),
  `prev_val` varchar(1024),
  `new_val` varchar(1024),
  `trx_id` varchar(1024),
  `action` varchar(255),
  `sess_id` varchar(512),
  `req_id` varchar(30),
  `sess_type` varchar(30)
) ENGINE=MyISAM */;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `x_asset`
--

DROP TABLE IF EXISTS `x_asset`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `x_asset` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `added_by_id` bigint(20) DEFAULT NULL,
  `upd_by_id` bigint(20) DEFAULT NULL,
  `asset_name` varchar(1024) NOT NULL,
  `descr` varchar(4000) NOT NULL,
  `act_status` int(11) NOT NULL DEFAULT '0',
  `asset_type` int(11) NOT NULL DEFAULT '0',
  `config` text,
  `sup_native` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `x_asset_FK_added_by_id` (`added_by_id`),
  KEY `x_asset_FK_upd_by_id` (`upd_by_id`),
  KEY `x_asset_cr_time` (`create_time`),
  KEY `x_asset_up_time` (`update_time`),
  CONSTRAINT `x_asset_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_asset_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `x_asset`
--

LOCK TABLES `x_asset` WRITE;
/*!40000 ALTER TABLE `x_asset` DISABLE KEYS */;
/*!40000 ALTER TABLE `x_asset` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `x_audit_map`
--

DROP TABLE IF EXISTS `x_audit_map`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `x_audit_map` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `added_by_id` bigint(20) DEFAULT NULL,
  `upd_by_id` bigint(20) DEFAULT NULL,
  `res_id` bigint(20) DEFAULT NULL,
  `group_id` bigint(20) DEFAULT NULL,
  `user_id` bigint(20) DEFAULT NULL,
  `audit_type` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `x_audit_map_FK_added_by_id` (`added_by_id`),
  KEY `x_audit_map_FK_upd_by_id` (`upd_by_id`),
  KEY `x_audit_map_FK_res_id` (`res_id`),
  KEY `x_audit_map_FK_group_id` (`group_id`),
  KEY `x_audit_map_FK_user_id` (`user_id`),
  KEY `x_audit_map_cr_time` (`create_time`),
  KEY `x_audit_map_up_time` (`update_time`),
  CONSTRAINT `x_audit_map_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_audit_map_FK_group_id` FOREIGN KEY (`group_id`) REFERENCES `x_group` (`id`),
  CONSTRAINT `x_audit_map_FK_res_id` FOREIGN KEY (`res_id`) REFERENCES `x_resource` (`id`),
  CONSTRAINT `x_audit_map_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_audit_map_FK_user_id` FOREIGN KEY (`user_id`) REFERENCES `x_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `x_audit_map`
--

LOCK TABLES `x_audit_map` WRITE;
/*!40000 ALTER TABLE `x_audit_map` DISABLE KEYS */;
/*!40000 ALTER TABLE `x_audit_map` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `x_auth_sess`
--

DROP TABLE IF EXISTS `x_auth_sess`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `x_auth_sess` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `added_by_id` bigint(20) DEFAULT NULL,
  `upd_by_id` bigint(20) DEFAULT NULL,
  `login_id` varchar(767) NOT NULL,
  `user_id` bigint(20) DEFAULT NULL,
  `ext_sess_id` varchar(512) DEFAULT NULL,
  `auth_time` datetime NOT NULL,
  `auth_status` int(11) NOT NULL DEFAULT '0',
  `auth_type` int(11) NOT NULL DEFAULT '0',
  `auth_provider` int(11) NOT NULL DEFAULT '0',
  `device_type` int(11) NOT NULL DEFAULT '0',
  `req_ip` varchar(48) NOT NULL,
  `req_ua` varchar(1024) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `x_auth_sess_FK_added_by_id` (`added_by_id`),
  KEY `x_auth_sess_FK_upd_by_id` (`upd_by_id`),
  KEY `x_auth_sess_FK_user_id` (`user_id`),
  KEY `x_auth_sess_cr_time` (`create_time`),
  KEY `x_auth_sess_up_time` (`update_time`),
  CONSTRAINT `x_auth_sess_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_auth_sess_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_auth_sess_FK_user_id` FOREIGN KEY (`user_id`) REFERENCES `x_portal_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `x_auth_sess`
--

LOCK TABLES `x_auth_sess` WRITE;
/*!40000 ALTER TABLE `x_auth_sess` DISABLE KEYS */;
/*!40000 ALTER TABLE `x_auth_sess` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `x_cred_store`
--

DROP TABLE IF EXISTS `x_cred_store`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `x_cred_store` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `added_by_id` bigint(20) DEFAULT NULL,
  `upd_by_id` bigint(20) DEFAULT NULL,
  `store_name` varchar(1024) NOT NULL,
  `descr` varchar(4000) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `x_cred_store_FK_added_by_id` (`added_by_id`),
  KEY `x_cred_store_FK_upd_by_id` (`upd_by_id`),
  KEY `x_cred_store_cr_time` (`create_time`),
  KEY `x_cred_store_up_time` (`update_time`),
  CONSTRAINT `x_cred_store_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_cred_store_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `x_cred_store`
--

LOCK TABLES `x_cred_store` WRITE;
/*!40000 ALTER TABLE `x_cred_store` DISABLE KEYS */;
/*!40000 ALTER TABLE `x_cred_store` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `x_db_base`
--

DROP TABLE IF EXISTS `x_db_base`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `x_db_base` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `added_by_id` bigint(20) DEFAULT NULL,
  `upd_by_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `x_db_base_FK_added_by_id` (`added_by_id`),
  KEY `x_db_base_FK_upd_by_id` (`upd_by_id`),
  KEY `x_db_base_cr_time` (`create_time`),
  KEY `x_db_base_up_time` (`update_time`),
  CONSTRAINT `x_db_base_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_db_base_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `x_db_base`
--

LOCK TABLES `x_db_base` WRITE;
/*!40000 ALTER TABLE `x_db_base` DISABLE KEYS */;
/*!40000 ALTER TABLE `x_db_base` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `x_group`
--

DROP TABLE IF EXISTS `x_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `x_group` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `added_by_id` bigint(20) DEFAULT NULL,
  `upd_by_id` bigint(20) DEFAULT NULL,
  `group_name` varchar(1024) NOT NULL,
  `descr` varchar(4000) NOT NULL,
  `status` int(11) NOT NULL DEFAULT '0',
  `group_type` int(11) NOT NULL DEFAULT '0',
  `group_src` int(11) NOT NULL DEFAULT '0',
  `cred_store_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `x_group_FK_added_by_id` (`added_by_id`),
  KEY `x_group_FK_upd_by_id` (`upd_by_id`),
  KEY `x_group_FK_cred_store_id` (`cred_store_id`),
  KEY `x_group_cr_time` (`create_time`),
  KEY `x_group_up_time` (`update_time`),
  CONSTRAINT `x_group_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_group_FK_cred_store_id` FOREIGN KEY (`cred_store_id`) REFERENCES `x_cred_store` (`id`),
  CONSTRAINT `x_group_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `x_group`
--

LOCK TABLES `x_group` WRITE;
/*!40000 ALTER TABLE `x_group` DISABLE KEYS */;
/*!40000 ALTER TABLE `x_group` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `x_group_groups`
--

DROP TABLE IF EXISTS `x_group_groups`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `x_group_groups` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `added_by_id` bigint(20) DEFAULT NULL,
  `upd_by_id` bigint(20) DEFAULT NULL,
  `group_name` varchar(1024) NOT NULL,
  `p_group_id` bigint(20) DEFAULT NULL,
  `group_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `x_group_groups_FK_added_by_id` (`added_by_id`),
  KEY `x_group_groups_FK_upd_by_id` (`upd_by_id`),
  KEY `x_group_groups_FK_p_group_id` (`p_group_id`),
  KEY `x_group_groups_FK_group_id` (`group_id`),
  KEY `x_group_groups_cr_time` (`create_time`),
  KEY `x_group_groups_up_time` (`update_time`),
  CONSTRAINT `x_group_groups_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_group_groups_FK_group_id` FOREIGN KEY (`group_id`) REFERENCES `x_group` (`id`),
  CONSTRAINT `x_group_groups_FK_p_group_id` FOREIGN KEY (`p_group_id`) REFERENCES `x_group` (`id`),
  CONSTRAINT `x_group_groups_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `x_group_groups`
--

LOCK TABLES `x_group_groups` WRITE;
/*!40000 ALTER TABLE `x_group_groups` DISABLE KEYS */;
/*!40000 ALTER TABLE `x_group_groups` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `x_group_users`
--

DROP TABLE IF EXISTS `x_group_users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `x_group_users` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `added_by_id` bigint(20) DEFAULT NULL,
  `upd_by_id` bigint(20) DEFAULT NULL,
  `group_name` varchar(1024) NOT NULL,
  `p_group_id` bigint(20) DEFAULT NULL,
  `user_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `x_group_users_FK_added_by_id` (`added_by_id`),
  KEY `x_group_users_FK_upd_by_id` (`upd_by_id`),
  KEY `x_group_users_FK_p_group_id` (`p_group_id`),
  KEY `x_group_users_FK_user_id` (`user_id`),
  KEY `x_group_users_cr_time` (`create_time`),
  KEY `x_group_users_up_time` (`update_time`),
  CONSTRAINT `x_group_users_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_group_users_FK_p_group_id` FOREIGN KEY (`p_group_id`) REFERENCES `x_group` (`id`),
  CONSTRAINT `x_group_users_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_group_users_FK_user_id` FOREIGN KEY (`user_id`) REFERENCES `x_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `x_group_users`
--

LOCK TABLES `x_group_users` WRITE;
/*!40000 ALTER TABLE `x_group_users` DISABLE KEYS */;
/*!40000 ALTER TABLE `x_group_users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `x_perm_map`
--

DROP TABLE IF EXISTS `x_perm_map`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `x_perm_map` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `added_by_id` bigint(20) DEFAULT NULL,
  `upd_by_id` bigint(20) DEFAULT NULL,
  `perm_group` varchar(1024) DEFAULT NULL,
  `res_id` bigint(20) DEFAULT NULL,
  `group_id` bigint(20) DEFAULT NULL,
  `user_id` bigint(20) DEFAULT NULL,
  `perm_for` int(11) NOT NULL DEFAULT '0',
  `perm_type` int(11) NOT NULL DEFAULT '0',
  `is_recursive` int(11) NOT NULL DEFAULT '0',
  `is_wild_card` tinyint(1) NOT NULL DEFAULT '1',
  `grant_revoke` tinyint(1) NOT NULL DEFAULT '1',
  `ip_address` text,
  PRIMARY KEY (`id`),
  KEY `x_perm_map_FK_added_by_id` (`added_by_id`),
  KEY `x_perm_map_FK_upd_by_id` (`upd_by_id`),
  KEY `x_perm_map_FK_res_id` (`res_id`),
  KEY `x_perm_map_FK_group_id` (`group_id`),
  KEY `x_perm_map_FK_user_id` (`user_id`),
  KEY `x_perm_map_cr_time` (`create_time`),
  KEY `x_perm_map_up_time` (`update_time`),
  CONSTRAINT `x_perm_map_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_perm_map_FK_group_id` FOREIGN KEY (`group_id`) REFERENCES `x_group` (`id`),
  CONSTRAINT `x_perm_map_FK_res_id` FOREIGN KEY (`res_id`) REFERENCES `x_resource` (`id`),
  CONSTRAINT `x_perm_map_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_perm_map_FK_user_id` FOREIGN KEY (`user_id`) REFERENCES `x_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `x_perm_map`
--

LOCK TABLES `x_perm_map` WRITE;
/*!40000 ALTER TABLE `x_perm_map` DISABLE KEYS */;
/*!40000 ALTER TABLE `x_perm_map` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `x_policy_export_audit`
--

DROP TABLE IF EXISTS `x_policy_export_audit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `x_policy_export_audit` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `added_by_id` bigint(20) DEFAULT NULL,
  `upd_by_id` bigint(20) DEFAULT NULL,
  `client_ip` varchar(255) NOT NULL,
  `agent_id` varchar(255) DEFAULT NULL,
  `req_epoch` bigint(20) NOT NULL,
  `last_updated` datetime DEFAULT NULL,
  `repository_name` varchar(1024) DEFAULT NULL,
  `exported_json` text,
  `http_ret_code` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `x_policy_export_audit_FK_added_by_id` (`added_by_id`),
  KEY `x_policy_export_audit_FK_upd_by_id` (`upd_by_id`),
  KEY `x_policy_export_audit_cr_time` (`create_time`),
  KEY `x_policy_export_audit_up_time` (`update_time`),
  CONSTRAINT `x_policy_export_audit_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_policy_export_audit_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `x_policy_export_audit`
--

LOCK TABLES `x_policy_export_audit` WRITE;
/*!40000 ALTER TABLE `x_policy_export_audit` DISABLE KEYS */;
/*!40000 ALTER TABLE `x_policy_export_audit` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `x_portal_user`
--

DROP TABLE IF EXISTS `x_portal_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `x_portal_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `added_by_id` bigint(20) DEFAULT NULL,
  `upd_by_id` bigint(20) DEFAULT NULL,
  `first_name` varchar(1022) DEFAULT NULL,
  `last_name` varchar(1022) DEFAULT NULL,
  `pub_scr_name` varchar(2048) DEFAULT NULL,
  `login_id` varchar(767) DEFAULT NULL,
  `password` varchar(512) NOT NULL,
  `email` varchar(512) DEFAULT NULL,
  `status` int(11) NOT NULL DEFAULT '0',
  `user_src` int(11) NOT NULL DEFAULT '0',
  `notes` varchar(4000) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `x_portal_user_UK_login_id` (`login_id`),
  UNIQUE KEY `x_portal_user_UK_email` (`email`),
  KEY `x_portal_user_FK_added_by_id` (`added_by_id`),
  KEY `x_portal_user_FK_upd_by_id` (`upd_by_id`),
  KEY `x_portal_user_cr_time` (`create_time`),
  KEY `x_portal_user_up_time` (`update_time`),
  KEY `x_portal_user_name` (`first_name`(767)),
  KEY `x_portal_user_email` (`email`),
  CONSTRAINT `x_portal_user_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_portal_user_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `x_portal_user`
--

LOCK TABLES `x_portal_user` WRITE;
/*!40000 ALTER TABLE `x_portal_user` DISABLE KEYS */;
INSERT INTO `x_portal_user` VALUES (1,'2014-05-25 00:07:26','2014-05-25 00:07:26',NULL,NULL,'Admin','','Admin','admin','ceb4f32325eda6142bd65215f4c0f371','',1,0,NULL);
/*!40000 ALTER TABLE `x_portal_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `x_portal_user_role`
--

DROP TABLE IF EXISTS `x_portal_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `x_portal_user_role` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `added_by_id` bigint(20) DEFAULT NULL,
  `upd_by_id` bigint(20) DEFAULT NULL,
  `user_id` bigint(20) NOT NULL,
  `user_role` varchar(128) DEFAULT NULL,
  `status` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `x_portal_user_role_FK_added_by_id` (`added_by_id`),
  KEY `x_portal_user_role_FK_upd_by_id` (`upd_by_id`),
  KEY `x_portal_user_role_FK_user_id` (`user_id`),
  KEY `x_portal_user_role_cr_time` (`create_time`),
  KEY `x_portal_user_role_up_time` (`update_time`),
  CONSTRAINT `x_portal_user_role_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_portal_user_role_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_portal_user_role_FK_user_id` FOREIGN KEY (`user_id`) REFERENCES `x_portal_user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `x_portal_user_role`
--

LOCK TABLES `x_portal_user_role` WRITE;
/*!40000 ALTER TABLE `x_portal_user_role` DISABLE KEYS */;
INSERT INTO `x_portal_user_role` VALUES (1,'2014-05-25 00:07:26','2014-05-25 00:07:26',NULL,NULL,1,'ROLE_SYS_ADMIN',1);
/*!40000 ALTER TABLE `x_portal_user_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `x_resource`
--

DROP TABLE IF EXISTS `x_resource`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `x_resource` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `added_by_id` bigint(20) DEFAULT NULL,
  `upd_by_id` bigint(20) DEFAULT NULL,
  `res_name` varchar(4000) DEFAULT NULL,
  `policy_name` varchar(500)  DEFAULT NULL,
  `descr` varchar(4000) DEFAULT NULL,
  `res_type` int(11) NOT NULL DEFAULT '0',
  `asset_id` bigint(20) NOT NULL,
  `parent_id` bigint(20) DEFAULT NULL,
  `parent_path` varchar(4000) DEFAULT NULL,
  `is_encrypt` int(11) NOT NULL DEFAULT '0',
  `is_recursive` int(11) NOT NULL DEFAULT '0',
  `res_group` varchar(1024) DEFAULT NULL,
  `res_dbs` text,
  `res_tables` text,
  `res_col_fams` text,
  `res_cols` text,
  `res_udfs` text,
  `res_status` int(11) NOT NULL DEFAULT '1',
  `table_type` int(11) NOT NULL DEFAULT '0',
  `col_type` int(11) NOT NULL DEFAULT '0',
  `res_topologies` text,
  `res_services` text,
  PRIMARY KEY (`id`),
  UNIQUE KEY `policy_name` (`policy_name`),
  KEY `x_resource_FK_added_by_id` (`added_by_id`),
  KEY `x_resource_FK_upd_by_id` (`upd_by_id`),
  KEY `x_resource_FK_asset_id` (`asset_id`),
  KEY `x_resource_FK_parent_id` (`parent_id`),
  KEY `x_resource_cr_time` (`create_time`),
  KEY `x_resource_up_time` (`update_time`),
  CONSTRAINT `x_resource_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_resource_FK_asset_id` FOREIGN KEY (`asset_id`) REFERENCES `x_asset` (`id`),
  CONSTRAINT `x_resource_FK_parent_id` FOREIGN KEY (`parent_id`) REFERENCES `x_resource` (`id`),
  CONSTRAINT `x_resource_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `x_resource`
--

LOCK TABLES `x_resource` WRITE;
/*!40000 ALTER TABLE `x_resource` DISABLE KEYS */;
/*!40000 ALTER TABLE `x_resource` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `x_trx_log`
--

DROP TABLE IF EXISTS `x_trx_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `x_trx_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `added_by_id` bigint(20) DEFAULT NULL,
  `upd_by_id` bigint(20) DEFAULT NULL,
  `class_type` int(11) NOT NULL DEFAULT '0',
  `object_id` bigint(20) DEFAULT NULL,
  `parent_object_id` bigint(20) DEFAULT NULL,
  `parent_object_class_type` int(11) NOT NULL DEFAULT '0',
  `parent_object_name` varchar(1024) DEFAULT NULL,
  `object_name` varchar(1024) DEFAULT NULL,
  `attr_name` varchar(255) DEFAULT NULL,
  `prev_val` varchar(1024) DEFAULT NULL,
  `new_val` varchar(1024) DEFAULT NULL,
  `trx_id` varchar(1024) DEFAULT NULL,
  `action` varchar(255) DEFAULT NULL,
  `sess_id` varchar(512) DEFAULT NULL,
  `req_id` varchar(30) DEFAULT NULL,
  `sess_type` varchar(30) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `x_trx_log_FK_added_by_id` (`added_by_id`),
  KEY `x_trx_log_FK_upd_by_id` (`upd_by_id`),
  KEY `x_trx_log_cr_time` (`create_time`),
  KEY `x_trx_log_up_time` (`update_time`),
  CONSTRAINT `x_trx_log_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_trx_log_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `x_trx_log`
--

LOCK TABLES `x_trx_log` WRITE;
/*!40000 ALTER TABLE `x_trx_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `x_trx_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `x_user`
--

DROP TABLE IF EXISTS `x_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `x_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `added_by_id` bigint(20) DEFAULT NULL,
  `upd_by_id` bigint(20) DEFAULT NULL,
  `user_name` varchar(1024) NOT NULL,
  `descr` varchar(4000) NOT NULL,
  `status` int(11) NOT NULL DEFAULT '0',
  `cred_store_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `x_user_FK_added_by_id` (`added_by_id`),
  KEY `x_user_FK_upd_by_id` (`upd_by_id`),
  KEY `x_user_FK_cred_store_id` (`cred_store_id`),
  KEY `x_user_cr_time` (`create_time`),
  KEY `x_user_up_time` (`update_time`),
  CONSTRAINT `x_user_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_user_FK_cred_store_id` FOREIGN KEY (`cred_store_id`) REFERENCES `x_cred_store` (`id`),
  CONSTRAINT `x_user_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `x_user`
--

LOCK TABLES `x_user` WRITE;
/*!40000 ALTER TABLE `x_user` DISABLE KEYS */;
/*!40000 ALTER TABLE `x_user` ENABLE KEYS */;
UNLOCK TABLES;

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
  `request_data` varchar(2000) DEFAULT NULL,
  `resource_path` varchar(2000) DEFAULT NULL,
  `resource_type` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `xa_access_audit_FK_added_by_id` (`added_by_id`),
  KEY `xa_access_audit_FK_upd_by_id` (`upd_by_id`),
  KEY `xa_access_audit_cr_time` (`create_time`),
  KEY `xa_access_audit_up_time` (`update_time`),
  CONSTRAINT `xa_access_audit_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `xa_access_audit_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `xa_access_audit`
--

LOCK TABLES `xa_access_audit` WRITE;
/*!40000 ALTER TABLE `xa_access_audit` DISABLE KEYS */;
/*!40000 ALTER TABLE `xa_access_audit` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Current Database: `xa_db`
--

USE `xa_db`;

--
-- Final view structure for view `vx_trx_log`
--

/*!50001 DROP TABLE IF EXISTS `vx_trx_log`*/;
/*!50001 DROP VIEW IF EXISTS `vx_trx_log`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = latin1 */;
/*!50001 SET character_set_results     = latin1 */;
/*!50001 SET collation_connection      = latin1_swedish_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`xaadmin`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `vx_trx_log` AS select `x_trx_log`.`id` AS `id`,`x_trx_log`.`create_time` AS `create_time`,`x_trx_log`.`update_time` AS `update_time`,`x_trx_log`.`added_by_id` AS `added_by_id`,`x_trx_log`.`upd_by_id` AS `upd_by_id`,`x_trx_log`.`class_type` AS `class_type`,`x_trx_log`.`object_id` AS `object_id`,`x_trx_log`.`parent_object_id` AS `parent_object_id`,`x_trx_log`.`parent_object_class_type` AS `parent_object_class_type`,`x_trx_log`.`attr_name` AS `attr_name`,`x_trx_log`.`parent_object_name` AS `parent_object_name`,`x_trx_log`.`object_name` AS `object_name`,`x_trx_log`.`prev_val` AS `prev_val`,`x_trx_log`.`new_val` AS `new_val`,`x_trx_log`.`trx_id` AS `trx_id`,`x_trx_log`.`action` AS `action`,`x_trx_log`.`sess_id` AS `sess_id`,`x_trx_log`.`req_id` AS `req_id`,`x_trx_log`.`sess_type` AS `sess_type` from `x_trx_log` group by `x_trx_log`.`trx_id` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2014-05-25  0:07:27
