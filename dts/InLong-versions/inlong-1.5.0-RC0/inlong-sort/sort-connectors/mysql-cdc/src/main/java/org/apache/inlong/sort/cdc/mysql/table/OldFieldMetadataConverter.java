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

package org.apache.inlong.sort.cdc.mysql.table;

import io.debezium.data.Envelope;
import io.debezium.data.SpecialValueDecimal;
import io.debezium.data.VariableScaleDecimal;
import io.debezium.time.MicroTime;
import io.debezium.time.MicroTimestamp;
import io.debezium.time.NanoTime;
import io.debezium.time.NanoTimestamp;
import io.debezium.time.Timestamp;
import org.apache.flink.table.data.DecimalData;
import org.apache.flink.table.data.GenericArrayData;
import org.apache.flink.table.data.GenericMapData;
import org.apache.flink.table.data.StringData;
import org.apache.flink.table.data.TimestampData;
import org.apache.flink.table.types.logical.DecimalType;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.table.types.logical.RowType;
import org.apache.inlong.sort.cdc.base.debezium.table.MetadataConverter;
import org.apache.inlong.sort.cdc.base.util.TemporalConversions;
import org.apache.kafka.connect.data.Decimal;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;

/**
 * A {@link MetadataConverter} for {@link MySqlReadableMetadata#OLD}.
 */
public class OldFieldMetadataConverter implements MetadataConverter {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(OldFieldMetadataConverter.class);

    /**
     * Formatter for SQL string representation of a time value.
     */
    private static final DateTimeFormatter SQL_TIME_FORMAT =
            new DateTimeFormatterBuilder()
                    .appendPattern("HH:mm:ss")
                    .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
                    .toFormatter();

    /**
     * Formatter for SQL string representation of a timestamp value (without UTC timezone).
     */
    private static final DateTimeFormatter SQL_TIMESTAMP_FORMAT =
            new DateTimeFormatterBuilder()
                    .append(DateTimeFormatter.ISO_LOCAL_DATE)
                    .appendLiteral(' ')
                    .append(SQL_TIME_FORMAT)
                    .toFormatter();

    /**
     * Formatter for SQL string representation of a timestamp value (with UTC timezone).
     */
    private static final DateTimeFormatter SQL_TIMESTAMP_WITH_LOCAL_TIMEZONE_FORMAT =
            new DateTimeFormatterBuilder()
                    .append(DateTimeFormatter.ISO_LOCAL_DATE)
                    .appendLiteral(' ')
                    .append(SQL_TIME_FORMAT)
                    .appendPattern("'Z'")
                    .toFormatter();

    private final ZoneId serverTimeZone;
    private final MetadataConverter converter;
    private final StringConverter toStringConverter;
    private final String[] fieldNames;
    private final StringConverter[] stringConverters;

    public OldFieldMetadataConverter(RowType rowType, ZoneId serverTimeZone) {
        this.serverTimeZone = serverTimeZone;
        this.converter = MySqlReadableMetadata.OLD.getConverter();
        this.toStringConverter = new ToStringConverter();
        this.fieldNames = rowType.getFieldNames().toArray(new String[0]);
        this.stringConverters =
                rowType.getChildren().stream()
                        .map(this::createConverter)
                        .toArray(StringConverter[]::new);
    }

    @Override
    public Object read(SourceRecord record) {
        Object obj = converter.read(record);
        if (obj == null) {
            return null;
        }
        Struct value = (Struct) record.value();
        Schema valueSchema = record.valueSchema();
        Schema beforeSchema = valueSchema.field(Envelope.FieldName.BEFORE).schema();
        Struct before = value.getStruct(Envelope.FieldName.BEFORE);

        Map<StringData, StringData> oldData = new HashMap<>();
        for (int i = 0; i < this.fieldNames.length; i++) {
            final String fieldName = this.fieldNames[i];
            final StringConverter stringConverter = this.stringConverters[i];
            final Field field = beforeSchema.field(fieldName);

            if (field == null) {
                oldData.put(StringData.fromString(fieldName), null);
            } else {
                final Object fieldValue = before.get(field);
                final Schema fieldSchema = field.schema();
                StringData strFieldValue = null;
                try {
                    final String str = stringConverter.convert(fieldValue, fieldSchema);
                    strFieldValue = StringData.fromString(str);
                } catch (Exception e) {
                    LOG.error(
                            "Failed to convert value "
                                    + fieldValue
                                    + " ("
                                    + fieldSchema.name()
                                    + ") to string.");
                }

                oldData.put(StringData.fromString(fieldName), strFieldValue);
            }
        }

        return new GenericArrayData(new Object[]{new GenericMapData(oldData)});
    }

