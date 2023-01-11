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

IF EXISTS(select * from INFORMATION_SCHEMA.columns where table_name = 'x_portal_user')
BEGIN
    IF EXISTS(select * from INFORMATION_SCHEMA.columns where table_name = 'x_portal_user_role')
    BEGIN
        IF EXISTS(select * from INFORMATION_SCHEMA.columns where table_name = 'x_user')
        BEGIN
            IF NOT EXISTS(select * from x_portal_user where login_id = 'rangertagsync')
            BEGIN
				INSERT INTO x_portal_user(create_time,update_time,added_by_id,upd_by_id,first_name,last_name,pub_scr_name,login_id,password,email,status,user_src,notes) VALUES (CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,NULL,NULL,'rangertagsync','','rangertagsync','rangertagsync','f5820e1229418dcf2575908f2c493da5','rangertagsync',1,0,NULL);
            END
            DECLARE @loginID bigint
            set @loginID=(select id from x_portal_user where login_id = 'rangertagsync')
            IF NOT EXISTS (select * from x_portal_user_role where user_id =@loginID )
            BEGIN
				INSERT INTO x_portal_user_role(create_time,update_time,added_by_id,upd_by_id,user_id,user_role,status) VALUES (CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,NULL,NULL,@loginID,'ROLE_SYS_ADMIN',1);
            END
            IF NOT EXISTS (select * from x_user where user_name = 'rangertagsync')
            BEGIN
				INSERT INTO x_user(create_time,update_time,added_by_id,upd_by_id,user_name,descr,status) values (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,NULL,NULL,'rangertagsync','rangertagsync',0);
            END
        END
    END
    IF EXISTS(select * from INFORMATION_SCHEMA.CONSTRAINT_COLUMN_USAGE where constraint_name = 'x_portal_user$x_portal_user_UK_email')
    BEGIN
        ALTER TABLE x_portal_user DROP CONSTRAINT x_portal_user$x_portal_user_UK_email;
    END
END
GO
exit
