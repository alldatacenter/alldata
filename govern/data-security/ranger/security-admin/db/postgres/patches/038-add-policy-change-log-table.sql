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

DROP TABLE IF EXISTS x_policy_change_log;
DROP SEQUENCE IF EXISTS x_policy_change_log_seq;

CREATE SEQUENCE x_policy_change_log_seq;

CREATE TABLE x_policy_change_log (
id BIGINT DEFAULT nextval('x_policy_change_log_seq'::regclass),
create_time TIMESTAMP DEFAULT NULL NULL,
service_id bigint NOT NULL,
change_type int NOT NULL,
policy_version bigint DEFAULT '0' NOT NULL,
service_type varchar(256) DEFAULT NULL NULL,
policy_type int DEFAULT NULL NULL,
zone_name varchar(256) DEFAULT NULL NULL,
policy_id bigint DEFAULT NULL NULL,
primary key (id)
);
commit;
CREATE INDEX x_policy_change_log_IDX_service_id ON x_policy_change_log(service_id);
CREATE INDEX x_policy_change_log_IDX_policy_version ON x_policy_change_log(policy_version);
commit;
