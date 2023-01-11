-- Licensed to the Apache Software Foundation(ASF) under one or more
-- contributor license agreements.  See the NOTICE file distributed with
-- this work for additional information regarding copyright ownership.
-- The ASF licenses this file to You under the Apache License, Version 2.0
--(the "License"); you may not use this file except in compliance with
-- the License.  You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.

DROP TABLE IF EXISTS x_rms_mapping_provider CASCADE;
DROP TABLE IF EXISTS x_rms_resource_mapping CASCADE;
DROP TABLE IF EXISTS x_rms_notification CASCADE;
DROP TABLE IF EXISTS x_rms_service_resource CASCADE;
DROP TABLE IF EXISTS x_tag_change_log;
DROP TABLE IF EXISTS x_role_ref_role CASCADE;
DROP TABLE IF EXISTS x_policy_ref_role CASCADE;
DROP TABLE IF EXISTS x_role_ref_group CASCADE;
DROP TABLE IF EXISTS x_role_ref_user CASCADE;
DROP TABLE IF EXISTS x_role CASCADE;
DROP TABLE IF EXISTS x_policy_change_log;
DROP TABLE IF EXISTS x_security_zone_ref_resource CASCADE;
DROP TABLE IF EXISTS x_policy_ref_group CASCADE;
DROP TABLE IF EXISTS x_policy_ref_user CASCADE;
DROP TABLE IF EXISTS x_policy_ref_datamask_type CASCADE;
DROP TABLE IF EXISTS x_policy_ref_condition CASCADE;
DROP TABLE IF EXISTS x_policy_ref_access_type CASCADE;
DROP TABLE IF EXISTS x_policy_ref_resource CASCADE;
DROP TABLE IF EXISTS x_ugsync_audit_info CASCADE;
DROP TABLE IF EXISTS x_policy_label_map CASCADE;
DROP TABLE IF EXISTS x_policy_label CASCADE;
DROP TABLE IF EXISTS x_plugin_info CASCADE;
DROP TABLE IF EXISTS x_service_version_info;
DROP TABLE IF EXISTS x_policy_item_rowfilter;
DROP TABLE IF EXISTS x_policy_item_datamask;
DROP TABLE IF EXISTS x_datamask_type_def;
DROP TABLE IF EXISTS x_tag_resource_map CASCADE;
DROP TABLE IF EXISTS x_service_resource CASCADE;
DROP TABLE IF EXISTS x_tag CASCADE;
DROP TABLE IF EXISTS x_tag_def CASCADE;
DROP TABLE IF EXISTS x_group_module_perm CASCADE;
DROP TABLE IF EXISTS x_user_module_perm CASCADE;
DROP TABLE IF EXISTS x_modules_master CASCADE;
DROP TABLE IF EXISTS x_data_hist CASCADE;
DROP TABLE IF EXISTS x_policy_item_group_perm CASCADE;
DROP TABLE IF EXISTS x_policy_item_user_perm CASCADE;
DROP TABLE IF EXISTS x_policy_item_condition CASCADE;
DROP TABLE IF EXISTS x_policy_item_access CASCADE;
DROP TABLE IF EXISTS x_policy_item CASCADE;
DROP TABLE IF EXISTS x_policy_resource_map CASCADE;
DROP TABLE IF EXISTS x_policy_resource CASCADE;
DROP TABLE IF EXISTS x_service_config_map CASCADE;
DROP TABLE IF EXISTS x_enum_element_def CASCADE;
DROP TABLE IF EXISTS x_enum_def CASCADE;
DROP TABLE IF EXISTS x_context_enricher_def CASCADE;
DROP TABLE IF EXISTS x_policy_condition_def CASCADE;
DROP TABLE IF EXISTS x_access_type_def_grants CASCADE;
DROP TABLE IF EXISTS x_access_type_def CASCADE;
DROP TABLE IF EXISTS x_resource_def CASCADE;
DROP TABLE IF EXISTS x_service_config_def CASCADE;
DROP TABLE IF EXISTS x_policy CASCADE;
DROP TABLE IF EXISTS x_security_zone_ref_group CASCADE;
DROP TABLE IF EXISTS x_security_zone_ref_user CASCADE;
DROP TABLE IF EXISTS x_security_zone_ref_tag_srvc CASCADE;
DROP TABLE IF EXISTS x_security_zone_ref_service CASCADE;
DROP TABLE IF EXISTS x_ranger_global_state CASCADE;
DROP TABLE IF EXISTS x_security_zone CASCADE;
DROP TABLE IF EXISTS x_service CASCADE;
DROP TABLE IF EXISTS x_service_def CASCADE;
DROP TABLE IF EXISTS x_audit_map CASCADE;
DROP TABLE IF EXISTS x_perm_map CASCADE;
DROP TABLE IF EXISTS x_trx_log CASCADE;
DROP TABLE IF EXISTS x_resource CASCADE;
DROP TABLE IF EXISTS x_policy_export_audit CASCADE;
DROP TABLE IF EXISTS x_group_users CASCADE;
DROP TABLE IF EXISTS x_user CASCADE;
DROP TABLE IF EXISTS x_group_groups;
DROP TABLE IF EXISTS x_group CASCADE;
DROP TABLE IF EXISTS x_db_base CASCADE;
DROP TABLE IF EXISTS x_cred_store CASCADE;
DROP TABLE IF EXISTS x_auth_sess CASCADE;
DROP TABLE IF EXISTS x_asset CASCADE;
DROP TABLE IF EXISTS xa_access_audit CASCADE;
DROP TABLE IF EXISTS x_portal_user_role CASCADE;
DROP TABLE IF EXISTS x_portal_user CASCADE;
DROP TABLE IF EXISTS x_db_version_h CASCADE;

DROP SEQUENCE IF EXISTS x_sec_zone_ref_group_seq;
DROP SEQUENCE IF EXISTS x_sec_zone_ref_user_seq;
DROP SEQUENCE IF EXISTS x_sec_zone_ref_resource_seq;
DROP SEQUENCE IF EXISTS x_sec_zone_ref_service_seq;
DROP SEQUENCE IF EXISTS x_sec_zone_ref_tag_srvc_SEQ;
DROP SEQUENCE IF EXISTS x_ranger_global_state_seq;
DROP SEQUENCE IF EXISTS x_security_zone_seq;
DROP SEQUENCE IF EXISTS x_policy_change_log_seq;
DROP SEQUENCE IF EXISTS x_tag_change_log_seq;
DROP SEQUENCE IF EXISTS x_policy_ref_group_seq;
DROP SEQUENCE IF EXISTS x_policy_ref_user_seq;
DROP SEQUENCE IF EXISTS x_policy_ref_datamask_type_seq;
DROP SEQUENCE IF EXISTS x_policy_ref_access_type_seq;
DROP SEQUENCE IF EXISTS x_policy_ref_resource_seq;
DROP SEQUENCE IF EXISTS x_ugsync_audit_info_seq;
DROP SEQUENCE IF EXISTS x_policy_label_map_seq;
DROP SEQUENCE IF EXISTS x_policy_label_seq;
DROP SEQUENCE IF EXISTS x_plugin_info_seq;
DROP SEQUENCE IF EXISTS x_service_version_info_seq;
DROP SEQUENCE IF EXISTS x_policy_item_rowfilter_seq;
DROP SEQUENCE IF EXISTS x_policy_item_datamask_seq;
DROP SEQUENCE IF EXISTS x_datamask_type_def_seq;
DROP SEQUENCE IF EXISTS x_tag_resource_map_seq;
DROP SEQUENCE IF EXISTS x_service_resource_seq;
DROP SEQUENCE IF EXISTS x_tag_seq;
DROP SEQUENCE IF EXISTS x_tag_def_seq;
DROP SEQUENCE IF EXISTS x_group_module_perm_seq;
DROP SEQUENCE IF EXISTS x_user_module_perm_seq;
DROP SEQUENCE IF EXISTS x_modules_master_seq;
DROP SEQUENCE IF EXISTS x_data_hist_seq;
DROP SEQUENCE IF EXISTS x_policy_item_group_perm_seq;
DROP SEQUENCE IF EXISTS x_policy_item_user_perm_seq;
DROP SEQUENCE IF EXISTS x_policy_item_condition_seq;
DROP SEQUENCE IF EXISTS x_policy_item_access_seq;
DROP SEQUENCE IF EXISTS x_policy_item_seq;
DROP SEQUENCE IF EXISTS x_policy_resource_map_seq;
DROP SEQUENCE IF EXISTS x_policy_resource_seq;
DROP SEQUENCE IF EXISTS x_service_config_map_seq;
DROP SEQUENCE IF EXISTS x_enum_element_def_seq;
DROP SEQUENCE IF EXISTS x_enum_def_seq;
DROP SEQUENCE IF EXISTS x_context_enricher_def_seq;
DROP SEQUENCE IF EXISTS x_policy_condition_def_seq;
DROP SEQUENCE IF EXISTS x_access_type_def_grants_seq;
DROP SEQUENCE IF EXISTS x_access_type_def_seq;
DROP SEQUENCE IF EXISTS x_resource_def_seq;
DROP SEQUENCE IF EXISTS x_service_config_def_seq;
DROP SEQUENCE IF EXISTS x_policy_seq;
DROP SEQUENCE IF EXISTS x_service_seq;
DROP SEQUENCE IF EXISTS x_service_def_seq;
DROP SEQUENCE IF EXISTS x_audit_map_seq;
DROP SEQUENCE IF EXISTS x_perm_map_seq;
DROP SEQUENCE IF EXISTS x_trx_log_seq;
DROP SEQUENCE IF EXISTS x_resource_seq;
DROP SEQUENCE IF EXISTS x_policy_export_seq;
DROP SEQUENCE IF EXISTS x_group_users_seq;
DROP SEQUENCE IF EXISTS x_role_ref_role_SEQ;
DROP SEQUENCE IF EXISTS x_policy_ref_role_SEQ;
DROP SEQUENCE IF EXISTS x_role_ref_group_SEQ;
DROP SEQUENCE IF EXISTS x_role_ref_user_SEQ;
DROP SEQUENCE IF EXISTS x_role_SEQ;
DROP SEQUENCE IF EXISTS x_user_seq;
DROP SEQUENCE IF EXISTS x_group_groups_seq;
DROP SEQUENCE IF EXISTS x_group_seq;
DROP SEQUENCE IF EXISTS x_db_base_seq;
DROP SEQUENCE IF EXISTS x_cred_store_seq;
DROP SEQUENCE IF EXISTS x_auth_sess_seq;
DROP SEQUENCE IF EXISTS x_asset_seq;
DROP SEQUENCE IF EXISTS xa_access_audit_seq;
DROP SEQUENCE IF EXISTS x_portal_user_role_seq;
DROP SEQUENCE IF EXISTS x_portal_user_seq;
DROP SEQUENCE IF EXISTS X_RMS_SERVICE_RESOURCE_SEQ;
DROP SEQUENCE IF EXISTS X_RMS_NOTIFICATION_SEQ;
DROP SEQUENCE IF EXISTS X_RMS_RESOURCE_MAPPING_SEQ;
DROP SEQUENCE IF EXISTS X_RMS_MAPPING_PROVIDER_SEQ;

create table x_db_version_h(
id	SERIAL primary key,
version	varchar(64) NOT NULL,
inst_at	timestamp NOT NULL DEFAULT current_timestamp,
inst_by	varchar(256) NOT NULL,
updated_at	timestamp NOT NULL,
updated_by	varchar(256) NOT NULL,
active	VARCHAR(1) CHECK (active IN ('Y','N')) DEFAULT 'Y'
);

