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

package org.apache.inlong.sort.base.format;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.table.types.logical.IntType;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.table.types.logical.VarCharType;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test for {@link DebeziumJsonDynamicSchemaFormat}
 */
public class DebeziumJsonDynamicSchemaFormatWithSchemaTest extends DynamicSchemaFormatBaseTest<JsonNode> {

    private AbstractDynamicSchemaFormat schemaFormat = DynamicSchemaFormatFactory.getFormat("debezium-json");

    @Override
    protected String getSource() {
        return "{\n"
                + "  \"schema\": { \n"
                + "    \"type\": \"struct\",\n"
                + "    \"fields\": [\n"
                + "      {\n"
                + "        \"type\": \"struct\",\n"
                + "        \"fields\": [\n"
                + "          {\n"
                + "            \"type\": \"int32\",\n"
                + "            \"optional\": false,\n"
                + "            \"field\": \"id\"\n"
                + "          },\n"
                + "          {\n"
                + "            \"type\": \"string\",\n"
                + "            \"optional\": false,\n"
                + "            \"field\": \"first_name\"\n"
                + "          },\n"
                + "          {\n"
                + "            \"type\": \"string\",\n"
                + "            \"optional\": false,\n"
                + "            \"field\": \"last_name\"\n"
                + "          },\n"
                + "          {\n"
                + "            \"type\": \"string\",\n"
                + "            \"optional\": false,\n"
                + "            \"field\": \"email\"\n"
                + "          }\n"
                + "        ],\n"
                + "        \"optional\": true,\n"
                + "        \"name\": \"mysql-server-1.inventory.customers.Value\", \n"
                + "        \"field\": \"before\"\n"
                + "      },\n"
                + "      {\n"
                + "        \"type\": \"struct\",\n"
                + "        \"fields\": [\n"
                + "          {\n"
                + "            \"type\": \"int32\",\n"
                + "            \"optional\": false,\n"
                + "            \"field\": \"id\"\n"
                + "          },\n"
                + "          {\n"
                + "            \"type\": \"string\",\n"
                + "            \"optional\": false,\n"
                + "            \"field\": \"first_name\"\n"
                + "          },\n"
                + "          {\n"
                + "            \"type\": \"string\",\n"
                + "            \"optional\": false,\n"
                + "            \"field\": \"last_name\"\n"
                + "          },\n"
                + "          {\n"
                + "            \"type\": \"string\",\n"
                + "            \"optional\": false,\n"
                + "            \"field\": \"email\"\n"
                + "          }\n"
                + "        ],\n"
                + "        \"optional\": true,\n"
                + "        \"name\": \"mysql-server-1.inventory.customers.Value\",\n"
                + "        \"field\": \"after\"\n"
                + "      },\n"
                + "      {\n"
                + "        \"type\": \"struct\",\n"
                + "        \"fields\": [\n"
                + "          {\n"
                + "            \"type\": \"string\",\n"
                + "            \"optional\": false,\n"
                + "            \"field\": \"version\"\n"
                + "          },\n"
                + "          {\n"
                + "            \"type\": \"string\",\n"
                + "            \"optional\": false,\n"
                + "            \"field\": \"connector\"\n"
                + "          },\n"
                + "          {\n"
                + "            \"type\": \"string\",\n"
                + "            \"optional\": false,\n"
                + "            \"field\": \"name\"\n"
                + "          },\n"
                + "          {\n"
                + "            \"type\": \"int64\",\n"
                + "            \"optional\": false,\n"
                + "            \"field\": \"ts_ms\"\n"
                + "          },\n"
                + "          {\n"
                + "            \"type\": \"boolean\",\n"
                + "            \"optional\": true,\n"
                + "            \"default\": false,\n"
                + "            \"field\": \"snapshot\"\n"
                + "          },\n"
                + "          {\n"
                + "            \"type\": \"string\",\n"
                + "            \"optional\": false,\n"
                + "            \"field\": \"db\"\n"
                + "          },\n"
                + "          {\n"
                + "            \"type\": \"string\",\n"
                + "            \"optional\": true,\n"
                + "            \"field\": \"table\"\n"
                + "          },\n"
                + "          {\n"
                + "            \"type\": \"int64\",\n"
                + "            \"optional\": false,\n"
                + "            \"field\": \"server_id\"\n"
                + "          },\n"
                + "          {\n"
                + "            \"type\": \"string\",\n"
                + "            \"optional\": true,\n"
                + "            \"field\": \"gtid\"\n"
                + "          },\n"
                + "          {\n"
                + "            \"type\": \"string\",\n"
                + "            \"optional\": false,\n"
                + "            \"field\": \"file\"\n"
                + "          },\n"
                + "          {\n"
                + "            \"type\": \"int64\",\n"
                + "            \"optional\": false,\n"
                + "            \"field\": \"pos\"\n"
                + "          },\n"
                + "          {\n"
                + "            \"type\": \"int32\",\n"
                + "            \"optional\": false,\n"
                + "            \"field\": \"row\"\n"
                + "          },\n"
                + "          {\n"
                + "            \"type\": \"int64\",\n"
                + "            \"optional\": true,\n"
                + "            \"field\": \"thread\"\n"
                + "          },\n"
                + "          {\n"
                + "            \"type\": \"string\",\n"
                + "            \"optional\": true,\n"
                + "            \"field\": \"query\"\n"
                + "          }\n"
                + "        ],\n"
                + "        \"optional\": false,\n"
                + "        \"name\": \"io.debezium.connector.mysql.Source\", \n"
                + "        \"field\": \"source\"\n"
                + "      },\n"
                + "      {\n"
                + "        \"type\": \"string\",\n"
                + "        \"optional\": false,\n"
                + "        \"field\": \"op\"\n"
                + "      },\n"
                + "      {\n"
                + "        \"type\": \"int64\",\n"
                + "        \"optional\": true,\n"
                + "        \"field\": \"ts_ms\"\n"
                + "      }\n"
                + "    ],\n"
                + "    \"optional\": false,\n"
                + "    \"name\": \"mysql-server-1.inventory.customers.Envelope\" \n"
                + "  },\n"
                + "  \"payload\": { \n"
                + "    \"op\": \"c\", \n"
                + "    \"ts_ms\": 1465491411815, \n"
                + "    \"before\": null, \n"
                + "    \"after\": { \n"
                + "      \"id\": 1004,\n"
                + "      \"first_name\": \"Anne\",\n"
                + "      \"last_name\": \"Kretchmar\",\n"
                + "      \"email\": \"annek@noanswer.org\"\n"
                + "    },\n"
                + "    \"source\": { \n"
                + "      \"version\": \"1.9.6.Final\",\n"
                + "      \"connector\": \"mysql\",\n"
                + "      \"name\": \"mysql-server-1\",\n"
                + "      \"ts_ms\": 0,\n"
                + "      \"pkNames\":[\"id\", \"first_name\"],"
                + "      \"snapshot\": false,\n"
                + "      \"db\": \"inventory\",\n"
                + "      \"table\": \"customers\",\n"
                + "      \"server_id\": 0,\n"
                + "      \"gtid\": null,\n"
                + "      \"file\": \"mysql-bin.000003\",\n"
                + "      \"pos\": 154,\n"
                + "      \"row\": 0,\n"
                + "      \"thread\": 7,\n"
                + "      \"query\": \"INSERT INTO customers (first_name, last_name, email)"
                + " VALUES ('Anne', 'Kretchmar', 'annek@noanswer.org')\"\n"
                + "    }\n"
                + "  }\n"
                + "}";
    }

