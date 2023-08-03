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

drop procedure if exists add_is_visible_column_to_x_user_table;
delimiter ;;
 create procedure add_is_visible_column_to_x_user_table() begin
 
/* add is visible column in x_user table if not exist */
 if exists (select * from information_schema.columns where table_schema=database() and table_name = 'x_user') then
	if not exists (select * from information_schema.columns where table_schema=database() and table_name = 'x_user' and column_name = 'is_visible') then
		ALTER TABLE `x_user` ADD `is_visible` INT(11) NOT NULL DEFAULT '1';
 	end if;
 end if; 
end;;

delimiter ;

call add_is_visible_column_to_x_user_table();

drop procedure if exists add_is_visible_column_to_x_user_table;

drop procedure if exists add_is_visible_column_to_x_group_table;
delimiter ;;
 create procedure add_is_visible_column_to_x_group_table() begin
 
/* add is visible column in x_group table if not exist */
 if exists (select * from information_schema.columns where table_schema=database() and table_name = 'x_group') then
	if not exists (select * from information_schema.columns where table_schema=database() and table_name = 'x_group' and column_name = 'is_visible') then
		ALTER TABLE `x_group` ADD `is_visible` INT(11) NOT NULL DEFAULT '1';
 	end if;
 end if; 
end;;

delimiter ;

call add_is_visible_column_to_x_group_table();

drop procedure if exists add_is_visible_column_to_x_group_table;
