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

drop procedure if exists increase_column_size_of_xa_access_audit_table;

delimiter ;;
create procedure increase_column_size_of_xa_access_audit_table() begin

 /* change request_data and resource_path column size from 2000 to 4000 */
 if exists (select * from information_schema.columns where table_schema=database() and table_name = 'xa_access_audit' and (column_name = 'request_data' or column_name = 'resource_path') and data_type='varchar' and CHARACTER_MAXIMUM_LENGTH=2000) then
	ALTER TABLE  `xa_access_audit` CHANGE  `request_data`  `request_data` VARCHAR(4000) NULL DEFAULT NULL,CHANGE  `resource_path`  `resource_path` VARCHAR(4000) NULL DEFAULT NULL;
 end if;

end;;

delimiter ;
call increase_column_size_of_xa_access_audit_table();

drop procedure if exists increase_column_size_of_xa_access_audit_table;