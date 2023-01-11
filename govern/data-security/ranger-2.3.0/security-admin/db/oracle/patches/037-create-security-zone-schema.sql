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

DECLARE
        v_count number:=0;
BEGIN   
        select count(*) into v_count from user_tab_cols where table_name='X_POLICY_EXPORT_AUDIT' and column_name='ZONE_NAME';
        if (v_count = 0) then 
                execute immediate 'ALTER TABLE x_policy_export_audit ADD zone_name varchar(255) DEFAULT NULL NULL';
        end if; 
        commit; 
END;/

DECLARE
	v_column_exists number := 0;
BEGIN
Select count(*) into v_column_exists from user_tab_cols where column_name = upper('zone_id') and table_name = upper('x_policy');
	if (v_column_exists > 0) then
		execute immediate 'ALTER TABLE x_policy DROP CONSTRAINT x_policy_FK_zone_id';
		execute immediate 'ALTER TABLE x_policy DROP COLUMN zone_id';
		commit;
	end if;
end;/

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

call spdropsequence('X_SEC_ZONE_REF_GROUP_SEQ');
call spdropsequence('X_SEC_ZONE_REF_USER_SEQ');
call spdropsequence('X_SEC_ZONE_REF_RESOURCE_SEQ');
call spdropsequence('X_SEC_ZONE_REF_SERVICE_SEQ');
call spdropsequence('X_SEC_ZONE_REF_TAG_SRVC_SEQ');
call spdropsequence('X_RANGER_GLOBAL_STATE_SEQ');
call spdropsequence('X_SECURITY_ZONE_SEQ');

CREATE SEQUENCE X_SECURITY_ZONE_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE X_RANGER_GLOBAL_STATE_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE X_SEC_ZONE_REF_SERVICE_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE X_SEC_ZONE_REF_TAG_SRVC_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE X_SEC_ZONE_REF_RESOURCE_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE X_SEC_ZONE_REF_USER_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE X_SEC_ZONE_REF_GROUP_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

call spdroptable('x_security_zone_ref_resource');
call spdroptable('x_security_zone_ref_group');
call spdroptable('x_security_zone_ref_user');
call spdroptable('x_security_zone_ref_service');
call spdroptable('x_security_zone_ref_tag_srvc');
call spdroptable('x_ranger_global_state');
call spdroptable('x_security_zone');

