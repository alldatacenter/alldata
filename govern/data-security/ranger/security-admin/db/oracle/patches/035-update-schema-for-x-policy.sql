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

call spdropsequence('X_POLICY_REF_RESOURCE_SEQ');
call spdropsequence('X_POLICY_REF_ACCESS_TYPE_SEQ');
call spdropsequence('X_POLICY_REF_CONDITION_SEQ');
call spdropsequence('X_POLICY_REF_DATAMASK_TYPE_SEQ');
call spdropsequence('X_POLICY_REF_USER_SEQ');
call spdropsequence('X_POLICY_REF_GROUP_SEQ');

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

call spdroptable('x_policy_ref_group');
call spdroptable('x_policy_ref_user');
call spdroptable('x_policy_ref_datamask_type');
call spdroptable('x_policy_ref_condition');
call spdroptable('x_policy_ref_access_type');
call spdroptable('x_policy_ref_resource');

CREATE SEQUENCE X_POLICY_REF_RESOURCE_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE X_POLICY_REF_ACCESS_TYPE_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE X_POLICY_REF_CONDITION_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE X_POLICY_REF_DATAMASK_TYPE_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE X_POLICY_REF_USER_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE X_POLICY_REF_GROUP_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
commit;
CREATE TABLE x_policy_ref_resource (
id NUMBER(20) NOT NULL,
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
policy_id NUMBER(20) NOT NULL,
resource_def_id NUMBER(20) NOT NULL,
resource_name VARCHAR(4000) DEFAULT NULL NULL,
primary key (id),
CONSTRAINT x_p_ref_res_UK_polId_resDefId UNIQUE (policy_id, resource_def_id),
CONSTRAINT x_p_ref_res_FK_policy_id FOREIGN KEY (policy_id) REFERENCES x_policy (id),
CONSTRAINT x_p_ref_res_FK_resource_def_id FOREIGN KEY (resource_def_id) REFERENCES x_resource_def (id),
CONSTRAINT x_p_ref_res_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_p_ref_res_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
commit;
CREATE TABLE x_policy_ref_access_type (
id NUMBER(20) NOT NULL,
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
policy_id NUMBER(20) NOT NULL,
access_def_id NUMBER(20) NOT NULL,
access_type_name VARCHAR(4000) DEFAULT NULL NULL,
primary key (id),
CONSTRAINT x_p_ref_acc_UK_polId_accDefId UNIQUE(policy_id, access_def_id),
CONSTRAINT x_p_ref_acc_FK_policy_id FOREIGN KEY (policy_id) REFERENCES x_policy (id),
CONSTRAINT x_p_ref_acc_FK_acc_def_id FOREIGN KEY (access_def_id) REFERENCES x_access_type_def (id),
CONSTRAINT x_p_ref_acc_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_p_ref_acc_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
commit;
CREATE TABLE x_policy_ref_condition (
id NUMBER(20) NOT NULL,
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
policy_id NUMBER(20) NOT NULL,
condition_def_id NUMBER(20) NOT NULL,
condition_name VARCHAR(4000) DEFAULT NULL NULL,
primary key (id),
CONSTRAINT x_p_ref_cond_UK_polId_cDefId UNIQUE(policy_id, condition_def_id),
CONSTRAINT x_p_ref_cond_FK_policy_id FOREIGN KEY (policy_id) REFERENCES x_policy (id),
CONSTRAINT x_p_ref_cond_FK_cond_def_id FOREIGN KEY (condition_def_id) REFERENCES x_policy_condition_def (id),
CONSTRAINT x_p_ref_cond_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_p_ref_cond_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
commit;
CREATE TABLE x_policy_ref_datamask_type (
id NUMBER(20) NOT NULL,
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
policy_id NUMBER(20) NOT NULL,
datamask_def_id NUMBER(20) NOT NULL,
datamask_type_name VARCHAR(4000) DEFAULT NULL NULL,
primary key (id),
CONSTRAINT x_p_ref_dmsk_UK_polId_dDefId UNIQUE(policy_id, datamask_def_id),
CONSTRAINT x_p_ref_dmsk_FK_policy_id FOREIGN KEY (policy_id) REFERENCES x_policy (id),
CONSTRAINT x_p_ref_dmsk_FK_dmk_def_id FOREIGN KEY (datamask_def_id) REFERENCES x_datamask_type_def (id),
CONSTRAINT x_p_ref_dmsk_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_p_ref_dmsk_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
commit;
CREATE TABLE x_policy_ref_user (
id NUMBER(20) NOT NULL,
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
policy_id NUMBER(20) NOT NULL,
user_id NUMBER(20) NOT NULL,
user_name VARCHAR(4000) DEFAULT NULL NULL,
primary key (id),
CONSTRAINT x_p_ref_usr_UK_polId_userId UNIQUE(policy_id, user_id),
CONSTRAINT x_p_ref_usr_FK_policy_id FOREIGN KEY (policy_id) REFERENCES x_policy (id),
CONSTRAINT x_p_ref_usr_FK_user_id FOREIGN KEY (user_id) REFERENCES x_user (id),
CONSTRAINT x_p_ref_usr_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_p_ref_usr_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
commit;
CREATE TABLE x_policy_ref_group (
id NUMBER(20) NOT NULL,
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
policy_id NUMBER(20) NOT NULL,
group_id NUMBER(20) NOT NULL,
group_name VARCHAR(4000) DEFAULT NULL NULL,
primary key (id),
CONSTRAINT x_p_ref_grp_UK_polId_grpId UNIQUE(policy_id, group_id),
CONSTRAINT x_p_ref_grp_FK_policy_id FOREIGN KEY (policy_id) REFERENCES x_policy (id),
CONSTRAINT x_p_ref_grp_FK_group_id FOREIGN KEY (group_id) REFERENCES x_group (id),
CONSTRAINT x_p_ref_grp_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_p_ref_grp_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
commit;
DECLARE
	v_column_exists number := 0;
BEGIN
Select count(*) into v_column_exists from user_tab_cols where column_name = upper('policy_text') and table_name = upper('x_policy');
	if (v_column_exists = 0) then
		execute immediate 'ALTER TABLE x_policy ADD policy_text CLOB DEFAULT NULL NULL';
		commit;
	end if;
end;/

CREATE OR REPLACE PROCEDURE removeConstraints(ObjName IN varchar2) IS
BEGIN
FOR rec IN(
select owner, constraint_name
from all_constraints
where owner = sys_context('userenv','current_schema')
and table_name = ObjName
and constraint_type = 'R')
LOOP
execute immediate 'ALTER TABLE ' || rec.owner || '.' || ObjName || ' DROP CONSTRAINT ' || rec.constraint_name;
END LOOP;
END;/
/

CALL removeConstraints('X_POLICY_ITEM');
CALL removeConstraints('X_POLICY_ITEM_ACCESS');
CALL removeConstraints('X_POLICY_ITEM_CONDITION');
CALL removeConstraints('X_POLICY_ITEM_DATAMASK');
CALL removeConstraints('X_POLICY_ITEM_GROUP_PERM');
CALL removeConstraints('X_POLICY_RESOURCE');
CALL removeConstraints('X_POLICY_RESOURCE_MAP');
CALL removeConstraints('X_POLICY_ITEM_USER_PERM');
CALL removeConstraints('X_POLICY_ITEM_ROWFILTER');

DECLARE
	v_record_exists number := 0;
	new_atlas_def_name VARCHAR(1024);
	sql_stmt VARCHAR(1024);
BEGIN
select count(*) into v_record_exists from x_db_version_h where version = 'J10013';
	if (v_record_exists = 1) then
		select count(*) into v_record_exists from x_service_def where name like 'atlas.%';
		if (v_record_exists > 0) then
			select name into new_atlas_def_name from x_service_def where name like 'atlas.%';
		end if;
		select count(*) into v_record_exists from x_access_type_def where def_id in(select id from x_service_def where name='tag') and name in('atlas:read','atlas:create','atlas:update','atlas:delete','atlas:all');
		if (v_record_exists > 0) then
			sql_stmt := 'UPDATE x_access_type_def set name=concat(:1,:2) where def_id=100 and name=:3';
			EXECUTE IMMEDIATE sql_stmt USING new_atlas_def_name,':create','atlas:create';
			sql_stmt := 'UPDATE x_access_type_def set name=concat(:1,:2) where def_id=100 and name=:3';
			EXECUTE IMMEDIATE sql_stmt USING new_atlas_def_name,':update','atlas:update';
			sql_stmt := 'UPDATE x_access_type_def set name=concat(:1,:2) where def_id=100 and name=:3';
			EXECUTE IMMEDIATE sql_stmt USING new_atlas_def_name,':delete','atlas:delete';
			sql_stmt := 'UPDATE x_access_type_def set name=concat(:1,:2) where def_id=100 and name=:3';
			EXECUTE IMMEDIATE sql_stmt USING new_atlas_def_name,':read','atlas:read';
			sql_stmt := 'UPDATE x_access_type_def set name=concat(:1,:2) where def_id=100 and name=:3';
			EXECUTE IMMEDIATE sql_stmt USING new_atlas_def_name,':all','atlas:all';
		end if;
		select count(*) into v_record_exists from x_access_type_def_grants where atd_id in (select id from x_access_type_def where def_id in (select id from x_service_def where name='tag') and name like 'atlas%') and implied_grant in ('atlas:read','atlas:create','atlas:update','atlas:delete','atlas:all');
		if (v_record_exists > 0) then
			sql_stmt := 'UPDATE x_access_type_def_grants set implied_grant=concat(:1,:2) where implied_grant=:3';
			EXECUTE IMMEDIATE sql_stmt USING new_atlas_def_name,':create','atlas:create';
			sql_stmt := 'UPDATE x_access_type_def_grants set implied_grant=concat(:1,:2) where implied_grant=:3';
			EXECUTE IMMEDIATE sql_stmt USING new_atlas_def_name,':update','atlas:update';
			sql_stmt := 'UPDATE x_access_type_def_grants set implied_grant=concat(:1,:2) where implied_grant=:3';
			EXECUTE IMMEDIATE sql_stmt USING new_atlas_def_name,':delete','atlas:delete';
			sql_stmt := 'UPDATE x_access_type_def_grants set implied_grant=concat(:1,:2) where implied_grant=:3';
			EXECUTE IMMEDIATE sql_stmt USING new_atlas_def_name,':read','atlas:read';
			sql_stmt := 'UPDATE x_access_type_def_grants set implied_grant=concat(:1,:2) where implied_grant=:3';
			EXECUTE IMMEDIATE sql_stmt USING new_atlas_def_name,':all','atlas:all';
		end if;
	end if;
	commit;
end;/
