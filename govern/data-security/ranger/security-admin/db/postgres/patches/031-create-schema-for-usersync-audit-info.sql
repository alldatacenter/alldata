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

DROP TABLE IF EXISTS x_ugsync_audit_info CASCADE;
DROP SEQUENCE IF EXISTS x_ugsync_audit_info_seq;

CREATE SEQUENCE x_ugsync_audit_info_seq;

CREATE TABLE x_ugsync_audit_info (
id BIGINT DEFAULT nextval('x_ugsync_audit_info_seq'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
event_time TIMESTAMP DEFAULT NULL NULL,
user_name varchar(255) NOT  NULL,
sync_source varchar(128) NOT NULL,
no_of_new_users bigint NOT NULL,
no_of_new_groups bigint NOT NULL,
no_of_modified_users bigint NOT NULL,
no_of_modified_groups bigint NOT NULL,
sync_source_info varchar(4000) NOT NULL,
session_id varchar(255) DEFAULT NULL,
primary key (id)
);
CREATE INDEX x_ugsync_audit_info_etime ON x_ugsync_audit_info(event_time);
CREATE INDEX x_ugsync_audit_info_sync_src ON x_ugsync_audit_info(sync_source);
CREATE INDEX x_ugsync_audit_info_uname ON x_ugsync_audit_info(user_name);
