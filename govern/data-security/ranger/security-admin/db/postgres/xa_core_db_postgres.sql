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

DROP TABLE IF EXISTS x_portal_user CASCADE;
DROP SEQUENCE IF EXISTS x_portal_user_seq;
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
PRIMARY KEY(id),
CONSTRAINT x_portal_user_UK_login_id UNIQUE(login_id),
CONSTRAINT x_portal_user_UK_email UNIQUE(email),
CONSTRAINT x_portal_user_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_portal_user_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);

DROP TABLE IF EXISTS x_asset CASCADE;
DROP SEQUENCE IF EXISTS x_asset_seq;
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

DROP TABLE IF EXISTS x_cred_store CASCADE;
DROP SEQUENCE IF EXISTS x_cred_store_seq;
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

DROP TABLE IF EXISTS x_group CASCADE;
DROP SEQUENCE IF EXISTS x_group_seq;
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
PRIMARY KEY(ID),
CONSTRAINT X_GROUP_FK_ADDED_BY_ID FOREIGN KEY(ADDED_BY_ID) REFERENCES X_PORTAL_USER(ID),
CONSTRAINT X_GROUP_FK_CRED_STORE_ID FOREIGN KEY(CRED_STORE_ID) REFERENCES X_CRED_STORE(ID),
CONSTRAINT X_GROUP_FK_UPD_BY_ID FOREIGN KEY(UPD_BY_ID) REFERENCES X_PORTAL_USER(ID)
);

