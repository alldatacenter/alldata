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

drop procedure if exists create_unique_constraint_on_guid_service;

delimiter ;;
create procedure create_unique_constraint_on_guid_service() begin
 /* check table and columns exist or not */
	if exists (select * from information_schema.columns where table_schema=database() and table_name = 'x_policy' and column_name in('guid','service','zone_id')) then
		/* check unique constraint exist on guid and service column or not */
		if not exists (select * from information_schema.table_constraints where table_schema=database() and table_name = 'x_policy' and constraint_name='x_policy_UK_guid_service_zone') then
			ALTER TABLE x_policy ADD UNIQUE INDEX x_policy_UK_guid_service_zone(guid(180),service,zone_id);
		end if;
	end if;
end;;

delimiter ;
call create_unique_constraint_on_guid_service();

drop procedure if exists create_unique_constraint_on_guid_service;