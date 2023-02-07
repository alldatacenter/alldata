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

package org.apache.inlong.sort.formats.inlongmsg;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.formats.csv.CsvRowDataDeserializationSchema;
import org.apache.flink.formats.csv.CsvRowSchemaConverter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.apache.flink.table.catalog.ResolvedSchema;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.factories.TestDynamicTableFactory;
import org.apache.flink.table.runtime.connector.sink.SinkRuntimeProviderContext;
import org.apache.flink.table.runtime.connector.source.ScanRuntimeProviderContext;
import org.apache.flink.table.runtime.typeutils.InternalTypeInfo;
import org.apache.flink.table.types.logical.RowType;
import org.apache.inlong.sort.formats.inlongmsg.InLongMsgDeserializationSchema.MetadataConverter;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.apache.flink.table.factories.utils.FactoryMocks.PHYSICAL_TYPE;
import static org.apache.flink.table.factories.utils.FactoryMocks.SCHEMA;
import static org.apache.flink.table.factories.utils.FactoryMocks.createTableSink;
import static org.apache.flink.table.factories.utils.FactoryMocks.createTableSource;
import static org.junit.Assert.assertEquals;

public class InLongMsgFormatFactoryTest {

    @Test
    public void testUserDefinedOptions()
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        final Map<String, String> tableOptions =
                getModifiedOptions(opts -> {
                    opts.put("inlong-msg.inner.format", "csv");
                    opts.put("inlong-msg.ignore-parse-errors", "true");
                });

        // test Deser
        Constructor[] constructors = CsvRowDataDeserializationSchema.class.getDeclaredConstructors();
        Constructor constructor = CsvRowDataDeserializationSchema.class.getDeclaredConstructor(
                RowType.class, TypeInformation.class, CsvSchema.class, boolean.class);
        constructor.setAccessible(true);
        DeserializationSchema<RowData> deserializationSchema = (DeserializationSchema<RowData>) constructor.newInstance(
                PHYSICAL_TYPE, InternalTypeInfo.of(PHYSICAL_TYPE),
                CsvRowSchemaConverter.convert(PHYSICAL_TYPE), false);

        InLongMsgDeserializationSchema expectedDeser = new InLongMsgDeserializationSchema(
                deserializationSchema, new MetadataConverter[0], InternalTypeInfo.of(PHYSICAL_TYPE), true);
        DeserializationSchema<RowData> actualDeser = createDeserializationSchema(tableOptions, SCHEMA);
        assertEquals(expectedDeser, actualDeser);
    }

    // ------------------------------------------------------------------------
    // Public Tools
    // ------------------------------------------------------------------------
    public static DeserializationSchema<RowData> createDeserializationSchema(
            Map<String, String> options, ResolvedSchema schema) {
        DynamicTableSource source = createTableSource(schema, options);

        assert source instanceof TestDynamicTableFactory.DynamicTableSourceMock;
        TestDynamicTableFactory.DynamicTableSourceMock scanSourceMock =
                (TestDynamicTableFactory.DynamicTableSourceMock) source;

        return scanSourceMock.valueFormat.createRuntimeDecoder(
                ScanRuntimeProviderContext.INSTANCE, schema.toPhysicalRowDataType());
    }

    public static SerializationSchema<RowData> createSerializationSchema(
            Map<String, String> options, ResolvedSchema schema) {
        DynamicTableSink sink = createTableSink(schema, options);

        assert sink instanceof TestDynamicTableFactory.DynamicTableSinkMock;
        TestDynamicTableFactory.DynamicTableSinkMock sinkMock =
                (TestDynamicTableFactory.DynamicTableSinkMock) sink;

        return sinkMock.valueFormat.createRuntimeEncoder(
                new SinkRuntimeProviderContext(false), schema.toPhysicalRowDataType());
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
        options.put("format", "inlong-msg");
        return options;
    }

}
