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

package org.apache.inlong.sort.connector.kudu;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.types.DataType;
import org.apache.inlong.sort.kudu.common.KuduTableInfo;
import org.apache.inlong.sort.kudu.table.KuduDynamicTableFactory;
import org.apache.inlong.sort.kudu.table.KuduDynamicTableSink;
import org.apache.inlong.sort.kudu.table.KuduDynamicTableSource;
import org.apache.kudu.client.KuduException;
import org.apache.kudu.client.SessionConfiguration.FlushMode;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * The unit tests for {@link KuduDynamicTableFactory}.
 */
public class KuduTableFactoryTest extends KuduTestBase {

    public KuduTableFactoryTest() throws KuduException {
    }

    @Test
    public void testCreateSink() {

        Map<String, String> properties = getProperties();
        DynamicTableSink actualSink = createTableSink(testSchema, properties);

        KuduTableInfo kuduTableInfo = KuduTableInfo.builder()
                .masters(masters)
                .tableName(tableName)
                .fieldNames(testSchema.getColumnNames().toArray(new String[0]))
                .dataTypes(testSchema.getColumnDataTypes().toArray(new DataType[0]))
                .flushMode(FlushMode.AUTO_FLUSH_BACKGROUND)
                .build();
        DynamicTableSink expectedSink =
                new KuduDynamicTableSink(
                        kuduTableInfo,
                        new Configuration(),
                        null,
                        null);

        assertEquals(expectedSink, actualSink);
    }

    @Test
    public void testCreateLookup() {
        Map<String, String> properties = getProperties();

        DynamicTableSource actualSource = createTableSource(testSchema, properties);

        KuduTableInfo kuduTableInfo = KuduTableInfo.builder()
                .masters(masters)
                .tableName(tableName)
                .fieldNames(testSchema.getColumnNames().toArray(new String[0]))
                .dataTypes(testSchema.getColumnDataTypes().toArray(new DataType[0]))
                .flushMode(FlushMode.AUTO_FLUSH_BACKGROUND)
                .build();

        DynamicTableSource expectedSource =
                new KuduDynamicTableSource(
                        kuduTableInfo,
                        new Configuration(),
                        null,
                        null);

        assertEquals(expectedSource, actualSource);
    }

    private Map<String, String> getProperties() {
        Map<String, String> properties = new HashMap<>();

        properties.put("connector", "kudu-inlong");
        properties.put("connector.property-version", "1");
        properties.put("masters", masters);
        properties.put("table-name", tableName);

        properties.put("schema.0.name", "key");
        properties.put("schema.0.type", "STRING");
        properties.put("schema.1.name", "aaa");
        properties.put("schema.1.type", "STRING");
        properties.put("schema.2.name", "bbb");
        properties.put("schema.2.type", "DOUBLE");
        properties.put("schema.3.name", "ccc");
        properties.put("schema.3.type", "INT");

        properties.put("format.type", "csv");
        properties.put("format.property-version", "1");
        properties.put("format.derive-schema", "true");

        return properties;
    }

}
