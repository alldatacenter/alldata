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

drop procedure if exists modify_column_sort_order_to_int;

delimiter ;;
create procedure modify_column_sort_order_to_int(IN tableName varchar(64)) begin
	if exists (select * from information_schema.columns where table_schema=database() and table_name = tableName and column_name = 'sort_order' and data_type='tinyint') then
		SET @query = CONCAT('ALTER TABLE `', tableName,'` MODIFY COLUMN `sort_order` INT DEFAULT 0');
		PREPARE stmt FROM @query;
		EXECUTE stmt;
		DEALLOCATE PREPARE stmt;
	end if;
end;;

delimiter ;

call modify_column_sort_order_to_int('x_service_config_def');
call modify_column_sort_order_to_int('x_resource_def');
call modify_column_sort_order_to_int('x_access_type_def');
call modify_column_sort_order_to_int('x_policy_condition_def');
call modify_column_sort_order_to_int('x_context_enricher_def');
call modify_column_sort_order_to_int('x_enum_element_def');
call modify_column_sort_order_to_int('x_policy_resource_map');
call modify_column_sort_order_to_int('x_policy_item');
call modify_column_sort_order_to_int('x_policy_item_access');
call modify_column_sort_order_to_int('x_policy_item_condition');
call modify_column_sort_order_to_int('x_policy_item_user_perm');
call modify_column_sort_order_to_int('x_policy_item_group_perm');
call modify_column_sort_order_to_int('x_datamask_type_def');

drop procedure if exists modify_column_sort_order_to_int;
