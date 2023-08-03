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
CREATE OR REPLACE FUNCTION add_col_in_x_service_def_and_x_service()
RETURNS void AS $$
DECLARE
 v_column_exists integer := 0;
BEGIN
 select count(*) into v_column_exists from pg_attribute where attrelid in(select oid from pg_class where relname='x_service_def') and attname='display_name';
 IF v_column_exists = 0 THEN
  ALTER TABLE x_service_def ADD COLUMN display_name VARCHAR(1024) DEFAULT NULL NULL;
  UPDATE x_service_def SET display_name=name;
  UPDATE x_service_def SET display_name='Hadoop SQL' where name='hive';
 END IF;

 v_column_exists:=0;
 select count(*) into v_column_exists from pg_attribute where attrelid in(select oid from pg_class where relname='x_service') and attname='display_name';
 IF v_column_exists = 0 THEN
  ALTER TABLE x_service ADD COLUMN display_name VARCHAR(255) DEFAULT NULL NULL;
  UPDATE x_service SET display_name=name;
 END IF;

END;
$$ LANGUAGE plpgsql;
select 'delimiter end';

select add_col_in_x_service_def_and_x_service();
select 'delimiter end';
commit;

select 'delimiter start';
CREATE OR REPLACE FUNCTION add_column_in_x_user_and_x_portal_user_and_x_group()
RETURNS void AS $$
DECLARE
 v_column_exists integer := 0;
BEGIN
 select count(*) into v_column_exists from pg_attribute where attrelid in(select oid from pg_class where relname='x_portal_user') and attname='other_attributes';
 IF v_column_exists = 0 THEN
  ALTER TABLE x_portal_user ADD COLUMN other_attributes TEXT DEFAULT NULL NULL;
 END IF;

 v_column_exists:=0;
 select count(*) into v_column_exists from pg_attribute where attrelid in(select oid from pg_class where relname='x_user') and attname='other_attributes';
 IF v_column_exists = 0 THEN
  ALTER TABLE x_user ADD COLUMN other_attributes TEXT DEFAULT NULL NULL;
 END IF;

 v_column_exists:=0;
 select count(*) into v_column_exists from pg_attribute where attrelid in(select oid from pg_class where relname='x_group') and attname='other_attributes';
 IF v_column_exists = 0 THEN
  ALTER TABLE x_group ADD COLUMN other_attributes TEXT DEFAULT NULL NULL;
 END IF;

END;
$$ LANGUAGE plpgsql;
select 'delimiter end';

select add_column_in_x_user_and_x_portal_user_and_x_group();
select 'delimiter end';