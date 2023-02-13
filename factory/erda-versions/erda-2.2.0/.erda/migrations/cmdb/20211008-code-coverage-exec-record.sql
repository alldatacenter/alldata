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

CREATE TABLE `dice_code_coverage_exec_record`
(
    `id`             bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `project_id`     bigint(20) NOT NULL COMMENT '项目ID',
    `status`         varchar(128)   NOT NULL COMMENT 'running,ready,ending,success,fail,cancel',
    `report_status`  varchar(128)   NOT NULL COMMENT 'running,success,fail,cancel',
    `coverage`       decimal(65, 2) NOT NULL DEFAULT 0.00 COMMENT '行覆盖率',
    `report_url`     varchar(255)   NOT NULL DEFAULT "" COMMENT '报告下载地址',
    `report_content` longtext       NOT NULL COMMENT '报告分析内容',
    `start_executor` varchar(255)   NOT NULL COMMENT '开始执行者',
    `end_executor`   varchar(255)   NOT NULL DEFAULT "" COMMENT '结束执行者',
    `time_begin`     datetime       NOT NULL COMMENT '开始时间',
    `time_end`       datetime       NOT NULL DEFAULT '1000-01-01 00:00:00' COMMENT '结束时间',
    `created_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `msg`            varchar(2000)  NOT NULL DEFAULT "" COMMENT '日志信息',
    `report_msg`     varchar(2000)  NOT NULL DEFAULT "" COMMENT '报告日志信息',
    `report_time`    datetime       NOT NULL DEFAULT '1000-01-01 00:00:00' COMMENT '报告生成时间',
    PRIMARY KEY (`id`),
    KEY              `idx_project_id` (`project_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='代码覆盖率执行记录';