CREATE SEQUENCE x_portal_user_seq;
CREATE TABLE x_portal_user(
id BIGINT DEFAULT nextval('x_portal_user_seq'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
first_name VARCHAR(256) DEFAULT NULL NULL,
last_name VARCHAR(256) DEFAULT NULL NULL,
pub_scr_name VARCHAR(2048) DEFAULT NULL NULL,
login_id VARCHAR(767) DEFAULT NULL NULL,
password VARCHAR(512) NOT NULL,
email VARCHAR(512) DEFAULT NULL NULL,
status INT DEFAULT '0' NOT NULL,
user_src INT DEFAULT '0' NOT NULL,
notes VARCHAR(4000) DEFAULT NULL NULL,
other_attributes VARCHAR(4000) DEFAULT NULL NULL,
sync_source VARCHAR(4000) DEFAULT NULL NULL,
old_passwords TEXT DEFAULT NULL,
password_updated_time TIMESTAMP DEFAULT NULL,
PRIMARY KEY(id),
CONSTRAINT x_portal_user_UK_login_id UNIQUE(login_id),
CONSTRAINT x_portal_user_UK_email UNIQUE(email),
CONSTRAINT x_portal_user_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_portal_user_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);

CREATE SEQUENCE x_portal_user_role_seq;
CREATE TABLE x_portal_user_role(
id BIGINT DEFAULT nextval('x_portal_user_role_seq'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
user_id BIGINT NOT NULL,
user_role VARCHAR(128) DEFAULT NULL NULL,
status INT DEFAULT 0 NOT NULL,
PRIMARY KEY(id),
CONSTRAINT x_portal_user_role_FK_addedby FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_portal_user_role_FK_updby FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_portal_user_role_FK_user_id FOREIGN KEY(user_id) REFERENCES x_portal_user(id)
);

CREATE SEQUENCE xa_access_audit_seq;
CREATE TABLE xa_access_audit(
id BIGINT DEFAULT nextval('xa_access_audit_seq'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
audit_type INT DEFAULT '0' NOT NULL,
access_result INT DEFAULT '0' NULL,
access_type VARCHAR(255) DEFAULT NULL NULL,
acl_enforcer VARCHAR(255) DEFAULT NULL NULL,
agent_id VARCHAR(255) DEFAULT NULL NULL,
client_ip VARCHAR(255) DEFAULT NULL NULL,
client_type VARCHAR(255) DEFAULT NULL NULL,
policy_id BIGINT DEFAULT '0' NULL,
repo_name VARCHAR(255) DEFAULT NULL NULL,
repo_type BIGINT DEFAULT '0' NULL,
result_reason VARCHAR(255) DEFAULT NULL NULL,
session_id VARCHAR(255) DEFAULT NULL NULL,
event_time TIMESTAMP DEFAULT NULL NULL,
request_user VARCHAR(255) DEFAULT NULL NULL,
action VARCHAR(2000) DEFAULT NULL NULL,
request_data VARCHAR(4000) DEFAULT NULL NULL,
resource_path VARCHAR(4000) DEFAULT NULL NULL,
resource_type VARCHAR(255) DEFAULT NULL NULL,
PRIMARY KEY(id)
);

CREATE SEQUENCE x_asset_seq;
CREATE TABLE x_asset(
id BIGINT DEFAULT nextval('x_asset_seq'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
asset_name VARCHAR(1024) NOT NULL,
descr VARCHAR(4000) DEFAULT NULL NULL,
act_status INT DEFAULT '0' NOT NULL,
asset_type INT DEFAULT '0' NOT NULL,
config TEXT NULL DEFAULT NULL,
sup_native BOOLEAN DEFAULT '0' NOT NULL,
PRIMARY KEY(id),
CONSTRAINT x_asset_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_asset_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);

CREATE SEQUENCE x_auth_sess_seq;
CREATE TABLE x_auth_sess(
id BIGINT DEFAULT nextval('x_auth_sess_seq'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
login_id VARCHAR(767) NOT NULL,
user_id BIGINT DEFAULT NULL NULL,
ext_sess_id VARCHAR(512) DEFAULT NULL NULL,
auth_time TIMESTAMP NOT NULL,
auth_status INT DEFAULT '0' NOT NULL,
auth_type INT DEFAULT '0' NOT NULL,
auth_provider INT DEFAULT '0' NOT NULL,
device_type INT DEFAULT '0' NOT NULL,
req_ip VARCHAR(48) NOT NULL,
req_ua VARCHAR(1024) DEFAULT NULL NULL,
PRIMARY KEY(id),
CONSTRAINT x_auth_sess_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_auth_sess_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_auth_sess_FK_user_id FOREIGN KEY(user_id) REFERENCES x_portal_user(id)
);

CREATE SEQUENCE x_cred_store_seq;
CREATE TABLE x_cred_store(
id BIGINT DEFAULT nextval('x_cred_store_seq'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
store_name VARCHAR(1024) NOT NULL,
descr VARCHAR(4000) NOT NULL,
PRIMARY KEY(id),
CONSTRAINT x_cred_store_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_cred_store_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);

CREATE SEQUENCE x_db_base_seq;
CREATE TABLE x_db_base(
id BIGINT DEFAULT nextval('x_db_base_seq'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
PRIMARY KEY(id),
CONSTRAINT x_db_base_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_db_base_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);

CREATE SEQUENCE x_group_seq;
CREATE TABLE x_group(
id BIGINT DEFAULT nextval('x_group_seq'::regclass),
CREATE_TIME TIMESTAMP DEFAULT NULL,
UPDATE_TIME TIMESTAMP DEFAULT NULL,
ADDED_BY_ID BIGINT DEFAULT NULL,
UPD_BY_ID BIGINT DEFAULT NULL,
GROUP_NAME VARCHAR(1024) NOT NULL,
DESCR VARCHAR(4000) DEFAULT NULL NULL,
STATUS INT DEFAULT '0' NOT NULL,
GROUP_TYPE INT DEFAULT '0' NOT NULL,
CRED_STORE_ID BIGINT DEFAULT NULL,
GROUP_SRC INT DEFAULT 0 NOT NULL,
IS_VISIBLE INT DEFAULT '1' NOT NULL,
other_attributes VARCHAR(4000) DEFAULT NULL NULL,
sync_source VARCHAR(4000) DEFAULT NULL NULL,
PRIMARY KEY(ID),
CONSTRAINT x_group_UK_group_name UNIQUE(group_name),
CONSTRAINT X_GROUP_FK_ADDED_BY_ID FOREIGN KEY(ADDED_BY_ID) REFERENCES X_PORTAL_USER(ID),
CONSTRAINT X_GROUP_FK_CRED_STORE_ID FOREIGN KEY(CRED_STORE_ID) REFERENCES X_CRED_STORE(ID),
CONSTRAINT X_GROUP_FK_UPD_BY_ID FOREIGN KEY(UPD_BY_ID) REFERENCES X_PORTAL_USER(ID)
);

CREATE SEQUENCE x_group_groups_seq;
CREATE TABLE x_group_groups(
id BIGINT DEFAULT nextval('x_group_groups_seq'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
group_name VARCHAR(1024) NOT NULL,
p_group_id BIGINT DEFAULT NULL NULL,
group_id BIGINT DEFAULT NULL NULL,
PRIMARY KEY(id),
CONSTRAINT x_group_groups_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_group_groups_FK_group_id FOREIGN KEY(group_id) REFERENCES x_group(id),
CONSTRAINT x_group_groups_FK_p_group_id FOREIGN KEY(p_group_id) REFERENCES x_group(id),
CONSTRAINT x_group_groups_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);

CREATE SEQUENCE x_user_seq;
CREATE TABLE x_user(
id BIGINT DEFAULT nextval('x_user_seq'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
user_name VARCHAR(767) NOT NULL,
descr VARCHAR(4000) DEFAULT NULL NULL,
status INT DEFAULT '0' NOT NULL,
cred_store_id BIGINT DEFAULT NULL NULL,
is_visible INT DEFAULT '1' NOT NULL,
other_attributes VARCHAR(4000) DEFAULT NULL NULL,
sync_source VARCHAR(4000) DEFAULT NULL NULL,
PRIMARY KEY(id),
CONSTRAINT x_user_UK_user_name UNIQUE(user_name),
CONSTRAINT x_user_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_user_FK_cred_store_id FOREIGN KEY(cred_store_id) REFERENCES x_cred_store(id),
CONSTRAINT x_user_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);

CREATE SEQUENCE x_group_users_seq;
CREATE TABLE x_group_users(
id BIGINT DEFAULT nextval('x_group_users_seq'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
group_name VARCHAR(767) NOT NULL,
p_group_id BIGINT DEFAULT NULL NULL,
user_id BIGINT DEFAULT NULL NULL,
PRIMARY KEY(id),
CONSTRAINT x_group_users_UK_uid_gname UNIQUE(user_id,group_name),
CONSTRAINT x_group_users_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_group_users_FK_p_group_id FOREIGN KEY(p_group_id) REFERENCES x_group(id),
CONSTRAINT x_group_users_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_group_users_FK_user_id FOREIGN KEY(user_id) REFERENCES x_user(id)
);

CREATE SEQUENCE x_policy_export_seq;
CREATE TABLE x_policy_export_audit(
id BIGINT DEFAULT nextval('x_policy_export_seq'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
client_ip VARCHAR(255) NOT NULL,
agent_id VARCHAR(255) DEFAULT NULL NULL,
req_epoch BIGINT NOT NULL,
last_updated TIMESTAMP DEFAULT NULL NULL,
repository_name VARCHAR(1024) DEFAULT NULL NULL,
exported_json TEXT NULL,
http_ret_code INT DEFAULT '0' NOT NULL,
cluster_name VARCHAR(255) DEFAULT NULL NULL,
zone_name VARCHAR(255) DEFAULT NULL NULL,
policy_version BIGINT DEFAULT NULL NULL,
PRIMARY KEY(id),
CONSTRAINT x_policy_export_audit_FK_added FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_policy_export_audit_FK_upd FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);

CREATE SEQUENCE x_resource_seq;
CREATE TABLE x_resource(
id BIGINT DEFAULT nextval('x_resource_seq'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
res_name VARCHAR(4000) DEFAULT NULL NULL,
descr VARCHAR(4000) DEFAULT NULL NULL,
res_type INT DEFAULT '0' NOT NULL,
asset_id BIGINT NOT NULL,
parent_id BIGINT DEFAULT NULL NULL,
parent_path VARCHAR(4000) DEFAULT NULL NULL,
is_encrypt INT DEFAULT '0' NOT NULL,
is_recursive INT DEFAULT '0' NOT NULL,
res_group VARCHAR(1024) DEFAULT NULL NULL,
res_dbs TEXT NULL,
res_tables TEXT NULL,
res_col_fams TEXT NULL,
res_cols TEXT NULL,
res_udfs TEXT NULL,
res_status INT DEFAULT '1' NOT NULL,
table_type INT DEFAULT '0' NOT NULL,
col_type INT DEFAULT '0' NOT NULL,
policy_name VARCHAR( 500 ) NULL DEFAULT NULL,
res_topologies TEXT NULL DEFAULT NULL,
res_services TEXT NULL DEFAULT NULL,
PRIMARY KEY(id),
CONSTRAINT x_resource_UK_policy_name UNIQUE(policy_name),
CONSTRAINT x_resource_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_resource_FK_asset_id FOREIGN KEY(asset_id) REFERENCES x_asset(id),
CONSTRAINT x_resource_FK_parent_id FOREIGN KEY(parent_id) REFERENCES x_resource(id),
CONSTRAINT x_resource_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);

CREATE SEQUENCE x_trx_log_seq;
CREATE TABLE x_trx_log(
id BIGINT DEFAULT nextval('x_trx_log_seq'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
class_type INT DEFAULT '0' NOT NULL,
object_id BIGINT DEFAULT NULL NULL,
parent_object_id BIGINT DEFAULT NULL NULL,
parent_object_class_type INT DEFAULT '0' NOT NULL,
parent_object_name VARCHAR(1024) DEFAULT NULL NULL,
object_name VARCHAR(1024) DEFAULT NULL NULL,
attr_name VARCHAR(255) DEFAULT NULL NULL,
prev_val TEXT NULL DEFAULT NULL,
new_val TEXT NULL DEFAULT NULL,
trx_id VARCHAR(1024) DEFAULT NULL NULL,
action VARCHAR(255) DEFAULT NULL NULL,
sess_id VARCHAR(512) DEFAULT NULL NULL,
req_id VARCHAR(30) DEFAULT NULL NULL,
sess_type VARCHAR(30) DEFAULT NULL NULL,
PRIMARY KEY(id),
CONSTRAINT x_trx_log_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_trx_log_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);

CREATE SEQUENCE x_perm_map_seq;
CREATE TABLE x_perm_map(
id BIGINT DEFAULT nextval('x_perm_map_seq'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
perm_group VARCHAR(1024) DEFAULT NULL NULL,
res_id BIGINT DEFAULT NULL NULL,
group_id BIGINT DEFAULT NULL NULL,
user_id BIGINT DEFAULT NULL NULL,
perm_for INT DEFAULT '0' NOT NULL,
perm_type INT DEFAULT '0' NOT NULL,
is_recursive INT DEFAULT '0' NOT NULL,
is_wild_card BOOLEAN DEFAULT '1' NOT NULL,
grant_revoke BOOLEAN DEFAULT '1' NOT NULL,
ip_address TEXT NULL DEFAULT NULL,
PRIMARY KEY(id),
CONSTRAINT x_perm_map_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_perm_map_FK_group_id FOREIGN KEY(group_id) REFERENCES x_group(id),
CONSTRAINT x_perm_map_FK_res_id FOREIGN KEY(res_id) REFERENCES x_resource(id),
CONSTRAINT x_perm_map_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_perm_map_FK_user_id FOREIGN KEY(user_id) REFERENCES x_user(id)
);

CREATE SEQUENCE x_audit_map_seq;
CREATE TABLE x_audit_map(
id BIGINT DEFAULT nextval('x_audit_map_seq'::regclass),
CREATE_TIME TIMESTAMP DEFAULT NULL,
UPDATE_TIME TIMESTAMP DEFAULT NULL,
ADDED_BY_ID BIGINT DEFAULT NULL,
UPD_BY_ID BIGINT DEFAULT NULL,
RES_ID BIGINT DEFAULT NULL,
GROUP_ID BIGINT DEFAULT NULL,
USER_ID BIGINT DEFAULT NULL,
AUDIT_TYPE BIGINT DEFAULT 0 NOT NULL,
PRIMARY KEY(ID),
CONSTRAINT X_AUDIT_MAP_FK_ADDED_BY_ID FOREIGN KEY(ADDED_BY_ID) REFERENCES X_PORTAL_USER(ID),
CONSTRAINT X_AUDIT_MAP_FK_GROUP_ID FOREIGN KEY(GROUP_ID) REFERENCES X_GROUP(ID),
CONSTRAINT X_AUDIT_MAP_FK_RES_ID FOREIGN KEY(RES_ID) REFERENCES X_RESOURCE(ID),
CONSTRAINT X_AUDIT_MAP_FK_UPD_BY_ID FOREIGN KEY(UPD_BY_ID) REFERENCES X_PORTAL_USER(ID),
CONSTRAINT X_AUDIT_MAP_FK_USER_ID FOREIGN KEY(USER_ID) REFERENCES X_USER(ID)
);

CREATE SEQUENCE x_service_def_seq;
CREATE TABLE x_service_def(
id BIGINT DEFAULT nextval('x_service_def_seq'::regclass),
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
version BIGINT DEFAULT NULL NULL,
name VARCHAR(1024) DEFAULT NULL NULL,
display_name VARCHAR(1024) DEFAULT NULL NULL,
impl_class_name VARCHAR(1024) DEFAULT NULL NULL,
label VARCHAR(1024) DEFAULT NULL NULL,
description VARCHAR(1024) DEFAULT NULL NULL,
rb_key_label VARCHAR(1024) DEFAULT NULL NULL,
rb_key_description VARCHAR(1024) DEFAULT NULL NULL,
is_enabled BOOLEAN DEFAULT '1' NULL,
def_options VARCHAR(1024) DEFAULT NULL NULL,
PRIMARY KEY(id),
CONSTRAINT x_service_def_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_service_def_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);

CREATE SEQUENCE x_service_seq;
CREATE TABLE x_service(
id BIGINT DEFAULT nextval('x_service_seq'::regclass),
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
version BIGINT DEFAULT NULL NULL,
type BIGINT DEFAULT NULL NULL,
name VARCHAR(255) DEFAULT NULL NULL,
display_name VARCHAR(255) DEFAULT NULL NULL,
policy_version BIGINT DEFAULT NULL NULL,
policy_update_time TIMESTAMP DEFAULT NULL NULL,
description VARCHAR(1024) DEFAULT NULL NULL,
is_enabled BOOLEAN DEFAULT '0' NOT NULL,
tag_service BIGINT DEFAULT NULL NULL,
tag_version BIGINT DEFAULT 0 NOT NULL,
tag_update_time TIMESTAMP DEFAULT NULL NULL,
primary key(id),
CONSTRAINT x_service_name UNIQUE(name),
CONSTRAINT x_service_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_service_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_service_FK_type FOREIGN KEY(type) REFERENCES x_service_def(id),
CONSTRAINT x_service_FK_tag_service FOREIGN KEY (tag_service) REFERENCES x_service(id)
);

CREATE SEQUENCE x_security_zone_seq;
CREATE TABLE x_security_zone (
id BIGINT DEFAULT nextval('x_security_zone_seq'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
version BIGINT DEFAULT NULL NULL,
name varchar(255) NOT NULL,
jsonData text DEFAULT NULL NULL,
description VARCHAR(1024) DEFAULT NULL NULL,
primary key (id),
CONSTRAINT x_security_zone_UK_name UNIQUE (name),
CONSTRAINT x_security_zone_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_security_zone_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);


CREATE SEQUENCE x_ranger_global_state_seq;
CREATE TABLE x_ranger_global_state (
id BIGINT DEFAULT nextval('x_ranger_global_state_seq'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
version BIGINT DEFAULT NULL NULL,
state_name varchar(255) NOT NULL,
app_data varchar(255) DEFAULT NULL NULL,
primary key (id),
CONSTRAINT x_ranger_global_state_UK_state_name UNIQUE (state_name),
CONSTRAINT x_ranger_global_state_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_ranger_global_state_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);

CREATE SEQUENCE x_policy_seq;
CREATE TABLE x_policy(
id BIGINT DEFAULT nextval('x_policy_seq'::regclass),
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
version BIGINT DEFAULT NULL NULL,
service BIGINT NOT NULL,
name VARCHAR(512) NOT NULL,
policy_type int DEFAULT 0 NULL,
description VARCHAR(1024) DEFAULT NULL NULL,
resource_signature VARCHAR(128) DEFAULT NULL NULL,
is_enabled BOOLEAN DEFAULT '0' NOT NULL,
is_audit_enabled BOOLEAN DEFAULT '0' NOT NULL,
policy_options VARCHAR(4000) DEFAULT NULL NULL,
policy_priority INT DEFAULT 0 NOT NULL,
policy_text TEXT DEFAULT NULL NULL,
zone_id BIGINT DEFAULT '1' NOT NULL,
primary key(id),
CONSTRAINT x_policy_uk_name_service_zone UNIQUE(name,service,zone_id),
CONSTRAINT x_policy_uk_guid_service_zone UNIQUE(guid,service,zone_id),
CONSTRAINT x_policy_uk_service_signature UNIQUE(service,resource_signature),
CONSTRAINT x_policy_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_policy_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_policy_FK_service FOREIGN KEY(service) REFERENCES x_service(id),
CONSTRAINT x_policy_FK_zone_id FOREIGN KEY(zone_id) REFERENCES x_security_zone(id)
);

CREATE SEQUENCE x_service_config_def_seq;
CREATE TABLE x_service_config_def(
id BIGINT DEFAULT nextval('x_service_config_def_seq'::regclass),
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
def_id BIGINT NOT NULL,
item_id BIGINT NOT NULL,
name VARCHAR(1024) DEFAULT NULL NULL,
type VARCHAR(1024) DEFAULT NULL NULL,
sub_type VARCHAR(1024) DEFAULT NULL NULL,
is_mandatory BOOLEAN DEFAULT '0' NOT NULL,
default_value VARCHAR(1024) DEFAULT NULL NULL,
validation_reg_ex varchar(1024) DEFAULT NULL NULL,
validation_message varchar(1024) DEFAULT NULL NULL,
ui_hint varchar(1024) DEFAULT NULL NULL,
label VARCHAR(1024) DEFAULT NULL NULL,
description VARCHAR(1024) DEFAULT NULL NULL,
rb_key_label VARCHAR(1024) DEFAULT NULL NULL,
rb_key_description VARCHAR(1024) DEFAULT NULL NULL,
rb_key_validation_message VARCHAR(1024) DEFAULT NULL NULL,
sort_order INT DEFAULT '0' NULL,
primary key(id),
CONSTRAINT x_service_conf_def_FK_defid FOREIGN KEY(def_id) REFERENCES x_service_def(id),
CONSTRAINT x_service_conf_def_FK_added_by FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_service_conf_def_FK_upd_by FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);

CREATE SEQUENCE x_resource_def_seq;
CREATE TABLE x_resource_def(
id BIGINT DEFAULT nextval('x_resource_def_seq'::regclass),
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
def_id BIGINT NOT NULL,
item_id BIGINT NOT NULL,
name VARCHAR(1024) DEFAULT NULL NULL,
type VARCHAR(1024) DEFAULT NULL NULL,
res_level BIGINT DEFAULT NULL NULL,
parent BIGINT DEFAULT NULL NULL,
mandatory BOOLEAN DEFAULT '0' NOT NULL,
look_up_supported BOOLEAN DEFAULT '0' NOT NULL,
recursive_supported BOOLEAN DEFAULT '0' NOT NULL,
excludes_supported BOOLEAN DEFAULT '0' NOT NULL,
matcher VARCHAR(1024) DEFAULT NULL NULL,
matcher_options varchar(1024) DEFAULT NULL NULL,
validation_reg_ex varchar(1024) DEFAULT NULL NULL,
validation_message varchar(1024) DEFAULT NULL NULL,
ui_hint varchar(1024) DEFAULT NULL NULL,
label VARCHAR(1024) DEFAULT NULL NULL,
description VARCHAR(1024) DEFAULT NULL NULL,
rb_key_label VARCHAR(1024) DEFAULT NULL NULL,
rb_key_description VARCHAR(1024) DEFAULT NULL NULL,
rb_key_validation_message VARCHAR(1024) DEFAULT NULL NULL,
sort_order INT DEFAULT '0' NULL,
datamask_options VARCHAR(1024) DEFAULT NULL NULL,
rowfilter_options VARCHAR(1024) DEFAULT NULL NULL,
primary key(id),
CONSTRAINT x_resource_def_FK_parent FOREIGN KEY(parent) REFERENCES x_resource_def(id),
CONSTRAINT x_resource_def_FK_defid FOREIGN KEY(def_id) REFERENCES x_service_def(id),
CONSTRAINT x_resource_def_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_resource_def_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);

CREATE SEQUENCE x_access_type_def_seq;
CREATE TABLE x_access_type_def(
id BIGINT DEFAULT nextval('x_access_type_def_seq'::regclass),
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
def_id BIGINT NOT NULL,
item_id BIGINT NOT NULL,
name VARCHAR(1024) DEFAULT NULL NULL,
label VARCHAR(1024) DEFAULT NULL NULL,
rb_key_label VARCHAR(1024) DEFAULT NULL NULL,
sort_order INT DEFAULT '0' NULL,
datamask_options VARCHAR(1024) DEFAULT NULL NULL,
rowfilter_options VARCHAR(1024) DEFAULT NULL NULL,
primary key(id),
CONSTRAINT x_access_type_def_FK_defid FOREIGN KEY(def_id) REFERENCES x_service_def(id),
CONSTRAINT x_access_type_def_FK_added_by FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_access_type_def_FK_upd_by FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);

CREATE SEQUENCE x_access_type_def_grants_seq;
CREATE TABLE x_access_type_def_grants(
id BIGINT DEFAULT nextval('x_access_type_def_grants_seq'::regclass),
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
atd_id BIGINT NOT NULL,
implied_grant VARCHAR(1024) DEFAULT NULL NULL,
primary key(id),
CONSTRAINT x_atd_grants_FK_atdid FOREIGN KEY(atd_id) REFERENCES x_access_type_def(id),
CONSTRAINT x_atd_grants_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_atd_grants_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);

CREATE SEQUENCE x_policy_condition_def_seq;
CREATE TABLE x_policy_condition_def(
id BIGINT DEFAULT nextval('x_policy_condition_def_seq'::regclass),
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
def_id BIGINT NOT NULL,
item_id BIGINT NOT NULL,
name VARCHAR(1024) DEFAULT NULL NULL,
evaluator VARCHAR(1024) DEFAULT NULL NULL,
evaluator_options VARCHAR(1024) DEFAULT NULL NULL,
validation_reg_ex varchar(1024) DEFAULT NULL NULL,
validation_message varchar(1024) DEFAULT NULL NULL,
ui_hint varchar(1024) DEFAULT NULL NULL,
label VARCHAR(1024) DEFAULT NULL NULL,
description VARCHAR(1024) DEFAULT NULL NULL,
rb_key_label VARCHAR(1024) DEFAULT NULL NULL,
rb_key_description VARCHAR(1024) DEFAULT NULL NULL,
rb_key_validation_message VARCHAR(1024) DEFAULT NULL NULL,
sort_order INT DEFAULT '0' NULL,
primary key(id),
CONSTRAINT x_policy_cond_def_FK_defid FOREIGN KEY(def_id) REFERENCES x_service_def(id),
CONSTRAINT x_policy_cond_def_FK_added_by FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_policy_cond_def_FK_upd_by FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);

CREATE SEQUENCE x_context_enricher_def_seq;
CREATE TABLE x_context_enricher_def(
id BIGINT DEFAULT nextval('x_context_enricher_def_seq'::regclass),
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
def_id BIGINT NOT NULL,
item_id BIGINT NOT NULL,
name VARCHAR(1024) DEFAULT NULL NULL,
enricher VARCHAR(1024) DEFAULT NULL NULL,
enricher_options VARCHAR(1024) DEFAULT NULL NULL,
sort_order INT DEFAULT '0' NULL,
primary key(id),
CONSTRAINT x_context_enricher_def_FK_defid FOREIGN KEY(def_id) REFERENCES x_service_def(id),
CONSTRAINT x_context_enricher_def_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_context_enricher_def_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);

CREATE SEQUENCE x_enum_def_seq;
CREATE TABLE x_enum_def(
id BIGINT DEFAULT nextval('x_enum_def_seq'::regclass),
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
def_id BIGINT NOT NULL,
item_id BIGINT NOT NULL,
name VARCHAR(1024) DEFAULT NULL NULL,
default_index BIGINT DEFAULT NULL NULL,
primary key(id),
CONSTRAINT x_enum_def_FK_def_id FOREIGN KEY(def_id) REFERENCES x_service_def(id),
CONSTRAINT x_enum_def_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_enum_def_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);

CREATE SEQUENCE x_enum_element_def_seq;
CREATE TABLE x_enum_element_def(
id BIGINT DEFAULT nextval('x_enum_element_def_seq'::regclass),
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
enum_def_id BIGINT NOT NULL,
item_id BIGINT NOT NULL,
name VARCHAR(1024) DEFAULT NULL NULL,
label VARCHAR(1024) DEFAULT NULL NULL,
rb_key_label VARCHAR(1024) DEFAULT NULL NULL,
sort_order INT DEFAULT '0' NULL,
primary key(id),
CONSTRAINT x_enum_element_def_FK_defid FOREIGN KEY(enum_def_id) REFERENCES x_enum_def(id),
CONSTRAINT x_enum_element_def_FK_added_by FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_enum_element_def_FK_upd_by FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);

CREATE SEQUENCE x_service_config_map_seq;
CREATE TABLE x_service_config_map(
id BIGINT DEFAULT nextval('x_service_config_map_seq'::regclass),
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
service BIGINT NOT NULL,
config_key VARCHAR(1024) DEFAULT NULL NULL,
config_value VARCHAR(4000) DEFAULT NULL NULL,
primary key(id),
CONSTRAINT x_service_conf_map_FK_service FOREIGN KEY(service) REFERENCES x_service(id),
CONSTRAINT x_service_conf_map_FK_added_by FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_service_conf_map_FK_upd_by FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);

CREATE SEQUENCE x_policy_resource_seq;
CREATE TABLE x_policy_resource(
id BIGINT DEFAULT nextval('x_policy_resource_seq'::regclass),
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
policy_id BIGINT NOT NULL,
res_def_id BIGINT NOT NULL,
is_excludes BOOLEAN DEFAULT '0' NOT NULL,
is_recursive BOOLEAN DEFAULT '0' NOT NULL,
primary key(id),
CONSTRAINT x_policy_res_FK_policy_id FOREIGN KEY(policy_id) REFERENCES x_policy(id),
CONSTRAINT x_policy_res_FK_res_def_id FOREIGN KEY(res_def_id) REFERENCES x_resource_def(id),
CONSTRAINT x_policy_res_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_policy_res_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);

CREATE SEQUENCE x_policy_resource_map_seq;
CREATE TABLE x_policy_resource_map(
id BIGINT DEFAULT nextval('x_policy_resource_map_seq'::regclass),
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
resource_id BIGINT NOT NULL,
value VARCHAR(1024) DEFAULT NULL NULL,
sort_order INT DEFAULT '0' NULL,
primary key(id),
CONSTRAINT x_policy_res_map_FK_res_id FOREIGN KEY(resource_id) REFERENCES x_policy_resource(id),
CONSTRAINT x_policy_res_map_FK_added_by FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_policy_res_map_FK_upd_by FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);

CREATE SEQUENCE x_policy_item_seq;
CREATE TABLE x_policy_item(
id BIGINT DEFAULT nextval('x_policy_item_seq'::regclass),
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
policy_id BIGINT NOT NULL,
delegate_admin BOOLEAN DEFAULT '0' NOT NULL,
sort_order INT DEFAULT '0' NULL,
item_type INT DEFAULT 0 NOT NULL,
is_enabled BOOLEAN DEFAULT '1' NOT NULL,
comments VARCHAR(255) DEFAULT NULL NULL,
primary key(id),
CONSTRAINT x_policy_item_FK_policy_id FOREIGN KEY(policy_id) REFERENCES x_policy(id),
CONSTRAINT x_policy_item_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_policy_item_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);

CREATE SEQUENCE x_policy_item_access_seq;
CREATE TABLE x_policy_item_access(
id BIGINT DEFAULT nextval('x_policy_item_access_seq'::regclass),
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
policy_item_id BIGINT NOT NULL,
type BIGINT NOT NULL,
is_allowed BOOLEAN DEFAULT '0' NOT NULL,
sort_order INT DEFAULT '0' NULL,
primary key(id),
CONSTRAINT x_plc_item_access_FK_pi_id FOREIGN KEY(policy_item_id) REFERENCES x_policy_item(id),
CONSTRAINT x_plc_item_access_FK_atd_id FOREIGN KEY(type) REFERENCES x_access_type_def(id),
CONSTRAINT x_plc_item_access_FK_added_by FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_plc_item_access_FK_upd_by FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);

CREATE SEQUENCE x_policy_item_condition_seq;
CREATE TABLE x_policy_item_condition(
id BIGINT DEFAULT nextval('x_policy_item_condition_seq'::regclass),
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
policy_item_id BIGINT NOT NULL,
type BIGINT NOT NULL,
value VARCHAR(1024) DEFAULT NULL NULL,
sort_order INT DEFAULT '0' NULL,
primary key(id),
CONSTRAINT x_plc_item_cond_FK_pi_id FOREIGN KEY(policy_item_id) REFERENCES x_policy_item(id),
CONSTRAINT x_plc_item_cond_FK_pcd_id FOREIGN KEY(type) REFERENCES x_policy_condition_def(id),
CONSTRAINT x_plc_item_cond_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_plc_item_cond_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);

CREATE SEQUENCE x_policy_item_user_perm_seq;
CREATE TABLE x_policy_item_user_perm(
id BIGINT DEFAULT nextval('x_policy_item_user_perm_seq'::regclass),
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
policy_item_id BIGINT NOT NULL,
user_id BIGINT DEFAULT NULL NULL,
sort_order INT DEFAULT '0' NULL,
primary key(id),
CONSTRAINT x_plc_itm_usr_perm_FK_pi_id FOREIGN KEY(policy_item_id) REFERENCES x_policy_item(id),
CONSTRAINT x_plc_itm_usr_perm_FK_user_id FOREIGN KEY(user_id) REFERENCES x_user(id),
CONSTRAINT x_plc_itm_usr_perm_FK_added_by FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_plc_itm_usr_perm_FK_upd_by FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);

CREATE SEQUENCE x_policy_item_group_perm_seq;
CREATE TABLE x_policy_item_group_perm(
id BIGINT DEFAULT nextval('x_policy_item_group_perm_seq'::regclass),
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
policy_item_id BIGINT NOT NULL,
group_id BIGINT DEFAULT NULL NULL,
sort_order INT DEFAULT '0' NULL,
primary key(id),
CONSTRAINT x_plc_itm_grp_perm_FK_pi_id FOREIGN KEY(policy_item_id) REFERENCES x_policy_item(id),
CONSTRAINT x_plc_itm_grp_perm_FK_group_id FOREIGN KEY(group_id) REFERENCES x_group(id),
CONSTRAINT x_plc_itm_grp_perm_FK_added_by FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_plc_itm_grp_perm_FK_upd_by FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);

CREATE SEQUENCE x_data_hist_seq;
CREATE TABLE x_data_hist(
id BIGINT DEFAULT nextval('x_data_hist_seq'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
obj_guid VARCHAR(1024) NOT NULL,
obj_class_type INT NOT NULL,
obj_id BIGINT NOT NULL,
obj_name VARCHAR(1024) NOT NULL,
version BIGINT DEFAULT NULL NULL,
action VARCHAR(512)NOT NULL,
from_time TIMESTAMP NOT NULL,
to_time TIMESTAMP DEFAULT NULL NULL,
content TEXT NOT NULL,
primary key(id)
);

CREATE SEQUENCE x_modules_master_seq;
CREATE TABLE x_modules_master(
id BIGINT DEFAULT nextval('x_modules_master_seq'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
module VARCHAR(1024) NOT NULL,
url VARCHAR(1024) DEFAULT NULL NULL,
PRIMARY KEY(id)
);

CREATE SEQUENCE x_user_module_perm_seq;
CREATE TABLE x_user_module_perm(
id BIGINT DEFAULT nextval('x_user_module_perm_seq'::regclass),
user_id BIGINT DEFAULT NULL NULL,
module_id BIGINT DEFAULT NULL NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
is_allowed INT DEFAULT '1' NOT NULL,
PRIMARY KEY(id),
CONSTRAINT x_user_module_perm_FK_moduleid FOREIGN KEY (module_id) REFERENCES x_modules_master(id),
CONSTRAINT x_user_module_perm_FK_userid FOREIGN KEY (user_id) REFERENCES x_portal_user(id)
);

CREATE SEQUENCE x_group_module_perm_seq;
CREATE TABLE x_group_module_perm(
id BIGINT DEFAULT nextval('x_group_module_perm_seq'::regclass),
group_id BIGINT DEFAULT NULL NULL,
module_id BIGINT DEFAULT NULL NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
is_allowed INT DEFAULT '1' NOT NULL,
PRIMARY KEY(id),
CONSTRAINT x_grp_module_perm_FK_module_id FOREIGN KEY (module_id) REFERENCES x_modules_master(id),
CONSTRAINT x_grp_module_perm_FK_group_id FOREIGN KEY (group_id) REFERENCES x_group(id)
);

CREATE SEQUENCE x_tag_def_seq;
CREATE TABLE x_tag_def(
id BIGINT DEFAULT nextval('x_tag_def_seq'::regclass),
guid VARCHAR(64) NOT NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
version BIGINT DEFAULT NULL NULL,
name VARCHAR(255) NOT NULL,
source VARCHAR(128) DEFAULT NULL NULL,
is_enabled BOOLEAN DEFAULT '0' NOT NULL,
tag_attrs_def_text TEXT DEFAULT NULL NULL,
PRIMARY KEY (id),
CONSTRAINT x_tag_def_UK_guid UNIQUE (guid),
CONSTRAINT x_tag_def_UK_name UNIQUE (name),
CONSTRAINT x_tag_def_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_tag_def_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);

CREATE SEQUENCE x_tag_seq;
CREATE TABLE x_tag(
id BIGINT DEFAULT nextval('x_tag_seq'::regclass),
guid VARCHAR(64) NOT NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
version BIGINT DEFAULT NULL NULL,
type BIGINT NOT NULL,
owned_by SMALLINT DEFAULT 0 NOT NULL,
policy_options VARCHAR(4000) DEFAULT NULL NULL,
tag_attrs_text TEXT DEFAULT NULL NULL,
primary key (id),
CONSTRAINT x_tag_UK_guid UNIQUE (guid),
CONSTRAINT x_tag_FK_type FOREIGN KEY (type) REFERENCES x_tag_def (id),
CONSTRAINT x_tag_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_tag_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);

CREATE SEQUENCE x_service_resource_seq;
CREATE TABLE x_service_resource(
id BIGINT DEFAULT nextval('x_service_resource_seq'::regclass),
guid VARCHAR(64) NOT NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
version BIGINT DEFAULT NULL NULL,
service_id BIGINT NOT NULL,
resource_signature VARCHAR(128) DEFAULT NULL NULL,
is_enabled BOOLEAN DEFAULT '1' NOT NULL,
service_resource_elements_text TEXT DEFAULT NULL NULL,
tags_text TEXT DEFAULT NULL NULL,
primary key (id),
CONSTRAINT x_service_res_UK_guid UNIQUE (guid),
CONSTRAINT x_service_res_FK_service_id FOREIGN KEY (service_id) REFERENCES x_service (id),
CONSTRAINT x_service_res_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_service_res_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_service_resource_IDX_svc_id_resource_signature UNIQUE (service_id, resource_signature)
);

CREATE SEQUENCE x_tag_resource_map_seq;
CREATE TABLE x_tag_resource_map(
id BIGINT NOT NULL,
guid VARCHAR(64) NOT NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
tag_id BIGINT NOT NULL,
res_id BIGINT NOT NULL,
primary key (id),
CONSTRAINT x_tag_res_map_UK_guid UNIQUE (guid),
CONSTRAINT x_tag_res_map_FK_tag_id FOREIGN KEY (tag_id) REFERENCES x_tag (id),
CONSTRAINT x_tag_res_map_FK_res_id FOREIGN KEY (res_id) REFERENCES x_service_resource (id),
CONSTRAINT x_tag_res_map_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_tag_res_map_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);

CREATE SEQUENCE x_datamask_type_def_seq;
CREATE TABLE x_datamask_type_def (
  id BIGINT DEFAULT nextval('x_datamask_type_def_seq'::regclass),
  guid VARCHAR(64) DEFAULT NULL NULL,
  create_time TIMESTAMP DEFAULT NULL NULL,
  update_time TIMESTAMP DEFAULT NULL NULL,
  added_by_id BIGINT DEFAULT NULL NULL,
  upd_by_id BIGINT DEFAULT NULL NULL,
  def_id BIGINT NOT NULL,
  item_id BIGINT NOT NULL,
  name VARCHAR(1024) NOT NULL,
  label VARCHAR(1024) NOT NULL,
  description VARCHAR(1024) DEFAULT NULL NULL,
  transformer VARCHAR(1024) DEFAULT NULL NULL,
  datamask_options VARCHAR(1024) DEFAULT NULL NULL,
  rb_key_label VARCHAR(1024) DEFAULT NULL NULL,
  rb_key_description VARCHAR(1024) DEFAULT NULL NULL,
  sort_order INT DEFAULT '0' NULL,
  primary key (id),
  CONSTRAINT x_datamask_type_def_FK_def_id FOREIGN KEY (def_id) REFERENCES x_service_def (id) ,
  CONSTRAINT x_datamask_type_def_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
  CONSTRAINT x_datamask_type_def_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);

CREATE SEQUENCE x_policy_item_datamask_seq;
CREATE TABLE x_policy_item_datamask (
  id BIGINT DEFAULT nextval('x_policy_item_datamask_seq'::regclass),
  guid VARCHAR(64) DEFAULT NULL NULL,
  create_time TIMESTAMP DEFAULT NULL NULL,
  update_time TIMESTAMP DEFAULT NULL NULL,
  added_by_id BIGINT DEFAULT NULL NULL,
  upd_by_id BIGINT DEFAULT NULL NULL,
  policy_item_id BIGINT NOT NULL,
  type BIGINT NOT NULL,
  condition_expr VARCHAR(1024) DEFAULT NULL NULL,
  value_expr VARCHAR(1024) DEFAULT NULL NULL,
  primary key (id),
  CONSTRAINT x_policy_item_datamask_FK_policy_item_id FOREIGN KEY (policy_item_id) REFERENCES x_policy_item (id) ,
  CONSTRAINT x_policy_item_datamask_FK_type FOREIGN KEY (type) REFERENCES x_datamask_type_def (id),
  CONSTRAINT x_policy_item_datamask_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
  CONSTRAINT x_policy_item_datamask_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);

CREATE SEQUENCE x_policy_item_rowfilter_seq;
CREATE TABLE x_policy_item_rowfilter (
  id BIGINT DEFAULT nextval('x_policy_item_rowfilter_seq'::regclass),
  guid VARCHAR(64) DEFAULT NULL NULL,
  create_time TIMESTAMP DEFAULT NULL NULL,
  update_time TIMESTAMP DEFAULT NULL NULL,
  added_by_id BIGINT DEFAULT NULL NULL,
  upd_by_id BIGINT DEFAULT NULL NULL,
  policy_item_id BIGINT NOT NULL,
  filter_expr VARCHAR(1024) DEFAULT NULL NULL,
  primary key (id),
  CONSTRAINT x_policy_item_rowfilter_FK_policy_item_id FOREIGN KEY (policy_item_id) REFERENCES x_policy_item (id) ,
  CONSTRAINT x_policy_item_rowfilter_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
  CONSTRAINT x_policy_item_rowfilter_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);

CREATE SEQUENCE x_service_version_info_seq;
CREATE TABLE x_service_version_info (
id BIGINT DEFAULT nextval('x_service_version_info_seq'::regclass),
service_id bigint NOT NULL,
policy_version bigint NOT NULL DEFAULT '0',
policy_update_time TIMESTAMP DEFAULT NULL,
tag_version bigint NOT NULL DEFAULT '0',
tag_update_time TIMESTAMP DEFAULT NULL,
role_version bigint NOT NULL DEFAULT '0',
role_update_time TIMESTAMP DEFAULT NULL,
version bigint NOT NULL DEFAULT '1',
primary key (id),
CONSTRAINT x_service_version_info_service_id FOREIGN KEY (service_id) REFERENCES x_service (id)
);

CREATE SEQUENCE x_plugin_info_seq;
CREATE TABLE x_plugin_info (
id BIGINT DEFAULT nextval('x_plugin_info_seq'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
service_name varchar(255) NOT NULL,
app_type varchar(128) NOT NULL,
host_name varchar(255) NOT NULL,
ip_address varchar(64) NOT NULL,
info varchar(1024) NOT NULL,
primary key (id),
CONSTRAINT x_plugin_info_UK UNIQUE (service_name, host_name, app_type)
);

CREATE SEQUENCE x_policy_label_seq;
CREATE TABLE x_policy_label (
id BIGINT DEFAULT nextval('x_policy_label_seq'::regclass),
guid VARCHAR(64) DEFAULT NULL NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
label_name VARCHAR(512) DEFAULT NULL,
primary key (id),
CONSTRAINT x_policy_label_UK_label_name UNIQUE (label_name),
CONSTRAINT x_policy_label_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_policy_label_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);

CREATE SEQUENCE x_policy_label_map_seq;
CREATE TABLE  x_policy_label_map (
id BIGINT DEFAULT nextval('x_policy_label_map_seq'::regclass),
guid VARCHAR(64) DEFAULT NULL NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
policy_id BIGINT DEFAULT NULL,
policy_label_id BIGINT DEFAULT NULL,
primary key (id),
CONSTRAINT x_policy_label_map_pid_plid UNIQUE (policy_id, policy_label_id),
CONSTRAINT x_policy_label_map_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_policy_label_map_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_policy_label_map_FK_policy_id FOREIGN KEY (policy_id) REFERENCES x_policy (id),
CONSTRAINT x_policy_label_map_FK_policy_label_id FOREIGN KEY (policy_label_id) REFERENCES x_policy_label (id)
);

CREATE SEQUENCE x_ugsync_audit_info_seq;
CREATE TABLE x_ugsync_audit_info (
id BIGINT DEFAULT nextval('x_ugsync_audit_info_seq'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
event_time TIMESTAMP DEFAULT NULL NULL,
user_name varchar(255) NOT  NULL,
sync_source varchar(128) NOT NULL,
no_of_new_users bigint NOT NULL,
no_of_new_groups bigint NOT NULL,
no_of_modified_users bigint NOT NULL,
no_of_modified_groups bigint NOT NULL,
sync_source_info TEXT NOT NULL,
session_id varchar(255) DEFAULT NULL,
primary key (id)
);

CREATE SEQUENCE x_policy_ref_resource_seq;
CREATE TABLE x_policy_ref_resource(
id BIGINT DEFAULT nextval('x_policy_ref_resource_seq'::regclass),
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
policy_id BIGINT NOT NULL,
resource_def_id BIGINT NOT NULL,
resource_name varchar(4000) DEFAULT NULL,
primary key(id),
CONSTRAINT x_p_ref_res_UK_polId_resDefId UNIQUE (policy_id, resource_def_id),
CONSTRAINT x_p_ref_res_FK_policy_id FOREIGN KEY(policy_id) REFERENCES x_policy(id),
CONSTRAINT x_p_ref_res_FK_resource_def_id FOREIGN KEY(resource_def_id) REFERENCES x_resource_def(id),
CONSTRAINT x_p_ref_res_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_p_ref_res_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);
commit;

CREATE SEQUENCE x_policy_ref_access_type_seq;
CREATE TABLE x_policy_ref_access_type(
id BIGINT DEFAULT nextval('x_policy_ref_access_type_seq'::regclass),
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
policy_id BIGINT NOT NULL,
access_def_id BIGINT NOT NULL,
access_type_name varchar(4000) DEFAULT NULL,
primary key(id),
CONSTRAINT x_p_ref_acc_UK_polId_accDefId UNIQUE(policy_id, access_def_id),
CONSTRAINT x_p_ref_acc_FK_policy_id FOREIGN KEY(policy_id) REFERENCES x_policy(id),
CONSTRAINT x_p_ref_acc_FK_acc_def_id FOREIGN KEY(access_def_id) REFERENCES x_access_type_def(id),
CONSTRAINT x_p_ref_acc_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_p_ref_acc_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);
commit;
DROP TABLE IF EXISTS x_policy_ref_condition CASCADE;
DROP SEQUENCE IF EXISTS x_policy_ref_condition_seq;
CREATE SEQUENCE x_policy_ref_condition_seq;
CREATE TABLE x_policy_ref_condition(
id BIGINT DEFAULT nextval('x_policy_ref_condition_seq'::regclass),
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
policy_id BIGINT NOT NULL,
condition_def_id BIGINT NOT NULL,
condition_name varchar(4000) DEFAULT NULL,
primary key(id),
CONSTRAINT x_p_ref_cond_UK_polId_cDefId UNIQUE(policy_id, condition_def_id),
CONSTRAINT x_p_ref_cond_FK_policy_id FOREIGN KEY(policy_id) REFERENCES x_policy(id),
CONSTRAINT x_p_ref_cond_FK_cond_def_id FOREIGN KEY(condition_def_id) REFERENCES x_policy_condition_def(id),
CONSTRAINT x_p_ref_cond_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_p_ref_cond_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);
commit;

CREATE SEQUENCE x_policy_ref_datamask_type_seq;
CREATE TABLE x_policy_ref_datamask_type(
id BIGINT DEFAULT nextval('x_policy_ref_datamask_type_seq'::regclass),
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
policy_id BIGINT NOT NULL,
datamask_def_id BIGINT NOT NULL,
datamask_type_name varchar(4000) DEFAULT NULL,
primary key(id),
CONSTRAINT x_p_ref_dmk_UK_polId_dDefId UNIQUE(policy_id, datamask_def_id),
CONSTRAINT x_p_ref_dmk_FK_policy_id FOREIGN KEY(policy_id) REFERENCES x_policy(id),
CONSTRAINT x_p_ref_dmk_FK_dmk_def_id FOREIGN KEY(datamask_def_id) REFERENCES x_datamask_type_def(id),
CONSTRAINT x_p_ref_dmk_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_p_ref_dmk_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);
commit;

CREATE SEQUENCE x_policy_ref_user_seq;
CREATE TABLE x_policy_ref_user(
id BIGINT DEFAULT nextval('x_policy_ref_user_seq'::regclass),
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
policy_id BIGINT NOT NULL,
user_id BIGINT NOT NULL,
user_name varchar(4000) DEFAULT NULL,
primary key(id),
CONSTRAINT x_p_ref_usr_UK_polId_userId UNIQUE(policy_id, user_id),
CONSTRAINT x_p_ref_usr_FK_policy_id FOREIGN KEY(policy_id) REFERENCES x_policy(id),
CONSTRAINT x_p_ref_usr_FK_user_id FOREIGN KEY(user_id) REFERENCES x_user(id),
CONSTRAINT x_p_ref_usr_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_p_ref_usr_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);
commit;

CREATE SEQUENCE x_policy_ref_group_seq;
CREATE TABLE x_policy_ref_group(
id BIGINT DEFAULT nextval('x_policy_ref_group_seq'::regclass),
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
policy_id BIGINT NOT NULL,
group_id BIGINT NOT NULL,
group_name varchar(4000) DEFAULT NULL,
primary key(id),
CONSTRAINT x_p_ref_grp_UK_polId_grpId UNIQUE(policy_id, group_id),
CONSTRAINT x_p_ref_grp_FK_policy_id FOREIGN KEY(policy_id) REFERENCES x_policy(id),
CONSTRAINT x_p_ref_grp_FK_group_id FOREIGN KEY(group_id) REFERENCES x_group(id),
CONSTRAINT x_p_ref_grp_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_p_ref_grp_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);
commit;

CREATE SEQUENCE x_sec_zone_ref_service_seq;
CREATE TABLE x_security_zone_ref_service (
id BIGINT DEFAULT nextval('x_sec_zone_ref_service_seq'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
zone_id BIGINT DEFAULT NULL NULL,
service_id BIGINT DEFAULT NULL NULL,
service_name varchar(255) NULL DEFAULT NULL::character varying,
primary key (id),
CONSTRAINT x_sz_ref_service_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_sz_ref_service_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_sz_ref_service_FK_zone_id FOREIGN KEY (zone_id) REFERENCES x_security_zone (id),
CONSTRAINT x_sz_ref_service_FK_service_id FOREIGN KEY (service_id) REFERENCES x_service (id),
CONSTRAINT x_sz_ref_service_FK_service_name FOREIGN KEY (service_name) REFERENCES x_service (name)
);

CREATE SEQUENCE x_sec_zone_ref_tag_srvc_seq;
CREATE TABLE x_security_zone_ref_tag_srvc (
id BIGINT DEFAULT nextval('x_sec_zone_ref_tag_srvc_SEQ'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
zone_id BIGINT DEFAULT NULL NULL,
tag_srvc_id BIGINT DEFAULT NULL NULL,
tag_srvc_name varchar(255) NULL DEFAULT NULL::character varying,
primary key (id),
CONSTRAINT x_sz_refTagSrvc_FK_aded_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_sz_refTagSrvc_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_sz_refTagSrvc_FK_zone_id FOREIGN KEY (zone_id) REFERENCES x_security_zone (id),
CONSTRAINT x_sz_refTagSrvc_FK_tag_srvc_id FOREIGN KEY (tag_srvc_id) REFERENCES x_service (id),
CONSTRAINT x_sz_refTagSrvc_FK_tag_srvc_name FOREIGN KEY (tag_srvc_name) REFERENCES x_service (name)
);

CREATE SEQUENCE x_sec_zone_ref_resource_seq;
CREATE TABLE x_security_zone_ref_resource (
id BIGINT DEFAULT nextval('x_sec_zone_ref_resource_seq'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
zone_id BIGINT DEFAULT NULL NULL,
resource_def_id BIGINT DEFAULT NULL NULL,
resource_name varchar(255) NULL DEFAULT NULL::character varying,
primary key (id),
CONSTRAINT x_sz_ref_res_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_sz_ref_res_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_sz_ref_service_FK_zone_id FOREIGN KEY (zone_id) REFERENCES x_security_zone (id),
CONSTRAINT x_sz_ref_res_FK_resource_def_id FOREIGN KEY (resource_def_id) REFERENCES x_resource_def (id)
);

CREATE SEQUENCE x_sec_zone_ref_user_seq;
CREATE TABLE x_security_zone_ref_user (
id BIGINT DEFAULT nextval('x_sec_zone_ref_user_seq'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
zone_id BIGINT DEFAULT NULL NULL,
user_id BIGINT DEFAULT NULL NULL,
user_name varchar(255) NULL DEFAULT NULL::character varying,
user_type SMALLINT DEFAULT NULL NULL,
primary key (id),
CONSTRAINT x_sz_ref_user_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_sz_ref_user_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_sz_ref_user_FK_zone_id FOREIGN KEY (zone_id) REFERENCES x_security_zone (id),
CONSTRAINT x_sz_ref_user_FK_user_id FOREIGN KEY (user_id) REFERENCES x_user (id),
CONSTRAINT x_sz_ref_user_FK_user_name FOREIGN KEY (user_name) REFERENCES x_user (user_name)
);

CREATE SEQUENCE x_sec_zone_ref_group_seq;
CREATE TABLE x_security_zone_ref_group (
id BIGINT DEFAULT nextval('x_sec_zone_ref_group_seq'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
zone_id BIGINT DEFAULT NULL NULL,
group_id BIGINT DEFAULT NULL NULL,
group_name varchar(255) NULL DEFAULT NULL::character varying,
group_type SMALLINT DEFAULT NULL NULL,
primary key (id),
CONSTRAINT x_sz_ref_group_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_sz_ref_group_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_sz_ref_group_FK_zone_id FOREIGN KEY (zone_id) REFERENCES x_security_zone (id),
CONSTRAINT x_sz_ref_group_FK_group_id FOREIGN KEY (group_id) REFERENCES x_group (id)
);
commit;
CREATE SEQUENCE x_policy_change_log_seq;
CREATE TABLE x_policy_change_log (
id BIGINT DEFAULT nextval('x_policy_change_log_seq'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
service_id bigint NOT NULL,
change_type int NOT NULL,
policy_version bigint DEFAULT '0' NOT NULL,
service_type varchar(256) DEFAULT NULL NULL,
policy_type int DEFAULT NULL NULL,
zone_name varchar(256) DEFAULT NULL NULL,
policy_id bigint DEFAULT NULL NULL,
policy_guid varchar(1024) DEFAULT NULL NULL,
primary key (id),
CONSTRAINT x_policy_change_log_uk_service_id_policy_version UNIQUE(service_id, policy_version)
);
commit;

CREATE SEQUENCE x_role_SEQ;
CREATE TABLE x_role(
id BIGINT DEFAULT nextval('x_role_SEQ'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
version BIGINT DEFAULT '0' NOT NULL,
name varchar(255) NOT NULL,
description varchar(1024) DEFAULT NULL NULL,
role_options varchar(4000) DEFAULT NULL NULL,
role_text text DEFAULT NULL NULL,
 PRIMARY KEY (id),
 CONSTRAINT x_role_UK_name UNIQUE(name),
 CONSTRAINT x_role_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
 CONSTRAINT x_role_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
commit;

CREATE SEQUENCE x_role_ref_user_SEQ;
CREATE TABLE x_role_ref_user(
id BIGINT DEFAULT nextval('x_role_ref_user_SEQ'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
role_id BIGINT NOT NULL,
user_id BIGINT DEFAULT NULL NULL,
user_name varchar(767) DEFAULT NULL NULL,
priv_type INT DEFAULT NULL NULL,
 PRIMARY KEY (id),
 CONSTRAINT x_role_ref_user_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
 CONSTRAINT x_role_ref_user_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id),
 CONSTRAINT x_role_ref_user_FK_role_id FOREIGN KEY (role_id) REFERENCES x_role (id),
 CONSTRAINT x_role_ref_user_FK_user_id FOREIGN KEY (user_id) REFERENCES x_user (id)
);
commit;

CREATE SEQUENCE x_role_ref_group_SEQ;
CREATE TABLE x_role_ref_group(
id BIGINT DEFAULT nextval('x_role_ref_group_SEQ'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
role_id BIGINT NOT NULL,
group_id BIGINT DEFAULT NULL NULL,
group_name varchar(767) DEFAULT NULL NULL,
priv_type INT DEFAULT NULL NULL,
 PRIMARY KEY (id),
 CONSTRAINT x_role_ref_grp_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
 CONSTRAINT x_role_ref_grp_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id),
 CONSTRAINT x_role_ref_grp_FK_role_id FOREIGN KEY (role_id) REFERENCES x_role (id),
 CONSTRAINT x_role_ref_grp_FK_group_id FOREIGN KEY (group_id) REFERENCES x_group (id)
);
commit;

CREATE SEQUENCE x_policy_ref_role_SEQ;
CREATE TABLE x_policy_ref_role(
id BIGINT DEFAULT nextval('x_policy_ref_role_SEQ'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
policy_id BIGINT NOT NULL,
role_id BIGINT NOT NULL,
role_name varchar(255) DEFAULT NULL,
 PRIMARY KEY (id),
 CONSTRAINT x_pol_ref_role_UK_polId_roleId UNIQUE(policy_id,role_id),
 CONSTRAINT x_pol_ref_role_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
 CONSTRAINT x_pol_ref_role_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id),
 CONSTRAINT x_pol_ref_role_FK_policy_id FOREIGN KEY (policy_id) REFERENCES x_policy (id),
 CONSTRAINT x_pol_ref_role_FK_role_id FOREIGN KEY (role_id) REFERENCES x_role (id)
);
commit;

CREATE SEQUENCE x_role_ref_role_SEQ;
CREATE TABLE x_role_ref_role(
id BIGINT DEFAULT nextval('x_role_ref_role_SEQ'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
role_ref_id BIGINT DEFAULT NULL NULL,
role_id BIGINT NOT NULL,
role_name varchar(255) DEFAULT NULL NULL,
priv_type INT DEFAULT NULL NULL,
 PRIMARY KEY (id),
 CONSTRAINT x_role_ref_role_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
 CONSTRAINT x_role_ref_role_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id),
 CONSTRAINT x_role_ref_role_FK_role_ref_id FOREIGN KEY (role_ref_id) REFERENCES x_role (id)
);
commit;

CREATE SEQUENCE x_tag_change_log_seq;

CREATE TABLE x_tag_change_log (
id BIGINT DEFAULT nextval('x_tag_change_log_seq'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
service_id bigint NOT NULL,
change_type int NOT NULL,
service_tags_version bigint DEFAULT '0' NOT NULL,
service_resource_id bigint DEFAULT NULL NULL,
tag_id bigint DEFAULT NULL NULL,
primary key (id),
CONSTRAINT x_tag_change_log_uk_service_id_service_tags_version UNIQUE(service_id, service_tags_version)
);
commit;

CREATE SEQUENCE x_rms_service_resource_seq;

CREATE TABLE x_rms_service_resource(
id BIGINT DEFAULT nextval('x_rms_service_resource_seq'::regclass),
guid VARCHAR(64) NOT NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
version BIGINT DEFAULT NULL NULL,
service_id BIGINT NOT NULL,
resource_signature VARCHAR(128) DEFAULT NULL NULL,
is_enabled BOOLEAN DEFAULT '1' NOT NULL,
service_resource_elements_text TEXT DEFAULT NULL NULL,
primary key (id),
CONSTRAINT x_rms_service_res_UK_guid UNIQUE (guid),
CONSTRAINT x_rms_service_res_FK_service_id FOREIGN KEY (service_id) REFERENCES x_service (id)
);
commit;

CREATE SEQUENCE X_RMS_NOTIFICATION_SEQ;

CREATE TABLE x_rms_notification (
id BIGINT DEFAULT nextval('X_RMS_NOTIFICATION_SEQ'::regclass),
hms_name VARCHAR(128) NULL DEFAULT NULL,
notification_id BIGINT NULL DEFAULT NULL,
change_timestamp TIMESTAMP NULL DEFAULT NULL,
change_type VARCHAR(64) NULL DEFAULT  NULL,
hl_resource_id BIGINT NULL DEFAULT NULL,
hl_service_id BIGINT NULL DEFAULT NULL,
ll_resource_id BIGINT NULL DEFAULT NULL,
ll_service_id BIGINT NULL DEFAULT NULL,
PRIMARY KEY (id),
CONSTRAINT x_rms_notification_FK_hl_service_id FOREIGN KEY(hl_service_id) REFERENCES x_service(id),
CONSTRAINT x_rms_notification_FK_ll_service_id FOREIGN KEY(ll_service_id) REFERENCES x_service(id)
);
commit;

CREATE SEQUENCE X_RMS_RESOURCE_MAPPING_SEQ;

CREATE TABLE x_rms_resource_mapping(
id BIGINT DEFAULT nextval('X_RMS_RESOURCE_MAPPING_SEQ'::regclass),
change_timestamp TIMESTAMP NULL DEFAULT NULL,
hl_resource_id BIGINT NOT NULL,
ll_resource_id BIGINT NOT NULL,
PRIMARY KEY (id),
CONSTRAINT x_rms_res_map_UK_hl_res_id_ll_res_id UNIQUE(hl_resource_id, ll_resource_id),
CONSTRAINT x_rms_res_map_FK_hl_res_id FOREIGN KEY(hl_resource_id) REFERENCES x_rms_service_resource(id),
CONSTRAINT x_rms_res_map_FK_ll_res_id FOREIGN KEY(ll_resource_id) REFERENCES x_rms_service_resource(id)
);
commit;

CREATE SEQUENCE X_RMS_MAPPING_PROVIDER_SEQ;

CREATE TABLE x_rms_mapping_provider (
id BIGINT DEFAULT nextval('X_RMS_MAPPING_PROVIDER_SEQ'::regclass),
change_timestamp TIMESTAMP DEFAULT NULL NULL,
name VARCHAR(128) NOT NULL,
last_known_version BIGINT NOT NULL,
PRIMARY KEY (id),
CONSTRAINT x_rms_mapping_provider_UK_name UNIQUE(name)
);
commit;

CREATE INDEX x_tag_change_log_IDX_service_id ON x_tag_change_log(service_id);
CREATE INDEX x_tag_change_log_IDX_tag_version ON x_tag_change_log(service_tags_version);
commit;

CREATE INDEX x_policy_change_log_IDX_service_id ON x_policy_change_log(service_id);
CREATE INDEX x_policy_change_log_IDX_policy_version ON x_policy_change_log(policy_version);
commit;

CREATE INDEX xa_access_audit_added_by_id ON xa_access_audit(added_by_id);
CREATE INDEX xa_access_audit_upd_by_id ON xa_access_audit(upd_by_id);
CREATE INDEX xa_access_audit_cr_time ON xa_access_audit(create_time);
CREATE INDEX xa_access_audit_up_time ON xa_access_audit(update_time);
CREATE INDEX xa_access_audit_event_time ON xa_access_audit(event_time);
CREATE INDEX x_asset_FK_added_by_id ON x_asset(added_by_id);
CREATE INDEX x_asset_FK_upd_by_id ON x_asset(upd_by_id);
CREATE INDEX x_asset_cr_time ON x_asset(create_time);
CREATE INDEX x_asset_up_time ON x_asset(update_time);
CREATE INDEX x_audit_map_FK_added_by_id ON x_audit_map(added_by_id);
CREATE INDEX x_audit_map_FK_upd_by_id ON x_audit_map(upd_by_id);
CREATE INDEX x_audit_map_FK_res_id ON x_audit_map(res_id);
CREATE INDEX x_audit_map_FK_group_id ON x_audit_map(group_id);
CREATE INDEX x_audit_map_FK_user_id ON x_audit_map(user_id);
CREATE INDEX x_audit_map_cr_time ON x_audit_map(create_time);
CREATE INDEX x_audit_map_up_time ON x_audit_map(update_time);
CREATE INDEX x_auth_sess_FK_added_by_id ON x_auth_sess(added_by_id);
CREATE INDEX x_auth_sess_FK_upd_by_id ON x_auth_sess(upd_by_id);
CREATE INDEX x_auth_sess_FK_user_id ON x_auth_sess(user_id);
CREATE INDEX x_auth_sess_cr_time ON x_auth_sess(create_time);
CREATE INDEX x_auth_sess_up_time ON x_auth_sess(update_time);
CREATE INDEX x_cred_store_FK_added_by_id ON x_cred_store(added_by_id);
CREATE INDEX x_cred_store_FK_upd_by_id ON x_cred_store(upd_by_id);
CREATE INDEX x_cred_store_cr_time ON x_cred_store(create_time);
CREATE INDEX x_cred_store_up_time ON x_cred_store(update_time);
CREATE INDEX x_db_base_FK_added_by_id ON x_db_base(added_by_id);
CREATE INDEX x_db_base_FK_upd_by_id ON x_db_base(upd_by_id);
CREATE INDEX x_db_base_cr_time ON x_db_base(create_time);
CREATE INDEX x_db_base_up_time ON x_db_base(update_time);
CREATE INDEX x_group_FK_added_by_id ON x_group(added_by_id);
CREATE INDEX x_group_FK_upd_by_id ON x_group(upd_by_id);
CREATE INDEX x_group_FK_cred_store_id ON x_group(cred_store_id);
CREATE INDEX x_group_cr_time ON x_group(create_time);
CREATE INDEX x_group_up_time ON x_group(update_time);
CREATE INDEX x_group_groups_FK_added_by_id ON x_group_groups(added_by_id);
CREATE INDEX x_group_groups_FK_upd_by_id ON x_group_groups(upd_by_id);
CREATE INDEX x_group_groups_FK_p_group_id ON x_group_groups(p_group_id);
CREATE INDEX x_group_groups_FK_group_id ON x_group_groups(group_id);
CREATE INDEX x_group_groups_cr_time ON x_group_groups(create_time);
CREATE INDEX x_group_groups_up_time ON x_group_groups(update_time);
CREATE INDEX x_group_users_FK_added_by_id ON x_group_users(added_by_id);
CREATE INDEX x_group_users_FK_upd_by_id ON x_group_users(upd_by_id);
CREATE INDEX x_group_users_FK_p_group_id ON x_group_users(p_group_id);
CREATE INDEX x_group_users_FK_user_id ON x_group_users(user_id);
CREATE INDEX x_group_users_cr_time ON x_group_users(create_time);
CREATE INDEX x_group_users_up_time ON x_group_users(update_time);
CREATE INDEX x_perm_map_FK_added_by_id ON x_perm_map(added_by_id);
CREATE INDEX x_perm_map_FK_upd_by_id ON x_perm_map(upd_by_id);
CREATE INDEX x_perm_map_FK_res_id ON x_perm_map(res_id);
CREATE INDEX x_perm_map_FK_group_id ON x_perm_map(group_id);
CREATE INDEX x_perm_map_FK_user_id ON x_perm_map(user_id);
CREATE INDEX x_perm_map_cr_time ON x_perm_map(create_time);
CREATE INDEX x_perm_map_up_time ON x_perm_map(update_time);
CREATE INDEX x_policy_export_audit_FK_added ON x_policy_export_audit(added_by_id);
CREATE INDEX x_policy_export_audit_FK_upd ON x_policy_export_audit(upd_by_id);
CREATE INDEX x_policy_export_audit_cr_time ON x_policy_export_audit(create_time);
CREATE INDEX x_policy_export_audit_up_time ON x_policy_export_audit(update_time);
CREATE INDEX x_portal_user_FK_added_by_id ON x_portal_user(added_by_id);
CREATE INDEX x_portal_user_FK_upd_by_id ON x_portal_user(upd_by_id);
CREATE INDEX x_portal_user_cr_time ON x_portal_user(create_time);
CREATE INDEX x_portal_user_up_time ON x_portal_user(update_time);
CREATE INDEX x_portal_user_name ON x_portal_user(first_name);
CREATE INDEX x_portal_user_role_FK_added ON x_portal_user_role(added_by_id);
CREATE INDEX x_portal_user_role_FK_upd ON x_portal_user_role(upd_by_id);
CREATE INDEX x_portal_user_role_FK_user_id ON x_portal_user_role(user_id);
CREATE INDEX x_portal_user_role_cr_time ON x_portal_user_role(create_time);
CREATE INDEX x_portal_user_role_up_time ON x_portal_user_role(update_time);
CREATE INDEX x_resource_FK_added_by_id ON x_resource(added_by_id);
CREATE INDEX x_resource_FK_upd_by_id ON x_resource(upd_by_id);
CREATE INDEX x_resource_FK_asset_id ON x_resource(asset_id);
CREATE INDEX x_resource_FK_parent_id ON x_resource(parent_id);
CREATE INDEX x_resource_cr_time ON x_resource(create_time);
CREATE INDEX x_resource_up_time ON x_resource(update_time);
CREATE INDEX x_trx_log_FK_added_by_id ON x_trx_log(added_by_id);
CREATE INDEX x_trx_log_FK_upd_by_id ON x_trx_log(upd_by_id);
CREATE INDEX x_trx_log_cr_time ON x_trx_log(create_time);
CREATE INDEX x_trx_log_up_time ON x_trx_log(update_time);
CREATE INDEX x_user_FK_added_by_id ON x_user(added_by_id);
CREATE INDEX x_user_FK_upd_by_id ON x_user(upd_by_id);
CREATE INDEX x_user_FK_cred_store_id ON x_user(cred_store_id);
CREATE INDEX x_user_cr_time ON x_user(create_time);
CREATE INDEX x_user_up_time ON x_user(update_time);
CREATE INDEX x_usr_module_perm_idx_moduleid ON x_user_module_perm(module_id);
CREATE INDEX x_usr_module_perm_idx_userid ON x_user_module_perm(user_id);
CREATE INDEX x_grp_module_perm_idx_groupid ON x_group_module_perm(group_id);
CREATE INDEX x_grp_module_perm_idx_moduleid ON x_group_module_perm(module_id);
CREATE INDEX x_service_def_added_by_id ON x_service_def(added_by_id);
CREATE INDEX x_service_def_upd_by_id ON x_service_def(upd_by_id);
CREATE INDEX x_service_def_cr_time ON x_service_def(create_time);
CREATE INDEX x_service_def_up_time ON x_service_def(update_time);
CREATE INDEX x_service_added_by_id ON x_service(added_by_id);
CREATE INDEX x_service_upd_by_id ON x_service(upd_by_id);
CREATE INDEX x_service_cr_time ON x_service(create_time);
CREATE INDEX x_service_up_time ON x_service(update_time);
CREATE INDEX x_service_type ON x_service(type);
CREATE INDEX x_policy_added_by_id ON x_policy(added_by_id);
CREATE INDEX x_policy_upd_by_id ON x_policy(upd_by_id);
CREATE INDEX x_policy_cr_time ON x_policy(create_time);
CREATE INDEX x_policy_up_time ON x_policy(update_time);
CREATE INDEX x_policy_service ON x_policy(service);
CREATE INDEX x_resource_def_parent ON x_resource_def(parent);
CREATE INDEX x_policy_resource_signature ON x_policy(resource_signature);
CREATE INDEX x_tag_def_IDX_added_by_id ON x_tag_def(added_by_id);
CREATE INDEX x_tag_def_IDX_upd_by_id ON x_tag_def(upd_by_id);
CREATE INDEX x_tag_IDX_type ON x_tag(type);
CREATE INDEX x_tag_IDX_added_by_id ON x_tag(added_by_id);
CREATE INDEX x_tag_IDX_upd_by_id ON x_tag(upd_by_id);
CREATE INDEX x_service_res_IDX_added_by_id ON x_service_resource(added_by_id);
CREATE INDEX x_service_res_IDX_upd_by_id ON x_service_resource(upd_by_id);
CREATE INDEX x_tag_res_map_IDX_tag_id ON x_tag_resource_map(tag_id);
CREATE INDEX x_tag_res_map_IDX_res_id ON x_tag_resource_map(res_id);
CREATE INDEX x_tag_res_map_IDX_added_by_id ON x_tag_resource_map(added_by_id);
CREATE INDEX x_tag_res_map_IDX_upd_by_id ON x_tag_resource_map(upd_by_id);
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
CREATE INDEX x_plugin_info_IDX_service_name ON x_plugin_info(service_name);
CREATE INDEX x_plugin_info_IDX_host_name ON x_plugin_info(host_name);
CREATE INDEX x_policy_label_label_id ON x_policy_label(id);
CREATE INDEX x_policy_label_label_name ON x_policy_label(label_name);
CREATE INDEX x_policy_label_label_map_id ON x_policy_label_map(id);
CREATE INDEX x_ugsync_audit_info_etime ON x_ugsync_audit_info(event_time);
CREATE INDEX x_ugsync_audit_info_sync_src ON x_ugsync_audit_info(sync_source);
CREATE INDEX x_ugsync_audit_info_uname ON x_ugsync_audit_info(user_name);
CREATE INDEX x_data_hist_idx_objid_objclstype ON x_data_hist(obj_id,obj_class_type);

CREATE INDEX x_rms_service_resource_IDX_service_id ON x_rms_service_resource(service_id);
CREATE INDEX x_rms_service_resource_IDX_resource_signature ON x_rms_service_resource(resource_signature);
CREATE INDEX x_rms_notification_IDX_notification_id ON x_rms_notification(notification_id);
CREATE INDEX x_rms_notification_IDX_hms_name_notification_id ON x_rms_notification(hms_name, notification_id);
CREATE INDEX x_rms_notification_IDX_hl_service_id ON x_rms_notification(hl_service_id);
CREATE INDEX x_rms_notification_IDX_ll_service_id ON x_rms_notification(ll_service_id);
CREATE INDEX x_rms_resource_mapping_IDX_hl_resource_id ON x_rms_resource_mapping(hl_resource_id);
CREATE INDEX x_rms_resource_mapping_IDX_ll_resource_id ON x_rms_resource_mapping(ll_resource_id);

CREATE OR REPLACE FUNCTION getXportalUIdByLoginId(input_val varchar(100))
RETURNS bigint LANGUAGE SQL AS $$ SELECT x_portal_user.id FROM x_portal_user
WHERE x_portal_user.login_id = $1; $$;

CREATE OR REPLACE FUNCTION getModulesIdByName(input_val varchar(100))
RETURNS bigint LANGUAGE SQL AS $$ SELECT x_modules_master.id FROM x_modules_master
WHERE x_modules_master.module = $1; $$;

INSERT INTO x_portal_user(CREATE_TIME,UPDATE_TIME,FIRST_NAME,LAST_NAME,PUB_SCR_NAME,LOGIN_ID,PASSWORD,EMAIL,STATUS)VALUES(current_timestamp,current_timestamp,'Admin','','Admin','admin','ceb4f32325eda6142bd65215f4c0f371','',1);
INSERT INTO x_portal_user_role(CREATE_TIME,UPDATE_TIME,USER_ID,USER_ROLE,STATUS)VALUES(current_timestamp,current_timestamp,getXportalUIdByLoginId('admin'),'ROLE_SYS_ADMIN',1);
INSERT INTO x_user(CREATE_TIME,UPDATE_TIME,user_name,status,descr)VALUES(current_timestamp,current_timestamp,'admin',0,'Administrator');
INSERT INTO x_group(CREATE_TIME,DESCR,GROUP_SRC,GROUP_TYPE,GROUP_NAME,STATUS,UPDATE_TIME,UPD_BY_ID)VALUES(CURRENT_TIMESTAMP,'public group',0,0,'public',0,CURRENT_TIMESTAMP,getXportalUIdByLoginId('admin'));

INSERT INTO x_modules_master(create_time,update_time,added_by_id,upd_by_id,module,url) VALUES(current_timestamp,current_timestamp,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),'Resource Based Policies','');
INSERT INTO x_modules_master(create_time,update_time,added_by_id,upd_by_id,module,url) VALUES(current_timestamp,current_timestamp,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),'Users/Groups','');
INSERT INTO x_modules_master(create_time,update_time,added_by_id,upd_by_id,module,url) VALUES(current_timestamp,current_timestamp,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),'Reports','');
INSERT INTO x_modules_master(create_time,update_time,added_by_id,upd_by_id,module,url) VALUES(current_timestamp,current_timestamp,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),'Audit','');
INSERT INTO x_modules_master(create_time,update_time,added_by_id,upd_by_id,module,url) VALUES(current_timestamp,current_timestamp,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),'Key Manager','');

INSERT INTO x_portal_user(CREATE_TIME,UPDATE_TIME,FIRST_NAME,LAST_NAME,PUB_SCR_NAME,LOGIN_ID,PASSWORD,EMAIL,STATUS)VALUES(current_timestamp,current_timestamp,'rangerusersync','','rangerusersync','rangerusersync','70b8374d3dfe0325aaa5002a688c7e3b','rangerusersync',1);
INSERT INTO x_portal_user_role(CREATE_TIME,UPDATE_TIME,USER_ID,USER_ROLE,STATUS)VALUES(current_timestamp,current_timestamp,getXportalUIdByLoginId('rangerusersync'),'ROLE_SYS_ADMIN',1);
INSERT INTO x_user(CREATE_TIME,UPDATE_TIME,user_name,status,descr)VALUES(current_timestamp,current_timestamp,'rangerusersync',0,'rangerusersync');

INSERT INTO x_portal_user(CREATE_TIME,UPDATE_TIME,FIRST_NAME,LAST_NAME,PUB_SCR_NAME,LOGIN_ID,PASSWORD,EMAIL,STATUS)VALUES(current_timestamp,current_timestamp,'keyadmin','','keyadmin','keyadmin','a05f34d2dce2b4688fa82e82a89ba958','keyadmin',1);
INSERT INTO x_portal_user_role(CREATE_TIME,UPDATE_TIME,USER_ID,USER_ROLE,STATUS)VALUES(current_timestamp,current_timestamp,getXportalUIdByLoginId('keyadmin'),'ROLE_KEY_ADMIN',1);
INSERT INTO x_user(CREATE_TIME,UPDATE_TIME,user_name,status,descr)VALUES(current_timestamp,current_timestamp,'keyadmin',0,'keyadmin');

INSERT INTO x_portal_user(CREATE_TIME,UPDATE_TIME,FIRST_NAME,LAST_NAME,PUB_SCR_NAME,LOGIN_ID,PASSWORD,EMAIL,STATUS)VALUES(current_timestamp,current_timestamp,'rangertagsync','','rangertagsync','rangertagsync','f5820e1229418dcf2575908f2c493da5','rangertagsync',1);
INSERT INTO x_portal_user_role(CREATE_TIME,UPDATE_TIME,USER_ID,USER_ROLE,STATUS)VALUES(current_timestamp,current_timestamp,getXportalUIdByLoginId('rangertagsync'),'ROLE_SYS_ADMIN',1);
INSERT INTO x_user(CREATE_TIME,UPDATE_TIME,user_name,status,descr)VALUES(current_timestamp,current_timestamp,'rangertagsync',0,'rangertagsync');

INSERT INTO x_modules_master(create_time,update_time,added_by_id,upd_by_id,module,url) VALUES(current_timestamp,current_timestamp,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),'Tag Based Policies','');
INSERT INTO x_modules_master(create_time,update_time,added_by_id,upd_by_id,module,url) VALUES(current_timestamp,current_timestamp,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),'Security Zone','');
INSERT INTO x_security_zone(create_time, update_time, added_by_id, upd_by_id, version, name, jsonData, description) VALUES (current_timestamp, current_timestamp, getXportalUIdByLoginId('admin'), getXportalUIdByLoginId('admin'), 1, ' ', '', 'Unzoned zone');

INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('CORE_DB_SCHEMA',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('016',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('018',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('019',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('020',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('021',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('022',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('023',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('024',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('025',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('026',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('027',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('028',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('029',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('030',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('031',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('032',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('033',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('034',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('035',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('036',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('037',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('038',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('039',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('040',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('041',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('042',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('043',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('044',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('045',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('046',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('047',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('048',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('049',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('050',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('051',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('052',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('054',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('055',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('056',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('057',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('058',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('059',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('DB_PATCHES',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');

INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES
(getXportalUIdByLoginId('admin'),getModulesIdByName('Reports'),current_timestamp,current_timestamp,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES
(getXportalUIdByLoginId('admin'),getModulesIdByName('Resource Based Policies'),current_timestamp,current_timestamp,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES
(getXportalUIdByLoginId('admin'),getModulesIdByName('Audit'),current_timestamp,current_timestamp,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES
(getXportalUIdByLoginId('admin'),getModulesIdByName('Users/Groups'),current_timestamp,current_timestamp,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES
(getXportalUIdByLoginId('admin'),getModulesIdByName('Tag Based Policies'),current_timestamp,current_timestamp,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES
(getXportalUIdByLoginId('rangerusersync'),getModulesIdByName('Reports'),current_timestamp,current_timestamp,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES
(getXportalUIdByLoginId('rangerusersync'),getModulesIdByName('Resource Based Policies'),current_timestamp,current_timestamp,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES
(getXportalUIdByLoginId('rangerusersync'),getModulesIdByName('Audit'),current_timestamp,current_timestamp,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES
(getXportalUIdByLoginId('rangerusersync'),getModulesIdByName('Users/Groups'),current_timestamp,current_timestamp,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES
(getXportalUIdByLoginId('rangerusersync'),getModulesIdByName('Tag Based Policies'),current_timestamp,current_timestamp,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES
(getXportalUIdByLoginId('keyadmin'),getModulesIdByName('Key Manager'),current_timestamp,current_timestamp,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES
(getXportalUIdByLoginId('keyadmin'),getModulesIdByName('Reports'),current_timestamp,current_timestamp,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES
(getXportalUIdByLoginId('keyadmin'),getModulesIdByName('Resource Based Policies'),current_timestamp,current_timestamp,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES
(getXportalUIdByLoginId('rangertagsync'),getModulesIdByName('Reports'),current_timestamp,current_timestamp,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES
(getXportalUIdByLoginId('rangertagsync'),getModulesIdByName('Resource Based Policies'),current_timestamp,current_timestamp,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES
(getXportalUIdByLoginId('rangertagsync'),getModulesIdByName('Audit'),current_timestamp,current_timestamp,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES
(getXportalUIdByLoginId('rangertagsync'),getModulesIdByName('Users/Groups'),current_timestamp,current_timestamp,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES
(getXportalUIdByLoginId('rangertagsync'),getModulesIdByName('Tag Based Policies'),current_timestamp,current_timestamp,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES
(getXportalUIdByLoginId('keyadmin'),getModulesIdByName('Users/Groups'),current_timestamp,current_timestamp,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES
(getXportalUIdByLoginId('keyadmin'),getModulesIdByName('Audit'),current_timestamp,current_timestamp,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (getXportalUIdByLoginId('admin'),getModulesIdByName('Security Zone'),current_timestamp,current_timestamp,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (getXportalUIdByLoginId('rangerusersync'),getModulesIdByName('Security Zone'),current_timestamp,current_timestamp,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (getXportalUIdByLoginId('rangertagsync'),getModulesIdByName('Security Zone'),current_timestamp,current_timestamp,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),1);


INSERT INTO x_ranger_global_state (create_time,update_time,added_by_id,upd_by_id,version,state_name,app_data) VALUES (current_timestamp,current_timestamp,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),1,'RangerRole','{"Version":"1"}');
INSERT INTO x_ranger_global_state (create_time,update_time,added_by_id,upd_by_id,version,state_name,app_data) VALUES (current_timestamp,current_timestamp,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),1,'RangerUserStore','{"Version":"1"}');
INSERT INTO x_ranger_global_state (create_time,update_time,added_by_id,upd_by_id,version,state_name,app_data) VALUES (current_timestamp,current_timestamp,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),1,'RangerSecurityZone','{"Version":"1"}');

INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10001',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10002',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10003',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10004',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10005',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10006',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10007',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10008',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10009',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10010',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10011',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10012',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10013',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10014',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10015',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10016',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10017',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10019',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10020',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10025',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10026',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10027',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10028',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10030',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10033',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10034',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10035',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10036',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10037',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10038',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10040',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10041',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10043',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10044',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10045',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10046',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10047',current_timestamp,'Ranger 2.2.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10049',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10050',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10052',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10053',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10054',current_timestamp,'Ranger 3.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10055',current_timestamp,'Ranger 3.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10056',current_timestamp,'Ranger 3.0.0',current_timestamp,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('JAVA_PATCHES',current_timestamp,'Ranger 1.0.0',current_timestamp,'localhost','Y');

DROP VIEW IF EXISTS vx_trx_log;
CREATE VIEW vx_trx_log AS select x_trx_log.id AS id,x_trx_log.create_time AS create_time,x_trx_log.update_time AS update_time,x_trx_log.added_by_id AS added_by_id,x_trx_log.upd_by_id AS upd_by_id,x_trx_log.class_type AS class_type,x_trx_log.object_id AS object_id,x_trx_log.parent_object_id AS parent_object_id,x_trx_log.parent_object_class_type AS parent_object_class_type,x_trx_log.attr_name AS attr_name,x_trx_log.parent_object_name AS parent_object_name,x_trx_log.object_name AS object_name,x_trx_log.prev_val AS prev_val,x_trx_log.new_val AS new_val,x_trx_log.trx_id AS trx_id,x_trx_log.action AS action,x_trx_log.sess_id AS sess_id,x_trx_log.req_id AS req_id,x_trx_log.sess_type AS sess_type from x_trx_log where id in(select min(x_trx_log.id) from x_trx_log group by x_trx_log.trx_id);
