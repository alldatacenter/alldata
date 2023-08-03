-- Licensed to the Apache Software Foundation(ASF) under one or more
-- contributor license agreements.  See the NOTICE file distributed with
-- this work for additional information regarding copyright ownership.
-- The ASF licenses this file to You under the Apache License, Version 2.0
--(the "License"); you may not use this file except in compliance with
-- the License.  You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.

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
	CONSTRAINT x_portal_user_PK_id PRIMARY KEY CLUSTERED(id),
	CONSTRAINT x_portal_user_UK_email UNIQUE NONCLUSTERED (email),
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
	group_name varchar(1024) NOT NULL,
	descr varchar(4000) NOT NULL,
	status int DEFAULT 0 NOT NULL,
	group_type int DEFAULT 0 NOT NULL,
	cred_store_id bigint DEFAULT NULL NULL,
	group_src int DEFAULT 0 NOT NULL,
	is_visible int DEFAULT 1 NOT NULL,
	CONSTRAINT x_group_PK_id PRIMARY KEY CLUSTERED(id)
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
	user_name varchar(1024) NOT NULL,
	descr varchar(4000) NOT NULL,
	status int DEFAULT 0 NOT NULL,
	cred_store_id bigint DEFAULT NULL NULL,
	is_visible int DEFAULT 1 NOT NULL,
	CONSTRAINT x_user_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
create table dbo.x_group_users(
	id bigint IDENTITY NOT NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	group_name varchar(1024) NOT NULL,
	p_group_id bigint DEFAULT NULL NULL,
	user_id bigint DEFAULT NULL NULL,
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
	impl_class_name varchar(1024) DEFAULT NULL NULL,
	label varchar(1024) DEFAULT NULL NULL,
	description varchar(1024) DEFAULT NULL NULL,
	rb_key_label varchar(1024) DEFAULT NULL NULL,
	rb_key_description varchar(1024) DEFAULT NULL NULL,
	is_enabled tinyint DEFAULT 1 NULL,
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
	policy_version bigint DEFAULT NULL NULL,
	policy_update_time datetime DEFAULT NULL NULL,
	description varchar(1024) DEFAULT NULL NULL,
	is_enabled tinyint DEFAULT 0 NOT NULL,
	CONSTRAINT x_service_def_PK_id PRIMARY KEY CLUSTERED(id)
)
GO
create table dbo.x_policy (
	id bigint IDENTITY NOT NULL,
	guid varchar(1024) DEFAULT NULL NULL,
	create_time datetime DEFAULT NULL NULL,
	update_time datetime DEFAULT NULL NULL,
	added_by_id bigint DEFAULT NULL NULL,
	upd_by_id bigint DEFAULT NULL NULL,
	version bigint DEFAULT NULL NULL,
	service bigint DEFAULT NULL NULL,
	name varchar(512) DEFAULT NULL NULL,
	policy_type int DEFAULT 0 NULL,
	description varchar(1024) DEFAULT NULL NULL,
	resource_signature varchar(128) DEFAULT NULL NULL,
	is_enabled tinyint DEFAULT 0 NOT NULL,
	is_audit_enabled tinyint DEFAULT 0 NOT NULL,
	CONSTRAINT x_policy_PK_id PRIMARY KEY CLUSTERED(id)
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
	sort_order tinyint DEFAULT 0 NULL,
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
	sort_order tinyint DEFAULT 0 NULL,
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
	sort_order tinyint DEFAULT 0 NULL,
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
	sort_order tinyint DEFAULT 0 NULL,
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
	sort_order tinyint DEFAULT 0 NULL,
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
	sort_order tinyint DEFAULT 0 NULL,
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
	sort_order tinyint DEFAULT 0 NULL,
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
	sort_order tinyint DEFAULT 0 NULL,
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
	sort_order tinyint DEFAULT 0 NULL,
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
	sort_order tinyint DEFAULT 0 NULL,
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
	sort_order tinyint DEFAULT 0 NULL,
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
	sort_order tinyint DEFAULT 0 NULL,
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
insert into x_portal_user (create_time,update_time,first_name,last_name,pub_scr_name,login_id,password,email,status) values (GETDATE(),GETDATE(),'Admin','','Admin','admin','ceb4f32325eda6142bd65215f4c0f371','',1)
GO
insert into x_portal_user_role (create_time,update_time,user_id,user_role,status) values (GETDATE(),GETDATE(),1,'ROLE_SYS_ADMIN',1)
GO
insert into x_user (create_time,update_time,user_name,status,descr) values (GETDATE(),GETDATE(),'admin',0,'Administrator')
GO
insert into x_group (added_by_id,create_time,descr,group_type,group_name,status,update_time,upd_by_id) values (1,GETDATE(),'public group',0,'public',0,GETDATE(),1)
GO
INSERT INTO x_modules_master(create_time,update_time,added_by_id,upd_by_id,module,url) VALUES(GETDATE(),GETDATE(),1,1,'Resource Based Policies','')
GO
INSERT INTO x_modules_master(create_time,update_time,added_by_id,upd_by_id,module,url) VALUES(GETDATE(),GETDATE(),1,1,'Users/Groups','')
GO
INSERT INTO x_modules_master(create_time,update_time,added_by_id,upd_by_id,module,url) VALUES(GETDATE(),GETDATE(),1,1,'Reports','')
GO
INSERT INTO x_modules_master(create_time,update_time,added_by_id,upd_by_id,module,url) VALUES(GETDATE(),GETDATE(),1,1,'Audit','')
GO
INSERT INTO x_modules_master(create_time,update_time,added_by_id,upd_by_id,module,url) VALUES(GETDATE(),GETDATE(),1,1,'Key Manager','')
GO
insert into x_portal_user (create_time,update_time,first_name,last_name,pub_scr_name,login_id,password,email,status) values (GETDATE(),GETDATE(),'rangerusersync','','rangerusersync','rangerusersync','70b8374d3dfe0325aaa5002a688c7e3b','rangerusersync',1)
GO
insert into x_portal_user_role (create_time,update_time,user_id,user_role,status) values (GETDATE(),GETDATE(),2,'ROLE_SYS_ADMIN',1)
GO
insert into x_user (create_time,update_time,user_name,status,descr) values (GETDATE(),GETDATE(),'rangerusersync',0,'rangerusersync')
GO
insert into x_portal_user (create_time,update_time,first_name,last_name,pub_scr_name,login_id,password,email,status) values (GETDATE(),GETDATE(),'keyadmin','','keyadmin','keyadmin','a05f34d2dce2b4688fa82e82a89ba958','keyadmin',1)
GO
insert into x_portal_user_role (create_time,update_time,user_id,user_role,status) values (GETDATE(),GETDATE(),3,'ROLE_KEY_ADMIN',1)
GO
insert into x_user (create_time,update_time,user_name,status,descr) values (GETDATE(),GETDATE(),'keyadmin',0,'keyadmin')
GO
exit