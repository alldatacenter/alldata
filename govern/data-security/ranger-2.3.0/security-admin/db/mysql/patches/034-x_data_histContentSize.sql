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

drop procedure if exists change_content_column_datatype_of_x_data_hist_table;

delimiter ;;
create procedure change_content_column_datatype_of_x_data_hist_table() begin

 /* change content data type to longtext if not exist */
 if exists (select * from information_schema.columns where table_schema=database() and table_name = 'x_data_hist' and column_name = 'content' and data_type='text') then
	ALTER TABLE  `x_data_hist` CHANGE `content` `content` MEDIUMTEXT NOT NULL;
 end if;

end;;

delimiter ;
call change_content_column_datatype_of_x_data_hist_table();

drop procedure if exists change_content_column_datatype_of_x_data_hist_table;
