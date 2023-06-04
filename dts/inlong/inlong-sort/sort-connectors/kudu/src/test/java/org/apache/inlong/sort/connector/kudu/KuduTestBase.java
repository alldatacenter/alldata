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
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.catalog.CatalogTable;
import org.apache.flink.table.catalog.Column;
import org.apache.flink.table.catalog.ObjectIdentifier;
import org.apache.flink.table.catalog.ResolvedCatalogTable;
import org.apache.flink.table.catalog.ResolvedSchema;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.factories.FactoryUtil;
import org.apache.flink.table.types.DataType;
import org.apache.inlong.sort.formats.common.DoubleFormatInfo;
import org.apache.inlong.sort.formats.common.FormatInfo;
import org.apache.inlong.sort.formats.common.IntFormatInfo;
import org.apache.inlong.sort.formats.common.RowFormatInfo;
import org.apache.inlong.sort.formats.common.StringFormatInfo;
import org.apache.kudu.ColumnSchema;
import org.apache.kudu.Schema;
import org.apache.kudu.Type;
import org.junit.BeforeClass;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.apache.flink.table.api.config.ExecutionConfigOptions.TABLE_EXEC_RESOURCE_DEFAULT_PARALLELISM;

/**
 * The test base for kudu.
 */
public class KuduTestBase {

    protected static String masters = "localhost:8080,localhost:8081";
    protected static String tableName = "demo_table";

    protected static ResolvedSchema testSchema =
            ResolvedSchema.of(
                    Column.physical("key", DataTypes.STRING()),
                    Column.physical("aaa", DataTypes.STRING()),
                    Column.physical("bbb", DataTypes.DOUBLE()),
                    Column.physical("ccc", DataTypes.INT()));

    protected static RowFormatInfo rowFormatInfo = new RowFormatInfo(
            new String[]{"key", "aaa", "bbb", "ccc"},
            new FormatInfo[]{
                    StringFormatInfo.INSTANCE,
                    StringFormatInfo.INSTANCE,
                    DoubleFormatInfo.INSTANCE,
                    IntFormatInfo.INSTANCE
            });

    protected static StreamTableEnvironment tableEnv;
    protected static StreamExecutionEnvironment env;

    @BeforeClass
    public static void init() throws Exception {
        env = StreamExecutionEnvironment.getExecutionEnvironment().setParallelism(1);
        EnvironmentSettings settings = EnvironmentSettings.newInstance().inStreamingMode().build();
        tableEnv = StreamTableEnvironment.create(env, settings);
        tableEnv.getConfig().getConfiguration()
                .setInteger(TABLE_EXEC_RESOURCE_DEFAULT_PARALLELISM.key(), 1);
    }

    private static Schema toKuduSchema(TableSchema testSchema) {
        final List<ColumnSchema> cols = new ArrayList<>(testSchema.getFieldCount());
        for (int i = 0; i < testSchema.getFieldCount(); i++) {
            final Optional<String> fieldName = testSchema.getFieldName(i);
            final Optional<DataType> fieldDataType = testSchema.getFieldDataType(i);
            final DataType dataType = fieldDataType.get();
            Type columnType;
            if (DataTypes.STRING().equals(dataType)) {
                columnType = Type.STRING;
            } else if (DataTypes.DOUBLE().equals(dataType)) {
                columnType = Type.DOUBLE;
            } else if (DataTypes.INT().equals(dataType)) {
                columnType = Type.INT32;
            } else {
                throw new UnsupportedOperationException("unsupport dataType: " + dataType + ".");
            }
            final ColumnSchema columnSchema = new ColumnSchema.ColumnSchemaBuilder(fieldName.get(), columnType).build();
            cols.add(columnSchema);
        }
        return new Schema(cols);
    }

    private final ObjectIdentifier IDENTIFIER =
            ObjectIdentifier.of("default", "default", "t1");

    public DynamicTableSource createTableSource(
            ResolvedSchema schema, Map<String, String> options) {
        return FactoryUtil.createTableSource(
                null,
                IDENTIFIER,
                new ResolvedCatalogTable(
                        CatalogTable.of(
                                org.apache.flink.table.api.Schema.newBuilder().fromResolvedSchema(schema).build(),
                                "mock source",
                                Collections.emptyList(),
                                options),
                        schema),
                new Configuration(),
                KuduTestBase.class.getClassLoader(),
                false);
    }

    public DynamicTableSink createTableSink(
            ResolvedSchema schema, Map<String, String> options) {
        return createTableSink(schema, Collections.emptyList(), options);
    }

    public DynamicTableSink createTableSink(
            ResolvedSchema schema, List<String> partitionKeys, Map<String, String> options) {
        return FactoryUtil.createTableSink(
                null,
                IDENTIFIER,
                new ResolvedCatalogTable(
                        CatalogTable.of(
                                org.apache.flink.table.api.Schema.newBuilder().fromResolvedSchema(schema).build(),
                                "mock sink",
                                partitionKeys,
                                options),
                        schema),
                new Configuration(),
                KuduTestBase.class.getClassLoader(),
                false);
    }
}
