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
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.binary.BinaryStringData;
import org.apache.flink.types.RowKind;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test for {@link CanalJsonDynamicSchemaFormat}
 */
public class CanalJsonDynamicSchemaFormatTest extends DynamicSchemaFormatBaseTest<JsonNode> {

    private AbstractDynamicSchemaFormat schemaFormat = DynamicSchemaFormatFactory.getFormat("canal-json");

    @Override
    protected String getSource() {
        return "{\n"
                + "  \"data\": [\n"
                + "    {\n"
                + "      \"id\": 111,\n"
                + "      \"name\": \"scooter\",\n"
                + "      \"description\": \"Big 2-wheel scooter\",\n"
                + "      \"weight\": \"5.18\"\n"
                + "    }\n"
                + "  ],\n"
                + "  \"database\": \"inventory\",\n"
                + "  \"es\": 1589373560000,\n"
                + "  \"id\": 9,\n"
                + "  \"isDdl\": false,\n"
                + "  \"mysqlType\": {\n"
                + "    \"id\": \"INTEGER\",\n"
                + "    \"name\": \"VARCHAR(255)\",\n"
                + "    \"description\": \"VARCHAR(512)\",\n"
                + "    \"weight\": \"FLOAT\"\n"
                + "  },\n"
                + "  \"old\": [\n"
                + "    {\n"
                + "      \"weight\": \"5.15\"\n"
                + "    }\n"
                + "  ],\n"
                + "  \"pkNames\": [\n"
                + "    \"id\"\n"
                + "  ],\n"
                + "  \"sql\": \"\",\n"
                + "  \"sqlType\": {\n"
                + "    \"id\": 4,\n"
                + "    \"name\": 12,\n"
                + "    \"description\": 12,\n"
                + "    \"weight\": 7\n"
                + "  },\n"
                + "  \"table\": \"products\",\n"
                + "  \"ts\": 1589373560798,\n"
                + "  \"type\": \"UPDATE\"\n"
                + "}";
    }

    @Override
    protected Map<String, String> getExpectedValues() {
        Map<String, String> expectedValues = new HashMap<>();
        expectedValues.put("${database}${table}", "inventoryproducts");
        expectedValues.put("${database}_${table}", "inventory_products");
        expectedValues.put("prefix_${database}_${table}_suffix", "prefix_inventory_products_suffix");
        expectedValues.put("${ \t database \t }${ table }", "inventoryproducts");
        expectedValues.put("${database}_${table}_${id}_${name}", "inventory_products_111_scooter");
        return expectedValues;
    }

    @Override
    protected AbstractDynamicSchemaFormat<JsonNode> getDynamicSchemaFormat() {
        return schemaFormat;
    }

    @Test
    @SuppressWarnings({"unchecked"})
    public void testExtractPrimaryKey() throws IOException {
        JsonNode rootNode = (JsonNode) getDynamicSchemaFormat()
                .deserialize(getSource().getBytes(StandardCharsets.UTF_8));
        List<String> primaryKeys = getDynamicSchemaFormat().extractPrimaryKeyNames(rootNode);
        List<String> values = getDynamicSchemaFormat().extractValues(rootNode, primaryKeys.toArray(new String[]{}));
        Assert.assertEquals(values, Collections.singletonList("111"));
    }

    @Test
    public void testExtractRowData() throws IOException {
        JsonNode rootNode = (JsonNode) getDynamicSchemaFormat()
                .deserialize(getSource().getBytes(StandardCharsets.UTF_8));
        List<RowData> values = getDynamicSchemaFormat().extractRowData(rootNode);
        List<RowData> rowDataList = new ArrayList<>();
        rowDataList.add(GenericRowData.ofKind(RowKind.UPDATE_BEFORE,
                111,
                BinaryStringData.fromString("scooter"),
                BinaryStringData.fromString("Big 2-wheel scooter"),
                5.15f));
        rowDataList.add(GenericRowData.ofKind(RowKind.UPDATE_AFTER,
                111,
                BinaryStringData.fromString("scooter"),
                BinaryStringData.fromString("Big 2-wheel scooter"),
                5.18f));
        Assert.assertEquals(values, rowDataList);
    }
}
