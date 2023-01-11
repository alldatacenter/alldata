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

drop procedure if exists create_tag_sync;

delimiter ;;
create procedure create_tag_sync() begin
DECLARE loginID bigint(20);
 /* check tables exist or not */
 if exists (select * from information_schema.columns where table_schema=database() and table_name = 'x_portal_user') then
 	if exists (select * from information_schema.columns where table_schema=database() and table_name = 'x_portal_user_role') then
	 	if exists (select * from information_schema.columns where table_schema=database() and table_name = 'x_user') then
	 		/* check record for login id rangertagsync exist or not */
		 	if not exists (select * from x_portal_user where login_id = 'rangertagsync') then
		 		INSERT INTO x_portal_user(create_time,update_time,added_by_id,upd_by_id,first_name,last_name,pub_scr_name,login_id,password,email,status,user_src,notes) VALUES (UTC_TIMESTAMP(),UTC_TIMESTAMP(),NULL,NULL,'rangertagsync','','rangertagsync','rangertagsync','f5820e1229418dcf2575908f2c493da5','rangertagsync',1,0,NULL);		
			end if;
			set loginID = (select id from x_portal_user where login_id = 'rangertagsync');
		 	if not exists (select * from x_portal_user_role where user_id =loginID ) then		 		
		 		INSERT INTO x_portal_user_role(create_time,update_time,added_by_id,upd_by_id,user_id,user_role,status) VALUES (UTC_TIMESTAMP(),UTC_TIMESTAMP(),NULL,NULL,loginID,'ROLE_SYS_ADMIN',1);
			end if;
			if not exists (select * from x_user where user_name = 'rangertagsync') then
		 		INSERT INTO x_user(create_time,update_time,added_by_id,upd_by_id,user_name,descr,status) values (UTC_TIMESTAMP(), UTC_TIMESTAMP(),NULL,NULL,'rangertagsync','rangertagsync',0);
		 	end if;
		end if;
	end if;
 end if;
  
end;;

delimiter ;
call create_tag_sync();

drop procedure if exists create_tag_sync;
