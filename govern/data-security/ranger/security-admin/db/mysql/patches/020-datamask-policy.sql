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

/* add datamask_options column in x_access_type_def table if not exist */
drop procedure if exists add_datamask_options_to_x_access_type_def_table;
delimiter ;;
 create procedure add_datamask_options_to_x_access_type_def_table() begin
 
 if exists (select * from information_schema.columns where table_schema=database() and table_name = 'x_access_type_def') then
	if not exists (select * from information_schema.columns where table_schema=database() and table_name = 'x_access_type_def' and column_name = 'datamask_options') then
		ALTER TABLE `x_access_type_def` ADD `datamask_options` varchar(1024) NULL DEFAULT NULL;
 	end if;
	if not exists (select * from information_schema.columns where table_schema=database() and table_name = 'x_access_type_def' and column_name = 'rowfilter_options') then
		ALTER TABLE `x_access_type_def` ADD `rowfilter_options` varchar(1024) NULL DEFAULT NULL;
 	end if;
 end if; 
end;;

delimiter ;
call add_datamask_options_to_x_access_type_def_table();
drop procedure if exists add_datamask_options_to_x_access_type_def_table;

/* add datamask_options column in x_resource_def table if not exist */
drop procedure if exists add_datamask_options_to_x_resource_def_table;
delimiter ;;
 create procedure add_datamask_options_to_x_resource_def_table() begin
 
 if exists (select * from information_schema.columns where table_schema=database() and table_name = 'x_resource_def') then
	if not exists (select * from information_schema.columns where table_schema=database() and table_name = 'x_resource_def' and column_name = 'datamask_options') then
		ALTER TABLE `x_resource_def` ADD `datamask_options` varchar(1024) NULL DEFAULT NULL;
 	end if;
	if not exists (select * from information_schema.columns where table_schema=database() and table_name = 'x_resource_def' and column_name = 'rowfilter_options') then
		ALTER TABLE `x_resource_def` ADD `rowfilter_options` varchar(1024) NULL DEFAULT NULL;
 	end if;
 end if; 
end;;

delimiter ;
call add_datamask_options_to_x_resource_def_table();
drop procedure if exists add_datamask_options_to_x_resource_def_table;

DROP TABLE IF EXISTS `x_policy_item_rowfilter`;
DROP TABLE IF EXISTS `x_policy_item_datamask`;
DROP TABLE IF EXISTS `x_datamask_type_def`;
CREATE TABLE `x_datamask_type_def` (
`id` bigint(20) NOT NULL AUTO_INCREMENT ,
`guid` varchar(64) NULL DEFAULT NULL,
`create_time` datetime NULL DEFAULT NULL,
`update_time` datetime NULL DEFAULT NULL,
`added_by_id` bigint(20) NULL DEFAULT NULL,
`upd_by_id` bigint(20) NULL DEFAULT NULL,
`def_id` bigint(20) NOT NULL,    
`item_id` bigint(20) NOT NULL,    
`name` varchar(1024) NOT NULL,
`label` varchar(1024) NOT NULL,
`description` varchar(1024) NULL DEFAULT NULL,
`transformer` varchar(1024) NULL DEFAULT NULL,
`datamask_options` varchar(1024) NULL DEFAULT NULL,
`rb_key_label` varchar(1024) NULL DEFAULT NULL,
`rb_key_description` varchar(1024) DEFAULT NULL,
`sort_order` int DEFAULT 0,
primary key (`id`),      
CONSTRAINT `x_datamask_type_def_FK_def_id` FOREIGN KEY (`def_id`) REFERENCES `x_service_def` (`id`) ,
CONSTRAINT `x_datamask_type_def_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_datamask_type_def_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;
CREATE INDEX x_datamask_type_def_IDX_def_id ON x_datamask_type_def(def_id);

CREATE TABLE `x_policy_item_datamask` (
`id` bigint(20) NOT NULL AUTO_INCREMENT ,
`guid` varchar(64) NULL DEFAULT NULL,
`create_time` datetime NULL DEFAULT NULL,
`update_time` datetime NULL DEFAULT NULL,
`added_by_id` bigint(20) NULL DEFAULT NULL,
`upd_by_id` bigint(20) NULL DEFAULT NULL,
`policy_item_id` bigint(20) NOT NULL, 
`type` bigint(20) NOT NULL,
`condition_expr` varchar(1024) NULL DEFAULT NULL,
`value_expr` varchar(1024) NULL DEFAULT NULL,
primary key (id), 
CONSTRAINT `x_policy_item_datamask_FK_policy_item_id` FOREIGN KEY (`policy_item_id`) REFERENCES `x_policy_item` (`id`) ,
CONSTRAINT `x_policy_item_datamask_FK_type` FOREIGN KEY (`type`) REFERENCES `x_datamask_type_def` (`id`),
CONSTRAINT `x_policy_item_datamask_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_policy_item_datamask_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;
CREATE INDEX x_policy_item_datamask_IDX_policy_item_id ON x_policy_item_datamask(policy_item_id);

CREATE TABLE `x_policy_item_rowfilter` (
`id` bigint(20) NOT NULL AUTO_INCREMENT ,
`guid` varchar(64) NULL DEFAULT NULL,
`create_time` datetime NULL DEFAULT NULL,
`update_time` datetime NULL DEFAULT NULL,
`added_by_id` bigint(20) NULL DEFAULT NULL,
`upd_by_id` bigint(20) NULL DEFAULT NULL,
`policy_item_id` bigint(20) NOT NULL, 
`filter_expr` varchar(1024) NULL DEFAULT NULL,
primary key (id), 
CONSTRAINT `x_policy_item_rowfilter_FK_policy_item_id` FOREIGN KEY (`policy_item_id`) REFERENCES `x_policy_item` (`id`) ,
CONSTRAINT `x_policy_item_rowfilter_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_policy_item_rowfilter_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;
CREATE INDEX x_policy_item_rowfilter_IDX_policy_item_id ON x_policy_item_rowfilter(policy_item_id);
