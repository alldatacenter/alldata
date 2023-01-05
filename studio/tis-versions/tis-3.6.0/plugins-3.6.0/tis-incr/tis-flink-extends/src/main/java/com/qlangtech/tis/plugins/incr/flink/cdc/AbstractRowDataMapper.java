/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.tis.plugins.incr.flink.cdc;

import com.qlangtech.plugins.incr.flink.cdc.BiFunction;
import com.qlangtech.plugins.incr.flink.cdc.FlinkCol;
import com.qlangtech.plugins.incr.flink.cdc.RowFieldGetterFactory;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.datax.IStreamTableMeataCreator;
import com.qlangtech.tis.plugin.ds.DataType;
import com.qlangtech.tis.plugin.ds.IColMetaGetter;
import com.qlangtech.tis.plugins.incr.flink.FlinkColMapper;
import com.qlangtech.tis.realtime.BasicFlinkSourceHandle;
import com.qlangtech.tis.realtime.transfer.DTO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.data.DecimalData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;
import org.apache.flink.table.data.TimestampData;
import org.apache.flink.table.runtime.functions.SqlDateTimeUtils;
import org.apache.flink.table.types.AtomicDataType;
import org.apache.flink.table.types.logical.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-02-18 12:04
 **/
public abstract class AbstractRowDataMapper implements MapFunction<DTO, RowData>, Serializable {
    protected final List<FlinkCol> cols;


    public AbstractRowDataMapper(List<FlinkCol> cols) {
        if (CollectionUtils.isEmpty(cols)) {
            throw new IllegalArgumentException("param cols can not be empty");
        }
        this.cols = cols;
    }

    public static List<FlinkCol> getAllTabColsMeta(TargetResName dataxName, String tabName) {
        IStreamTableMeataCreator.IStreamTableMeta streamTableMeta = BasicFlinkSourceHandle.getStreamTableMeta(dataxName, tabName);
        return getAllTabColsMeta(streamTableMeta);
    }

    public static List<FlinkCol> getAllTabColsMeta(IStreamTableMeataCreator.IStreamTableMeta streamTableMeta) {
        final AtomicInteger colIndex = new AtomicInteger();
        return streamTableMeta.getColsMeta()
                .stream()
                .map((c) -> mapFlinkCol(c, colIndex.getAndDecrement()))
                .collect(Collectors.toList());
    }

    public static <T extends IColMetaGetter> List<FlinkCol> getAllTabColsMeta(List<T> colsMeta) {
        return getAllTabColsMeta(colsMeta, AbstractRowDataMapper::mapFlinkCol);
    }


    public static <T extends IColMetaGetter> List<FlinkCol> getAllTabColsMeta(List<T> colsMeta, IFlinkColCreator flinkColCreator) {
        final AtomicInteger colIndex = new AtomicInteger();
        return colsMeta.stream()
                .map((c) -> flinkColCreator.build(c, colIndex.getAndIncrement()))
                .collect(Collectors.toList());
    }

    public static <T extends IColMetaGetter> FlinkColMapper getAllTabColsMetaMapper(List<T> colsMeta) {
        return getAllTabColsMetaMapper(colsMeta, AbstractRowDataMapper::mapFlinkCol);
    }

    public static <T extends IColMetaGetter> FlinkColMapper getAllTabColsMetaMapper(List<T> colsMeta, IFlinkColCreator flinkColCreator) {
        List<FlinkCol> cols = getAllTabColsMeta(colsMeta, flinkColCreator);
        return new FlinkColMapper(cols.stream().collect(Collectors.toMap((c) -> c.name, (c) -> c)));
    }

    public static class DefaultTypeVisitor implements DataType.TypeVisitor<FlinkCol>, Serializable {
        protected final IColMetaGetter meta;
        protected final int colIndex;
        protected boolean nullable;

        public DefaultTypeVisitor(IColMetaGetter meta, int colIndex) {
            this.meta = meta;
            this.colIndex = colIndex;
            this.nullable = !meta.isPk();
        }

