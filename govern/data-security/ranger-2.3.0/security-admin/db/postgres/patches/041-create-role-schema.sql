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

DROP TABLE IF EXISTS x_role_ref_role CASCADE;
DROP SEQUENCE IF EXISTS x_role_ref_role_SEQ;
DROP TABLE IF EXISTS x_policy_ref_role CASCADE;
DROP SEQUENCE IF EXISTS x_policy_ref_role_SEQ;
DROP TABLE IF EXISTS x_role_ref_group CASCADE;
DROP SEQUENCE IF EXISTS x_role_ref_group_SEQ;
DROP TABLE IF EXISTS x_role_ref_user CASCADE;
DROP SEQUENCE IF EXISTS x_role_ref_user_SEQ;
DROP TABLE IF EXISTS x_role CASCADE;
DROP SEQUENCE IF EXISTS x_role_SEQ;

CREATE SEQUENCE x_role_SEQ;
CREATE TABLE x_role(
id BIGINT DEFAULT nextval('x_role_SEQ'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
version BIGINT DEFAULT '0' NOT NULL,
name varchar(255) NOT NULL,
description varchar(1024) DEFAULT NULL NULL,
role_options varchar(4000) DEFAULT NULL NULL,
role_text text DEFAULT NULL NULL,
 PRIMARY KEY (id),
 CONSTRAINT x_role_UK_name UNIQUE(name),
 CONSTRAINT x_role_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
 CONSTRAINT x_role_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id)
);
commit;

CREATE SEQUENCE x_role_ref_user_SEQ;
CREATE TABLE x_role_ref_user(
id BIGINT DEFAULT nextval('x_role_ref_user_SEQ'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
role_id BIGINT NOT NULL,
user_id BIGINT DEFAULT NULL NULL,
user_name varchar(767) DEFAULT NULL NULL,
priv_type INT DEFAULT NULL NULL,
 PRIMARY KEY (id),
 CONSTRAINT x_role_ref_user_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
 CONSTRAINT x_role_ref_user_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id),
 CONSTRAINT x_role_ref_user_FK_role_id FOREIGN KEY (role_id) REFERENCES x_role (id),
 CONSTRAINT x_role_ref_user_FK_user_id FOREIGN KEY (user_id) REFERENCES x_user (id)
);
commit;

CREATE SEQUENCE x_role_ref_group_SEQ;
CREATE TABLE x_role_ref_group(
id BIGINT DEFAULT nextval('x_role_ref_group_SEQ'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
role_id BIGINT NOT NULL,
group_id BIGINT DEFAULT NULL NULL,
group_name varchar(767) DEFAULT NULL NULL,
priv_type INT DEFAULT NULL NULL,
 PRIMARY KEY (id),
 CONSTRAINT x_role_ref_grp_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
 CONSTRAINT x_role_ref_grp_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id),
 CONSTRAINT x_role_ref_grp_FK_role_id FOREIGN KEY (role_id) REFERENCES x_role (id),
 CONSTRAINT x_role_ref_grp_FK_group_id FOREIGN KEY (group_id) REFERENCES x_group (id)
);
commit;

CREATE SEQUENCE x_policy_ref_role_SEQ;
CREATE TABLE x_policy_ref_role(
id BIGINT DEFAULT nextval('x_policy_ref_role_SEQ'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
policy_id BIGINT NOT NULL,
role_id BIGINT NOT NULL,
role_name varchar(255) DEFAULT NULL,
 PRIMARY KEY (id),
 CONSTRAINT x_pol_ref_role_UK_polId_roleId UNIQUE(policy_id,role_id),
 CONSTRAINT x_pol_ref_role_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
 CONSTRAINT x_pol_ref_role_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id),
 CONSTRAINT x_pol_ref_role_FK_policy_id FOREIGN KEY (policy_id) REFERENCES x_policy (id),
 CONSTRAINT x_pol_ref_role_FK_role_id FOREIGN KEY (role_id) REFERENCES x_role (id)
);
commit;

CREATE SEQUENCE x_role_ref_role_SEQ;
CREATE TABLE x_role_ref_role(
id BIGINT DEFAULT nextval('x_role_ref_role_SEQ'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
update_time TIMESTAMP DEFAULT NULL NULL,
added_by_id BIGINT DEFAULT NULL NULL,
upd_by_id BIGINT DEFAULT NULL NULL,
role_ref_id BIGINT DEFAULT NULL NULL,
role_id BIGINT NOT NULL,
role_name varchar(255) DEFAULT NULL NULL,
priv_type INT DEFAULT NULL NULL,
 PRIMARY KEY (id),
 CONSTRAINT x_role_ref_role_FK_added_by_id FOREIGN KEY (added_by_id) REFERENCES x_portal_user (id),
 CONSTRAINT x_role_ref_role_FK_upd_by_id FOREIGN KEY (upd_by_id) REFERENCES x_portal_user (id),
 CONSTRAINT x_role_ref_role_FK_role_ref_id FOREIGN KEY (role_ref_id) REFERENCES x_role (id)
);
commit;

