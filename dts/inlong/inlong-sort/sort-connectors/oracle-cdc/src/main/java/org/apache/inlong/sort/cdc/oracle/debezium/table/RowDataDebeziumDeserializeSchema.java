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

package org.apache.inlong.sort.cdc.oracle.debezium.table;

import static org.apache.flink.util.Preconditions.checkNotNull;

import io.debezium.data.Envelope;
import io.debezium.data.SpecialValueDecimal;
import io.debezium.data.VariableScaleDecimal;
import io.debezium.relational.Column;
import io.debezium.relational.history.TableChanges.TableChange;
import io.debezium.time.Date;
import io.debezium.time.MicroTime;
import io.debezium.time.MicroTimestamp;
import io.debezium.time.NanoTime;
import io.debezium.time.NanoTimestamp;
import io.debezium.time.Timestamp;
import io.debezium.time.ZonedTimestamp;
import java.io.Serializable;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.table.data.DecimalData;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;
import org.apache.flink.table.data.TimestampData;
import org.apache.flink.table.types.logical.DecimalType;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.types.RowKind;
import org.apache.flink.util.Collector;
import org.apache.inlong.sort.cdc.base.debezium.DebeziumDeserializationSchema;
import org.apache.inlong.sort.cdc.base.debezium.table.AppendMetadataCollector;
import org.apache.inlong.sort.cdc.base.debezium.table.DeserializationRuntimeConverter;
import org.apache.inlong.sort.cdc.base.debezium.table.DeserializationRuntimeConverterFactory;
import org.apache.inlong.sort.cdc.base.debezium.table.MetadataConverter;
import org.apache.inlong.sort.cdc.base.util.RecordUtils;
import org.apache.inlong.sort.cdc.base.util.TemporalConversions;
import org.apache.kafka.connect.data.ConnectSchema;
import org.apache.kafka.connect.data.Decimal;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deserialization schema from Debezium object to Flink Table/SQL internal data structure {@link
 * RowData}.
 */
