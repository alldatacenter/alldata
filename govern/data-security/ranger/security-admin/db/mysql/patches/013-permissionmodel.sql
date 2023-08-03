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

DROP TABLE IF EXISTS `x_group_module_perm`;
DROP TABLE IF EXISTS `x_user_module_perm`;
DROP TABLE IF EXISTS `x_modules_master`;
CREATE TABLE `x_modules_master` (
`id` bigint(20) NOT NULL AUTO_INCREMENT,
`create_time` datetime NULL DEFAULT NULL,
`update_time` datetime NULL DEFAULT NULL,
`added_by_id` bigint(20) NULL DEFAULT NULL,
`upd_by_id` bigint(20) NULL DEFAULT NULL,
`module` varchar(1024) NOT NULL,
`url` varchar(1024) NULL DEFAULT NULL,
PRIMARY KEY (`id`)
)ROW_FORMAT=DYNAMIC;

DELIMITER $$
DROP PROCEDURE if exists getXportalUIdByLoginId$$
CREATE PROCEDURE `getXportalUIdByLoginId`(IN input_val VARCHAR(100), OUT myid BIGINT)
BEGIN
SET myid = 0;
SELECT x_portal_user.id into myid FROM x_portal_user WHERE x_portal_user.login_id = input_val;
END $$

DELIMITER ;

DELIMITER $$
DROP PROCEDURE if exists insertModuleMasterEntries $$
CREATE PROCEDURE `insertModuleMasterEntries`()
BEGIN
DECLARE adminID bigint;
call getXportalUIdByLoginId('admin', adminID);

INSERT INTO `x_modules_master` (`create_time`,`update_time`,`added_by_id`,`upd_by_id`,`module`,`url`) VALUES (now(),now(),adminID,adminID,'Resource Based Policies',''),(now(),now(),adminID,adminID,'Users/Groups',''),(now(),now(),adminID,adminID,'Reports',''),(now(),now(),adminID,adminID,'Audit',''),(now(),now(),adminID,adminID,'Key Manager','');

END $$
DELIMITER ;
call insertModuleMasterEntries();

CREATE TABLE `x_user_module_perm` (
`id` bigint(20) NOT NULL AUTO_INCREMENT,
`user_id` bigint(20) NULL DEFAULT NULL,
`module_id` bigint(20) NULL DEFAULT NULL,
`create_time` datetime NULL DEFAULT NULL,
`update_time` datetime NULL DEFAULT NULL,
`added_by_id` bigint(20) NULL DEFAULT NULL,
`upd_by_id` bigint(20) NULL DEFAULT NULL,
`is_allowed` int(11) NOT NULL DEFAULT '1',
PRIMARY KEY (`id`),
KEY `x_user_module_perm_idx_module_id` (`module_id`),
KEY `x_user_module_perm_idx_user_id` (`user_id`),
CONSTRAINT `x_user_module_perm_FK_module_id` FOREIGN KEY (`module_id`) REFERENCES `x_modules_master` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
CONSTRAINT `x_user_module_perm_FK_user_id` FOREIGN KEY (`user_id`) REFERENCES `x_portal_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
)ROW_FORMAT=DYNAMIC;

CREATE TABLE `x_group_module_perm` (
`id` bigint(20) NOT NULL AUTO_INCREMENT,
`group_id` bigint(20) NULL DEFAULT NULL,
`module_id` bigint(20) NULL DEFAULT NULL,
`create_time` datetime NULL DEFAULT NULL,
`update_time` datetime NULL DEFAULT NULL,
`added_by_id` bigint(20) NULL DEFAULT NULL,
`upd_by_id` bigint(20) NULL DEFAULT NULL,
`is_allowed` int(11) NOT NULL DEFAULT '1',
PRIMARY KEY (`id`),
KEY `x_group_module_perm_idx_group_id` (`group_id`),
KEY `x_group_module_perm_idx_module_id` (`module_id`),
CONSTRAINT `x_group_module_perm_FK_module_id` FOREIGN KEY (`module_id`) REFERENCES `x_modules_master` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
CONSTRAINT `x_group_module_perm_FK_user_id` FOREIGN KEY (`group_id`) REFERENCES `x_group` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
)ROW_FORMAT=DYNAMIC;
