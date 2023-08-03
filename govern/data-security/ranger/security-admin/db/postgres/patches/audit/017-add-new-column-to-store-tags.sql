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

-- function add_column_x_service_def_options
select 'delimiter start';
CREATE OR REPLACE FUNCTION add_column_xa_access_audit_tags() 
RETURNS void AS $$
DECLARE
 v_column_exists integer := 0;
BEGIN
 select count(*) into v_column_exists from pg_attribute where attrelid in(select oid from pg_class where relname='xa_access_audit') and attname='tags';
 IF v_column_exists = 0 THEN
 	ALTER TABLE xa_access_audit ADD COLUMN tags VARCHAR(4000) DEFAULT NULL NULL;
 END IF;
END;
$$ LANGUAGE plpgsql;
select add_column_xa_access_audit_tags();
select 'delimiter end';