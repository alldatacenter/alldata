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

call spdropsequence('X_RMS_SERVICE_RESOURCE_SEQ');
call spdropsequence('X_RMS_NOTIFICATION_SEQ');
call spdropsequence('X_RMS_RESOURCE_MAPPING_SEQ');
call spdropsequence('X_RMS_MAPPING_PROVIDER_SEQ');

commit;

CREATE SEQUENCE X_RMS_SERVICE_RESOURCE_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE X_RMS_NOTIFICATION_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE X_RMS_RESOURCE_MAPPING_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE X_RMS_MAPPING_PROVIDER_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;


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

call spdroptable('X_RMS_NOTIFICATION');
call spdroptable('X_RMS_RESOURCE_MAPPING');
call spdroptable('X_RMS_MAPPING_PROVIDER');
call spdroptable('X_RMS_SERVICE_RESOURCE');

CREATE TABLE x_rms_service_resource(
id NUMBER(20) NOT NULL,
guid VARCHAR(1024) DEFAULT NULL NULL,
create_time DATE DEFAULT NULL NULL,
update_time DATE DEFAULT NULL NULL,
added_by_id NUMBER(20) DEFAULT NULL NULL,
upd_by_id NUMBER(20) DEFAULT NULL NULL,
version NUMBER(20) DEFAULT NULL NULL,
service_id NUMBER(20) NOT NULL,
resource_signature VARCHAR(128) DEFAULT NULL NULL,
is_enabled NUMBER(1) DEFAULT '1' NOT NULL,
service_resource_elements_text CLOB DEFAULT NULL NULL,
primary key (id),
CONSTRAINT x_rms_service_res_UK_guid UNIQUE (guid),
CONSTRAINT x_rms_svc_res_FK_service_id FOREIGN KEY (service_id) REFERENCES x_service (id)
);

CREATE INDEX x_rms_svc_res_IDX_service_id ON x_rms_service_resource(service_id);

CREATE TABLE x_rms_notification (
id NUMBER(20) NOT NULL,
hms_name VARCHAR(128) DEFAULT NULL NULL,
notification_id NUMBER(20) DEFAULT NULL NULL,
change_timestamp DATE DEFAULT NULL NULL,
change_type VARCHAR(64) DEFAULT NULL NULL,
hl_resource_id NUMBER(20) DEFAULT NULL NULL,
hl_service_id NUMBER(20) DEFAULT NULL NULL,
ll_resource_id NUMBER(20) DEFAULT NULL NULL,
ll_service_id NUMBER(20) DEFAULT NULL NULL,
PRIMARY KEY (id),
CONSTRAINT x_rms_notis_FK_hl_service_id FOREIGN KEY(hl_service_id) REFERENCES x_service(id),
CONSTRAINT x_rms_notis_FK_ll_service_id FOREIGN KEY(ll_service_id) REFERENCES x_service(id)
);

CREATE INDEX x_rms_notis_IDX_notis_id ON x_rms_notification(notification_id);
CREATE INDEX x_rms_notis_IDX_hms_notis_id ON x_rms_notification(hms_name, notification_id);
CREATE INDEX x_rms_notis_IDX_hl_svc_id ON x_rms_notification(hl_service_id);
CREATE INDEX x_rms_notis_IDX_ll_svc_id ON x_rms_notification(ll_service_id);

CREATE TABLE x_rms_resource_mapping(
id NUMBER(20) NOT NULL,
change_timestamp DATE DEFAULT NULL NULL,
hl_resource_id NUMBER(20) NOT NULL,
ll_resource_id NUMBER(20) NOT NULL,
PRIMARY KEY (id),
CONSTRAINT x_rms_res_map_UK_hl_id_ll_id UNIQUE(hl_resource_id, ll_resource_id),
CONSTRAINT x_rms_res_map_FK_hl_res_id FOREIGN KEY(hl_resource_id) REFERENCES x_rms_service_resource(id),
CONSTRAINT x_rms_res_map_FK_ll_res_id FOREIGN KEY(ll_resource_id) REFERENCES x_rms_service_resource(id)
);

CREATE INDEX x_rms_res_map_IDX_hl_svc_id ON x_rms_resource_mapping(hl_resource_id);
CREATE INDEX x_rms_res_map_IDX_ll_svc_id ON x_rms_resource_mapping(ll_resource_id);


CREATE TABLE x_rms_mapping_provider (
id NUMBER(20) NOT NULL,
change_timestamp DATE DEFAULT NULL NULL,
name VARCHAR(128) NOT NULL,
last_known_version NUMBER(20) NOT NULL,
PRIMARY KEY (id),
CONSTRAINT x_rms_map_provider_UK_name UNIQUE(name)
);

commit;
