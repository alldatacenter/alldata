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

DROP VIEW IF EXISTS `vx_trx_log`;
DROP TABLE IF EXISTS `x_rms_mapping_provider`;
DROP TABLE IF EXISTS `x_rms_resource_mapping`;
DROP TABLE IF EXISTS `x_rms_notification`;
DROP TABLE IF EXISTS `x_rms_service_resource`;
DROP TABLE IF EXISTS `x_tag_change_log`;
DROP TABLE IF EXISTS `x_role_ref_role`;
DROP TABLE IF EXISTS `x_policy_ref_role`;
DROP TABLE IF EXISTS `x_role_ref_group`;
DROP TABLE IF EXISTS `x_role_ref_user`;
DROP TABLE IF EXISTS `x_role`;
DROP TABLE IF EXISTS `x_policy_change_log`;
DROP TABLE IF EXISTS `x_security_zone_ref_resource`;
DROP TABLE IF EXISTS `x_policy_ref_group`;
DROP TABLE IF EXISTS `x_policy_ref_user`;
DROP TABLE IF EXISTS `x_policy_ref_datamask_type`;
DROP TABLE IF EXISTS `x_policy_ref_condition`;
DROP TABLE IF EXISTS `x_policy_ref_access_type`;
DROP TABLE IF EXISTS `x_policy_ref_resource`;
DROP TABLE IF EXISTS `x_ugsync_audit_info`;
DROP TABLE IF EXISTS `x_policy_label_map`;
DROP TABLE IF EXISTS `x_policy_label`;
DROP TABLE IF EXISTS `x_plugin_info`;
DROP TABLE IF EXISTS `x_service_version_info`;
DROP TABLE IF EXISTS `x_policy_item_rowfilter`;
DROP TABLE IF EXISTS `x_policy_item_datamask`;
DROP TABLE IF EXISTS `x_datamask_type_def`;
DROP TABLE IF EXISTS `x_tag_resource_map`;
DROP TABLE IF EXISTS `x_service_resource`;
DROP TABLE IF EXISTS `x_tag`;
DROP TABLE IF EXISTS `x_tag_def`;
DROP TABLE IF EXISTS `x_group_module_perm`;
DROP TABLE IF EXISTS `x_user_module_perm`;
DROP TABLE IF EXISTS `x_modules_master`;
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
DROP TABLE IF EXISTS `x_security_zone_ref_group`;
DROP TABLE IF EXISTS `x_security_zone_ref_user`;
DROP TABLE IF EXISTS `x_security_zone_ref_tag_srvc`;
DROP TABLE IF EXISTS `x_security_zone_ref_service`;
DROP TABLE IF EXISTS `x_ranger_global_state`;
DROP TABLE IF EXISTS `x_security_zone`;
DROP TABLE IF EXISTS `x_service`;
DROP TABLE IF EXISTS `x_service_def`;
DROP TABLE IF EXISTS `x_audit_map`;
DROP TABLE IF EXISTS `x_perm_map`;
DROP TABLE IF EXISTS `x_trx_log`;
DROP TABLE IF EXISTS `x_resource`;
DROP TABLE IF EXISTS `x_policy_export_audit`;
DROP TABLE IF EXISTS `x_group_users`;
DROP TABLE IF EXISTS `x_user`;
DROP TABLE IF EXISTS `x_group_groups`;
DROP TABLE IF EXISTS `x_group`;
DROP TABLE IF EXISTS `x_db_base`;
DROP TABLE IF EXISTS `x_cred_store`;
DROP TABLE IF EXISTS `x_auth_sess`;
DROP TABLE IF EXISTS `x_asset`;
DROP TABLE IF EXISTS `xa_access_audit`;
DROP TABLE IF EXISTS `x_portal_user_role`;
DROP TABLE IF EXISTS `x_portal_user`;
DROP TABLE IF EXISTS `x_db_version_h`;

CREATE TABLE `x_db_version_h`  (
        `id`				bigint NOT NULL auto_increment primary key,
        `version`   		varchar(64) NOT NULL,
        `inst_at` 	    	timestamp NOT NULL DEFAULT current_timestamp,
        `inst_by` 	    	varchar(256) NOT NULL,
        `updated_at`      	timestamp NULL DEFAULT NULL,
    `updated_by`      	varchar(256) NOT NULL,
        `active`          	ENUM('Y', 'N') DEFAULT 'Y'
)ROW_FORMAT=DYNAMIC;

