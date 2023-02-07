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

package org.apache.inlong.sort.formats.base;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.api.ValidationException;
import org.apache.flink.table.descriptors.DescriptorProperties;
import org.apache.flink.table.factories.DeserializationSchemaFactory;
import org.apache.flink.table.factories.SerializationSchemaFactory;
import org.apache.flink.table.factories.TableFactoryService;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.ArrayType;
import org.apache.flink.table.types.logical.BigIntType;
import org.apache.flink.table.types.logical.BinaryType;
import org.apache.flink.table.types.logical.BooleanType;
import org.apache.flink.table.types.logical.DateType;
import org.apache.flink.table.types.logical.DecimalType;
import org.apache.flink.table.types.logical.DoubleType;
import org.apache.flink.table.types.logical.FloatType;
import org.apache.flink.table.types.logical.IntType;
import org.apache.flink.table.types.logical.LocalZonedTimestampType;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.table.types.logical.MapType;
import org.apache.flink.table.types.logical.NullType;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.table.types.logical.SmallIntType;
import org.apache.flink.table.types.logical.TimeType;
import org.apache.flink.table.types.logical.TimestampType;
import org.apache.flink.table.types.logical.TinyIntType;
import org.apache.flink.table.types.logical.VarBinaryType;
import org.apache.flink.table.types.logical.VarCharType;
import org.apache.flink.types.Row;
import org.apache.inlong.sort.formats.common.ArrayFormatInfo;
import org.apache.inlong.sort.formats.common.ArrayTypeInfo;
import org.apache.inlong.sort.formats.common.BasicFormatInfo;
import org.apache.inlong.sort.formats.common.BinaryFormatInfo;
import org.apache.inlong.sort.formats.common.BinaryTypeInfo;
import org.apache.inlong.sort.formats.common.BooleanFormatInfo;
import org.apache.inlong.sort.formats.common.BooleanTypeInfo;
import org.apache.inlong.sort.formats.common.ByteFormatInfo;
import org.apache.inlong.sort.formats.common.ByteTypeInfo;
import org.apache.inlong.sort.formats.common.DateFormatInfo;
import org.apache.inlong.sort.formats.common.DateTypeInfo;
import org.apache.inlong.sort.formats.common.DecimalFormatInfo;
import org.apache.inlong.sort.formats.common.DecimalTypeInfo;
import org.apache.inlong.sort.formats.common.DoubleFormatInfo;
import org.apache.inlong.sort.formats.common.DoubleTypeInfo;
import org.apache.inlong.sort.formats.common.FloatFormatInfo;
import org.apache.inlong.sort.formats.common.FloatTypeInfo;
import org.apache.inlong.sort.formats.common.FormatInfo;
import org.apache.inlong.sort.formats.common.FormatUtils;
import org.apache.inlong.sort.formats.common.IntFormatInfo;
import org.apache.inlong.sort.formats.common.IntTypeInfo;
import org.apache.inlong.sort.formats.common.LocalZonedTimestampFormatInfo;
import org.apache.inlong.sort.formats.common.LocalZonedTimestampTypeInfo;
import org.apache.inlong.sort.formats.common.LongFormatInfo;
import org.apache.inlong.sort.formats.common.LongTypeInfo;
import org.apache.inlong.sort.formats.common.MapFormatInfo;
import org.apache.inlong.sort.formats.common.MapTypeInfo;
import org.apache.inlong.sort.formats.common.NullFormatInfo;
import org.apache.inlong.sort.formats.common.RowFormatInfo;
import org.apache.inlong.sort.formats.common.RowTypeInfo;
import org.apache.inlong.sort.formats.common.ShortFormatInfo;
import org.apache.inlong.sort.formats.common.ShortTypeInfo;
import org.apache.inlong.sort.formats.common.StringFormatInfo;
import org.apache.inlong.sort.formats.common.StringTypeInfo;
import org.apache.inlong.sort.formats.common.TimeFormatInfo;
import org.apache.inlong.sort.formats.common.TimeTypeInfo;
import org.apache.inlong.sort.formats.common.TimestampFormatInfo;
import org.apache.inlong.sort.formats.common.TimestampTypeInfo;
import org.apache.inlong.sort.formats.common.TypeInfo;
import org.apache.inlong.sort.formats.common.VarBinaryFormatInfo;
import org.apache.inlong.sort.formats.common.VarCharFormatInfo;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.apache.flink.table.descriptors.FormatDescriptorValidator.FORMAT_DERIVE_SCHEMA;
import static org.apache.flink.table.factories.TableFormatFactoryBase.deriveSchema;
import static org.apache.flink.util.Preconditions.checkState;
import static org.apache.inlong.sort.formats.base.TableFormatConstants.FORMAT_SCHEMA;

