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

call spdropsequence('X_MODULES_MASTER_SEQ');
call spdropsequence('X_USER_MODULE_PERM_SEQ');
call spdropsequence('X_GROUP_MODULE_PERM_SEQ');

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

call spdroptable('x_group_module_perm');
call spdroptable('x_user_module_perm');
call spdroptable('x_modules_master');

CREATE SEQUENCE X_MODULES_MASTER_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE TABLE x_modules_master(
id NUMBER(20) NOT NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
module VARCHAR(1024) NOT NULL,
url VARCHAR(1024) DEFAULT NULL NULL,
PRIMARY KEY (id)
);
COMMIT;
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
INSERT INTO x_modules_master VALUES(X_MODULES_MASTER_SEQ.NEXTVAL,SYSDATE,SYSDATE,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),'Resource Based Policies','');
INSERT INTO x_modules_master VALUES(X_MODULES_MASTER_SEQ.NEXTVAL,SYSDATE,SYSDATE,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),'Users/Groups','');
INSERT INTO x_modules_master VALUES(X_MODULES_MASTER_SEQ.NEXTVAL,SYSDATE,SYSDATE,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),'Reports','');
INSERT INTO x_modules_master VALUES(X_MODULES_MASTER_SEQ.NEXTVAL,SYSDATE,SYSDATE,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),'Audit','');
INSERT INTO x_modules_master VALUES(X_MODULES_MASTER_SEQ.NEXTVAL,SYSDATE,SYSDATE,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),'Key Manager','');
COMMIT;
CREATE SEQUENCE X_USER_MODULE_PERM_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE TABLE x_user_module_perm(
id NUMBER(20) NOT NULL,
user_id NUMBER(20) DEFAULT NULL NULL,
module_id NUMBER(20) DEFAULT NULL NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
is_allowed NUMBER(11) DEFAULT '1' NOT NULL ,
PRIMARY KEY (id),
CONSTRAINT x_user_module_perm_FK_moduleid FOREIGN KEY (module_id) REFERENCES x_modules_master(id),
CONSTRAINT x_user_module_perm_FK_userid FOREIGN KEY (user_id) REFERENCES x_portal_user(id) 
);
COMMIT;
CREATE SEQUENCE X_GROUP_MODULE_PERM_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE TABLE x_group_module_perm(
id NUMBER(20) NOT NULL,
group_id NUMBER(20) DEFAULT NULL NULL,
module_id NUMBER(20) DEFAULT NULL NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
is_allowed NUMBER(11) DEFAULT '1' NOT NULL ,
PRIMARY KEY (id),
CONSTRAINT x_grp_module_perm_FK_module_id FOREIGN KEY (module_id) REFERENCES x_modules_master(id),
CONSTRAINT x_grp_module_perm_FK_group_id FOREIGN KEY (group_id) REFERENCES x_group(id) 
);
COMMIT;
CREATE INDEX x_usr_module_perm_idx_moduleid ON x_user_module_perm(module_id);
CREATE INDEX x_usr_module_perm_idx_userid ON x_user_module_perm(user_id);
CREATE INDEX x_grp_module_perm_idx_groupid ON x_group_module_perm(group_id);
CREATE INDEX x_grp_module_perm_idx_moduleid ON x_group_module_perm(module_id);
COMMIT;