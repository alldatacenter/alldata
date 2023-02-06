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

CREATE TABLE `erda_code_coverage_setting` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `maven_setting` text NOT NULL COMMENT 'maven 的配置',
  `includes` varchar(1000) NOT NULL DEFAULT '' COMMENT '包含的package',
  `excludes` varchar(1000) NOT NULL DEFAULT '' COMMENT '排除的package',
  `project_id` bigint(20) NOT NULL COMMENT '项目的id',
  `workspace` varchar(100) NOT NULL DEFAULT '' COMMENT '关联环境',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COMMENT='代码覆盖率配置表';


ALTER TABLE dice_code_coverage_exec_record ADD `workspace` varchar(100) NOT NULL DEFAULT '' COMMENT '关联环境';
