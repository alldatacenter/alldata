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

call spdropsequence('X_UGSYNC_AUDIT_INFO_SEQ');

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

call spdroptable('x_ugsync_audit_info');

CREATE SEQUENCE X_UGSYNC_AUDIT_INFO_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE TABLE x_ugsync_audit_info(
id NUMBER(20) NOT NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
event_time DATE DEFAULT NULL NULL,
user_name VARCHAR(255) NOT  NULL,
sync_source VARCHAR(128) NOT NULL,
no_of_new_users NUMBER(20) NOT NULL,
no_of_new_groups NUMBER(20) NOT NULL,
no_of_modified_users NUMBER(20) NOT NULL,
no_of_modified_groups NUMBER(20) NOT NULL,
sync_source_info VARCHAR(4000) NOT NULL,
session_id VARCHAR(255) DEFAULT NULL,
 PRIMARY KEY (id)
);
CREATE INDEX x_ugsync_audit_info_etime ON x_ugsync_audit_info(event_time);
CREATE INDEX x_ugsync_audit_info_sync_src ON x_ugsync_audit_info(sync_source);
CREATE INDEX x_ugsync_audit_info_uname ON x_ugsync_audit_info(user_name);
COMMIT;