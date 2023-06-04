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

package org.apache.inlong.sort.base.dirty;

import org.apache.flink.formats.common.TimestampFormat;
import org.apache.flink.formats.json.JsonOptions.MapNullKeyMode;
import org.apache.flink.formats.json.RowDataToJsonConverters;
import org.apache.flink.formats.json.RowDataToJsonConverters.RowDataToJsonConverter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.catalog.Column;
import org.apache.flink.table.catalog.ResolvedSchema;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.RowData.FieldGetter;
import org.apache.flink.table.data.StringData;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.inlong.sort.base.dirty.utils.FormatUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.apache.flink.table.data.RowData.createFieldGetter;

/**
 * Test for {@link org.apache.inlong.sort.base.dirty.utils.FormatUtils}
 */
public class FormatUtilsTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode jsonNode;
    private RowData rowData;
    private FieldGetter[] fieldGetters;
    private Map<String, String> labelMap;
    private ResolvedSchema schema;

    @Before
    public void init() throws JsonProcessingException {
        schema = ResolvedSchema.of(
                Column.physical("id", DataTypes.BIGINT()),
                Column.physical("name", DataTypes.STRING()),
                Column.physical("age", DataTypes.INT()));
        List<LogicalType> logicalTypes = schema.toPhysicalRowDataType()
                .getLogicalType().getChildren();
        fieldGetters = new RowData.FieldGetter[logicalTypes.size()];
        for (int i = 0; i < logicalTypes.size(); i++) {
            fieldGetters[i] = createFieldGetter(logicalTypes.get(i), i);
        }
        rowData = GenericRowData.of(1L, StringData.fromString("leo"), 18);
        String jsonStr = "{\"id\":1,\"name\":\"leo\",\"age\":18}";
        jsonNode = mapper.readValue(jsonStr, JsonNode.class);
        labelMap = new HashMap<>();
        labelMap.put("database", "inlong");
        labelMap.put("table", "student");
    }

    @Test
    public void testCsvFormat() {
        String expected = "inlong,student,1,leo,18";
        Assert.assertEquals(expected, FormatUtils.csvFormat(jsonNode, labelMap, ","));
        Assert.assertEquals(expected, FormatUtils.csvFormat(rowData, fieldGetters, labelMap, ","));
        Assert.assertEquals(expected, FormatUtils.csvFormat("1,leo,18", labelMap, ","));
    }

    @Test
    public void testJsonFormat() throws JsonProcessingException {
        RowDataToJsonConverter converter = new RowDataToJsonConverters(TimestampFormat.SQL,
                MapNullKeyMode.DROP, null)
                        .createConverter(schema.toPhysicalRowDataType().getLogicalType());
        String expectedStr = "{\"database\":\"inlong\",\"table\":\"student\",\"id\":1,\"name\":\"leo\",\"age\":18}";
        JsonNode expected = mapper.readValue(expectedStr, JsonNode.class);
        String actual = FormatUtils.jsonFormat(jsonNode, labelMap);
        Assert.assertEquals(expected, mapper.readValue(actual, JsonNode.class));
        actual = FormatUtils.jsonFormat(rowData, converter, labelMap);
        Assert.assertEquals(expected, mapper.readValue(actual, JsonNode.class));
    }
}