CREATE TABLE `x_portal_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `added_by_id` bigint(20) DEFAULT NULL,
  `upd_by_id` bigint(20) DEFAULT NULL,
  `first_name` varchar(1022) DEFAULT NULL,
  `last_name` varchar(1022) DEFAULT NULL,
  `pub_scr_name` varchar(2048) DEFAULT NULL,
  `login_id` varchar(767) DEFAULT NULL,
  `password` varchar(512) NOT NULL,
  `email` varchar(512) DEFAULT NULL,
  `status` int(11) NOT NULL DEFAULT '0',
  `user_src` int(11) NOT NULL DEFAULT '0',
  `notes` varchar(4000) DEFAULT NULL,
  `other_attributes` varchar(4000) DEFAULT NULL,
  `sync_source` varchar(4000) DEFAULT NULL,
  `old_passwords` text DEFAULT NULL,
  `password_updated_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `x_portal_user_UK_login_id` (`login_id`),
  UNIQUE KEY `x_portal_user_UK_email` (`email`),
  KEY `x_portal_user_FK_added_by_id` (`added_by_id`),
  KEY `x_portal_user_FK_upd_by_id` (`upd_by_id`),
  KEY `x_portal_user_cr_time` (`create_time`),
  KEY `x_portal_user_up_time` (`update_time`),
  KEY `x_portal_user_name` (`first_name`(767)),
  KEY `x_portal_user_email` (`email`),
  CONSTRAINT `x_portal_user_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_portal_user_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
) ROW_FORMAT=DYNAMIC;

CREATE TABLE `x_portal_user_role` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `added_by_id` bigint(20) DEFAULT NULL,
  `upd_by_id` bigint(20) DEFAULT NULL,
  `user_id` bigint(20) NOT NULL,
  `user_role` varchar(128) DEFAULT NULL,
  `status` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `x_portal_user_role_FK_added_by_id` (`added_by_id`),
  KEY `x_portal_user_role_FK_upd_by_id` (`upd_by_id`),
  KEY `x_portal_user_role_FK_user_id` (`user_id`),
  KEY `x_portal_user_role_cr_time` (`create_time`),
  KEY `x_portal_user_role_up_time` (`update_time`),
  CONSTRAINT `x_portal_user_role_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_portal_user_role_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_portal_user_role_FK_user_id` FOREIGN KEY (`user_id`) REFERENCES `x_portal_user` (`id`)
) ROW_FORMAT=DYNAMIC;

CREATE TABLE `xa_access_audit` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `added_by_id` bigint(20) DEFAULT NULL,
  `upd_by_id` bigint(20) DEFAULT NULL,
  `audit_type` int(11) NOT NULL DEFAULT '0',
  `access_result` int(11) DEFAULT '0',
  `access_type` varchar(255) DEFAULT NULL,
  `acl_enforcer` varchar(255) DEFAULT NULL,
  `agent_id` varchar(255) DEFAULT NULL,
  `client_ip` varchar(255) DEFAULT NULL,
  `client_type` varchar(255) DEFAULT NULL,
  `policy_id` bigint(20) DEFAULT '0',
  `repo_name` varchar(255) DEFAULT NULL,
  `repo_type` int(11) DEFAULT '0',
  `result_reason` varchar(255) DEFAULT NULL,
  `session_id` varchar(255) DEFAULT NULL,
  `event_time` datetime DEFAULT NULL,
  `request_user` varchar(255) DEFAULT NULL,
  `action` varchar(2000) DEFAULT NULL,
  `request_data` varchar(2000) DEFAULT NULL,
  `resource_path` varchar(2000) DEFAULT NULL,
  `resource_type` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `xa_access_audit_added_by_id` (`added_by_id`),
  KEY `xa_access_audit_upd_by_id` (`upd_by_id`),
  KEY `xa_access_audit_cr_time` (`create_time`),
  KEY `xa_access_audit_up_time` (`update_time`),
  KEY `xa_access_audit_event_time` (`event_time`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE `x_asset` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `added_by_id` bigint(20) DEFAULT NULL,
  `upd_by_id` bigint(20) DEFAULT NULL,
  `asset_name` varchar(1024) NOT NULL,
  `descr` varchar(4000) NOT NULL,
  `act_status` int(11) NOT NULL DEFAULT '0',
  `asset_type` int(11) NOT NULL DEFAULT '0',
  `config` MEDIUMTEXT NULL DEFAULT NULL,
  `sup_native` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `x_asset_FK_added_by_id` (`added_by_id`),
  KEY `x_asset_FK_upd_by_id` (`upd_by_id`),
  KEY `x_asset_cr_time` (`create_time`),
  KEY `x_asset_up_time` (`update_time`),
  CONSTRAINT `x_asset_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_asset_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE `x_auth_sess` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `added_by_id` bigint(20) DEFAULT NULL,
  `upd_by_id` bigint(20) DEFAULT NULL,
  `login_id` varchar(767) NOT NULL,
  `user_id` bigint(20) DEFAULT NULL,
  `ext_sess_id` varchar(512) DEFAULT NULL,
  `auth_time` datetime NOT NULL,
  `auth_status` int(11) NOT NULL DEFAULT '0',
  `auth_type` int(11) NOT NULL DEFAULT '0',
  `auth_provider` int(11) NOT NULL DEFAULT '0',
  `device_type` int(11) NOT NULL DEFAULT '0',
  `req_ip` varchar(48) NOT NULL,
  `req_ua` varchar(1024) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `x_auth_sess_FK_added_by_id` (`added_by_id`),
  KEY `x_auth_sess_FK_upd_by_id` (`upd_by_id`),
  KEY `x_auth_sess_FK_user_id` (`user_id`),
  KEY `x_auth_sess_cr_time` (`create_time`),
  KEY `x_auth_sess_up_time` (`update_time`),
  CONSTRAINT `x_auth_sess_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_auth_sess_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_auth_sess_FK_user_id` FOREIGN KEY (`user_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE `x_cred_store` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `added_by_id` bigint(20) DEFAULT NULL,
  `upd_by_id` bigint(20) DEFAULT NULL,
  `store_name` varchar(1024) NOT NULL,
  `descr` varchar(4000) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `x_cred_store_FK_added_by_id` (`added_by_id`),
  KEY `x_cred_store_FK_upd_by_id` (`upd_by_id`),
  KEY `x_cred_store_cr_time` (`create_time`),
  KEY `x_cred_store_up_time` (`update_time`),
  CONSTRAINT `x_cred_store_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_cred_store_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE `x_db_base` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `added_by_id` bigint(20) DEFAULT NULL,
  `upd_by_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `x_db_base_FK_added_by_id` (`added_by_id`),
  KEY `x_db_base_FK_upd_by_id` (`upd_by_id`),
  KEY `x_db_base_cr_time` (`create_time`),
  KEY `x_db_base_up_time` (`update_time`),
  CONSTRAINT `x_db_base_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_db_base_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE `x_group` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `added_by_id` bigint(20) DEFAULT NULL,
  `upd_by_id` bigint(20) DEFAULT NULL,
  `group_name` varchar(767) NOT NULL,
  `descr` varchar(4000) NOT NULL,
  `status` int(11) NOT NULL DEFAULT '0',
  `group_type` int(11) NOT NULL DEFAULT '0',
  `cred_store_id` bigint(20) DEFAULT NULL,
  `group_src` INT NOT NULL DEFAULT 0,
  `is_visible` INT(11) NOT NULL DEFAULT '1',
  `other_attributes` varchar(4000) DEFAULT NULL,
  `sync_source` varchar(4000) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `x_group_UK_group_name` (`group_name`),
  KEY `x_group_FK_added_by_id` (`added_by_id`),
  KEY `x_group_FK_upd_by_id` (`upd_by_id`),
  KEY `x_group_FK_cred_store_id` (`cred_store_id`),
  KEY `x_group_cr_time` (`create_time`),
  KEY `x_group_up_time` (`update_time`),
  CONSTRAINT `x_group_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_group_FK_cred_store_id` FOREIGN KEY (`cred_store_id`) REFERENCES `x_cred_store` (`id`),
  CONSTRAINT `x_group_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE `x_group_groups` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `added_by_id` bigint(20) DEFAULT NULL,
  `upd_by_id` bigint(20) DEFAULT NULL,
  `group_name` varchar(1024) NOT NULL,
  `p_group_id` bigint(20) DEFAULT NULL,
  `group_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `x_group_groups_FK_added_by_id` (`added_by_id`),
  KEY `x_group_groups_FK_upd_by_id` (`upd_by_id`),
  KEY `x_group_groups_FK_p_group_id` (`p_group_id`),
  KEY `x_group_groups_FK_group_id` (`group_id`),
  KEY `x_group_groups_cr_time` (`create_time`),
  KEY `x_group_groups_up_time` (`update_time`),
  CONSTRAINT `x_group_groups_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_group_groups_FK_group_id` FOREIGN KEY (`group_id`) REFERENCES `x_group` (`id`),
  CONSTRAINT `x_group_groups_FK_p_group_id` FOREIGN KEY (`p_group_id`) REFERENCES `x_group` (`id`),
  CONSTRAINT `x_group_groups_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE `x_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `added_by_id` bigint(20) DEFAULT NULL,
  `upd_by_id` bigint(20) DEFAULT NULL,
  `user_name` varchar(767) NOT NULL,
  `descr` varchar(4000) NOT NULL,
  `status` int(11) NOT NULL DEFAULT '0',
  `cred_store_id` bigint(20) DEFAULT NULL,
  `is_visible` INT(11) NOT NULL DEFAULT '1',
  `other_attributes` varchar(4000) DEFAULT NULL,
  `sync_source` varchar(4000) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `x_user_FK_added_by_id` (`added_by_id`),
  KEY `x_user_FK_upd_by_id` (`upd_by_id`),
  KEY `x_user_FK_cred_store_id` (`cred_store_id`),
  KEY `x_user_cr_time` (`create_time`),
  KEY `x_user_up_time` (`update_time`),
  CONSTRAINT x_user_UK_user_name UNIQUE(user_name(767)),
  CONSTRAINT `x_user_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_user_FK_cred_store_id` FOREIGN KEY (`cred_store_id`) REFERENCES `x_cred_store` (`id`),
  CONSTRAINT `x_user_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE `x_group_users` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `added_by_id` bigint(20) DEFAULT NULL,
  `upd_by_id` bigint(20) DEFAULT NULL,
  `group_name` varchar(740) NOT NULL,
  `p_group_id` bigint(20) DEFAULT NULL,
  `user_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `x_group_users_UK_uid_gname` (`user_id`, `group_name`),
  KEY `x_group_users_FK_added_by_id` (`added_by_id`),
  KEY `x_group_users_FK_upd_by_id` (`upd_by_id`),
  KEY `x_group_users_FK_p_group_id` (`p_group_id`),
  KEY `x_group_users_FK_user_id` (`user_id`),
  KEY `x_group_users_cr_time` (`create_time`),
  KEY `x_group_users_up_time` (`update_time`),
  CONSTRAINT `x_group_users_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_group_users_FK_p_group_id` FOREIGN KEY (`p_group_id`) REFERENCES `x_group` (`id`),
  CONSTRAINT `x_group_users_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_group_users_FK_user_id` FOREIGN KEY (`user_id`) REFERENCES `x_user` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE `x_policy_export_audit` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `added_by_id` bigint(20) DEFAULT NULL,
  `upd_by_id` bigint(20) DEFAULT NULL,
  `client_ip` varchar(255) NOT NULL,
  `agent_id` varchar(255) DEFAULT NULL,
  `req_epoch` bigint(20) NOT NULL,
  `last_updated` datetime DEFAULT NULL,
  `repository_name` varchar(1024) DEFAULT NULL,
  `exported_json` text,
  `http_ret_code` int(11) NOT NULL DEFAULT '0',
  `cluster_name` varchar(255) NULL DEFAULT NULL,
  `zone_name` varchar(255) DEFAULT NULL NULL,
  `policy_version` bigint(20) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `x_policy_export_audit_FK_added_by_id` (`added_by_id`),
  KEY `x_policy_export_audit_FK_upd_by_id` (`upd_by_id`),
  KEY `x_policy_export_audit_cr_time` (`create_time`),
  KEY `x_policy_export_audit_up_time` (`update_time`),
  CONSTRAINT `x_policy_export_audit_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_policy_export_audit_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE `x_resource` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `added_by_id` bigint(20) DEFAULT NULL,
  `upd_by_id` bigint(20) DEFAULT NULL,
  `res_name` varchar(4000) DEFAULT NULL,
  `descr` varchar(4000) DEFAULT NULL,
  `res_type` int(11) NOT NULL DEFAULT '0',
  `asset_id` bigint(20) NOT NULL,
  `parent_id` bigint(20) DEFAULT NULL,
  `parent_path` varchar(4000) DEFAULT NULL,
  `is_encrypt` int(11) NOT NULL DEFAULT '0',
  `is_recursive` int(11) NOT NULL DEFAULT '0',
  `res_group` varchar(1024) DEFAULT NULL,
  `res_dbs` text,
  `res_tables` text,
  `res_col_fams` text,
  `res_cols` text,
  `res_udfs` text,
  `res_status` int(11) NOT NULL DEFAULT '1',
  `table_type` int(11) NOT NULL DEFAULT '0',
  `col_type` int(11) NOT NULL DEFAULT '0',
  `policy_name` VARCHAR( 500 ) NULL DEFAULT NULL,
  `res_topologies` TEXT NULL DEFAULT NULL,
  `res_services` TEXT NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `x_resource_UK_policy_name` (`policy_name`),
  KEY `x_resource_FK_added_by_id` (`added_by_id`),
  KEY `x_resource_FK_upd_by_id` (`upd_by_id`),
  KEY `x_resource_FK_asset_id` (`asset_id`),
  KEY `x_resource_FK_parent_id` (`parent_id`),
  KEY `x_resource_cr_time` (`create_time`),
  KEY `x_resource_up_time` (`update_time`),
  CONSTRAINT `x_resource_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_resource_FK_asset_id` FOREIGN KEY (`asset_id`) REFERENCES `x_asset` (`id`),
  CONSTRAINT `x_resource_FK_parent_id` FOREIGN KEY (`parent_id`) REFERENCES `x_resource` (`id`),
  CONSTRAINT `x_resource_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE `x_trx_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `added_by_id` bigint(20) DEFAULT NULL,
  `upd_by_id` bigint(20) DEFAULT NULL,
  `class_type` int(11) NOT NULL DEFAULT '0',
  `object_id` bigint(20) DEFAULT NULL,
  `parent_object_id` bigint(20) DEFAULT NULL,
  `parent_object_class_type` int(11) NOT NULL DEFAULT '0',
  `parent_object_name` varchar(1024) DEFAULT NULL,
  `object_name` varchar(1024) DEFAULT NULL,
  `attr_name` varchar(255) DEFAULT NULL,
  `prev_val` MEDIUMTEXT NULL DEFAULT NULL,
  `new_val` MEDIUMTEXT NULL DEFAULT NULL,
  `trx_id` varchar(1024) DEFAULT NULL,
  `action` varchar(255) DEFAULT NULL,
  `sess_id` varchar(512) DEFAULT NULL,
  `req_id` varchar(30) DEFAULT NULL,
  `sess_type` varchar(30) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `x_trx_log_FK_added_by_id` (`added_by_id`),
  KEY `x_trx_log_FK_upd_by_id` (`upd_by_id`),
  KEY `x_trx_log_cr_time` (`create_time`),
  KEY `x_trx_log_up_time` (`update_time`),
  CONSTRAINT `x_trx_log_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_trx_log_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE `x_perm_map` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `added_by_id` bigint(20) DEFAULT NULL,
  `upd_by_id` bigint(20) DEFAULT NULL,
  `perm_group` varchar(1024) DEFAULT NULL,
  `res_id` bigint(20) DEFAULT NULL,
  `group_id` bigint(20) DEFAULT NULL,
  `user_id` bigint(20) DEFAULT NULL,
  `perm_for` int(11) NOT NULL DEFAULT '0',
  `perm_type` int(11) NOT NULL DEFAULT '0',
  `is_recursive` int(11) NOT NULL DEFAULT '0',
  `is_wild_card` tinyint(1) NOT NULL DEFAULT '1',
  `grant_revoke` tinyint(1) NOT NULL DEFAULT '1',
  `ip_address` TEXT NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `x_perm_map_FK_added_by_id` (`added_by_id`),
  KEY `x_perm_map_FK_upd_by_id` (`upd_by_id`),
  KEY `x_perm_map_FK_res_id` (`res_id`),
  KEY `x_perm_map_FK_group_id` (`group_id`),
  KEY `x_perm_map_FK_user_id` (`user_id`),
  KEY `x_perm_map_cr_time` (`create_time`),
  KEY `x_perm_map_up_time` (`update_time`),
  CONSTRAINT `x_perm_map_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_perm_map_FK_group_id` FOREIGN KEY (`group_id`) REFERENCES `x_group` (`id`),
  CONSTRAINT `x_perm_map_FK_res_id` FOREIGN KEY (`res_id`) REFERENCES `x_resource` (`id`),
  CONSTRAINT `x_perm_map_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_perm_map_FK_user_id` FOREIGN KEY (`user_id`) REFERENCES `x_user` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE `x_audit_map` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `added_by_id` bigint(20) DEFAULT NULL,
  `upd_by_id` bigint(20) DEFAULT NULL,
  `res_id` bigint(20) DEFAULT NULL,
  `group_id` bigint(20) DEFAULT NULL,
  `user_id` bigint(20) DEFAULT NULL,
  `audit_type` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `x_audit_map_FK_added_by_id` (`added_by_id`),
  KEY `x_audit_map_FK_upd_by_id` (`upd_by_id`),
  KEY `x_audit_map_FK_res_id` (`res_id`),
  KEY `x_audit_map_FK_group_id` (`group_id`),
  KEY `x_audit_map_FK_user_id` (`user_id`),
  KEY `x_audit_map_cr_time` (`create_time`),
  KEY `x_audit_map_up_time` (`update_time`),
  CONSTRAINT `x_audit_map_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_audit_map_FK_group_id` FOREIGN KEY (`group_id`) REFERENCES `x_group` (`id`),
  CONSTRAINT `x_audit_map_FK_res_id` FOREIGN KEY (`res_id`) REFERENCES `x_resource` (`id`),
  CONSTRAINT `x_audit_map_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`),
  CONSTRAINT `x_audit_map_FK_user_id` FOREIGN KEY (`user_id`) REFERENCES `x_user` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE `x_service_def` (
`id` bigint(20) NOT NULL AUTO_INCREMENT ,
`guid` varchar(1024) DEFAULT NULL,
`create_time` datetime DEFAULT NULL,
`update_time` datetime DEFAULT NULL,
`added_by_id` bigint(20) DEFAULT NULL,
`upd_by_id` bigint(20) DEFAULT NULL,
`version` bigint(20) DEFAULT NULL,
`name` varchar(1024) DEFAULT NULL,
`display_name` varchar(1024) DEFAULT NULL,
`impl_class_name` varchar(1024) DEFAULT NULL,
`label` varchar(1024) DEFAULT NULL,
`description` varchar(1024) DEFAULT NULL,
`rb_key_label` varchar(1024) DEFAULT NULL,
`rb_key_description` varchar(1024) DEFAULT NULL,
`is_enabled` tinyint DEFAULT 1,
`def_options` VARCHAR(1024) DEFAULT NULL NULL,
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
`display_name` varchar(255) DEFAULT NULL,
`policy_version` bigint(20) DEFAULT NULL,
`policy_update_time`datetime DEFAULT NULL,
`description` varchar(1024) DEFAULT NULL,
`is_enabled` tinyint(1) NOT NULL DEFAULT '0',
`tag_service` BIGINT DEFAULT NULL NULL,
`tag_version` BIGINT DEFAULT 0 NOT NULL,
`tag_update_time` DATETIME DEFAULT NULL NULL,
primary key (`id`),
UNIQUE KEY `X_service_name` (`name`),
KEY `x_service_added_by_id` (`added_by_id`),
KEY `x_service_upd_by_id` (`upd_by_id`),
KEY `x_service_cr_time` (`create_time`),
KEY `x_service_up_time` (`update_time`),
KEY `x_service_type` (`type`),
CONSTRAINT `x_service_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_service_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_service_FK_type` FOREIGN KEY (`type`) REFERENCES `x_service_def` (`id`),
CONSTRAINT `x_service_FK_tag_service` FOREIGN KEY (`tag_service`) REFERENCES `x_service` (`id`)
)ROW_FORMAT=DYNAMIC;

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
UNIQUE  KEY `x_ranger_global_state_UK_state_name`(`state_name`),
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
 CONSTRAINT `x_sz_ref_tag_service_FK_tag_srvc_id` FOREIGN KEY (`tag_srvc_id`) REFERENCES `x_service` (`id`),
 CONSTRAINT `x_sz_ref_tag_service_FK_tag_srvc_name` FOREIGN KEY (`tag_srvc_name`) REFERENCES `x_service` (`name`)
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

