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

import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.formats.common.TimestampFormat;
import org.apache.flink.formats.json.JsonOptions;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.connector.format.EncodingFormat;
import org.apache.flink.table.connector.sink.DynamicTableSink.Context;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.DataType;
import org.apache.flink.types.RowKind;
import org.apache.inlong.sort.formats.json.canal.CanalJsonEnhancedSerializationSchema.MetadataConverter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * {@link org.apache.flink.table.connector.format.EncodingFormat} for Canal using JSON encoding.
 *
 * different from flink:1.13.5. This can apply metadata, sink metadata into canal format
 */
public class CanalJsonEnhancedEncodingFormat implements EncodingFormat<SerializationSchema<RowData>> {

    private final TimestampFormat timestampFormat;
    private final JsonOptions.MapNullKeyMode mapNullKeyMode;
    private final String mapNullKeyLiteral;
    private List<String> metadataKeys;
    private boolean encodeDecimalAsPlainNumber;

    public CanalJsonEnhancedEncodingFormat(
            TimestampFormat timestampFormat,
            JsonOptions.MapNullKeyMode mapNullKeyMode,
            String mapNullKeyLiteral,
            boolean encodeDecimalAsPlainNumber) {
        this.timestampFormat = timestampFormat;
        this.mapNullKeyMode = mapNullKeyMode;
        this.mapNullKeyLiteral = mapNullKeyLiteral;
        this.encodeDecimalAsPlainNumber = encodeDecimalAsPlainNumber;
        this.metadataKeys = Collections.emptyList();
    }

    @Override
    public SerializationSchema<RowData> createRuntimeEncoder(Context context, DataType physicalDataType) {
        final List<WriteableMetadata> writeableMetadata =
                metadataKeys.stream()
                        .map(
                                k -> Stream.of(WriteableMetadata.values())
                                        .filter(rm -> rm.key.equals(k))
                                        .findFirst()
                                        .orElseThrow(IllegalStateException::new))
                        .collect(Collectors.toList());
        return new CanalJsonEnhancedSerializationSchema(
                physicalDataType,
                writeableMetadata,
                timestampFormat,
                mapNullKeyMode,
                mapNullKeyLiteral,
                encodeDecimalAsPlainNumber);
    }

    @Override
    public Map<String, DataType> listWritableMetadata() {
        return Arrays.stream(WriteableMetadata.values())
                .collect(
                        Collectors.toMap(m -> m.key, m -> m.dataType));
    }

    @Override
    public void applyWritableMetadata(List<String> metadataKeys) {
        this.metadataKeys = metadataKeys;
    }

    @Override
    public ChangelogMode getChangelogMode() {
        return ChangelogMode.newBuilder()
                .addContainedKind(RowKind.INSERT)
                .addContainedKind(RowKind.UPDATE_BEFORE)
                .addContainedKind(RowKind.UPDATE_AFTER)
                .addContainedKind(RowKind.DELETE)
                .build();
    }

    // --------------------------------------------------------------------------------------------
    // Metadata handling
    // --------------------------------------------------------------------------------------------

    /**
     * List of metadata that can write into this format.
     * canal json inner data type
     */
    enum WriteableMetadata {

