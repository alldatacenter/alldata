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

drop procedure if exists add_column_zone_in_x_policy_export_audit;

delimiter ;;
create procedure add_column_zone_in_x_policy_export_audit() begin

if not exists (select * from information_schema.columns where table_schema=database() and table_name = 'x_policy_export_audit' and column_name='zone_name') then
        ALTER TABLE x_policy_export_audit ADD zone_name varchar(255) NULL DEFAULT NULL;
end if;
end;;

delimiter ;
call add_column_zone_in_x_policy_export_audit();

drop procedure if exists add_column_zone_in_x_policy_export_audit;

drop procedure if exists remove_x_policy_zone_id;
delimiter ;;
create procedure remove_x_policy_zone_id() begin
if exists (select * from information_schema.columns where table_schema=database() and table_name = 'x_policy') then
  if exists (select * from information_schema.columns where table_schema=database() and table_name = 'x_policy' and column_name = 'zone_id') then
        if exists(select * FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE WHERE REFERENCED_TABLE_NAME = 'x_security_zone' AND TABLE_SCHEMA  = database() AND TABLE_NAME = 'x_policy' AND CONSTRAINT_NAME = 'x_policy_FK_zone_id') then
            ALTER TABLE `x_policy` DROP FOREIGN KEY `x_policy_FK_zone_id`;
        end if;
    ALTER TABLE `x_policy` DROP COLUMN `zone_id`;
  end if;
 end if;
end;;

delimiter ;
call remove_x_policy_zone_id();
drop procedure if exists remove_x_policy_zone_id;

drop procedure if exists createIndex;
delimiter ;;
create procedure createIndex()
begin
if not exists (select * from information_schema.columns where table_schema=database() and table_name = 'x_group') then
        ALTER TABLE `x_group` ADD INDEX `grp_name`(`group_name`);
end if;
end;;
delimiter ;
call createIndex();
drop procedure if exists createIndex;

DROP TABLE IF EXISTS `x_security_zone_ref_group`;
DROP TABLE IF EXISTS `x_security_zone_ref_user`;
DROP TABLE IF EXISTS `x_security_zone_ref_resource`;
DROP TABLE IF EXISTS `x_security_zone_ref_service`;
DROP TABLE IF EXISTS `x_security_zone_ref_tag_srvc`;
DROP TABLE IF EXISTS `x_ranger_global_state`;
DROP TABLE IF EXISTS `x_security_zone`;

