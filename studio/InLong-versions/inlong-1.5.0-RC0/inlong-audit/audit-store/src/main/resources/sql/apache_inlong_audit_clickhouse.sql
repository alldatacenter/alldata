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
    `ip`               String COMMENT 'Client IP',
    `docker_id`        String COMMENT 'Client docker id',
    `thread_id`        String COMMENT 'Client thread id',
    `sdk_ts`           DateTime COMMENT 'SDK timestamp',
    `packet_id`        Int64 COMMENT 'Packet id',
    `log_ts`           DateTime COMMENT 'Log timestamp',
    `inlong_group_id`  String COMMENT 'The target inlong group id',
    `inlong_stream_id` String COMMENT 'The target inlong stream id',
    `audit_id`         String COMMENT 'Audit id',
    `count`            Int64 COMMENT 'Message count',
    `size`             Int64 COMMENT 'Message size',
    `delay`            Int64 COMMENT 'Message delay',
    `update_time`      DateTime COMMENT 'Update time'
) ENGINE = MergeTree
      ORDER BY inlong_group_id
      SETTINGS index_granularity = 8192;
