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
package io.datavines.engine.local.connector;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MySQLSink extends BaseJdbcSink {

    @Override
    protected String getExecutionResultTableSql() {

        return "CREATE TABLE `dv_job_execution_result` (\n" +
                "    `id` bigint(20) NOT NULL AUTO_INCREMENT,\n" +
                "    `job_execution_id` bigint(20) DEFAULT NULL,\n" +
                "    `metric_unique_key` varchar(255) DEFAULT NULL,\n" +
                "    `metric_type` varchar(255) DEFAULT NULL,\n" +
                "    `metric_dimension` varchar(255) DEFAULT NULL,\n" +
                "    `metric_name` varchar(255) DEFAULT NULL,\n" +
                "    `database_name` varchar(255) DEFAULT NULL,\n" +
                "    `table_name` varchar(255) DEFAULT NULL,\n" +
                "    `column_name` varchar(255) DEFAULT NULL,\n" +
                "    `actual_value` double DEFAULT NULL,\n" +
                "    `expected_value` double DEFAULT NULL,\n" +
                "    `expected_type` varchar(255) DEFAULT NULL,\n" +
                "    `result_formula` varchar(255) DEFAULT NULL,\n" +
                "    `operator` varchar(255) DEFAULT NULL,\n" +
                "    `threshold` double DEFAULT NULL,\n" +
                "    `state` int(2) NOT NULL DEFAULT 0,\n" +
                "    `create_time` datetime DEFAULT NULL,\n" +
                "    `update_time` datetime DEFAULT NULL,\n" +
                "    PRIMARY KEY (`id`)\n" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
    }

    @Override
    protected String getActualValueTableSql() {
        return "CREATE TABLE `dv_actual_values` (\n" +
                "    `id` bigint(20) NOT NULL AUTO_INCREMENT,\n" +
                "    `job_execution_id` bigint(20) DEFAULT NULL,\n" +
                "    `metric_name` varchar(255) DEFAULT NULL,\n" +
                "    `unique_code` varchar(255) DEFAULT NULL,\n" +
                "    `actual_value` double DEFAULT NULL,\n" +
                "    `data_time` datetime DEFAULT NULL,\n" +
                "    `create_time` datetime DEFAULT NULL,\n" +
                "    `update_time` datetime DEFAULT NULL,\n" +
                "    PRIMARY KEY (`id`)\n" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
    }

    @Override
    protected String getProfileValueTableSql() {
        return "CREATE TABLE `dv_catalog_entity_profile` (\n" +
                "  `id` bigint(20) NOT NULL AUTO_INCREMENT,\n" +
                "  `entity_uuid` varchar(64) NOT NULL,\n" +
                "  `metric_name` varchar(255) NOT NULL,\n" +
                "  `actual_value` text NOT NULL,\n" +
                "  `actual_value_type` varchar(255) DEFAULT NULL,\n" +
                "  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ,\n" +
                "  PRIMARY KEY (`id`),\n" +
                "  UNIQUE KEY `dv_entity_definition_un` (`entity_uuid`)\n" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;\n";
    }
}
