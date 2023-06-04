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

package org.apache.inlong.sort.formats.json.canal;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.formats.common.TimestampFormat;
import org.apache.flink.formats.json.JsonOptions;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.factories.TestDynamicTableFactory;
import org.apache.flink.table.runtime.connector.sink.SinkRuntimeProviderContext;
import org.apache.flink.table.runtime.connector.source.ScanRuntimeProviderContext;
import org.apache.flink.table.runtime.typeutils.InternalTypeInfo;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.apache.flink.table.factories.utils.FactoryMocks.PHYSICAL_DATA_TYPE;
import static org.apache.flink.table.factories.utils.FactoryMocks.PHYSICAL_TYPE;
import static org.apache.flink.table.factories.utils.FactoryMocks.SCHEMA;
import static org.apache.flink.table.factories.utils.FactoryMocks.createTableSink;
import static org.apache.flink.table.factories.utils.FactoryMocks.createTableSource;
import static org.junit.Assert.assertEquals;

public class CanalJsonEnhancedFormatFactoryTest {

    private static final InternalTypeInfo<RowData> ROW_TYPE_INFO =
            InternalTypeInfo.of(PHYSICAL_TYPE);

    @Test
    public void testUserDefinedOptions() {
        final Map<String, String> tableOptions =
                getModifiedOptions(opts -> {
                    opts.put("canal-json-inlong.map-null-key.mode", "LITERAL");
                    opts.put("canal-json-inlong.map-null-key.literal", "nullKey");
                    opts.put("canal-json-inlong.ignore-parse-errors", "true");
                    opts.put("canal-json-inlong.timestamp-format.standard", "ISO-8601");
                    opts.put("canal-json-inlong.database.include", "mydb");
                    opts.put("canal-json-inlong.table.include", "mytable");
                    opts.put("canal-json-inlong.map-null-key.mode", "LITERAL");
                    opts.put("canal-json-inlong.map-null-key.literal", "nullKey");
                    opts.put("canal-json-inlong.encode.decimal-as-plain-number", "true");
                });

        // test Deser
        CanalJsonEnhancedDeserializationSchema expectedDeser =
                CanalJsonEnhancedDeserializationSchema.builder(
                        PHYSICAL_DATA_TYPE, Collections.emptyList(), ROW_TYPE_INFO)
                        .setIgnoreParseErrors(true)
                        .setTimestampFormat(TimestampFormat.ISO_8601)
                        .setDatabase("mydb")
                        .setTable("mytable")
                        .build();
        DeserializationSchema<RowData> actualDeser = createDeserializationSchema(tableOptions);
        assertEquals(expectedDeser, actualDeser);

        // test Ser
        CanalJsonEnhancedSerializationSchema expectedSer =
                new CanalJsonEnhancedSerializationSchema(
                        PHYSICAL_DATA_TYPE,
                        new ArrayList<>(),
                        TimestampFormat.ISO_8601,
                        JsonOptions.MapNullKeyMode.LITERAL,
                        "nullKey",
                        true);
        SerializationSchema<RowData> actualSer = createSerializationSchema(tableOptions);
        assertEquals(expectedSer, actualSer);
    }

    // ------------------------------------------------------------------------
    // Public Tools
    // ------------------------------------------------------------------------

    public static DeserializationSchema<RowData> createDeserializationSchema(
            Map<String, String> options) {
        DynamicTableSource source = createTableSource(SCHEMA, options);

        assert source instanceof TestDynamicTableFactory.DynamicTableSourceMock;
        TestDynamicTableFactory.DynamicTableSourceMock scanSourceMock =
                (TestDynamicTableFactory.DynamicTableSourceMock) source;

        return scanSourceMock.valueFormat.createRuntimeDecoder(
                ScanRuntimeProviderContext.INSTANCE, PHYSICAL_DATA_TYPE);
    }

    public static SerializationSchema<RowData> createSerializationSchema(
            Map<String, String> options) {
        DynamicTableSink sink = createTableSink(SCHEMA, options);

        assert sink instanceof TestDynamicTableFactory.DynamicTableSinkMock;
        TestDynamicTableFactory.DynamicTableSinkMock sinkMock =
                (TestDynamicTableFactory.DynamicTableSinkMock) sink;

        return sinkMock.valueFormat.createRuntimeEncoder(
                new SinkRuntimeProviderContext(false), PHYSICAL_DATA_TYPE);
    }

    /**
     * Returns the full options modified by the given consumer {@code optionModifier}.
     *
     * @param optionModifier Consumer to modify the options
     */
    public static Map<String, String> getModifiedOptions(Consumer<Map<String, String>> optionModifier) {
        Map<String, String> options = getAllOptions();
        optionModifier.accept(options);
        return options;
    }

    private static Map<String, String> getAllOptions() {
        final Map<String, String> options = new HashMap<>();
        options.put("connector", TestDynamicTableFactory.IDENTIFIER);
        options.put("target", "MyTarget");
        options.put("buffer-size", "1000");
        options.put("format", "canal-json-inlong");
        return options;
    }
}
