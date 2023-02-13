/*
 * Copyright (c) 2021 Terminus, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

CREATE TABLE `pipeline_triggers` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `event` varchar(191) NOT NULL DEFAULT '' COMMENT '触发事件',
  `pipeline_source` varchar(191) NOT NULL DEFAULT '' COMMENT '来源',
  `pipeline_yml_name` varchar(191) NOT NULL DEFAULT '' COMMENT '名称',
  `pipeline_definition_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '定义id',
  `filter` mediumtext NOT NULL COMMENT '过滤条件',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'CREATED AT',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'UPDATED AT',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='pipeline trigger table';
