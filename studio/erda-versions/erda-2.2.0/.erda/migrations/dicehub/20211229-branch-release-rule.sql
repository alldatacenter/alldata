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

CREATE TABLE `erda_branch_release_rule`
(
    `id`              VARCHAR(36)  NOT NULL DEFAULT '' PRIMARY KEY COMMENT 'primary',
    `created_at`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '表记录创建时间',
    `updated_at`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '表记录更新时间',
    `project_id`      BIGINT(20)   NOT NULL DEFAULT 0 COMMENT '项目 ID',
    `pattern`         VARCHAR(191) NOT NULL DEFAULT '' COMMENT '分支模板, 如 release/*',
    `is_enabled`      BOOLEAN      NOT NULL DEFAULT true COMMENT '是否启用规则',
    `soft_deleted_at` BIGINT(20)   NOT NULL DEFAULT 0 COMMENT '逻辑删除, 0: 正常, 13位时间戳: 删除时间'
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4 COMMENT '分支 release 规则';