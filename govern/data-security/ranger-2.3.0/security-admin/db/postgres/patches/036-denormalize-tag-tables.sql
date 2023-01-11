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

-- function denormalize_tag_tables()
select 'delimiter start';
CREATE OR REPLACE FUNCTION denormalize_tag_tables()
RETURNS void AS $$
DECLARE
 v_column_exists integer := 0;
BEGIN
 select count(*) into v_column_exists from pg_attribute where attrelid in(select oid from pg_class where relname='x_tag_def') and attname='tag_attrs_def_text';
 IF v_column_exists = 0 THEN
  ALTER TABLE x_tag_def ADD COLUMN tag_attrs_def_text TEXT DEFAULT NULL NULL;
 END IF;
  select count(*) into v_column_exists from pg_attribute where attrelid in(select oid from pg_class where relname='x_tag') and attname='tag_attrs_text';
 IF v_column_exists = 0 THEN
  ALTER TABLE x_tag ADD COLUMN tag_attrs_text TEXT DEFAULT NULL NULL;
 END IF;
  select count(*) into v_column_exists from pg_attribute where attrelid in(select oid from pg_class where relname='x_service_resource') and attname='service_resource_elements_text';
 IF v_column_exists = 0 THEN
  ALTER TABLE x_service_resource ADD COLUMN service_resource_elements_text TEXT DEFAULT NULL NULL;
 END IF;
   select count(*) into v_column_exists from pg_attribute where attrelid in(select oid from pg_class where relname='x_service_resource') and attname='tags_text';
 IF v_column_exists = 0 THEN
  ALTER TABLE x_service_resource ADD COLUMN tags_text TEXT DEFAULT NULL NULL;
 END IF;
END;
$$ LANGUAGE plpgsql;
select 'delimiter end';

select denormalize_tag_tables();
select 'delimiter end';

select 'delimiter start';
CREATE OR REPLACE FUNCTION remove_foreign_key(objName varchar(4000))
RETURNS void AS $$
declare
 tableName VARCHAR(256);
 constraintName VARCHAR(512);
 query varchar(4000);
 curs CURSOR FOR SELECT table_name,constraint_name from information_schema.key_column_usage where constraint_catalog=current_catalog and table_name=objName and position_in_unique_constraint notnull;
begin
  OPEN curs;
  loop
	FETCH curs INTO tableName,constraintName;
	EXIT WHEN NOT FOUND;
	query :='ALTER TABLE ' || objName || ' drop constraint ' || constraintName;
	execute query;
  end loop;
  close curs;
END;
$$ LANGUAGE plpgsql;
select 'delimiter end';

CREATE OR REPLACE FUNCTION removekeys()
RETURNS void AS
$$
BEGIN
	perform remove_foreign_key('x_tag_attr_def');
	perform remove_foreign_key('x_tag_attr');
	perform remove_foreign_key('x_service_resource_element');
	perform remove_foreign_key('x_service_resource_element_val');
END;
$$ LANGUAGE plpgsql;
select removekeys();

select 'delimiter end';
