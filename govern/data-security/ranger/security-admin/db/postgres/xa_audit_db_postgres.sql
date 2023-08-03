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

DROP TABLE IF EXISTS xa_access_audit CASCADE;
DROP SEQUENCE IF EXISTS xa_access_audit_seq;
CREATE SEQUENCE xa_access_audit_seq;
CREATE TABLE xa_access_audit(
id BIGINT DEFAULT nextval('xa_access_audit_seq'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
audit_type INT DEFAULT '0' NOT NULL,
access_result INT DEFAULT '0' NULL,
access_type VARCHAR(255) DEFAULT NULL NULL,
acl_enforcer VARCHAR(255) DEFAULT NULL NULL,
agent_id VARCHAR(255) DEFAULT NULL NULL,
client_ip VARCHAR(255) DEFAULT NULL NULL,
client_type VARCHAR(255) DEFAULT NULL NULL,
policy_id BIGINT DEFAULT '0' NULL,
repo_name VARCHAR(255) DEFAULT NULL NULL,
repo_type INT DEFAULT '0' NULL,
result_reason VARCHAR(255) DEFAULT NULL NULL,
session_id VARCHAR(255) DEFAULT NULL NULL,
event_time TIMESTAMP DEFAULT NULL NULL,
request_user VARCHAR(255) DEFAULT NULL NULL,
action VARCHAR(2000) DEFAULT NULL NULL,
request_data VARCHAR(4000) DEFAULT NULL NULL,
resource_path VARCHAR(4000) DEFAULT NULL NULL,
resource_type VARCHAR(255) DEFAULT NULL NULL,
seq_num BIGINT DEFAULT '0' NULL,
event_count BIGINT DEFAULT '1' NULL,
event_dur_ms BIGINT DEFAULT '1' NULL,
PRIMARY KEY (id)
);
CREATE INDEX xa_access_audit_added_by_id ON  xa_access_audit(added_by_id);
CREATE INDEX xa_access_audit_upd_by_id ON  xa_access_audit(upd_by_id);
CREATE INDEX xa_access_audit_cr_time ON  xa_access_audit(create_time);
CREATE INDEX xa_access_audit_up_time ON  xa_access_audit(update_time);
CREATE INDEX xa_access_audit_event_time ON  xa_access_audit(event_time);
commit;