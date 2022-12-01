/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.platform;


import com.platform.explainer.lineage.LineageBuilder;
import com.platform.explainer.lineage.LineageResult;

/**
 * Lineage Build
 * @author AllDataDC
 * @date 2022/12/1
 */
public class FlinkLineageBuild {


    public static void buildLineage() {
        String sql = "CREATE TEMPORARY TABLE users (\n" +
                "  user_id BIGINT,\n" +
                "  user_name STRING,\n" +
                "  user_level STRING,\n" +
                "  region STRING,\n" +
                "  PRIMARY KEY (user_id) NOT ENFORCED\n" +
                ") WITH (\n" +
                "  'connector' = 'upsert-kafka',\n" +
                "  'topic' = 'users',\n" +
                "  'properties.bootstrap.servers' = '...',\n" +
                "  'key.format' = 'csv',\n" +
                "  'value.format' = 'avro'\n" +
                ");\n" +
                "\n" +
                "-- set sync mode\n" +
                "SET 'table.dml-sync' = 'true';\n" +
                "\n" +
                "-- set the job name\n" +
                "SET 'pipeline.name' = 'SqlJob';\n" +
                "\n" +
                "-- set the queue that the job submit to\n" +
                "SET 'yarn.application.queue' = 'root';\n" +
                "\n" +
                "-- set the job parallelism\n" +
                "SET 'parallelism.default' = '100';\n" +
                "\n" +
                "-- restore from the specific savepoint path\n" +
                "SET 'execution.savepoint.path' = '/tmp/flink-savepoints/savepoint-cca7bc-bb1e257f0dab';\n" +
                "\n" +
                "INSERT INTO pageviews_enriched\n" +
                "SELECT *\n" +
                "FROM pageviews AS p\n" +
                "LEFT JOIN users FOR SYSTEM_TIME AS OF p.proctime AS u\n" +
                "ON p.user_id = u.user_id;";
        LineageResult result = LineageBuilder.getLineage(sql);
        System.out.println("end");
    }

    public static void main(String[] args) {
        buildLineage();
    }
}
