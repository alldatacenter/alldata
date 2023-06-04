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
import org.apache.flink.formats.json.JsonRowDataSerializationSchema;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.data.ArrayData;
import org.apache.flink.table.data.GenericArrayData;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.table.types.utils.DataTypeUtils;
import org.apache.flink.types.RowKind;
import org.apache.inlong.sort.formats.json.canal.CanalJsonEnhancedEncodingFormat.WriteableMetadata;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Serialization schema that serializes an object of Flink Table/SQL internal data structure {@link
 * RowData} into a Canal JSON bytes.
 * Different from flink:1.13.5.This can write metadata.
 *
 * @see <a href="https://github.com/alibaba/canal">Alibaba Canal</a>
 */
public class CanalJsonEnhancedSerializationSchema implements SerializationSchema<RowData> {

    private static final long serialVersionUID = 1L;

    private static final StringData OP_INSERT = StringData.fromString("INSERT");
    private static final StringData OP_DELETE = StringData.fromString("DELETE");
    /**
     * The serializer to serialize Canal JSON data.
     */
    private final JsonRowDataSerializationSchema jsonSerializer;
    private final RowData.FieldGetter[] physicalFieldGetter;
    private final RowData.FieldGetter[] wirteableMetadataFieldGetter;
    /**
     * row schema that json serializer can parse output row to json format
     */
    private final RowType jsonRowType;
    /**
     * The index in writeableMetadata of {@link WriteableMetadata#TYPE}
     */
    private final int typeIndex;
    private transient GenericRowData reuse;

    /**
     * Constructor of CanalJsonEnhancedSerializationSchema.
     */
    public CanalJsonEnhancedSerializationSchema(
            DataType physicalDataType,
            List<WriteableMetadata> writeableMetadata,
            TimestampFormat timestampFormat,
            JsonOptions.MapNullKeyMode mapNullKeyMode,
            String mapNullKeyLiteral,
            boolean encodeDecimalAsPlainNumber) {
        final List<LogicalType> physicalChildren = physicalDataType.getLogicalType().getChildren();
        this.jsonRowType = createJsonRowType(physicalDataType, writeableMetadata);
        typeIndex = writeableMetadata.indexOf(WriteableMetadata.TYPE);
        this.physicalFieldGetter = IntStream.range(0, physicalChildren.size())
                .mapToObj(targetField -> RowData.createFieldGetter(physicalChildren.get(targetField), targetField))
                .toArray(RowData.FieldGetter[]::new);
        this.wirteableMetadataFieldGetter =
                IntStream.range(physicalChildren.size(), physicalChildren.size() + writeableMetadata.size())
                        .mapToObj(targetField -> new RowData.FieldGetter() {

                            @Nullable
                            @Override
                            public Object getFieldOrNull(RowData row) {
                                WriteableMetadata curWriteableMetadata = writeableMetadata
                                        .get(targetField - physicalChildren.size());
                                return curWriteableMetadata.converter.convert(row, targetField);
                            }
                        }).toArray(RowData.FieldGetter[]::new);

        this.jsonSerializer =
                new JsonRowDataSerializationSchema(
                        jsonRowType,
                        timestampFormat,
                        mapNullKeyMode,
                        mapNullKeyLiteral,
                        encodeDecimalAsPlainNumber);
    }

    private static RowType createJsonRowType(DataType physicalDataType, List<WriteableMetadata> writeableMetadata) {
        // Canal JSON contains other information, e.g. "database", "ts"
        // but we don't need them
        // and we don't need "old" , because can not support UPDATE_BEFORE,UPDATE_AFTER
        DataType root =
                DataTypes.ROW(
                        DataTypes.FIELD("data", DataTypes.ARRAY(physicalDataType)),
                        WriteableMetadata.TYPE.requiredJsonField);
        // append fields that are required for reading metadata in the root
        final List<DataTypes.Field> metadataFields =
                writeableMetadata.stream().filter(m -> m != WriteableMetadata.TYPE)
                        .map(m -> m.requiredJsonField)
                        .distinct()
                        .collect(Collectors.toList());
        return (RowType) DataTypeUtils.appendRowFields(root, metadataFields).getLogicalType();
    }

    /**
     * Init for this serialization
     * In this method, it initializes {@link this#reuse}, the size of the {@link this#reuse} will be
     * length of physicalFields add the length of metadata fields.Here we put the physical field into a array whose key
     * is 'data', and put it in the zeroth element of the {@link this#reuse}, and put the {@link WriteableMetadata#TYPE}
     * in the first element of the {@link this#reuse},so when the metadata field does not contain
     * {@link WriteableMetadata#TYPE}, it's size is two + the number of metadata fields, when included, it's size is
     * one + the number of metadata fields
     *
     * @param context The context used for initialization
     */
    @Override
    public void open(InitializationContext context) {
        int size = 2 + wirteableMetadataFieldGetter.length;
        if (typeIndex != -1) {
            size--;
        }
        reuse = new GenericRowData(size);
    }

    /**
     * Serialize the row with ignore the {@link WriteableMetadata#TYPE}
     */
    @Override
    public byte[] serialize(RowData row) {
        try {
            // physical data injection
            GenericRowData physicalData = new GenericRowData(physicalFieldGetter.length);
            IntStream.range(0, physicalFieldGetter.length)
                    .forEach(targetField -> physicalData.setField(targetField,
                            physicalFieldGetter[targetField].getFieldOrNull(row)));
            ArrayData arrayData = new GenericArrayData(new RowData[]{physicalData});
            reuse.setField(0, arrayData);

            // mete data injection
            StringData opType = rowKind2String(row.getRowKind());
            reuse.setField(1, opType);
            if (typeIndex != -1) {
                IntStream.range(0, wirteableMetadataFieldGetter.length)
                        .forEach(metaIndex -> {
                            if (metaIndex < typeIndex) {
                                reuse.setField(metaIndex + 2,
                                        wirteableMetadataFieldGetter[metaIndex].getFieldOrNull(row));
                            } else if (metaIndex > typeIndex) {
                                reuse.setField(metaIndex + 1,
                                        wirteableMetadataFieldGetter[metaIndex].getFieldOrNull(row));
                            }
                        });
            } else {
                IntStream.range(0, wirteableMetadataFieldGetter.length)
                        .forEach(metaIndex -> reuse
                                .setField(metaIndex + 2, wirteableMetadataFieldGetter[metaIndex].getFieldOrNull(row)));
            }
            return jsonSerializer.serialize(reuse);
        } catch (Throwable t) {
            throw new RuntimeException("Could not serialize row '" + row + "'.", t);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CanalJsonEnhancedSerializationSchema that = (CanalJsonEnhancedSerializationSchema) o;
        return Objects.equals(jsonSerializer, that.jsonSerializer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jsonSerializer);
    }

    private StringData rowKind2String(RowKind rowKind) {
        switch (rowKind) {
            case INSERT:
            case UPDATE_AFTER:
                return OP_INSERT;
            case UPDATE_BEFORE:
            case DELETE:
                return OP_DELETE;
            default:
                throw new UnsupportedOperationException(
                        "Unsupported operation '" + rowKind + "' for row kind.");
        }
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Converter that load a metadata field from the row that comes out of the input RowData.
     * Finally all metadata field will splice into a GenericRowData, then json Serializer serialize it into json string.
     */
    interface MetadataConverter extends Serializable {

        Object convert(RowData inputRow, int pos);
    }
}