DROP TABLE IF EXISTS x_user CASCADE;
DROP SEQUENCE IF EXISTS x_user_seq;
CREATE SEQUENCE x_user_seq;
CREATE TABLE x_user(
id BIGINT DEFAULT nextval('x_user_seq'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
user_name VARCHAR(1024) NOT NULL,
descr VARCHAR(4000) DEFAULT NULL NULL,
status INT DEFAULT '0' NOT NULL,
cred_store_id BIGINT DEFAULT NULL NULL,
is_visible INT DEFAULT '1' NOT NULL,
PRIMARY KEY(id),
CONSTRAINT x_user_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_user_FK_cred_store_id FOREIGN KEY(cred_store_id) REFERENCES x_cred_store(id),
CONSTRAINT x_user_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);

DROP TABLE IF EXISTS x_resource CASCADE;
DROP SEQUENCE IF EXISTS x_resource_seq;
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

DROP TABLE IF EXISTS x_audit_map CASCADE;
DROP SEQUENCE IF EXISTS x_audit_map_seq;
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

DROP TABLE IF EXISTS x_auth_sess CASCADE;
DROP SEQUENCE IF EXISTS x_auth_sess_seq;
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

DROP TABLE IF EXISTS x_db_base CASCADE;
DROP SEQUENCE IF EXISTS x_db_base_seq;
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

DROP TABLE IF EXISTS x_group_groups;
DROP SEQUENCE IF EXISTS x_group_groups_seq;
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

DROP TABLE IF EXISTS x_group_users CASCADE;
DROP SEQUENCE IF EXISTS x_group_users_seq;
CREATE SEQUENCE x_group_users_seq;
CREATE TABLE x_group_users(
id BIGINT DEFAULT nextval('x_group_users_seq'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
group_name VARCHAR(1024) NOT NULL,
p_group_id BIGINT DEFAULT NULL NULL,
user_id BIGINT DEFAULT NULL NULL,
PRIMARY KEY(id),
CONSTRAINT x_group_users_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_group_users_FK_p_group_id FOREIGN KEY(p_group_id) REFERENCES x_group(id),
CONSTRAINT x_group_users_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_group_users_FK_user_id FOREIGN KEY(user_id) REFERENCES x_user(id)
);

DROP TABLE IF EXISTS x_perm_map CASCADE;
DROP SEQUENCE IF EXISTS x_perm_map_seq;
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

DROP TABLE IF EXISTS x_policy_export_audit CASCADE;
DROP SEQUENCE IF EXISTS x_policy_export_seq;
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
PRIMARY KEY(id),
CONSTRAINT x_policy_export_audit_FK_added FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_policy_export_audit_FK_upd FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);

DROP TABLE IF EXISTS x_portal_user_role CASCADE;
DROP SEQUENCE IF EXISTS x_portal_user_role_seq;
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

DROP TABLE IF EXISTS x_trx_log CASCADE;
DROP SEQUENCE IF EXISTS x_trx_log_seq;
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

DROP TABLE IF EXISTS xa_access_audit CASCADE;
DROP SEQUENCE IF EXISTS xa_access_audit_seq;
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

DROP TABLE IF EXISTS x_service_def CASCADE;
DROP SEQUENCE IF EXISTS x_service_def_seq;
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
impl_class_name VARCHAR(1024) DEFAULT NULL NULL,
label VARCHAR(1024) DEFAULT NULL NULL,
description VARCHAR(1024) DEFAULT NULL NULL,
rb_key_label VARCHAR(1024) DEFAULT NULL NULL,
rb_key_description VARCHAR(1024) DEFAULT NULL NULL,
is_enabled BOOLEAN DEFAULT '1' NULL,
PRIMARY KEY(id),
CONSTRAINT x_service_def_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_service_def_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);
DROP TABLE IF EXISTS x_service CASCADE;
DROP SEQUENCE IF EXISTS x_service_seq;
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
policy_version BIGINT DEFAULT NULL NULL,
policy_update_time TIMESTAMP DEFAULT NULL NULL,
description VARCHAR(1024) DEFAULT NULL NULL,
is_enabled BOOLEAN DEFAULT '0' NOT NULL,
primary key(id),
CONSTRAINT x_service_name UNIQUE(name),
CONSTRAINT x_service_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_service_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_service_FK_type FOREIGN KEY(type) REFERENCES x_service_def(id)
);
DROP TABLE IF EXISTS x_policy CASCADE;
DROP SEQUENCE IF EXISTS x_policy_seq;
CREATE SEQUENCE x_policy_seq;
CREATE TABLE x_policy(
id BIGINT DEFAULT nextval('x_policy_seq'::regclass),
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
version BIGINT DEFAULT NULL NULL,
service BIGINT DEFAULT NULL NULL,
name VARCHAR(512) DEFAULT NULL NULL,
policy_type int DEFAULT 0 NULL,
description VARCHAR(1024) DEFAULT NULL NULL,
resource_signature VARCHAR(128) DEFAULT NULL NULL,
is_enabled BOOLEAN DEFAULT '0' NOT NULL,
is_audit_enabled BOOLEAN DEFAULT '0' NOT NULL,
primary key(id),
CONSTRAINT x_policy_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_policy_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_policy_FK_service FOREIGN KEY(service) REFERENCES x_service(id)
);
DROP TABLE IF EXISTS x_service_config_def CASCADE;
DROP SEQUENCE IF EXISTS x_service_config_def_seq;
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
sort_order SMALLINT DEFAULT '0' NULL,
primary key(id),
CONSTRAINT x_service_conf_def_FK_defid FOREIGN KEY(def_id) REFERENCES x_service_def(id),
CONSTRAINT x_service_conf_def_FK_added_by FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_service_conf_def_FK_upd_by FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);
DROP TABLE IF EXISTS x_resource_def CASCADE;
DROP SEQUENCE IF EXISTS x_resource_def_seq;
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
sort_order SMALLINT DEFAULT '0' NULL,
primary key(id),
CONSTRAINT x_resource_def_FK_parent FOREIGN KEY(parent) REFERENCES x_resource_def(id),
CONSTRAINT x_resource_def_FK_defid FOREIGN KEY(def_id) REFERENCES x_service_def(id),
CONSTRAINT x_resource_def_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_resource_def_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);
DROP TABLE IF EXISTS x_access_type_def CASCADE;
DROP SEQUENCE IF EXISTS x_access_type_def_seq;
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
sort_order SMALLINT DEFAULT '0' NULL,
primary key(id),
CONSTRAINT x_access_type_def_FK_defid FOREIGN KEY(def_id) REFERENCES x_service_def(id),
CONSTRAINT x_access_type_def_FK_added_by FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_access_type_def_FK_upd_by FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);
DROP TABLE IF EXISTS x_access_type_def_grants CASCADE;
DROP SEQUENCE IF EXISTS x_access_type_def_grants_seq;
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
DROP TABLE IF EXISTS x_policy_condition_def CASCADE;
DROP SEQUENCE IF EXISTS x_policy_condition_def_seq;
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
sort_order SMALLINT DEFAULT '0' NULL,
primary key(id),
CONSTRAINT x_policy_cond_def_FK_defid FOREIGN KEY(def_id) REFERENCES x_service_def(id),
CONSTRAINT x_policy_cond_def_FK_added_by FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_policy_cond_def_FK_upd_by FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);
DROP TABLE IF EXISTS x_context_enricher_def CASCADE;
DROP SEQUENCE IF EXISTS x_context_enricher_def_seq;
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
sort_order SMALLINT DEFAULT '0' NULL,
primary key(id),
CONSTRAINT x_context_enricher_def_FK_defid FOREIGN KEY(def_id) REFERENCES x_service_def(id),
CONSTRAINT x_context_enricher_def_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_context_enricher_def_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);
DROP TABLE IF EXISTS x_enum_def CASCADE;
DROP SEQUENCE IF EXISTS x_enum_def_seq;
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
DROP TABLE IF EXISTS x_enum_element_def CASCADE;
DROP SEQUENCE IF EXISTS x_enum_element_def_seq;
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
sort_order SMALLINT DEFAULT '0' NULL,
primary key(id),
CONSTRAINT x_enum_element_def_FK_defid FOREIGN KEY(enum_def_id) REFERENCES x_enum_def(id),
CONSTRAINT x_enum_element_def_FK_added_by FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_enum_element_def_FK_upd_by FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);
DROP TABLE IF EXISTS x_service_config_map CASCADE;
DROP SEQUENCE IF EXISTS x_service_config_map_seq;
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
DROP TABLE IF EXISTS x_policy_resource CASCADE;
DROP SEQUENCE IF EXISTS x_policy_resource_seq;
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
DROP TABLE IF EXISTS x_policy_resource_map CASCADE;
DROP SEQUENCE IF EXISTS x_policy_resource_map_seq;
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
sort_order SMALLINT DEFAULT '0' NULL,
primary key(id),
CONSTRAINT x_policy_res_map_FK_res_id FOREIGN KEY(resource_id) REFERENCES x_policy_resource(id),
CONSTRAINT x_policy_res_map_FK_added_by FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_policy_res_map_FK_upd_by FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);
DROP TABLE IF EXISTS x_policy_item CASCADE;
DROP SEQUENCE IF EXISTS x_policy_item_seq;
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
sort_order SMALLINT DEFAULT '0' NULL,
primary key(id),
CONSTRAINT x_policy_item_FK_policy_id FOREIGN KEY(policy_id) REFERENCES x_policy(id),
CONSTRAINT x_policy_item_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_policy_item_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);
DROP TABLE IF EXISTS x_policy_item_access CASCADE;
DROP SEQUENCE IF EXISTS x_policy_item_access_seq;
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
sort_order SMALLINT DEFAULT '0' NULL,
primary key(id),
CONSTRAINT x_plc_item_access_FK_pi_id FOREIGN KEY(policy_item_id) REFERENCES x_policy_item(id),
CONSTRAINT x_plc_item_access_FK_atd_id FOREIGN KEY(type) REFERENCES x_access_type_def(id),
CONSTRAINT x_plc_item_access_FK_added_by FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_plc_item_access_FK_upd_by FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);
DROP TABLE IF EXISTS x_policy_item_condition CASCADE;
DROP SEQUENCE IF EXISTS x_policy_item_condition_seq;
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
sort_order SMALLINT DEFAULT '0' NULL,
primary key(id),
CONSTRAINT x_plc_item_cond_FK_pi_id FOREIGN KEY(policy_item_id) REFERENCES x_policy_item(id),
CONSTRAINT x_plc_item_cond_FK_pcd_id FOREIGN KEY(type) REFERENCES x_policy_condition_def(id),
CONSTRAINT x_plc_item_cond_FK_added_by_id FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_plc_item_cond_FK_upd_by_id FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);
DROP TABLE IF EXISTS x_policy_item_user_perm CASCADE;
DROP SEQUENCE IF EXISTS x_policy_item_user_perm_seq;
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
sort_order SMALLINT DEFAULT '0' NULL,
primary key(id),
CONSTRAINT x_plc_itm_usr_perm_FK_pi_id FOREIGN KEY(policy_item_id) REFERENCES x_policy_item(id),
CONSTRAINT x_plc_itm_usr_perm_FK_user_id FOREIGN KEY(user_id) REFERENCES x_user(id),
CONSTRAINT x_plc_itm_usr_perm_FK_added_by FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_plc_itm_usr_perm_FK_upd_by FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);
DROP TABLE IF EXISTS x_policy_item_group_perm CASCADE;
DROP SEQUENCE IF EXISTS x_policy_item_group_perm_seq;
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
sort_order SMALLINT DEFAULT '0' NULL,
primary key(id),
CONSTRAINT x_plc_itm_grp_perm_FK_pi_id FOREIGN KEY(policy_item_id) REFERENCES x_policy_item(id),
CONSTRAINT x_plc_itm_grp_perm_FK_group_id FOREIGN KEY(group_id) REFERENCES x_group(id),
CONSTRAINT x_plc_itm_grp_perm_FK_added_by FOREIGN KEY(added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_plc_itm_grp_perm_FK_upd_by FOREIGN KEY(upd_by_id) REFERENCES x_portal_user(id)
);
DROP TABLE IF EXISTS x_data_hist CASCADE;
DROP SEQUENCE IF EXISTS x_data_hist_seq;
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
DROP VIEW IF EXISTS vx_trx_log;
CREATE VIEW vx_trx_log AS select x_trx_log.id AS id,x_trx_log.create_time AS create_time,x_trx_log.update_time AS update_time,x_trx_log.added_by_id AS added_by_id,x_trx_log.upd_by_id AS upd_by_id,x_trx_log.class_type AS class_type,x_trx_log.object_id AS object_id,x_trx_log.parent_object_id AS parent_object_id,x_trx_log.parent_object_class_type AS parent_object_class_type,x_trx_log.attr_name AS attr_name,x_trx_log.parent_object_name AS parent_object_name,x_trx_log.object_name AS object_name,x_trx_log.prev_val AS prev_val,x_trx_log.new_val AS new_val,x_trx_log.trx_id AS trx_id,x_trx_log.action AS action,x_trx_log.sess_id AS sess_id,x_trx_log.req_id AS req_id,x_trx_log.sess_type AS sess_type from x_trx_log where id in(select min(x_trx_log.id) from x_trx_log group by x_trx_log.trx_id);

