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
DROP TABLE IF EXISTS x_policy_label_map CASCADE;
DROP SEQUENCE IF EXISTS x_policy_label_map_seq;
DROP TABLE IF EXISTS x_policy_label CASCADE;
DROP SEQUENCE IF EXISTS x_policy_label_seq;
commit;
CREATE SEQUENCE x_policy_label_seq;
CREATE TABLE x_policy_label (
id BIGINT DEFAULT nextval('x_policy_label_seq'::regclass),
guid VARCHAR(64) DEFAULT NULL NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
label_name VARCHAR(512) DEFAULT NULL,
primary key (id),
CONSTRAINT x_policy_label_UK_label_name UNIQUE (label_name),
CONSTRAINT x_policy_label_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_policy_label_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
commit;
CREATE SEQUENCE x_policy_label_map_seq;
CREATE TABLE  x_policy_label_map (
id BIGINT DEFAULT nextval('x_policy_label_map_seq'::regclass),
guid VARCHAR(64) DEFAULT NULL NULL,
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
policy_id BIGINT DEFAULT NULL,
policy_label_id BIGINT DEFAULT NULL,
primary key (id),
CONSTRAINT x_policy_label_map_pid_plid UNIQUE (policy_id, policy_label_id),
CONSTRAINT x_policy_label_map_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_policy_label_map_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id),
CONSTRAINT x_policy_label_map_FK_policy_id FOREIGN KEY (policy_id) REFERENCES x_policy (id),
CONSTRAINT x_policy_label_map_FK_policy_label_id FOREIGN KEY (policy_label_id) REFERENCES x_policy_label (id)
);
commit;
CREATE INDEX x_policy_label_label_id ON x_policy_label(id);
CREATE INDEX x_policy_label_label_name ON x_policy_label(label_name);
CREATE INDEX x_policy_label_label_map_id ON x_policy_label_map(id);
commit;
