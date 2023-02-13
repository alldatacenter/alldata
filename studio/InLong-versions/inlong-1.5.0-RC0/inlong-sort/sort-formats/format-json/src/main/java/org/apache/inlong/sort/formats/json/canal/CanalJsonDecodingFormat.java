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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.formats.common.TimestampFormat;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.connector.format.DecodingFormat;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.data.GenericArrayData;
import org.apache.flink.table.data.GenericMapData;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;
import org.apache.flink.table.data.TimestampData;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.utils.DataTypeUtils;
import org.apache.flink.types.RowKind;
import org.apache.inlong.sort.formats.json.canal.CanalJsonDeserializationSchema.MetadataConverter;

/** {@link DecodingFormat} for Canal using JSON encoding. */
public class CanalJsonDecodingFormat implements DecodingFormat<DeserializationSchema<RowData>> {

    // --------------------------------------------------------------------------------------------
    // Mutable attributes
    // --------------------------------------------------------------------------------------------

    private List<String> metadataKeys;

    // --------------------------------------------------------------------------------------------
    // Canal-specific attributes
    // --------------------------------------------------------------------------------------------

    private final @Nullable String database;

    private final @Nullable String table;

    private final boolean ignoreParseErrors;

    private final TimestampFormat timestampFormat;

    public CanalJsonDecodingFormat(
            String database,
            String table,
            boolean ignoreParseErrors,
            TimestampFormat timestampFormat) {
        this.database = database;
        this.table = table;
        this.ignoreParseErrors = ignoreParseErrors;
        this.timestampFormat = timestampFormat;
        this.metadataKeys = Collections.emptyList();
    }

    @Override
    public DeserializationSchema<RowData> createRuntimeDecoder(
            DynamicTableSource.Context context, DataType physicalDataType) {
        final List<ReadableMetadata> readableMetadata =
                metadataKeys.stream()
                        .map(
                                k -> Stream.of(ReadableMetadata.values())
                                        .filter(rm -> rm.key.equals(k))
                                        .findFirst()
                                        .<IllegalStateException>orElseThrow(IllegalStateException::new))
                        .collect(Collectors.toList());
        final List<DataTypes.Field> metadataFields =
                readableMetadata.stream()
                        .map(m -> DataTypes.FIELD(m.key, m.dataType))
                        .collect(Collectors.toList());
        final DataType producedDataType =
                DataTypeUtils.appendRowFields(physicalDataType, metadataFields);
        final TypeInformation<RowData> producedTypeInfo =
                context.createTypeInformation(producedDataType);
        return CanalJsonDeserializationSchema.builder(
                physicalDataType, readableMetadata, producedTypeInfo)
                .setDatabase(database)
                .setTable(table)
                .setIgnoreParseErrors(ignoreParseErrors)
                .setTimestampFormat(timestampFormat)
                .build();
    }

    @Override
    public Map<String, DataType> listReadableMetadata() {
        final Map<String, DataType> metadataMap = new LinkedHashMap<>();
        Stream.of(ReadableMetadata.values())
                .forEachOrdered(m -> metadataMap.put(m.key, m.dataType));
        return metadataMap;
    }

    @Override
    public void applyReadableMetadata(List<String> metadataKeys) {
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

    /** List of metadata that can be read with this format. */
    public enum ReadableMetadata {

        DATABASE(
                "database",
                DataTypes.STRING().nullable(),
                DataTypes.FIELD("database", DataTypes.STRING()),
                new MetadataConverter() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public Object convert(GenericRowData row, int pos) {
                        return row.getString(pos);
                    }

                    @Override
                    public Object convert(Object in) {
                        return StringData.fromString(in.toString());
                    }
                }),

        TABLE(
                "table",
                DataTypes.STRING().nullable(),
                DataTypes.FIELD("table", DataTypes.STRING()),
                new MetadataConverter() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public Object convert(GenericRowData row, int pos) {
                        return row.getString(pos);
                    }

                    @Override
                    public Object convert(Object in) {
                        return StringData.fromString(in.toString());
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
                    public Object convert(GenericRowData row, int pos) {
                        return row.getMap(pos);
                    }

                    @Override
                    public Object convert(Object in) {
                        return new GenericMapData((Map<String, Integer>) in);
                    }
                }),

        PK_NAMES(
                "pk-names",
                DataTypes.ARRAY(DataTypes.STRING()).nullable(),
                DataTypes.FIELD("pkNames", DataTypes.ARRAY(DataTypes.STRING())),
                new MetadataConverter() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public Object convert(GenericRowData row, int pos) {
                        return row.getArray(pos);
                    }

                    @Override
                    public Object convert(Object in) {
                        return new GenericArrayData((Object[]) in);
                    }
                }),

        INGESTION_TIMESTAMP(
                "ingestion-timestamp",
                DataTypes.TIMESTAMP_WITH_LOCAL_TIME_ZONE(3).nullable(),
                DataTypes.FIELD("ts", DataTypes.BIGINT()),
                new MetadataConverter() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public Object convert(GenericRowData row, int pos) {
                        if (row.isNullAt(pos)) {
                            return null;
                        }
                        return TimestampData.fromEpochMillis(row.getLong(pos));
                    }

                    @Override
                    public Object convert(Object in) {
                        return in;
                    }
                }),

        EVENT_TIMESTAMP(
                "event-timestamp",
                DataTypes.BIGINT().nullable(),
                DataTypes.FIELD("es", DataTypes.BIGINT()),
                new MetadataConverter() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public Object convert(GenericRowData row, int pos) {
                        if (row.isNullAt(pos)) {
                            return null;
                        }
                        return row.getLong(pos);
                    }

                    @Override
                    public Object convert(Object in) {
                        return in;
                    }
                }),

        IS_DDL(
                "is-ddl",
                DataTypes.BOOLEAN().nullable(),
                DataTypes.FIELD("isDdl", DataTypes.BOOLEAN()),
                new MetadataConverter() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public Object convert(GenericRowData row, int pos) {
                        if (row.isNullAt(pos)) {
                            return null;
                        }
                        return row.getBoolean(pos);
                    }

                    @Override
                    public Object convert(Object in) {
                        return in;
                    }
                });

        final String key;

        final DataType dataType;

        final DataTypes.Field requiredJsonField;

        final MetadataConverter converter;

        ReadableMetadata(
                String key,
                DataType dataType,
                DataTypes.Field requiredJsonField,
                MetadataConverter converter) {
            this.key = key;
            this.dataType = dataType;
            this.requiredJsonField = requiredJsonField;
            this.converter = converter;
        }

        public String getKey() {
            return key;
        }
    }
}