commit;
CREATE TABLE x_security_zone(
id NUMBER(20) NOT NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
version NUMBER(20)  DEFAULT NULL NULL,
name varchar(255) NOT NULL,
jsonData CLOB DEFAULT NULL NULL,
description VARCHAR(1024) DEFAULT NULL NULL,
primary key (id),
CONSTRAINT x_security_zone_UK_name UNIQUE(name),
CONSTRAINT x_security_zone_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_security_zone_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
commit;

CREATE TABLE x_ranger_global_state(
id NUMBER(20) NOT NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
version NUMBER(20)  DEFAULT NULL NULL,
state_name varchar(255) NOT NULL,
app_data varchar(255) DEFAULT NULL NULL,
primary key (id),
CONSTRAINT x_rngr_glbl_state_UK_statename UNIQUE(state_name),
CONSTRAINT x_rngr_glbl_state_FK_addedbyid FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_rngr_glbl_state_FK_updbyid FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
commit;

CREATE TABLE x_security_zone_ref_service (
id NUMBER(20) NOT NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
zone_id NUMBER(20)  DEFAULT NULL NULL,
service_id NUMBER(20)  DEFAULT NULL NULL,
service_name VARCHAR(255) DEFAULT NULL NULL,
primary key (id),
CONSTRAINT x_sz_ref_ser_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_sz_ref_ser_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_sz_ref_ser_FK_zone_id FOREIGN KEY (zone_id) REFERENCES x_security_zone (id),
CONSTRAINT x_sz_ref_ser_FK_service_id FOREIGN KEY (service_id) REFERENCES x_service (id),
CONSTRAINT x_sz_ref_ser_FK_service_name FOREIGN KEY (service_name) REFERENCES x_service (name)
);
commit;

CREATE TABLE x_security_zone_ref_tag_srvc (
id NUMBER(20) NOT NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
zone_id NUMBER(20)  DEFAULT NULL NULL,
tag_srvc_id NUMBER(20)  DEFAULT NULL NULL,
tag_srvc_name VARCHAR(255) DEFAULT NULL NULL,
primary key (id),
CONSTRAINT x_sz_refTagTser_FK_aded_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_sz_refTagTser_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_sz_refTagTser_FK_zone_id FOREIGN KEY (zone_id) REFERENCES x_security_zone (id),
CONSTRAINT x_sz_refTagTser_FK_srvc_id FOREIGN KEY (tag_srvc_id) REFERENCES x_service (id),
CONSTRAINT x_sz_refTagTser_FK_srvc_name FOREIGN KEY (tag_srvc_name) REFERENCES x_service (name)
);
commit;

CREATE TABLE x_security_zone_ref_resource (
id NUMBER(20) NOT NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
zone_id NUMBER(20)  DEFAULT NULL NULL,
resource_def_id NUMBER(20)  DEFAULT NULL NULL,
resource_name VARCHAR(255) DEFAULT NULL NULL,
primary key (id),
CONSTRAINT x_sz_ref_res_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_sz_ref_res_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_sz_ref_res_FK_zone_id FOREIGN KEY (zone_id) REFERENCES x_security_zone (id),
CONSTRAINT x_sz_ref_res_FK_res_def_id FOREIGN KEY (resource_def_id) REFERENCES x_resource_def (id)
);
commit;
CREATE TABLE x_security_zone_ref_user (
id NUMBER(20) NOT NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
zone_id NUMBER(20)  DEFAULT NULL NULL,
user_id NUMBER(20)  DEFAULT NULL NULL,
user_name varchar(255) DEFAULT NULL NULL,
user_type NUMBER(3)  DEFAULT NULL NULL,
primary key (id),
CONSTRAINT x_sz_ref_user_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_sz_ref_user_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_sz_ref_user_FK_zone_id FOREIGN KEY (zone_id) REFERENCES x_security_zone (id),
CONSTRAINT x_sz_ref_user_FK_user_id FOREIGN KEY (user_id) REFERENCES x_user (id),
CONSTRAINT x_sz_ref_user_FK_user_name FOREIGN KEY (user_name) REFERENCES x_user (user_name)
);
commit;
CREATE TABLE x_security_zone_ref_group (
id NUMBER(20) NOT NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
zone_id NUMBER(20)  DEFAULT NULL NULL,
group_id NUMBER(20)  DEFAULT NULL NULL,
group_name varchar(255) DEFAULT NULL NULL,
group_type NUMBER(3)  DEFAULT NULL NULL,
primary key (id),
CONSTRAINT x_sz_ref_group_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_sz_ref_group_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_sz_ref_group_FK_zone_id FOREIGN KEY (zone_id) REFERENCES x_security_zone (id),
CONSTRAINT x_sz_ref_group_FK_group_id FOREIGN KEY (group_id) REFERENCES x_group (id)
);
commit;

CREATE OR REPLACE FUNCTION getModulesIdByName(inputval IN VARCHAR2)
RETURN NUMBER is
BEGIN
Declare
myid Number := 0;
begin
   SELECT id into myid FROM x_modules_master
   WHERE MODULE = inputval;
   RETURN myid;
end;
END;/

CREATE OR REPLACE FUNCTION getXportalUIdByLoginId(input_val IN VARCHAR2)
RETURN NUMBER iS
BEGIN
DECLARE
myid Number := 0;
begin
    SELECT x_portal_user.id into myid FROM x_portal_user
    WHERE x_portal_user.login_id=input_val;
    RETURN myid;
end;
END;/

DECLARE
	v_column_exists number := 0;
BEGIN
Select count(*) into v_column_exists from x_security_zone where id = 1 and name = ' ';
	if (v_column_exists = 0) then
		INSERT INTO x_security_zone(id, create_time, update_time, added_by_id, upd_by_id, version, name, jsonData, description) VALUES (X_SECURITY_ZONE_SEQ.NEXTVAL, sys_extract_utc(systimestamp), sys_extract_utc(systimestamp), getXportalUIdByLoginId('admin'), getXportalUIdByLoginId('admin'), 1, ' ', '','Unzoned zone');
		commit;
	end if;
end;/

DECLARE
	v_column_exists number := 0;
BEGIN
Select count(*) into v_column_exists from user_tab_cols where column_name = upper('zone_id') and table_name = upper('x_policy');
	if (v_column_exists = 0) then
		execute immediate 'ALTER TABLE x_policy ADD (zone_id NUMBER(20) DEFAULT 1 NOT NULL) ADD CONSTRAINT x_policy_FK_zone_id FOREIGN KEY (zone_id) REFERENCES x_security_zone (id)';
		commit;
	end if;
end;/

DECLARE
        v_count number:=0;
BEGIN   
        select count(*) into v_count from x_modules_master where module='Security Zone';
        if (v_count = 0) then 
			INSERT INTO x_modules_master VALUES(X_MODULES_MASTER_SEQ.NEXTVAL,sys_extract_utc(systimestamp),sys_extract_utc(systimestamp),getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),'Security Zone','');        
		end if;
		v_count:=0;
		select count(*) into v_count from x_user_module_perm where user_id=getXportalUIdByLoginId('admin') and module_id=getModulesIdByName('Security Zone');
        if (v_count = 0) then
			INSERT INTO x_user_module_perm (id,user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (X_USER_MODULE_PERM_SEQ.nextval,getXportalUIdByLoginId('admin'),getModulesIdByName('Security Zone'),sys_extract_utc(systimestamp),sys_extract_utc(systimestamp),getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),1);
        end if;
        v_count:=0;
        select count(*) into v_count from x_user_module_perm where user_id=getXportalUIdByLoginId('rangerusersync') and module_id=getModulesIdByName('Security Zone');
        if (v_count = 0) then
			INSERT INTO x_user_module_perm (id,user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (X_USER_MODULE_PERM_SEQ.nextval,getXportalUIdByLoginId('rangerusersync'),getModulesIdByName('Security Zone'),sys_extract_utc(systimestamp),sys_extract_utc(systimestamp),getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),1);
        end if;
        v_count:=0;
        select count(*) into v_count from x_user_module_perm where user_id=getXportalUIdByLoginId('rangertagsync') and module_id=getModulesIdByName('Security Zone');
        if (v_count = 0) then
			INSERT INTO x_user_module_perm (id,user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (X_USER_MODULE_PERM_SEQ.nextval,getXportalUIdByLoginId('rangertagsync'),getModulesIdByName('Security Zone'),sys_extract_utc(systimestamp),sys_extract_utc(systimestamp),getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),1);
        end if;
        commit;
END;/