CREATE TABLE  `x_policy` (
`id` bigint(20) NOT NULL AUTO_INCREMENT ,
`guid` varchar(1024) DEFAULT NULL,
`create_time` datetime DEFAULT NULL,
`update_time` datetime DEFAULT NULL,
`added_by_id` bigint(20) DEFAULT NULL,
`upd_by_id` bigint(20) DEFAULT NULL,
`version` bigint(20) DEFAULT NULL,
`service` bigint(20) NOT NULL,
`name` varchar(512) NOT NULL,
`policy_type` int(11) DEFAULT 0,
`description` varchar(1024) DEFAULT NULL,
`resource_signature` varchar(128) DEFAULT NULL,
`is_enabled` tinyint(1) NOT NULL DEFAULT '0',
`is_audit_enabled` tinyint(1) NOT NULL DEFAULT '0',
`policy_options` varchar(4000) NULL DEFAULT NULL,
`policy_priority` int NOT NULL DEFAULT '0',
`policy_text` MEDIUMTEXT NULL DEFAULT NULL,
`zone_id` bigint(20) NOT NULL DEFAULT '1',
primary key (`id`),
KEY `x_policy_added_by_id` (`added_by_id`),
KEY `x_policy_upd_by_id` (`upd_by_id`),
KEY `x_policy_cr_time` (`create_time`),
KEY `x_policy_up_time` (`update_time`),
KEY `x_policy_service` (`service`),
KEY `x_policy_resource_signature` (`resource_signature`),
UNIQUE KEY `x_policy_UK_name_service_zone` (`name`(180),`service`, `zone_id`),
UNIQUE KEY `x_policy_UK_guid_service_zone` (`guid`(180),`service`, `zone_id`),
UNIQUE KEY `x_policy_UK_service_signature` (`service`,`resource_signature`),
CONSTRAINT `x_policy_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_policy_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_policy_FK_service` FOREIGN KEY (`service`) REFERENCES `x_service` (`id`),
CONSTRAINT `x_policy_FK_zone_id` FOREIGN KEY (`zone_id`) REFERENCES `x_security_zone` (`id`)
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
`datamask_options` varchar(1024) NULL DEFAULT NULL,
`rowfilter_options` varchar(1024) NULL DEFAULT NULL,
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
`datamask_options` varchar(1024) NULL DEFAULT NULL,
`rowfilter_options` varchar(1024) NULL DEFAULT NULL,
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
`item_type` INT DEFAULT 0 NOT NULL,
`is_enabled` TINYINT(1) NOT NULL DEFAULT '1',
`comments` VARCHAR(255) DEFAULT NULL NULL,
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
`content` MEDIUMTEXT NOT NULL,
primary key (`id`),
KEY `x_data_hist_idx_objid_objclstype` (`obj_id`,`obj_class_type`)
)ROW_FORMAT=DYNAMIC;

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
`tag_attrs_def_text` MEDIUMTEXT NULL DEFAULT NULL,
PRIMARY KEY (`id`),
UNIQUE KEY `x_tag_def_UK_guid` (`guid`),
UNIQUE KEY `x_tag_def_UK_name` (`name`),
KEY `x_tag_def_IDX_added_by_id` (`added_by_id`),
KEY `x_tag_def_IDX_upd_by_id` (`upd_by_id`),
CONSTRAINT `x_tag_def_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_tag_def_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE IF NOT EXISTS `x_tag` (
`id` BIGINT(20) NOT NULL AUTO_INCREMENT,
`guid` VARCHAR(64) NOT NULL,
`create_time` DATETIME NULL DEFAULT NULL,
`update_time` DATETIME NULL DEFAULT NULL,
`added_by_id` BIGINT(20) NULL DEFAULT NULL,
`upd_by_id` BIGINT(20) NULL DEFAULT NULL,
`version` BIGINT(20) NULL DEFAULT NULL,
`type` BIGINT(20) NOT NULL,
`owned_by` SMALLINT DEFAULT 0 NOT NULL,
`policy_options` varchar(4000) NULL DEFAULT NULL,
`tag_attrs_text` MEDIUMTEXT NULL DEFAULT NULL,
PRIMARY KEY (`id`),
UNIQUE KEY `x_tag_UK_guid` (`guid`),
KEY `x_tag_IDX_type` (`type`),
KEY `x_tag_IDX_added_by_id` (`added_by_id`),
KEY `x_tag_IDX_upd_by_id` (`upd_by_id`),
CONSTRAINT `x_tag_FK_type` FOREIGN KEY (`type`) REFERENCES `x_tag_def` (`id`),
CONSTRAINT `x_tag_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_tag_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;

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
`service_resource_elements_text` MEDIUMTEXT NULL DEFAULT NULL,
`tags_text` MEDIUMTEXT NULL DEFAULT NULL,
PRIMARY KEY (`id`),
UNIQUE KEY `x_service_res_UK_guid` (`guid`),
KEY `x_service_res_IDX_added_by_id` (`added_by_id`),
KEY `x_service_res_IDX_upd_by_id` (`upd_by_id`),
CONSTRAINT `x_service_res_FK_service_id` FOREIGN KEY (`service_id`) REFERENCES `x_service` (`id`),
CONSTRAINT `x_service_res_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_service_res_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;

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

CREATE TABLE `x_datamask_type_def` (
`id` bigint(20) NOT NULL AUTO_INCREMENT ,
`guid` varchar(64) NULL DEFAULT NULL,
`create_time` datetime NULL DEFAULT NULL,
`update_time` datetime NULL DEFAULT NULL,
`added_by_id` bigint(20) NULL DEFAULT NULL,
`upd_by_id` bigint(20) NULL DEFAULT NULL,
`def_id` bigint(20) NOT NULL,
`item_id` bigint(20) NOT NULL,
`name` varchar(1024) NOT NULL,
`label` varchar(1024) NOT NULL,
`description` varchar(1024) NULL DEFAULT NULL,
`transformer` varchar(1024) NULL DEFAULT NULL,
`datamask_options` varchar(1024) NULL DEFAULT NULL,
`rb_key_label` varchar(1024) NULL DEFAULT NULL,
`rb_key_description` varchar(1024) DEFAULT NULL,
`sort_order` int DEFAULT 0,
primary key (`id`),
CONSTRAINT `x_datamask_type_def_FK_def_id` FOREIGN KEY (`def_id`) REFERENCES `x_service_def` (`id`) ,
CONSTRAINT `x_datamask_type_def_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_datamask_type_def_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE `x_policy_item_datamask` (
`id` bigint(20) NOT NULL AUTO_INCREMENT ,
`guid` varchar(64) NULL DEFAULT NULL,
`create_time` datetime NULL DEFAULT NULL,
`update_time` datetime NULL DEFAULT NULL,
`added_by_id` bigint(20) NULL DEFAULT NULL,
`upd_by_id` bigint(20) NULL DEFAULT NULL,
`policy_item_id` bigint(20) NOT NULL,
`type` bigint(20) NOT NULL,
`condition_expr` varchar(1024) NULL DEFAULT NULL,
`value_expr` varchar(1024) NULL DEFAULT NULL,
primary key (id),
CONSTRAINT `x_policy_item_datamask_FK_policy_item_id` FOREIGN KEY (`policy_item_id`) REFERENCES `x_policy_item` (`id`) ,
CONSTRAINT `x_policy_item_datamask_FK_type` FOREIGN KEY (`type`) REFERENCES `x_datamask_type_def` (`id`),
CONSTRAINT `x_policy_item_datamask_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_policy_item_datamask_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE `x_policy_item_rowfilter` (
`id` bigint(20) NOT NULL AUTO_INCREMENT ,
`guid` varchar(64) NULL DEFAULT NULL,
`create_time` datetime NULL DEFAULT NULL,
`update_time` datetime NULL DEFAULT NULL,
`added_by_id` bigint(20) NULL DEFAULT NULL,
`upd_by_id` bigint(20) NULL DEFAULT NULL,
`policy_item_id` bigint(20) NOT NULL,
`filter_expr` varchar(1024) NULL DEFAULT NULL,
primary key (id),
CONSTRAINT `x_policy_item_rowfilter_FK_policy_item_id` FOREIGN KEY (`policy_item_id`) REFERENCES `x_policy_item` (`id`) ,
CONSTRAINT `x_policy_item_rowfilter_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_policy_item_rowfilter_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE `x_service_version_info` (
`id` bigint(20) NOT NULL AUTO_INCREMENT,
`service_id` bigint(20) NOT NULL,
`policy_version` bigint(20) NOT NULL DEFAULT 0,
`policy_update_time` datetime NULL DEFAULT NULL,
`tag_version` bigint(20) NOT NULL DEFAULT 0,
`tag_update_time` datetime NULL DEFAULT NULL,
`role_version` bigint(20) NOT NULL DEFAULT 0,
`role_update_time` datetime NULL DEFAULT NULL,
`version` bigint(20) NOT NULL DEFAULT '1',
primary key (`id`),
CONSTRAINT `x_service_version_info_FK_service_id` FOREIGN KEY (`service_id`) REFERENCES `x_service` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE IF NOT EXISTS `x_plugin_info`(
`id` bigint(20) NOT NULL AUTO_INCREMENT,
`create_time` datetime NULL DEFAULT NULL,
`update_time` datetime NULL DEFAULT NULL,
`service_name` varchar(255) NOT  NULL,
`app_type` varchar(128) NOT NULL,
`host_name` varchar(255) NOT NULL,
`ip_address` varchar(64) NOT NULL,
`info` varchar(1024) NOT NULL,
 PRIMARY KEY (`id`),
 UNIQUE KEY `x_plugin_info_UK`(`service_name`, `host_name`, `app_type`),
 KEY `x_plugin_info_IDX_service_name`(`service_name`),
 KEY `x_plugin_info_IDX_host_name`(`host_name`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE  `x_policy_label` (
`id` bigint(20) NOT NULL AUTO_INCREMENT ,
`guid` varchar(1024) DEFAULT NULL,
`create_time` datetime DEFAULT NULL,
`update_time` datetime DEFAULT NULL,
`added_by_id` bigint(20) DEFAULT NULL,
`upd_by_id` bigint(20) DEFAULT NULL,
`label_name` varchar(512) DEFAULT NULL,
primary key (`id`),
UNIQUE KEY `x_policy_label_UK_label_name` (`label_name`),
KEY `x_policy_label_added_by_id` (`added_by_id`),
KEY `x_policy_label_upd_by_id` (`upd_by_id`),
KEY `x_policy_label_cr_time` (`create_time`),
KEY `x_policy_label_up_time` (`update_time`),
KEY `x_policy_label_name` (`label_name`),
CONSTRAINT `x_policy_label_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_policy_label_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE TABLE  `x_policy_label_map` (
`id` bigint(20) NOT NULL AUTO_INCREMENT ,
`guid` varchar(1024) DEFAULT NULL,
`create_time` datetime DEFAULT NULL,
`update_time` datetime DEFAULT NULL,
`added_by_id` bigint(20) DEFAULT NULL,
`upd_by_id` bigint(20) DEFAULT NULL,
`policy_id` bigint(20) DEFAULT NULL,
`policy_label_id` bigint(20) DEFAULT NULL,
primary key (`id`),
UNIQUE INDEX `x_policy_label_map_pid_plid` (`policy_id`, `policy_label_id`),
KEY `x_policy_label_map_added_by_id` (`added_by_id`),
KEY `x_policy_label_map_upd_by_id` (`upd_by_id`),
KEY `x_policy_label_map_cr_time` (`create_time`),
KEY `x_policy_label_map_up_time` (`update_time`),
CONSTRAINT `x_policy_label_map_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_policy_label_map_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_policy_label_map_FK_policy_id` FOREIGN KEY (`policy_id`) REFERENCES `x_policy` (`id`),
CONSTRAINT `x_policy_label_map_FK_policy_label_id` FOREIGN KEY (`policy_label_id`) REFERENCES `x_policy_label` (`id`)
)ROW_FORMAT=DYNAMIC;

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
`sync_source_info` MEDIUMTEXT NOT NULL,
`session_id` varchar(255) DEFAULT NULL,
 PRIMARY KEY (`id`),
 KEY `x_ugsync_audit_info_etime`(`event_time`),
 KEY `x_ugsync_audit_info_sync_src`(`sync_source`),
 KEY `x_ugsync_audit_info_uname`(`user_name`)
)ROW_FORMAT=DYNAMIC;

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

CREATE TABLE IF NOT EXISTS `x_security_zone_ref_resource`(
`id` bigint(20) NOT NULL AUTO_INCREMENT,
`create_time` datetime NULL DEFAULT NULL,
`update_time` datetime NULL DEFAULT NULL,
`added_by_id` bigint(20) NULL DEFAULT NULL,
`upd_by_id` bigint(20) NULL DEFAULT NULL,
`zone_id` bigint(20) NOT NULL,
`resource_def_id` bigint(20) NOT NULL,
`resource_name` varchar(255) DEFAULT NULL,
 PRIMARY KEY (`id`),
 CONSTRAINT `x_sz_ref_resource_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
 CONSTRAINT `x_sz_ref_resource_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`),
 CONSTRAINT `x_sz_ref_resource_FK_zone_id` FOREIGN KEY (`zone_id`) REFERENCES `x_security_zone` (`id`),
 CONSTRAINT `x_sz_ref_resource_FK_resource_def_id` FOREIGN KEY (`resource_def_id`) REFERENCES `x_resource_def` (`id`)
) ROW_FORMAT=DYNAMIC;

CREATE TABLE IF NOT EXISTS `x_policy_change_log` (
`id` bigint(20) NOT NULL AUTO_INCREMENT,
`create_time` datetime NULL DEFAULT NULL,
`service_id` bigint(20) NOT NULL,
`change_type` int(11) NOT NULL,
`policy_version` bigint(20) NOT NULL DEFAULT '0',
`service_type` varchar(256) NULL DEFAULT NULL,
`policy_type` int(11) NULL DEFAULT NULL,
`zone_name` varchar(256) NULL DEFAULT NULL,
`policy_id` bigint(20) NULL DEFAULT NULL,
`policy_guid` varchar(1024) NULL DEFAULT NULL,
primary key (`id`)
) ROW_FORMAT=DYNAMIC;

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

CREATE TABLE IF NOT EXISTS `x_tag_change_log` (
`id` bigint(20) NOT NULL AUTO_INCREMENT,
`create_time` datetime NULL DEFAULT NULL,
`service_id` bigint(20) NOT NULL,
`change_type` int(11) NOT NULL,
`service_tags_version` bigint(20) NOT NULL DEFAULT '0',
`service_resource_id` bigint(20) NULL DEFAULT NULL,
`tag_id` bigint(20) NULL DEFAULT NULL,
primary key (`id`)
) ROW_FORMAT=DYNAMIC;

CREATE UNIQUE INDEX x_service_resource_IDX_svc_id_resource_signature ON x_service_resource(service_id, resource_signature);
CREATE INDEX x_tag_change_log_IDX_service_id ON x_tag_change_log(service_id);
CREATE INDEX x_tag_change_log_IDX_tag_version ON x_tag_change_log(service_tags_version);
CREATE UNIQUE INDEX x_tag_change_log_uk_service_id_service_tags_version ON x_tag_change_log(service_id, service_tags_version);
CREATE INDEX x_policy_change_log_IDX_service_id ON x_policy_change_log(service_id);
CREATE INDEX x_policy_change_log_IDX_policy_version ON x_policy_change_log(policy_version);
CREATE UNIQUE INDEX x_policy_change_log_uk_service_id_policy_version ON x_policy_change_log(service_id, policy_version);
CREATE INDEX x_service_config_def_IDX_def_id ON x_service_config_def(def_id);
CREATE INDEX x_resource_def_IDX_def_id ON x_resource_def(def_id);
CREATE INDEX x_access_type_def_IDX_def_id ON x_access_type_def(def_id);
CREATE INDEX x_access_type_def_IDX_grants_atd_id ON x_access_type_def_grants(atd_id);
CREATE INDEX x_context_enricher_def_IDX_def_id ON x_context_enricher_def(def_id);
CREATE INDEX x_enum_def_IDX_def_id ON x_enum_def(def_id);
CREATE INDEX x_enum_element_def_IDX_enum_def_id ON x_enum_element_def(enum_def_id);
CREATE INDEX x_service_config_map_IDX_service ON x_service_config_map(service);
CREATE INDEX x_policy_resource_IDX_policy_id ON x_policy_resource(policy_id);
CREATE INDEX x_policy_resource_IDX_res_def_id ON x_policy_resource(res_def_id);
CREATE INDEX x_policy_resource_map_IDX_resource_id ON x_policy_resource_map(resource_id);
CREATE INDEX x_policy_item_IDX_policy_id ON x_policy_item(policy_id);
CREATE INDEX x_policy_item_access_IDX_policy_item_id ON x_policy_item_access(policy_item_id);
CREATE INDEX x_policy_item_access_IDX_type ON x_policy_item_access(type);
CREATE INDEX x_policy_item_condition_IDX_policy_item_id ON x_policy_item_condition(policy_item_id);
CREATE INDEX x_policy_item_condition_IDX_type ON x_policy_item_condition(type);
CREATE INDEX x_policy_item_user_perm_IDX_policy_item_id ON x_policy_item_user_perm(policy_item_id);
CREATE INDEX x_policy_item_user_perm_IDX_user_id ON x_policy_item_user_perm(user_id);
CREATE INDEX x_policy_item_group_perm_IDX_policy_item_id ON x_policy_item_group_perm(policy_item_id);
CREATE INDEX x_policy_item_group_perm_IDX_group_id ON x_policy_item_group_perm(group_id);
CREATE INDEX x_service_resource_IDX_service_id ON x_service_resource(service_id);
CREATE INDEX x_datamask_type_def_IDX_def_id ON x_datamask_type_def(def_id);
CREATE INDEX x_policy_item_datamask_IDX_policy_item_id ON x_policy_item_datamask(policy_item_id);
CREATE INDEX x_policy_item_rowfilter_IDX_policy_item_id ON x_policy_item_rowfilter(policy_item_id);
CREATE INDEX x_service_version_info_IDX_service_id ON x_service_version_info(service_id);
CREATE INDEX x_policy_label_label_id ON x_policy_label(id);
CREATE INDEX x_policy_label_label_name ON x_policy_label(label_name);
CREATE INDEX x_policy_label_label_map_id ON x_policy_label_map(id);

CREATE VIEW vx_trx_log AS select x_trx_log.id AS id,x_trx_log.create_time AS create_time,x_trx_log.update_time AS update_time,x_trx_log.added_by_id AS added_by_id,x_trx_log.upd_by_id AS upd_by_id,x_trx_log.class_type AS class_type,x_trx_log.object_id AS object_id,x_trx_log.parent_object_id AS parent_object_id,x_trx_log.parent_object_class_type AS parent_object_class_type,x_trx_log.attr_name AS attr_name,x_trx_log.parent_object_name AS parent_object_name,x_trx_log.object_name AS object_name,x_trx_log.prev_val AS prev_val,x_trx_log.new_val AS new_val,x_trx_log.trx_id AS trx_id,x_trx_log.action AS action,x_trx_log.sess_id AS sess_id,x_trx_log.req_id AS req_id,x_trx_log.sess_type AS sess_type from x_trx_log  where id in(select min(x_trx_log.id) from x_trx_log group by x_trx_log.trx_id);

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

CREATE TABLE `x_rms_service_resource` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `guid` varchar(64) NOT NULL,
  `create_time` timestamp NULL DEFAULT NULL,
  `update_time` timestamp NULL DEFAULT NULL,
  `added_by_id` bigint(20) DEFAULT NULL,
  `upd_by_id` bigint(20) DEFAULT NULL,
  `version` bigint(20) DEFAULT NULL,
  `service_id` bigint(20) NOT NULL,
  `resource_signature` varchar(128) DEFAULT NULL,
  `is_enabled` tinyint(1) NOT NULL DEFAULT '1',
  `service_resource_elements_text` text,
  PRIMARY KEY (`id`),
  UNIQUE KEY `x_rms_service_res_UK_guid` (`guid`),
  CONSTRAINT `x_rms_service_res_FK_service_id` FOREIGN KEY (`service_id`) REFERENCES `x_service` (`id`)
);
CREATE INDEX x_rms_service_resource_IDX_service_id ON x_rms_service_resource(service_id);
CREATE INDEX x_rms_service_resource_IDX_resource_signature ON x_rms_service_resource(resource_signature);

CREATE TABLE `x_rms_notification` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `hms_name` varchar(128) DEFAULT NULL,
  `notification_id` bigint(20) DEFAULT NULL,
  `change_timestamp` timestamp NULL DEFAULT NULL,
  `change_type` varchar(64) DEFAULT NULL,
  `hl_resource_id` bigint(20) DEFAULT NULL,
  `hl_service_id` bigint(20) DEFAULT NULL,
  `ll_resource_id` bigint(20) DEFAULT NULL,
  `ll_service_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `x_rms_notification_FK_hl_service_id` FOREIGN KEY (`hl_service_id`) REFERENCES `x_service` (`id`),
  CONSTRAINT `x_rms_notification_FK_ll_service_id` FOREIGN KEY (`ll_service_id`) REFERENCES `x_service` (`id`)
);

CREATE INDEX x_rms_notification_IDX_notification_id ON x_rms_notification(notification_id);
CREATE INDEX x_rms_notification_IDX_hms_name_notification_id ON x_rms_notification(hms_name, notification_id);
CREATE INDEX x_rms_notification_IDX_hl_service_id ON x_rms_notification(hl_service_id);
CREATE INDEX x_rms_notification_IDX_ll_service_id ON x_rms_notification(ll_service_id);


CREATE TABLE `x_rms_resource_mapping` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `change_timestamp` timestamp NULL DEFAULT NULL,
  `hl_resource_id` bigint(20) NOT NULL,
  `ll_resource_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `x_rms_res_map_UK_hl_res_id_ll_res_id` (`hl_resource_id`,`ll_resource_id`),
  CONSTRAINT `x_rms_res_map_FK_hl_res_id` FOREIGN KEY (`hl_resource_id`) REFERENCES `x_rms_service_resource` (`id`),
  CONSTRAINT `x_rms_res_map_FK_ll_res_id` FOREIGN KEY (`ll_resource_id`) REFERENCES `x_rms_service_resource` (`id`)
);

CREATE INDEX x_rms_resource_mapping_IDX_hl_resource_id ON x_rms_resource_mapping(hl_resource_id);
CREATE INDEX x_rms_resource_mapping_IDX_ll_resource_id ON x_rms_resource_mapping(ll_resource_id);

CREATE TABLE `x_rms_mapping_provider` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `change_timestamp` timestamp NULL DEFAULT NULL,
  `name` varchar(128) NOT NULL,
  `last_known_version` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `x_rms_mapping_provider_UK_name` (`name`)
);