        @Override
        public FlinkCol intType(DataType type) {
            return new FlinkCol(meta, type
                    , new AtomicDataType(new IntType(nullable)), new IntegerConvert()
                    , RowFieldGetterFactory.intGetter(meta.getName(), colIndex));
        }


        @Override
        public FlinkCol smallIntType(DataType dataType) {
            return new FlinkCol(meta, dataType,
                    new AtomicDataType(new SmallIntType(nullable))
                    , new ShortConvert()
                    , new RowShortConvert()
                    , RowFieldGetterFactory.smallIntGetter(meta.getName(), colIndex));
        }


        @Override
        public FlinkCol tinyIntType(DataType dataType) {
            return new FlinkCol(meta, dataType,
                    new AtomicDataType(new TinyIntType(nullable))
                    //         , DataTypes.TINYINT()
                    , new TinyIntConvertByte()
                    , new TinyIntConvertByte()
                    , new RowFieldGetterFactory.ByteGetter(meta.getName(), colIndex));
        }


        @Override
        public FlinkCol floatType(DataType type) {
            return new FlinkCol(meta, type
                    , DataTypes.FLOAT()
                    , new FloatDataConvert()
                    , new FloatDataConvert()
                    , new RowFieldGetterFactory.FloatGetter(meta.getName(), colIndex));
        }


        /**
         * <pre>
         *
         * 由于报一下错误，将DataTypes.TIME(3) 改成 DataTypes.TIME()
         *
         * Caused by: org.apache.flink.table.api.ValidationException: Type TIME(3) of table field 'time_c' does not match with the physical type TIME(0) of the 'time_c' field of the TableSink consumed type.
         * at org.apache.flink.table.utils.TypeMappingUtils.lambda$checkPhysicalLogicalTypeCompatible$5(TypeMappingUtils.java:190)
         * at org.apache.flink.table.utils.TypeMappingUtils$1.defaultMethod(TypeMappingUtils.java:326)
         * at org.apache.flink.table.utils.TypeMappingUtils$1.defaultMethod(TypeMappingUtils.java:291)
         * at org.apache.flink.table.types.logical.utils.LogicalTypeDefaultVisitor.visit(LogicalTypeDefaultVisitor.java:127)
         * at org.apache.flink.table.types.logical.TimeType.accept(TimeType.java:134)
         * at org.apache.flink.table.utils.TypeMappingUtils.checkIfCompatible(TypeMappingUtils.java:290)
         * at org.apache.flink.table.utils.TypeMappingUtils.checkPhysicalLogicalTypeCompatible(TypeMappingUtils.
         *
         * </pre>
         *
         * @param type
         * @return
         */
        @Override
        public FlinkCol timeType(DataType type) {
            return new FlinkCol(meta //
                    , type
                    // , DataTypes.TIME(3) //
                    , DataTypes.TIME()
                    , new DTOLocalTimeConvert()
                    , new LocalTimeConvert()
                    // , (rowData) -> Time.valueOf(LocalTime.ofNanoOfDay(rowData.getInt(colIndex) * 1_000_000L))
                    , new RowFieldGetterFactory.TimeGetter(meta.getName(), colIndex));
        }


        @Override
        public FlinkCol bigInt(DataType type) {
            return new FlinkCol(meta
                    , type
                    , new AtomicDataType(new BigIntType(nullable))
                    // , DataTypes.BIGINT()
                    , new LongConvert()
                    , new RowFieldGetterFactory.BigIntGetter(meta.getName(), colIndex));
        }


        public FlinkCol decimalType(DataType type) {
            int precision = type.columnSize;
            Integer scale = type.getDecimalDigits();
            if (precision < 1 || precision > 38) {
                precision = 38;
            }
            try {
                return new FlinkCol(meta, type, DataTypes.DECIMAL(precision, scale)
                        , new DecimalConvert(precision, scale)
                        , FlinkCol.NoOp()
                        , new RowFieldGetterFactory.DecimalGetter(meta.getName(), colIndex));
            } catch (Exception e) {
                throw new RuntimeException("colName:" + meta.getName() + ",type:" + type.toString() + ",precision:" + precision + ",scale:" + scale, e);
            }
        }