INSERT INTO x_portal_user(CREATE_TIME,UPDATE_TIME,FIRST_NAME,LAST_NAME,PUB_SCR_NAME,LOGIN_ID,PASSWORD,EMAIL,STATUS)VALUES(current_timestamp,current_timestamp,'Admin','','Admin','admin','ceb4f32325eda6142bd65215f4c0f371','',1);
INSERT INTO x_portal_user_role(CREATE_TIME,UPDATE_TIME,USER_ID,USER_ROLE,STATUS)VALUES(current_timestamp,current_timestamp,1,'ROLE_SYS_ADMIN',1);
INSERT INTO x_user(CREATE_TIME,UPDATE_TIME,user_name,status,descr)VALUES(current_timestamp,current_timestamp,'admin',0,'Administrator');
INSERT INTO x_group(CREATE_TIME,DESCR,GROUP_SRC,GROUP_TYPE,GROUP_NAME,STATUS,UPDATE_TIME,UPD_BY_ID)VALUES(CURRENT_TIMESTAMP,'public group',0,0,'public',0,CURRENT_TIMESTAMP,1);
COMMIT;

DROP TABLE IF EXISTS x_modules_master CASCADE;
DROP SEQUENCE IF EXISTS x_modules_master_seq;
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

INSERT INTO x_modules_master(create_time,update_time,added_by_id,upd_by_id,module,url) VALUES(current_timestamp,current_timestamp,1,1,'Resource Based Policies','');
INSERT INTO x_modules_master(create_time,update_time,added_by_id,upd_by_id,module,url) VALUES(current_timestamp,current_timestamp,1,1,'Users/Groups','');
INSERT INTO x_modules_master(create_time,update_time,added_by_id,upd_by_id,module,url) VALUES(current_timestamp,current_timestamp,1,1,'Reports','');
INSERT INTO x_modules_master(create_time,update_time,added_by_id,upd_by_id,module,url) VALUES(current_timestamp,current_timestamp,1,1,'Audit','');
INSERT INTO x_modules_master(create_time,update_time,added_by_id,upd_by_id,module,url) VALUES(current_timestamp,current_timestamp,1,1,'Key Manager','');

