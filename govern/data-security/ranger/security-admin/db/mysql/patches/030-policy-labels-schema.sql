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

DROP TABLE IF EXISTS `x_policy_label_map`;
DROP TABLE IF EXISTS `x_policy_label`;
CREATE TABLE  `x_policy_label` (
`id` bigint(20) NOT NULL AUTO_INCREMENT ,
`guid` varchar(1024) DEFAULT NULL,
`create_time` datetime DEFAULT NULL,
`update_time` datetime DEFAULT NULL,
`added_by_id` bigint(20) DEFAULT NULL,
`upd_by_id` bigint(20) DEFAULT NULL,
`label_name` varchar(512) DEFAULT NULL,
primary key (`id`),
UNIQUE KEY `x_policy_label_UK_label_name` (`label_name`),
KEY `x_policy_label_added_by_id` (`added_by_id`),
KEY `x_policy_label_upd_by_id` (`upd_by_id`),
KEY `x_policy_label_cr_time` (`create_time`),
KEY `x_policy_label_up_time` (`update_time`),
KEY `x_policy_label_name` (`label_name`),
CONSTRAINT `x_policy_label_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_policy_label_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`)
)ROW_FORMAT=DYNAMIC;


CREATE TABLE  `x_policy_label_map` (
`id` bigint(20) NOT NULL AUTO_INCREMENT ,
`guid` varchar(1024) DEFAULT NULL,
`create_time` datetime DEFAULT NULL,
`update_time` datetime DEFAULT NULL,
`added_by_id` bigint(20) DEFAULT NULL,
`upd_by_id` bigint(20) DEFAULT NULL,
`policy_id` bigint(20) DEFAULT NULL,
`policy_label_id` bigint(20) DEFAULT NULL,
primary key (`id`),
UNIQUE INDEX `x_policy_label_map_pid_plid` (`policy_id`, `policy_label_id`),
KEY `x_policy_label_map_added_by_id` (`added_by_id`),
KEY `x_policy_label_map_upd_by_id` (`upd_by_id`),
KEY `x_policy_label_map_cr_time` (`create_time`),
KEY `x_policy_label_map_up_time` (`update_time`),
CONSTRAINT `x_policy_label_map_FK_added_by_id` FOREIGN KEY (`added_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_policy_label_map_FK_upd_by_id` FOREIGN KEY (`upd_by_id`) REFERENCES `x_portal_user` (`id`),
CONSTRAINT `x_policy_label_map_FK_policy_id` FOREIGN KEY (`policy_id`) REFERENCES `x_policy` (`id`),
CONSTRAINT `x_policy_label_map_FK_policy_label_id` FOREIGN KEY (`policy_label_id`) REFERENCES `x_policy_label` (`id`)
)ROW_FORMAT=DYNAMIC;

CREATE INDEX x_policy_label_label_id ON x_policy_label(id);
CREATE INDEX x_policy_label_label_name ON x_policy_label(label_name);
CREATE INDEX x_policy_label_label_map_id ON x_policy_label_map(id);