CREATE TABLE IF NOT EXISTS `x_security_zone`(
`id` bigint(20) NOT NULL AUTO_INCREMENT,
`create_time` datetime NULL DEFAULT NULL,
`update_time` datetime NULL DEFAULT NULL,
`added_by_id` bigint(20) NULL DEFAULT NULL,
`upd_by_id` bigint(20) NULL DEFAULT NULL,
`version` bigint(20) NULL DEFAULT NULL,
`name` varchar(255) NOT NULL,
`jsonData` MEDIUMTEXT NULL DEFAULT NULL,
`description` varchar(1024) DEFAULT NULL,
 PRIMARY KEY (`id`),
 UNIQUE KEY `x_security_zone_UK_name`(`name`(190)),
 CONSTRAINT `x_security_zone_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
 CONSTRAINT `x_security_zone_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE IF NOT EXISTS `x_ranger_global_state`(
`id` bigint(20) NOT NULL AUTO_INCREMENT,
`create_time` datetime NULL DEFAULT NULL,
`update_time` datetime NULL DEFAULT NULL,
`added_by_id` bigint(20) NULL  DEFAULT NULL,
`upd_by_id` bigint(20) NULL DEFAULT NULL,
`version` bigint(20) NULL DEFAULT NULL,
`state_name` varchar(255) NOT  NULL,
`app_data` varchar(255) NULL DEFAULT NULL,
PRIMARY KEY (`id`),
UNIQUE  KEY `x_ranger_global_state_UK_state_name`(`state_name`(190)),
CONSTRAINT `x_ranger_global_state_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_ranger_global_state_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE IF NOT EXISTS `x_security_zone_ref_service`(
`id` bigint(20) NOT NULL AUTO_INCREMENT,
`create_time` datetime NULL DEFAULT NULL,
`update_time` datetime NULL DEFAULT NULL,
`added_by_id` bigint(20) NULL DEFAULT NULL,
`upd_by_id` bigint(20) NULL DEFAULT NULL,
`zone_id` bigint(20) NULL DEFAULT NULL,
`service_id` bigint(20) NULL DEFAULT NULL,
`service_name` varchar(255) NULL DEFAULT NULL,
 PRIMARY KEY (`id`),
 CONSTRAINT `x_sz_ref_service_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
 CONSTRAINT `x_sz_ref_service_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`),
 CONSTRAINT `x_sz_ref_service_FK_zone_id` FOREIGN KEY (`zone_id`) REFERENCES `x_security_zone` (`id`),
 CONSTRAINT `x_sz_ref_service_FK_service_id` FOREIGN KEY (`service_id`) REFERENCES `x_service` (`id`),
 CONSTRAINT `x_sz_ref_service_FK_service_name` FOREIGN KEY (`service_name`) REFERENCES `x_service` (`name`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE IF NOT EXISTS `x_security_zone_ref_tag_srvc`(
`id` bigint(20) NOT NULL AUTO_INCREMENT,
`create_time` datetime NULL DEFAULT NULL,
`update_time` datetime NULL DEFAULT NULL,
`added_by_id` bigint(20) NULL DEFAULT NULL,
`upd_by_id` bigint(20) NULL DEFAULT NULL,
`zone_id` bigint(20) NULL DEFAULT NULL,
`tag_srvc_id` bigint(20) NULL DEFAULT NULL,
`tag_srvc_name` varchar(255) NULL DEFAULT NULL,
 PRIMARY KEY (`id`),
 CONSTRAINT `x_sz_ref_tag_service_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
 CONSTRAINT `x_sz_ref_tag_service_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`),
 CONSTRAINT `x_sz_ref_tag_service_FK_zone_id` FOREIGN KEY (`zone_id`) REFERENCES `x_security_zone` (`id`),
 CONSTRAINT `x_sz_ref_tag_service_FK_tag_service_id` FOREIGN KEY (`tag_srvc_id`) REFERENCES `x_service` (`id`),
 CONSTRAINT `x_sz_ref_tag_service_FK_tag_service_name` FOREIGN KEY (`tag_srvc_name`) REFERENCES `x_service` (`name`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE IF NOT EXISTS `x_security_zone_ref_resource`(
`id` bigint(20) NOT NULL AUTO_INCREMENT,
`create_time` datetime NULL DEFAULT NULL,
`update_time` datetime NULL DEFAULT NULL,
`added_by_id` bigint(20) NULL DEFAULT NULL,
`upd_by_id` bigint(20) NULL DEFAULT NULL,
`zone_id` bigint(20) NULL DEFAULT NULL,
`resource_def_id` bigint(20) NULL DEFAULT NULL,
`resource_name` varchar(255) NULL DEFAULT NULL,
 PRIMARY KEY (`id`),
 CONSTRAINT `x_sz_ref_resource_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
 CONSTRAINT `x_sz_ref_resource_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`),
 CONSTRAINT `x_sz_ref_resource_FK_zone_id` FOREIGN KEY (`zone_id`) REFERENCES `x_security_zone` (`id`),
 CONSTRAINT `x_sz_ref_resource_FK_resource_def_id` FOREIGN KEY (`resource_def_id`) REFERENCES `x_resource_def` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE IF NOT EXISTS `x_security_zone_ref_user`(
`id` bigint(20) NOT NULL AUTO_INCREMENT,
`create_time` datetime NULL DEFAULT NULL,
`update_time` datetime NULL DEFAULT NULL,
`added_by_id` bigint(20) NULL DEFAULT NULL,
`upd_by_id` bigint(20) NULL DEFAULT NULL,
`zone_id` bigint(20) NULL DEFAULT NULL,
`user_id` bigint(20) NULL DEFAULT NULL,
`user_name` varchar(255) NULL DEFAULT NULL,
`user_type` tinyint(3) NULL DEFAULT NULL,
 PRIMARY KEY (`id`),
 CONSTRAINT `x_sz_ref_user_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
 CONSTRAINT `x_sz_ref_user_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`),
 CONSTRAINT `x_sz_ref_user_FK_zone_id` FOREIGN KEY (`zone_id`) REFERENCES `x_security_zone` (`id`),
 CONSTRAINT `x_sz_ref_user_FK_user_id` FOREIGN KEY (`user_id`) REFERENCES `x_user` (`id`),
 CONSTRAINT `x_sz_ref_user_FK_user_name` FOREIGN KEY (`user_name`) REFERENCES `x_user` (`user_name`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE IF NOT EXISTS `x_security_zone_ref_group`(
`id` bigint(20) NOT NULL AUTO_INCREMENT,
`create_time` datetime NULL DEFAULT NULL,
`update_time` datetime NULL DEFAULT NULL,
`added_by_id` bigint(20) NULL DEFAULT NULL,
`upd_by_id` bigint(20) NULL DEFAULT NULL,
`zone_id` bigint(20) NULL DEFAULT NULL,
`group_id` bigint(20) NULL DEFAULT NULL,
`group_name` varchar(255) NULL DEFAULT NULL,
`group_type` tinyint(3) NULL DEFAULT NULL,
 PRIMARY KEY (`id`),
 CONSTRAINT `x_sz_ref_group_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
 CONSTRAINT `x_sz_ref_group_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`),
 CONSTRAINT `x_sz_ref_group_FK_zone_id` FOREIGN KEY (`zone_id`) REFERENCES `x_security_zone` (`id`),
 CONSTRAINT `x_sz_ref_group_FK_group_id` FOREIGN KEY (`group_id`) REFERENCES `x_group` (`id`)
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
DROP PROCEDURE if exists getModulesIdByName$$
CREATE PROCEDURE `getModulesIdByName`(IN input_val VARCHAR(100), OUT myid BIGINT)
BEGIN
SET myid = 0;
SELECT x_modules_master.id into myid FROM x_modules_master WHERE x_modules_master.module = input_val;
END $$

DELIMITER ;

drop procedure if exists add_unzone_entry;
delimiter ;;
create procedure add_unzone_entry() begin
DECLARE adminID bigint;
if not exists (select * from x_security_zone where id=1 and name=' ') then
	call getXportalUIdByLoginId('admin', adminID);
	INSERT INTO x_security_zone(id, create_time, update_time, added_by_id, upd_by_id, version, name, jsonData, description) VALUES (1, UTC_TIMESTAMP(),UTC_TIMESTAMP(), adminID, adminID, 1, ' ', '', 'Unzoned zone');
end if;
end;;
delimiter ;
call add_unzone_entry();
drop procedure if exists add_unzone_entry;

drop procedure if exists add_x_policy_zone_id;
delimiter ;;
create procedure add_x_policy_zone_id() begin
if exists (select * from information_schema.columns where table_schema=database() and table_name = 'x_policy') then
  if not exists (select * from information_schema.columns where table_schema=database() and table_name = 'x_policy' and column_name = 'zone_id') then
    ALTER TABLE `x_policy` ADD COLUMN `zone_id` bigint(20) DEFAULT 1 NOT NULL,ADD CONSTRAINT `x_policy_FK_zone_id` FOREIGN KEY(`zone_id`) REFERENCES `x_security_zone`(`id`);
  end if;
 end if;
end;;
delimiter ;
call add_x_policy_zone_id();
drop procedure if exists add_x_policy_zone_id;


drop procedure if exists add_security_zone_permissions;
delimiter ;;
create procedure add_security_zone_permissions() begin

	DECLARE adminID bigint;
	DECLARE rangerusersyncID bigint;
	DECLARE rangertagsyncID bigint;
	DECLARE moduleIdSecurityZone bigint;

	call getXportalUIdByLoginId('admin', adminID);
	call getXportalUIdByLoginId('rangerusersync', rangerusersyncID);
	call getXportalUIdByLoginId('rangertagsync', rangertagsyncID);

	if not exists (select * from x_modules_master where module='Security Zone') then
		INSERT INTO `x_modules_master` (`create_time`,`update_time`,`added_by_id`,`upd_by_id`,`module`,`url`) VALUES (UTC_TIMESTAMP(),UTC_TIMESTAMP(),adminID,adminID,'Security Zone','');
	end if;
	call getModulesIdByName('Security Zone', moduleIdSecurityZone);
	if not exists (select * from x_user_module_perm where user_id=adminID and module_id=moduleIdSecurityZone) then
		INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (adminID,moduleIdSecurityZone,UTC_TIMESTAMP(),UTC_TIMESTAMP(),adminID,adminID,1);
	end if;
	if not exists (select * from x_user_module_perm where user_id=rangerusersyncID and module_id=moduleIdSecurityZone) then
		INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (rangerusersyncID,moduleIdSecurityZone,UTC_TIMESTAMP(),UTC_TIMESTAMP(),adminID,adminID,1);
	end if;
	if not exists (select * from x_user_module_perm where user_id=rangertagsyncID and module_id=moduleIdSecurityZone) then
		INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (rangertagsyncID,moduleIdSecurityZone,UTC_TIMESTAMP(),UTC_TIMESTAMP(),adminID,adminID,1);
	end if;
end;;
delimiter ;
call add_security_zone_permissions();
drop procedure if exists add_security_zone_permissions;

