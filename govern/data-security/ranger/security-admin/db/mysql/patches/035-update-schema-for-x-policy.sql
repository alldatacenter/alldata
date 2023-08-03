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

drop procedure if exists update_TagDefAccessTypes_for_atlas;

delimiter ;;
create procedure update_TagDefAccessTypes_for_atlas() begin
DECLARE new_atlas_def_name varchar(100);
if exists (select version from x_db_version_h where version = 'J10013') then
	if exists (select name from x_service_def where name like 'atlas.%') then
		set new_atlas_def_name=(select name from x_service_def where name like 'atlas.%');
		if exists(select * from x_access_type_def where def_id in(select id from x_service_def where name='tag') and name in('atlas:read','atlas:create','atlas:update','atlas:delete','atlas:all')) then
			update x_access_type_def set name=concat(new_atlas_def_name,':read') where def_id=100 and name='atlas:read';
			update x_access_type_def set name=concat(new_atlas_def_name,':create') where def_id=100 and name='atlas:create';
			update x_access_type_def set name=concat(new_atlas_def_name,':update') where def_id=100 and name='atlas:update';
			update x_access_type_def set name=concat(new_atlas_def_name,':delete') where def_id=100 and name='atlas:delete';
			update x_access_type_def set name=concat(new_atlas_def_name,':all') where def_id=100 and name='atlas:all';
		end if;
		if exists(select * from x_access_type_def_grants where atd_id in (select id from x_access_type_def where def_id in (select id from x_service_def where name='tag') and name like 'atlas%') and implied_grant in ('atlas:read','atlas:create','atlas:update','atlas:delete','atlas:all')) then
			update x_access_type_def_grants set implied_grant=concat(new_atlas_def_name,':read') where implied_grant='atlas:read';
			update x_access_type_def_grants set implied_grant=concat(new_atlas_def_name,':create') where implied_grant='atlas:create';
			update x_access_type_def_grants set implied_grant=concat(new_atlas_def_name,':update') where implied_grant='atlas:update';
			update x_access_type_def_grants set implied_grant=concat(new_atlas_def_name,':delete') where implied_grant='atlas:delete';
			update x_access_type_def_grants set implied_grant=concat(new_atlas_def_name,':all') where implied_grant='atlas:all';
		end if;
	end if;
end if;
end;;

delimiter ;
call update_TagDefAccessTypes_for_atlas();

drop procedure if exists update_TagDefAccessTypes_for_atlas;


drop procedure if exists alter_table_x_policy;

delimiter ;;
create procedure alter_table_x_policy() begin

if exists (select * from information_schema.columns where table_schema=database() and table_name = 'x_policy') then
  if not exists (select * from information_schema.columns where table_schema=database() and table_name = 'x_policy' and column_name = 'policy_text') then
    ALTER TABLE `x_policy` ADD `policy_text` MEDIUMTEXT DEFAULT NULL;
  end if;
 end if; 
end;;

delimiter ;
call alter_table_x_policy();

drop procedure if exists alter_table_x_policy;

DROP PROCEDURE IF EXISTS removeConstraints;
DELIMITER ;;
CREATE PROCEDURE removeConstraints(vTableName varchar(128))
BEGIN
  DECLARE done INT DEFAULT FALSE;
  DECLARE cName VARCHAR(64);
  DECLARE cur CURSOR FOR
          SELECT DISTINCT CONSTRAINT_NAME
          FROM INFORMATION_SCHEMA.Key_COLUMN_USAGE
          WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME = vTableName
          AND REFERENCED_TABLE_NAME IS NOT NULL;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

  SET AUTOCOMMIT=0;
  SET FOREIGN_KEY_CHECKS=0;

  OPEN cur;

  read_loop: LOOP
    FETCH cur INTO cName;
    IF done THEN
      LEAVE read_loop;
    END IF;
    SET @sql = CONCAT('ALTER TABLE ',vTableName,' DROP FOREIGN KEY ',cName,';');
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END LOOP;

  CLOSE cur;

  SET FOREIGN_KEY_CHECKS=1;
  COMMIT;
  SET AUTOCOMMIT=1;
END ;;
DELIMITER ;

call removeConstraints('x_policy_item');
call removeConstraints('x_policy_item_access');
call removeConstraints('x_policy_item_condition');
call removeConstraints('x_policy_item_datamask');
call removeConstraints('x_policy_item_group_perm');
call removeConstraints('x_policy_item_user_perm');
call removeConstraints('x_policy_item_rowfilter');
call removeConstraints('x_policy_resource');
call removeConstraints('x_policy_resource_map');

