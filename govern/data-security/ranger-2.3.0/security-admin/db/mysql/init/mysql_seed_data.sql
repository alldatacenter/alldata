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

insert into x_portal_user (
       CREATE_TIME, UPDATE_TIME, 
       FIRST_NAME, LAST_NAME, PUB_SCR_NAME, 
       LOGIN_ID, PASSWORD, EMAIL, STATUS
) values (
  	 now(), now(), 
	 'Admin', '', 'Admin', 
	 'admin', 'ceb4f32325eda6142bd65215f4c0f371', '', 1
);
SET @user_id:= last_insert_id();

insert into x_portal_user_role (
       CREATE_TIME, UPDATE_TIME, 
       USER_ID, USER_ROLE, STATUS
) values (
  	 now(), now(), 
	 @user_id, 'ROLE_SYS_ADMIN', 1
);
SET @user_role_id:= last_insert_id();



DROP TABLE IF EXISTS `vx_trx_log`;
DROP VIEW IF EXISTS `vx_trx_log`;
CREATE VIEW `vx_trx_log` AS select `x_trx_log`.`id` AS `id`,`x_trx_log`.`create_time` AS `create_time`,`x_trx_log`.`update_time` AS `update_time`,`x_trx_log`.`added_by_id` AS `added_by_id`,`x_trx_log`.`upd_by_id` AS `upd_by_id`,`x_trx_log`.`class_type` AS `class_type`,`x_trx_log`.`object_id` AS `object_id`,`x_trx_log`.`parent_object_id` AS `parent_object_id`,`x_trx_log`.`parent_object_class_type` AS `parent_object_class_type`,`x_trx_log`.`attr_name` AS `attr_name`,`x_trx_log`.`parent_object_name` AS `parent_object_name`,`x_trx_log`.`object_name` AS `object_name`,`x_trx_log`.`prev_val` AS `prev_val`,`x_trx_log`.`new_val` AS `new_val`,`x_trx_log`.`trx_id` AS `trx_id`,`x_trx_log`.`action` AS `action`,`x_trx_log`.`sess_id` AS `sess_id`,`x_trx_log`.`req_id` AS `req_id`,`x_trx_log`.`sess_type` AS `sess_type` from `x_trx_log` group by `x_trx_log`.`trx_id`
