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

-- Temporary table structure for view `vx_trx_log`
--

DROP TABLE IF EXISTS `x_data_hist`;
DROP TABLE IF EXISTS `x_policy_item_group_perm`;
DROP TABLE IF EXISTS `x_policy_item_user_perm`;
DROP TABLE IF EXISTS `x_policy_item_condition`;
DROP TABLE IF EXISTS `x_policy_item_access`;
DROP TABLE IF EXISTS `x_policy_item`;
DROP TABLE IF EXISTS `x_policy_resource_map`;
DROP TABLE IF EXISTS `x_policy_resource`;
DROP TABLE IF EXISTS `x_service_config_map`;
DROP TABLE IF EXISTS `x_enum_element_def`;
DROP TABLE IF EXISTS `x_enum_def`;
DROP TABLE IF EXISTS `x_context_enricher_def`;
DROP TABLE IF EXISTS `x_policy_condition_def`;
DROP TABLE IF EXISTS `x_access_type_def_grants`;
DROP TABLE IF EXISTS `x_access_type_def`;
DROP TABLE IF EXISTS `x_resource_def`;
DROP TABLE IF EXISTS `x_service_config_def`;
DROP TABLE IF EXISTS `x_policy`;
DROP TABLE IF EXISTS `x_service`;
DROP TABLE IF EXISTS `x_service_def`;

