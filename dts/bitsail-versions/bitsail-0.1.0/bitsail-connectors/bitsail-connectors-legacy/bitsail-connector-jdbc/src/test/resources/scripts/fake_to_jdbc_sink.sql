/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

use test;

CREATE TABLE `jdbc_dynamic_table`
(
    `id`             bigint unsigned NOT NULL AUTO_INCREMENT,
    `tinyint_type`   int                                      NOT NULL DEFAULT '0',
    `smallint_type`  int                                      NOT NULL DEFAULT '0',
    `mediumint_type` int                                      NOT NULL DEFAULT '0',
    `int_type`       int                                      NOT NULL DEFAULT '0',
    `bigint_type`    bigint                                            DEFAULT '0',
    `timestamp_type` varchar(128) COLLATE utf8mb4_general_ci           DEFAULT NULL,
    `datetime_type`  varchar(128) COLLATE utf8mb4_general_ci           DEFAULT NULL,
    `float_type`     decimal(10, 4)                                    DEFAULT NULL,
    `double_type`    decimal(20, 4)                                    DEFAULT NULL,
    `date_type`      varchar(128) COLLATE utf8mb4_general_ci           DEFAULT NULL,
    `time_type`      varchar(128) COLLATE utf8mb4_general_ci           DEFAULT NULL,
    `year_type`      varchar(128) COLLATE utf8mb4_general_ci           DEFAULT NULL,
    `char_type`      varchar(10) COLLATE utf8mb4_general_ci   NOT NULL DEFAULT '',
    `varchar_type`   varchar(1024) COLLATE utf8mb4_general_ci          DEFAULT '',
    `text_type`      varchar(1024) COLLATE utf8mb4_general_ci          DEFAULT '',
    `longtext_type`  varchar(1024) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
    `blob_type`      varchar(1024) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
    `binary_type`    varchar(10) COLLATE utf8mb4_general_ci   NOT NULL DEFAULT '',
    `datetime`       date                                     NOT NULL COMMENT 'date',
    `tinyint_test`   tinyint unsigned DEFAULT NULL,
    PRIMARY KEY (`id`, `datetime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;