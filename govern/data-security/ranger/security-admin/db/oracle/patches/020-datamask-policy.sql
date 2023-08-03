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

call spdropsequence('X_DATAMASK_TYPE_DEF_SEQ');
call spdropsequence('X_POLICY_ITEM_DATAMASK_SEQ');
call spdropsequence('X_POLICY_ITEM_ROWFILTER_SEQ');

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

call spdroptable('x_policy_item_rowfilter');
call spdroptable('x_policy_item_datamask');
call spdroptable('x_datamask_type_def');

CREATE SEQUENCE X_DATAMASK_TYPE_DEF_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE TABLE x_datamask_type_def (
id NUMBER(20) NOT NULL,
guid VARCHAR(64) DEFAULT NULL NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
def_id NUMBER(20) NOT NULL,
item_id NUMBER(20) NOT NULL,
name VARCHAR(1024) NOT NULL,
label VARCHAR(1024) NOT NULL,
description VARCHAR(1024) DEFAULT NULL NULL,
transformer VARCHAR(1024) DEFAULT NULL NULL,
datamask_options VARCHAR(1024) DEFAULT NULL NULL,
rb_key_label VARCHAR(1024) DEFAULT NULL NULL,
rb_key_description VARCHAR(1024) DEFAULT NULL NULL,
sort_order NUMBER(10) DEFAULT '0' NULL,
primary key (id),
CONSTRAINT x_dm_type_def_FK_def_id FOREIGN KEY (def_id) REFERENCES x_service_def(id),
CONSTRAINT x_dm_type_def_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_dm_type_def_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user(id)
);
CREATE INDEX x_dm_type_def_IDX_def_id ON x_datamask_type_def(def_id);
commit;
CREATE SEQUENCE X_POLICY_ITEM_DATAMASK_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE TABLE x_policy_item_datamask(
id NUMBER(20) NOT NULL,
guid VARCHAR(64) DEFAULT NULL NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
policy_item_id NUMBER(20) NOT NULL,
type NUMBER(20) NOT NULL,
condition_expr VARCHAR(1024) DEFAULT NULL NULL,
value_expr VARCHAR(1024) DEFAULT NULL NULL,
primary key (id),
CONSTRAINT x_plc_item_dm_FK_plc_item_id FOREIGN KEY (policy_item_id) REFERENCES x_policy_item(id),
CONSTRAINT x_plc_item_dm_FK_type FOREIGN KEY (type) REFERENCES x_datamask_type_def(id),
CONSTRAINT x_plc_item_dm_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user(id),
CONSTRAINT x_plc_item_dm_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user(id)
);
CREATE INDEX x_plc_item_dm_IDX_plc_item_id ON x_policy_item_datamask(policy_item_id);
commit;
CREATE SEQUENCE X_POLICY_ITEM_ROWFILTER_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE TABLE x_policy_item_rowfilter(
id NUMBER(20) NOT NULL,
guid VARCHAR(64) DEFAULT NULL NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
policy_item_id NUMBER(20) NOT NULL, 
filter_expr VARCHAR(1024) DEFAULT NULL NULL,
primary key (id),
CONSTRAINT x_plc_item_rf_FK_plc_item_id FOREIGN KEY (policy_item_id) REFERENCES x_policy_item (id),
CONSTRAINT x_plc_item_rf_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_plc_item_rf_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
CREATE INDEX x_plc_item_rf_IDX_plc_item_id ON x_policy_item_rowfilter(policy_item_id);

DECLARE
	v_column1_exists number := 0;
	v_column2_exists number := 0;
BEGIN
  Select count(*) into v_column1_exists
    from user_tab_cols
    where column_name = upper('datamask_options')
      and table_name = upper('x_access_type_def');

  Select count(*) into v_column2_exists
    from user_tab_cols
    where column_name = upper('rowfilter_options')
      and table_name = upper('x_access_type_def');

  if (v_column1_exists = 0) AND (v_column2_exists = 0) then
      execute immediate 'ALTER TABLE x_access_type_def ADD (datamask_options VARCHAR(1024) DEFAULT NULL NULL,rowfilter_options VARCHAR(1024) DEFAULT NULL NULL)';
      commit;
  end if;
end;/

DECLARE
	v_column1_exists number := 0;
	v_column2_exists number := 0;
BEGIN
  Select count(*) into v_column1_exists
    from user_tab_cols
    where column_name = upper('datamask_options')
      and table_name = upper('x_resource_def');

 Select count(*) into v_column2_exists
    from user_tab_cols
    where column_name = upper('rowfilter_options')
      and table_name = upper('x_resource_def');

  if (v_column1_exists = 0) AND (v_column2_exists = 0) then
      execute immediate 'ALTER TABLE x_resource_def ADD (datamask_options VARCHAR(1024) DEFAULT NULL NULL,rowfilter_options VARCHAR(1024) DEFAULT NULL NULL)';
      commit;
  end if;
end;/
