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

-- This is the SQL change file from version 1.6.0 to the current version 1.7.0.
-- When upgrading to version 1.7.0, please execute those SQLs in the DB (such as MySQL) used by the Manager module.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

USE `apache_inlong_manager`;

DROP INDEX `group_ext_group_index` ON `inlong_group_ext`;
DROP INDEX `stream_id_index` ON `inlong_stream_ext`;
DROP INDEX `stream_group_id_index` ON `inlong_stream`;
DROP INDEX `unique_inlong_stream` ON `inlong_stream`;
ALTER TABLE `inlong_stream` ADD UNIQUE KEY `unique_inlong_stream` (`inlong_group_id`, `inlong_stream_id`, `is_deleted`);

ALTER TABLE `data_node`
    ADD COLUMN `display_name`  varchar(128) DEFAULT NULL COMMENT 'Data node display name';

ALTER TABLE `inlong_cluster`
    ADD COLUMN `display_name`  varchar(128) DEFAULT NULL COMMENT 'Cluster display name';

INSERT INTO `audit_base`(`name`, `type`, `is_sent`, `audit_id`)
VALUES ('audit_sort_postgres_input', 'POSTGRESQL', 0, '27'),
       ('audit_sort_postgres_output', 'POSTGRESQL', 1, '28');