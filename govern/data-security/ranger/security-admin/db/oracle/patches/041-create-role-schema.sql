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


call spdropsequence('X_ROLE_REF_ROLE_SEQ');
call spdropsequence('X_POLICY_REF_ROLE_SEQ');
call spdropsequence('X_ROLE_REF_GROUP_SEQ');
call spdropsequence('X_ROLE_REF_USER_SEQ');
call spdropsequence('X_ROLE_SEQ');

CREATE SEQUENCE X_ROLE_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE X_ROLE_REF_USER_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE X_ROLE_REF_GROUP_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE X_POLICY_REF_ROLE_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE X_ROLE_REF_ROLE_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

call spdroptable('x_role_ref_role');
call spdroptable('x_policy_ref_role');
call spdroptable('x_role_ref_group');
call spdroptable('x_role_ref_user');
call spdroptable('x_role');
commit;

CREATE TABLE x_role(
id NUMBER(20) NOT NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
version NUMBER(20) DEFAULT NULL NULL,
name varchar(255) NOT NULL,
description varchar(1024) DEFAULT NULL NULL,
role_options varchar(4000) DEFAULT NULL NULL,
role_text CLOB DEFAULT NULL NULL,
 PRIMARY KEY (id),
 CONSTRAINT x_role_UK_name UNIQUE(name),
 CONSTRAINT x_role_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
 CONSTRAINT x_role_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
commit;

CREATE TABLE x_role_ref_user(
id NUMBER(20) NOT NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
role_id NUMBER(20) NOT NULL,
user_id NUMBER(20) DEFAULT NULL NULL,
user_name varchar(767) DEFAULT NULL NULL,
priv_type NUMBER(10)  DEFAULT NULL NULL,
 PRIMARY KEY (id),
 CONSTRAINT x_role_ref_user_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
 CONSTRAINT x_role_ref_user_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id),
 CONSTRAINT x_role_ref_user_FK_role_id FOREIGN KEY (role_id) REFERENCES x_role (id),
 CONSTRAINT x_role_ref_user_FK_user_id FOREIGN KEY (user_id) REFERENCES x_user (id)
);
commit;

CREATE TABLE x_role_ref_group(
id NUMBER(20) NOT NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
role_id NUMBER(20) NOT NULL,
group_id NUMBER(20) DEFAULT NULL NULL,
group_name varchar(767) DEFAULT NULL NULL,
priv_type NUMBER(10)  DEFAULT NULL NULL,
 PRIMARY KEY (id),
 CONSTRAINT x_role_ref_grp_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
 CONSTRAINT x_role_ref_grp_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id),
 CONSTRAINT x_role_ref_grp_FK_role_id FOREIGN KEY (role_id) REFERENCES x_role (id),
 CONSTRAINT x_role_ref_grp_FK_group_id FOREIGN KEY (group_id) REFERENCES x_group (id)
);
commit;


CREATE TABLE x_policy_ref_role(
id NUMBER(20) NOT NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
policy_id NUMBER(20) NOT NULL,
role_id NUMBER(20) NOT NULL,
role_name varchar(255) DEFAULT NULL NULL,
 PRIMARY KEY (id),
 CONSTRAINT x_pol_ref_role_UK_polId_roleId UNIQUE(policy_id,role_id),
 CONSTRAINT x_pol_ref_role_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
 CONSTRAINT x_pol_ref_role_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id),
 CONSTRAINT x_pol_ref_role_FK_policy_id FOREIGN KEY (policy_id) REFERENCES x_policy (id),
 CONSTRAINT x_pol_ref_role_FK_role_id FOREIGN KEY (role_id) REFERENCES x_role (id)
);
commit;

CREATE TABLE x_role_ref_role(
id NUMBER(20) NOT NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
role_ref_id NUMBER(20) DEFAULT NULL NULL,
role_id NUMBER(20) NOT NULL,
role_name varchar(255) DEFAULT NULL NULL,
priv_type NUMBER(10)  DEFAULT NULL NULL,
 PRIMARY KEY (id),
 CONSTRAINT x_role_ref_role_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
 CONSTRAINT x_role_ref_role_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id),
 CONSTRAINT x_role_ref_role_FK_role_ref_id FOREIGN KEY (role_ref_id) REFERENCES x_role (id)
);
commit;