        @Override
        public FlinkCol doubleType(DataType type) {
            return new FlinkCol(meta, type
                    , DataTypes.DOUBLE()
                    , new RowFieldGetterFactory.DoubleGetter(meta.getName(), colIndex));
        }

        @Override
        public FlinkCol dateType(DataType type) {
            return new FlinkCol(meta, type, DataTypes.DATE()
                    , new DateConvert()
                    , FlinkCol.LocalDate()
                    , new RowFieldGetterFactory.DateGetter(meta.getName(), colIndex));
        }

        @Override
        public FlinkCol timestampType(DataType type) {
            return new FlinkCol(meta, type, DataTypes.TIMESTAMP(3)
                    , new TimestampDataConvert()
                    , new FlinkCol.DateTimeProcess()
                    , new RowFieldGetterFactory.TimestampGetter(meta.getName(), colIndex));
        }

        @Override
        public FlinkCol bitType(DataType type) {
            return new FlinkCol(meta, type, DataTypes.TINYINT()
                    , FlinkCol.Byte()
                    , new RowFieldGetterFactory.ByteGetter(meta.getName(), colIndex));
        }

        @Override
        public FlinkCol boolType(DataType dataType) {
            FlinkCol fcol = new FlinkCol(meta, dataType, DataTypes.BOOLEAN()
                    , new FlinkCol.BoolProcess()
                    , new RowFieldGetterFactory.BoolGetter(meta.getName(), colIndex));
            return fcol.setSourceDTOColValProcess(new BiFunction() {
                @Override
                public Object apply(Object o) {
                    if (o instanceof Number) {
                        return ((Number) o).shortValue() > 0;
                    }
                    return (Boolean) o;
                }
            });
        }


        @Override
        public FlinkCol blobType(DataType type) {
            FlinkCol col = new FlinkCol(meta, type, DataTypes.BYTES()
                    , new BinaryRawValueDataConvert()
                    , new RowFieldGetterFactory.BlobGetter(meta.getName(), colIndex));
            return col.setSourceDTOColValProcess(new BinaryRawValueDTOConvert());
        }


        @Override
        public FlinkCol varcharType(DataType type) {
            return new FlinkCol(meta //
                    , type
                    , new AtomicDataType(new VarCharType(nullable, type.columnSize))
                    //, DataTypes.VARCHAR(type.columnSize)
                    , new StringConvert()
                    , FlinkCol.NoOp()
                    , new RowFieldGetterFactory.StringGetter(meta.getName(), colIndex));
        }


    }

    public static FlinkCol mapFlinkCol(IColMetaGetter meta, int colIndex) {
        return meta.getType().accept(new DefaultTypeVisitor(meta, colIndex));
    }

    public interface IFlinkColCreator {
        FlinkCol build(IColMetaGetter meta, int colIndex);
    }


    @Override
    public RowData map(DTO dto) throws Exception {
        RowData row = createRowData(dto);

        Map<String, Object> vals
                = (dto.getEventType() == DTO.EventType.DELETE || dto.getEventType() == DTO.EventType.UPDATE_BEFORE)
                ? dto.getBefore() : dto.getAfter();
        if (vals == null) {
            throw new IllegalStateException("incr data of " + dto.getTableName() + " can not be null");
        }
        int index = 0;
        Object val = null;
        for (FlinkCol col : cols) {
            try {
                val = vals.get(col.name);
                setRowDataVal(index++, row, (val == null) ? null : col.processVal(val));
            } catch (Exception e) {
                throw new IllegalStateException("colName:" + col.name + ",index:" + index, e);
            }
        }
        return row;
    }

    protected abstract void setRowDataVal(int index, RowData row, Object value);
//    {
//        GenericRowData rowData = (GenericRowData) row;
//        rowData.setField(index, value);
//    }

    protected abstract RowData createRowData(DTO dto);
//    {
//        return new GenericRowData(DTO2RowMapper.getKind(dto), cols.size());
//    }

    static class ShortConvert extends BiFunction {
        @Override
        public Object apply(Object o) {
            if (o instanceof Number) {
                return ((Number) o).shortValue();
            }
            throw new IllegalStateException("val:" + o + ",type:" + o.getClass().getName());
        }
    }

