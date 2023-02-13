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

SET NAMES utf8;
SET FOREIGN_KEY_CHECKS = 0;
SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";

-- ----------------------------
-- Database for InLong Audit
-- ----------------------------
CREATE DATABASE IF NOT EXISTS apache_inlong_audit;

USE apache_inlong_audit;

-- ----------------------------
-- Table structure for audit_data
-- ----------------------------
CREATE TABLE IF NOT EXISTS `audit_data`
(
    `id`               int(32)      NOT NULL PRIMARY KEY AUTO_INCREMENT COMMENT 'Incremental primary key',
    `ip`               varchar(32)  NOT NULL DEFAULT '' COMMENT 'Client IP',
    `docker_id`        varchar(100) NOT NULL DEFAULT '' COMMENT 'Client docker id',
    `thread_id`        varchar(50)  NOT NULL DEFAULT '' COMMENT 'Client thread id',
    `sdk_ts`           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'SDK timestamp',
    `packet_id`        BIGINT       NOT NULL DEFAULT '0' COMMENT 'Packet id',
    `log_ts`           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Log timestamp',
    `inlong_group_id`  varchar(100) NOT NULL DEFAULT '' COMMENT 'The target inlong group id',
    `inlong_stream_id` varchar(100) NOT NULL DEFAULT '' COMMENT 'The target inlong stream id',
    `audit_id`         varchar(100) NOT NULL DEFAULT '' COMMENT 'Audit id',
    `count`            BIGINT       NOT NULL DEFAULT '0' COMMENT 'Message count',
    `size`             BIGINT       NOT NULL DEFAULT '0' COMMENT 'Message size',
    `delay`            BIGINT       NOT NULL DEFAULT '0' COMMENT 'Message delay count',
    `update_time`      timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    INDEX group_stream_audit_id (`inlong_group_id`, `inlong_stream_id`, `audit_id`, `log_ts`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8 COMMENT ='Inlong audit data table';
