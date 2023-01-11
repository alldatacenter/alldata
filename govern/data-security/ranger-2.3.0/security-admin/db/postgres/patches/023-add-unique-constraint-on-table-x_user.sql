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
CREATE OR REPLACE FUNCTION create_unique_constraint_on_username() 
RETURNS void AS $$
DECLARE
 v_attnum integer := 0;
BEGIN
 select attnum into v_attnum from pg_attribute where attrelid in(select oid from pg_class where relname='x_user') and attname='user_name';
 IF v_attnum > 0 THEN
 	IF not exists (select * from pg_constraint where conrelid in(select oid from pg_class where relname='x_user') and conname='x_user_uk_user_name' and contype='u') THEN
 		IF not exists (select * from pg_index where indrelid in(select oid from pg_class where relname='x_user') and indkey[0]=v_attnum) THEN
			ALTER TABLE x_user ALTER COLUMN user_name TYPE VARCHAR(767),ADD CONSTRAINT x_user_UK_user_name UNIQUE(user_name);
 		END IF;
 	END IF;
 END IF;
END;
$$ LANGUAGE plpgsql;
select create_unique_constraint_on_username();
select 'delimiter end';