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

drop procedure if exists create_index_for_x_rms_service_resource;

delimiter ;;
create procedure create_index_for_x_rms_service_resource() begin
if not exists (SELECT * FROM INFORMATION_SCHEMA.STATISTICS WHERE table_schema=DATABASE() AND table_name='x_rms_service_resource' AND index_name='x_rms_service_resource_IDX_resource_signature') then
	CREATE INDEX x_rms_service_resource_IDX_resource_signature ON x_rms_service_resource(resource_signature);
 end if;
end;;

delimiter ;
call create_index_for_x_rms_service_resource();

drop procedure if exists delete_rms_tables;
delimiter ;;
create procedure delete_rms_tables() begin
	SET FOREIGN_KEY_CHECKS = 0;
	truncate table x_rms_mapping_provider;
	truncate table x_rms_notification;
	truncate table x_rms_resource_mapping;
	truncate table x_rms_service_resource;
	SET FOREIGN_KEY_CHECKS = 1;
end;;

delimiter ;
call delete_rms_tables();
