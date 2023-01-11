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

CREATE OR REPLACE PROCEDURE spdropsequence(ObjName IN varchar2)
IS
v_counter integer;
BEGIN
    select count(*) into v_counter from user_sequences where sequence_name = upper(ObjName);
      if (v_counter > 0) then
        execute immediate 'DROP SEQUENCE ' || ObjName;
      end if;
END;/
/

call spdropsequence('X_SERVICE_DEF_SEQ');
call spdropsequence('X_SERVICE_SEQ');
call spdropsequence('X_POLICY_SEQ');
call spdropsequence('X_SERVICE_CONFIG_DEF_SEQ');
call spdropsequence('X_RESOURCE_DEF_SEQ');
call spdropsequence('X_ACCESS_TYPE_DEF_SEQ');
call spdropsequence('X_ACCESS_TYPE_DEF_GRANTS_SEQ');
call spdropsequence('X_POLICY_CONDITION_DEF_SEQ');
call spdropsequence('X_ENUM_DEF_SEQ');
call spdropsequence('X_ENUM_ELEMENT_DEF_SEQ');
call spdropsequence('X_SERVICE_CONFIG_MAP_SEQ');
call spdropsequence('X_POLICY_RESOURCE_SEQ');
call spdropsequence('X_POLICY_RESOURCE_MAP_SEQ');
call spdropsequence('X_POLICY_ITEM_SEQ');
call spdropsequence('X_POLICY_ITEM_ACCESS_SEQ');
call spdropsequence('X_POLICY_ITEM_CONDITION_SEQ');
call spdropsequence('X_CONTEXT_ENRICHER_DEF_SEQ');
call spdropsequence('X_POLICY_ITEM_USER_PERM_SEQ');
call spdropsequence('X_POLICY_ITEM_GROUP_PERM_SEQ');
call spdropsequence('X_DATA_HIST_SEQ');

CREATE OR REPLACE PROCEDURE spdroptable(ObjName IN varchar2)
IS
v_counter integer;
BEGIN
    select count(*) into v_counter from user_tables where table_name = upper(ObjName);
     if (v_counter > 0) then
     execute immediate 'drop table ' || ObjName || ' cascade constraints';
     end if;
END;/
/

call spdroptable('x_data_hist');
call spdroptable('x_policy_item_group_perm');
call spdroptable('x_policy_item_user_perm');
call spdroptable('x_policy_item_condition');
call spdroptable('x_policy_item_access');
call spdroptable('x_policy_item');
call spdroptable('x_policy_resource_map');
call spdroptable('x_policy_resource');
call spdroptable('x_service_config_map');
call spdroptable('x_enum_element_def');
call spdroptable('x_enum_def');
call spdroptable('x_context_enricher_def');
call spdroptable('x_policy_condition_def');
call spdroptable('x_access_type_def_grants');
call spdroptable('x_access_type_def');
call spdroptable('x_resource_def');
call spdroptable('x_service_config_def');
call spdroptable('x_policy');
call spdroptable('x_service');
call spdroptable('x_service_def')