public final class RowDataDebeziumDeserializeSchema
        implements
            DebeziumDeserializationSchema<RowData> {

    private static final Logger LOG = LoggerFactory.getLogger(RowDataDebeziumDeserializeSchema.class);

    private static final long serialVersionUID = 2L;

    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_DATE;

    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_TIME;

    private static final ZoneId ZONE_UTC = ZoneId.of("UTC");

    /**
     * TypeInformation of the produced {@link RowData}. *
     */
    private final TypeInformation<RowData> resultTypeInfo;
    /**
     * Runtime converter that converts Kafka {@link SourceRecord}s into {@link RowData} consisted of
     * physical column values.
     */
    private final DeserializationRuntimeConverter physicalConverter;
    /**
     * Whether the deserializer needs to handle metadata columns.
     */
    private final boolean hasMetadata;
    /**
     * Whether works append source.
     */
    private final boolean appendSource;
    /**
     * A wrapped output collector which is used to append metadata columns after physical columns.
     */
    private final AppendMetadataCollector appendMetadataCollector;
    /**
     * Validator to validate the row value.
     */
    private final ValueValidator validator;

    private boolean sourceMultipleEnable;

    private ZoneId serverTimeZone;

    RowDataDebeziumDeserializeSchema(
            RowType physicalDataType,
            MetadataConverter[] metadataConverters,
            TypeInformation<RowData> resultTypeInfo,
            ValueValidator validator,
            ZoneId serverTimeZone,
            boolean appendSource,
            DeserializationRuntimeConverterFactory userDefinedConverterFactory,
            boolean sourceMultipleEnable) {
        this.hasMetadata = checkNotNull(metadataConverters).length > 0;
        this.appendMetadataCollector = new AppendMetadataCollector(metadataConverters, sourceMultipleEnable);
        this.sourceMultipleEnable = sourceMultipleEnable;
        this.serverTimeZone = serverTimeZone;
        this.physicalConverter =
                createConverter(
                        checkNotNull(physicalDataType),
                        serverTimeZone,
                        userDefinedConverterFactory);
        this.resultTypeInfo = checkNotNull(resultTypeInfo);
        this.validator = checkNotNull(validator);
        this.appendSource = checkNotNull(appendSource);
    }

    /**
     * Returns a builder to build {@link RowDataDebeziumDeserializeSchema}.
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    private static DeserializationRuntimeConverter convertToBoolean() {
        return new DeserializationRuntimeConverter() {

            private static final long serialVersionUID = 1L;

            @Override
            public Object convert(Object dbzObj, Schema schema) {
                if (dbzObj instanceof Boolean) {
                    return dbzObj;
                } else if (dbzObj instanceof Byte) {
                    return (byte) dbzObj == 1;
                } else if (dbzObj instanceof Short) {
                    return (short) dbzObj == 1;
                } else {
                    return Boolean.parseBoolean(dbzObj.toString());
                }
            }

            @Override
            public Object convert(Object dbzObj, Schema schema, TableChange tableSchema) throws Exception {
                return convert(dbzObj, schema);
            }
        };
    }

    private static DeserializationRuntimeConverter convertToInt() {
        return new DeserializationRuntimeConverter() {

            private static final long serialVersionUID = 1L;

            @Override
            public Object convert(Object dbzObj, Schema schema) {
                if (dbzObj instanceof Integer) {
                    return dbzObj;
                } else if (dbzObj instanceof Long) {
                    return ((Long) dbzObj).intValue();
                } else {
                    return Integer.parseInt(dbzObj.toString());
                }
            }

            @Override
            public Object convert(Object dbzObj, Schema schema, TableChange tableSchema) throws Exception {
                return convert(dbzObj, schema);
            }
        };
    }

    private static DeserializationRuntimeConverter convertToLong() {
        return new DeserializationRuntimeConverter() {

            private static final long serialVersionUID = 1L;

            @Override
            public Object convert(Object dbzObj, Schema schema) {
                if (dbzObj instanceof Integer) {
                    return ((Integer) dbzObj).longValue();
                } else if (dbzObj instanceof Long) {
                    return dbzObj;
                } else {
                    return Long.parseLong(dbzObj.toString());
                }
            }

            @Override
            public Object convert(Object dbzObj, Schema schema, TableChange tableSchema) throws Exception {
                return convert(dbzObj, schema);
            }
        };
    }

    private static DeserializationRuntimeConverter convertToDouble() {
        return new DeserializationRuntimeConverter() {

            private static final long serialVersionUID = 1L;

            @Override
            public Object convert(Object dbzObj, Schema schema) {
                if (dbzObj instanceof Float) {
                    return ((Float) dbzObj).doubleValue();
                } else if (dbzObj instanceof Double) {
                    return dbzObj;
                } else {
                    return Double.parseDouble(dbzObj.toString());
                }
            }

            @Override
            public Object convert(Object dbzObj, Schema schema, TableChange tableSchema) throws Exception {
                return convert(dbzObj, schema);
            }
        };
    }

    private static DeserializationRuntimeConverter convertToFloat() {
        return new DeserializationRuntimeConverter() {

            private static final long serialVersionUID = 1L;

            @Override
            public Object convert(Object dbzObj, Schema schema) {
                if (dbzObj instanceof Float) {
                    return dbzObj;
                } else if (dbzObj instanceof Double) {
                    return ((Double) dbzObj).floatValue();
                } else {
                    return Float.parseFloat(dbzObj.toString());
                }
            }

            @Override
            public Object convert(Object dbzObj, Schema schema, TableChange tableSchema) throws Exception {
                return convert(dbzObj, schema);
            }
        };
    }

    private static DeserializationRuntimeConverter convertToDate() {
        return new DeserializationRuntimeConverter() {

            private static final long serialVersionUID = 1L;

            @Override
            public Object convert(Object dbzObj, Schema schema) {
                return (int) TemporalConversions.toLocalDate(dbzObj).toEpochDay();
            }

            @Override
            public Object convert(Object dbzObj, Schema schema, TableChange tableSchema) throws Exception {
                return convert(dbzObj, schema);
            }
        };
    }

    private static DeserializationRuntimeConverter convertToTime() {
        return new DeserializationRuntimeConverter() {

            private static final long serialVersionUID = 1L;

            @Override
            public Object convert(Object dbzObj, Schema schema) {
                if (dbzObj instanceof Long) {
                    // Because Oracle CDC has been shaded, the schema will have the prefix
                    // 'org.apache.inlong.sort.cdc.oracle.shaded' added,
                    // so we need to use `schemaName.endsWith()` to determine the Schema type.
                    String schemaName = schema.name();
                    if (schemaName.endsWith(MicroTime.SCHEMA_NAME)) {
                        return (int) ((long) dbzObj / 1000);
                    } else if (schemaName.endsWith(NanoTime.SCHEMA_NAME)) {
                        return (int) ((long) dbzObj / 1000_000);
                    }
                } else if (dbzObj instanceof Integer) {
                    return dbzObj;
                }
                // get number of milliseconds of the day
                return TemporalConversions.toLocalTime(dbzObj).toSecondOfDay() * 1000;
            }

            @Override
            public Object convert(Object dbzObj, Schema schema, TableChange tableSchema) throws Exception {
                return convert(dbzObj, schema);
            }
        };
    }

    // -------------------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------------------

    private static DeserializationRuntimeConverter convertToTimestamp(ZoneId serverTimeZone) {
        return new DeserializationRuntimeConverter() {

            private static final long serialVersionUID = 1L;

            @Override
            public Object convert(Object dbzObj, Schema schema) {
                if (dbzObj instanceof Long) {
                    // Because Oracle CDC has been shaded, the schema will have the prefix
                    // 'org.apache.inlong.sort.cdc.oracle.shaded' added,
                    // so we need to use `schemaName.endsWith()` to determine the Schema type.
                    String schemaName = schema.name();
                    if (schemaName.endsWith(Timestamp.SCHEMA_NAME)) {
                        return TimestampData.fromEpochMillis((Long) dbzObj);
                    } else if (schemaName.endsWith(MicroTimestamp.SCHEMA_NAME)) {
                        long micro = (long) dbzObj;
                        return TimestampData.fromEpochMillis(
                                micro / 1000, (int) (micro % 1000 * 1000));
                    } else if (schemaName.endsWith(NanoTimestamp.SCHEMA_NAME)) {
                        long nano = (long) dbzObj;
                        return TimestampData.fromEpochMillis(
                                nano / 1000_000, (int) (nano % 1000_000));
                    }
                }
                LocalDateTime localDateTime =
                        TemporalConversions.toLocalDateTime(dbzObj, serverTimeZone);
                return TimestampData.fromLocalDateTime(localDateTime);
            }

            @Override
            public Object convert(Object dbzObj, Schema schema, TableChange tableSchema) throws Exception {
                return convert(dbzObj, schema);
            }
        };
    }

    // -------------------------------------------------------------------------------------
    // Runtime Converters
    // -------------------------------------------------------------------------------------

    private static DeserializationRuntimeConverter convertToLocalTimeZoneTimestamp(
            ZoneId serverTimeZone) {
        return new DeserializationRuntimeConverter() {

            private static final long serialVersionUID = 1L;

            @Override
            public Object convert(Object dbzObj, Schema schema) {
                if (dbzObj instanceof String) {
                    String str = (String) dbzObj;
                    // TIMESTAMP_LTZ type is encoded in string type
                    Instant instant = Instant.parse(str);
                    return TimestampData.fromLocalDateTime(
                            LocalDateTime.ofInstant(instant, serverTimeZone));
                }
                throw new IllegalArgumentException(
                        "Unable to convert to TimestampData from unexpected value '"
                                + dbzObj
                                + "' of type "
                                + dbzObj.getClass().getName());
            }

            @Override
            public Object convert(Object dbzObj, Schema schema, TableChange tableSchema) throws Exception {
                return convert(dbzObj, schema);
            }
        };
    }

    // --------------------------------------------------------------------------------
    // IMPORTANT! We use anonymous classes instead of lambdas for a reason here. It is
    // necessary because the maven shade plugin cannot relocate classes in
    // SerializedLambdas (MSHADE-260).
    // --------------------------------------------------------------------------------

    private static DeserializationRuntimeConverter convertToString() {
        return new DeserializationRuntimeConverter() {

            private static final long serialVersionUID = 1L;

            @Override
            public Object convert(Object dbzObj, Schema schema) {
                return StringData.fromString(dbzObj.toString());
            }

            @Override
            public Object convert(Object dbzObj, Schema schema, TableChange tableSchema) throws Exception {
                return convert(dbzObj, schema);
            }
        };
    }

    private static DeserializationRuntimeConverter convertToBinary() {
        return new DeserializationRuntimeConverter() {

            private static final long serialVersionUID = 1L;

            @Override
            public Object convert(Object dbzObj, Schema schema) {
                if (dbzObj instanceof byte[]) {
                    return dbzObj;
                } else if (dbzObj instanceof ByteBuffer) {
                    ByteBuffer byteBuffer = (ByteBuffer) dbzObj;
                    byte[] bytes = new byte[byteBuffer.remaining()];
                    byteBuffer.get(bytes);
                    return bytes;
                } else {
                    throw new UnsupportedOperationException(
                            "Unsupported BYTES value type: " + dbzObj.getClass().getSimpleName());
                }
            }

            @Override
            public Object convert(Object dbzObj, Schema schema, TableChange tableSchema) throws Exception {
                return convert(dbzObj, schema);
            }
        };
    }

    private static DeserializationRuntimeConverter createDecimalConverter(DecimalType decimalType) {
        final int precision = decimalType.getPrecision();
        final int scale = decimalType.getScale();
        return new DeserializationRuntimeConverter() {

            private static final long serialVersionUID = 1L;

            @Override
            public Object convert(Object dbzObj, Schema schema) {
                BigDecimal bigDecimal;
                if (dbzObj instanceof byte[]) {
                    // decimal.handling.mode=precise
                    bigDecimal = Decimal.toLogical(schema, (byte[]) dbzObj);
                } else if (dbzObj instanceof String) {
                    // decimal.handling.mode=string
                    bigDecimal = new BigDecimal((String) dbzObj);
                } else if (dbzObj instanceof Double) {
                    // decimal.handling.mode=double
                    bigDecimal = BigDecimal.valueOf((Double) dbzObj);
                } else {
                    // Because Oracle CDC has been shaded, the schema will have the prefix
                    // 'org.apache.inlong.sort.cdc.oracle.shaded' added,
                    // so we need to use `schemaName.endsWith()` to determine the Schema type.
                    if (schema.name().endsWith(VariableScaleDecimal.LOGICAL_NAME)) {
                        SpecialValueDecimal decimal =
                                VariableScaleDecimal.toLogical((Struct) dbzObj);
                        bigDecimal = decimal.getDecimalValue().orElse(BigDecimal.ZERO);
                    } else {
                        // fallback to string
                        bigDecimal = new BigDecimal(dbzObj.toString());
                    }
                }
                return DecimalData.fromBigDecimal(bigDecimal, precision, scale);
            }

            @Override
            public Object convert(Object dbzObj, Schema schema, TableChange tableSchema) throws Exception {
                return convert(dbzObj, schema);
            }
        };
    }

    private static Object convertField(
            DeserializationRuntimeConverter fieldConverter, Object fieldValue, Schema fieldSchema)
            throws Exception {
        if (fieldValue == null) {
            return null;
        } else {
            return fieldConverter.convert(fieldValue, fieldSchema);
        }
    }

    private static DeserializationRuntimeConverter wrapIntoNullableConverter(
            DeserializationRuntimeConverter converter) {
        return new DeserializationRuntimeConverter() {

            private static final long serialVersionUID = 1L;

            @Override
            public Object convert(Object dbzObj, Schema schema) throws Exception {
                if (dbzObj == null) {
                    return null;
                }
                return converter.convert(dbzObj, schema);
            }

            @Override
            public Object convert(Object dbzObj, Schema schema, TableChange tableSchema) throws Exception {
                if (dbzObj == null) {
                    return null;
                }
                return converter.convert(dbzObj, schema, tableSchema);
            }
        };
    }

    /**
     * Creates a runtime converter which is null safe.
     */
    private DeserializationRuntimeConverter createConverter(
            LogicalType type,
            ZoneId serverTimeZone,
            DeserializationRuntimeConverterFactory userDefinedConverterFactory) {
        return wrapIntoNullableConverter(
                createNotNullConverter(type, serverTimeZone, userDefinedConverterFactory));
    }

    /**
     * Creates a runtime converter which assuming input object is not null.
     */
    public DeserializationRuntimeConverter createNotNullConverter(
            LogicalType type,
            ZoneId serverTimeZone,
            DeserializationRuntimeConverterFactory userDefinedConverterFactory) {
        // user defined converter has a higher resolve order
        Optional<DeserializationRuntimeConverter> converter =
                userDefinedConverterFactory.createUserDefinedConverter(type, serverTimeZone);
        if (converter.isPresent()) {
            return converter.get();
        }

        // if no matched user defined converter, fallback to the default converter
        switch (type.getTypeRoot()) {
            case NULL:
                return new DeserializationRuntimeConverter() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public Object convert(Object dbzObj, Schema schema) {
                        return null;
                    }

                    @Override
                    public Object convert(Object dbzObj, Schema schema, TableChange tableSchema) throws Exception {
                        return convert(dbzObj, schema);
                    }
                };
            case BOOLEAN:
                return convertToBoolean();
            case TINYINT:
                return new DeserializationRuntimeConverter() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public Object convert(Object dbzObj, Schema schema) {
                        return Byte.parseByte(dbzObj.toString());
                    }

                    @Override
                    public Object convert(Object dbzObj, Schema schema, TableChange tableSchema) throws Exception {
                        return convert(dbzObj, schema);
                    }
                };
            case SMALLINT:
                return new DeserializationRuntimeConverter() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public Object convert(Object dbzObj, Schema schema) {
                        return Short.parseShort(dbzObj.toString());
                    }

                    @Override
                    public Object convert(Object dbzObj, Schema schema, TableChange tableSchema) throws Exception {
                        return convert(dbzObj, schema);
                    }
                };
            case INTEGER:
            case INTERVAL_YEAR_MONTH:
                return convertToInt();
            case BIGINT:
            case INTERVAL_DAY_TIME:
                return convertToLong();
            case DATE:
                return convertToDate();
            case TIME_WITHOUT_TIME_ZONE:
                return convertToTime();
            case TIMESTAMP_WITHOUT_TIME_ZONE:
                return convertToTimestamp(serverTimeZone);
            case TIMESTAMP_WITH_LOCAL_TIME_ZONE:
                return convertToLocalTimeZoneTimestamp(serverTimeZone);
            case FLOAT:
                return convertToFloat();
            case DOUBLE:
                return convertToDouble();
            case CHAR:
            case VARCHAR:
                return convertToString();
            case BINARY:
            case VARBINARY:
                return convertToBinary();
            case DECIMAL:
                return createDecimalConverter((DecimalType) type);
            case ROW:
                return createRowConverter(
                        (RowType) type, serverTimeZone, userDefinedConverterFactory);
            case ARRAY:
            case MAP:
            case MULTISET:
            case RAW:
            default:
                throw new UnsupportedOperationException("Unsupported type: " + type);
        }
    }

    private DeserializationRuntimeConverter createRowConverter(
            RowType rowType,
            ZoneId serverTimeZone,
            DeserializationRuntimeConverterFactory userDefinedConverterFactory) {
        final DeserializationRuntimeConverter[] fieldConverters =
                rowType.getFields().stream()
                        .map(RowType.RowField::getType)
                        .map(
                                logicType -> createConverter(
                                        logicType,
                                        serverTimeZone,
                                        userDefinedConverterFactory))
                        .toArray(DeserializationRuntimeConverter[]::new);
        final String[] fieldNames = rowType.getFieldNames().toArray(new String[0]);

        if (!sourceMultipleEnable) {
            return new DeserializationRuntimeConverter() {

                private static final long serialVersionUID = 1L;

                @Override
                public Object convert(Object dbzObj, Schema schema) throws Exception {
                    Struct struct = (Struct) dbzObj;
                    int arity = fieldNames.length;
                    GenericRowData row = new GenericRowData(arity);
                    for (int i = 0; i < arity; i++) {
                        String fieldName = fieldNames[i];
                        Field field = schema.field(fieldName);
                        if (field == null) {
                            row.setField(i, null);
                        } else {
                            Object fieldValue = struct.getWithoutDefault(fieldName);
                            Schema fieldSchema = schema.field(fieldName).schema();
                            Object convertedField =
                                    convertField(fieldConverters[i], fieldValue, fieldSchema);
                            row.setField(i, convertedField);
                        }
                    }
                    return row;
                }

                @Override
                public Object convert(Object dbzObj, Schema schema, TableChange tableSchema) throws Exception {
                    return convert(dbzObj, schema);
                }
            };
        } else {
            return getMultipleMigrationConverter(serverTimeZone, userDefinedConverterFactory);
        }
    }

    private DeserializationRuntimeConverter getMultipleMigrationConverter(
            ZoneId serverTimeZone, DeserializationRuntimeConverterFactory userDefinedConverterFactory) {
        return new DeserializationRuntimeConverter() {

            private static final long serialVersionUID = 1L;

            @Override
            public Object convert(Object dbzObj, Schema schema) throws Exception {
                ConnectSchema connectSchema = (ConnectSchema) schema;
                List<Field> fields = connectSchema.fields();

                Map<String, Object> data = new HashMap<>();
                Struct struct = (Struct) dbzObj;

                for (Field field : fields) {
                    String fieldName = field.name();
                    Object fieldValue = struct.getWithoutDefault(fieldName);
                    Schema fieldSchema = schema.field(fieldName).schema();
                    String schemaName = fieldSchema.name();
                    if (schemaName != null) {
                        // normal type doesn't have schema name
                        // schema names are time schemas
                        fieldValue = getValueWithSchema(fieldValue, schemaName);
                    }
                    data.put(fieldName, fieldValue);
                }

                GenericRowData row = new GenericRowData(1);
                row.setField(0, data);

                return row;
            }

            @Override
            public Object convert(Object dbzObj, Schema schema, TableChange tableSchema) throws Exception {
                ConnectSchema connectSchema = (ConnectSchema) schema;
                List<Field> fields = connectSchema.fields();

                Map<String, Object> data = new HashMap<>();
                Struct struct = (Struct) dbzObj;

                for (Field field : fields) {
                    String fieldName = field.name();
                    Object fieldValue = struct.getWithoutDefault(fieldName);
                    Schema fieldSchema = schema.field(fieldName).schema();
                    String schemaName = fieldSchema.name();

                    // struct type convert normal type
                    if (fieldValue instanceof Struct) {
                        Column column = tableSchema.getTable().columnWithName(fieldName);
                        LogicalType logicType = RecordUtils.convertLogicType(column, (Struct) fieldValue);
                        DeserializationRuntimeConverter fieldConverter = createConverter(
                                logicType,
                                serverTimeZone,
                                userDefinedConverterFactory);
                        fieldValue =
                                convertField(fieldConverter, fieldValue, fieldSchema);
                        if (fieldValue instanceof DecimalData) {
                            fieldValue = ((DecimalData) fieldValue).toBigDecimal();
                        }
                        if (fieldValue instanceof TimestampData) {
                            fieldValue = ((TimestampData) fieldValue).toTimestamp();
                        }
                    }
                    if (schemaName != null) {
                        fieldValue = getValueWithSchema(fieldValue, schemaName);
                    }
                    if (fieldValue instanceof ByteBuffer) {
                        fieldValue = new String(((ByteBuffer) fieldValue).array());
                    }

                    data.put(fieldName, fieldValue);
                }

                GenericRowData row = new GenericRowData(1);
                row.setField(0, data);

                return row;
            }
        };
    }

    /**
     * extract the data with the format provided by debezium
     *
     * @param fieldValue
     * @param schemaName
     * @return the extracted data with schema
     */
    private Object getValueWithSchema(Object fieldValue, String schemaName) {
        if (fieldValue == null) {
            return null;
        }
        // Because Oracle CDC has been shaded, the schema will have the prefix
        // 'org.apache.inlong.sort.cdc.oracle.shaded' added,
        // so we need to use `schemaName.endsWith()` to determine the Schema type.
        if (schemaName.endsWith(MicroTime.SCHEMA_NAME)) {
            Instant instant = Instant.ofEpochMilli((Long) fieldValue / 1000);
            fieldValue = timeFormatter.format(LocalDateTime.ofInstant(instant, ZONE_UTC));
        } else if (schemaName.endsWith(Date.SCHEMA_NAME)) {
            fieldValue = dateFormatter.format(LocalDate.ofEpochDay((Integer) fieldValue));
        } else if (schemaName.endsWith(ZonedTimestamp.SCHEMA_NAME)) {
            ZonedDateTime zonedDateTime = ZonedDateTime.parse((CharSequence) fieldValue);
            fieldValue = zonedDateTime.withZoneSameInstant(serverTimeZone).toLocalDateTime()
                    .atZone(ZONE_UTC).format(DateTimeFormatter.ISO_INSTANT);
        } else if (schemaName.endsWith(Timestamp.SCHEMA_NAME)) {
            Instant instantTime = Instant.ofEpochMilli((Long) fieldValue);
            fieldValue = LocalDateTime.ofInstant(instantTime, ZONE_UTC).toString();
        } else if (schemaName.endsWith(MicroTimestamp.SCHEMA_NAME)) {
            Instant instantTime = Instant.ofEpochMilli((Long) fieldValue / 1000);
            fieldValue = LocalDateTime.ofInstant(instantTime, ZONE_UTC).toString();
        }
        return fieldValue;
    }

    @Override
    public void deserialize(SourceRecord record, Collector<RowData> out) throws Exception {
        deserialize(record, out);
    }

    @Override
    public void deserialize(SourceRecord record, Collector<RowData> out,
            TableChange tableSchema)
            throws Exception {
        Envelope.Operation op = Envelope.operationFor(record);
        Struct value = (Struct) record.value();
        Schema valueSchema = record.valueSchema();
        if (op == Envelope.Operation.CREATE || op == Envelope.Operation.READ) {
            GenericRowData insert = extractAfterRow(value, valueSchema, tableSchema);
            validator.validate(insert, RowKind.INSERT);
            insert.setRowKind(RowKind.INSERT);
            emit(record, insert, tableSchema, out);
        } else if (op == Envelope.Operation.DELETE) {
            GenericRowData delete = extractBeforeRow(value, valueSchema, tableSchema);
            validator.validate(delete, RowKind.DELETE);
            delete.setRowKind(RowKind.DELETE);
            emit(record, delete, tableSchema, out);
        } else {
            if (!appendSource) {
                GenericRowData before = extractBeforeRow(value, valueSchema, tableSchema);
                validator.validate(before, RowKind.UPDATE_BEFORE);
                before.setRowKind(RowKind.UPDATE_BEFORE);
                emit(record, before, tableSchema, out);
            }

            GenericRowData after = extractAfterRow(value, valueSchema, tableSchema);
            validator.validate(after, RowKind.UPDATE_AFTER);
            after.setRowKind(RowKind.UPDATE_AFTER);
            emit(record, after, tableSchema, out);
        }
    }

    private GenericRowData extractAfterRow(Struct value, Schema valueSchema,
            TableChange tableSchema) throws Exception {
        Schema afterSchema = valueSchema.field(Envelope.FieldName.AFTER).schema();
        Struct after = value.getStruct(Envelope.FieldName.AFTER);
        return (GenericRowData) physicalConverter.convert(after, afterSchema, tableSchema);
    }

    private GenericRowData extractBeforeRow(Struct value, Schema valueSchema,
            TableChange tableSchema) throws Exception {
        Schema beforeSchema = valueSchema.field(Envelope.FieldName.BEFORE).schema();
        Struct before = value.getStruct(Envelope.FieldName.BEFORE);
        return (GenericRowData) physicalConverter.convert(before, beforeSchema, tableSchema);
    }

    private void emit(SourceRecord inRecord, RowData physicalRow,
            TableChange tableChange, Collector<RowData> collector) {
        if (appendSource) {
            physicalRow.setRowKind(RowKind.INSERT);
        }
        if (!hasMetadata) {
            collector.collect(physicalRow);
            return;
        }

        appendMetadataCollector.inputRecord = inRecord;
        appendMetadataCollector.outputCollector = collector;
        appendMetadataCollector.collect(physicalRow, tableChange);
    }

    @Override
    public TypeInformation<RowData> getProducedType() {
        return resultTypeInfo;
    }

    /**
     * Custom validator to validate the row value.
     */
    public interface ValueValidator extends Serializable {

        void validate(RowData rowData, RowKind rowKind) throws Exception;
    }

    /**
     * Builder of {@link RowDataDebeziumDeserializeSchema}.
     */
    public static class Builder {

        private RowType physicalRowType;
        private TypeInformation<RowData> resultTypeInfo;
        private MetadataConverter[] metadataConverters = new MetadataConverter[0];
        private ValueValidator validator = (rowData, rowKind) -> {
        };
        private ZoneId serverTimeZone = ZoneId.of("UTC");
        private boolean appendSource = false;
        private boolean sourceMultipleEnable = false;
        private DeserializationRuntimeConverterFactory userDefinedConverterFactory =
                DeserializationRuntimeConverterFactory.DEFAULT;

        public Builder setPhysicalRowType(RowType physicalRowType) {
            this.physicalRowType = physicalRowType;
            return this;
        }

        public Builder setSourceMultipleEnable(boolean sourceMultipleEnable) {
            this.sourceMultipleEnable = sourceMultipleEnable;
            return this;
        }

        public Builder setMetadataConverters(MetadataConverter[] metadataConverters) {
            this.metadataConverters = metadataConverters;
            return this;
        }

        public Builder setResultTypeInfo(TypeInformation<RowData> resultTypeInfo) {
            this.resultTypeInfo = resultTypeInfo;
            return this;
        }

        public Builder setValueValidator(ValueValidator validator) {
            this.validator = validator;
            return this;
        }

        public Builder setServerTimeZone(ZoneId serverTimeZone) {
            this.serverTimeZone = serverTimeZone;
            return this;
        }

        public Builder setAppendSource(boolean appendSource) {
            this.appendSource = appendSource;
            return this;
        }

        public Builder setUserDefinedConverterFactory(
                DeserializationRuntimeConverterFactory userDefinedConverterFactory) {
            this.userDefinedConverterFactory = userDefinedConverterFactory;
            return this;
        }

        public RowDataDebeziumDeserializeSchema build() {
            return new RowDataDebeziumDeserializeSchema(
                    physicalRowType,
                    metadataConverters,
                    resultTypeInfo,
                    validator,
                    serverTimeZone,
                    appendSource,
                    userDefinedConverterFactory,
                    sourceMultipleEnable);
        }
    }
}
