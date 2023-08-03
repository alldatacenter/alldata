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

DROP TABLE IF EXISTS `x_rms_notification`;
DROP TABLE IF EXISTS `x_rms_resource_mapping`;
DROP TABLE IF EXISTS `x_rms_mapping_provider`;
DROP TABLE IF EXISTS `x_rms_service_resource`;

CREATE TABLE `x_rms_service_resource` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `guid` varchar(64) NOT NULL,
  `create_time` timestamp NULL DEFAULT NULL,
  `update_time` timestamp NULL DEFAULT NULL,
  `added_by_id` bigint(20) DEFAULT NULL,
  `upd_by_id` bigint(20) DEFAULT NULL,
  `version` bigint(20) DEFAULT NULL,
  `service_id` bigint(20) NOT NULL,
  `resource_signature` varchar(128) DEFAULT NULL,
  `is_enabled` tinyint(1) NOT NULL DEFAULT '1',
  `service_resource_elements_text` text,
  PRIMARY KEY (`id`),
  UNIQUE KEY `x_rms_service_res_UK_guid` (`guid`),
  CONSTRAINT `x_rms_service_res_FK_service_id` FOREIGN KEY (`service_id`) REFERENCES `x_service` (`id`)
);
CREATE INDEX x_rms_service_resource_IDX_service_id ON x_rms_service_resource(service_id);

CREATE TABLE `x_rms_notification` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `hms_name` varchar(128) DEFAULT NULL,
  `notification_id` bigint(20) DEFAULT NULL,
  `change_timestamp` timestamp NULL DEFAULT NULL,
  `change_type` varchar(64) DEFAULT NULL,
  `hl_resource_id` bigint(20) DEFAULT NULL,
  `hl_service_id` bigint(20) DEFAULT NULL,
  `ll_resource_id` bigint(20) DEFAULT NULL,
  `ll_service_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `x_rms_notification_FK_hl_service_id` FOREIGN KEY (`hl_service_id`) REFERENCES `x_service` (`id`),
  CONSTRAINT `x_rms_notification_FK_ll_service_id` FOREIGN KEY (`ll_service_id`) REFERENCES `x_service` (`id`)
);

CREATE INDEX x_rms_notification_IDX_notification_id ON x_rms_notification(notification_id);
CREATE INDEX x_rms_notification_IDX_hms_name_notification_id ON x_rms_notification(hms_name, notification_id);
CREATE INDEX x_rms_notification_IDX_hl_service_id ON x_rms_notification(hl_service_id);
CREATE INDEX x_rms_notification_IDX_ll_service_id ON x_rms_notification(ll_service_id);


CREATE TABLE `x_rms_resource_mapping` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `change_timestamp` timestamp NULL DEFAULT NULL,
  `hl_resource_id` bigint(20) NOT NULL,
  `ll_resource_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `x_rms_res_map_UK_hl_res_id_ll_res_id` (`hl_resource_id`,`ll_resource_id`),
  CONSTRAINT `x_rms_res_map_FK_hl_res_id` FOREIGN KEY (`hl_resource_id`) REFERENCES `x_rms_service_resource` (`id`),
  CONSTRAINT `x_rms_res_map_FK_ll_res_id` FOREIGN KEY (`ll_resource_id`) REFERENCES `x_rms_service_resource` (`id`)
);

CREATE INDEX x_rms_resource_mapping_IDX_hl_resource_id ON x_rms_resource_mapping(hl_resource_id);
CREATE INDEX x_rms_resource_mapping_IDX_ll_resource_id ON x_rms_resource_mapping(ll_resource_id);

CREATE TABLE `x_rms_mapping_provider` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `change_timestamp` timestamp NULL DEFAULT NULL,
  `name` varchar(128) NOT NULL,
  `last_known_version` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `x_rms_mapping_provider_UK_name` (`name`)
);
