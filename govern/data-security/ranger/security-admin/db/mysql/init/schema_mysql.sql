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


SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL';

drop table if exists x_db_base;
create table x_db_base (
	id BIGINT   NOT NULL AUTO_INCREMENT,
	create_time DATETIME  ,
	update_time DATETIME  ,
	added_by_id BIGINT  ,
	upd_by_id BIGINT  ,
	PRIMARY KEY(id)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

drop table if exists x_auth_sess;
create table x_auth_sess (
	id BIGINT   NOT NULL AUTO_INCREMENT,
	create_time DATETIME  ,
	update_time DATETIME  ,
	added_by_id BIGINT  ,
	upd_by_id BIGINT  ,
	login_id VARCHAR  (767)  NOT NULL,
	user_id BIGINT  ,
	ext_sess_id VARCHAR  (512) ,
	auth_time DATETIME   NOT NULL,
	auth_status INT   NOT NULL DEFAULT 0,
	auth_type INT   NOT NULL DEFAULT 0,
	auth_provider INT   NOT NULL DEFAULT 0,
	device_type INT   NOT NULL DEFAULT 0,
	req_ip VARCHAR  (48)  NOT NULL,
	req_ua VARCHAR  (1024) ,
	PRIMARY KEY(id)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

drop table if exists x_portal_user;
create table x_portal_user (
	id BIGINT   NOT NULL AUTO_INCREMENT,
	create_time DATETIME  ,
	update_time DATETIME  ,
	added_by_id BIGINT  ,
	upd_by_id BIGINT  ,
	first_name VARCHAR  (1022) ,
	last_name VARCHAR  (1022) ,
	pub_scr_name VARCHAR  (2048) ,
	login_id VARCHAR  (767) ,
	password VARCHAR  (512)  NOT NULL,
	email VARCHAR  (512) ,
	status INT   NOT NULL DEFAULT 0,
	user_src INT   NOT NULL DEFAULT 0,
	notes VARCHAR  (4000) ,
	PRIMARY KEY(id)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

drop table if exists x_portal_user_role;
create table x_portal_user_role (
	id BIGINT   NOT NULL AUTO_INCREMENT,
	create_time DATETIME  ,
	update_time DATETIME  ,
	added_by_id BIGINT  ,
	upd_by_id BIGINT  ,
	user_id BIGINT   NOT NULL,
	user_role VARCHAR  (128) ,
	status INT   NOT NULL DEFAULT 0,
	PRIMARY KEY(id)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

drop table if exists x_asset;
create table x_asset (
	id BIGINT   NOT NULL AUTO_INCREMENT,
	create_time DATETIME  ,
	update_time DATETIME  ,
	added_by_id BIGINT  ,
	upd_by_id BIGINT  ,
	asset_name VARCHAR  (1024)  NOT NULL,
	descr VARCHAR  (4000)  NOT NULL,
	act_status INT   NOT NULL DEFAULT 0,
	asset_type INT   NOT NULL DEFAULT 0,
	config TEXT  ,
	sup_native TINYINT  (1)  NOT NULL DEFAULT 0,
	PRIMARY KEY(id)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

drop table if exists x_resource;
create table x_resource (
	id BIGINT   NOT NULL AUTO_INCREMENT,
	create_time DATETIME  ,
	update_time DATETIME  ,
	added_by_id BIGINT  ,
	upd_by_id BIGINT  ,
	res_name VARCHAR  (4000) ,
	descr VARCHAR  (4000) ,
	res_type INT   NOT NULL DEFAULT 0,
	asset_id BIGINT   NOT NULL,
	parent_id BIGINT  ,
	parent_path VARCHAR  (4000) ,
	is_encrypt INT   NOT NULL DEFAULT 0,
	is_recursive INT   NOT NULL DEFAULT 0,
	res_group VARCHAR  (1024) ,
	res_dbs TEXT  ,
	res_tables TEXT  ,
	res_col_fams TEXT  ,
	res_cols TEXT  ,
	res_udfs TEXT  ,
	res_status INT   NOT NULL DEFAULT 1,
	table_type INT   NOT NULL DEFAULT 0,
	col_type INT   NOT NULL DEFAULT 0,
	PRIMARY KEY(id)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

drop table if exists x_cred_store;
create table x_cred_store (
	id BIGINT   NOT NULL AUTO_INCREMENT,
	create_time DATETIME  ,
	update_time DATETIME  ,
	added_by_id BIGINT  ,
	upd_by_id BIGINT  ,
	store_name VARCHAR  (1024)  NOT NULL,
	descr VARCHAR  (4000)  NOT NULL,
	PRIMARY KEY(id)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

drop table if exists x_group;
create table x_group (
	id BIGINT   NOT NULL AUTO_INCREMENT,
	create_time DATETIME  ,
	update_time DATETIME  ,
	added_by_id BIGINT  ,
	upd_by_id BIGINT  ,
	group_name VARCHAR  (1024)  NOT NULL,
	descr VARCHAR  (4000)  NOT NULL,
	status INT   NOT NULL DEFAULT 0,
	group_type INT   NOT NULL DEFAULT 0,
	cred_store_id BIGINT  ,
	PRIMARY KEY(id)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

drop table if exists x_user;
create table x_user (
	id BIGINT   NOT NULL AUTO_INCREMENT,
	create_time DATETIME  ,
	update_time DATETIME  ,
	added_by_id BIGINT  ,
	upd_by_id BIGINT  ,
	user_name VARCHAR  (1024)  NOT NULL,
	descr VARCHAR  (4000)  NOT NULL,
	status INT   NOT NULL DEFAULT 0,
	cred_store_id BIGINT  ,
	PRIMARY KEY(id)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

drop table if exists x_group_users;
create table x_group_users (
	id BIGINT   NOT NULL AUTO_INCREMENT,
	create_time DATETIME  ,
	update_time DATETIME  ,
	added_by_id BIGINT  ,
	upd_by_id BIGINT  ,
	group_name VARCHAR  (1024)  NOT NULL,
	p_group_id BIGINT  ,
	user_id BIGINT  ,
	PRIMARY KEY(id)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

drop table if exists x_group_groups;
create table x_group_groups (
	id BIGINT   NOT NULL AUTO_INCREMENT,
	create_time DATETIME  ,
	update_time DATETIME  ,
	added_by_id BIGINT  ,
	upd_by_id BIGINT  ,
	group_name VARCHAR  (1024)  NOT NULL,
	p_group_id BIGINT  ,
	group_id BIGINT  ,
	PRIMARY KEY(id)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

drop table if exists x_perm_map;
create table x_perm_map (
	id BIGINT   NOT NULL AUTO_INCREMENT,
	create_time DATETIME  ,
	update_time DATETIME  ,
	added_by_id BIGINT  ,
	upd_by_id BIGINT  ,
	perm_group VARCHAR  (1024) ,
	res_id BIGINT  ,
	group_id BIGINT  ,
	user_id BIGINT  ,
	perm_for INT   NOT NULL DEFAULT 0,
	perm_type INT   NOT NULL DEFAULT 0,
	is_recursive INT   NOT NULL DEFAULT 0,
	is_wild_card TINYINT  (1)  NOT NULL DEFAULT 1,
	grant_revoke TINYINT  (1)  NOT NULL DEFAULT 1,
	PRIMARY KEY(id)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

drop table if exists x_audit_map;
create table x_audit_map (
	id BIGINT   NOT NULL AUTO_INCREMENT,
	create_time DATETIME  ,
	update_time DATETIME  ,
	added_by_id BIGINT  ,
	upd_by_id BIGINT  ,
	res_id BIGINT  ,
	group_id BIGINT  ,
	user_id BIGINT  ,
	audit_type INT   NOT NULL DEFAULT 0,
	PRIMARY KEY(id)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

drop table if exists x_policy_export_audit;
create table x_policy_export_audit (
	id BIGINT   NOT NULL AUTO_INCREMENT,
	create_time DATETIME  ,
	update_time DATETIME  ,
	added_by_id BIGINT  ,
	upd_by_id BIGINT  ,
	client_ip VARCHAR  (255)  NOT NULL,
	agent_id VARCHAR  (255) ,
	req_epoch BIGINT   NOT NULL,
	last_updated DATETIME  ,
	repository_name VARCHAR  (1024) ,
	exported_json TEXT  ,
	http_ret_code INT   NOT NULL DEFAULT 0,
	PRIMARY KEY(id)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

drop table if exists x_trx_log;
create table x_trx_log (
	id BIGINT   NOT NULL AUTO_INCREMENT,
	create_time DATETIME  ,
	update_time DATETIME  ,
	added_by_id BIGINT  ,
	upd_by_id BIGINT  ,
	class_type INT   NOT NULL DEFAULT 0,
	object_id BIGINT  ,
	parent_object_id BIGINT  ,
	parent_object_class_type INT   NOT NULL DEFAULT 0,
	parent_object_name VARCHAR  (1024) ,
	object_name VARCHAR  (1024) ,
	attr_name VARCHAR  (255) ,
	prev_val VARCHAR  (1024) ,
	new_val VARCHAR  (1024) ,
	trx_id VARCHAR  (1024) ,
	action VARCHAR  (255) ,
	sess_id VARCHAR  (512) ,
	req_id VARCHAR  (30) ,
	sess_type VARCHAR  (30) ,
	PRIMARY KEY(id)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

drop table if exists xa_access_audit;
create table xa_access_audit (
	id BIGINT   NOT NULL AUTO_INCREMENT,
	create_time DATETIME  ,
	update_time DATETIME  ,
	added_by_id BIGINT  ,
	upd_by_id BIGINT  ,
	audit_type INT   NOT NULL DEFAULT 0,
	access_result INT   DEFAULT 0,
	access_type VARCHAR  (255) ,
	acl_enforcer VARCHAR  (255) ,
	agent_id VARCHAR  (255) ,
	client_ip VARCHAR  (255) ,
	client_type VARCHAR  (255) ,
	policy_id BIGINT   DEFAULT 0,
	repo_name VARCHAR  (255) ,
	repo_type INT   DEFAULT 0,
	result_reason VARCHAR  (255) ,
	session_id VARCHAR  (255) ,
	event_time DATETIME  ,
	request_user VARCHAR  (255) ,
	action VARCHAR  (2000) ,
	request_data VARCHAR  (2000) ,
	resource_path VARCHAR  (2000) ,
	resource_type VARCHAR  (255) ,
	PRIMARY KEY(id)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;


ALTER TABLE x_db_base ADD (
  CONSTRAINT x_db_base_FK_added_by_id FOREIGN KEY (added_by_id)
    REFERENCES x_portal_user (id));
ALTER TABLE x_db_base ADD (
  CONSTRAINT x_db_base_FK_upd_by_id FOREIGN KEY (upd_by_id)
    REFERENCES x_portal_user (id));
ALTER TABLE x_auth_sess ADD (
  CONSTRAINT x_auth_sess_FK_added_by_id FOREIGN KEY (added_by_id)
    REFERENCES x_portal_user (id));
ALTER TABLE x_auth_sess ADD (
  CONSTRAINT x_auth_sess_FK_upd_by_id FOREIGN KEY (upd_by_id)
    REFERENCES x_portal_user (id));
ALTER TABLE x_auth_sess ADD (
  CONSTRAINT x_auth_sess_FK_user_id FOREIGN KEY (user_id)
    REFERENCES x_portal_user (id));
ALTER TABLE x_portal_user ADD (
  CONSTRAINT x_portal_user_FK_added_by_id FOREIGN KEY (added_by_id)
    REFERENCES x_portal_user (id));
ALTER TABLE x_portal_user ADD (
  CONSTRAINT x_portal_user_FK_upd_by_id FOREIGN KEY (upd_by_id)
    REFERENCES x_portal_user (id));
ALTER TABLE x_portal_user_role ADD (
  CONSTRAINT x_portal_user_role_FK_added_by_id FOREIGN KEY (added_by_id)
    REFERENCES x_portal_user (id));
ALTER TABLE x_portal_user_role ADD (
  CONSTRAINT x_portal_user_role_FK_upd_by_id FOREIGN KEY (upd_by_id)
    REFERENCES x_portal_user (id));
ALTER TABLE x_portal_user_role ADD (
  CONSTRAINT x_portal_user_role_FK_user_id FOREIGN KEY (user_id)
    REFERENCES x_portal_user (id));
ALTER TABLE x_asset ADD (
  CONSTRAINT x_asset_FK_added_by_id FOREIGN KEY (added_by_id)
    REFERENCES x_portal_user (id));
ALTER TABLE x_asset ADD (
  CONSTRAINT x_asset_FK_upd_by_id FOREIGN KEY (upd_by_id)
    REFERENCES x_portal_user (id));
ALTER TABLE x_resource ADD (
  CONSTRAINT x_resource_FK_added_by_id FOREIGN KEY (added_by_id)
    REFERENCES x_portal_user (id));
ALTER TABLE x_resource ADD (
  CONSTRAINT x_resource_FK_upd_by_id FOREIGN KEY (upd_by_id)
    REFERENCES x_portal_user (id));
ALTER TABLE x_resource ADD (
  CONSTRAINT x_resource_FK_asset_id FOREIGN KEY (asset_id)
    REFERENCES x_asset (id));
ALTER TABLE x_resource ADD (
  CONSTRAINT x_resource_FK_parent_id FOREIGN KEY (parent_id)
    REFERENCES x_resource (id));
ALTER TABLE x_cred_store ADD (
  CONSTRAINT x_cred_store_FK_added_by_id FOREIGN KEY (added_by_id)
    REFERENCES x_portal_user (id));
ALTER TABLE x_cred_store ADD (
  CONSTRAINT x_cred_store_FK_upd_by_id FOREIGN KEY (upd_by_id)
    REFERENCES x_portal_user (id));
ALTER TABLE x_group ADD (
  CONSTRAINT x_group_FK_added_by_id FOREIGN KEY (added_by_id)
    REFERENCES x_portal_user (id));
ALTER TABLE x_group ADD (
  CONSTRAINT x_group_FK_upd_by_id FOREIGN KEY (upd_by_id)
    REFERENCES x_portal_user (id));
ALTER TABLE x_group ADD (
  CONSTRAINT x_group_FK_cred_store_id FOREIGN KEY (cred_store_id)
    REFERENCES x_cred_store (id));
ALTER TABLE x_user ADD (
  CONSTRAINT x_user_FK_added_by_id FOREIGN KEY (added_by_id)
    REFERENCES x_portal_user (id));
ALTER TABLE x_user ADD (
  CONSTRAINT x_user_FK_upd_by_id FOREIGN KEY (upd_by_id)
    REFERENCES x_portal_user (id));
ALTER TABLE x_user ADD (
  CONSTRAINT x_user_FK_cred_store_id FOREIGN KEY (cred_store_id)
    REFERENCES x_cred_store (id));
ALTER TABLE x_group_users ADD (
  CONSTRAINT x_group_users_FK_added_by_id FOREIGN KEY (added_by_id)
    REFERENCES x_portal_user (id));
ALTER TABLE x_group_users ADD (
  CONSTRAINT x_group_users_FK_upd_by_id FOREIGN KEY (upd_by_id)
    REFERENCES x_portal_user (id));
ALTER TABLE x_group_users ADD (
  CONSTRAINT x_group_users_FK_p_group_id FOREIGN KEY (p_group_id)
    REFERENCES x_group (id));
ALTER TABLE x_group_users ADD (
  CONSTRAINT x_group_users_FK_user_id FOREIGN KEY (user_id)
    REFERENCES x_user (id));
ALTER TABLE x_group_groups ADD (
  CONSTRAINT x_group_groups_FK_added_by_id FOREIGN KEY (added_by_id)
    REFERENCES x_portal_user (id));
ALTER TABLE x_group_groups ADD (
  CONSTRAINT x_group_groups_FK_upd_by_id FOREIGN KEY (upd_by_id)
    REFERENCES x_portal_user (id));
ALTER TABLE x_group_groups ADD (
  CONSTRAINT x_group_groups_FK_p_group_id FOREIGN KEY (p_group_id)
    REFERENCES x_group (id));
ALTER TABLE x_group_groups ADD (
  CONSTRAINT x_group_groups_FK_group_id FOREIGN KEY (group_id)
    REFERENCES x_group (id));
ALTER TABLE x_perm_map ADD (
  CONSTRAINT x_perm_map_FK_added_by_id FOREIGN KEY (added_by_id)
    REFERENCES x_portal_user (id));
ALTER TABLE x_perm_map ADD (
  CONSTRAINT x_perm_map_FK_upd_by_id FOREIGN KEY (upd_by_id)
    REFERENCES x_portal_user (id));
ALTER TABLE x_perm_map ADD (
  CONSTRAINT x_perm_map_FK_res_id FOREIGN KEY (res_id)
    REFERENCES x_resource (id));
ALTER TABLE x_perm_map ADD (
  CONSTRAINT x_perm_map_FK_group_id FOREIGN KEY (group_id)
    REFERENCES x_group (id));
ALTER TABLE x_perm_map ADD (
  CONSTRAINT x_perm_map_FK_user_id FOREIGN KEY (user_id)
    REFERENCES x_user (id));
ALTER TABLE x_audit_map ADD (
  CONSTRAINT x_audit_map_FK_added_by_id FOREIGN KEY (added_by_id)
    REFERENCES x_portal_user (id));
ALTER TABLE x_audit_map ADD (
  CONSTRAINT x_audit_map_FK_upd_by_id FOREIGN KEY (upd_by_id)
    REFERENCES x_portal_user (id));
ALTER TABLE x_audit_map ADD (
  CONSTRAINT x_audit_map_FK_res_id FOREIGN KEY (res_id)
    REFERENCES x_resource (id));
ALTER TABLE x_audit_map ADD (
  CONSTRAINT x_audit_map_FK_group_id FOREIGN KEY (group_id)
    REFERENCES x_group (id));
ALTER TABLE x_audit_map ADD (
  CONSTRAINT x_audit_map_FK_user_id FOREIGN KEY (user_id)
    REFERENCES x_user (id));
ALTER TABLE x_policy_export_audit ADD (
  CONSTRAINT x_policy_export_audit_FK_added_by_id FOREIGN KEY (added_by_id)
    REFERENCES x_portal_user (id));
ALTER TABLE x_policy_export_audit ADD (
  CONSTRAINT x_policy_export_audit_FK_upd_by_id FOREIGN KEY (upd_by_id)
    REFERENCES x_portal_user (id));
ALTER TABLE x_trx_log ADD (
  CONSTRAINT x_trx_log_FK_added_by_id FOREIGN KEY (added_by_id)
    REFERENCES x_portal_user (id));
ALTER TABLE x_trx_log ADD (
  CONSTRAINT x_trx_log_FK_upd_by_id FOREIGN KEY (upd_by_id)
    REFERENCES x_portal_user (id));
ALTER TABLE xa_access_audit ADD (
  CONSTRAINT xa_access_audit_FK_added_by_id FOREIGN KEY (added_by_id)
    REFERENCES x_portal_user (id));
ALTER TABLE xa_access_audit ADD (
  CONSTRAINT xa_access_audit_FK_upd_by_id FOREIGN KEY (upd_by_id)
    REFERENCES x_portal_user (id));

ALTER TABLE x_portal_user ADD (
  CONSTRAINT x_portal_user_UK_login_id UNIQUE (login_id(767)) );
ALTER TABLE x_portal_user ADD (
  CONSTRAINT x_portal_user_UK_email UNIQUE (email(512)) );

ALTER TABLE x_db_base ADD (INDEX x_db_base_cr_time (create_time));
ALTER TABLE x_db_base ADD (INDEX x_db_base_up_time (update_time));
ALTER TABLE x_auth_sess ADD (INDEX x_auth_sess_cr_time (create_time));
ALTER TABLE x_auth_sess ADD (INDEX x_auth_sess_up_time (update_time));
ALTER TABLE x_portal_user ADD (INDEX x_portal_user_cr_time (create_time));
ALTER TABLE x_portal_user ADD (INDEX x_portal_user_up_time (update_time));
ALTER TABLE x_portal_user ADD (INDEX x_portal_user_name (first_name(767)));
ALTER TABLE x_portal_user ADD (INDEX x_portal_user_email (email(512)));
ALTER TABLE x_portal_user_role ADD (INDEX x_portal_user_role_cr_time (create_time));
ALTER TABLE x_portal_user_role ADD (INDEX x_portal_user_role_up_time (update_time));
ALTER TABLE x_asset ADD (INDEX x_asset_cr_time (create_time));
ALTER TABLE x_asset ADD (INDEX x_asset_up_time (update_time));
ALTER TABLE x_resource ADD (INDEX x_resource_cr_time (create_time));
ALTER TABLE x_resource ADD (INDEX x_resource_up_time (update_time));
ALTER TABLE x_cred_store ADD (INDEX x_cred_store_cr_time (create_time));
ALTER TABLE x_cred_store ADD (INDEX x_cred_store_up_time (update_time));
ALTER TABLE x_group ADD (INDEX x_group_cr_time (create_time));
ALTER TABLE x_group ADD (INDEX x_group_up_time (update_time));
ALTER TABLE x_user ADD (INDEX x_user_cr_time (create_time));
ALTER TABLE x_user ADD (INDEX x_user_up_time (update_time));
ALTER TABLE x_group_users ADD (INDEX x_group_users_cr_time (create_time));
ALTER TABLE x_group_users ADD (INDEX x_group_users_up_time (update_time));
ALTER TABLE x_group_groups ADD (INDEX x_group_groups_cr_time (create_time));
ALTER TABLE x_group_groups ADD (INDEX x_group_groups_up_time (update_time));
ALTER TABLE x_perm_map ADD (INDEX x_perm_map_cr_time (create_time));
ALTER TABLE x_perm_map ADD (INDEX x_perm_map_up_time (update_time));
ALTER TABLE x_audit_map ADD (INDEX x_audit_map_cr_time (create_time));
ALTER TABLE x_audit_map ADD (INDEX x_audit_map_up_time (update_time));
ALTER TABLE x_policy_export_audit ADD (INDEX x_policy_export_audit_cr_time (create_time));
ALTER TABLE x_policy_export_audit ADD (INDEX x_policy_export_audit_up_time (update_time));
ALTER TABLE x_trx_log ADD (INDEX x_trx_log_cr_time (create_time));
ALTER TABLE x_trx_log ADD (INDEX x_trx_log_up_time (update_time));
ALTER TABLE xa_access_audit ADD (INDEX xa_access_audit_cr_time (create_time));
ALTER TABLE xa_access_audit ADD (INDEX xa_access_audit_up_time (update_time));
SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
##champlain changes
ALTER TABLE  `x_group` ADD  `group_src` INT NOT NULL DEFAULT 0;
ALTER TABLE  `x_resource` ADD  `policy_name` VARCHAR( 500 ) NULL DEFAULT NULL;
ALTER TABLE  `x_resource` ADD UNIQUE  `x_resource_UK_policy_name` (  `policy_name` );
ALTER TABLE  `x_resource` ADD  `res_topologies` TEXT NULL DEFAULT NULL ;
ALTER TABLE  `x_resource` ADD  `res_services` TEXT NULL DEFAULT NULL;
ALTER TABLE  `x_perm_map` ADD  `ip_address` TEXT NULL DEFAULT NULL;
ALTER TABLE  `x_asset` CHANGE  `config`  `config` LONGTEXT NULL DEFAULT NULL ;