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

DECLARE
	v_count number:=0;
	loginID number:=0;
	sql_stmt VARCHAR2(1000);
	first_name VARCHAR2(20):='rangerusersync';
	scr_name VARCHAR2(20):='rangerusersync';
	login_name VARCHAR2(20):='rangerusersync';
	password VARCHAR2(50):='70b8374d3dfe0325aaa5002a688c7e3b';
	user_role VARCHAR2(50):='ROLE_SYS_ADMIN';
	email VARCHAR2(20):='rangerusersync';
BEGIN
  	select count(*) into v_count from user_tables where table_name IN('X_PORTAL_USER','X_PORTAL_USER_ROLE','X_USER');
  	if (v_count = 3) then
  		v_count:=0;
		select count(*) into v_count from x_portal_user where login_id = login_name;
		if (v_count = 0) then
			sql_stmt := 'INSERT INTO x_portal_user(ID,CREATE_TIME,UPDATE_TIME,FIRST_NAME,LAST_NAME,PUB_SCR_NAME,LOGIN_ID,PASSWORD,EMAIL,STATUS,USER_SRC) VALUES (X_PORTAL_USER_SEQ.nextval,sys_extract_utc(systimestamp),sys_extract_utc(systimestamp),:1,NULL,:2,:3,:4,:5,1,0)';
			EXECUTE IMMEDIATE sql_stmt USING first_name,scr_name,login_name,password,email;
			commit;
		end if;
		select id into loginID from x_portal_user where login_id = login_name;
		if (loginID > 0) then
			sql_stmt := 'INSERT INTO x_portal_user_role(id,create_time,update_time,user_id,user_role,status) VALUES (X_PORTAL_USER_ROLE_SEQ.nextval,sys_extract_utc(systimestamp),sys_extract_utc(systimestamp),:1,:2,1)';
			EXECUTE IMMEDIATE sql_stmt USING loginID,user_role;
			commit;
		end if;
		v_count:=0;
		select count(*) into v_count from x_user where user_name = login_name;
		if (v_count = 0) then
			sql_stmt := 'INSERT INTO x_user(id,create_time,update_time,user_name,descr,status) values (X_USER_SEQ.nextval,sys_extract_utc(systimestamp),sys_extract_utc(systimestamp),:1,:2,0)';
			EXECUTE IMMEDIATE sql_stmt USING login_name,login_name;
			commit;
		end if;
	end if;
end;/