    static class RowShortConvert extends BiFunction {
        @Override
        public Object apply(Object o) {
            if (o instanceof Number) {
                return ((Number) o).shortValue();
            }
            return Short.parseShort(String.valueOf(o));
//            Short s = (Short) o;
//            return s;
        }
    }

    static class TinyIntConvertByte extends BiFunction {
        @Override
        public Object apply(Object o) {
            Short s = (Short) o;
            return new java.lang.Byte(s.byteValue());
            // return s.intValue();
        }
    }

    static class IntegerConvert extends BiFunction {
        @Override
        public Object apply(Object o) {
            if (o instanceof String) {
                return Integer.parseInt((String) o);
            }
            return o;
        }
    }

    static class DateConvert extends FlinkCol.LocalDateProcess {
        @Override
        public Object apply(Object o) {
            LocalDate localDate = (LocalDate) super.apply(o);
            return (int) localDate.toEpochDay();
        }
    }

    //  private static final ZoneId sysDefaultZone = ZoneId.systemDefault();

    static class TimestampDataConvert extends FlinkCol.DateTimeProcess {
        @Override
        public Object apply(Object o) {
            LocalDateTime v = (LocalDateTime) super.apply(o);
            return TimestampData.fromLocalDateTime(v);
//            ZoneOffset zoneOffset = sysDefaultZone.getRules().getOffset(v);
//            return v.toInstant(zoneOffset).toEpochMilli();
        }
    }

    static class FloatDataConvert extends BiFunction {
        @Override
        public Object apply(Object o) {
            if (o instanceof Number) {
                return ((Number) o).floatValue();
            }
            return o;
//            LocalDateTime v = (LocalDateTime) super.apply(o);
//            return TimestampData.fromLocalDateTime(v);
//            ZoneOffset zoneOffset = sysDefaultZone.getRules().getOffset(v);
//            return v.toInstant(zoneOffset).toEpochMilli();
        }
    }

    static class StringConvert extends BiFunction {
        @Override
        public Object deApply(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object apply(Object o) {
            return StringData.fromString((String) o);
        }
    }

    static class BinaryRawValueDataConvert extends BiFunction {
        @Override
        public Object deApply(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object apply(Object o) {
            java.nio.ByteBuffer buffer = (java.nio.ByteBuffer) o;
            return buffer.array();
        }
    }

    public static class BinaryRawValueDTOConvert extends BiFunction {

        @Override
        public Object apply(Object o) {
            if (o instanceof java.nio.ByteBuffer) {
                return o;
            }

            return java.nio.ByteBuffer.wrap((byte[]) o);
        }
    }

    static class DecimalConvert extends BiFunction {
        //  private final DataType type;

        final int precision;// = type.columnSize;
        final int scale;// = type.getDecimalDigits();

        public DecimalConvert(int precision, int scale) {
            this.precision = precision;
            this.scale = scale;
        }

        @Override
        public Object deApply(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object apply(Object o) {
            if (!(o instanceof BigDecimal)) {
                Number number = (Number) o;
                return DecimalData.fromBigDecimal(BigDecimal.valueOf(number.longValue()), precision, scale);
            }
            return DecimalData.fromBigDecimal((BigDecimal) o, precision, scale);
        }
    }

    public static class LocalTimeConvert extends BiFunction {
        public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

        @Override
        public Object apply(Object o) {
            if (o instanceof String) {
                return LocalTime.parse((String) o, TIME_FORMATTER);
            }
            return (LocalTime) o;
        }
    }

    static class DTOLocalTimeConvert extends LocalTimeConvert {
        @Override
        public Object apply(Object o) {
            LocalTime time = (LocalTime) super.apply(o);
            return SqlDateTimeUtils.localTimeToUnixDate(time);
        }
    }

    static class LongConvert extends BiFunction {
        @Override
        public Object deApply(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object apply(Object o) {
            if (o instanceof Number) {
                return ((Number) o).longValue();
            }
//            if (o instanceof Integer) {
//                return ((Integer) o).longValue();
//            }
            return Long.parseLong(String.valueOf(o));
        }
    }

}
