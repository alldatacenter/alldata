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

drop procedure if exists add_columns_to_support_audit_log_aggregation;

delimiter ;;
create procedure add_columns_to_support_audit_log_aggregation() begin

 if exists (select * from information_schema.columns where table_schema=database() and table_name = 'xa_access_audit') then
	if not exists (select * from information_schema.columns where table_schema=database() and table_name = 'xa_access_audit' and column_name = 'seq_num') then
		if not exists (select * from information_schema.columns where table_schema=database() and table_name = 'xa_access_audit' and column_name = 'event_count') then
			if not exists (select * from information_schema.columns where table_schema=database() and table_name = 'xa_access_audit' and column_name = 'event_dur_ms') then
				ALTER TABLE  `xa_access_audit` ADD  `seq_num` bigint NULL DEFAULT 0,ADD  `event_count` bigint NULL DEFAULT 1,ADD  `event_dur_ms` bigint NULL DEFAULT 1;
			end if;
		end if;
 	end if;
 end if;

end;;

delimiter ;
call add_columns_to_support_audit_log_aggregation();

drop procedure if exists add_columns_to_support_audit_log_aggregation;
