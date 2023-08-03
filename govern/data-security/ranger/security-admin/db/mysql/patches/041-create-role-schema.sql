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


DROP TABLE IF EXISTS `x_role_ref_role`;
DROP TABLE IF EXISTS `x_policy_ref_role`;
DROP TABLE IF EXISTS `x_role_ref_group`;
DROP TABLE IF EXISTS `x_role_ref_user`;
DROP TABLE IF EXISTS `x_role`;

CREATE TABLE IF NOT EXISTS `x_role`(
`id` bigint(20) NOT NULL AUTO_INCREMENT,
`create_time` datetime NULL DEFAULT NULL,
`update_time` datetime NULL DEFAULT NULL,
`added_by_id` bigint(20) NULL DEFAULT NULL,
`upd_by_id` bigint(20) NULL DEFAULT NULL,
`version` bigint(20) NULL DEFAULT NULL,
`name` varchar(255) NOT NULL,
`description` varchar(1024) NULL DEFAULT NULL,
`role_options` varchar(4000) NULL DEFAULT NULL,
`role_text` MEDIUMTEXT NULL DEFAULT NULL,
 PRIMARY KEY (`id`),
 UNIQUE KEY `x_role_UK_name`(`name`(190)),
 CONSTRAINT `x_role_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
 CONSTRAINT `x_role_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE IF NOT EXISTS `x_role_ref_user`(
`id` bigint(20) NOT NULL AUTO_INCREMENT,
`create_time` datetime NULL DEFAULT NULL,
`update_time` datetime NULL DEFAULT NULL,
`added_by_id` bigint(20) NULL DEFAULT NULL,
`upd_by_id` bigint(20) NULL DEFAULT NULL,
`role_id` bigint(20) NOT NULL,
`user_id` bigint(20) NULL DEFAULT NULL,
`user_name` varchar(767) NULL DEFAULT NULL,
`priv_type` int(10) NULL DEFAULT NULL,
 PRIMARY KEY (`id`),
 CONSTRAINT `x_role_ref_user_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
 CONSTRAINT `x_role_ref_user_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`),
 CONSTRAINT `x_role_ref_user_FK_role_id` FOREIGN KEY (`role_id`) REFERENCES `x_role` (`id`),
 CONSTRAINT `x_role_ref_user_FK_user_id` FOREIGN KEY (`user_id`) REFERENCES `x_user` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE IF NOT EXISTS `x_role_ref_group`(
`id` bigint(20) NOT NULL AUTO_INCREMENT,
`create_time` datetime NULL DEFAULT NULL,
`update_time` datetime NULL DEFAULT NULL,
`added_by_id` bigint(20) NULL DEFAULT NULL,
`upd_by_id` bigint(20) NULL DEFAULT NULL,
`role_id` bigint(20) NOT NULL,
`group_id` bigint(20) NULL DEFAULT NULL,
`group_name` varchar(767) NULL DEFAULT NULL,
`priv_type` int(10) NULL DEFAULT NULL,
 PRIMARY KEY (`id`),
 CONSTRAINT `x_role_ref_group_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
 CONSTRAINT `x_role_ref_group_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`),
 CONSTRAINT `x_role_ref_group_FK_role_id` FOREIGN KEY (`role_id`) REFERENCES `x_role` (`id`),
 CONSTRAINT `x_role_ref_group_FK_group_id` FOREIGN KEY (`group_id`) REFERENCES `x_group` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE IF NOT EXISTS `x_policy_ref_role`(
`id` bigint(20) NOT NULL AUTO_INCREMENT,
`create_time` datetime NULL DEFAULT NULL,
`update_time` datetime NULL DEFAULT NULL,
`added_by_id` bigint(20) NULL DEFAULT NULL,
`upd_by_id` bigint(20) NULL DEFAULT NULL,
`policy_id` bigint(20) NOT NULL,
`role_id` bigint(20) NOT NULL,
`role_name` varchar(255) NULL DEFAULT NULL,
 PRIMARY KEY (`id`),
 UNIQUE KEY `x_policy_ref_role_UK_polId_roleId`(`policy_id`, `role_id`),
 CONSTRAINT `x_policy_ref_role_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
 CONSTRAINT `x_policy_ref_role_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`),
 CONSTRAINT `x_policy_ref_role_FK_policy_id` FOREIGN KEY (`policy_id`) REFERENCES `x_policy` (`id`),
 CONSTRAINT `x_policy_ref_role_FK_role_id` FOREIGN KEY (`role_id`) REFERENCES `x_role` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE IF NOT EXISTS `x_role_ref_role`(
`id` bigint(20) NOT NULL AUTO_INCREMENT,
`create_time` datetime NULL DEFAULT NULL,
`update_time` datetime NULL DEFAULT NULL,
`added_by_id` bigint(20) NULL DEFAULT NULL,
`upd_by_id` bigint(20) NULL DEFAULT NULL,
`role_ref_id` bigint(20) NULL DEFAULT NULL,
`role_id` bigint(20) NOT NULL,
`role_name` varchar(255) NULL DEFAULT NULL,
`priv_type` int(10) NULL DEFAULT NULL,
 PRIMARY KEY (`id`),
 CONSTRAINT `x_role_ref_role_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
 CONSTRAINT `x_role_ref_role_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`),
 CONSTRAINT `x_role_ref_role_FK_role_ref_id` FOREIGN KEY (`role_ref_id`) REFERENCES `x_role` (`id`)
)ROW_FORMAT=DYNAMIC;
