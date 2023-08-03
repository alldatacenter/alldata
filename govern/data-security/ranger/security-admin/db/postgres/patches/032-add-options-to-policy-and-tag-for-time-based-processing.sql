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

-- function add_x_policy_columns_for_time_based_classification
select 'delimiter start';
CREATE OR REPLACE FUNCTION add_x_policy_columns_for_time_based_classification()
RETURNS void AS $$
DECLARE
 v_column_exists integer := 0;
BEGIN
 select count(*) into v_column_exists from pg_attribute where attrelid in(select oid from pg_class where relname='x_policy') and attname='policy_options';
 IF v_column_exists = 0 THEN
  ALTER TABLE x_policy ADD COLUMN policy_options VARCHAR(4000) DEFAULT NULL NULL;
 END IF;
  select count(*) into v_column_exists from pg_attribute where attrelid in(select oid from pg_class where relname='x_policy') and attname='policy_priority';
 IF v_column_exists = 0 THEN
  ALTER TABLE x_policy ADD COLUMN policy_priority INT DEFAULT 0 NOT NULL;
 END IF;
END;
$$ LANGUAGE plpgsql;
select 'delimiter end';

select add_x_policy_columns_for_time_based_classification();
select 'delimiter end';

select 'delimiter start';
CREATE OR REPLACE FUNCTION add_x_tag_columns_for_time_based_classification()
RETURNS void AS $$
DECLARE
 v_column_exists integer := 0;
BEGIN
 select count(*) into v_column_exists from pg_attribute where attrelid in(select oid from pg_class where relname='x_tag') and attname='policy_options';
 IF v_column_exists = 0 THEN
  ALTER TABLE x_tag ADD COLUMN policy_options VARCHAR(4000) DEFAULT NULL NULL;
 END IF;
END;
$$ LANGUAGE plpgsql;
select 'delimiter end';

select add_x_tag_columns_for_time_based_classification();
select 'delimiter end';
