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
CREATE OR REPLACE FUNCTION create_index_on_x_data_hist()
RETURNS void AS $$
DECLARE
	v_attnum1 integer := 0;
	v_attnum2 integer := 0;
BEGIN
	select attnum into v_attnum1 from pg_attribute where attrelid in(select oid from pg_class where relname='x_data_hist') and attname in('obj_id');
	select attnum into v_attnum2 from pg_attribute where attrelid in(select oid from pg_class where relname='x_data_hist') and attname in('obj_class_type');
	IF v_attnum1 > 0 and v_attnum2 > 0 THEN
		IF not exists (select * from pg_index where indrelid in(select oid from pg_class where relname='x_data_hist') and indkey[0]=v_attnum1 and indkey[1]=v_attnum2) THEN
			CREATE INDEX x_data_hist_idx_objid_objclstype ON x_data_hist(obj_id,obj_class_type);
		END IF;
	END IF;
END;
$$ LANGUAGE plpgsql;
select create_index_on_x_data_hist();
select 'delimiter end';