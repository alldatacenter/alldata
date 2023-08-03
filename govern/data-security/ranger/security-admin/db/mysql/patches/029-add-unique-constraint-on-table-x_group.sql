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

drop procedure if exists create_unique_constraint_on_groupname;

delimiter ;;
create procedure create_unique_constraint_on_groupname() begin
DECLARE loginID bigint(20);
 /* check tables exist or not */
        if exists (select * from information_schema.columns where table_schema=database() and table_name = 'x_group' and column_name='group_name') then
                /* check unique constraint exist on group_name column or not */
                if not exists (select * from information_schema.columns where table_schema=database() and table_name = 'x_group' and column_name='group_name' and column_key='UNI') then
                        if not exists (select * from information_schema.table_constraints where table_schema=database() and table_name = 'x_group' and constraint_name='x_group_UK_group_name') then
                                ALTER TABLE x_group ADD UNIQUE INDEX x_group_UK_group_name(group_name(767));
--	 			ALTER TABLE x_group MODIFY COLUMN group_name varchar(767) NOT NULL, ADD CONSTRAINT x_group_UK_group_name UNIQUE(group_name(767));
                        end if;
                end if;
        end if;
        if exists (select * from information_schema.columns where table_schema=database() and table_name = 'x_group_users' and column_name='group_name') then
                if exists (select * from information_schema.columns where table_schema=database() and table_name = 'x_group_users' and column_name='user_id') then
                /* check unique constraint exist on group_name column or not */
                        if not exists (select * from information_schema.table_constraints where table_schema=database() and table_name = 'x_group_users' and constraint_name='x_group_users_UK_uid_gname') then
                                ALTER TABLE x_group_users ADD UNIQUE INDEX x_group_users_UK_uid_gname(user_id,group_name(740));
-- 				ALTER TABLE x_group_users MODIFY COLUMN group_name varchar(767), ADD CONSTRAINT x_group_users_UK_uid_gname UNIQUE(user_id,group_name(767));
                        end if;
                end if;
        end if;
end;;

delimiter ;
call create_unique_constraint_on_groupname();

drop procedure if exists create_unique_constraint_on_groupname;