/**
 * A utility class for table formats.
 */
public class TableFormatUtils {

    /**
     * Returns the {@link DeserializationSchema} described by the given
     * properties.
     *
     * @param properties The properties describing the deserializer.
     * @param classLoader The class loader for the deserializer.
     * @param <T> The type of the data.
     * @return The {@link DeserializationSchema} described by the properties.
     */
    public static <T> DeserializationSchema<T> getDeserializationSchema(
            final Map<String, String> properties,
            final ClassLoader classLoader) {
        @SuppressWarnings("unchecked")
        final DeserializationSchemaFactory<T> deserializationSchemaFactory =
                TableFactoryService.find(
                        DeserializationSchemaFactory.class,
                        properties,
                        classLoader);

        return deserializationSchemaFactory.createDeserializationSchema(properties);
    }

    /**
     * Returns the {@link SerializationSchema} described by the given
     * properties.
     *
     * @param properties The properties describing the serializer.
     * @param classLoader The class loader for the serializer.
     * @param <T> The type of the data.
     * @return The {@link SerializationSchema} described by the properties.
     */
    public static <T> SerializationSchema<T> getSerializationSchema(
            final Map<String, String> properties,
            final ClassLoader classLoader) {
        @SuppressWarnings("unchecked")
        final SerializationSchemaFactory<T> serializationSchemaFactory =
                TableFactoryService.find(
                        SerializationSchemaFactory.class,
                        properties,
                        classLoader);

        return serializationSchemaFactory.createSerializationSchema(properties);
    }

    /**
     * Returns the {@link DeserializationSchema} described by the given
     * properties.
     *
     * @param properties The properties describing the deserializer.
     * @param fields The fields to project.
     * @param classLoader The class loader for the deserializer.
     * @param <T> The type of the data.
     * @return The {@link DeserializationSchema} described by the properties.
     */
    public static <T> DeserializationSchema<Row> getProjectedDeserializationSchema(
            final Map<String, String> properties,
            final int[] fields,
            final ClassLoader classLoader) {
        final ProjectedDeserializationSchemaFactory deserializationSchemaFactory =
                TableFactoryService.find(
                        ProjectedDeserializationSchemaFactory.class,
                        properties,
                        classLoader);

        return deserializationSchemaFactory
                .createProjectedDeserializationSchema(properties, fields);
    }

    /**
     * Returns the {@link SerializationSchema} described by the given
     * properties.
     *
     * @param properties The properties describing the serializer.
     * @param fields The fields to project.
     * @param classLoader The class loader for the serializer.
     * @return The {@link SerializationSchema} described by the properties.
     */
    public static SerializationSchema<Row> getProjectedSerializationSchema(
            final Map<String, String> properties,
            final int[] fields,
            final ClassLoader classLoader) {
        final ProjectedSerializationSchemaFactory serializationSchemaFactory =
                TableFactoryService.find(
                        ProjectedSerializationSchemaFactory.class,
                        properties,
                        classLoader);

        return serializationSchemaFactory
                .createProjectedSerializationSchema(properties, fields);
    }

    /**
     * Returns the {@link TableFormatSerializer} described by the given
     * properties.
     *
     * @param properties The properties describing the serializer.
     * @param classLoader The class loader for the serializer.
     * @return The {@link TableFormatSerializer} described by the properties.
     */
    public static TableFormatSerializer getTableFormatSerializer(
            final Map<String, String> properties,
            final ClassLoader classLoader) {
        final TableFormatSerializerFactory tableFormatSerializerFactory =
                TableFactoryService.find(
                        TableFormatSerializerFactory.class,
                        properties,
                        classLoader);

        return tableFormatSerializerFactory
                .createFormatSerializer(properties);
    }

