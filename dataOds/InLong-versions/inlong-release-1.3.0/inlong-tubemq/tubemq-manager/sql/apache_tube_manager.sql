/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- database for TubeMQ Manager
-- ----------------------------
CREATE DATABASE IF NOT EXISTS apache_inlong_tubemq;

use apache_inlong_tubemq;
-- ----------------------------
-- Table structure for broker
-- ----------------------------
DROP TABLE IF EXISTS `broker`;
CREATE TABLE `broker` (
                          `id` bigint NOT NULL AUTO_INCREMENT,
                          `broker_id` bigint DEFAULT NULL,
                          `broker_ip` bigint NOT NULL,
                          `cluster_id` bigint DEFAULT NULL,
                          `create_time` datetime(6) DEFAULT NULL,
                          `create_user` varchar(255) DEFAULT NULL,
                          `modify_time` datetime(6) DEFAULT NULL,
                          `region_id` bigint DEFAULT NULL,
                          PRIMARY KEY (`id`),
                          UNIQUE KEY `UKciq4ve8cnogwy80elee1am518` (`broker_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for cluster
-- ----------------------------
DROP TABLE IF EXISTS `cluster`;
CREATE TABLE `cluster` (
                           `cluster_id` bigint NOT NULL,
                           `cluster_name` varchar(255) DEFAULT NULL,
                           `create_time` datetime(6) DEFAULT NULL,
                           `create_user` varchar(255) DEFAULT NULL,
                           `modify_time` datetime(6) DEFAULT NULL,
                           `reload_broker_size` int NOT NULL,
                           PRIMARY KEY (`cluster_id`),
                           UNIQUE KEY `UKjo595af4i3co2onpspedgxcrs` (`cluster_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for create_topic_task
-- ----------------------------
DROP TABLE IF EXISTS `create_topic_task`;
CREATE TABLE `create_topic_task` (
                                     `id` bigint NOT NULL AUTO_INCREMENT,
                                     `cluster_id` bigint DEFAULT NULL,
                                     `config_retry_times` int DEFAULT NULL,
                                     `create_date` datetime(6) DEFAULT NULL,
                                     `modify_date` datetime(6) DEFAULT NULL,
                                     `modify_user` varchar(255) DEFAULT NULL,
                                     `reload_retry_times` int DEFAULT NULL,
                                     `status` int DEFAULT NULL,
                                     `token` varchar(255) DEFAULT NULL,
                                     `topic_name` varchar(255) DEFAULT NULL,
                                     PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for master
-- ----------------------------
DROP TABLE IF EXISTS `master`;
CREATE TABLE `master` (
                          `id` bigint NOT NULL AUTO_INCREMENT,
                          `cluster_id` bigint NOT NULL,
                          `ip` varchar(255) DEFAULT NULL,
                          `port` int NOT NULL,
                          `standby` bit(1) NOT NULL,
                          `token` varchar(255) DEFAULT NULL,
                          `web_port` int NOT NULL,
                          PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for region
-- ----------------------------
DROP TABLE IF EXISTS `region`;
CREATE TABLE `region` (
                          `id` bigint NOT NULL AUTO_INCREMENT,
                          `cluster_id` bigint DEFAULT NULL,
                          `create_date` datetime(6) DEFAULT NULL,
                          `create_user` varchar(255) DEFAULT NULL,
                          `modify_date` datetime(6) DEFAULT NULL,
                          `modify_user` varchar(255) DEFAULT NULL,
                          `name` varchar(255) DEFAULT NULL,
                          `region_id` bigint DEFAULT NULL,
                          PRIMARY KEY (`id`),
                          UNIQUE KEY `UK6bb56v767cxs5iujj2k08f1ic` (`cluster_id`,`region_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for topic
-- ----------------------------
DROP TABLE IF EXISTS `topic`;
CREATE TABLE `topic` (
                         `business_id` bigint NOT NULL AUTO_INCREMENT,
                         `base_dir` varchar(255) DEFAULT NULL,
                         `bg` varchar(255) DEFAULT NULL,
                         `business_cn_name` varchar(256) DEFAULT NULL,
                         `business_name` varchar(30) NOT NULL,
                         `category` varchar(255) DEFAULT NULL,
                         `cluster_id` int NOT NULL,
                         `create_time` date DEFAULT NULL,
                         `description` varchar(256) DEFAULT NULL,
                         `encoding_type` varchar(64) NOT NULL,
                         `example_data` varchar(255) DEFAULT NULL,
                         `field_splitter` varchar(10) DEFAULT NULL,
                         `import_type` varchar(255) DEFAULT NULL,
                         `in_charge` varchar(255) DEFAULT NULL,
                         `is_hybrid_data_source` int NOT NULL,
                         `is_sub_sort` int NOT NULL,
                         `issue_method` varchar(32) DEFAULT NULL,
                         `message_type` varchar(64) DEFAULT NULL,
                         `net_target` varchar(255) DEFAULT NULL,
                         `passwd` varchar(64) NOT NULL,
                         `predefined_fields` varchar(256) DEFAULT NULL,
                         `schema_name` varchar(240) NOT NULL,
                         `sn` int DEFAULT NULL,
                         `source_server` varchar(255) DEFAULT NULL,
                         `status` int NOT NULL,
                         `target_server` varchar(255) DEFAULT NULL,
                         `target_server_port` varchar(255) DEFAULT NULL,
                         `topic` varchar(64) NOT NULL,
                         `topology_name` varchar(255) DEFAULT NULL,
                         `username` varchar(32) NOT NULL,
                         PRIMARY KEY (`business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
