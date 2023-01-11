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
call spdropsequence('X_TAG_DEF_SEQ');
call spdropsequence('X_TAG_SEQ');
call spdropsequence('X_SERVICE_RESOURCE_SEQ');
call spdropsequence('X_SERVICE_RESOURCE_ELEMENT_SEQ');
call spdropsequence('X_TAG_ATTR_DEF_SEQ');
call spdropsequence('X_TAG_ATTR_SEQ');
call spdropsequence('X_TAG_RESOURCE_MAP_SEQ');
call spdropsequence('X_SERVICE_RES_EL_VAL_SEQ');

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

call spdroptable('x_service_resource_element_val');
call spdroptable('x_tag_resource_map');
call spdroptable('x_tag_attr');
call spdroptable('x_tag_attr_def');
call spdroptable('x_service_resource_element');
call spdroptable('x_service_resource');
call spdroptable('x_tag');
call spdroptable('x_tag_def');

CREATE SEQUENCE X_TAG_DEF_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE TABLE x_tag_def (
id NUMBER(20) NOT NULL,
guid VARCHAR(64) NOT NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
version NUMBER(20) DEFAULT NULL NULL,
name VARCHAR(255) NOT NULL,
source VARCHAR(128) DEFAULT NULL NULL,
is_enabled NUMBER(1) DEFAULT '0' NOT NULL,
PRIMARY KEY (id),
CONSTRAINT x_tag_def_UK_guid UNIQUE (guid),
CONSTRAINT x_tag_def_UK_name UNIQUE (name),
CONSTRAINT x_tag_def_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_tag_def_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
CREATE INDEX x_tag_def_IDX_added_by_id ON x_tag_def(added_by_id);
CREATE INDEX x_tag_def_IDX_upd_by_id ON x_tag_def(upd_by_id);
commit;
CREATE SEQUENCE X_TAG_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE TABLE x_tag(
id NUMBER(20) NOT NULL,
guid VARCHAR(64) NOT NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
version NUMBER(20) DEFAULT NULL NULL,
type NUMBER(20) NOT NULL,
primary key (id),
CONSTRAINT x_tag_UK_guid UNIQUE (guid),
CONSTRAINT x_tag_FK_type FOREIGN KEY (type) REFERENCES x_tag_def (id),
CONSTRAINT x_tag_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_tag_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
CREATE INDEX x_tag_IDX_type ON x_tag(type);
CREATE INDEX x_tag_IDX_added_by_id ON x_tag(added_by_id);
CREATE INDEX x_tag_IDX_upd_by_id ON x_tag(upd_by_id);
commit;
CREATE SEQUENCE X_SERVICE_RESOURCE_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE TABLE x_service_resource(
id NUMBER(20) NOT NULL,
guid VARCHAR(64) NOT NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
version NUMBER(20) DEFAULT NULL NULL,
service_id NUMBER(20) NOT NULL,
resource_signature VARCHAR(128) DEFAULT NULL NULL,
is_enabled NUMBER(1) DEFAULT '1' NOT NULL,
primary key (id),
CONSTRAINT x_service_res_UK_guid UNIQUE (guid),
CONSTRAINT x_service_res_FK_service_id FOREIGN KEY (service_id) REFERENCES x_service (id),
CONSTRAINT x_service_res_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_service_res_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
CREATE INDEX x_service_res_IDX_added_by_id ON x_service_resource(added_by_id);
CREATE INDEX x_service_res_IDX_upd_by_id ON x_service_resource(upd_by_id);
commit;
CREATE SEQUENCE X_SERVICE_RESOURCE_ELEMENT_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE TABLE x_service_resource_element(
id NUMBER(20) NOT NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
res_id NUMBER(20) NOT NULL,
res_def_id NUMBER(20) NOT NULL,
is_excludes NUMBER(1) DEFAULT '0' NOT NULL,
is_recursive NUMBER(1) DEFAULT '0' NOT NULL,
primary key (id),
CONSTRAINT x_srvc_res_el_FK_res_def_id FOREIGN KEY (res_def_id) REFERENCES x_resource_def (id),
CONSTRAINT x_srvc_res_el_FK_res_id FOREIGN KEY (res_id) REFERENCES x_service_resource (id),
CONSTRAINT x_srvc_res_el_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_srvc_res_el_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
CREATE INDEX x_srvc_res_el_IDX_added_by_id ON x_service_resource_element(added_by_id);
CREATE INDEX x_srvc_res_el_IDX_upd_by_id ON x_service_resource_element(upd_by_id);
commit;
CREATE SEQUENCE X_TAG_ATTR_DEF_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE TABLE x_tag_attr_def(
id NUMBER(20) NOT NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
tag_def_id NUMBER(20) NOT NULL,
name VARCHAR(255) NOT NULL,
type VARCHAR(50) NOT NULL,
primary key (id),
CONSTRAINT x_tag_attr_def_FK_tag_def_id FOREIGN KEY (tag_def_id) REFERENCES x_tag_def (id),
CONSTRAINT x_tag_attr_def_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_tag_attr_def_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
CREATE INDEX x_tag_attr_def_IDX_tag_def_id ON x_tag_attr_def(tag_def_id);
CREATE INDEX x_tag_attr_def_IDX_added_by_id ON x_tag_attr_def(added_by_id);
CREATE INDEX x_tag_attr_def_IDX_upd_by_id ON x_tag_attr_def(upd_by_id);
commit;
CREATE SEQUENCE X_TAG_ATTR_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE TABLE x_tag_attr(
id NUMBER(20) NOT NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
tag_id NUMBER(20) NOT NULL,
name VARCHAR(255) NOT NULL,
value VARCHAR(512) DEFAULT NULL NULL,
primary key (id),
CONSTRAINT x_tag_attr_FK_tag_id FOREIGN KEY (tag_id) REFERENCES x_tag (id),
CONSTRAINT x_tag_attr_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_tag_attr_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
CREATE INDEX x_tag_attr_IDX_tag_id ON x_tag_attr(tag_id);
CREATE INDEX x_tag_attr_IDX_added_by_id ON x_tag_attr(added_by_id);
CREATE INDEX x_tag_attr_IDX_upd_by_id ON x_tag_attr(upd_by_id);
commit;
CREATE SEQUENCE X_TAG_RESOURCE_MAP_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE TABLE x_tag_resource_map(
id NUMBER(20) NOT NULL,
guid VARCHAR(64) NOT NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
tag_id NUMBER(20) NOT NULL,
res_id NUMBER(20) NOT NULL,
primary key (id),
CONSTRAINT x_tag_res_map_UK_guid UNIQUE (guid),
CONSTRAINT x_tag_res_map_FK_tag_id FOREIGN KEY (tag_id) REFERENCES x_tag (id),
CONSTRAINT x_tag_res_map_FK_res_id FOREIGN KEY (res_id) REFERENCES x_service_resource (id),
CONSTRAINT x_tag_res_map_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_tag_res_map_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
CREATE INDEX x_tag_res_map_IDX_tag_id ON x_tag_resource_map(tag_id);
CREATE INDEX x_tag_res_map_IDX_res_id ON x_tag_resource_map(res_id);
CREATE INDEX x_tag_res_map_IDX_added_by_id ON x_tag_resource_map(added_by_id);
CREATE INDEX x_tag_res_map_IDX_upd_by_id ON x_tag_resource_map(upd_by_id);
commit;
CREATE SEQUENCE X_SERVICE_RES_EL_VAL_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE TABLE x_service_resource_element_val(
id NUMBER(20) NOT NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
res_element_id NUMBER(20) NOT NULL,
value VARCHAR(1024) NOT NULL,
sort_order NUMBER(10) DEFAULT '0' NULL,
primary key (id),
CONSTRAINT x_srvc_res_el_val_FK_res_el_id FOREIGN KEY (res_element_id) REFERENCES x_service_resource_element (id),
CONSTRAINT x_srvc_res_el_val_FK_add_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_srvc_res_el_val_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
CREATE INDEX x_srvc_res_el_val_IDX_resel_id ON x_service_resource_element_val(res_element_id);
CREATE INDEX x_srvc_res_el_val_IDX_addby_id ON x_service_resource_element_val(added_by_id);
CREATE INDEX x_srvc_res_el_val_IDX_updby_id ON x_service_resource_element_val(upd_by_id);

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
END; /
/
INSERT INTO x_modules_master VALUES(X_MODULES_MASTER_SEQ.NEXTVAL,SYSDATE,SYSDATE,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),'Tag Based Policies','');

DECLARE
	v_column_exists number := 0;
BEGIN
  Select count(*) into v_column_exists
    from user_tab_cols
    where column_name = upper('def_options')
      and table_name = upper('x_service_def');

  if (v_column_exists = 0) then
      execute immediate 'ALTER TABLE x_service_def ADD def_options VARCHAR(1024) DEFAULT NULL NULL';
      commit;
  end if;
end;/

DECLARE
	v_column1_exists number := 0;
	v_column2_exists number := 0;
	v_column3_exists number := 0;
BEGIN
  Select count(*) into v_column1_exists
    from user_tab_cols
    where column_name = upper('item_type')
      and table_name = upper('x_policy_item');

  Select count(*) into v_column2_exists
    from user_tab_cols
    where column_name = upper('is_enabled')
      and table_name = upper('x_policy_item');

  Select count(*) into v_column3_exists
    from user_tab_cols
    where column_name = upper('comments')
      and table_name = upper('x_policy_item');

  if (v_column1_exists = 0) AND (v_column2_exists = 0) AND (v_column3_exists = 0) then
      execute immediate 'ALTER TABLE x_policy_item ADD (item_type NUMBER(10) DEFAULT 0 NOT NULL,is_enabled NUMBER(1) DEFAULT 1 NOT NULL,comments VARCHAR(255) DEFAULT NULL NULL)';
      commit;
  end if;

end;/

DECLARE
	v_column1_exists number := 0;
	v_column2_exists number := 0;
	v_column3_exists number := 0;
BEGIN
  Select count(*) into v_column1_exists
    from user_tab_cols
    where column_name = upper('tag_service')
      and table_name = upper('x_service');

  Select count(*) into v_column2_exists
    from user_tab_cols
    where column_name = upper('tag_version')
      and table_name = upper('x_service');

  Select count(*) into v_column3_exists
    from user_tab_cols
    where column_name = upper('tag_update_time')
      and table_name = upper('x_service');

  if (v_column1_exists = 0) AND (v_column2_exists = 0) AND (v_column3_exists = 0) then
      execute immediate 'ALTER TABLE x_service ADD (tag_service NUMBER(20) DEFAULT NULL NULL,tag_version NUMBER(20) DEFAULT 0 NOT NULL,tag_update_time DATE DEFAULT NULL NULL) ADD CONSTRAINT x_service_FK_tag_service FOREIGN KEY (tag_service) REFERENCES x_service(id)';
      commit;
  end if;
end;/
commit;
