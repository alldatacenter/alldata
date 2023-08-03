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

drop procedure if exists change_column_datatype_in_x_user_and_x_portal_user_and_x_group;

delimiter ;;
create procedure change_column_datatype_in_x_user_and_x_portal_user_and_x_group() begin

if exists (select * from information_schema.columns where table_schema=database() and table_name = 'x_portal_user' and column_name in('notes', 'other_attributes', 'sync_source') and data_type='varchar') then
    ALTER TABLE x_portal_user MODIFY notes TEXT DEFAULT NULL, MODIFY other_attributes TEXT DEFAULT NULL, MODIFY sync_source TEXT DEFAULT NULL;
end if;

if exists (select * from information_schema.columns where table_schema=database() and table_name = 'x_user' and column_name in('descr', 'other_attributes', 'sync_source') and data_type='varchar') then
    ALTER TABLE x_user MODIFY descr TEXT DEFAULT NULL, MODIFY other_attributes TEXT DEFAULT NULL, MODIFY sync_source TEXT DEFAULT NULL;
end if;

if exists (select * from information_schema.columns where table_schema=database() and table_name = 'x_group' and column_name in('descr', 'other_attributes', 'sync_source') and data_type='varchar') then
    ALTER TABLE x_group MODIFY descr TEXT DEFAULT NULL, MODIFY other_attributes TEXT DEFAULT NULL, MODIFY sync_source TEXT DEFAULT NULL;
end if;

end;;

delimiter ;
call change_column_datatype_in_x_user_and_x_portal_user_and_x_group();

drop procedure if exists change_column_datatype_in_x_user_and_x_portal_user_and_x_group;