DELIMITER $$
DROP PROCEDURE if exists insertRangerPrerequisiteEntries $$
CREATE PROCEDURE `insertRangerPrerequisiteEntries`()
BEGIN
DECLARE adminID bigint;
DECLARE keyadminID bigint;
DECLARE rangerusersyncID bigint;
DECLARE rangertagsyncID bigint;
DECLARE moduleIdReports bigint;
DECLARE moduleIdResourceBasedPolicies bigint;
DECLARE moduleIdAudit bigint;
DECLARE moduleIdUG bigint;
DECLARE moduleIdTagBasedPolicies bigint;
DECLARE moduleIdKeyManager bigint;
DECLARE moduleIdSecurityZone bigint;

INSERT INTO x_portal_user(create_time,update_time,added_by_id,upd_by_id,first_name,last_name,pub_scr_name,login_id,password,email,status,user_src,notes) VALUES (UTC_TIMESTAMP(),UTC_TIMESTAMP(),NULL,NULL,'Admin','','Admin','admin','ceb4f32325eda6142bd65215f4c0f371','',1,0,NULL);
INSERT INTO x_portal_user(create_time,update_time,added_by_id,upd_by_id,first_name,last_name,pub_scr_name,login_id,password,email,status,user_src,notes) VALUES (UTC_TIMESTAMP(),UTC_TIMESTAMP(),NULL,NULL,'rangerusersync','','rangerusersync','rangerusersync','70b8374d3dfe0325aaa5002a688c7e3b','rangerusersync',1,0,NULL);
INSERT INTO x_portal_user(create_time,update_time,added_by_id,upd_by_id,first_name,last_name,pub_scr_name,login_id,password,email,status,user_src,notes) VALUES (UTC_TIMESTAMP(),UTC_TIMESTAMP(),NULL,NULL,'keyadmin','','keyadmin','keyadmin','a05f34d2dce2b4688fa82e82a89ba958','keyadmin',1,0,NULL);
INSERT INTO x_portal_user(create_time,update_time,added_by_id,upd_by_id,first_name,last_name,pub_scr_name,login_id,password,email,status,user_src,notes) VALUES (UTC_TIMESTAMP(),UTC_TIMESTAMP(),NULL,NULL,'rangertagsync','','rangertagsync','rangertagsync','f5820e1229418dcf2575908f2c493da5','rangertagsync',1,0,NULL);

