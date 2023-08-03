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

call spdropsequence('X_TAG_CHANGE_LOG_SEQ');

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

call spdroptable('X_TAG_CHANGE_LOG');

CREATE SEQUENCE X_TAG_CHANGE_LOG_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE TABLE x_tag_change_log (
id NUMBER(20) NOT NULL,
create_time DATE DEFAULT NULL NULL,
service_id NUMBER(20) NOT NULL,
change_type NUMBER(11) NOT NULL,
service_tags_version NUMBER(20) DEFAULT '0' NOT NULL,
service_resource_id NUMBER(20) DEFAULT NULL NULL,
tag_id NUMBER(20) DEFAULT NULL NULL,
primary key (id)
);
CREATE INDEX x_tag_chng_log_IDX_service_id ON x_tag_change_log(service_id);
CREATE INDEX x_tag_chng_log_IDX_tag_ver ON x_tag_change_log(service_tags_version);

COMMIT;

