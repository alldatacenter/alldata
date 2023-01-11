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

DROP TABLE IF EXISTS `x_service_resource_element_val` ;
DROP TABLE IF EXISTS `x_tag_resource_map` ;
DROP TABLE IF EXISTS `x_tag_attr` ;
DROP TABLE IF EXISTS `x_tag_attr_def` ;
DROP TABLE IF EXISTS `x_service_resource_element` ;
DROP TABLE IF EXISTS `x_service_resource` ;
DROP TABLE IF EXISTS `x_tag` ;
DROP TABLE IF EXISTS `x_tag_def` ;
-- -----------------------------------------------------
-- Table `x_tag_def`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `x_tag_def` (
`id` BIGINT(20) NOT NULL AUTO_INCREMENT,
`guid` VARCHAR(64) NOT NULL,
`create_time` DATETIME NULL DEFAULT NULL,
`update_time` DATETIME NULL DEFAULT NULL,
`added_by_id` BIGINT(20) NULL DEFAULT NULL,
`upd_by_id` BIGINT(20) NULL DEFAULT NULL,
`version` BIGINT(20) NULL DEFAULT NULL,
`name` VARCHAR(255) NOT NULL,
`source` VARCHAR(128) NULL DEFAULT NULL,
`is_enabled` TINYINT(1) NOT NULL DEFAULT '0',
PRIMARY KEY (`id`),
UNIQUE KEY `x_tag_def_UK_guid` (`guid`),
UNIQUE KEY `x_tag_def_UK_name` (`name`),
KEY `x_tag_def_IDX_added_by_id` (`added_by_id`),
KEY `x_tag_def_IDX_upd_by_id` (`upd_by_id`),
CONSTRAINT `x_tag_def_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_tag_def_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;
-- -----------------------------------------------------
-- Table `x_tag`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `x_tag` (
`id` BIGINT(20) NOT NULL AUTO_INCREMENT,
`guid` VARCHAR(64) NOT NULL,
`create_time` DATETIME NULL DEFAULT NULL,
`update_time` DATETIME NULL DEFAULT NULL,
`added_by_id` BIGINT(20) NULL DEFAULT NULL,
`upd_by_id` BIGINT(20) NULL DEFAULT NULL,
`version` BIGINT(20) NULL DEFAULT NULL,
`type` BIGINT(20) NOT NULL,
PRIMARY KEY (`id`),
UNIQUE KEY `x_tag_UK_guid` (`guid`),
KEY `x_tag_IDX_type` (`type`),
KEY `x_tag_IDX_added_by_id` (`added_by_id`),
KEY `x_tag_IDX_upd_by_id` (`upd_by_id`),
CONSTRAINT `x_tag_FK_type` FOREIGN KEY (`type`) REFERENCES `x_tag_def` (`id`),
CONSTRAINT `x_tag_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_tag_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;
-- -----------------------------------------------------
-- Table `x_service_resource`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `x_service_resource` (
`id` BIGINT(20) NOT NULL AUTO_INCREMENT,
`guid` VARCHAR(64) NOT NULL,
`create_time` DATETIME NULL DEFAULT NULL,
`update_time` DATETIME NULL DEFAULT NULL,
`added_by_id` BIGINT(20) NULL DEFAULT NULL,
`upd_by_id` BIGINT(20) NULL DEFAULT NULL,
`version` BIGINT(20) NULL DEFAULT NULL,
`service_id` BIGINT(20) NOT NULL,
`resource_signature` varchar(128) NULL DEFAULT NULL,
`is_enabled` TINYINT NOT NULL DEFAULT '1',
PRIMARY KEY (`id`),
UNIQUE KEY `x_service_res_UK_guid` (`guid`),
KEY `x_service_res_IDX_added_by_id` (`added_by_id`),
KEY `x_service_res_IDX_upd_by_id` (`upd_by_id`),
CONSTRAINT `x_service_res_FK_service_id` FOREIGN KEY (`service_id`) REFERENCES `x_service` (`id`),
CONSTRAINT `x_service_res_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_service_res_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;
-- -----------------------------------------------------
-- Table `x_service_resource_element`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `x_service_resource_element` (
`id` BIGINT(20) NOT NULL AUTO_INCREMENT,
`create_time` DATETIME NULL DEFAULT NULL,
`update_time` DATETIME NULL DEFAULT NULL,
`added_by_id` BIGINT(20) NULL DEFAULT NULL,
`upd_by_id` BIGINT(20) NULL DEFAULT NULL,
`res_id` BIGINT(20) NOT NULL,
`res_def_id` BIGINT(20) NOT NULL,
`is_excludes` TINYINT(1) NOT NULL DEFAULT '0',
`is_recursive` TINYINT(1) NOT NULL DEFAULT '0',
PRIMARY KEY (`id`),
KEY `x_srvc_res_el_IDX_added_by_id` (`added_by_id`),
KEY `x_srvc_res_el_IDX_upd_by_id` (`upd_by_id`),
CONSTRAINT `x_srvc_res_el_FK_res_def_id` FOREIGN KEY (`res_def_id`) REFERENCES `x_resource_def` (`id`),
CONSTRAINT `x_srvc_res_el_FK_res_id` FOREIGN KEY (`res_id`) REFERENCES `x_service_resource` (`id`),
CONSTRAINT `x_srvc_res_el_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_srvc_res_el_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;
-- -----------------------------------------------------
-- Table `x_tag_attr_def`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `x_tag_attr_def` (
`id` BIGINT(20) NOT NULL AUTO_INCREMENT,
`create_time` DATETIME NULL DEFAULT NULL,
`update_time` DATETIME NULL DEFAULT NULL,
`added_by_id` BIGINT(20) NULL DEFAULT NULL,
`upd_by_id` BIGINT(20) NULL DEFAULT NULL,
`tag_def_id` BIGINT(20) NOT NULL,
`name` VARCHAR(255) NOT NULL,
`type` VARCHAR(50) NOT NULL,
PRIMARY KEY (`id`),
KEY `x_tag_attr_def_IDX_tag_def_id` (`tag_def_id`),
KEY `x_tag_attr_def_IDX_added_by_id` (`added_by_id`),
KEY `x_tag_attr_def_IDX_upd_by_id` (`upd_by_id`),
CONSTRAINT `x_tag_attr_def_FK_tag_def_id` FOREIGN KEY (`tag_def_id`) REFERENCES `x_tag_def` (`id`),
CONSTRAINT `x_tag_attr_def_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_tag_attr_def_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;
-- -----------------------------------------------------
-- Table `x_tag_attr`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `x_tag_attr` (
`id` BIGINT(20) NOT NULL AUTO_INCREMENT,
`create_time` DATETIME NULL DEFAULT NULL,
`update_time` DATETIME NULL DEFAULT NULL,
`added_by_id` BIGINT(20) NULL DEFAULT NULL,
`upd_by_id` BIGINT(20) NULL DEFAULT NULL,
`tag_id` BIGINT(20) NOT NULL,
`name` VARCHAR(255) NOT NULL,
`value` VARCHAR(512) NULL,
PRIMARY KEY (`id`),
KEY `x_tag_attr_IDX_tag_id` (`tag_id`),
KEY `x_tag_attr_IDX_added_by_id` (`added_by_id`),
KEY `x_tag_attr_IDX_upd_by_id` (`upd_by_id`),
CONSTRAINT `x_tag_attr_FK_tag_id` FOREIGN KEY (`tag_id`) REFERENCES `x_tag` (`id`),
CONSTRAINT `x_tag_attr_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_tag_attr_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;
-- -----------------------------------------------------
-- Table `x_tag_resource_map`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `x_tag_resource_map` (
`id` BIGINT(20) NOT NULL AUTO_INCREMENT,
`guid` VARCHAR(64) NOT NULL,
`create_time` DATETIME NULL DEFAULT NULL,
`update_time` DATETIME NULL DEFAULT NULL,
`added_by_id` BIGINT(20) NULL DEFAULT NULL,
`upd_by_id` BIGINT(20) NULL DEFAULT NULL,
`tag_id` BIGINT(20) NOT NULL,
`res_id` BIGINT(20) NOT NULL,
PRIMARY KEY (`id`),
UNIQUE KEY `x_tag_res_map_UK_guid` (`guid`),
KEY `x_tag_res_map_IDX_tag_id` (`tag_id`),
KEY `x_tag_res_map_IDX_res_id` (`res_id`),
KEY `x_tag_res_map_IDX_added_by_id` (`added_by_id`),
KEY `x_tag_res_map_IDX_upd_by_id` (`upd_by_id`),
CONSTRAINT `x_tag_res_map_FK_tag_id` FOREIGN KEY (`tag_id`) REFERENCES `x_tag` (`id`),
CONSTRAINT `x_tag_res_map_FK_res_id` FOREIGN KEY (`res_id`) REFERENCES `x_service_resource` (`id`),
CONSTRAINT `x_tag_res_map_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_tag_res_map_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;
-- -----------------------------------------------------
-- Table `x_service_resource_element_val`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `x_service_resource_element_val` (
`id` BIGINT(20) NOT NULL AUTO_INCREMENT,
`create_time` DATETIME NULL DEFAULT NULL,
`update_time` DATETIME NULL DEFAULT NULL,
`added_by_id` BIGINT(20) NULL DEFAULT NULL,
`upd_by_id` BIGINT(20) NULL DEFAULT NULL,
`res_element_id` BIGINT(20) NOT NULL,
`value` VARCHAR(1024) NOT NULL,
`sort_order` int DEFAULT 0,
PRIMARY KEY (`id`),
KEY `x_srvc_res_el_val_IDX_resel_id` (`res_element_id`),
KEY `x_srvc_res_el_val_IDX_addby_id` (`added_by_id`),
KEY `x_srvc_res_el_val_IDX_updby_id` (`upd_by_id`),
CONSTRAINT `x_srvc_res_el_val_FK_res_el_id` FOREIGN KEY (`res_element_id`) REFERENCES `x_service_resource_element` (`id`),
CONSTRAINT `x_srvc_res_el_val_FK_add_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_srvc_res_el_val_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;
INSERT INTO `x_modules_master` VALUES (6,now(),now(),1,1,'Tag Based Policies','');
-- ---------------------------------------
-- add column in x_service_def.def_options
-- ---------------------------------------
DROP PROCEDURE IF EXISTS add_columns_x_service_def;

DELIMITER ;;
CREATE PROCEDURE add_columns_x_service_def() BEGIN
  IF EXISTS (SELECT * FROM information_schema.tables WHERE table_schema=database() AND table_name = 'x_service_def') THEN
    IF NOT EXISTS (SELECT * FROM information_schema.columns WHERE table_schema=database() AND table_name = 'x_service_def' AND column_name = 'def_options') THEN
      ALTER TABLE `x_service_def` ADD COLUMN `def_options` VARCHAR(1024) DEFAULT NULL NULL;
    END IF;
  END IF;
END;;

DELIMITER ;
CALL add_columns_x_service_def();
DROP PROCEDURE IF EXISTS add_columns_x_service_def;

-- ---------------------------------------------------------------------------------------
-- add column in x_policy_item.item_type, x_policy_item.is_enabled, x_policy_item.comments
-- ---------------------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS add_columns_x_policy_item;

DELIMITER ;;
CREATE PROCEDURE add_columns_x_policy_item() BEGIN
	IF EXISTS (SELECT * FROM information_schema.tables WHERE table_schema=database() AND table_name = 'x_policy_item') THEN
		IF NOT EXISTS (SELECT * FROM information_schema.columns WHERE table_schema=database() AND table_name = 'x_policy_item' AND column_name = 'item_type') THEN
			IF NOT EXISTS (SELECT * FROM information_schema.columns WHERE table_schema=database() AND table_name = 'x_policy_item' AND column_name = 'is_enabled') THEN
				IF NOT EXISTS (SELECT * FROM information_schema.columns WHERE table_schema=database() AND table_name = 'x_policy_item' AND column_name = 'comments') THEN
					ALTER TABLE `x_policy_item` ADD COLUMN `item_type` INT DEFAULT 0 NOT NULL,
					ADD COLUMN `is_enabled` TINYINT(1) NOT NULL DEFAULT '1',
					ADD COLUMN `comments` VARCHAR(255) DEFAULT NULL NULL;
				END IF;
			END IF;
		END IF;
	END IF;
END;;

DELIMITER ;
CALL add_columns_x_policy_item();
DROP PROCEDURE IF EXISTS add_columns_x_policy_item;

-- ---------------------------------------------------------------------------------------
-- add columns in x_service.tag_service, x_service.tag_version, x_service.tag_update_time
-- ---------------------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS add_tag_columns_x_service;

DELIMITER ;;
CREATE PROCEDURE add_tag_columns_x_service() BEGIN
	IF EXISTS (SELECT * FROM information_schema.tables WHERE table_schema=database() AND table_name = 'x_service') THEN
		IF NOT EXISTS (SELECT * FROM information_schema.columns WHERE table_schema=database() AND table_name = 'x_service' AND column_name = 'tag_service') THEN
			IF NOT EXISTS (SELECT * FROM information_schema.columns WHERE table_schema=database() AND table_name = 'x_service' AND column_name = 'tag_version') THEN
				IF NOT EXISTS (SELECT * FROM information_schema.columns WHERE table_schema=database() AND table_name = 'x_service' AND column_name = 'tag_update_time') THEN
					ALTER TABLE `x_service` ADD COLUMN `tag_service` BIGINT DEFAULT NULL NULL,
					ADD COLUMN `tag_version` BIGINT DEFAULT 0 NOT NULL,
					ADD COLUMN `tag_update_time` DATETIME DEFAULT NULL NULL,
					ADD CONSTRAINT `x_service_FK_tag_service` FOREIGN KEY (`tag_service`) REFERENCES `x_service` (`id`);
		END IF;
	END IF;
END IF;
END IF;
END;;

DELIMITER ;
CALL add_tag_columns_x_service();
DROP PROCEDURE IF EXISTS add_tag_columns_x_service;
