-- Licensed to the Apache Software Foundation(ASF) under one or more
-- contributor license agreements.  See the NOTICE file distributed with
-- this work for additional information regarding copyright ownership.
-- The ASF licenses this file to You under the Apache License, Version 2.0
--(the "License"); you may not use this file except in compliance with
-- the License.  You may obtain a copy of the License at
--
--	 http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.


CREATE OR REPLACE PROCEDURE dbo.removeForeignKeysAndTable (IN table_name varchar(100))
AS
BEGIN
	DECLARE @stmt VARCHAR(300)
	DECLARE @tblname VARCHAR(300)
	DECLARE @drpstmt VARCHAR(1000)
	DECLARE cur CURSOR FOR select 'alter table dbo.' + table_name + ' drop constraint ' + role from SYS.SYSFOREIGNKEYS where foreign_creator ='dbo' and foreign_tname = table_name
	OPEN cur WITH HOLD
		fetch cur into @stmt
		WHILE (@@sqlstatus = 0)
		BEGIN
			execute(@stmt)
			fetch cur into @stmt
		END
	close cur
	DEALLOCATE CURSOR cur
	SET @tblname ='dbo.' + table_name;
	SET @drpstmt = 'DROP TABLE IF EXISTS ' + @tblname;
	execute(@drpstmt)
END

GO
DROP VIEW IF EXISTS dbo.vx_trx_log
GO
call dbo.removeForeignKeysAndTable('x_rms_mapping_provider')
GO
call dbo.removeForeignKeysAndTable('x_rms_resource_mapping')
GO
call dbo.removeForeignKeysAndTable('x_rms_notification')
GO
call dbo.removeForeignKeysAndTable('x_rms_service_resource')
GO
call dbo.removeForeignKeysAndTable('x_tag_change_log')
GO
call dbo.removeForeignKeysAndTable('x_role_ref_role')
GO
call dbo.removeForeignKeysAndTable('x_policy_ref_role')
GO
call dbo.removeForeignKeysAndTable('x_role_ref_group')
GO
call dbo.removeForeignKeysAndTable('x_role_ref_user')
GO
call dbo.removeForeignKeysAndTable('x_role')
GO
call dbo.removeForeignKeysAndTable('x_policy_change_log')
GO
call dbo.removeForeignKeysAndTable('x_security_zone_ref_resource')
GO
call dbo.removeForeignKeysAndTable('x_policy_ref_group')
GO
call dbo.removeForeignKeysAndTable('x_policy_ref_user')
GO
call dbo.removeForeignKeysAndTable('x_policy_ref_datamask_type')
GO
call dbo.removeForeignKeysAndTable('x_policy_ref_condition')
GO
call dbo.removeForeignKeysAndTable('x_policy_ref_access_type')
GO
call dbo.removeForeignKeysAndTable('x_policy_ref_resource')
GO
call dbo.removeForeignKeysAndTable('x_ugsync_audit_info')
GO
call dbo.removeForeignKeysAndTable('x_policy_label_map')
GO
call dbo.removeForeignKeysAndTable('x_policy_label')
GO
call dbo.removeForeignKeysAndTable('x_plugin_info')
GO
call dbo.removeForeignKeysAndTable('x_service_version_info')
GO
call dbo.removeForeignKeysAndTable('x_policy_item_rowfilter')
GO
call dbo.removeForeignKeysAndTable('x_policy_item_datamask')
GO
call dbo.removeForeignKeysAndTable('x_datamask_type_def')
GO
call dbo.removeForeignKeysAndTable('x_tag_resource_map')
GO
call dbo.removeForeignKeysAndTable('x_service_resource')
GO
call dbo.removeForeignKeysAndTable('x_tag')
GO
call dbo.removeForeignKeysAndTable('x_tag_def')
GO
call dbo.removeForeignKeysAndTable('x_group_module_perm')
GO
call dbo.removeForeignKeysAndTable('x_user_module_perm')
GO
call dbo.removeForeignKeysAndTable('x_modules_master')
GO
call dbo.removeForeignKeysAndTable('x_data_hist')
GO
call dbo.removeForeignKeysAndTable('x_policy_item_group_perm')
GO
call dbo.removeForeignKeysAndTable('x_policy_item_user_perm')
GO
call dbo.removeForeignKeysAndTable('x_policy_item_condition')
GO
call dbo.removeForeignKeysAndTable('x_policy_item_access')
GO
call dbo.removeForeignKeysAndTable('x_policy_item')
GO
call dbo.removeForeignKeysAndTable('x_policy_resource_map')
GO
call dbo.removeForeignKeysAndTable('x_policy_resource')
GO
call dbo.removeForeignKeysAndTable('x_service_config_map')
GO
call dbo.removeForeignKeysAndTable('x_enum_element_def')
GO
call dbo.removeForeignKeysAndTable('x_enum_def')
GO
call dbo.removeForeignKeysAndTable('x_context_enricher_def')
GO
call dbo.removeForeignKeysAndTable('x_policy_condition_def')
GO
call dbo.removeForeignKeysAndTable('x_access_type_def_grants')
GO
call dbo.removeForeignKeysAndTable('x_access_type_def')
GO
call dbo.removeForeignKeysAndTable('x_resource_def')
GO
call dbo.removeForeignKeysAndTable('x_service_config_def')
GO
call dbo.removeForeignKeysAndTable('x_policy')
GO
call dbo.removeForeignKeysAndTable('x_security_zone_ref_group')
GO
call dbo.removeForeignKeysAndTable('x_security_zone_ref_user')
GO
call dbo.removeForeignKeysAndTable('x_security_zone_ref_tag_srvc')
GO
call dbo.removeForeignKeysAndTable('x_security_zone_ref_service')
GO
call dbo.removeForeignKeysAndTable('x_ranger_global_state')
GO
call dbo.removeForeignKeysAndTable('x_security_zone')
GO
call dbo.removeForeignKeysAndTable('x_service')
GO
call dbo.removeForeignKeysAndTable('x_service_def')
GO
call dbo.removeForeignKeysAndTable('x_audit_map')
GO
call dbo.removeForeignKeysAndTable('x_perm_map')
GO
call dbo.removeForeignKeysAndTable('x_trx_log')
GO
call dbo.removeForeignKeysAndTable('x_resource')
GO
call dbo.removeForeignKeysAndTable('x_policy_export_audit')
GO
call dbo.removeForeignKeysAndTable('x_group_users')
GO
call dbo.removeForeignKeysAndTable('x_user')
GO
call dbo.removeForeignKeysAndTable('x_group_groups')
GO
call dbo.removeForeignKeysAndTable('x_group')
GO
call dbo.removeForeignKeysAndTable('x_db_base')
GO
call dbo.removeForeignKeysAndTable('x_cred_store')
GO
call dbo.removeForeignKeysAndTable('x_auth_sess')
GO
call dbo.removeForeignKeysAndTable('x_asset')
GO
call dbo.removeForeignKeysAndTable('xa_access_audit')
GO
call dbo.removeForeignKeysAndTable('x_portal_user_role')
GO
call dbo.removeForeignKeysAndTable('x_portal_user')
GO
call dbo.removeForeignKeysAndTable('x_db_version_h')
GO

