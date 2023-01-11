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

select 'delimiter start';
CREATE OR REPLACE FUNCTION create_unique_constraint_on_change_logs()
RETURNS void AS $$
BEGIN
	IF not exists (select * from pg_constraint where conrelid in(select oid from pg_class where relname='x_policy_change_log') and conname='x_policy_change_log_uk_service_id_policy_version' and contype='u') THEN
		ALTER TABLE x_policy_change_log ADD CONSTRAINT x_policy_change_log_uk_service_id_policy_version UNIQUE(service_id, policy_version);
	END IF;
	IF not exists (select * from pg_constraint where conrelid in(select oid from pg_class where relname='x_tag_change_log') and conname='x_tag_change_log_uk_service_id_service_tags_version' and contype='u') THEN
		ALTER TABLE x_tag_change_log ADD CONSTRAINT x_tag_change_log_uk_service_id_service_tags_version UNIQUE(service_id, service_tags_version);
	END IF;

END;
$$ LANGUAGE plpgsql;
truncate x_policy_change_log;
truncate x_tag_change_log;
select create_unique_constraint_on_change_logs();
select 'delimiter end';