CREATE SEQUENCE X_SERVICE_DEF_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE X_SERVICE_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE X_POLICY_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE X_SERVICE_CONFIG_DEF_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE X_RESOURCE_DEF_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE X_ACCESS_TYPE_DEF_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE X_ACCESS_TYPE_DEF_GRANTS_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE X_POLICY_CONDITION_DEF_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE X_ENUM_DEF_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE X_ENUM_ELEMENT_DEF_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE X_SERVICE_CONFIG_MAP_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE X_POLICY_RESOURCE_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE X_POLICY_RESOURCE_MAP_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE X_POLICY_ITEM_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE X_POLICY_ITEM_ACCESS_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE X_POLICY_ITEM_CONDITION_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE X_CONTEXT_ENRICHER_DEF_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE X_POLICY_ITEM_USER_PERM_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE X_POLICY_ITEM_GROUP_PERM_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE X_DATA_HIST_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
commit;
CREATE TABLE x_service_def (
id NUMBER(20) NOT NULL,
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
version NUMBER(20) DEFAULT NULL NULL,
name VARCHAR(1024) DEFAULT NULL NULL,
impl_class_name VARCHAR(1024) DEFAULT NULL NULL,
label VARCHAR(1024) DEFAULT NULL NULL,
description VARCHAR(1024) DEFAULT NULL NULL,
rb_key_label VARCHAR(1024) DEFAULT NULL NULL,
rb_key_description VARCHAR(1024) DEFAULT NULL NULL,
is_enabled NUMBER(1) DEFAULT '1' NULL,
PRIMARY KEY (id),
CONSTRAINT x_service_def_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_service_def_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
commit;
CREATE TABLE x_service(
id NUMBER(20) NOT NULL,
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
version NUMBER(20) DEFAULT NULL NULL,
type NUMBER(20) DEFAULT NULL NULL,
name varchar(255) DEFAULT NULL NULL,
policy_version NUMBER(20) DEFAULT NULL NULL,
policy_update_time DATE DEFAULT NULL NULL,
description VARCHAR(1024) DEFAULT NULL NULL,
is_enabled NUMBER(1) DEFAULT '0' NOT NULL,
primary key (id),
CONSTRAINT x_service_name UNIQUE (name),
CONSTRAINT x_service_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_service_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_service_FK_type FOREIGN KEY (type) REFERENCES x_service_def (id)
);
commit;
CREATE TABLE x_policy (
id NUMBER(20) NOT NULL,
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
version NUMBER(20) DEFAULT NULL NULL,
service NUMBER(20) DEFAULT NULL NULL,
name VARCHAR(512) DEFAULT NULL NULL,
policy_type NUMBER(11) DEFAULT '0' NULL,
description VARCHAR(1024) DEFAULT NULL NULL,
resource_signature VARCHAR(128) DEFAULT NULL NULL,
is_enabled NUMBER(1) DEFAULT '0' NOT NULL,
is_audit_enabled NUMBER(1) DEFAULT '0' NOT NULL,
primary key (id),
CONSTRAINT x_policy_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_policy_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_policy_FK_service FOREIGN KEY (service) REFERENCES x_service (id) 
);
commit;
CREATE TABLE x_service_config_def (
id NUMBER(20) NOT NULL,
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
def_id NUMBER(20) NOT NULL,
item_id NUMBER(20) NOT NULL,
name VARCHAR(1024) DEFAULT NULL NULL,
type VARCHAR(1024) DEFAULT NULL NULL,
sub_type VARCHAR(1024) DEFAULT NULL NULL,
is_mandatory NUMBER(1) DEFAULT '0' NOT NULL,
default_value VARCHAR(1024) DEFAULT NULL NULL,
validation_reg_ex VARCHAR(1024) DEFAULT NULL NULL,
validation_message VARCHAR(1024) DEFAULT NULL NULL,
ui_hint VARCHAR(1024) DEFAULT NULL NULL,
label VARCHAR(1024) DEFAULT NULL NULL,
description VARCHAR(1024) DEFAULT NULL NULL,
rb_key_label VARCHAR(1024) DEFAULT NULL NULL,
rb_key_description VARCHAR(1024) DEFAULT NULL NULL,
rb_key_validation_message VARCHAR(1024) DEFAULT NULL NULL,
sort_order NUMBER(10) DEFAULT '0' NULL,
primary key (id),
CONSTRAINT x_service_conf_def_FK_defid FOREIGN KEY (def_id) REFERENCES x_service_def (id),
CONSTRAINT x_service_conf_def_FK_added_by FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_service_conf_def_FK_upd_by FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
commit;
CREATE TABLE x_resource_def (
id NUMBER(20) NOT NULL,
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
def_id NUMBER(20) NOT NULL,
item_id NUMBER(20) NOT NULL,
name VARCHAR(1024) DEFAULT NULL NULL,
type VARCHAR(1024) DEFAULT NULL NULL,
res_level NUMBER(20) DEFAULT NULL NULL,
parent NUMBER(20) DEFAULT NULL NULL,
mandatory NUMBER(1) DEFAULT '0' NOT NULL,
look_up_supported NUMBER(1) DEFAULT '0' NOT NULL,
recursive_supported NUMBER(1) DEFAULT '0' NOT NULL,
excludes_supported NUMBER(1) DEFAULT '0' NOT NULL,
matcher VARCHAR(1024) DEFAULT NULL NULL,
matcher_options varchar(1024) DEFAULT NULL NULL,
validation_reg_ex VARCHAR(1024) DEFAULT NULL NULL,
validation_message VARCHAR(1024) DEFAULT NULL NULL,
ui_hint VARCHAR(1024) DEFAULT NULL NULL,
label VARCHAR(1024) DEFAULT NULL NULL,
description VARCHAR(1024) DEFAULT NULL NULL,
rb_key_label VARCHAR(1024) DEFAULT NULL NULL,
rb_key_description VARCHAR(1024) DEFAULT NULL NULL,
rb_key_validation_message VARCHAR(1024) DEFAULT NULL NULL,
sort_order NUMBER(10) DEFAULT '0' NULL,
primary key (id),
CONSTRAINT x_resource_def_FK_parent FOREIGN KEY (parent) REFERENCES x_resource_def (id),
CONSTRAINT x_resource_def_FK_defid FOREIGN KEY (def_id) REFERENCES x_service_def (id),
CONSTRAINT x_resource_def_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_resource_def_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
commit;
CREATE TABLE x_access_type_def (
id NUMBER(20) NOT NULL,
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
def_id NUMBER(20) NOT NULL,
item_id NUMBER(20) NOT NULL,
name VARCHAR(1024) DEFAULT NULL NULL,
label VARCHAR(1024) DEFAULT NULL NULL,
rb_key_label VARCHAR(1024) DEFAULT NULL NULL,
sort_order NUMBER(10) DEFAULT '0' NULL,
primary key (id),
CONSTRAINT x_access_type_def_FK_defid FOREIGN KEY (def_id) REFERENCES x_service_def (id),
CONSTRAINT x_access_type_def_FK_added_by FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_access_type_def_FK_upd_by FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
commit;
CREATE TABLE x_access_type_def_grants (
id NUMBER(20) NOT NULL,
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
atd_id NUMBER(20) NOT NULL,
implied_grant VARCHAR(1024) DEFAULT NULL NULL,
primary key (id),
CONSTRAINT x_atd_grants_FK_atdid FOREIGN KEY (atd_id) REFERENCES x_access_type_def (id),
CONSTRAINT x_atd_grants_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_atd_grants_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
commit;
CREATE TABLE x_policy_condition_def (
id NUMBER(20) NOT NULL,
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
def_id NUMBER(20) NOT NULL,
item_id NUMBER(20) NOT NULL,
name VARCHAR(1024) DEFAULT NULL NULL,
evaluator VARCHAR(1024) DEFAULT NULL NULL,
evaluator_options VARCHAR(1024) DEFAULT NULL NULL,
validation_reg_ex VARCHAR(1024) DEFAULT NULL NULL,
validation_message VARCHAR(1024) DEFAULT NULL NULL,
ui_hint VARCHAR(1024) DEFAULT NULL NULL,
label VARCHAR(1024) DEFAULT NULL NULL,
description VARCHAR(1024) DEFAULT NULL NULL,
rb_key_label VARCHAR(1024) DEFAULT NULL NULL,
rb_key_description VARCHAR(1024) DEFAULT NULL NULL,
rb_key_validation_message VARCHAR(1024) DEFAULT NULL NULL,
sort_order NUMBER(10) DEFAULT '0' NULL,
primary key (id),
CONSTRAINT x_policy_cond_def_FK_defid FOREIGN KEY (def_id) REFERENCES x_service_def (id),
CONSTRAINT x_policy_cond_def_FK_added_by FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_policy_cond_def_FK_upd_by FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
commit;
CREATE TABLE x_context_enricher_def(
id NUMBER(20) NOT NULL,
guid varchar(1024) DEFAULT NULL NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL,
upd_by_id NUMBER(20) DEFAULT NULL,
def_id NUMBER(20) NOT NULL,
item_id NUMBER(20) NOT NULL,
name varchar(1024) DEFAULT NULL NULL,
enricher varchar(1024) DEFAULT NULL NULL,
enricher_options varchar(1024) DEFAULT NULL NULL,
sort_order NUMBER(10) DEFAULT '0' NULL,
primary key (id),
CONSTRAINT x_cont_enr_def_FK_defid FOREIGN KEY (def_id) REFERENCES x_service_def (id),
CONSTRAINT x_cont_enr_def_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_cont_enr_def_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
commit;
CREATE TABLE x_enum_def (
id NUMBER(20) NOT NULL,
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
def_id NUMBER(20) NOT NULL,
item_id NUMBER(20) NOT NULL,
name VARCHAR(1024) DEFAULT NULL NULL,
default_index NUMBER(20) DEFAULT NULL NULL,
primary key (id),
CONSTRAINT x_enum_def_FK_def_id FOREIGN KEY (def_id) REFERENCES x_service_def (id),
CONSTRAINT x_enum_def_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_enum_def_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
commit;
CREATE TABLE x_enum_element_def (
id NUMBER(20) NOT NULL,
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
enum_def_id NUMBER(20) NOT NULL,
item_id NUMBER(20) NOT NULL,
name VARCHAR(1024) DEFAULT NULL NULL,
label VARCHAR(1024) DEFAULT NULL NULL,
rb_key_label VARCHAR(1024) DEFAULT NULL NULL,
sort_order NUMBER(10) DEFAULT '0' NULL,
primary key (id),
CONSTRAINT x_enum_element_def_FK_defid FOREIGN KEY (enum_def_id) REFERENCES x_enum_def (id),
CONSTRAINT x_enum_element_def_FK_added_by FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_enum_element_def_FK_upd_by FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
commit;
CREATE TABLE x_service_config_map (
id NUMBER(20) NOT NULL,
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
service NUMBER(20) NOT NULL,
config_key VARCHAR(1024) DEFAULT NULL NULL,
config_value VARCHAR(4000) DEFAULT NULL NULL,
primary key (id),
CONSTRAINT x_service_conf_map_FK_service FOREIGN KEY (service) REFERENCES x_service (id),
CONSTRAINT x_service_conf_map_FK_added_by FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_service_conf_map_FK_upd_by FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
commit;
CREATE TABLE x_policy_resource (
id NUMBER(20) NOT NULL,
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
policy_id NUMBER(20) NOT NULL,
res_def_id NUMBER(20) NOT NULL,
is_excludes NUMBER(1) DEFAULT '0' NOT NULL,
is_recursive NUMBER(1) DEFAULT '0' NOT NULL,
primary key (id),
CONSTRAINT x_policy_res_FK_policy_id FOREIGN KEY (policy_id) REFERENCES x_policy (id),
CONSTRAINT x_policy_res_FK_res_def_id FOREIGN KEY (res_def_id) REFERENCES x_resource_def (id),
CONSTRAINT x_policy_res_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_policy_res_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
commit;
CREATE TABLE x_policy_resource_map (
id NUMBER(20) NOT NULL,
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
resource_id NUMBER(20) NOT NULL,
value VARCHAR(1024) DEFAULT NULL NULL,
sort_order NUMBER(10) DEFAULT '0' NULL,
primary key (id),
CONSTRAINT x_policy_res_map_FK_res_id FOREIGN KEY (resource_id) REFERENCES x_policy_resource (id),
CONSTRAINT x_policy_res_map_FK_added_by FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_policy_res_map_FK_upd_by FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
commit;
CREATE TABLE x_policy_item (
id NUMBER(20) NOT NULL,
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
policy_id NUMBER(20) NOT NULL,
delegate_admin NUMBER(1) DEFAULT '0' NOT NULL,
sort_order NUMBER(10) DEFAULT '0' NULL,
primary key (id),
CONSTRAINT x_policy_item_FK_policy_id FOREIGN KEY (policy_id) REFERENCES x_policy (id),
CONSTRAINT x_policy_item_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_policy_item_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id) 
);
commit;
CREATE TABLE x_policy_item_access (
id NUMBER(20) NOT NULL,
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
policy_item_id NUMBER(20) NOT NULL,
type NUMBER(20) NOT NULL,
is_allowed NUMBER(3) DEFAULT '0' NOT NULL,
sort_order NUMBER(10) DEFAULT '0' NULL,
primary key (id),
CONSTRAINT x_plc_item_access_FK_pi_id FOREIGN KEY (policy_item_id) REFERENCES x_policy_item (id),
CONSTRAINT x_plc_item_access_FK_atd_id FOREIGN KEY (type) REFERENCES x_access_type_def (id),
CONSTRAINT x_plc_item_access_FK_added_by FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_plc_item_access_FK_upd_by FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
commit;
CREATE TABLE x_policy_item_condition (
id NUMBER(20) NOT NULL,
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
policy_item_id NUMBER(20) NOT NULL,
type NUMBER(20) NOT NULL,
value VARCHAR(1024) DEFAULT NULL NULL,
sort_order NUMBER(10) DEFAULT '0' NULL,
primary key (id),
CONSTRAINT x_plc_item_cond_FK_pi_id FOREIGN KEY (policy_item_id) REFERENCES x_policy_item (id),
CONSTRAINT x_plc_item_cond_FK_pcd_id FOREIGN KEY (type) REFERENCES x_policy_condition_def (id),
CONSTRAINT x_plc_item_cond_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_plc_item_cond_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
commit;
CREATE TABLE x_policy_item_user_perm (
id NUMBER(20) NOT NULL,
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
policy_item_id NUMBER(20) NOT NULL,
user_id NUMBER(20) DEFAULT NULL NULL,
sort_order NUMBER(10) DEFAULT '0' NULL,
primary key (id),
CONSTRAINT x_plc_itm_usr_perm_FK_pi_id FOREIGN KEY (policy_item_id) REFERENCES x_policy_item (id),
CONSTRAINT x_plc_itm_usr_perm_FK_user_id FOREIGN KEY (user_id) REFERENCES x_user (id),
CONSTRAINT x_plc_itm_usr_perm_FK_added_by FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_plc_itm_usr_perm_FK_upd_by FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
commit;
CREATE TABLE x_policy_item_group_perm (
id NUMBER(20) NOT NULL,
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
policy_item_id NUMBER(20) NOT NULL,
group_id NUMBER(20) DEFAULT NULL NULL,
sort_order NUMBER(10) DEFAULT '0' NULL,
primary key (id),
CONSTRAINT x_plc_itm_grp_perm_FK_pi_id FOREIGN KEY (policy_item_id) REFERENCES x_policy_item (id),
CONSTRAINT x_plc_itm_grp_perm_FK_group_id FOREIGN KEY (group_id) REFERENCES x_group (id),
CONSTRAINT x_plc_itm_grp_perm_FK_added_by FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_plc_itm_grp_perm_FK_upd_by FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
commit;
CREATE TABLE x_data_hist (
id NUMBER(20) NOT NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
obj_guid VARCHAR(1024) NOT NULL,
obj_class_type NUMBER(11) NOT NULL,
obj_id NUMBER(20) NOT NULL,
obj_name VARCHAR(1024) NOT NULL,
version NUMBER(20) DEFAULT NULL NULL,
action VARCHAR(512)NOT NULL,
from_time DATE NOT NULL,
to_time DATE DEFAULT NULL NULL,
content CLOB NOT NULL,
primary key (id)
);
commit;
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
commit;
