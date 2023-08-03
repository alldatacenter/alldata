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

drop procedure if exists create_index_for_x_service_resource;

delimiter ;;
create procedure create_index_for_x_service_resource() begin
if exists (SELECT * FROM INFORMATION_SCHEMA.STATISTICS WHERE table_schema=DATABASE() AND table_name='x_service_resource' AND index_name='x_service_resource_IDX_resource_signature') then
	DROP INDEX x_service_resource_IDX_resource_signature on x_service_resource;
end if;
if not exists (SELECT * FROM INFORMATION_SCHEMA.STATISTICS WHERE table_schema=DATABASE() AND table_name='x_service_resource' AND index_name='x_service_resource_IDX_svc_id_resource_signature') then
	CREATE UNIQUE INDEX x_service_resource_IDX_svc_id_resource_signature ON x_service_resource(service_id, resource_signature);
end if;
end;;

delimiter ;
call create_index_for_x_service_resource();