DROP TABLE IF EXISTS x_user_module_perm CASCADE;
DROP SEQUENCE IF EXISTS x_user_module_perm_seq;
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

DROP TABLE IF EXISTS x_group_module_perm CASCADE;
DROP SEQUENCE IF EXISTS x_group_module_perm_seq;
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

CREATE INDEX x_usr_module_perm_idx_moduleid ON x_user_module_perm(module_id);
CREATE INDEX x_usr_module_perm_idx_userid ON x_user_module_perm(user_id);
CREATE INDEX x_grp_module_perm_idx_groupid ON x_group_module_perm(group_id);
CREATE INDEX x_grp_module_perm_idx_moduleid ON x_group_module_perm(module_id);
COMMIT;
INSERT INTO x_portal_user(CREATE_TIME,UPDATE_TIME,FIRST_NAME,LAST_NAME,PUB_SCR_NAME,LOGIN_ID,PASSWORD,EMAIL,STATUS)VALUES(current_timestamp,current_timestamp,'rangerusersync','','rangerusersync','rangerusersync','70b8374d3dfe0325aaa5002a688c7e3b','rangerusersync',1);
INSERT INTO x_portal_user_role(CREATE_TIME,UPDATE_TIME,USER_ID,USER_ROLE,STATUS)VALUES(current_timestamp,current_timestamp,2,'ROLE_SYS_ADMIN',1);
INSERT INTO x_user(CREATE_TIME,UPDATE_TIME,user_name,status,descr)VALUES(current_timestamp,current_timestamp,'rangerusersync',0,'rangerusersync');
COMMIT;
INSERT INTO x_portal_user(CREATE_TIME,UPDATE_TIME,FIRST_NAME,LAST_NAME,PUB_SCR_NAME,LOGIN_ID,PASSWORD,EMAIL,STATUS)VALUES(current_timestamp,current_timestamp,'keyadmin','','keyadmin','keyadmin','a05f34d2dce2b4688fa82e82a89ba958','keyadmin',1);
INSERT INTO x_portal_user_role(CREATE_TIME,UPDATE_TIME,USER_ID,USER_ROLE,STATUS)VALUES(current_timestamp,current_timestamp,3,'ROLE_KEY_ADMIN',1);
INSERT INTO x_user(CREATE_TIME,UPDATE_TIME,user_name,status,descr)VALUES(current_timestamp,current_timestamp,'keyadmin',0,'keyadmin');
COMMIT;
INSERT INTO x_portal_user(CREATE_TIME,UPDATE_TIME,FIRST_NAME,LAST_NAME,PUB_SCR_NAME,LOGIN_ID,PASSWORD,EMAIL,STATUS)VALUES(current_timestamp,current_timestamp,'rangertagsync','','rangertagsync','rangertagsync','f5820e1229418dcf2575908f2c493da5','rangertagsync',1);
INSERT INTO x_portal_user_role(CREATE_TIME,UPDATE_TIME,USER_ID,USER_ROLE,STATUS)VALUES(current_timestamp,current_timestamp,4,'ROLE_SYS_ADMIN',1);
INSERT INTO x_user(CREATE_TIME,UPDATE_TIME,user_name,status,descr)VALUES(current_timestamp,current_timestamp,'rangertagsync',0,'rangertagsync');
COMMIT;