call getXportalUIdByLoginId('admin', adminID);
call getXportalUIdByLoginId('keyadmin', keyadminID);
call getXportalUIdByLoginId('rangerusersync', rangerusersyncID);
call getXportalUIdByLoginId('rangertagsync', rangertagsyncID);

INSERT INTO `x_modules_master` (`create_time`,`update_time`,`added_by_id`,`upd_by_id`,`module`,`url`) VALUES (UTC_TIMESTAMP(),UTC_TIMESTAMP(),adminID,adminID,'Resource Based Policies',''),(UTC_TIMESTAMP(),UTC_TIMESTAMP(),adminID,adminID,'Users/Groups',''),(UTC_TIMESTAMP(),UTC_TIMESTAMP(),adminID,adminID,'Reports',''),(UTC_TIMESTAMP(),UTC_TIMESTAMP(),adminID,adminID,'Audit',''),(UTC_TIMESTAMP(),UTC_TIMESTAMP(),adminID,adminID,'Key Manager',''),(UTC_TIMESTAMP(),UTC_TIMESTAMP(),adminID,adminID,'Tag Based Policies','');
INSERT INTO `x_modules_master` (`create_time`,`update_time`,`added_by_id`,`upd_by_id`,`module`,`url`) VALUES (UTC_TIMESTAMP(),UTC_TIMESTAMP(),adminID,adminID,'Security Zone','');

