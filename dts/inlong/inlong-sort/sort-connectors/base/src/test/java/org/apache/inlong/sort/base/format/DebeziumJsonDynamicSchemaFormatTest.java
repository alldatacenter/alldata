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
public class DebeziumJsonDynamicSchemaFormatTest extends DynamicSchemaFormatBaseTest<JsonNode> {

    private AbstractDynamicSchemaFormat schemaFormat = DynamicSchemaFormatFactory.getFormat("debezium-json");

    @Override
    protected String getSource() {
        return "{\n"
                + "  \"before\": {\n"
                + "    \"id\": 111,\n"
                + "    \"name\": \"scooter\",\n"
                + "    \"description\": \"Big 2-wheel scooter\",\n"
                + "    \"weight\": 5.18\n"
                + "  },\n"
                + "  \"after\": {\n"
                + "    \"id\": 111,\n"
                + "    \"name\": \"scooter\",\n"
                + "    \"description\": \"Big 2-wheel scooter\",\n"
                + "    \"weight\": 5.15\n"
                + "  },\n"
                + "  \"source\": {\"version\": \"0.9.5.Final\",\n"
                + "\"pkNames\":[\"id\", \"name\"],"
                + "\t\"connector\": \"mysql\",\n"
                + "\t\"name\": \"fullfillment\",\n"
                + "\t\"server_id\" :1,\n"
                + "\t\"ts_sec\": 1629607909,\n"
                + "\t\"gtid\": \"mysql-bin.000001\",\n"
                + "\t\"pos\": 2238,\"row\": 0,\n"
                + "\t\"snapshot\": false,\n"
                + "\t\"thread\": 7,\n"
                + "\t\"db\": \"inventory\",\n"
                + "\t\"table\": \"products\",\n"
                + "\t\"query\": null},\n"
                + "  \"op\": \"u\",\n"
                + "  \"ts_ms\": 1589362330904,\n"
                + "  \"transaction\": null\n"
                + "}";
    }

    @Override
    protected Map<String, String> getExpectedValues() {
        Map<String, String> expectedValues = new HashMap<>();
        expectedValues.put("${source.db}${source.table}", "inventoryproducts");
        expectedValues.put("${source.db}_${source.table}", "inventory_products");
        expectedValues.put("prefix_${source.db}_${source.table}_suffix", "prefix_inventory_products_suffix");
        expectedValues.put("${ \t source.db \t }${ source.table }", "inventoryproducts");
        expectedValues.put("${source.db}_${source.table}_${id}_${name}", "inventory_products_111_scooter");
        return expectedValues;
    }

    @Test
    @SuppressWarnings({"unchecked"})
    public void testExtractPrimaryKey() throws IOException {
        JsonNode rootNode = (JsonNode) getDynamicSchemaFormat()
                .deserialize(getSource().getBytes(StandardCharsets.UTF_8));
        List<String> primaryKeys = getDynamicSchemaFormat().extractPrimaryKeyNames(rootNode);
        List<String> values = getDynamicSchemaFormat().extractValues(rootNode, primaryKeys.toArray(new String[]{}));
        Assert.assertEquals(values, Arrays.asList("111", "scooter"));
    }

    @Test
    public void testExtractPhysicalData() throws IOException {
        JsonNode rootNode = (JsonNode) getDynamicSchemaFormat()
                .deserialize(getSource().getBytes(StandardCharsets.UTF_8));
        Assert.assertNotNull(((JsonDynamicSchemaFormat) getDynamicSchemaFormat()).getPhysicalData(rootNode));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    protected AbstractDynamicSchemaFormat getDynamicSchemaFormat() {
        return schemaFormat;
    }
}
