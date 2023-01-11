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
CREATE OR REPLACE FUNCTION alter_column_sort_order_to_int(tableName varchar(64))
RETURNS void AS $$
declare
 v_column_exists integer := 0;
 query varchar(4000);
begin
 select count(*) into v_column_exists from pg_attribute where attrelid in(select oid from pg_class where relname=tableName) and attname='sort_order' and attlen=2;
 IF v_column_exists = 1 THEN
  query := 'ALTER TABLE ' || tableName || ' ALTER COLUMN sort_order TYPE INT';
	execute query;
 END IF;
END;
$$ LANGUAGE plpgsql;
select 'delimiter end';

select alter_column_sort_order_to_int('x_service_config_def');
select alter_column_sort_order_to_int('x_resource_def');
select alter_column_sort_order_to_int('x_access_type_def');
select alter_column_sort_order_to_int('x_policy_condition_def');
select alter_column_sort_order_to_int('x_context_enricher_def');
select alter_column_sort_order_to_int('x_enum_element_def');
select alter_column_sort_order_to_int('x_policy_resource_map');
select alter_column_sort_order_to_int('x_policy_item');
select alter_column_sort_order_to_int('x_policy_item_access');
select alter_column_sort_order_to_int('x_policy_item_condition');
select alter_column_sort_order_to_int('x_policy_item_user_perm');
select alter_column_sort_order_to_int('x_policy_item_group_perm');
select alter_column_sort_order_to_int('x_datamask_type_def');
