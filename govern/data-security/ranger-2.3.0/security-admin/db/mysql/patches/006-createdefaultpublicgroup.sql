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

drop procedure if exists insert_public_group_in_x_group_table;

delimiter ;;
create procedure insert_public_group_in_x_group_table() begin

 /* check table x_group exist or not */
 if exists (select * from information_schema.columns where table_schema=database() and table_name = 'x_group') then
 	/* check record for group name public exist or not */
 	if not exists (select * from x_group where group_name = 'public') then
 		INSERT INTO x_group (ADDED_BY_ID, CREATE_TIME, DESCR, GROUP_SRC, GROUP_TYPE, GROUP_NAME, STATUS, UPDATE_TIME, UPD_BY_ID) VALUES (1, UTC_TIMESTAMP(), 'public group', 0, 0, 'public', 0, UTC_TIMESTAMP(), 1);
 	end if;
 end if;
  
end;;

delimiter ;
call insert_public_group_in_x_group_table();

drop procedure if exists insert_public_group_in_x_group_table;
