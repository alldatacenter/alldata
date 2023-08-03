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
CREATE OR REPLACE FUNCTION change_sort_order_column_datatype_of_resourcemap_tables() 
RETURNS void AS $$
DECLARE
 v_column_exists1 integer := 0;
BEGIN
 select count(*) into v_column_exists1 from pg_attribute where attrelid in(select oid from pg_class where relname='x_policy_resource_map') and attname='sort_order' and attlen=2;
 IF v_column_exists1 = 1 THEN
  ALTER TABLE x_policy_resource_map ALTER COLUMN sort_order TYPE INT;
 END IF;
END;
$$ LANGUAGE plpgsql;
select change_sort_order_column_datatype_of_resourcemap_tables();
select 'delimiter end';