call getModulesIdByName('Reports', moduleIdReports);
call getModulesIdByName('Resource Based Policies', moduleIdResourceBasedPolicies);
call getModulesIdByName('Audit', moduleIdAudit);
call getModulesIdByName('Users/Groups', moduleIdUG);
call getModulesIdByName('Tag Based Policies', moduleIdTagBasedPolicies);
call getModulesIdByName('Key Manager', moduleIdKeyManager);
call getModulesIdByName('Security Zone', moduleIdSecurityZone);

INSERT INTO x_portal_user_role(create_time,update_time,added_by_id,upd_by_id,user_id,user_role,status) VALUES (UTC_TIMESTAMP(),UTC_TIMESTAMP(),NULL,NULL,adminID,'ROLE_SYS_ADMIN',1);
INSERT INTO x_group (ADDED_BY_ID, CREATE_TIME, DESCR, GROUP_SRC, GROUP_TYPE, GROUP_NAME, STATUS, UPDATE_TIME, UPD_BY_ID) VALUES (adminID, UTC_TIMESTAMP(), 'public group', 0, 0, 'public', 0, UTC_TIMESTAMP(), adminID);
INSERT INTO x_portal_user_role(create_time,update_time,added_by_id,upd_by_id,user_id,user_role,status) VALUES (UTC_TIMESTAMP(),UTC_TIMESTAMP(),NULL,NULL,rangerusersyncID,'ROLE_SYS_ADMIN',1);
INSERT INTO x_user(create_time,update_time,added_by_id,upd_by_id,user_name,descr,status) values (UTC_TIMESTAMP(), UTC_TIMESTAMP(),NULL,NULL,'admin','Administrator',0);
INSERT INTO x_user(create_time,update_time,added_by_id,upd_by_id,user_name,descr,status) values (UTC_TIMESTAMP(), UTC_TIMESTAMP(),NULL,NULL,'rangerusersync','rangerusersync',0);
INSERT INTO x_portal_user_role(create_time,update_time,added_by_id,upd_by_id,user_id,user_role,status) VALUES (UTC_TIMESTAMP(),UTC_TIMESTAMP(),NULL,NULL,keyadminID,'ROLE_KEY_ADMIN',1);
INSERT INTO x_user(create_time,update_time,added_by_id,upd_by_id,user_name,descr,status) values (UTC_TIMESTAMP(), UTC_TIMESTAMP(),NULL,NULL,'keyadmin','keyadmin',0);
INSERT INTO x_portal_user_role(create_time,update_time,added_by_id,upd_by_id,user_id,user_role,status) VALUES (UTC_TIMESTAMP(),UTC_TIMESTAMP(),NULL,NULL,rangertagsyncID,'ROLE_SYS_ADMIN',1);
INSERT INTO x_user(create_time,update_time,added_by_id,upd_by_id,user_name,descr,status) values (UTC_TIMESTAMP(), UTC_TIMESTAMP(),NULL,NULL,'rangertagsync','rangertagsync',0);