    /**
     * Returns the {@link TableFormatDeserializer} described by the
     * given properties.
     *
     * @param properties The properties describing the deserializer.
     * @param classLoader The class loader for the deserializer.
     * @return The {@link TableFormatDeserializer} described by the properties.
     */
    public static TableFormatDeserializer getTableFormatDeserializer(
            final Map<String, String> properties,
            final ClassLoader classLoader) {
        final TableFormatDeserializerFactory tableFormatDeserializerFactory =
                TableFactoryService.find(
                        TableFormatDeserializerFactory.class,
                        properties,
                        classLoader);

        return tableFormatDeserializerFactory
                .createFormatDeserializer(properties);
    }

    /**
     * Derive the format information for the given type.
     *
     * @param logicalType The type whose format is derived.
     * @return The format information for the given type.
     */
    public static FormatInfo deriveFormatInfo(LogicalType logicalType) {
        if (logicalType instanceof VarCharType) {
            return StringFormatInfo.INSTANCE;
        } else if (logicalType instanceof BooleanType) {
            return BooleanFormatInfo.INSTANCE;
        } else if (logicalType instanceof TinyIntType) {
            return ByteFormatInfo.INSTANCE;
        } else if (logicalType instanceof SmallIntType) {
            return ShortFormatInfo.INSTANCE;
        } else if (logicalType instanceof IntType) {
            return IntFormatInfo.INSTANCE;
        } else if (logicalType instanceof BigIntType) {
            return LongFormatInfo.INSTANCE;
        } else if (logicalType instanceof FloatType) {
            return FloatFormatInfo.INSTANCE;
        } else if (logicalType instanceof DoubleType) {
            return DoubleFormatInfo.INSTANCE;
        } else if (logicalType instanceof DecimalType) {
            return DecimalFormatInfo.INSTANCE;
        } else if (logicalType instanceof DateType) {
            return new DateFormatInfo();
        } else if (logicalType instanceof TimeType) {
            return new TimeFormatInfo();
        } else if (logicalType instanceof TimestampType) {
            return new TimestampFormatInfo();
        } else if (logicalType instanceof LocalZonedTimestampType) {
            return new LocalZonedTimestampFormatInfo();
        } else if (logicalType instanceof ArrayType) {
            ArrayType arrayType = (ArrayType) logicalType;
            LogicalType elementType = arrayType.getElementType();

            FormatInfo elementFormatInfo = deriveFormatInfo(elementType);

            return new ArrayFormatInfo(elementFormatInfo);
        } else if (logicalType instanceof MapType) {
            MapType mapType = (MapType) logicalType;
            LogicalType keyType = mapType.getKeyType();
            LogicalType valueType = mapType.getValueType();

            FormatInfo keyFormatInfo = deriveFormatInfo(keyType);
            FormatInfo valueFormatInfo = deriveFormatInfo(valueType);

            return new MapFormatInfo(keyFormatInfo, valueFormatInfo);
        } else if (logicalType instanceof RowType) {
            RowType rowType = (RowType) logicalType;
            List<RowType.RowField> rowFields = rowType.getFields();

            String[] fieldNames = new String[rowFields.size()];
            FormatInfo[] fieldFormatInfos = new FormatInfo[rowFields.size()];

            for (int i = 0; i < rowFields.size(); ++i) {
                RowType.RowField rowField = rowFields.get(i);

                fieldNames[i] = rowField.getName();
                fieldFormatInfos[i] = deriveFormatInfo(rowField.getType());
            }

            return new RowFormatInfo(fieldNames, fieldFormatInfos);
        } else if (logicalType instanceof BinaryType) {
            return BinaryFormatInfo.INSTANCE;
        } else if (logicalType instanceof VarBinaryType) {
            return VarBinaryFormatInfo.INSTANCE;
        } else if (logicalType instanceof NullType) {
            return NullFormatInfo.INSTANCE;
        } else {
            throw new IllegalArgumentException(String.format("not found logicalType %s",
                    logicalType == null ? "null" : logicalType.toString()));
        }
    }

