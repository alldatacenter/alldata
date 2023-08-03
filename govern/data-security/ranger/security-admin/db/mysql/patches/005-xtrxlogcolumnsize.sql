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

drop procedure if exists change_values_columns_datatype_of_x_trx_log_table;

delimiter ;;
create procedure change_values_columns_datatype_of_x_trx_log_table() begin

 /* change prev_value column data type to mediumtext */
 if exists (select * from information_schema.columns where table_schema=database() and table_name = 'x_trx_log' and column_name = 'prev_val' and data_type='varchar') then
 	ALTER TABLE  `x_trx_log` CHANGE  `prev_val`  `prev_val` MEDIUMTEXT NULL DEFAULT NULL ;
 end if;
 if exists (select * from information_schema.columns where table_schema=database() and table_name = 'x_trx_log' and column_name = 'new_val'  and data_type='varchar') then
 	ALTER TABLE  `x_trx_log` CHANGE  `new_val`  `new_val` MEDIUMTEXT NULL DEFAULT NULL ;
 end if;
  
end;;

delimiter ;
call change_values_columns_datatype_of_x_trx_log_table();

drop procedure if exists change_values_columns_datatype_of_x_trx_log_table;
