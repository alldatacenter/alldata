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

CREATE OR REPLACE FUNCTION getXportalUIdByLoginId(input_val IN VARCHAR2)
RETURN NUMBER iS
BEGIN
DECLARE
myid Number := 0;
begin
    SELECT x_portal_user.id into myid FROM x_portal_user
    WHERE x_portal_user.login_id=input_val;
    RETURN myid;
end;
END;/

DECLARE
	t_count number:=0;
	v_count_1 number:=0;
	v_count_2 number:=0;
	v_count_3 number:=0;
	sql_stmt VARCHAR(1024);
	state_name_1 varchar(255):='RangerRole';
	state_name_2 varchar(255):='RangerUserStore';
	state_name_3 varchar(255):='RangerSecurityZone';
	app_data_1 varchar(255):='{"Version":"1"}';
	x_portal_user_id number:=getXportalUIdByLoginId('admin');
BEGIN
	select count(*) into t_count from user_tables where table_name = 'x_ranger_global_state';
	if (t_count > 0) then
		
		select count(*) into v_count_1 from x_ranger_global_state where state_name='RangerRole';
		if (v_count_1 = 0) then
			sql_stmt := 'INSERT INTO x_ranger_global_state (id,create_time,update_time,added_by_id,upd_by_id,version,state_name,app_data) VALUES (X_RANGER_GLOBAL_STATE_SEQ.nextval,sys_extract_utc(systimestamp),sys_extract_utc(systimestamp),:1,:2,1,:3,:4)';
			EXECUTE IMMEDIATE sql_stmt USING x_portal_user_id,x_portal_user_id,state_name_1,app_data_1;
			commit;
		end if;
		
		select count(*) into v_count_2 from x_ranger_global_state where state_name='RangerUserStore';
		if (v_count_2 = 0) then
			sql_stmt := 'INSERT INTO x_ranger_global_state (id,create_time,update_time,added_by_id,upd_by_id,version,state_name,app_data) VALUES (X_RANGER_GLOBAL_STATE_SEQ.nextval,sys_extract_utc(systimestamp),sys_extract_utc(systimestamp),:1,:2,1,:3,:4)';
			EXECUTE IMMEDIATE sql_stmt USING x_portal_user_id,x_portal_user_id,state_name_2,app_data_1;
			commit;
		end if;
		
		select count(*) into v_count_3 from x_ranger_global_state where state_name='RangerSecurityZone';
		if (v_count_3 = 0) then
			sql_stmt := 'INSERT INTO x_ranger_global_state (id,create_time,update_time,added_by_id,upd_by_id,version,state_name,app_data) VALUES (X_RANGER_GLOBAL_STATE_SEQ.nextval,sys_extract_utc(systimestamp),sys_extract_utc(systimestamp),:1,:2,1,:3,:4)';
			EXECUTE IMMEDIATE sql_stmt USING x_portal_user_id,x_portal_user_id,state_name_3,app_data_1;
			commit;
		end if;

	end if;
end;/