    /**
     * Derive the LogicalType for the given FormatInfo.
     */
    public static LogicalType deriveLogicalType(FormatInfo formatInfo) {
        if (formatInfo instanceof StringFormatInfo) {
            return new VarCharType(VarCharType.MAX_LENGTH);
        } else if (formatInfo instanceof VarCharFormatInfo) {
            return new VarCharType(((VarCharFormatInfo) formatInfo).getLength());
        } else if (formatInfo instanceof BooleanFormatInfo) {
            return new BooleanType();
        } else if (formatInfo instanceof ByteFormatInfo) {
            return new TinyIntType();
        } else if (formatInfo instanceof ShortFormatInfo) {
            return new SmallIntType();
        } else if (formatInfo instanceof IntFormatInfo) {
            return new IntType();
        } else if (formatInfo instanceof LongFormatInfo) {
            return new BigIntType();
        } else if (formatInfo instanceof FloatFormatInfo) {
            return new FloatType();
        } else if (formatInfo instanceof DoubleFormatInfo) {
            return new DoubleType();
        } else if (formatInfo instanceof DecimalFormatInfo) {
            DecimalFormatInfo decimalFormatInfo = (DecimalFormatInfo) formatInfo;
            return new DecimalType(decimalFormatInfo.getPrecision(), decimalFormatInfo.getScale());
        } else if (formatInfo instanceof TimeFormatInfo) {
            return new TimeType(((TimeFormatInfo) formatInfo).getPrecision());
        } else if (formatInfo instanceof DateFormatInfo) {
            return new DateType();
        } else if (formatInfo instanceof TimestampFormatInfo) {
            return new TimestampType(((TimestampFormatInfo) formatInfo).getPrecision());
        } else if (formatInfo instanceof LocalZonedTimestampFormatInfo) {
            return new LocalZonedTimestampType(((LocalZonedTimestampFormatInfo) formatInfo).getPrecision());
        } else if (formatInfo instanceof ArrayFormatInfo) {
            FormatInfo elementFormatInfo = ((ArrayFormatInfo) formatInfo).getElementFormatInfo();
            return new ArrayType(deriveLogicalType(elementFormatInfo));
        } else if (formatInfo instanceof MapFormatInfo) {
            MapFormatInfo mapFormatInfo = (MapFormatInfo) formatInfo;
            FormatInfo keyFormatInfo = mapFormatInfo.getKeyFormatInfo();
            FormatInfo valueFormatInfo = mapFormatInfo.getValueFormatInfo();
            return new MapType(deriveLogicalType(keyFormatInfo), deriveLogicalType(valueFormatInfo));
        } else if (formatInfo instanceof RowFormatInfo) {
            RowFormatInfo rowFormatInfo = (RowFormatInfo) formatInfo;
            FormatInfo[] formatInfos = rowFormatInfo.getFieldFormatInfos();
            int formatInfosSize = formatInfos.length;
            LogicalType[] logicalTypes = new LogicalType[formatInfosSize];

            for (int i = 0; i < formatInfosSize; ++i) {
                logicalTypes[i] = deriveLogicalType(formatInfos[i]);
            }
            return RowType.of(logicalTypes, rowFormatInfo.getFieldNames());
        } else if (formatInfo instanceof BinaryFormatInfo) {
            BinaryFormatInfo binaryFormatInfo = (BinaryFormatInfo) formatInfo;
            return new BinaryType(binaryFormatInfo.getLength());
        } else if (formatInfo instanceof VarBinaryFormatInfo) {
            VarBinaryFormatInfo varBinaryFormatInfo = (VarBinaryFormatInfo) formatInfo;
            return new VarBinaryType(varBinaryFormatInfo.getLength());
        } else if (formatInfo instanceof NullFormatInfo) {
            return new NullType();
        } else {
            throw new IllegalArgumentException(String.format("not found formatInfo %s",
                    formatInfo == null ? "null" : formatInfo.toString()));
        }
    }

