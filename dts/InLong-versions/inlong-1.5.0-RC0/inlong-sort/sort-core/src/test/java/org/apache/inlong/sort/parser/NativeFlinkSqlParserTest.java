/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.parser;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.inlong.sort.parser.impl.NativeFlinkSqlParser;
import org.apache.inlong.sort.parser.result.ParseResult;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test for {@link NativeFlinkSqlParser}
 */
public class NativeFlinkSqlParserTest {

    @Test
    public void testNativeFlinkSqlParser() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.enableCheckpointing(10000);
        env.disableOperatorChaining();
        EnvironmentSettings settings = EnvironmentSettings
                .newInstance()
                .useBlinkPlanner()
                .inStreamingMode()
                .build();
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, settings);
        String data = "  CREATe TABLE `table_1`(\n"
                + "    `age` INT,\n"
                + "    `name` STRING)\n"
                + "    WITH (\n"
                + "    'connector' = 'mysql-cdc-inlong',\n"
                + "    'hostname' = 'localhost',\n"
                + "    'username' = 'root',\n"
                + "    'password' = 'inlong',\n"
                + "    'database-name' = 'test',\n"
                + "    'scan.incremental.snapshot.enabled' = 'false',\n"
                + "    'server-time-zone' = 'GMT+8',\n"
                + "    'table-name' = 'user',\n"
                + "    'port' = '3306'\n"
                + ");\n"
                + "CREATE TABLE `table_2`(\n"
                + "    PRIMARY KEY (`name`,`age`) NOT ENFORCED,\n"
                + "    `name` STRING,\n"
                + "    `age` INT)\n"
                + "    WITH (\n"
                + "    'connector' = 'jdbc',\n"
                + "    'url' = 'jdbc:postgresql://localhost:5432/postgres',\n"
                + "    'username' = 'postgres',\n"
                + "    'password' = 'inlong',\n"
                + "    'table-name' = 'public.user'\n"
                + ");\n"
                + "INSERT INTO `table_2` \n"
                + "    SELECT \n"
                + "    `name` AS `name`,\n"
                + "    `age` AS `age`\n"
                + "    FROM `table_1`;\n";
        NativeFlinkSqlParser parser = NativeFlinkSqlParser.getInstance(tableEnv, data);
        ParseResult result = parser.parse();
        Assert.assertTrue(result.tryExecute());
    }

}