        DATABASE(
                "database",
                DataTypes.STRING().nullable(),
                DataTypes.FIELD("database", DataTypes.STRING()),
                new MetadataConverter() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public Object convert(RowData row, int pos) {
                        if (row.isNullAt(pos)) {
                            return null;
                        }
                        return row.getString(pos);
                    }
                }),
        TABLE(
                "table",
                DataTypes.STRING().nullable(),
                DataTypes.FIELD("table", DataTypes.STRING()),
                new MetadataConverter() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public Object convert(RowData row, int pos) {
                        if (row.isNullAt(pos)) {
                            return null;
                        }
                        return row.getString(pos);
                    }
                }),
        SQL_TYPE(
                "sql-type",
                DataTypes.MAP(DataTypes.STRING().nullable(), DataTypes.INT().nullable()).nullable(),
                DataTypes.FIELD(
                        "sqlType",
                        DataTypes.MAP(DataTypes.STRING().nullable(), DataTypes.INT().nullable())),
                new MetadataConverter() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public Object convert(RowData row, int pos) {
                        if (row.isNullAt(pos)) {
                            return null;
                        }
                        return row.getMap(pos);
                    }
                }),
        PK_NAMES(
                "pk-names",
                DataTypes.ARRAY(DataTypes.STRING()).nullable(),
                DataTypes.FIELD("pkNames", DataTypes.ARRAY(DataTypes.STRING())),
                new MetadataConverter() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public Object convert(RowData row, int pos) {
                        if (row.isNullAt(pos)) {
                            return null;
                        }
                        return row.getArray(pos);
                    }
                }),
        INGESTION_TIMESTAMP(
                "ingestion-timestamp",
                DataTypes.TIMESTAMP_WITH_LOCAL_TIME_ZONE(3).nullable(),
                DataTypes.FIELD("ts", DataTypes.BIGINT()),
                new MetadataConverter() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public Object convert(RowData row, int pos) {
                        if (row.isNullAt(pos)) {
                            return null;
                        }
                        return row.getTimestamp(pos, 3).getMillisecond();
                    }
                }),
        EVENT_TIMESTAMP(
                "event-timestamp",
                DataTypes.TIMESTAMP_WITH_LOCAL_TIME_ZONE(3).nullable(),
                DataTypes.FIELD("es", DataTypes.BIGINT()),
                new MetadataConverter() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public Object convert(RowData row, int pos) {
                        if (row.isNullAt(pos)) {
                            return null;
                        }
                        return row.getTimestamp(pos, 3).getMillisecond();
                    }
                }),
        // additional metadata
        TYPE(
                "type",
                DataTypes.STRING().nullable(),
                DataTypes.FIELD("type", DataTypes.STRING()),
                new MetadataConverter() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public Object convert(RowData row, int pos) {
                        if (row.isNullAt(pos)) {
                            return null;
                        }
                        return row.getString(pos);
                    }
                }),
        /**
         * It is deprecated, please use {@link this#TYPE} instead
         */
        @Deprecated
        OP_TYPE(
                "op-type",
                DataTypes.STRING().nullable(),
                DataTypes.FIELD("opType", DataTypes.STRING()),
                new MetadataConverter() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public Object convert(RowData row, int pos) {
                        if (row.isNullAt(pos)) {
                            return null;
                        }
                        return row.getString(pos);
                    }
                }),
        IS_DDL(
                "is-ddl",
                DataTypes.BOOLEAN().nullable(),
                DataTypes.FIELD("isDdl", DataTypes.BOOLEAN()),
                new MetadataConverter() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public Object convert(RowData row, int pos) {
                        if (row.isNullAt(pos)) {
                            return null;
                        }
                        return row.getBoolean(pos);
                    }
                }),

        MYSQL_TYPE(
                "mysql-type",
                DataTypes.MAP(DataTypes.STRING().nullable(), DataTypes.STRING().nullable()).nullable(),
                DataTypes.FIELD("mysqlType", DataTypes.MAP(DataTypes.STRING(), DataTypes.STRING())),
                new MetadataConverter() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public Object convert(RowData row, int pos) {
                        if (row.isNullAt(pos)) {
                            return null;
                        }
                        return row.getMap(pos);
                    }
                }),
        BATCH_ID(
                "batch-id",
                DataTypes.BIGINT().nullable(),
                DataTypes.FIELD("batchId", DataTypes.BIGINT()),
                new MetadataConverter() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public Object convert(RowData row, int pos) {
                        if (row.isNullAt(pos)) {
                            return null;
                        }
                        return row.getLong(pos);
                    }
                }),
        UPDATE_BEFORE(
                "update-before",
                DataTypes.ARRAY(
                        DataTypes.MAP(
                                DataTypes.STRING().nullable(), DataTypes.STRING().nullable()).nullable())
                        .nullable(),
                DataTypes.FIELD("updateBefore", DataTypes.ARRAY(
                        DataTypes.MAP(DataTypes.STRING(), DataTypes.STRING()))),
                new MetadataConverter() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public Object convert(RowData row, int pos) {
                        if (row.isNullAt(pos)) {
                            return null;
                        }
                        return row.getArray(pos);
                    }
                });

        final String key;

        final DataType dataType;

        final DataTypes.Field requiredJsonField;

        final MetadataConverter converter;

        WriteableMetadata(
                String key,
                DataType dataType,
                DataTypes.Field requiredJsonField,
                MetadataConverter converter) {
            this.key = key;
            this.dataType = dataType;
            this.requiredJsonField = requiredJsonField;
            this.converter = converter;
        }
    }
}