    /**
     * Returns the type represented by the given format.
     *
     * @param typeInfo The type information.
     * @return The type represented by the given format.
     */
    public static TypeInformation<?> getType(TypeInfo typeInfo) {
        if (typeInfo instanceof StringTypeInfo) {
            return Types.STRING;
        } else if (typeInfo instanceof BooleanTypeInfo) {
            return Types.BOOLEAN;
        } else if (typeInfo instanceof ByteTypeInfo) {
            return Types.BYTE;
        } else if (typeInfo instanceof ShortTypeInfo) {
            return Types.SHORT;
        } else if (typeInfo instanceof IntTypeInfo) {
            return Types.INT;
        } else if (typeInfo instanceof LongTypeInfo) {
            return Types.LONG;
        } else if (typeInfo instanceof FloatTypeInfo) {
            return Types.FLOAT;
        } else if (typeInfo instanceof DoubleTypeInfo) {
            return Types.DOUBLE;
        } else if (typeInfo instanceof DecimalTypeInfo) {
            return Types.BIG_DEC;
        } else if (typeInfo instanceof DateTypeInfo) {
            return Types.SQL_DATE;
        } else if (typeInfo instanceof TimeTypeInfo) {
            return Types.SQL_TIME;
        } else if (typeInfo instanceof TimestampTypeInfo) {
            return Types.SQL_TIMESTAMP;
        } else if (typeInfo instanceof LocalZonedTimestampTypeInfo) {
            return Types.LOCAL_DATE_TIME;
        } else if (typeInfo instanceof BinaryTypeInfo) {
            return Types.PRIMITIVE_ARRAY(Types.BYTE);
        } else if (typeInfo instanceof ArrayTypeInfo) {
            ArrayTypeInfo arrayTypeInfo = (ArrayTypeInfo) typeInfo;
            TypeInfo elementTypeInfo =
                    arrayTypeInfo.getElementTypeInfo();
            TypeInformation<?> elementType = getType(elementTypeInfo);

            return Types.OBJECT_ARRAY(elementType);
        } else if (typeInfo instanceof MapTypeInfo) {
            MapTypeInfo mapTypeInfo = (MapTypeInfo) typeInfo;
            TypeInfo keyTypeInfo = mapTypeInfo.getKeyTypeInfo();
            TypeInfo valueTypeInfo = mapTypeInfo.getValueTypeInfo();

            TypeInformation<?> keyType = getType(keyTypeInfo);
            TypeInformation<?> valueType = getType(valueTypeInfo);

            return Types.MAP(keyType, valueType);
        } else if (typeInfo instanceof RowTypeInfo) {
            RowTypeInfo rowTypeInfo = (RowTypeInfo) typeInfo;
            String[] fieldNames = rowTypeInfo.getFieldNames();
            TypeInfo[] fieldTypeInfos = rowTypeInfo.getFieldTypeInfos();

            TypeInformation<?>[] fieldTypes =
                    Arrays.stream(fieldTypeInfos)
                            .map(TableFormatUtils::getType)
                            .toArray(TypeInformation<?>[]::new);

            return Types.ROW_NAMED(fieldNames, fieldTypes);
        } else {
            throw new IllegalStateException("Unexpected type info " + typeInfo + ".");
        }
    }

    /**
     * Returns the format defined in the given property.
     *
     * @param descriptorProperties The properties of the descriptor.
     * @return The basic row format defined in the descriptor.
     */
    public static RowFormatInfo deserializeRowFormatInfo(
            DescriptorProperties descriptorProperties) {
        try {
            String schema = descriptorProperties.getString(FORMAT_SCHEMA);

            FormatInfo formatInfo = FormatUtils.demarshall(schema);
            if (!(formatInfo instanceof RowFormatInfo)) {
                throw new IllegalStateException("Unexpected format type.");
            }

            return (RowFormatInfo) formatInfo;
        } catch (Exception e) {
            throw new ValidationException("The schema is invalid.", e);
        }
    }

    /**
     * Derives the format from the given schema.
     *
     * @param descriptorProperties The properties of the descriptor.
     * @return The format derived from the schema in the descriptor.
     */
    public static RowFormatInfo deriveRowFormatInfo(
            DescriptorProperties descriptorProperties) {
        TableSchema tableSchema =
                deriveSchema(descriptorProperties.asMap());

        int numFields = tableSchema.getFieldCount();
        String[] fieldNames = tableSchema.getFieldNames();
        DataType[] fieldTypes = tableSchema.getFieldDataTypes();

        FormatInfo[] fieldFormatInfos = new FormatInfo[numFields];
        for (int i = 0; i < numFields; ++i) {
            LogicalType fieldType = fieldTypes[i].getLogicalType();
            fieldFormatInfos[i] = deriveFormatInfo(fieldType);
        }

        return new RowFormatInfo(fieldNames, fieldFormatInfos);
    }

