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
CREATE OR REPLACE FUNCTION create_tag_sync() 
RETURNS void AS $$
DECLARE
 is_exist_x_portal_user integer := 0;
 is_exist_x_portal_user_role integer := 0;
 is_exist_x_user integer := 0;
 loginID BIGINT := 0;
BEGIN
 select count(*) into is_exist_x_portal_user from pg_class where relname='x_portal_user';
 select count(*) into is_exist_x_portal_user_role from pg_class where relname='x_portal_user_role';
 select count(*) into is_exist_x_user from pg_class where relname='x_user';
 IF is_exist_x_portal_user > 0 AND is_exist_x_portal_user_role > 0 AND is_exist_x_user > 0 THEN
	IF not exists (select * from x_portal_user where login_id = 'rangertagsync') THEN
		INSERT INTO x_portal_user(create_time,update_time,added_by_id,upd_by_id,first_name,last_name,pub_scr_name,login_id,password,email,status,user_src,notes) VALUES (current_timestamp,current_timestamp,NULL,NULL,'rangertagsync','','rangertagsync','rangertagsync','f5820e1229418dcf2575908f2c493da5','rangertagsync',1,0,NULL);		
	END IF;
	select id into loginID from x_portal_user where login_id = 'rangertagsync';
	IF not exists (select * from x_portal_user_role where user_id =loginID ) THEN		 		
		INSERT INTO x_portal_user_role(create_time,update_time,added_by_id,upd_by_id,user_id,user_role,status) VALUES (current_timestamp,current_timestamp,NULL,NULL,loginID,'ROLE_SYS_ADMIN',1);
	END IF;
	IF not exists (select * from x_user where user_name = 'rangertagsync') THEN
		INSERT INTO x_user(create_time,update_time,added_by_id,upd_by_id,user_name,descr,status) values (current_timestamp, current_timestamp,NULL,NULL,'rangertagsync','rangertagsync',0);
	END IF;
 END IF;
END;
$$ LANGUAGE plpgsql;
select create_tag_sync();
select 'delimiter end';