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

drop procedure if exists add_column_in_x_service_def_and_x_service;

delimiter ;;
create procedure add_column_in_x_service_def_and_x_service() begin

if not exists (select * from information_schema.columns where table_schema=database() and table_name = 'x_service_def' and column_name='display_name') then
        ALTER TABLE x_service_def ADD display_name varchar(1024) DEFAULT NULL;
        UPDATE x_service_def SET display_name=name;
        UPDATE x_service_def SET display_name='Hadoop SQL' where name='hive';
end if;

if not exists (select * from information_schema.columns where table_schema=database() and table_name = 'x_service' and column_name='display_name') then
        ALTER TABLE x_service ADD display_name varchar(255) DEFAULT NULL;
        UPDATE x_service SET display_name = name;
end if;

end;;

delimiter ;
call add_column_in_x_service_def_and_x_service();

drop procedure if exists add_column_in_x_service_def_and_x_service;

-- User changes
drop procedure if exists add_column_in_x_user_and_x_portal_user_and_x_group;

delimiter ;;
create procedure add_column_in_x_user_and_x_portal_user_and_x_group() begin

if not exists (select * from information_schema.columns where table_schema=database() and table_name = 'x_user' and column_name='other_attributes') then
        ALTER TABLE x_user ADD other_attributes text DEFAULT NULL;
end if;

if not exists (select * from information_schema.columns where table_schema=database() and table_name = 'x_portal_user' and column_name='other_attributes') then
        ALTER TABLE x_portal_user ADD other_attributes text DEFAULT NULL;
end if;

if not exists (select * from information_schema.columns where table_schema=database() and table_name = 'x_group' and column_name='other_attributes') then
        ALTER TABLE x_group ADD other_attributes text DEFAULT NULL;
end if;

end;;

delimiter ;
call add_column_in_x_user_and_x_portal_user_and_x_group();

drop procedure if exists add_column_in_x_user_and_x_portal_user_and_x_group;