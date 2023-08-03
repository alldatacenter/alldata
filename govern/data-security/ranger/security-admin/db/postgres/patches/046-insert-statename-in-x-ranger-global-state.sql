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

CREATE OR REPLACE FUNCTION getXportalUIdByLoginId(input_val varchar(100))
RETURNS bigint LANGUAGE SQL AS $$ SELECT x_portal_user.id FROM x_portal_user
WHERE x_portal_user.login_id = $1; $$;

select 'delimiter start';
CREATE OR REPLACE FUNCTION insert_statename_in_x_ranger_global_state()
RETURNS void AS $$
DECLARE
	t_count integer:=0;
	v_count_1 integer:=0;
	v_count_2 integer:=0;
	v_count_3 integer:=0;
BEGIN
	select count(*) into t_count from pg_class where relname = 'x_ranger_global_state';
	IF (t_count > 0) then
		
		select count(*) into v_count_1 from x_ranger_global_state where state_name='RangerRole';
		IF (v_count_1 = 0) then
			INSERT INTO x_ranger_global_state (create_time,update_time,added_by_id,upd_by_id,version,state_name,app_data) VALUES (current_timestamp,current_timestamp,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),1,'RangerRole','{"Version":"1"}');
		END IF;
		
		select count(*) into v_count_2 from x_ranger_global_state where state_name='RangerUserStore';
		IF (v_count_2 = 0) then
			INSERT INTO x_ranger_global_state (create_time,update_time,added_by_id,upd_by_id,version,state_name,app_data) VALUES (current_timestamp,current_timestamp,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),1,'RangerUserStore','{"Version":"1"}');
		END IF;
		
		select count(*) into v_count_3 from x_ranger_global_state where state_name='RangerSecurityZone';
		IF (v_count_3 = 0) then
			INSERT INTO x_ranger_global_state (create_time,update_time,added_by_id,upd_by_id,version,state_name,app_data) VALUES (current_timestamp,current_timestamp,getXportalUIdByLoginId('admin'),getXportalUIdByLoginId('admin'),1,'RangerSecurityZone','{"Version":"1"}');
		END IF;
	END IF;
END;
$$ LANGUAGE plpgsql;
select 'delimiter end';

select insert_statename_in_x_ranger_global_state();
select 'delimiter end';