INSERT INTO x_security_zone(id, create_time, update_time, added_by_id, upd_by_id, version, name, jsonData, description) VALUES (1, UTC_TIMESTAMP(),UTC_TIMESTAMP(), adminID, adminID, 1, ' ', '', 'Unzoned zone');

INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (adminID,moduleIdReports,UTC_TIMESTAMP(),UTC_TIMESTAMP(),adminID,adminID,1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (adminID,moduleIdResourceBasedPolicies,UTC_TIMESTAMP(),UTC_TIMESTAMP(),adminID,adminID,1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (adminID,moduleIdAudit,UTC_TIMESTAMP(),UTC_TIMESTAMP(),adminID,adminID,1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (adminID,moduleIdUG,UTC_TIMESTAMP(),UTC_TIMESTAMP(),adminID,adminID,1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (adminID,moduleIdTagBasedPolicies,UTC_TIMESTAMP(),UTC_TIMESTAMP(),adminID,adminID,1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (rangerusersyncID,moduleIdReports,UTC_TIMESTAMP(),UTC_TIMESTAMP(),adminID,adminID,1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (rangerusersyncID,moduleIdResourceBasedPolicies,UTC_TIMESTAMP(),UTC_TIMESTAMP(),adminID,adminID,1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (rangerusersyncID,moduleIdAudit,UTC_TIMESTAMP(),UTC_TIMESTAMP(),adminID,adminID,1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (rangerusersyncID,moduleIdUG,UTC_TIMESTAMP(),UTC_TIMESTAMP(),adminID,adminID,1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (rangerusersyncID,moduleIdTagBasedPolicies,UTC_TIMESTAMP(),UTC_TIMESTAMP(),adminID,adminID,1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (keyadminID,moduleIdKeyManager,UTC_TIMESTAMP(),UTC_TIMESTAMP(),adminID,adminID,1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (keyadminID,moduleIdReports,UTC_TIMESTAMP(),UTC_TIMESTAMP(),adminID,adminID,1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (keyadminID,moduleIdResourceBasedPolicies,UTC_TIMESTAMP(),UTC_TIMESTAMP(),adminID,adminID,1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (rangertagsyncID,moduleIdReports,UTC_TIMESTAMP(),UTC_TIMESTAMP(),adminID,adminID,1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (rangertagsyncID,moduleIdResourceBasedPolicies,UTC_TIMESTAMP(),UTC_TIMESTAMP(),adminID,adminID,1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (rangertagsyncID,moduleIdAudit,UTC_TIMESTAMP(),UTC_TIMESTAMP(),adminID,adminID,1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (rangertagsyncID,moduleIdUG,UTC_TIMESTAMP(),UTC_TIMESTAMP(),adminID,adminID,1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (rangertagsyncID,moduleIdTagBasedPolicies,UTC_TIMESTAMP(),UTC_TIMESTAMP(),adminID,adminID,1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (keyadminID,moduleIdUG,UTC_TIMESTAMP(),UTC_TIMESTAMP(),adminID,adminID,1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (keyadminID,moduleIdAudit,UTC_TIMESTAMP(),UTC_TIMESTAMP(),adminID,adminID,1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (adminID,moduleIdSecurityZone,UTC_TIMESTAMP(),UTC_TIMESTAMP(),adminID,adminID,1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (rangerusersyncID,moduleIdSecurityZone,UTC_TIMESTAMP(),UTC_TIMESTAMP(),adminID,adminID,1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (rangertagsyncID,moduleIdSecurityZone,UTC_TIMESTAMP(),UTC_TIMESTAMP(),adminID,adminID,1);

INSERT INTO x_ranger_global_state (create_time,update_time,added_by_id,upd_by_id,version,state_name,app_data) VALUES (UTC_TIMESTAMP(),UTC_TIMESTAMP(),adminID,adminID,1,'RangerRole','{"Version":"1"}');
INSERT INTO x_ranger_global_state (create_time,update_time,added_by_id,upd_by_id,version,state_name,app_data) VALUES (UTC_TIMESTAMP(),UTC_TIMESTAMP(),adminID,adminID,1,'RangerUserStore','{"Version":"1"}');
INSERT INTO x_ranger_global_state (create_time,update_time,added_by_id,upd_by_id,version,state_name,app_data) VALUES (UTC_TIMESTAMP(),UTC_TIMESTAMP(),adminID,adminID,1,'RangerSecurityZone','{"Version":"1"}');

END $$
DELIMITER ;
call insertRangerPrerequisiteEntries();

INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('CORE_DB_SCHEMA',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('001',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('002',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('003',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('004',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('005',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('006',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('007',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('008',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('009',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('010',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('012',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('013',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('014',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('016',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('018',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('019',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('020',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('021',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('022',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('023',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('024',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('025',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('026',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('027',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('028',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('029',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('030',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('031',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('032',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('033',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('034',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('035',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('036',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('037',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('038',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('039',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('040',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('041',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('042',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('043',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('044',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('045',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('046',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('047',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('048',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('049',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('050',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('051',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('052',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('054',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('055',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('056',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('057',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('058',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('059',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('DB_PATCHES',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');

INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10001',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10002',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10003',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10004',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10005',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10006',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10007',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10008',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10009',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10010',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10011',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10012',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10013',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10014',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10015',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10016',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10017',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10019',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10020',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10025',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10026',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10027',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10028',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10030',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10033',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10034',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10035',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10036',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10037',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10038',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10040',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10041',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10043',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10044',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10045',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10046',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10047',UTC_TIMESTAMP(),'Ranger 2.2.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10049',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10050',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10052',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10053',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10054',UTC_TIMESTAMP(),'Ranger 3.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10055',UTC_TIMESTAMP(),'Ranger 3.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10056',UTC_TIMESTAMP(),'Ranger 3.0.0',UTC_TIMESTAMP(),'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('JAVA_PATCHES',UTC_TIMESTAMP(),'Ranger 1.0.0',UTC_TIMESTAMP(),'localhost','Y');
