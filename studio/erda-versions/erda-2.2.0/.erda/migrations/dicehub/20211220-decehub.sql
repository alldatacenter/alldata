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

ALTER table `dice_release`
    ADD COLUMN `changelog`                text NOT NULL COMMENT 'Changelog',
    ADD COLUMN `is_stable`                tinyint(1) NOT NULL DEFAULT 0 COMMENT 'stable表示非临时制品',
    ADD COLUMN `is_formal`                tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否为正式制品',
    ADD COLUMN `is_project_release`       tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否为项目制品',
    ADD COLUMN `application_release_list` text NOT NULL COMMENT '依赖的应用制品ID列表',
    ADD COLUMN `tags`                     varchar(100) DEFAULT NULL COMMENT 'Tag'