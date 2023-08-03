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

drop procedure if exists create_unique_constraint_on_username;

delimiter ;;
create procedure create_unique_constraint_on_username() begin
DECLARE loginID bigint(20);
 /* check tables exist or not */
	if exists (select * from information_schema.columns where table_schema=database() and table_name = 'x_user' and column_name='user_name') then
 		/* check unique constraint exist on user_name column or not */
	 	if not exists (select * from information_schema.columns where table_schema=database() and table_name = 'x_user' and column_name='user_name' and column_key='UNI') then
	 		if not exists (select * from information_schema.table_constraints where table_schema=database() and table_name = 'x_user' and constraint_name='x_user_UK_user_name') then
	 			ALTER TABLE x_user MODIFY COLUMN user_name varchar(767) NOT NULL, ADD CONSTRAINT x_user_UK_user_name UNIQUE(user_name(767)); 
	 		end if;
	 	end if;
	end if;
end;;

delimiter ;
call create_unique_constraint_on_username();

drop procedure if exists create_unique_constraint_on_username;
