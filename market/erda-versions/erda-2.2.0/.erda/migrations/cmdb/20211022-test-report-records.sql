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

CREATE TABLE `erda_test_report_records`
(
    `id`            bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `created_at`    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `project_id`    bigint(20) NOT NULL COMMENT '项目ID',
    `name`          varchar(255)   NOT NULL COMMENT '报告名称',
    `summary`       varchar(2000)  NOT NULL DEFAULT "" COMMENT '测试总结',
    `iteration_id`  bigint(20) NOT NULL COMMENT '所属迭代ID',
    `creator_id`    varchar(255)   NOT NULL COMMENT '创建者ID',
    `quality_score` decimal(65, 2) NOT NULL DEFAULT 0.00 COMMENT '总体质量分,保留两位小数',
    `report_data`   longtext       NOT NULL COMMENT '测试和事项的报告',
    PRIMARY KEY (`id`),
    KEY             `idx_project_id` (`project_id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4 COMMENT ='事项和测试的测试报告记录';