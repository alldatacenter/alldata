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
SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";

CREATE DATABASE IF NOT EXISTS apache_inlong_audit;
USE apache_inlong_audit;

CREATE TABLE `audit_data`
(
    `ip`               String COMMENT 'client ip',
    `docker_id`        String COMMENT 'client docker id',
    `thread_id`        String COMMENT 'client thread id',
    `sdk_ts`           DateTime COMMENT 'sdk timestamp',
    `packet_id`        Int64 COMMENT 'packet id',
    `log_ts`           DateTime COMMENT 'log timestamp',
    `inlong_group_id`  String COMMENT 'inlong group id',
    `inlong_stream_id` String COMMENT 'inlong stream id',
    `audit_id`         String COMMENT 'audit id',
    `count`            Int64 COMMENT 'msg count',
    `size`             Int64 COMMENT 'msg size',
    `delay`            Int64 COMMENT 'msg delay',
    `update_time`       DateTime COMMENT 'update time'
)
ENGINE = MergeTree
ORDER BY inlong_group_id
SETTINGS index_granularity = 8192;