    @Override
    protected Map<String, String> getExpectedValues() {
        Map<String, String> expectedValues = new HashMap<>();
        expectedValues.put("${source.db}${source.table}", "inventorycustomers");
        expectedValues.put("${source.db}_${source.table}", "inventory_customers");
        expectedValues.put("prefix_${source.db}_${source.table}_suffix", "prefix_inventory_customers_suffix");
        expectedValues.put("${ \t source.db \t }${ source.table }", "inventorycustomers");
        expectedValues.put("${source.db}_${source.table}_${id}_${first_name}", "inventory_customers_1004_Anne");
        return expectedValues;
    }

    @Test
    @SuppressWarnings({"unchecked"})
    public void testExtractPrimaryKey() throws IOException {
        JsonNode rootNode = (JsonNode) getDynamicSchemaFormat()
                .deserialize(getSource().getBytes(StandardCharsets.UTF_8));
        List<String> primaryKeys = getDynamicSchemaFormat().extractPrimaryKeyNames(rootNode);
        List<String> values = getDynamicSchemaFormat().extractValues(rootNode, primaryKeys.toArray(new String[]{}));
        Assert.assertEquals(values, Arrays.asList("1004", "Anne"));
    }

    @Test
    public void testExtractPhysicalData() throws IOException {
        JsonNode rootNode = (JsonNode) getDynamicSchemaFormat()
                .deserialize(getSource().getBytes(StandardCharsets.UTF_8));
        Assert.assertNotNull(((JsonDynamicSchemaFormat) getDynamicSchemaFormat()).getPhysicalData(rootNode));
    }

    @Test
    public void testExtractRowType() throws IOException {
        JsonNode rootNode = (JsonNode) getDynamicSchemaFormat()
                .deserialize(getSource().getBytes(StandardCharsets.UTF_8));
        String[] names = new String[]{"id", "first_name", "last_name", "email"};
        LogicalType[] types = new LogicalType[]{
                new IntType(false),
                new VarCharType(false, 1),
                new VarCharType(),
                new VarCharType()
        };
        RowType rowType = RowType.of(true, types, names);
        Assert.assertEquals(getDynamicSchemaFormat().extractSchema(rootNode), rowType);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    protected AbstractDynamicSchemaFormat getDynamicSchemaFormat() {
        return schemaFormat;
    }
}
