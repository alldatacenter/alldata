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

DROP TABLE IF EXISTS x_rms_service_resource CASCADE;
DROP TABLE IF EXISTS x_rms_notification CASCADE;
DROP TABLE IF EXISTS x_rms_resource_mapping CASCADE;
DROP TABLE IF EXISTS x_rms_mapping_provider CASCADE;

DROP SEQUENCE IF EXISTS X_RMS_SERVICE_RESOURCE_SEQ;
DROP SEQUENCE IF EXISTS X_RMS_NOTIFICATION_SEQ;
DROP SEQUENCE IF EXISTS X_RMS_RESOURCE_MAPPING_SEQ;
DROP SEQUENCE IF EXISTS X_RMS_MAPPING_PROVIDER_SEQ;


CREATE SEQUENCE x_rms_service_resource_seq;
CREATE TABLE x_rms_service_resource(
id BIGINT DEFAULT nextval('x_rms_service_resource_seq'::regclass),
guid VARCHAR(64) NOT NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
version BIGINT DEFAULT NULL NULL,
service_id BIGINT NOT NULL,
resource_signature VARCHAR(128) DEFAULT NULL NULL,
is_enabled BOOLEAN DEFAULT '1' NOT NULL,
service_resource_elements_text TEXT DEFAULT NULL NULL,
primary key (id),
CONSTRAINT x_rms_service_res_UK_guid UNIQUE (guid),
CONSTRAINT x_rms_service_res_FK_service_id FOREIGN KEY (service_id) REFERENCES x_service (id)
);

CREATE INDEX x_rms_service_resource_IDX_service_id ON x_rms_service_resource(service_id);

CREATE SEQUENCE X_RMS_NOTIFICATION_SEQ;
CREATE TABLE x_rms_notification (
id BIGINT DEFAULT nextval('X_RMS_NOTIFICATION_SEQ'::regclass),
hms_name VARCHAR(128) NULL DEFAULT NULL,
notification_id BIGINT NULL DEFAULT NULL,
change_timestamp TIMESTAMP NULL DEFAULT NULL,
change_type VARCHAR(64) NULL DEFAULT  NULL,
hl_resource_id BIGINT NULL DEFAULT NULL,
hl_service_id BIGINT NULL DEFAULT NULL,
ll_resource_id BIGINT NULL DEFAULT NULL,
ll_service_id BIGINT NULL DEFAULT NULL,
PRIMARY KEY (id),
CONSTRAINT x_rms_notification_FK_hl_service_id FOREIGN KEY(hl_service_id) REFERENCES x_service(id),
CONSTRAINT x_rms_notification_FK_ll_service_id FOREIGN KEY(ll_service_id) REFERENCES x_service(id)
);

CREATE INDEX x_rms_notification_IDX_notification_id ON x_rms_notification(notification_id);
CREATE INDEX x_rms_notification_IDX_hms_name_notification_id ON x_rms_notification(hms_name, notification_id);
CREATE INDEX x_rms_notification_IDX_hl_service_id ON x_rms_notification(hl_service_id);
CREATE INDEX x_rms_notification_IDX_ll_service_id ON x_rms_notification(ll_service_id);

CREATE SEQUENCE X_RMS_RESOURCE_MAPPING_SEQ;
CREATE TABLE x_rms_resource_mapping(
id BIGINT DEFAULT nextval('X_RMS_RESOURCE_MAPPING_SEQ'::regclass),
change_timestamp TIMESTAMP NULL DEFAULT NULL,
hl_resource_id BIGINT NOT NULL,
ll_resource_id BIGINT NOT NULL,
PRIMARY KEY (id),
CONSTRAINT x_rms_res_map_UK_hl_res_id_ll_res_id UNIQUE(hl_resource_id, ll_resource_id),
CONSTRAINT x_rms_res_map_FK_hl_res_id FOREIGN KEY(hl_resource_id) REFERENCES x_rms_service_resource(id),
CONSTRAINT x_rms_res_map_FK_ll_res_id FOREIGN KEY(ll_resource_id) REFERENCES x_rms_service_resource(id)
);

CREATE INDEX x_rms_resource_mapping_IDX_hl_resource_id ON x_rms_resource_mapping(hl_resource_id);
CREATE INDEX x_rms_resource_mapping_IDX_ll_resource_id ON x_rms_resource_mapping(ll_resource_id);

CREATE SEQUENCE X_RMS_MAPPING_PROVIDER_SEQ;
CREATE TABLE x_rms_mapping_provider (
id BIGINT DEFAULT nextval('X_RMS_MAPPING_PROVIDER_SEQ'::regclass),
change_timestamp TIMESTAMP DEFAULT NULL NULL,
name VARCHAR(128) NOT NULL,
last_known_version BIGINT NOT NULL,
PRIMARY KEY (id),
CONSTRAINT x_rms_mapping_provider_UK_name UNIQUE(name)
);