create table dbo.x_db_version_h(
id bigint identity not null primary key,
version varchar(64) not null,
inst_at datetime not null,
inst_by varchar(256) not null,
updated_at datetime not null,
updated_by varchar(256) not null,
active varchar(1) default 'Y' check(active IN ('Y', 'N'))
)
GO
create table dbo.x_portal_user(
	id bigint IDENTITY NOT NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	first_name varchar(256) DEFAULT NULL NULL,
	last_name varchar(256) DEFAULT NULL NULL,
	pub_scr_name varchar(2048) DEFAULT NULL NULL,
	login_id varchar(767) DEFAULT NULL NULL,
	password varchar(512) NOT NULL,
	email varchar(512) DEFAULT NULL NULL,
	status int DEFAULT 0 NOT NULL,
	user_src int DEFAULT 0 NOT NULL,
	notes varchar(4000) DEFAULT NULL NULL,
	other_attributes varchar(4000) DEFAULT NULL NULL,
	sync_source varchar(4000) DEFAULT NULL NULL,
	old_passwords text DEFAULT NULL,
	password_updated_time datetime DEFAULT NULL,
	CONSTRAINT x_portal_user_PK_id PRIMARY KEY CLUSTERED(id),
	CONSTRAINT x_portal_user_UK_login_id UNIQUE NONCLUSTERED (login_id)
)
GO
create table dbo.x_portal_user_role(
	id bigint IDENTITY NOT NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	user_id bigint NOT NULL,
	user_role varchar(128) DEFAULT NULL NULL,
	status int DEFAULT 0 NOT NULL,
	CONSTRAINT x_portal_user_role_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
create table dbo.xa_access_audit(
	id bigint IDENTITY NOT NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	audit_type int DEFAULT 0 NOT NULL,
	access_result int DEFAULT 0 NULL,
	access_type varchar(255) DEFAULT NULL NULL,
	acl_enforcer varchar(255) DEFAULT NULL NULL,
	agent_id varchar(255) DEFAULT NULL NULL,
	client_ip varchar(255) DEFAULT NULL NULL,
	client_type varchar(255) DEFAULT NULL NULL,
	policy_id bigint DEFAULT 0 NULL,
	repo_name varchar(255) DEFAULT NULL NULL,
	repo_type int DEFAULT 0 NULL,
	result_reason varchar(255) DEFAULT NULL NULL,
	session_id varchar(255) DEFAULT NULL NULL,
	event_time datetime DEFAULT NULL NULL,
	request_user varchar(255) DEFAULT NULL NULL,
	action varchar(2000) DEFAULT NULL NULL,
	request_data varchar(4000) DEFAULT NULL NULL,
	resource_path varchar(4000) DEFAULT NULL NULL,
	resource_type varchar(255) DEFAULT NULL NULL,
	seq_num bigint DEFAULT 0 NULL,
	event_count bigint DEFAULT 1 NULL,
	event_dur_ms bigint DEFAULT 1 NULL,
	CONSTRAINT xa_access_audit_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
create table dbo.x_asset(
	id bigint IDENTITY NOT NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	asset_name varchar(1024) NOT NULL,
	descr varchar(4000) NOT NULL,
	act_status int DEFAULT 0 NOT NULL,
	asset_type int DEFAULT 0 NOT NULL,
	config text NULL,
	sup_native tinyint DEFAULT 0 NOT NULL,
	CONSTRAINT x_asset_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
create table dbo.x_auth_sess(
	id bigint IDENTITY NOT NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	login_id varchar(767) NOT NULL,
	user_id bigint DEFAULT NULL NULL,
	ext_sess_id varchar(512) DEFAULT NULL NULL,
	auth_time datetime NOT NULL,
	auth_status int DEFAULT 0 NOT NULL,
	auth_type int DEFAULT 0 NOT NULL,
	auth_provider int DEFAULT 0 NOT NULL,
	device_type int DEFAULT 0 NOT NULL,
	req_ip varchar(48) NOT NULL,
	req_ua varchar(1024) DEFAULT NULL NULL,
	CONSTRAINT x_auth_sess_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
create table dbo.x_cred_store(
	id bigint IDENTITY NOT NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	store_name varchar(1024) NOT NULL,
	descr varchar(4000) NOT NULL,
	CONSTRAINT x_cred_store_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
create table dbo.x_db_base(
	id bigint IDENTITY NOT NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	CONSTRAINT x_db_base_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
create table dbo.x_group(
	id bigint IDENTITY NOT NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	group_name varchar(767) NOT NULL,
	descr varchar(4000) NOT NULL,
	status int DEFAULT 0 NOT NULL,
	group_type int DEFAULT 0 NOT NULL,
	cred_store_id bigint DEFAULT NULL NULL,
	group_src int DEFAULT 0 NOT NULL,
	is_visible int DEFAULT 1 NOT NULL,
	other_attributes varchar(4000) DEFAULT NULL NULL,
	sync_source varchar(4000) DEFAULT NULL NULL,
	CONSTRAINT x_group_PK_id PRIMARY KEY CLUSTERED(id),
	CONSTRAINT x_group_UK_group_name UNIQUE NONCLUSTERED (group_name)
)
GO
create table dbo.x_group_groups(
	id bigint IDENTITY NOT NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	group_name varchar(1024) NOT NULL,
	p_group_id bigint DEFAULT NULL NULL,
	group_id bigint DEFAULT NULL NULL,
	CONSTRAINT x_group_groups_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
create table dbo.x_user(
	id bigint IDENTITY NOT NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	user_name varchar(767) NOT NULL,
	descr varchar(4000) NOT NULL,
	status int DEFAULT 0 NOT NULL,
	cred_store_id bigint DEFAULT NULL NULL,
	is_visible int DEFAULT 1 NOT NULL,
	other_attributes varchar(4000) DEFAULT NULL NULL,
	sync_source varchar(4000) DEFAULT NULL NULL,
	CONSTRAINT x_user_PK_id PRIMARY KEY CLUSTERED(id),
	CONSTRAINT x_user_UK_user_name UNIQUE NONCLUSTERED (user_name)
)
GO
create table dbo.x_group_users(
	id bigint IDENTITY NOT NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	group_name varchar(767) NOT NULL,
	p_group_id bigint DEFAULT NULL NULL,
	user_id bigint NOT NULL,
	CONSTRAINT x_group_users_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
create table dbo.x_policy_export_audit(
	id bigint IDENTITY NOT NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	client_ip varchar(255) NOT NULL,
	agent_id varchar(255) DEFAULT NULL NULL,
	req_epoch bigint NOT NULL,
	last_updated datetime DEFAULT NULL NULL,
	repository_name varchar(1024) DEFAULT NULL NULL,
	exported_json text DEFAULT NULL NULL,
	http_ret_code int DEFAULT 0 NOT NULL,
	cluster_name varchar(255) DEFAULT NULL NULL,
	zone_name varchar(255) DEFAULT NULL NULL,
	policy_version bigint DEFAULT NULL NULL,
	CONSTRAINT x_policy_export_audit_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
create table dbo.x_resource(
	id bigint IDENTITY NOT NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	res_name varchar(4000) DEFAULT NULL NULL,
	descr varchar(4000) DEFAULT NULL NULL,
	res_type int DEFAULT 0 NOT NULL,
	asset_id bigint NOT NULL,
	parent_id bigint DEFAULT NULL NULL,
	parent_path varchar(4000) DEFAULT NULL NULL,
	is_encrypt int DEFAULT 0 NOT NULL,
	is_recursive int DEFAULT 0 NOT NULL,
	res_group varchar(1024) DEFAULT NULL NULL,
	res_dbs text NULL,
	res_tables text NULL,
	res_col_fams text NULL,
	res_cols text NULL,
	res_udfs text NULL,
	res_status int DEFAULT 1 NOT NULL,
	table_type int DEFAULT 0 NOT NULL,
	col_type int DEFAULT 0 NOT NULL,
	policy_name varchar(500) DEFAULT NULL NULL,
	res_topologies text DEFAULT NULL NULL,
	res_services text DEFAULT NULL NULL,
	CONSTRAINT x_resource_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
create table dbo.x_trx_log(
	id bigint IDENTITY NOT NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	class_type int DEFAULT 0 NOT NULL,
	object_id bigint DEFAULT NULL NULL,
	parent_object_id bigint DEFAULT NULL NULL,
	parent_object_class_type int DEFAULT 0 NOT NULL,
	parent_object_name varchar(1024) DEFAULT NULL NULL,
	object_name varchar(1024) DEFAULT NULL NULL,
	attr_name varchar(255) DEFAULT NULL NULL,
	prev_val text DEFAULT NULL NULL,
	new_val text DEFAULT NULL NULL,
	trx_id varchar(1024)DEFAULT NULL NULL,
	action varchar(255) DEFAULT NULL NULL,
	sess_id varchar(512) DEFAULT NULL NULL,
	req_id varchar(30) DEFAULT NULL NULL,
	sess_type varchar(30) DEFAULT NULL NULL,
	CONSTRAINT x_trx_log_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
create table dbo.x_perm_map(
	id bigint IDENTITY NOT NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	perm_group varchar(1024) DEFAULT NULL NULL,
	res_id bigint DEFAULT NULL NULL,
	group_id bigint DEFAULT NULL NULL,
	user_id bigint DEFAULT NULL NULL,
	perm_for int DEFAULT 0 NOT NULL,
	perm_type int DEFAULT 0 NOT NULL,
	is_recursive int DEFAULT 0 NOT NULL,
	is_wild_card tinyint DEFAULT 1 NOT NULL,
	grant_revoke tinyint DEFAULT 1 NOT NULL,
	ip_address text DEFAULT NULL NULL,
	CONSTRAINT x_perm_map_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
create table dbo.x_audit_map(
	id bigint IDENTITY NOT NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	res_id bigint DEFAULT NULL NULL,
	group_id bigint DEFAULT NULL NULL,
	user_id bigint DEFAULT NULL NULL,
	audit_type int DEFAULT 0 NOT NULL,
	CONSTRAINT x_audit_map_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
CREATE VIEW dbo.vx_trx_log AS select x_trx_log.id AS id,x_trx_log.create_time AS create_time,x_trx_log.update_time AS update_time,x_trx_log.added_by_id AS added_by_id,x_trx_log.upd_by_id AS upd_by_id,x_trx_log.class_type AS class_type,x_trx_log.object_id AS object_id,x_trx_log.parent_object_id AS parent_object_id,x_trx_log.parent_object_class_type AS parent_object_class_type,x_trx_log.attr_name AS attr_name,x_trx_log.parent_object_name AS parent_object_name,x_trx_log.object_name AS object_name,x_trx_log.prev_val AS prev_val,x_trx_log.new_val AS new_val,x_trx_log.trx_id AS trx_id,x_trx_log.action AS action,x_trx_log.sess_id AS sess_id,x_trx_log.req_id AS req_id,x_trx_log.sess_type AS sess_type from x_trx_log where id in(select min(x_trx_log.id) from x_trx_log group by x_trx_log.trx_id)
GO
create table dbo.x_service_def(
	id bigint IDENTITY NOT NULL,
	guid varchar(1024) DEFAULT NULL NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	version bigint DEFAULT NULL NULL,
	name varchar(1024) DEFAULT NULL NULL,
	display_name varchar(1024) DEFAULT NULL NULL,
	impl_class_name varchar(1024) DEFAULT NULL NULL,
	label varchar(1024) DEFAULT NULL NULL,
	description varchar(1024) DEFAULT NULL NULL,
	rb_key_label varchar(1024) DEFAULT NULL NULL,
	rb_key_description varchar(1024) DEFAULT NULL NULL,
	is_enabled tinyint DEFAULT 1 NULL,
	def_options varchar(1024) DEFAULT NULL NULL,
	CONSTRAINT x_service_def_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
create table dbo.x_service (
	id bigint IDENTITY NOT NULL,
	guid varchar(1024) DEFAULT NULL NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	version bigint DEFAULT NULL NULL,
	type bigint DEFAULT NULL NULL,
	name varchar(255) DEFAULT NULL NULL,
	display_name varchar(255) DEFAULT NULL NULL,
	policy_version bigint DEFAULT NULL NULL,
	policy_update_time datetime DEFAULT NULL NULL,
	description varchar(1024) DEFAULT NULL NULL,
	is_enabled tinyint DEFAULT 0 NOT NULL,
	tag_service bigint DEFAULT NULL NULL,
	tag_version bigint DEFAULT 0 NOT NULL,
	tag_update_time datetime DEFAULT NULL NULL,
	CONSTRAINT x_service_def_PK_id PRIMARY KEY CLUSTERED(id),
	CONSTRAINT x_service_UK_name UNIQUE NONCLUSTERED (name)
)
GO
CREATE TABLE dbo.x_security_zone(
	id bigint IDENTITY NOT NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	version bigint DEFAULT NULL NULL,
	name varchar(255) NOT NULL,
	jsonData text DEFAULT NULL NULL,
	description varchar(1024) DEFAULT NULL NULL,
	CONSTRAINT x_security_zone_PK_id PRIMARY KEY CLUSTERED(id),
	CONSTRAINT x_security_zone_UK_name UNIQUE NONCLUSTERED(name)
)
GO

CREATE TABLE dbo.x_ranger_global_state(
	id bigint IDENTITY NOT NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	version bigint DEFAULT NULL NULL,
	state_name varchar(255) NOT NULL,
	app_data varchar(255) DEFAULT NULL NULL,
	CONSTRAINT x_ranger_global_state_PK_id PRIMARY KEY CLUSTERED(id),
	CONSTRAINT x_ranger_global_state_UK_state_name UNIQUE NONCLUSTERED(state_name)
)
GO
create table dbo.x_policy (
	id bigint IDENTITY NOT NULL,
	guid varchar(1024) NOT NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	version bigint DEFAULT NULL NULL,
	service bigint NOT NULL,
	name varchar(512) NOT NULL,
	policy_type int DEFAULT 0 NULL,
	description varchar(1024) DEFAULT NULL NULL,
	resource_signature varchar(128) NOT NULL,
	is_enabled tinyint DEFAULT 0 NOT NULL,
	is_audit_enabled tinyint DEFAULT 0 NOT NULL,
	policy_options varchar(4000) DEFAULT NULL NULL,
	policy_priority int DEFAULT 0 NOT NULL,
	policy_text text DEFAULT NULL NULL,
	zone_id bigint DEFAULT '1' NOT NULL,
	CONSTRAINT x_policy_PK_id PRIMARY KEY CLUSTERED(id),
	CONSTRAINT x_policy_UK_name_service_zone UNIQUE NONCLUSTERED (name,service,zone_id),
	CONSTRAINT x_policy_UK_guid_service_zone UNIQUE NONCLUSTERED (guid,service,zone_id),
	CONSTRAINT x_policy_UK_service_signature UNIQUE NONCLUSTERED (service,resource_signature),
)
GO
create table dbo.x_service_config_def (
	id bigint IDENTITY NOT NULL,
	guid varchar(1024) DEFAULT NULL NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	def_id bigint NOT NULL,
	item_id bigint NOT NULL,
	name varchar(1024) DEFAULT NULL NULL,
	type varchar(1024) DEFAULT NULL NULL,
	sub_type varchar(1024) DEFAULT NULL NULL,
	is_mandatory tinyint DEFAULT 0 NOT NULL,
	default_value varchar(1024) DEFAULT NULL NULL,
	validation_reg_ex varchar(1024) DEFAULT NULL NULL,
	validation_message varchar(1024) DEFAULT NULL NULL,
	ui_hint varchar(1024) DEFAULT NULL NULL,
	label varchar(1024) DEFAULT NULL NULL,
	description varchar(1024) DEFAULT NULL NULL,
	rb_key_label varchar(1024) DEFAULT NULL NULL,
	rb_key_description varchar(1024) DEFAULT NULL NULL,
	rb_key_validation_message varchar(1024) DEFAULT NULL NULL,
	sort_order int DEFAULT 0 NULL,
	CONSTRAINT x_service_config_def_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
create table dbo.x_resource_def (
	id bigint IDENTITY NOT NULL,
	guid varchar(1024) DEFAULT NULL NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	def_id bigint NOT NULL,
	item_id bigint NOT NULL,
	name varchar(1024) DEFAULT NULL NULL,
	type varchar(1024) DEFAULT NULL NULL,
	res_level bigint DEFAULT NULL NULL,
	parent bigint DEFAULT NULL NULL,
	mandatory tinyint DEFAULT 0 NOT NULL,
	look_up_supported tinyint DEFAULT 0 NOT NULL,
	recursive_supported tinyint DEFAULT 0 NOT NULL,
	excludes_supported tinyint DEFAULT 0 NOT NULL,
	matcher varchar(1024) DEFAULT NULL NULL,
	matcher_options varchar(1024) DEFAULT NULL NULL,
	validation_reg_ex varchar(1024) DEFAULT NULL NULL,
	validation_message varchar(1024) DEFAULT NULL NULL,
	ui_hint varchar(1024) DEFAULT NULL NULL,
	label varchar(1024) DEFAULT NULL NULL,
	description varchar(1024) DEFAULT NULL NULL,
	rb_key_label varchar(1024) DEFAULT NULL NULL,
	rb_key_description varchar(1024) DEFAULT NULL NULL,
	rb_key_validation_message varchar(1024) DEFAULT NULL NULL,
	sort_order int DEFAULT 0 NULL,
	datamask_options VARCHAR(1024) DEFAULT NULL NULL,
	rowfilter_options VARCHAR(1024) DEFAULT NULL NULL,
	CONSTRAINT x_resource_def_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
create table dbo.x_access_type_def (
	id bigint IDENTITY NOT NULL,
	guid varchar(1024) DEFAULT NULL NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	def_id bigint NOT NULL,
	item_id bigint NOT NULL,
	name varchar(1024) DEFAULT NULL NULL,
	label varchar(1024) DEFAULT NULL NULL,
	rb_key_label varchar(1024) DEFAULT NULL NULL,
	sort_order int DEFAULT 0 NULL,
	datamask_options VARCHAR(1024) DEFAULT NULL NULL,
	rowfilter_options VARCHAR(1024) DEFAULT NULL NULL,
	CONSTRAINT x_access_type_def_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
create table dbo.x_access_type_def_grants(
	id bigint IDENTITY NOT NULL,
	guid varchar(1024) DEFAULT NULL NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	atd_id bigint NOT NULL,
	implied_grant varchar(1024) DEFAULT NULL NULL,
	CONSTRAINT x_access_type_def_grants_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
create table dbo.x_policy_condition_def (
	id bigint IDENTITY NOT NULL,
	guid varchar(1024) DEFAULT NULL NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	def_id bigint NOT NULL,
	item_id bigint NOT NULL,
	name varchar(1024) DEFAULT NULL NULL,
	evaluator varchar(1024) DEFAULT NULL NULL,
	evaluator_options varchar(1024) DEFAULT NULL NULL,
	validation_reg_ex varchar(1024) DEFAULT NULL NULL,
	validation_message varchar(1024) DEFAULT NULL NULL,
	ui_hint varchar(1024) DEFAULT NULL NULL,
	label varchar(1024) DEFAULT NULL NULL,
	description varchar(1024) DEFAULT NULL NULL,
	rb_key_label varchar(1024) DEFAULT NULL NULL,
	rb_key_description varchar(1024) DEFAULT NULL NULL,
	rb_key_validation_message varchar(1024) DEFAULT NULL NULL,
	sort_order int DEFAULT 0 NULL,
	CONSTRAINT x_policy_condition_def_grants_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
create table dbo.x_context_enricher_def(
	id bigint IDENTITY NOT NULL,
	guid varchar(1024) DEFAULT NULL NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	def_id bigint NOT NULL,
	item_id bigint NOT NULL,
	name varchar(1024) DEFAULT NULL NULL,
	enricher varchar(1024) DEFAULT NULL NULL,
	enricher_options varchar(1024) DEFAULT NULL NULL,
	sort_order int DEFAULT 0 NULL,
	CONSTRAINT x_context_enricher_def_grants_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
create table dbo.x_enum_def (
	id bigint IDENTITY NOT NULL,
	guid varchar(1024) DEFAULT NULL NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	def_id bigint NOT NULL,
	item_id bigint NOT NULL,
	name varchar(1024) DEFAULT NULL NULL,
	default_index bigint DEFAULT NULL NULL,
	CONSTRAINT x_enum_def_grants_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
create table dbo.x_enum_element_def (
	id bigint IDENTITY NOT NULL,
	guid varchar(1024) DEFAULT NULL NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	enum_def_id bigint NOT NULL,
	item_id bigint NOT NULL,
	name varchar(1024) DEFAULT NULL NULL,
	label varchar(1024) DEFAULT NULL NULL,
	rb_key_label varchar(1024) DEFAULT NULL NULL,
	sort_order int DEFAULT 0 NULL,
	CONSTRAINT x_enum_element_def_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
create table dbo.x_service_config_map (
	id bigint IDENTITY NOT NULL,
	guid varchar(1024) DEFAULT NULL NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	service bigint NOT NULL,
	config_key varchar(1024) DEFAULT NULL NULL,
	config_value varchar(4000) DEFAULT NULL NULL,
	CONSTRAINT x_service_config_map_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
create table dbo.x_policy_resource (
	id bigint IDENTITY NOT NULL,
	guid varchar(1024) DEFAULT NULL NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	policy_id bigint NOT NULL,
	res_def_id bigint NOT NULL,
	is_excludes tinyint DEFAULT 0 NOT NULL,
	is_recursive tinyint DEFAULT 0 NOT NULL,
	CONSTRAINT x_policy_resource_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
create table dbo.x_policy_resource_map (
	id bigint IDENTITY NOT NULL,
	guid varchar(1024) DEFAULT NULL NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	resource_id bigint NOT NULL,
	value varchar(1024) DEFAULT NULL NULL,
	sort_order int DEFAULT 0 NULL,
	CONSTRAINT x_policy_resource_map_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
create table dbo.x_policy_item (
	id bigint IDENTITY NOT NULL,
	guid varchar(1024) DEFAULT NULL NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	policy_id bigint NOT NULL,
	delegate_admin tinyint DEFAULT 0 NOT NULL,
	sort_order int DEFAULT 0 NULL,
	item_type int DEFAULT 0 NOT NULL,
	is_enabled tinyint DEFAULT 1 NOT NULL,
	comments varchar(255) DEFAULT NULL NULL,
	CONSTRAINT x_policy_item_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
create table dbo.x_policy_item_access (
	id bigint IDENTITY NOT NULL,
	guid varchar(1024) DEFAULT NULL NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	policy_item_id bigint NOT NULL,
	type bigint NOT NULL,
	is_allowed tinyint DEFAULT 0 NOT NULL,
	sort_order int DEFAULT 0 NULL,
	CONSTRAINT x_policy_item_access_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
create table dbo.x_policy_item_condition (
	id bigint IDENTITY NOT NULL,
	guid varchar(1024) DEFAULT NULL NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	policy_item_id bigint NOT NULL,
	type bigint NOT NULL,
	value varchar(1024) DEFAULT NULL NULL,
	sort_order int DEFAULT 0 NULL,
	CONSTRAINT x_policy_item_condition_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
create table dbo.x_policy_item_user_perm (
	id bigint IDENTITY NOT NULL,
	guid varchar(1024) DEFAULT NULL NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	policy_item_id bigint NOT NULL,
	user_id bigint DEFAULT NULL NULL,
	sort_order int DEFAULT 0 NULL,
	CONSTRAINT x_policy_item_user_perm_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
create table dbo.x_policy_item_group_perm (
	id bigint IDENTITY NOT NULL,
	guid varchar(1024) DEFAULT NULL NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	policy_item_id bigint NOT NULL,
	group_id bigint DEFAULT NULL NULL,
	sort_order int DEFAULT 0 NULL,
	CONSTRAINT x_policy_item_group_perm_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
create table dbo.x_data_hist (
	id bigint IDENTITY NOT NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	obj_guid varchar(1024) NOT NULL,
	obj_class_type int NOT NULL,
	obj_id bigint NOT NULL,
	obj_name varchar(1024) NOT NULL,
	version bigint DEFAULT NULL NULL,
	action varchar(512) NOT NULL,
	from_time datetime NOT NULL,
	to_time datetime DEFAULT NULL NULL,
	content text NOT NULL,
	CONSTRAINT x_data_hist_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
create table dbo.x_modules_master (
	id bigint IDENTITY NOT NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	module varchar(1024)NOT NULL,
	url varchar(1024) DEFAULT NULL NULL,
	CONSTRAINT x_modules_master_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
create table dbo.x_user_module_perm (
	id bigint IDENTITY NOT NULL,
	user_id bigint DEFAULT NULL NULL,
	module_id bigint DEFAULT NULL NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	is_allowed int DEFAULT 1 NOT NULL,
	CONSTRAINT x_user_module_perm_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
create table dbo.x_group_module_perm (
	id bigint IDENTITY NOT NULL,
	group_id bigint DEFAULT NULL NULL,
	module_id bigint DEFAULT NULL NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	is_allowed int DEFAULT 1 NOT NULL,
	CONSTRAINT x_group_module_perm_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
CREATE TABLE dbo.x_tag_def(
	id bigint IDENTITY NOT NULL,
	guid varchar(64) NOT NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	version bigint DEFAULT NULL NULL,
	name varchar(255) NOT NULL,
	source varchar(128) DEFAULT NULL NULL,
	is_enabled tinyint DEFAULT 0 NOT NULL,
	tag_attrs_def_text text DEFAULT NULL NULL,
	CONSTRAINT x_tag_def_PK_id PRIMARY KEY CLUSTERED(id),
	CONSTRAINT x_tag_def_UK_guid UNIQUE NONCLUSTERED (guid),
	CONSTRAINT x_tag_def_UK_name UNIQUE NONCLUSTERED (name)
)
GO
CREATE TABLE dbo.x_tag(
	id bigint IDENTITY NOT NULL,
	guid varchar(64) NOT NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	version bigint DEFAULT NULL NULL,
	type bigint NOT NULL,
	owned_by smallint DEFAULT 0 NOT NULL,
	policy_options varchar(4000) DEFAULT NULL NULL,
	tag_attrs_text text DEFAULT NULL NULL,
	CONSTRAINT x_tag_PK_id PRIMARY KEY CLUSTERED(id),
	CONSTRAINT x_tag_UK_guid UNIQUE NONCLUSTERED (guid)
)
GO
CREATE TABLE dbo.x_service_resource(
	id bigint IDENTITY NOT NULL,
	guid varchar(64) NOT NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	version bigint DEFAULT NULL NULL,
	service_id bigint NOT NULL,
	resource_signature varchar(128) DEFAULT NULL NULL,
	is_enabled tinyint DEFAULT 1 NOT NULL,
	service_resource_elements_text text DEFAULT NULL NULL,
	tags_text text DEFAULT NULL NULL,
	CONSTRAINT x_service_res_PK_id PRIMARY KEY CLUSTERED(id),
	CONSTRAINT x_service_res_UK_guid UNIQUE NONCLUSTERED (guid)
)
CREATE UNIQUE INDEX x_service_resource_IDX_svc_id_resource_signature ON x_service_resource(service_id, resource_signature);
GO
CREATE TABLE dbo.x_tag_resource_map(
	id bigint IDENTITY NOT NULL,
	guid varchar(64) NOT NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	tag_id bigint NOT NULL,
	res_id bigint NOT NULL,
	CONSTRAINT x_tag_res_map_PK_id PRIMARY KEY CLUSTERED(id),
	CONSTRAINT x_tag_res_map_UK_guid UNIQUE NONCLUSTERED (guid)
)
GO
CREATE TABLE dbo.x_datamask_type_def(
	id bigint IDENTITY NOT NULL,
	guid varchar(64) DEFAULT NULL NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	def_id bigint NOT NULL,
	item_id bigint NOT NULL,
	name varchar(1024) NOT NULL,
	label varchar(1024) NOT NULL,
	description varchar(1024) DEFAULT NULL NULL,
	transformer varchar(1024) DEFAULT NULL NULL,
	datamask_options varchar(1024) DEFAULT NULL NULL,
	rb_key_label varchar(1024) DEFAULT NULL NULL,
	rb_key_description varchar(1024) DEFAULT NULL NULL,
	sort_order int DEFAULT 0 NULL,
	CONSTRAINT x_datamask_type_def_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
CREATE TABLE dbo.x_policy_item_datamask(
	id bigint IDENTITY NOT NULL,
	guid varchar(64) DEFAULT NULL NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	policy_item_id bigint NOT NULL,
	type bigint NOT NULL,
	condition_expr varchar(1024) DEFAULT NULL NULL,
	value_expr varchar(1024) DEFAULT NULL NULL,
	CONSTRAINT x_policy_item_datamask_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
CREATE TABLE dbo.x_policy_item_rowfilter(
	id bigint IDENTITY NOT NULL,
	guid varchar(64) DEFAULT NULL NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	policy_item_id bigint NOT NULL,
	filter_expr varchar(1024) DEFAULT NULL NULL,
	CONSTRAINT x_policy_item_rowfilter_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
CREATE TABLE dbo.x_service_version_info(
	id bigint IDENTITY NOT NULL,
	service_id bigint NOT NULL,
	policy_version bigint NOT NULL DEFAULT 0,
	policy_update_time datetime DEFAULT NULL NULL,
	tag_version bigint NOT NULL DEFAULT 0,
	tag_update_time datetime DEFAULT NULL NULL,
	role_version bigint NOT NULL DEFAULT 0,
	role_update_time datetime DEFAULT NULL NULL,
	version bigint NOT NULL DEFAULT 1,
	CONSTRAINT x_service_version_info_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
CREATE TABLE dbo.x_plugin_info(
		id bigint IDENTITY NOT NULL,
		create_time datetime DEFAULT NULL NULL,
		update_time datetime DEFAULT NULL NULL,
		service_name varchar(255) NOT NULL,
		app_type varchar(128) NOT NULL,
		host_name varchar(255) NOT NULL,
		ip_address varchar(64) NOT NULL,
		info varchar(1024) NOT NULL,
		CONSTRAINT x_plugin_info_PK_id PRIMARY KEY CLUSTERED(id),
		CONSTRAINT x_plugin_info_UK UNIQUE NONCLUSTERED (service_name, host_name, app_type)
)
GO
CREATE TABLE dbo.x_policy_label (
		id bigint IDENTITY NOT NULL,
		guid varchar(64) DEFAULT NULL NULL,
		create_time datetime DEFAULT NULL NULL,
		update_time datetime DEFAULT NULL NULL,
		added_by_id bigint DEFAULT NULL NULL,
		upd_by_id bigint DEFAULT NULL NULL,
		label_name varchar(512) DEFAULT NULL,
		CONSTRAINT x_policy_label_PK_id PRIMARY KEY CLUSTERED(id),
		CONSTRAINT x_policy_label_UK_label_name UNIQUE NONCLUSTERED (label_name)
)
GO
CREATE TABLE dbo.x_policy_label_map (
		id bigint IDENTITY NOT NULL,
		guid varchar(64) DEFAULT NULL NULL,
		create_time datetime DEFAULT NULL NULL,
		update_time datetime DEFAULT NULL NULL,
		added_by_id bigint DEFAULT NULL NULL,
		upd_by_id bigint DEFAULT NULL NULL,
		policy_id bigint NOT NULL,
		policy_label_id bigint NOT NULL,
		CONSTRAINT x_policy_label_map_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
CREATE TABLE dbo.x_ugsync_audit_info(
		id bigint IDENTITY NOT NULL,
		create_time datetime DEFAULT NULL NULL,
		update_time datetime DEFAULT NULL NULL,
		added_by_id bigint DEFAULT NULL NULL,
		upd_by_id bigint DEFAULT NULL NULL,
		event_time datetime DEFAULT NULL NULL,
		user_name varchar(255) NOT  NULL,
		sync_source varchar(128) NOT NULL,
		no_of_new_users bigint NOT NULL,
		no_of_new_groups bigint NOT NULL,
		no_of_modified_users bigint NOT NULL,
		no_of_modified_groups bigint NOT NULL,
		sync_source_info text NOT NULL,
		session_id varchar(255) DEFAULT NULL NULL,
		CONSTRAINT x_ugsync_audit_info_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
create table dbo.x_policy_ref_resource (
	id bigint IDENTITY NOT NULL,
	guid varchar(1024) DEFAULT NULL NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	policy_id bigint NOT NULL,
	resource_def_id bigint NOT NULL,
	resource_name varchar(4000) DEFAULT NULL NULL,
	CONSTRAINT x_policy_ref_res_PK_id PRIMARY KEY CLUSTERED(id),
	CONSTRAINT x_p_ref_res_UK_polId_resDefId UNIQUE NONCLUSTERED (policy_id, resource_def_id)
)
GO
create table dbo.x_policy_ref_access_type (
		id bigint IDENTITY NOT NULL,
		guid varchar(1024) DEFAULT NULL NULL,
		create_time datetime DEFAULT NULL NULL,
		update_time datetime DEFAULT NULL NULL,
		added_by_id bigint DEFAULT NULL NULL,
		upd_by_id bigint DEFAULT NULL NULL,
		policy_id bigint NOT NULL,
		access_def_id bigint NOT NULL,
		access_type_name varchar(4000) DEFAULT NULL NULL,
		CONSTRAINT x_policy_ref_acc_PK_id PRIMARY KEY CLUSTERED(id),
		CONSTRAINT x_p_ref_acc_UK_polId_accDefId UNIQUE NONCLUSTERED (policy_id, access_def_id)
)
GO
create table dbo.x_policy_ref_condition (
		id bigint IDENTITY NOT NULL,
		guid varchar(1024) DEFAULT NULL NULL,
		create_time datetime DEFAULT NULL NULL,
		update_time datetime DEFAULT NULL NULL,
		added_by_id bigint DEFAULT NULL NULL,
		upd_by_id bigint DEFAULT NULL NULL,
		policy_id bigint NOT NULL,
		condition_def_id bigint NOT NULL,
		condition_name varchar(4000) DEFAULT NULL NULL,
		CONSTRAINT x_policy_ref_cond_PK_id PRIMARY KEY CLUSTERED(id),
		CONSTRAINT x_p_ref_cond_UK_polId_cDefId UNIQUE NONCLUSTERED (policy_id, condition_def_id)
)
GO
create table dbo.x_policy_ref_datamask_type (
		id bigint IDENTITY NOT NULL,
		guid varchar(1024) DEFAULT NULL NULL,
		create_time datetime DEFAULT NULL NULL,
		update_time datetime DEFAULT NULL NULL,
		added_by_id bigint DEFAULT NULL NULL,
		upd_by_id bigint DEFAULT NULL NULL,
		policy_id bigint NOT NULL,
		datamask_def_id bigint NOT NULL,
		datamask_type_name varchar(4000) DEFAULT NULL NULL,
		CONSTRAINT x_policy_ref_dmk_PK_id PRIMARY KEY CLUSTERED(id),
		CONSTRAINT x_p_ref_dmk_UK_polId_dDefId UNIQUE NONCLUSTERED (policy_id, datamask_def_id)
)
GO
create table dbo.x_policy_ref_user (
		id bigint IDENTITY NOT NULL,
		guid varchar(1024) DEFAULT NULL NULL,
		create_time datetime DEFAULT NULL NULL,
		update_time datetime DEFAULT NULL NULL,
		added_by_id bigint DEFAULT NULL NULL,
		upd_by_id bigint DEFAULT NULL NULL,
		policy_id bigint NOT NULL,
		user_id bigint NOT NULL,
		user_name varchar(4000) DEFAULT NULL NULL,
		CONSTRAINT x_policy_ref_user_PK_id PRIMARY KEY CLUSTERED(id),
		CONSTRAINT x_p_ref_usr_UK_polId_userId UNIQUE NONCLUSTERED (policy_id, user_id)
)
GO
create table dbo.x_policy_ref_group (
		id bigint IDENTITY NOT NULL,
		guid varchar(1024) DEFAULT NULL NULL,
		create_time datetime DEFAULT NULL NULL,
		update_time datetime DEFAULT NULL NULL,
		added_by_id bigint DEFAULT NULL NULL,
		upd_by_id bigint DEFAULT NULL NULL,
		policy_id bigint NOT NULL,
		group_id bigint NOT NULL,
		group_name varchar(4000) DEFAULT NULL NULL,
		CONSTRAINT x_policy_ref_group_PK_id PRIMARY KEY CLUSTERED(id),
		CONSTRAINT x_p_ref_grp_UK_polId_grpId UNIQUE NONCLUSTERED (policy_id, group_id)
)
GO
CREATE TABLE dbo.x_security_zone_ref_service(
        id bigint IDENTITY NOT NULL,
        create_time datetime DEFAULT NULL NULL,
        update_time datetime DEFAULT NULL NULL,
        added_by_id bigint DEFAULT NULL NULL,
        upd_by_id bigint DEFAULT NULL NULL,
        zone_id bigint DEFAULT NULL NULL,
        service_id bigint DEFAULT NULL NULL,
        service_name varchar(255) DEFAULT NULL NULL,
        CONSTRAINT x_sz_ref_service_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
CREATE TABLE dbo.x_security_zone_ref_tag_srvc(
        id bigint IDENTITY NOT NULL,
        create_time datetime DEFAULT NULL NULL,
        update_time datetime DEFAULT NULL NULL,
        added_by_id bigint DEFAULT NULL NULL,
        upd_by_id bigint DEFAULT NULL NULL,
        zone_id bigint DEFAULT NULL NULL,
        tag_srvc_id bigint DEFAULT NULL NULL,
        tag_srvc_name varchar(255) DEFAULT NULL NULL,
        CONSTRAINT x_sz_ref_tag_service_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
CREATE TABLE dbo.x_security_zone_ref_resource(
        id bigint IDENTITY NOT NULL,
        create_time datetime DEFAULT NULL NULL,
        update_time datetime DEFAULT NULL NULL,
        added_by_id bigint DEFAULT NULL NULL,
        upd_by_id bigint DEFAULT NULL NULL,
        zone_id bigint DEFAULT NULL NULL,
        resource_def_id bigint DEFAULT NULL NULL,
        resource_name varchar(255) DEFAULT NULL NULL,
        CONSTRAINT x_sz_ref_resource_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
CREATE TABLE dbo.x_security_zone_ref_user(
        id bigint IDENTITY NOT NULL,
        create_time datetime DEFAULT NULL NULL,
        update_time datetime DEFAULT NULL NULL,
        added_by_id bigint DEFAULT NULL NULL,
        upd_by_id bigint DEFAULT NULL NULL,
        zone_id bigint DEFAULT NULL NULL,
        user_id bigint DEFAULT NULL NULL,
        user_name varchar(767) DEFAULT NULL NULL,
        user_type tinyint DEFAULT NULL,
        CONSTRAINT x_sz_ref_auser_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
CREATE TABLE dbo.x_security_zone_ref_group(
        id bigint IDENTITY NOT NULL,
        create_time datetime DEFAULT NULL NULL,
        update_time datetime DEFAULT NULL NULL,
        added_by_id bigint DEFAULT NULL NULL,
        upd_by_id bigint DEFAULT NULL NULL,
        zone_id bigint DEFAULT NULL NULL,
        group_id bigint DEFAULT NULL NULL,
        group_name varchar(767) DEFAULT NULL NULL,
        group_type tinyint DEFAULT NULL,
        CONSTRAINT x_sz_ref_agroup_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
CREATE TABLE dbo.x_policy_change_log(
        id bigint IDENTITY NOT NULL,
        create_time datetime DEFAULT NULL NULL,
        service_id bigint NOT NULL,
        change_type int NOT NULL,
        policy_version bigint DEFAULT 0 NOT NULL,
        service_type varchar(256) DEFAULT NULL NULL,
        policy_type int DEFAULT NULL NULL,
        zone_name varchar(256) DEFAULT NULL NULL,
        policy_id bigint DEFAULT NULL NULL,
        policy_guid varchar(1024) DEFAULT NULL NULL,
        CONSTRAINT x_policy_change_log_PK_id PRIMARY KEY CLUSTERED(id)
)
GO

CREATE TABLE IF NOT EXISTS dbo.x_tag_change_log (
id bigint IDENTITY NOT NULL,
create_time datetime DEFAULT NULL NULL,
service_id bigint NOT NULL,
change_type int NOT NULL,
service_tags_version  bigint DEFAULT 0 NOT NULL,
service_resource_id bigint DEFAULT NULL NULL,
tag_id bigint DEFAULT NULL NULL,
CONSTRAINT x_tag_change_log_PK_id PRIMARY KEY CLUSTERED(id)
)
GO

CREATE TABLE dbo.x_role(
id bigint IDENTITY NOT NULL,
create_time datetime DEFAULT NULL NULL,
update_time datetime DEFAULT NULL NULL,
added_by_id bigint DEFAULT NULL NULL,
upd_by_id bigint  DEFAULT NULL NULL,
version bigint  DEFAULT 0 NOT NULL,
name varchar(255) NOT NULL,
description varchar(1024) DEFAULT NULL NULL,
role_options varchar(4000) DEFAULT NULL NULL,
role_text text DEFAULT NULL NULL,
CONSTRAINT x_role_PK_id PRIMARY KEY CLUSTERED(id),
CONSTRAINT x_role_UK_name UNIQUE NONCLUSTERED (name)
)
GO

CREATE TABLE dbo.x_role_ref_user(
id bigint IDENTITY NOT NULL,
create_time datetime DEFAULT NULL NULL,
update_time datetime DEFAULT NULL NULL,
added_by_id bigint  DEFAULT NULL NULL,
upd_by_id bigint DEFAULT NULL NULL,
role_id bigint NOT NULL,
user_id bigint DEFAULT NULL NULL,
user_name varchar(767) DEFAULT NULL NULL,
priv_type int DEFAULT NULL,
CONSTRAINT x_role_ref_user_PK_id PRIMARY KEY CLUSTERED(id)
)
GO

CREATE TABLE dbo.x_role_ref_group(
id bigint IDENTITY NOT NULL,
create_time datetime DEFAULT NULL NULL,
update_time datetime DEFAULT NULL NULL,
added_by_id bigint DEFAULT NULL NULL,
upd_by_id bigint DEFAULT NULL NULL,
role_id bigint NOT NULL,
group_id bigint DEFAULT NULL NULL,
group_name varchar(767) DEFAULT NULL NULL,
priv_type int DEFAULT NULL,
 CONSTRAINT x_role_ref_grp_PK_id PRIMARY KEY CLUSTERED(id)
)
GO

CREATE TABLE dbo.x_policy_ref_role(
id bigint IDENTITY NOT NULL,
create_time datetime DEFAULT NULL NULL,
update_time datetime DEFAULT NULL NULL,
added_by_id bigint DEFAULT NULL NULL,
upd_by_id bigint DEFAULT NULL NULL,
policy_id bigint NOT NULL,
role_id bigint NOT NULL,
role_name varchar(255) DEFAULT NULL NULL,
 CONSTRAINT x_pol_ref_role_PK_id PRIMARY KEY CLUSTERED(id),
 CONSTRAINT x_pol_ref_role_UK_polId_roleId UNIQUE NONCLUSTERED (policy_id, role_id)
 )
GO

CREATE TABLE dbo.x_role_ref_role(
id bigint IDENTITY NOT NULL,
create_time datetime DEFAULT NULL NULL,
update_time datetime DEFAULT NULL NULL,
added_by_id bigint DEFAULT NULL NULL,
upd_by_id bigint DEFAULT NULL NULL,
role_ref_id bigint DEFAULT NULL NULL,
role_id bigint NOT NULL,
role_name varchar(255) DEFAULT NULL NULL,
priv_type int DEFAULT NULL,
 CONSTRAINT x_role_ref_role_PK_id PRIMARY KEY CLUSTERED(id)
 )
GO

ALTER TABLE dbo.x_asset ADD CONSTRAINT x_asset_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user(id)
GO
ALTER TABLE dbo.x_asset ADD CONSTRAINT x_asset_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_audit_map ADD CONSTRAINT x_audit_map_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_audit_map ADD CONSTRAINT x_audit_map_FK_group_id FOREIGN KEY(group_id) REFERENCES dbo.x_group (id)
GO
ALTER TABLE dbo.x_audit_map ADD CONSTRAINT x_audit_map_FK_res_id FOREIGN KEY(res_id) REFERENCES dbo.x_resource (id)
GO
ALTER TABLE dbo.x_audit_map ADD CONSTRAINT x_audit_map_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_audit_map ADD CONSTRAINT x_audit_map_FK_user_id FOREIGN KEY(user_id) REFERENCES dbo.x_user (id)
GO
ALTER TABLE dbo.x_auth_sess ADD CONSTRAINT x_auth_sess_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_auth_sess ADD CONSTRAINT x_auth_sess_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_auth_sess ADD CONSTRAINT x_auth_sess_FK_user_id FOREIGN KEY(user_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_cred_store ADD CONSTRAINT x_cred_store_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_cred_store ADD CONSTRAINT x_cred_store_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_db_base ADD CONSTRAINT x_db_base_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_db_base ADD CONSTRAINT x_db_base_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_group ADD CONSTRAINT x_group_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_group ADD CONSTRAINT x_group_FK_cred_store_id FOREIGN KEY(cred_store_id) REFERENCES dbo.x_cred_store (id)
GO
ALTER TABLE dbo.x_group ADD CONSTRAINT x_group_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_group_groups ADD CONSTRAINT x_group_groups_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_group_groups ADD CONSTRAINT x_group_groups_FK_group_id FOREIGN KEY(group_id) REFERENCES dbo.x_group (id)
GO
ALTER TABLE dbo.x_group_groups ADD CONSTRAINT x_group_groups_FK_p_group_id FOREIGN KEY(p_group_id) REFERENCES dbo.x_group (id)
GO
ALTER TABLE dbo.x_group_users ADD CONSTRAINT x_group_users_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_group_users ADD CONSTRAINT x_group_users_FK_p_group_id FOREIGN KEY(p_group_id) REFERENCES dbo.x_group (id)
GO
ALTER TABLE dbo.x_group_users ADD CONSTRAINT x_group_users_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_group_users ADD CONSTRAINT x_group_users_FK_user_id FOREIGN KEY(user_id) REFERENCES dbo.x_user (id)
GO
ALTER TABLE dbo.x_group_users ADD CONSTRAINT x_group_users_UK_uid_gname UNIQUE NONCLUSTERED (user_id,group_name)
GO
ALTER TABLE dbo.x_perm_map ADD CONSTRAINT x_perm_map_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_perm_map ADD CONSTRAINT x_perm_map_FK_group_id FOREIGN KEY(group_id) REFERENCES dbo.x_group (id)
GO
ALTER TABLE dbo.x_perm_map ADD CONSTRAINT x_perm_map_FK_res_id FOREIGN KEY(res_id) REFERENCES dbo.x_resource (id)
GO
ALTER TABLE dbo.x_perm_map ADD CONSTRAINT x_perm_map_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_perm_map ADD CONSTRAINT x_perm_map_FK_user_id FOREIGN KEY(user_id) REFERENCES dbo.x_user (id)
GO
ALTER TABLE dbo.x_policy_export_audit ADD CONSTRAINT x_policy_export_audit_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_export_audit ADD CONSTRAINT x_policy_export_audit_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_portal_user ADD CONSTRAINT x_portal_user_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_portal_user ADD CONSTRAINT x_portal_user_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_portal_user_role ADD CONSTRAINT x_portal_user_role_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_portal_user_role ADD CONSTRAINT x_portal_user_role_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_portal_user_role ADD CONSTRAINT x_portal_user_role_FK_user_id FOREIGN KEY(user_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_resource ADD CONSTRAINT x_resource_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_resource ADD CONSTRAINT x_resource_FK_asset_id FOREIGN KEY(asset_id) REFERENCES dbo.x_asset (id)
GO
ALTER TABLE dbo.x_resource ADD CONSTRAINT x_resource_FK_parent_id FOREIGN KEY(parent_id) REFERENCES dbo.x_resource (id)
GO
ALTER TABLE dbo.x_resource ADD CONSTRAINT x_resource_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_trx_log ADD CONSTRAINT x_trx_log_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_trx_log ADD CONSTRAINT x_trx_log_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_user ADD CONSTRAINT x_user_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_user ADD CONSTRAINT x_user_FK_cred_store_id FOREIGN KEY(cred_store_id) REFERENCES dbo.x_cred_store (id)
GO
ALTER TABLE dbo.x_user ADD CONSTRAINT x_user_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_service_def ADD CONSTRAINT x_service_def_FK_added_by_id FOREIGN KEY(added_by_id)REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_service_def ADD CONSTRAINT x_service_def_FK_upd_by_id FOREIGN KEY(upd_by_id)REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_service ADD CONSTRAINT x_service_FK_added_by_id FOREIGN KEY(added_by_id)REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_service ADD CONSTRAINT x_service_FK_upd_by_id FOREIGN KEY(upd_by_id)REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_service ADD CONSTRAINT x_service_FK_type FOREIGN KEY(type)REFERENCES dbo.x_service_def (id)
GO
ALTER TABLE dbo.x_policy ADD CONSTRAINT x_policy_FK_added_by_id FOREIGN KEY(added_by_id)REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy ADD CONSTRAINT x_policy_FK_upd_by_id FOREIGN KEY(upd_by_id)REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy ADD CONSTRAINT x_policy_FK_service FOREIGN KEY(service)REFERENCES dbo.x_service (id)
GO
ALTER TABLE dbo.x_service_config_def ADD CONSTRAINT x_service_config_def_FK_defid FOREIGN KEY(def_id)REFERENCES dbo.x_service_def (id)
GO
ALTER TABLE dbo.x_service_config_def ADD CONSTRAINT x_service_conf_def_FK_added_by FOREIGN KEY(added_by_id)REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_service_config_def ADD CONSTRAINT x_service_conf_def_FK_upd_by FOREIGN KEY(upd_by_id)REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_resource_def ADD CONSTRAINT x_resource_def_FK_parent FOREIGN KEY(parent)REFERENCES dbo.x_resource_def (id)
GO
ALTER TABLE dbo.x_resource_def ADD CONSTRAINT x_resource_def_FK_defid FOREIGN KEY(def_id)REFERENCES dbo.x_service_def (id)
GO
ALTER TABLE dbo.x_resource_def ADD CONSTRAINT x_resource_def_FK_added_by FOREIGN KEY(added_by_id)REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_resource_def ADD CONSTRAINT x_resource_def_FK_upd_by FOREIGN KEY(upd_by_id)REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_access_type_def ADD CONSTRAINT x_access_type_def_FK_defid FOREIGN KEY(def_id)REFERENCES dbo.x_service_def (id)
GO
ALTER TABLE dbo.x_access_type_def ADD CONSTRAINT x_access_type_def_FK_added_by FOREIGN KEY(added_by_id)REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_access_type_def ADD CONSTRAINT x_access_type_def_FK_upd_by FOREIGN KEY(upd_by_id)REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_access_type_def_grants ADD CONSTRAINT x_atd_grants_FK_atdid FOREIGN KEY(atd_id)REFERENCES dbo.x_access_type_def (id)
GO
ALTER TABLE dbo.x_access_type_def_grants ADD CONSTRAINT x_access_type_def_grants_FK_added_by FOREIGN KEY(added_by_id)REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_access_type_def_grants ADD CONSTRAINT x_access_type_def_grants_FK_upd_by FOREIGN KEY(upd_by_id)REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_condition_def ADD CONSTRAINT x_policy_condition_def_FK_defid FOREIGN KEY(def_id)REFERENCES dbo.x_service_def (id)
GO
ALTER TABLE dbo.x_policy_condition_def ADD CONSTRAINT x_policy_condition_def_FK_added_by FOREIGN KEY(added_by_id)REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_condition_def ADD CONSTRAINT x_policy_condition_def_FK_upd_by FOREIGN KEY(upd_by_id)REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_context_enricher_def ADD CONSTRAINT x_context_enricher_def_FK_defid FOREIGN KEY(def_id)REFERENCES dbo.x_service_def (id)
GO
ALTER TABLE dbo.x_context_enricher_def ADD CONSTRAINT x_context_enricher_def_FK_added_by_id FOREIGN KEY(added_by_id)REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_context_enricher_def ADD CONSTRAINT x_context_enricher_def_FK_upd_by_id FOREIGN KEY(upd_by_id)REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_enum_def ADD CONSTRAINT x_enum_def_FK_defid FOREIGN KEY(def_id)REFERENCES dbo.x_service_def (id)
GO
ALTER TABLE dbo.x_enum_element_def ADD CONSTRAINT x_enum_element_def_FK_defid FOREIGN KEY(enum_def_id)REFERENCES dbo.x_enum_def (id)
GO
ALTER TABLE dbo.x_enum_def ADD CONSTRAINT x_enum_def_FK_added_by FOREIGN KEY(added_by_id)REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_enum_def ADD CONSTRAINT x_enum_def_FK_upd_by FOREIGN KEY(upd_by_id)REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_service_config_map ADD CONSTRAINT x_service_config_map_FK_ FOREIGN KEY(service)REFERENCES dbo.x_service (id)
GO
ALTER TABLE dbo.x_service_config_map ADD CONSTRAINT x_service_config_map_FK_added_by FOREIGN KEY(added_by_id)REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_service_config_map ADD CONSTRAINT x_service_config_map_FK_upd_by FOREIGN KEY(upd_by_id)REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_resource ADD CONSTRAINT x_policy_resource_FK_policy_id FOREIGN KEY(policy_id)REFERENCES dbo.x_policy (id)
GO
ALTER TABLE dbo.x_policy_resource ADD CONSTRAINT x_policy_resource_FK_res_def_id FOREIGN KEY(res_def_id)REFERENCES dbo.x_resource_def (id)
GO
ALTER TABLE dbo.x_policy_resource ADD CONSTRAINT x_policy_resource_FK_added_by FOREIGN KEY(added_by_id)REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_resource ADD CONSTRAINT x_policy_resource_FK_upd_by FOREIGN KEY(upd_by_id)REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_resource_map ADD CONSTRAINT x_policy_resource_map_FK_resource_id FOREIGN KEY(resource_id)REFERENCES dbo.x_policy_resource (id)
GO
ALTER TABLE dbo.x_policy_resource_map ADD CONSTRAINT x_policy_resource_map_FK_added_by FOREIGN KEY(added_by_id)REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_resource_map ADD CONSTRAINT x_policy_resource_map_FK_upd_by FOREIGN KEY(upd_by_id)REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_item ADD CONSTRAINT x_policy_item_FK_policy_id FOREIGN KEY(policy_id)REFERENCES dbo.x_policy (id)
GO
ALTER TABLE dbo.x_policy_item ADD CONSTRAINT x_policy_item_FK_added_by FOREIGN KEY(added_by_id)REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_item ADD CONSTRAINT x_policy_item_FK_upd_by FOREIGN KEY(upd_by_id)REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_item_access ADD CONSTRAINT x_policy_item_access_FK_pi_id FOREIGN KEY(policy_item_id)REFERENCES dbo.x_policy_item (id)
GO
ALTER TABLE dbo.x_policy_item_access ADD CONSTRAINT x_policy_item_access_FK_atd_id FOREIGN KEY(type)REFERENCES dbo.x_access_type_def (id)
GO
ALTER TABLE dbo.x_policy_item_access ADD CONSTRAINT x_policy_item_access_FK_added_by FOREIGN KEY(added_by_id)REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_item_access ADD CONSTRAINT x_policy_item_access_FK_upd_by FOREIGN KEY(upd_by_id)REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_item_condition ADD CONSTRAINT x_policy_item_condition_FK_pi_id FOREIGN KEY(policy_item_id)REFERENCES dbo.x_policy_item (id)
GO
ALTER TABLE dbo.x_policy_item_condition ADD CONSTRAINT x_policy_item_condition_FK_pcd_id FOREIGN KEY(type)REFERENCES dbo.x_policy_condition_def (id)
GO
ALTER TABLE dbo.x_policy_item_condition ADD CONSTRAINT x_policy_item_condition_FK_added_by FOREIGN KEY(added_by_id)REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_item_condition ADD CONSTRAINT x_policy_item_condition_FK_upd_by FOREIGN KEY(upd_by_id)REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_item_user_perm ADD CONSTRAINT x_policy_item_user_perm_FK_pi_id FOREIGN KEY(policy_item_id)REFERENCES dbo.x_policy_item (id)
GO
ALTER TABLE dbo.x_policy_item_user_perm ADD CONSTRAINT x_policy_item_user_perm_FK_user_id FOREIGN KEY(user_id)REFERENCES dbo.x_user (id)
GO
ALTER TABLE dbo.x_policy_item_user_perm ADD CONSTRAINT x_policy_item_user_perm_FK_added_by FOREIGN KEY(added_by_id)REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_item_user_perm ADD CONSTRAINT x_policy_item_user_perm_FK_upd_by FOREIGN KEY(upd_by_id)REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_item_group_perm ADD CONSTRAINT x_policy_item_group_perm_FK_pi_id FOREIGN KEY(policy_item_id)REFERENCES dbo.x_policy_item (id)
GO
ALTER TABLE dbo.x_policy_item_group_perm ADD CONSTRAINT x_policy_item_group_perm_FK_group_id FOREIGN KEY(group_id)REFERENCES dbo.x_group (id)
GO
ALTER TABLE dbo.x_policy_item_group_perm ADD CONSTRAINT x_policy_item_group_perm_FK_added_by FOREIGN KEY(added_by_id)REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_item_group_perm ADD CONSTRAINT x_policy_item_group_perm_FK_upd_by FOREIGN KEY(upd_by_id)REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_user_module_perm ADD CONSTRAINT x_user_module_perm_FK_moduleid FOREIGN KEY(module_id) REFERENCES dbo.x_modules_master(id)
GO
ALTER TABLE dbo.x_user_module_perm ADD CONSTRAINT x_user_module_perm_FK_userid FOREIGN KEY(user_id) REFERENCES dbo.x_portal_user(id)
GO
ALTER TABLE dbo.x_group_module_perm ADD CONSTRAINT x_grp_module_perm_FK_module_id FOREIGN KEY(module_id) REFERENCES dbo.x_modules_master(id)
GO
ALTER TABLE dbo.x_group_module_perm ADD CONSTRAINT x_grp_module_perm_FK_group_id FOREIGN KEY(group_id) REFERENCES dbo.x_group(id)
GO
ALTER TABLE dbo.x_tag_def ADD CONSTRAINT x_tag_def_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_tag_def ADD CONSTRAINT x_tag_def_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_tag ADD CONSTRAINT x_tag_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_tag ADD CONSTRAINT x_tag_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_tag ADD CONSTRAINT x_tag_FK_type FOREIGN KEY(type) REFERENCES dbo.x_tag_def (id)
GO
ALTER TABLE dbo.x_service_resource ADD CONSTRAINT x_service_res_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_service_resource ADD CONSTRAINT x_service_res_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_service_resource ADD CONSTRAINT x_service_res_FK_service_id FOREIGN KEY(service_id) REFERENCES dbo.x_service (id)
GO
ALTER TABLE dbo.x_tag_resource_map ADD CONSTRAINT x_tag_res_map_FK_tag_id FOREIGN KEY(tag_id) REFERENCES dbo.x_tag (id)
GO
ALTER TABLE dbo.x_tag_resource_map ADD CONSTRAINT x_tag_res_map_FK_res_id FOREIGN KEY(res_id) REFERENCES dbo.x_service_resource (id)
GO
ALTER TABLE dbo.x_tag_resource_map ADD CONSTRAINT x_tag_res_map_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_tag_resource_map ADD CONSTRAINT x_tag_res_map_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_service ADD CONSTRAINT x_service_FK_tag_service FOREIGN KEY(tag_service) REFERENCES dbo.x_service (id)
GO
ALTER TABLE dbo.x_datamask_type_def ADD CONSTRAINT x_datamask_type_def_FK_def_id FOREIGN KEY(def_id) REFERENCES dbo.x_service_def (id)
GO
ALTER TABLE dbo.x_datamask_type_def ADD CONSTRAINT x_datamask_type_def_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_datamask_type_def ADD CONSTRAINT x_datamask_type_def_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_item_datamask ADD CONSTRAINT x_policy_item_datamask_FK_policy_item_id FOREIGN KEY(policy_item_id) REFERENCES dbo.x_policy_item (id)
GO
ALTER TABLE dbo.x_policy_item_datamask ADD CONSTRAINT x_policy_item_datamask_FK_type FOREIGN KEY(type) REFERENCES dbo.x_datamask_type_def (id)
GO
ALTER TABLE dbo.x_policy_item_datamask ADD CONSTRAINT x_policy_item_datamask_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_item_datamask ADD CONSTRAINT x_policy_item_datamask_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_item_rowfilter ADD CONSTRAINT x_policy_item_rowfilter_FK_policy_item_id FOREIGN KEY(policy_item_id) REFERENCES dbo.x_policy_item (id)
GO
ALTER TABLE dbo.x_policy_item_rowfilter ADD CONSTRAINT x_policy_item_rowfilter_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_item_rowfilter ADD CONSTRAINT x_policy_item_rowfilter_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_service_version_info ADD CONSTRAINT x_service_version_info_service_id FOREIGN KEY(service_id) REFERENCES dbo.x_service (id)
GO
ALTER TABLE dbo.x_policy_label ADD CONSTRAINT x_policy_label_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_label ADD CONSTRAINT x_policy_label_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_label_map ADD CONSTRAINT x_policy_label_map_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_label_map ADD CONSTRAINT x_policy_label_map_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_label_map ADD CONSTRAINT x_policy_label_map_FK_policy_id FOREIGN KEY (policy_id) REFERENCES dbo.x_policy (id)
GO
ALTER TABLE dbo.x_policy_label_map ADD CONSTRAINT x_policy_label_map_FK_policy_label_id FOREIGN KEY (policy_label_id) REFERENCES dbo.x_policy_label (id)
GO
ALTER TABLE dbo.x_policy_label_map ADD CONSTRAINT x_policy_label_map$x_policy_label_map_pid_plid UNIQUE (policy_id, policy_label_id)
GO
ALTER TABLE dbo.x_policy_ref_resource ADD CONSTRAINT x_policy_ref_resource_FK_policy_id FOREIGN KEY (policy_id) REFERENCES dbo.x_policy (id)
GO
ALTER TABLE dbo.x_policy_ref_resource ADD CONSTRAINT x_policy_ref_resource_FK_resource_def_id FOREIGN KEY (resource_def_id) REFERENCES dbo.x_resource_def (id)
GO
ALTER TABLE dbo.x_policy_ref_resource ADD CONSTRAINT x_policy_ref_resource_FK_added_by FOREIGN KEY (added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_ref_resource ADD CONSTRAINT x_policy_ref_resource_FK_upd_by FOREIGN KEY (upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_ref_access_type ADD CONSTRAINT x_policy_ref_access_type_FK_policy_id FOREIGN KEY (policy_id) REFERENCES dbo.x_policy (id)
GO
ALTER TABLE dbo.x_policy_ref_access_type ADD CONSTRAINT x_policy_ref_access_type_FK_access_def_id FOREIGN KEY (access_def_id) REFERENCES dbo.x_access_type_def (id)
GO
ALTER TABLE dbo.x_policy_ref_access_type ADD CONSTRAINT x_policy_ref_access_type_FK_added_by FOREIGN KEY (added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_ref_access_type ADD CONSTRAINT x_policy_ref_access_type_FK_upd_by FOREIGN KEY (upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_ref_condition ADD CONSTRAINT x_policy_ref_condition_FK_policy_id FOREIGN KEY (policy_id) REFERENCES dbo.x_policy (id)
GO
ALTER TABLE dbo.x_policy_ref_condition ADD CONSTRAINT x_policy_ref_condition_FK_condition_def_id FOREIGN KEY (condition_def_id) REFERENCES dbo.x_policy_condition_def (id)
GO
ALTER TABLE dbo.x_policy_ref_condition ADD CONSTRAINT x_policy_ref_condition_FK_added_by FOREIGN KEY (added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_ref_condition ADD CONSTRAINT x_policy_ref_condition_FK_upd_by FOREIGN KEY (upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_ref_datamask_type ADD CONSTRAINT x_policy_ref_datamask_type_FK_policy_id FOREIGN KEY (policy_id) REFERENCES dbo.x_policy (id)
GO
ALTER TABLE dbo.x_policy_ref_datamask_type ADD CONSTRAINT x_policy_ref_datamask_type_FK_datamask_def_id FOREIGN KEY (datamask_def_id) REFERENCES dbo.x_datamask_type_def (id)
GO
ALTER TABLE dbo.x_policy_ref_datamask_type ADD CONSTRAINT x_policy_ref_datamask_type_FK_added_by FOREIGN KEY (added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_ref_datamask_type ADD CONSTRAINT x_policy_ref_datamask_type_FK_upd_by FOREIGN KEY (upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_ref_user ADD CONSTRAINT x_policy_ref_user_FK_policy_id FOREIGN KEY (policy_id) REFERENCES dbo.x_policy (id)
GO
ALTER TABLE dbo.x_policy_ref_user ADD CONSTRAINT x_policy_ref_user_FK_user_id FOREIGN KEY (user_id) REFERENCES dbo.x_user (id)
GO
ALTER TABLE dbo.x_policy_ref_user ADD CONSTRAINT x_policy_ref_user_FK_added_by FOREIGN KEY (added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_ref_user ADD CONSTRAINT x_policy_ref_user_FK_upd_by FOREIGN KEY (upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_ref_group ADD CONSTRAINT x_policy_ref_group_FK_policy_id FOREIGN KEY (policy_id) REFERENCES dbo.x_policy (id)
GO
ALTER TABLE dbo.x_policy_ref_group ADD CONSTRAINT x_policy_ref_group_FK_group_id FOREIGN KEY (group_id) REFERENCES dbo.x_group (id)
GO
ALTER TABLE dbo.x_policy_ref_group ADD CONSTRAINT x_policy_ref_group_FK_added_by FOREIGN KEY (added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_ref_group ADD CONSTRAINT x_policy_ref_group_FK_upd_by FOREIGN KEY (upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_security_zone ADD CONSTRAINT x_security_zone_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_security_zone ADD CONSTRAINT x_security_zone_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_ranger_global_state ADD CONSTRAINT x_ranger_global_state_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_ranger_global_state ADD CONSTRAINT x_ranger_global_state_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy ADD CONSTRAINT x_policy_FK_zone_id FOREIGN KEY(zone_id) REFERENCES dbo.x_security_zone (id)
GO
ALTER TABLE dbo.x_security_zone_ref_service ADD CONSTRAINT x_sz_ref_service_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_security_zone_ref_service ADD CONSTRAINT x_sz_ref_service_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_security_zone_ref_service ADD CONSTRAINT x_sz_ref_service_FK_zone_id FOREIGN KEY(zone_id) REFERENCES dbo.x_security_zone (id)
GO
ALTER TABLE dbo.x_security_zone_ref_service ADD CONSTRAINT x_sz_ref_service_FK_service_id FOREIGN KEY(service_id) REFERENCES dbo.x_service (id)
GO
ALTER TABLE dbo.x_security_zone_ref_service ADD CONSTRAINT x_sz_ref_service_FK_service_name FOREIGN KEY(service_name) REFERENCES dbo.x_service (name)
GO
ALTER TABLE dbo.x_security_zone_ref_tag_srvc ADD CONSTRAINT x_sz_ref_tag_service_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_security_zone_ref_tag_srvc ADD CONSTRAINT x_sz_ref_tag_service_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_security_zone_ref_tag_srvc ADD CONSTRAINT x_sz_ref_tag_service_FK_zone_id FOREIGN KEY(zone_id) REFERENCES dbo.x_security_zone (id)
GO
ALTER TABLE dbo.x_security_zone_ref_tag_srvc ADD CONSTRAINT x_sz_ref_tag_service_FK_tag_service_id FOREIGN KEY(tag_service_id) REFERENCES dbo.x_service (id)
GO
ALTER TABLE dbo.x_security_zone_ref_tag_srvc ADD CONSTRAINT x_sz_ref_tag_service_FK_tag_service_name FOREIGN KEY(tag_service_name) REFERENCES dbo.x_service (name)
GO
ALTER TABLE dbo.x_security_zone_ref_resource ADD CONSTRAINT x_sz_ref_resource_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_security_zone_ref_resource ADD CONSTRAINT x_sz_ref_resource_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_security_zone_ref_resource ADD CONSTRAINT x_sz_ref_resource_FK_zone_id FOREIGN KEY(zone_id) REFERENCES dbo.x_security_zone (id)
GO
ALTER TABLE dbo.x_security_zone_ref_resource ADD CONSTRAINT x_sz_ref_resource_FK_service_id FOREIGN KEY(resource_def_id) REFERENCES dbo.x_resource_def (id)
GO
ALTER TABLE dbo.x_security_zone_ref_user ADD CONSTRAINT x_sz_ref_user_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_security_zone_ref_user ADD CONSTRAINT x_sz_ref_user_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_security_zone_ref_user ADD CONSTRAINT x_sz_ref_user_FK_zone_id FOREIGN KEY(zone_id) REFERENCES dbo.x_security_zone (id)
GO
ALTER TABLE dbo.x_security_zone_ref_user ADD CONSTRAINT x_sz_ref_user_FK_user_id FOREIGN KEY(user_id) REFERENCES dbo.x_user (id)
GO
ALTER TABLE dbo.x_security_zone_ref_user ADD CONSTRAINT x_sz_ref_user_FK_user_name FOREIGN KEY(user_name) REFERENCES dbo.x_user (user_name)
GO
ALTER TABLE dbo.x_security_zone_ref_group ADD CONSTRAINT x_sz_ref_grp_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_security_zone_ref_group ADD CONSTRAINT x_sz_ref_grp_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_security_zone_ref_group ADD CONSTRAINT x_sz_ref_grp_FK_zone_id FOREIGN KEY(zone_id) REFERENCES dbo.x_security_zone (id)
GO
ALTER TABLE dbo.x_security_zone_ref_group ADD CONSTRAINT x_sz_ref_grp_FK_group_id FOREIGN KEY(group_id) REFERENCES dbo.x_group (id)
GO

ALTER TABLE dbo.x_role_ref_role ADD CONSTRAINT x_role_ref_role_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_role_ref_role ADD CONSTRAINT x_role_ref_role_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_role_ref_role ADD CONSTRAINT x_role_ref_role_FK_role_ref_id FOREIGN KEY (role_ref_id) REFERENCES dbo.x_role (id)
GO
ALTER TABLE dbo.x_policy_ref_role ADD CONSTRAINT x_pol_ref_role_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_ref_role ADD CONSTRAINT x_pol_ref_role_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_policy_ref_role ADD CONSTRAINT x_pol_ref_role_FK_policy_id FOREIGN KEY (policy_id) REFERENCES dbo.x_policy (id)
GO
ALTER TABLE dbo.x_policy_ref_role ADD CONSTRAINT x_pol_ref_role_FK_role_id FOREIGN KEY (role_id) REFERENCES dbo.x_role (id)
GO
ALTER TABLE dbo.x_role_ref_group ADD CONSTRAINT x_role_ref_grp_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_role_ref_group ADD CONSTRAINT x_role_ref_grp_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_role_ref_group ADD CONSTRAINT x_role_ref_grp_FK_role_id FOREIGN KEY (role_id) REFERENCES dbo.x_role (id)
GO
ALTER TABLE dbo.x_role_ref_group ADD CONSTRAINT x_role_ref_grp_FK_group_id FOREIGN KEY (group_id) REFERENCES dbo.x_group (id)
GO
ALTER TABLE dbo.x_role_ref_user ADD CONSTRAINT x_role_ref_user_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_role_ref_user ADD CONSTRAINT x_role_ref_user_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_role_ref_user ADD CONSTRAINT x_role_ref_user_FK_role_id FOREIGN KEY (role_id) REFERENCES dbo.x_role (id)
GO
ALTER TABLE dbo.x_role_ref_user ADD CONSTRAINT x_role_ref_user_FK_user_id FOREIGN KEY (user_id) REFERENCES dbo.x_user (id)
GO
ALTER TABLE dbo.x_role ADD CONSTRAINT x_role_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES dbo.x_portal_user (id)
GO
ALTER TABLE dbo.x_role ADD CONSTRAINT x_role_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES dbo.x_portal_user (id)

GO
CREATE NONCLUSTERED INDEX x_asset_cr_time ON dbo.x_asset(create_time ASC)
GO
CREATE NONCLUSTERED INDEX x_asset_FK_added_by_id ON dbo.x_asset(added_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_asset_FK_upd_by_id ON dbo.x_asset(upd_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_asset_up_time ON dbo.x_asset(update_time ASC)
GO
CREATE NONCLUSTERED INDEX x_audit_map_cr_time ON dbo.x_audit_map(create_time ASC)
GO
CREATE NONCLUSTERED INDEX x_audit_map_FK_added_by_id ON dbo.x_audit_map(added_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_audit_map_FK_group_id ON dbo.x_audit_map(group_id ASC)
GO
CREATE NONCLUSTERED INDEX x_audit_map_FK_res_id ON dbo.x_audit_map(res_id ASC)
GO
CREATE NONCLUSTERED INDEX x_audit_map_FK_upd_by_id ON dbo.x_audit_map(upd_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_audit_map_FK_user_id ON dbo.x_audit_map(user_id ASC)
GO
CREATE NONCLUSTERED INDEX x_audit_map_up_time ON dbo.x_audit_map(update_time ASC)
GO
CREATE NONCLUSTERED INDEX x_auth_sess_cr_time ON dbo.x_auth_sess(create_time ASC)
GO
CREATE NONCLUSTERED INDEX x_auth_sess_FK_added_by_id ON dbo.x_auth_sess(added_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_auth_sess_FK_upd_by_id ON dbo.x_auth_sess(upd_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_auth_sess_FK_user_id ON dbo.x_auth_sess(user_id ASC)
GO
CREATE NONCLUSTERED INDEX x_auth_sess_up_time ON dbo.x_auth_sess(update_time ASC)
GO
CREATE NONCLUSTERED INDEX x_cred_store_cr_time ON dbo.x_cred_store(create_time ASC)
GO
CREATE NONCLUSTERED INDEX x_cred_store_FK_added_by_id ON dbo.x_cred_store(added_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_cred_store_FK_upd_by_id ON dbo.x_cred_store(upd_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_cred_store_up_time ON dbo.x_cred_store(update_time ASC)
GO
CREATE NONCLUSTERED INDEX x_db_base_cr_time ON dbo.x_db_base(create_time ASC)
GO
CREATE NONCLUSTERED INDEX x_db_base_FK_added_by_id ON dbo.x_db_base(added_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_db_base_FK_upd_by_id ON dbo.x_db_base(upd_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_db_base_up_time ON dbo.x_db_base(update_time ASC)
GO
CREATE NONCLUSTERED INDEX x_group_cr_time ON dbo.x_group(create_time ASC)
GO
CREATE NONCLUSTERED INDEX x_group_FK_added_by_id ON dbo.x_group(added_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_group_FK_cred_store_id ON dbo.x_group(cred_store_id ASC)
GO
CREATE NONCLUSTERED INDEX x_group_FK_upd_by_id ON dbo.x_group(upd_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_group_groups_cr_time ON dbo.x_group_groups(create_time ASC)
GO
CREATE NONCLUSTERED INDEX x_group_groups_FK_added_by_id ON dbo.x_group_groups(added_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_group_groups_FK_group_id ON dbo.x_group_groups(group_id ASC)
GO
CREATE NONCLUSTERED INDEX x_group_groups_FK_p_group_id ON dbo.x_group_groups(p_group_id ASC)
GO
CREATE NONCLUSTERED INDEX x_group_groups_FK_upd_by_id ON dbo.x_group_groups(upd_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_group_groups_up_time ON dbo.x_group_groups(update_time ASC)
GO
CREATE NONCLUSTERED INDEX x_group_up_time ON dbo.x_group(update_time ASC)
GO
CREATE NONCLUSTERED INDEX x_group_users_cr_time ON dbo.x_group_users(create_time ASC)
GO
CREATE NONCLUSTERED INDEX x_group_users_FK_added_by_id ON dbo.x_group_users(added_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_group_users_FK_p_group_id ON dbo.x_group_users(p_group_id ASC)
GO
CREATE NONCLUSTERED INDEX x_group_users_FK_upd_by_id ON dbo.x_group_users(upd_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_group_users_FK_user_id ON dbo.x_group_users(user_id ASC)
GO
CREATE NONCLUSTERED INDEX x_group_users_up_time ON dbo.x_group_users(update_time ASC)
GO
CREATE NONCLUSTERED INDEX x_perm_map_cr_time ON dbo.x_perm_map(create_time ASC)
GO
CREATE NONCLUSTERED INDEX x_perm_map_FK_added_by_id ON dbo.x_perm_map(added_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_perm_map_FK_group_id ON dbo.x_perm_map(group_id ASC)
GO
CREATE NONCLUSTERED INDEX x_perm_map_FK_res_id ON dbo.x_perm_map(res_id ASC)
GO
CREATE NONCLUSTERED INDEX x_perm_map_FK_upd_by_id ON dbo.x_perm_map(upd_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_perm_map_FK_user_id ON dbo.x_perm_map(user_id ASC)
GO
CREATE NONCLUSTERED INDEX x_perm_map_up_time ON dbo.x_perm_map(update_time ASC)
GO
CREATE NONCLUSTERED INDEX x_policy_export_audit_cr_time ON dbo.x_policy_export_audit(create_time ASC)
GO
CREATE NONCLUSTERED INDEX x_policy_export_audit_FK_added_by_id ON dbo.x_policy_export_audit(added_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_policy_export_audit_FK_upd_by_id ON dbo.x_policy_export_audit(upd_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_policy_export_audit_up_time ON dbo.x_policy_export_audit(update_time ASC)
GO
CREATE NONCLUSTERED INDEX x_portal_user_cr_time ON dbo.x_portal_user(create_time ASC)
GO
CREATE NONCLUSTERED INDEX x_portal_user_email ON dbo.x_portal_user(email ASC)
GO
CREATE NONCLUSTERED INDEX x_portal_user_FK_added_by_id ON dbo.x_portal_user(added_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_portal_user_FK_upd_by_id ON dbo.x_portal_user(upd_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_portal_user_name ON dbo.x_portal_user(first_name ASC)
GO
CREATE NONCLUSTERED INDEX x_portal_user_role_cr_time ON dbo.x_portal_user_role(create_time ASC)
GO
CREATE NONCLUSTERED INDEX x_portal_user_role_FK_added_by_id ON dbo.x_portal_user_role(added_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_portal_user_role_FK_upd_by_id ON dbo.x_portal_user_role(upd_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_portal_user_role_FK_user_id ON dbo.x_portal_user_role(user_id ASC)
GO
CREATE NONCLUSTERED INDEX x_portal_user_role_up_time ON dbo.x_portal_user_role(update_time ASC)
GO
CREATE NONCLUSTERED INDEX x_portal_user_up_time ON dbo.x_portal_user(update_time ASC)
GO
CREATE NONCLUSTERED INDEX x_resource_cr_time ON dbo.x_resource(create_time ASC)
GO
CREATE NONCLUSTERED INDEX x_resource_FK_added_by_id ON dbo.x_resource(added_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_resource_FK_asset_id ON dbo.x_resource(asset_id ASC)
GO
CREATE NONCLUSTERED INDEX x_resource_FK_parent_id ON dbo.x_resource(parent_id ASC)
GO
CREATE NONCLUSTERED INDEX x_resource_FK_upd_by_id ON dbo.x_resource(upd_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_resource_up_time ON dbo.x_resource(update_time ASC)
GO
CREATE NONCLUSTERED INDEX x_trx_log_cr_time ON dbo.x_trx_log(create_time ASC)
GO
CREATE NONCLUSTERED INDEX x_trx_log_FK_added_by_id ON dbo.x_trx_log(added_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_trx_log_FK_upd_by_id ON dbo.x_trx_log(upd_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_trx_log_up_time ON dbo.x_trx_log(update_time ASC)
GO
CREATE NONCLUSTERED INDEX x_user_cr_time ON dbo.x_user(create_time ASC)
GO
CREATE NONCLUSTERED INDEX x_user_FK_added_by_id ON dbo.x_user(added_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_user_FK_cred_store_id ON dbo.x_user(cred_store_id ASC)
GO
CREATE NONCLUSTERED INDEX x_user_FK_upd_by_id ON dbo.x_user(upd_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_user_up_time ON dbo.x_user(update_time ASC)
GO
CREATE NONCLUSTERED INDEX xa_access_audit_cr_time ON dbo.xa_access_audit(create_time ASC)
GO
CREATE NONCLUSTERED INDEX xa_access_audit_event_time ON dbo.xa_access_audit(event_time ASC)
GO
CREATE NONCLUSTERED INDEX xa_access_audit_added_by_id ON dbo.xa_access_audit(added_by_id ASC)
GO
CREATE NONCLUSTERED INDEX xa_access_audit_upd_by_id ON dbo.xa_access_audit(upd_by_id ASC)
GO
CREATE NONCLUSTERED INDEX xa_access_audit_up_time ON dbo.xa_access_audit(update_time ASC)
GO
CREATE NONCLUSTERED INDEX x_service_def_added_by_id ON dbo.x_service_def(added_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_service_def_upd_by_id ON dbo.x_service_def(upd_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_service_def_cr_time ON dbo.x_service_def(create_time ASC)
GO
CREATE NONCLUSTERED INDEX x_service_def_up_time ON dbo.x_service_def(update_time ASC)
GO
CREATE NONCLUSTERED INDEX x_service_added_by_id ON dbo.x_service(added_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_service_upd_by_id ON dbo.x_service(upd_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_service_cr_time ON dbo.x_service(create_time ASC)
GO
CREATE NONCLUSTERED INDEX x_service_up_time ON dbo.x_service(update_time ASC)
GO
CREATE NONCLUSTERED INDEX x_service_type ON dbo.x_service(type ASC)
GO
CREATE NONCLUSTERED INDEX x_policy_added_by_id ON dbo.x_policy(added_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_policy_upd_by_id ON dbo.x_policy(upd_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_policy_cr_time ON dbo.x_policy(create_time ASC)
GO
CREATE NONCLUSTERED INDEX x_policy_up_time ON dbo.x_policy(update_time ASC)
GO
CREATE NONCLUSTERED INDEX x_policy_service ON dbo.x_policy(service ASC)
GO
CREATE NONCLUSTERED INDEX x_policy_resource_signature ON dbo.x_policy(resource_signature ASC)
GO
CREATE NONCLUSTERED INDEX x_resource_def_parent ON dbo.x_resource_def(parent ASC)
GO
CREATE NONCLUSTERED INDEX x_usr_module_perm_idx_moduleid ON dbo.x_user_module_perm(module_id ASC)
GO
CREATE NONCLUSTERED INDEX x_usr_module_perm_idx_userid ON dbo.x_user_module_perm(user_id ASC)
GO
CREATE NONCLUSTERED INDEX x_grp_module_perm_idx_groupid ON dbo.x_group_module_perm(group_id ASC)
GO
CREATE NONCLUSTERED INDEX x_grp_module_perm_idx_moduleid ON dbo.x_group_module_perm(module_id ASC)
GO
CREATE NONCLUSTERED INDEX x_tag_def_IDX_added_by_id ON dbo.x_tag_def(added_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_tag_def_IDX_upd_by_id ON dbo.x_tag_def(upd_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_tag_IDX_type ON dbo.x_tag(type ASC)
GO
CREATE NONCLUSTERED INDEX x_tag_IDX_added_by_id ON dbo.x_tag(added_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_tag_IDX_upd_by_id ON dbo.x_tag(upd_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_service_res_IDX_added_by_id ON dbo.x_service_resource(added_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_service_res_IDX_upd_by_id ON dbo.x_service_resource(upd_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_tag_res_map_IDX_tag_id ON dbo.x_tag_resource_map(tag_id ASC)
GO
CREATE NONCLUSTERED INDEX x_tag_res_map_IDX_res_id ON dbo.x_tag_resource_map(res_id ASC)
GO
CREATE NONCLUSTERED INDEX x_tag_res_map_IDX_added_by_id ON dbo.x_tag_resource_map(added_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_tag_res_map_IDX_upd_by_id ON dbo.x_tag_resource_map(upd_by_id ASC)
GO
CREATE NONCLUSTERED INDEX x_service_config_def_IDX_def_id ON dbo.x_service_config_def(def_id ASC)
GO
CREATE NONCLUSTERED INDEX x_resource_def_IDX_def_id ON dbo.x_resource_def(def_id ASC)
GO
CREATE NONCLUSTERED INDEX x_access_type_def_IDX_def_id ON dbo.x_access_type_def(def_id ASC)
GO
CREATE NONCLUSTERED INDEX x_access_type_def_IDX_grants_atd_id ON dbo.x_access_type_def_grants(atd_id ASC)
GO
CREATE NONCLUSTERED INDEX x_context_enricher_def_IDX_def_id ON dbo.x_context_enricher_def(def_id ASC)
GO
CREATE NONCLUSTERED INDEX x_enum_def_IDX_def_id ON dbo.x_enum_def(def_id ASC)
GO
CREATE NONCLUSTERED INDEX x_enum_element_def_IDX_enum_def_id ON dbo.x_enum_element_def(enum_def_id ASC)
GO
CREATE NONCLUSTERED INDEX x_service_config_map_IDX_service ON dbo.x_service_config_map(service ASC)
GO
CREATE NONCLUSTERED INDEX x_policy_resource_IDX_policy_id ON dbo.x_policy_resource(policy_id ASC)
GO
CREATE NONCLUSTERED INDEX x_policy_resource_IDX_res_def_id ON dbo.x_policy_resource(res_def_id ASC)
GO
CREATE NONCLUSTERED INDEX x_policy_resource_map_IDX_resource_id ON dbo.x_policy_resource_map(resource_id ASC)
GO
CREATE NONCLUSTERED INDEX x_policy_item_IDX_policy_id ON dbo.x_policy_item(policy_id ASC)
GO
CREATE NONCLUSTERED INDEX x_policy_item_access_IDX_policy_item_id ON dbo.x_policy_item_access(policy_item_id ASC)
GO
CREATE NONCLUSTERED INDEX x_policy_item_access_IDX_type ON dbo.x_policy_item_access(type ASC)
GO
CREATE NONCLUSTERED INDEX x_policy_item_condition_IDX_policy_item_id ON dbo.x_policy_item_condition(policy_item_id ASC)
GO
CREATE NONCLUSTERED INDEX x_policy_item_condition_IDX_type ON dbo.x_policy_item_condition(type ASC)
GO
CREATE NONCLUSTERED INDEX x_policy_item_user_perm_IDX_policy_item_id ON dbo.x_policy_item_user_perm(policy_item_id ASC)
GO
CREATE NONCLUSTERED INDEX x_policy_item_user_perm_IDX_user_id ON dbo.x_policy_item_user_perm(user_id ASC)
GO
CREATE NONCLUSTERED INDEX x_policy_item_group_perm_IDX_policy_item_id ON dbo.x_policy_item_group_perm(policy_item_id ASC)
GO
CREATE NONCLUSTERED INDEX x_policy_item_group_perm_IDX_group_id ON dbo.x_policy_item_group_perm(group_id ASC)
GO
CREATE NONCLUSTERED INDEX x_service_resource_IDX_service_id ON dbo.x_service_resource(service_id ASC)
GO
CREATE NONCLUSTERED INDEX x_datamask_type_def_IDX_def_id ON dbo.x_datamask_type_def(def_id ASC)
GO
CREATE NONCLUSTERED INDEX x_policy_item_datamask_IDX_policy_item_id ON dbo.x_policy_item_datamask(policy_item_id ASC)
GO
CREATE NONCLUSTERED INDEX x_policy_item_rowfilter_IDX_policy_item_id ON dbo.x_policy_item_rowfilter(policy_item_id ASC)
GO
CREATE NONCLUSTERED INDEX x_service_version_info_IDX_service_id ON dbo.x_service_version_info(service_id ASC)
GO
CREATE NONCLUSTERED INDEX x_plugin_info_IDX_service_name ON dbo.x_plugin_info(service_name ASC)
GO
CREATE NONCLUSTERED INDEX x_plugin_info_IDX_host_name ON dbo.x_plugin_info(host_name ASC)
GO
CREATE NONCLUSTERED INDEX x_policy_label_IDX_label_id ON dbo.x_policy_label(id ASC)
GO
CREATE NONCLUSTERED INDEX x_policy_label_IDX_label_name ON dbo.x_policy_label(label_name ASC)
GO
CREATE NONCLUSTERED INDEX x_policy_label_IDX_label_map_id ON dbo.x_policy_label_map(id ASC)
GO
CREATE NONCLUSTERED INDEX x_ugsync_audit_info_etime ON dbo.x_ugsync_audit_info(event_time ASC)
GO
CREATE NONCLUSTERED INDEX x_ugsync_audit_info_sync_src ON dbo.x_ugsync_audit_info(sync_source ASC)
GO
CREATE NONCLUSTERED INDEX x_ugsync_audit_info_uname ON dbo.x_ugsync_audit_info(user_name ASC)
GO
CREATE NONCLUSTERED INDEX x_data_hist_idx_objid_objclstype ON dbo.x_data_hist(obj_id ASC, obj_class_type ASC)
GO

CREATE OR REPLACE FUNCTION dbo.getXportalUIdByLoginId (input_val CHAR(60))
RETURNS INTEGER
BEGIN
  DECLARE myid INTEGER;
  SELECT x_portal_user.id into myid FROM x_portal_user WHERE x_portal_user.login_id=input_val;
  RETURN (myid);
END;

CREATE OR REPLACE FUNCTION dbo.getModulesIdByName (input_val CHAR(60))
RETURNS INTEGER
BEGIN
  DECLARE myid INTEGER;
  SELECT x_modules_master.id into myid FROM x_modules_master WHERE x_modules_master.module=input_val;
  RETURN (myid);
END;

CREATE NONCLUSTERED INDEX x_policy_change_log_IDX_service_id ON dbo.x_policy_change_log(service_id ASC)
GO
CREATE NONCLUSTERED INDEX x_policy_change_log_IDX_policy_version ON dbo.x_policy_change_log(policy_version ASC)
GO
CREATE NONCLUSTERED UNIQUE INDEX x_policy_change_log_uk_service_id_policy_version ON dbo.x_policy_change_log((service_id, policy_version) ASC)
GO
CREATE NONCLUSTERED INDEX x_tag_change_log_IDX_service_id ON dbo.x_tag_change_log(service_id ASC);
GO
CREATE NONCLUSTERED INDEX x_tag_change_log_IDX_tag_version ON dbo.x_tag_change_log(service_tags_version ASC);
GO
CREATE NONCLUSTERED INDEX x_tag_change_log_uk_service_id_service_tags_version ON dbo.x_tag_change_log((service_id, service_tags_version) ASC);
GO

CREATE TABLE dbo.x_rms_service_resource(
id BIGINT IDENTITY NOT NULL,
guid VARCHAR(64) NOT NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
version BIGINT DEFAULT NULL NULL,
service_id BIGINT NOT NULL,
resource_signature VARCHAR(128) DEFAULT NULL NULL,
is_enabled tinyint DEFAULT 1 NOT NULL,
service_resource_elements_text TEXT DEFAULT NULL NULL,
primary key (id),
CONSTRAINT x_rms_notification_FK_hl_service_id FOREIGN KEY(hl_service_id) REFERENCES x_service(id),
CONSTRAINT x_rms_notification_FK_ll_service_id FOREIGN KEY(ll_service_id) REFERENCES x_service(id)
);
CREATE INDEX x_rms_service_resource_IDX_service_id ON x_rms_service_resource(service_id);
CREATE INDEX x_rms_service_resource_IDX_resource_signature ON x_rms_service_resource(resource_signature);
GO

CREATE TABLE dbo.x_rms_notification (
id BIGINT IDENTITY NOT NULL ,
hms_name VARCHAR(128)  DEFAULT NULL NULL,
notification_id BIGINT  DEFAULT NULL NULL,
change_timestamp TIMESTAMP  DEFAULT NULL NULL,
change_type VARCHAR(64) DEFAULT  NULL NULL,
hl_resource_id BIGINT DEFAULT NULL NULL,
hl_service_id BIGINT DEFAULT NULL  NULL,
ll_resource_id BIGINT DEFAULT NULL NULL,
ll_service_id BIGINT  DEFAULT NULL NULL,
PRIMARY KEY (id),
CONSTRAINT x_rms_notification_FK_hl_service_id FOREIGN KEY(hl_service_id) REFERENCES x_service(id),
CONSTRAINT x_rms_notification_FK_ll_service_id FOREIGN KEY(ll_service_id) REFERENCES x_service(id)
);

CREATE INDEX x_rms_notification_IDX_notification_id ON x_rms_notification(notification_id);
CREATE INDEX x_rms_notification_IDX_hms_name_notification_id ON x_rms_notification(hms_name, notification_id);
CREATE INDEX x_rms_notification_IDX_hl_service_id ON x_rms_notification(hl_service_id);
CREATE INDEX x_rms_notification_IDX_ll_service_id ON x_rms_notification(ll_service_id);
GO

CREATE TABLE dbo.x_rms_resource_mapping(
id BIGINT  IDENTITY NOT NULL ,
change_timestamp TIMESTAMP  DEFAULT NULL NULL,
hl_resource_id BIGINT NOT NULL,
ll_resource_id BIGINT NOT NULL,
PRIMARY KEY (id),
CONSTRAINT x_rms_res_map_UK_hl_res_id_ll_res_id UNIQUE(hl_resource_id, ll_resource_id),
CONSTRAINT x_rms_res_map_FK_hl_res_id FOREIGN KEY(hl_resource_id) REFERENCES x_rms_service_resource(id),
CONSTRAINT x_rms_res_map_FK_ll_res_id FOREIGN KEY(ll_resource_id) REFERENCES x_rms_service_resource(id)
);

CREATE INDEX x_rms_resource_mapping_IDX_hl_resource_id ON x_rms_resource_mapping(hl_resource_id);
CREATE INDEX x_rms_resource_mapping_IDX_ll_resource_id ON x_rms_resource_mapping(ll_resource_id);
GO

CREATE TABLE dbo.x_rms_mapping_provider (
id BIGINT IDENTITY NOT NULL ,
change_timestamp TIMESTAMP DEFAULT NULL NULL,
name VARCHAR(128) DEFAULT  NULL NULL,
last_known_version BIGINT NOT NULL,
PRIMARY KEY (id),
CONSTRAINT x_rms_mapping_provider_UK_name UNIQUE(name)
);
GO

insert into x_portal_user (create_time,update_time,first_name,last_name,pub_scr_name,login_id,password,email,status) values (GETDATE(),GETDATE(),'Admin','','Admin','admin','ceb4f32325eda6142bd65215f4c0f371','',1)
GO
insert into x_portal_user_role (create_time,update_time,user_id,user_role,status) values (GETDATE(),GETDATE(),dbo.getXportalUIdByLoginId('admin'),'ROLE_SYS_ADMIN',1)
GO
insert into x_user (create_time,update_time,user_name,status,descr) values (GETDATE(),GETDATE(),'admin',0,'Administrator')
GO
insert into x_group (added_by_id,create_time,descr,group_type,group_name,status,update_time,upd_by_id) values (dbo.getXportalUIdByLoginId('admin'),GETDATE(),'public group',0,'public',0,GETDATE(),1)
GO
INSERT INTO x_modules_master(create_time,update_time,added_by_id,upd_by_id,module,url) VALUES(GETDATE(),GETDATE(),dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),'Resource Based Policies','')
GO
INSERT INTO x_modules_master(create_time,update_time,added_by_id,upd_by_id,module,url) VALUES(GETDATE(),GETDATE(),dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),'Users/Groups','')
GO
INSERT INTO x_modules_master(create_time,update_time,added_by_id,upd_by_id,module,url) VALUES(GETDATE(),GETDATE(),dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),'Reports','')
GO
INSERT INTO x_modules_master(create_time,update_time,added_by_id,upd_by_id,module,url) VALUES(GETDATE(),GETDATE(),dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),'Audit','')
GO
INSERT INTO x_modules_master(create_time,update_time,added_by_id,upd_by_id,module,url) VALUES(GETDATE(),GETDATE(),dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),'Key Manager','')
GO
INSERT INTO x_modules_master(create_time,update_time,added_by_id,upd_by_id,module,url) VALUES(GETDATE(),GETDATE(),dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),'Tag Based Policies','')
GO
INSERT INTO x_modules_master(create_time,update_time,added_by_id,upd_by_id,module,url) VALUES(GETDATE(),GETDATE(),dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),'Security Zone','')
GO
insert into x_portal_user (create_time,update_time,first_name,last_name,pub_scr_name,login_id,password,email,status) values (GETDATE(),GETDATE(),'rangerusersync','','rangerusersync','rangerusersync','70b8374d3dfe0325aaa5002a688c7e3b','rangerusersync',1)
GO
insert into x_portal_user_role (create_time,update_time,user_id,user_role,status) values (GETDATE(),GETDATE(),dbo.getXportalUIdByLoginId('rangerusersync'),'ROLE_SYS_ADMIN',1)
GO
insert into x_user (create_time,update_time,user_name,status,descr) values (GETDATE(),GETDATE(),'rangerusersync',0,'rangerusersync')
GO
insert into x_portal_user (create_time,update_time,first_name,last_name,pub_scr_name,login_id,password,email,status) values (GETDATE(),GETDATE(),'keyadmin','','keyadmin','keyadmin','a05f34d2dce2b4688fa82e82a89ba958','keyadmin',1)
GO
insert into x_portal_user_role (create_time,update_time,user_id,user_role,status) values (GETDATE(),GETDATE(),dbo.getXportalUIdByLoginId('keyadmin'),'ROLE_KEY_ADMIN',1)
GO
insert into x_user (create_time,update_time,user_name,status,descr) values (GETDATE(),GETDATE(),'keyadmin',0,'keyadmin')
GO
INSERT INTO x_portal_user(create_time,update_time,added_by_id,upd_by_id,first_name,last_name,pub_scr_name,login_id,password,email,status,user_src,notes) VALUES (CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,NULL,NULL,'rangertagsync','','rangertagsync','rangertagsync','f5820e1229418dcf2575908f2c493da5','rangertagsync',1,0,NULL);
GO
INSERT INTO x_portal_user_role(create_time,update_time,added_by_id,upd_by_id,user_id,user_role,status) VALUES (CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,NULL,NULL,dbo.getXportalUIdByLoginId('rangertagsync'),'ROLE_SYS_ADMIN',1);
GO
INSERT INTO x_user(create_time,update_time,added_by_id,upd_by_id,user_name,descr,status) values (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,NULL,NULL,'rangertagsync','rangertagsync',0);
GO
INSERT INTO x_security_zone(create_time, update_time, added_by_id, upd_by_id, version, name, jsonData, description) VALUES (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, dbo.getXportalUIdByLoginId('admin'), dbo.getXportalUIdByLoginId('admin'), 1, ' ', '', 'Unzoned zone');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('CORE_DB_SCHEMA',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('016',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('018',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('019',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('020',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('021',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('022',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('023',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('024',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('025',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('026',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('027',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('028',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('029',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('030',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('031',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('032',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('033',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('034',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('035',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('036',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('037',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('038',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('039',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('040',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('041',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('042',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('043',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('044',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('045',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('046',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('047',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('048',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('049',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('050',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('051',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('052',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('054',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('055',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('056',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('057',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('058',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('059',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('DB_PATCHES',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('admin'),dbo.getModulesIdByName('Reports'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
GO
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('admin'),dbo.getModulesIdByName('Resource Based Policies'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
GO
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('admin'),dbo.getModulesIdByName('Audit'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
GO
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('admin'),dbo.getModulesIdByName('Users/Groups'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
GO
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('admin'),dbo.getModulesIdByName('Tag Based Policies'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
GO
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('rangerusersync'),dbo.getModulesIdByName('Reports'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
GO
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('rangerusersync'),dbo.getModulesIdByName('Resource Based Policies'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
GO
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('rangerusersync'),dbo.getModulesIdByName('Audit'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
GO
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('rangerusersync'),dbo.getModulesIdByName('Users/Groups'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
GO
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('rangerusersync'),dbo.getModulesIdByName('Tag Based Policies'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
GO
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('keyadmin'),dbo.getModulesIdByName('Key Manager'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
GO
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('keyadmin'),dbo.getModulesIdByName('Reports'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
GO
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('keyadmin'),dbo.getModulesIdByName('Resource Based Policies'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
GO
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('rangertagsync'),dbo.getModulesIdByName('Reports'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
GO
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('rangertagsync'),dbo.getModulesIdByName('Resource Based Policies'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
GO
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('rangertagsync'),dbo.getModulesIdByName('Audit'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
GO
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('rangertagsync'),dbo.getModulesIdByName('Users/Groups'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
GO
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('rangertagsync'),dbo.getModulesIdByName('Tag Based Policies'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
GO
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('keyadmin'),dbo.getModulesIdByName('Users/Groups'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
GO
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('keyadmin'),dbo.getModulesIdByName('Audit'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
GO
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('admin'),dbo.getModulesIdByName('Security Zone'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
GO
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('rangerusersync'),dbo.getModulesIdByName('Security Zone'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
GO
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('rangertagsync'),dbo.getModulesIdByName('Security Zone'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
GO
INSERT INTO x_ranger_global_state (create_time,update_time,added_by_id,upd_by_id,version,state_name,app_data) VALUES (CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),1,'RangerRole','{"Version":"1"}');
GO
INSERT INTO x_ranger_global_state (create_time,update_time,added_by_id,upd_by_id,version,state_name,app_data) VALUES (CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),1,'RangerUserStore','{"Version":"1"}');
GO
INSERT INTO x_ranger_global_state (create_time,update_time,added_by_id,upd_by_id,version,state_name,app_data) VALUES (CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),1,'RangerSecurityZone','{"Version":"1"}');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10001',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10002',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10003',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10004',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10005',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10006',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10007',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10008',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10009',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10010',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10011',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10012',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10013',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10014',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10015',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10016',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10017',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10019',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10020',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10025',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10026',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10027',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10028',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10030',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10033',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10034',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10035',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10036',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10037',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10038',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10040',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10041',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10043',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10044',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10045',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10046',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10047',CURRENT_TIMESTAMP,'Ranger 2.2.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10049',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10050',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10052',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10053',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10054',CURRENT_TIMESTAMP,'Ranger 3.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10055',CURRENT_TIMESTAMP,'Ranger 3.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10056',CURRENT_TIMESTAMP,'Ranger 3.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('JAVA_PATCHES',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
exit
