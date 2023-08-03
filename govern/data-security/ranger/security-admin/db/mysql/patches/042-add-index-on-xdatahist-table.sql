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

drop procedure if exists create_index_on_x_data_hist;

delimiter ;;
create procedure create_index_on_x_data_hist() begin
 /* check tables exist or not */
	if exists (select * from information_schema.columns where table_schema=database() and table_name = 'x_data_hist' and column_name in('obj_id', 'obj_class_type')) then
		/* check index exist on id and obj_class_type column or not */
		if not exists (select * from information_schema.statistics where table_schema=database() and table_name = 'x_data_hist' and column_name in('obj_id', 'obj_class_type'))  then
			ALTER TABLE x_data_hist ADD INDEX x_data_hist_idx_objid_objclstype(obj_id, obj_class_type);
		end if;
	end if;
end;;

delimiter ;
call create_index_on_x_data_hist();

drop procedure if exists create_index_on_x_data_hist;