    /**
     * Returns the schema in the properties.
     *
     * @param descriptorProperties The properties of the descriptor.
     * @return The schema in the properties.
     */
    public static RowFormatInfo getRowFormatInfo(
            DescriptorProperties descriptorProperties) {
        if (descriptorProperties.containsKey(FORMAT_SCHEMA)) {
            return deserializeRowFormatInfo(descriptorProperties);
        } else {
            return deriveRowFormatInfo(descriptorProperties);
        }
    }

    /**
     * Projects the given schema.
     *
     * @param rowFormatInfo The schema to be projected.
     * @return The projected schema in the properties.
     */
    public static RowFormatInfo projectRowFormatInfo(
            RowFormatInfo rowFormatInfo,
            int[] fields) {
        String[] fieldNames = rowFormatInfo.getFieldNames();
        FormatInfo[] fieldFormatInfos = rowFormatInfo.getFieldFormatInfos();

        String[] projectedFieldNames = new String[fields.length];
        FormatInfo[] projectedFieldFormatInfos = new FormatInfo[fields.length];

        for (int i = 0; i < fields.length; ++i) {
            projectedFieldNames[i] = fieldNames[fields[i]];
            projectedFieldFormatInfos[i] = fieldFormatInfos[fields[i]];
        }

        return new RowFormatInfo(projectedFieldNames, projectedFieldFormatInfos);
    }

    /**
     * Validates the schema in the descriptor.
     *
     * @param descriptorProperties The properties of the descriptor.
     */
    public static void validateSchema(DescriptorProperties descriptorProperties) {
        final boolean defineSchema = descriptorProperties.containsKey(FORMAT_SCHEMA);
        final boolean deriveSchema = descriptorProperties.containsKey(FORMAT_DERIVE_SCHEMA);

        if (defineSchema && deriveSchema) {
            throw new ValidationException("Format cannot define a schema and "
                    + "derive from the table's schema at the same time.");
        } else if (defineSchema) {
            descriptorProperties.validateString(FORMAT_SCHEMA, false);
        } else if (deriveSchema) {
            descriptorProperties.validateBoolean(FORMAT_DERIVE_SCHEMA, false);
        } else {
            throw new ValidationException("A definition of a schema or "
                    + "derivation from the table's schema is required.");
        }
    }

    /**
     * Deserializes the basic field.
     */
    public static Object deserializeBasicField(
            String fieldName,
            FormatInfo fieldFormatInfo,
            String fieldText,
            String nullLiteral) {
        checkState(fieldFormatInfo instanceof BasicFormatInfo);

        if (fieldText == null) {
            return null;
        }

        if (nullLiteral == null) {
            if (fieldText.isEmpty()) {
                if (fieldFormatInfo instanceof StringFormatInfo) {
                    return "";
                } else {
                    return null;
                }
            }
        } else {
            if (fieldText.equals(nullLiteral)) {
                return null;
            }
        }

        try {
            return ((BasicFormatInfo<?>) fieldFormatInfo).deserialize(fieldText);
        } catch (Exception e) {
            throw new RuntimeException("Could not properly deserialize the "
                    + "text " + fieldText + " for field " + fieldName + ".", e);
        }
    }

    /**
     * Serializes the basic field.
     */
    @SuppressWarnings("unchecked")
    public static String serializeBasicField(
            String fieldName,
            FormatInfo fieldFormatInfo,
            Object field,
            String nullLiteral) {
        checkState(fieldFormatInfo instanceof BasicFormatInfo);

        if (field == null) {
            return nullLiteral == null ? "" : nullLiteral;
        }

        try {
            return ((BasicFormatInfo<Object>) fieldFormatInfo).serialize(field);
        } catch (Exception e) {
            throw new RuntimeException("Could not properly serialize the "
                    + "value " + field + " for field " + fieldName + ".", e);
        }
    }
}