CREATE TABLE `x_service_def` (
`id` bigint(20) NOT NULL AUTO_INCREMENT ,
`guid` varchar(1024) DEFAULT NULL,
`create_time` datetime DEFAULT NULL,
`update_time` datetime DEFAULT NULL,
`added_by_id` bigint(20) DEFAULT NULL,
`upd_by_id` bigint(20) DEFAULT NULL,
`version` bigint(20) DEFAULT NULL,
`name` varchar(1024) DEFAULT NULL,
`impl_class_name` varchar(1024) DEFAULT NULL,
`label` varchar(1024) DEFAULT NULL,
`description` varchar(1024) DEFAULT NULL,
`rb_key_label` varchar(1024) DEFAULT NULL,
`rb_key_description` varchar(1024) DEFAULT NULL,
`is_enabled` tinyint DEFAULT 1,
primary key (`id`),      
KEY `x_service_def_added_by_id` (`added_by_id`),
KEY `x_service_def_upd_by_id` (`upd_by_id`),
KEY `x_service_def_cr_time` (`create_time`),
KEY `x_service_def_up_time` (`update_time`),
CONSTRAINT `x_service_def_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_service_def_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE `x_service` ( 
`id` bigint(20) NOT NULL AUTO_INCREMENT ,
`guid` varchar(1024) DEFAULT NULL,
`create_time` datetime DEFAULT NULL,
`update_time` datetime DEFAULT NULL,
`added_by_id` bigint(20) DEFAULT NULL,
`upd_by_id` bigint(20) DEFAULT NULL,
`version` bigint(20) DEFAULT NULL,
`type` bigint(20) DEFAULT NULL,
`name` varchar(255) DEFAULT NULL,   
`policy_version` bigint(20) DEFAULT NULL,
`policy_update_time`datetime DEFAULT NULL,
`description` varchar(1024) DEFAULT NULL,
`is_enabled` tinyint(1) NOT NULL DEFAULT '0',   
primary key (`id`),
UNIQUE KEY `X_service_name` (`name`),
KEY `x_service_added_by_id` (`added_by_id`),
KEY `x_service_upd_by_id` (`upd_by_id`),
KEY `x_service_cr_time` (`create_time`),
KEY `x_service_up_time` (`update_time`),
KEY `x_service_type` (`type`),  
CONSTRAINT `x_service_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_service_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_service_FK_type` FOREIGN KEY (`type`) REFERENCES `x_service_def` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE  `x_policy` (
`id` bigint(20) NOT NULL AUTO_INCREMENT ,
`guid` varchar(1024) DEFAULT NULL,
`create_time` datetime DEFAULT NULL,
`update_time` datetime DEFAULT NULL,
`added_by_id` bigint(20) DEFAULT NULL,
`upd_by_id` bigint(20) DEFAULT NULL,
`version` bigint(20) DEFAULT NULL,
`service` bigint(20) DEFAULT NULL,
`name` varchar(512) DEFAULT NULL, 
`policy_type` int(11) DEFAULT 0,
`description` varchar(1024) DEFAULT NULL,
`resource_signature` varchar(128) DEFAULT NULL,
`is_enabled` tinyint(1) NOT NULL DEFAULT '0',
`is_audit_enabled` tinyint(1) NOT NULL DEFAULT '0',
primary key (`id`),
KEY `x_policy_added_by_id` (`added_by_id`),
KEY `x_policy_upd_by_id` (`upd_by_id`),
KEY `x_policy_cr_time` (`create_time`),
KEY `x_policy_up_time` (`update_time`),
KEY `x_policy_service` (`service`),
KEY `x_policy_resource_signature` (`resource_signature`),
CONSTRAINT `x_policy_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_policy_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_policy_FK_service` FOREIGN KEY (`service`) REFERENCES `x_service` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE `x_service_config_def` (
`id` bigint(20) NOT NULL AUTO_INCREMENT ,
`guid` varchar(1024) DEFAULT NULL,
`create_time` datetime DEFAULT NULL,
`update_time` datetime DEFAULT NULL,
`added_by_id` bigint(20) DEFAULT NULL,
`upd_by_id` bigint(20) DEFAULT NULL,
`def_id` bigint(20) NOT NULL,
`item_id` bigint(20) NOT NULL,
`name` varchar(1024) DEFAULT NULL,  
`type` varchar(1024) DEFAULT NULL,
`sub_type` varchar(1024) DEFAULT NULL,
`is_mandatory` tinyint(1) NOT NULL DEFAULT '0',
`default_value` varchar(1024) DEFAULT NULL,
`validation_reg_ex` varchar(1024) DEFAULT NULL,
`validation_message` varchar(1024) DEFAULT NULL,
`ui_hint` varchar(1024) DEFAULT NULL,
`label` varchar(1024) DEFAULT NULL,
`description` varchar(1024) DEFAULT NULL,
`rb_key_label` varchar(1024) DEFAULT NULL,
`rb_key_description` varchar(1024) DEFAULT NULL,
`rb_key_validation_message` varchar(1024) DEFAULT NULL,
`sort_order` int DEFAULT 0,
primary key (`id`),
CONSTRAINT `x_service_config_def_FK_defid` FOREIGN KEY (`def_id`) REFERENCES `x_service_def` (`id`),
CONSTRAINT `x_service_config_def_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_service_config_def_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE `x_resource_def` (
`id` bigint(20) NOT NULL AUTO_INCREMENT ,
`guid` varchar(1024) DEFAULT NULL,
`create_time` datetime DEFAULT NULL,
`update_time` datetime DEFAULT NULL,
`added_by_id` bigint(20) DEFAULT NULL,
`upd_by_id` bigint(20) DEFAULT NULL,
`def_id` bigint(20) NOT NULL,  
`item_id` bigint(20) NOT NULL,  
`name` varchar(1024) DEFAULT NULL,
`type` varchar(1024) DEFAULT NULL,  
`res_level` bigint(20) DEFAULT NULL,  
`parent` bigint(20) DEFAULT NULL,  
`mandatory` tinyint(1) NOT NULL DEFAULT '0',
`look_up_supported` tinyint(1) NOT NULL DEFAULT '0',
`recursive_supported` tinyint(1) NOT NULL DEFAULT '0',
`excludes_supported` tinyint(1) NOT NULL DEFAULT '0',
`matcher` varchar(1024) DEFAULT NULL,
`matcher_options` varchar(1024) DEFAULT NULL,
`validation_reg_ex` varchar(1024) DEFAULT NULL,
`validation_message` varchar(1024) DEFAULT NULL,
`ui_hint` varchar(1024) DEFAULT NULL,
`label` varchar(1024) DEFAULT NULL,  
`description` varchar(1024) DEFAULT NULL,  
`rb_key_label` varchar(1024) DEFAULT NULL,  
`rb_key_description` varchar(1024) DEFAULT NULL, 
`rb_key_validation_message` varchar(1024) DEFAULT NULL,
`sort_order` int DEFAULT 0,
primary key (`id`),
KEY `x_resource_def_FK_parent` (`parent`),   
CONSTRAINT `x_resource_def_FK_parent` FOREIGN KEY (`parent`) REFERENCES `x_resource_def` (`id`) ,
CONSTRAINT `x_resource_def_FK_defid` FOREIGN KEY (`def_id`) REFERENCES `x_service_def` (`id`),
CONSTRAINT `x_resource_def_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_resource_def_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE `x_access_type_def` (
`id` bigint(20) NOT NULL AUTO_INCREMENT ,
`guid` varchar(1024) DEFAULT NULL,
`create_time` datetime DEFAULT NULL,
`update_time` datetime DEFAULT NULL,
`added_by_id` bigint(20) DEFAULT NULL,
`upd_by_id` bigint(20) DEFAULT NULL,
`def_id` bigint(20) NOT NULL,    
`item_id` bigint(20) NOT NULL,    
`name` varchar(1024) DEFAULT NULL,  
`label` varchar(1024) DEFAULT NULL,   
`rb_key_label` varchar(1024) DEFAULT NULL, 
`sort_order` int DEFAULT 0,
primary key (`id`)   ,
CONSTRAINT `x_access_type_def_FK_defid` FOREIGN KEY (`def_id`) REFERENCES `x_service_def` (`id`),
CONSTRAINT `x_access_type_def_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_access_type_def_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE `x_access_type_def_grants` (
`id` bigint(20) NOT NULL AUTO_INCREMENT ,
`guid` varchar(1024) DEFAULT NULL,
`create_time` datetime DEFAULT NULL,
`update_time` datetime DEFAULT NULL,
`added_by_id` bigint(20) DEFAULT NULL,
`upd_by_id` bigint(20) DEFAULT NULL,
`atd_id` bigint(20) NOT NULL,     
`implied_grant` varchar(1024) DEFAULT NULL,  
primary key (`id`),
CONSTRAINT `x_atd_grants_FK_atdid` FOREIGN KEY (`atd_id`) REFERENCES `x_access_type_def` (`id`),
CONSTRAINT `x_atd_grants_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_atd_grants_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE `x_policy_condition_def` (
`id` bigint(20) NOT NULL AUTO_INCREMENT ,
`guid` varchar(1024) DEFAULT NULL,
`create_time` datetime DEFAULT NULL,
`update_time` datetime DEFAULT NULL,
`added_by_id` bigint(20) DEFAULT NULL,
`upd_by_id` bigint(20) DEFAULT NULL,
`def_id` bigint(20) NOT NULL,      
`item_id` bigint(20) NOT NULL,      
`name` varchar(1024) DEFAULT NULL,  
`evaluator` varchar(1024) DEFAULT NULL,
`evaluator_options` varchar(1024) DEFAULT NULL,
`validation_reg_ex` varchar(1024) DEFAULT NULL,
`validation_message` varchar(1024) DEFAULT NULL,
`ui_hint` varchar(1024) DEFAULT NULL,
`label` varchar(1024) DEFAULT NULL,  
`description` varchar(1024) DEFAULT NULL,  
`rb_key_label` varchar(1024) DEFAULT NULL,  
`rb_key_description` varchar(1024) DEFAULT NULL,  
`rb_key_validation_message` varchar(1024) DEFAULT NULL,
`sort_order` int DEFAULT 0,
primary key (`id`)   ,
CONSTRAINT `x_policy_condition_def_FK_defid` FOREIGN KEY (`def_id`) REFERENCES `x_service_def` (`id`),
CONSTRAINT `x_policy_condition_def_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_policy_condition_def_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE `x_context_enricher_def` (
`id` bigint(20) NOT NULL AUTO_INCREMENT ,
`guid` varchar(1024) DEFAULT NULL,
`create_time` datetime DEFAULT NULL,
`update_time` datetime DEFAULT NULL,
`added_by_id` bigint(20) DEFAULT NULL,
`upd_by_id` bigint(20) DEFAULT NULL,
`def_id` bigint(20) NOT NULL,      
`item_id` bigint(20) NOT NULL,      
`name` varchar(1024) DEFAULT NULL,  
`enricher` varchar(1024) DEFAULT NULL,
`enricher_options` varchar(1024) DEFAULT NULL,
`sort_order` int DEFAULT 0,
primary key (`id`)   ,
CONSTRAINT `x_context_enricher_def_FK_defid` FOREIGN KEY (`def_id`) REFERENCES `x_service_def` (`id`),
CONSTRAINT `x_context_enricher_def_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_context_enricher_def_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE `x_enum_def` (
`id` bigint(20) NOT NULL AUTO_INCREMENT ,
`guid` varchar(1024) DEFAULT NULL,
`create_time` datetime DEFAULT NULL,
`update_time` datetime DEFAULT NULL,
`added_by_id` bigint(20) DEFAULT NULL,
`upd_by_id` bigint(20) DEFAULT NULL,
`def_id` bigint(20) NOT NULL, 
`item_id` bigint(20) NOT NULL, 
`name` varchar(1024) DEFAULT NULL,  
`default_index` bigint(20) DEFAULT NULL,    
primary key (`id`),    
CONSTRAINT `x_enum_def_FK_defid` FOREIGN KEY (`def_id`) REFERENCES `x_service_def` (`id`),
CONSTRAINT `x_enum_def_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_enum_def_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE `x_enum_element_def` (
`id` bigint(20) NOT NULL AUTO_INCREMENT ,
`guid` varchar(1024) DEFAULT NULL,
`create_time` datetime DEFAULT NULL,
`update_time` datetime DEFAULT NULL,
`added_by_id` bigint(20) DEFAULT NULL,
`upd_by_id` bigint(20) DEFAULT NULL,
`enum_def_id` bigint(20) NOT NULL, 
`item_id` bigint(20) NOT NULL, 
`name` varchar(1024) DEFAULT NULL,  
`label` varchar(1024) DEFAULT NULL,  
`rb_key_label` varchar(1024) DEFAULT NULL,   
`sort_order` int DEFAULT 0,
primary key (`id`),    
CONSTRAINT `x_enum_element_def_FK_defid` FOREIGN KEY (`enum_def_id`) REFERENCES `x_enum_def` (`id`),
CONSTRAINT `x_enum_element_def_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_enum_element_def_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE `x_service_config_map` (
`id` bigint(20) NOT NULL AUTO_INCREMENT ,
`guid` varchar(1024) DEFAULT NULL,
`create_time` datetime DEFAULT NULL,
`update_time` datetime DEFAULT NULL,
`added_by_id` bigint(20) DEFAULT NULL,
`upd_by_id` bigint(20) DEFAULT NULL,
`service` bigint(20) NOT NULL, 
`config_key` varchar(1024) DEFAULT NULL,   
`config_value` varchar(4000) DEFAULT NULL,    
primary key (`id`),    
CONSTRAINT `x_service_config_map_FK_` FOREIGN KEY (`service`) REFERENCES `x_service` (`id`),
CONSTRAINT `x_service_config_map_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_service_config_map_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE `x_policy_resource` (
`id` bigint(20) NOT NULL AUTO_INCREMENT ,
`guid` varchar(1024) DEFAULT NULL,
`create_time` datetime DEFAULT NULL,
`update_time` datetime DEFAULT NULL,
`added_by_id` bigint(20) DEFAULT NULL,
`upd_by_id` bigint(20) DEFAULT NULL,
`policy_id` bigint(20) NOT NULL, 
`res_def_id` bigint(20) NOT NULL, 
`is_excludes` tinyint(1) NOT NULL DEFAULT '0',
`is_recursive` tinyint(1) NOT NULL DEFAULT '0',
primary key (`id`),    
CONSTRAINT `x_policy_resource_FK_policy_id` FOREIGN KEY (`policy_id`) REFERENCES `x_policy` (`id`), 
CONSTRAINT `x_policy_resource_FK_res_def_id` FOREIGN KEY (`res_def_id`) REFERENCES `x_resource_def` (`id`),
CONSTRAINT `x_policy_resource_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_policy_resource_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE `x_policy_resource_map` (
`id` bigint(20) NOT NULL AUTO_INCREMENT ,
`guid` varchar(1024) DEFAULT NULL,
`create_time` datetime DEFAULT NULL,
`update_time` datetime DEFAULT NULL,
`added_by_id` bigint(20) DEFAULT NULL,
`upd_by_id` bigint(20) DEFAULT NULL,
`resource_id` bigint(20) NOT NULL, 
`value` varchar(1024) DEFAULT NULL,  
`sort_order` int DEFAULT 0,
primary key (`id`),    
CONSTRAINT `x_policy_resource_map_FK_resource_id` FOREIGN KEY (`resource_id`) REFERENCES `x_policy_resource` (`id`),
CONSTRAINT `x_policy_resource_map_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_policy_resource_map_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE `x_policy_item` (
`id` bigint(20) NOT NULL AUTO_INCREMENT ,
`guid` varchar(1024) DEFAULT NULL,
`create_time` datetime DEFAULT NULL,
`update_time` datetime DEFAULT NULL,
`added_by_id` bigint(20) DEFAULT NULL,
`upd_by_id` bigint(20) DEFAULT NULL,
`policy_id` bigint(20) NOT NULL,
`delegate_admin` tinyint(1) NOT NULL DEFAULT '0',
`sort_order` int DEFAULT 0,
primary key (`id`), 
CONSTRAINT `x_policy_item_FK_policy_id` FOREIGN KEY (`policy_id`) REFERENCES `x_policy` (`id`),
CONSTRAINT `x_policy_item_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_policy_item_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE `x_policy_item_access` (
`id` bigint(20) NOT NULL AUTO_INCREMENT ,
`guid` varchar(1024) DEFAULT NULL,
`create_time` datetime DEFAULT NULL,
`update_time` datetime DEFAULT NULL,
`added_by_id` bigint(20) DEFAULT NULL,
`upd_by_id` bigint(20) DEFAULT NULL,
`policy_item_id` bigint(20) NOT NULL, 
`type` bigint(20) NOT NULL,
`is_allowed` tinyint(11) NOT NULL DEFAULT '0',
`sort_order` int DEFAULT 0,
primary key (id), 
CONSTRAINT `x_policy_item_access_FK_pi_id` FOREIGN KEY (`policy_item_id`) REFERENCES `x_policy_item` (`id`) ,
CONSTRAINT `x_policy_item_access_FK_atd_id` FOREIGN KEY (`type`) REFERENCES `x_access_type_def` (`id`),
CONSTRAINT `x_policy_item_access_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_policy_item_access_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE `x_policy_item_condition` (
`id` bigint(20) NOT NULL AUTO_INCREMENT ,
`guid` varchar(1024) DEFAULT NULL,
`create_time` datetime DEFAULT NULL,
`update_time` datetime DEFAULT NULL,
`added_by_id` bigint(20) DEFAULT NULL,
`upd_by_id` bigint(20) DEFAULT NULL,
`policy_item_id` bigint(20) NOT NULL, 
`type` bigint(20) NOT NULL,
`value` varchar(1024) DEFAULT NULL, 
`sort_order` int DEFAULT 0,
primary key (id), 
CONSTRAINT `x_policy_item_condition_FK_pi_id` FOREIGN KEY (`policy_item_id`) REFERENCES `x_policy_item` (`id`) ,
CONSTRAINT `x_policy_item_condition_FK_pcd_id` FOREIGN KEY (`type`) REFERENCES `x_policy_condition_def` (`id`),
CONSTRAINT `x_policy_item_condition_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_policy_item_condition_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE `x_policy_item_user_perm` (
`id` bigint(20) NOT NULL AUTO_INCREMENT ,
`guid` varchar(1024) DEFAULT NULL,
`create_time` datetime DEFAULT NULL,
`update_time` datetime DEFAULT NULL,
`added_by_id` bigint(20) DEFAULT NULL,
`upd_by_id` bigint(20) DEFAULT NULL,
`policy_item_id` bigint(20) NOT NULL,
`user_id` bigint(20) NULL DEFAULT NULL,
`sort_order` int DEFAULT 0,
primary key (`id`), 
CONSTRAINT `x_policy_item_user_perm_FK_pi_id` FOREIGN KEY (`policy_item_id`) REFERENCES `x_policy_item` (`id`) ,
CONSTRAINT `x_policy_item_user_perm_FK_user_id` FOREIGN KEY (`user_id`) REFERENCES `x_user` (`id`),
CONSTRAINT `x_policy_item_user_perm_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_policy_item_user_perm_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE `x_policy_item_group_perm` (
`id` bigint(20) NOT NULL AUTO_INCREMENT ,
`guid` varchar(1024) DEFAULT NULL,
`create_time` datetime DEFAULT NULL,
`update_time` datetime DEFAULT NULL,
`added_by_id` bigint(20) DEFAULT NULL,
`upd_by_id` bigint(20) DEFAULT NULL,
`policy_item_id` bigint(20) NOT NULL,
`group_id` bigint(20) NULL DEFAULT NULL,
`sort_order` int DEFAULT 0,
primary key (`id`), 
CONSTRAINT `x_policy_item_group_perm_FK_pi_id` FOREIGN KEY (`policy_item_id`) REFERENCES `x_policy_item` (`id`) ,
CONSTRAINT `x_policy_item_group_perm_FK_group_id` FOREIGN KEY (`group_id`) REFERENCES `x_group` (`id`),
CONSTRAINT `x_policy_item_group_perm_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_policy_item_group_perm_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE `x_data_hist` (
`id` bigint(20) NOT NULL AUTO_INCREMENT ,
`create_time` datetime DEFAULT NULL,
`update_time` datetime DEFAULT NULL,
`obj_guid` varchar(1024) not null,
`obj_class_type` int NOT NULL,
`obj_id` bigint(20) not null,
`obj_name` varchar(1024) NOT NULL,
`version` bigint(20) DEFAULT NULL,
`action` varchar(512) NOT NULL,
`from_time` datetime NOT NULL,
`to_time` datetime DEFAULT NULL,
`content` text NOT NULL,
primary key (`id`)
)ROW_FORMAT=DYNAMIC;

