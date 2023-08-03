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

DELIMITER $$
DROP PROCEDURE if exists getXportalUIdByLoginId$$
CREATE PROCEDURE `getXportalUIdByLoginId`(IN input_val VARCHAR(100), OUT myid BIGINT)
BEGIN
SET myid = 0;
SELECT x_portal_user.id into myid FROM x_portal_user WHERE x_portal_user.login_id = input_val;
END $$

DELIMITER ;

drop procedure if exists insert_statename_in_x_ranger_global_state;

delimiter ;;
create procedure insert_statename_in_x_ranger_global_state() begin
	DECLARE adminID bigint;
	if exists (select * from information_schema.columns where table_schema=database() and table_name = 'x_ranger_global_state' and column_name='state_name')
	then
		call getXportalUIdByLoginId('admin', adminID);
		if not exists(select * from x_ranger_global_state where state_name='RangerRole')
		then
			INSERT INTO x_ranger_global_state (create_time,update_time,added_by_id,upd_by_id,version,state_name,app_data) VALUES (UTC_TIMESTAMP(),UTC_TIMESTAMP(),adminID,adminID,1,'RangerRole','{"Version":"1"}');
		end if;
		
		if not exists(select * from x_ranger_global_state where state_name='RangerUserStore')
		then
			INSERT INTO x_ranger_global_state (create_time,update_time,added_by_id,upd_by_id,version,state_name,app_data) VALUES (UTC_TIMESTAMP(),UTC_TIMESTAMP(),adminID,adminID,1,'RangerUserStore','{"Version":"1"}');
		end if;

		if not exists(select * from x_ranger_global_state where state_name='RangerSecurityZone')
		then
			INSERT INTO x_ranger_global_state (create_time,update_time,added_by_id,upd_by_id,version,state_name,app_data) VALUES (UTC_TIMESTAMP(),UTC_TIMESTAMP(),adminID,adminID,1,'RangerSecurityZone','{"Version":"1"}');
		end if;

	end if;
end;;

delimiter ;
call insert_statename_in_x_ranger_global_state();
drop procedure if exists insert_statename_in_x_ranger_global_state;	
