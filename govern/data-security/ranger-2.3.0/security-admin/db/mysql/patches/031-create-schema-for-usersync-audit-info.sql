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

DROP TABLE IF EXISTS `x_ugsync_audit_info`;


CREATE TABLE IF NOT EXISTS `x_ugsync_audit_info`(
`id` bigint(20) NOT NULL AUTO_INCREMENT,
`create_time` datetime NULL DEFAULT NULL,
`update_time` datetime NULL DEFAULT NULL,
`added_by_id` bigint(20) NULL DEFAULT NULL,
`upd_by_id` bigint(20) NULL DEFAULT NULL,
`event_time` datetime NULL DEFAULT NULL,
`user_name` varchar(255) NOT  NULL,
`sync_source` varchar(128) NOT NULL,
`no_of_new_users` bigint(20) NOT NULL,
`no_of_new_groups` bigint(20) NOT NULL,
`no_of_modified_users` bigint(20) NOT NULL,
`no_of_modified_groups` bigint(20) NOT NULL,
`sync_source_info` varchar(4000) NOT NULL,
`session_id` varchar(255) DEFAULT NULL,
 PRIMARY KEY (`id`),
 KEY `x_ugsync_audit_info_etime`(`event_time`),
 KEY `x_ugsync_audit_info_sync_src`(`sync_source`),
 KEY `x_ugsync_audit_info_uname`(`user_name`)
)DEFAULT CHARSET=latin1;