DROP PROCEDURE removeConstraints;
DROP TABLE IF EXISTS `x_policy_ref_group`;
DROP TABLE IF EXISTS `x_policy_ref_user`;
DROP TABLE IF EXISTS `x_policy_ref_datamask_type`;
DROP TABLE IF EXISTS `x_policy_ref_condition`;
DROP TABLE IF EXISTS `x_policy_ref_access_type`;
DROP TABLE IF EXISTS `x_policy_ref_resource`;
CREATE TABLE IF NOT EXISTS `x_policy_ref_resource` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `guid` varchar(1024) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `added_by_id` bigint(20) DEFAULT NULL,
  `upd_by_id` bigint(20) DEFAULT NULL,
  `policy_id` bigint(20) NOT NULL,
  `resource_def_id` bigint(20) NOT NULL,
  `resource_name` varchar(4000) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `x_policy_ref_res_UK_polId_resDefId`(`policy_id`, `resource_def_id`),
  CONSTRAINT `x_policy_ref_res_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_policy_ref_res_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_policy_ref_res_FK_policy_id` FOREIGN KEY (`policy_id`) REFERENCES `x_policy` (`id`),
  CONSTRAINT `x_policy_ref_res_FK_resource_def_id` FOREIGN KEY (`resource_def_id`) REFERENCES `x_resource_def` (`id`)
) ROW_FORMAT=DYNAMIC;

CREATE TABLE IF NOT EXISTS `x_policy_ref_access_type` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `guid` varchar(1024) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `added_by_id` bigint(20) DEFAULT NULL,
  `upd_by_id` bigint(20) DEFAULT NULL,
  `policy_id` bigint(20) NOT NULL,
  `access_def_id` bigint(20) NOT NULL,
  `access_type_name` varchar(4000) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `x_policy_ref_access_UK_polId_accessDefId`(`policy_id`, `access_def_id`),
  CONSTRAINT `x_policy_ref_access_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_policy_ref_access_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_policy_ref_access_FK_policy_id` FOREIGN KEY (`policy_id`) REFERENCES `x_policy` (`id`),
  CONSTRAINT `x_policy_ref_access_FK_access_def_id` FOREIGN KEY (`access_def_id`) REFERENCES `x_access_type_def` (`id`)
) ROW_FORMAT=DYNAMIC;

CREATE TABLE IF NOT EXISTS `x_policy_ref_condition` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `guid` varchar(1024) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `added_by_id` bigint(20) DEFAULT NULL,
  `upd_by_id` bigint(20) DEFAULT NULL,
  `policy_id` bigint(20) NOT NULL,
  `condition_def_id` bigint(20) NOT NULL,
  `condition_name` varchar(4000) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `x_policy_ref_condition_UK_polId_condDefId`(`policy_id`, `condition_def_id`),
  CONSTRAINT `x_policy_ref_condition_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_policy_ref_condition_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_policy_ref_condition_FK_policy_id` FOREIGN KEY (`policy_id`) REFERENCES `x_policy` (`id`),
  CONSTRAINT `x_policy_ref_condition_FK_condition_def_id` FOREIGN KEY (`condition_def_id`) REFERENCES `x_policy_condition_def` (`id`)
) ROW_FORMAT=DYNAMIC;

CREATE TABLE IF NOT EXISTS `x_policy_ref_datamask_type` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `guid` varchar(1024) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `added_by_id` bigint(20) DEFAULT NULL,
  `upd_by_id` bigint(20) DEFAULT NULL,
  `policy_id` bigint(20) NOT NULL,
  `datamask_def_id` bigint(20) NOT NULL,
  `datamask_type_name` varchar(4000) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `x_policy_ref_datamask_UK_polId_dmaskDefId`(`policy_id`, `datamask_def_id`),
  CONSTRAINT `x_policy_ref_datamask_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_policy_ref_datamask_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_policy_ref_datamask_FK_policy_id` FOREIGN KEY (`policy_id`) REFERENCES `x_policy` (`id`),
  CONSTRAINT `x_policy_ref_datamask_FK_datamask_def_id` FOREIGN KEY (`datamask_def_id`) REFERENCES `x_datamask_type_def` (`id`)
) ROW_FORMAT=DYNAMIC;

CREATE TABLE IF NOT EXISTS `x_policy_ref_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `guid` varchar(1024) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `added_by_id` bigint(20) DEFAULT NULL,
  `upd_by_id` bigint(20) DEFAULT NULL,
  `policy_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `user_name` varchar(4000) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `x_policy_ref_user_UK_polId_userId`(`policy_id`, `user_id`),
  CONSTRAINT `x_policy_ref_user_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_policy_ref_user_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_policy_ref_user_FK_policy_id` FOREIGN KEY (`policy_id`) REFERENCES `x_policy` (`id`),
  CONSTRAINT `x_policy_ref_user_FK_user_id` FOREIGN KEY (`user_id`) REFERENCES `x_user` (`id`)
) ROW_FORMAT=DYNAMIC;

CREATE TABLE IF NOT EXISTS `x_policy_ref_group` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `guid` varchar(1024) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `added_by_id` bigint(20) DEFAULT NULL,
  `upd_by_id` bigint(20) DEFAULT NULL,
  `policy_id` bigint(20) NOT NULL,
  `group_id` bigint(20) NOT NULL,
  `group_name` varchar(4000) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `x_policy_ref_group_UK_polId_groupId`(`policy_id`, `group_id`),
  CONSTRAINT `x_policy_ref_group_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_policy_ref_group_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_policy_ref_group_FK_policy_id` FOREIGN KEY (`policy_id`) REFERENCES `x_policy` (`id`),
  CONSTRAINT `x_policy_ref_group_FK_group_id` FOREIGN KEY (`group_id`) REFERENCES `x_group` (`id`)
) ROW_FORMAT=DYNAMIC;