    private StringConverter createConverter(LogicalType type) {
        return wrapIntoNullableConverter(createNotNullConverter(type));
    }

    private StringConverter createNotNullConverter(LogicalType type) {
        switch (type.getTypeRoot()) {
            case NULL:
                return new StringConverter() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public String convert(Object dbzObj, Schema schema) throws Exception {
                        return null;
                    }
                };
            case BOOLEAN:
            case TINYINT:
            case SMALLINT:
            case INTEGER:
            case INTERVAL_YEAR_MONTH:
            case BIGINT:
            case INTERVAL_DAY_TIME:
                return toStringConverter;
            case DATE:
                return createDateConverter();
            case TIME_WITHOUT_TIME_ZONE:
                return createTimeConverter();
            case TIMESTAMP_WITHOUT_TIME_ZONE:
                return createTimestampConverter();
            case TIMESTAMP_WITH_LOCAL_TIME_ZONE:
                return createLocalTimeZoneTimestampConverter();
            case FLOAT:
            case DOUBLE:
            case CHAR:
            case VARCHAR:
                return toStringConverter;
            case BINARY:
            case VARBINARY:
                return createBinaryConverter();
            case DECIMAL:
                return createDecimalConverter((DecimalType) type);
            case ROW:
            case ARRAY:
            case MAP:
            case MULTISET:
            case RAW:
            default:
                throw new UnsupportedOperationException("Unsupported type: " + type);
        }
    }

    private StringConverter createDateConverter() {
        return new StringConverter() {

            private static final long serialVersionUID = 1L;

            @Override
            public String convert(Object dbzObj, Schema schema) throws Exception {
                return ISO_LOCAL_DATE.format(TemporalConversions.toLocalDate(dbzObj));
            }
        };
    }

    // --------------------------------------------------------------------------------
    // IMPORTANT! We use anonymous classes instead of lambdas for a reason here. It is
    // necessary because the maven shade plugin cannot relocate classes in
    // SerializedLambdas (MSHADE-260).
    // --------------------------------------------------------------------------------

    private StringConverter createTimeConverter() {
        return new StringConverter() {

            private static final long serialVersionUID = 1L;

            @Override
            public String convert(Object dbzObj, Schema schema) throws Exception {
                final int millisecond = toMillisecond(dbzObj, schema);
                LocalTime time = LocalTime.ofSecondOfDay(millisecond / 1000L);
                return SQL_TIME_FORMAT.format(time);
            }

            private int toMillisecond(Object dbzObj, Schema schema) {
                if (dbzObj instanceof Long) {
                    switch (schema.name()) {
                        case MicroTime.SCHEMA_NAME:
                            return (int) ((long) dbzObj / 1000);
                        case NanoTime.SCHEMA_NAME:
                            return (int) ((long) dbzObj / 1000_000);
                        default:
                            break;
                    }
                } else if (dbzObj instanceof Integer) {
                    return (Integer) dbzObj;
                }
                // get number of milliseconds of the day
                return TemporalConversions.toLocalTime(dbzObj).toSecondOfDay() * 1000;
            }
        };
    }

    private StringConverter createTimestampConverter() {
        return new StringConverter() {

            private static final long serialVersionUID = 1L;

            @Override
            public String convert(Object dbzObj, Schema schema) throws Exception {
                return SQL_TIMESTAMP_FORMAT.format(
                        toTimestampData(dbzObj, schema).toLocalDateTime());
            }

            private TimestampData toTimestampData(Object dbzObj, Schema schema) throws Exception {
                if (dbzObj instanceof Long) {
                    switch (schema.name()) {
                        case Timestamp.SCHEMA_NAME:
                            return TimestampData.fromEpochMillis((Long) dbzObj);
                        case MicroTimestamp.SCHEMA_NAME:
                            long micro = (long) dbzObj;
                            return TimestampData.fromEpochMillis(
                                    micro / 1000, (int) (micro % 1000 * 1000));
                        case NanoTimestamp.SCHEMA_NAME:
                            long nano = (long) dbzObj;
                            return TimestampData.fromEpochMillis(
                                    nano / 1000_000, (int) (nano % 1000_000));
                        default:
                            break;
                    }
                }
                LocalDateTime localDateTime =
                        TemporalConversions.toLocalDateTime(dbzObj, serverTimeZone);
                return TimestampData.fromLocalDateTime(localDateTime);
            }
        };
    }

    private StringConverter createLocalTimeZoneTimestampConverter() {
        return new StringConverter() {

            private static final long serialVersionUID = 1L;

            @Override
            public String convert(Object dbzObj, Schema schema) throws Exception {
                return SQL_TIMESTAMP_WITH_LOCAL_TIMEZONE_FORMAT.format(
                        toTimestampData(dbzObj, schema).toInstant().atOffset(ZoneOffset.UTC));
            }

            private TimestampData toTimestampData(Object dbzObj, Schema schema) throws Exception {
                if (dbzObj instanceof String) {
                    String str = (String) dbzObj;
                    // TIMESTAMP type is encoded in string type
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
        };
    }

    private StringConverter createBinaryConverter() {
        return new StringConverter() {

            private static final long serialVersionUID = 1L;

            @Override
            public String convert(Object dbzObj, Schema schema) throws Exception {
                final Base64.Encoder base64Encoder = Base64.getEncoder();
                return base64Encoder.encodeToString(convertToBinary(dbzObj, schema));
            }

            private byte[] convertToBinary(Object dbzObj, Schema schema) {
                if (dbzObj instanceof byte[]) {
                    return (byte[]) dbzObj;
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
        };
    }

    private StringConverter createDecimalConverter(DecimalType decimalType) {
        final int precision = decimalType.getPrecision();
        final int scale = decimalType.getScale();

        return new StringConverter() {

            private static final long serialVersionUID = 1L;

            @Override
            public String convert(Object dbzObj, Schema schema) throws Exception {
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
                    if (VariableScaleDecimal.LOGICAL_NAME.equals(schema.name())) {
                        SpecialValueDecimal decimal =
                                VariableScaleDecimal.toLogical((Struct) dbzObj);
                        bigDecimal = decimal.getDecimalValue().orElse(BigDecimal.ZERO);
                    } else {
                        // fallback to string
                        bigDecimal = new BigDecimal(dbzObj.toString());
                    }
                }
                return DecimalData.fromBigDecimal(bigDecimal, precision, scale)
                        .toBigDecimal()
                        .toString();
            }
        };
    }

    private StringConverter wrapIntoNullableConverter(StringConverter converter) {
        return new StringConverter() {

            private static final long serialVersionUID = 1L;

            @Override
            public String convert(Object dbzObj, Schema schema) throws Exception {
                if (dbzObj == null) {
                    return null;
                }
                return converter.convert(dbzObj, schema);
            }
        };
    }

    /**
     * Converter that converts objects of Debezium to String.
     */
    @FunctionalInterface
    private interface StringConverter extends Serializable {

        String convert(Object dbzObj, Schema schema) throws Exception;
    }

    private static class ToStringConverter implements StringConverter {

        private static final long serialVersionUID = 1L;

        @Override
        public String convert(Object dbzObj, Schema schema) throws Exception {
            return dbzObj.toString();
        }
    }
}
