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

package org.apache.inlong.common.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * MaskDataUtils unit test
 */
public class MaskDataUtilsTest {

    @Test
    public void testMaskDataUtils() throws Exception {
        String unmasked = "{\n"
                + "  \"password\": \"inlong\",\n"
                + "  \"pwd\": \"inlong\",\n"
                + "  \"pass\": \"inlong\",\n"
                + "  \"token\": \"inlong\",\n"
                + "  \"secret_token\": \"inlong\",\n"
                + "  \"secretToken\": \"inlong\",\n"
                + "  \"secret_id\": \"inlong\",\n"
                + "  \"secretId\": \"inlong\",\n"
                + "  \"secret_key\": \"inlong\",\n"
                + "  \"secretKey\": \"inlong\",\n"
                + "  \"public_key\": \"inlong\",\n"
                + "  \"publicKey\": \"inlong\"\n"
                + "}";
        String masked = "{\n"
                + "  \"password\": \"******\",\n"
                + "  \"pwd\": \"******\",\n"
                + "  \"pass\": \"******\",\n"
                + "  \"token\": \"******\",\n"
                + "  \"secret_token\": \"******\",\n"
                + "  \"secretToken\": \"******\",\n"
                + "  \"secret_id\": \"******\",\n"
                + "  \"secretId\": \"******\",\n"
                + "  \"secret_key\": \"******\",\n"
                + "  \"secretKey\": \"******\",\n"
                + "  \"public_key\": \"******\",\n"
                + "  \"publicKey\": \"******\"\n"
                + "}";
        StringBuilder buffer = new StringBuilder(unmasked);
        MaskDataUtils.mask(buffer);
        assertEquals(masked, buffer.toString());
    }

    /**
     * Remove sensitive message in flink sql
     */
    @Test
    public void testMaskFlinkSql() {
        String unmasked = "CREATE TABLE `table_1`(\n"
                + "    PRIMARY KEY (`id`) NOT ENFORCED,\n"
                + "    `id` INT,\n"
                + "    `name` STRING,\n"
                + "    `age` INT)\n"
                + "    WITH (\n"
                + "    'inlong.metric.labels' = 'groupId=1&streamId=1&nodeId=1',\n"
                + "    'connector' = 'mysql-cdc-inlong',\n"
                + "    'hostname' = 'localhost',\n"
                + "    'database-name' = 'test',\n"
                + "    'port' = '3306',\n"
                + "    'server-id' = '10011',\n"
                + "    'scan.incremental.snapshot.enabled' = 'true',\n"
                + "    'username' = 'root',\n"
                + "    'password' = 'inlong',\n"
                + "    'table-name' = 'user'\n"
                + ")";

        String masked = "CREATE TABLE `table_1`(\n"
                + "    PRIMARY KEY (`id`) NOT ENFORCED,\n"
                + "    `id` INT,\n"
                + "    `name` STRING,\n"
                + "    `age` INT)\n"
                + "    WITH (\n"
                + "    'inlong.metric.labels' = 'groupId=1&streamId=1&nodeId=1',\n"
                + "    'connector' = 'mysql-cdc-inlong',\n"
                + "    'hostname' = 'localhost',\n"
                + "    'database-name' = 'test',\n"
                + "    'port' = '3306',\n"
                + "    'server-id' = '10011',\n"
                + "    'scan.incremental.snapshot.enabled' = 'true',\n"
                + "    'username' = 'root',\n"
                + "    'password' = '******',\n"
                + "    'table-name' = 'user'\n"
                + ")";
        StringBuilder buffer = new StringBuilder(unmasked);
        MaskDataUtils.mask(buffer);
        assertEquals(masked, buffer.toString());
    }

}
