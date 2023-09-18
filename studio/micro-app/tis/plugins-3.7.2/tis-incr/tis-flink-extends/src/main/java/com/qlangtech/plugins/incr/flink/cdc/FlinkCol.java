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

package com.qlangtech.plugins.incr.flink.cdc;

import com.qlangtech.tis.plugin.ds.IColMetaGetter;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.DataType;
import org.apache.flink.types.Row;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-02-17 16:11
 **/
public class FlinkCol implements Serializable {
    public final String name;
    public final org.apache.flink.table.types.DataType type;

    public final com.qlangtech.tis.plugin.ds.DataType colType;

    private final RowData.FieldGetter rowDataValGetter;

    private boolean pk;

    /**
     * CDC Source组装数据时对从debezium中取得到的值进行处理
     */
    private BiFunction sourceDTOColValProcess;

    /**
     * @see RowData (处理从DTO中取数据组装RowData中的列内容处理器)
     */
    public BiFunction rowDataProcess;
    /**
     * @see Row (处理从DTO中取数据组装Row中的列内容处理器)
     */
    public BiFunction rowProcess;

    public FlinkCol(IColMetaGetter meta, com.qlangtech.tis.plugin.ds.DataType colType, DataType type, RowData.FieldGetter rowDataValGetter) {
        this(meta, colType, type, new NoOpProcess(), rowDataValGetter);
    }

    public Object getRowDataVal(RowData row) {
        return rowDataValGetter.getFieldOrNull(row);
    }

    public FlinkCol(IColMetaGetter meta, com.qlangtech.tis.plugin.ds.DataType colType, DataType type, BiFunction rowDataProcess, RowData.FieldGetter rowDataValGetter) {
        this(meta, colType, type, rowDataProcess, rowDataProcess, rowDataValGetter);
    }

    public FlinkCol(IColMetaGetter meta, com.qlangtech.tis.plugin.ds.DataType colType, DataType type, BiFunction rowDataProcess
            , BiFunction rowProcess, RowData.FieldGetter rowDataValGetter) {
        if (StringUtils.isEmpty(meta.getName())) {
            throw new IllegalArgumentException("param name can not be null");
        }
        this.name = meta.getName();
        this.type = type;
        this.colType = colType;
        this.rowDataProcess = rowDataProcess;
        this.rowProcess = rowProcess;
        this.rowDataValGetter = rowDataValGetter;
        this.setPk(meta.isPk());
    }

    /**
     * CDC Source组装数据时对从debezium中取得到的值进行处理
     *
     * @param sourceDTOColValProcess
     * @return
     */
    public FlinkCol setSourceDTOColValProcess(BiFunction sourceDTOColValProcess) {
        this.sourceDTOColValProcess = sourceDTOColValProcess;
        return this;
    }

    public BiFunction getSourceDTOColValProcess() {
        if (this.sourceDTOColValProcess == null) {
            this.sourceDTOColValProcess = NoOp();
        }
        return this.sourceDTOColValProcess;
    }

    public RowData.FieldGetter getRowDataValGetter() {
        return rowDataValGetter;
    }

    public boolean isPk() {
        return pk;
    }

    public FlinkCol setPk(boolean pk) {
        this.pk = pk;
        return this;
    }

    public Object processVal(Object val) {
        return this.rowDataProcess.apply(val);
    }

    public static BiFunction ByteBuffer() {
        return new ByteBufferProcess();
    }

    public static BiFunction Byte() {
        return new ByteProcess();
    }

    public static BiFunction DateTime() {
        return new DateTimeProcess();
    }

    public static BiFunction LocalDate() {
        return new LocalDateProcess();
    }

    public static BiFunction NoOp() {
        return new NoOpProcess();
    }

    private static class ByteBufferProcess extends BiFunction {
        @Override
        public Object apply(Object o) {
            java.nio.ByteBuffer buffer = (java.nio.ByteBuffer) o;
            return buffer.array();
        }

        @Override
        public Object deApply(Object o) {
            return null;
        }
    }

    private static class ByteProcess extends BiFunction {
        @Override
        public Object apply(Object o) {
            if (o instanceof java.lang.Short) {
                return new Byte(((java.lang.Short) o).byteValue());
            }
            if (o instanceof java.lang.Boolean) {
                return (byte) (((Boolean) o) ? 1 : 0);
            }
            //  Boolean b = (Boolean) o;
            return (Byte) o;
        }

        @Override
        public Object deApply(Object o) {
            return o;
        }
    }

    public static class BoolProcess extends BiFunction {
        @Override
        public Object apply(Object o) {
            if (o instanceof java.lang.Number) {
                return ((java.lang.Number) o).byteValue() > 0;
            }
            return (Boolean) o;
        }
    }


    public static class LocalDateProcess extends BiFunction {
        private final static DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-M-d");

        @Override
        public Object apply(Object o) {
            if (o instanceof String) {
                // com.qlangtech.plugins.incr.flink.cdc.valconvert.DateTimeConverter
                return LocalDate.parse((String) o, dateFormatter);
            }
            return (LocalDate) o;
        }

        @Override
        public Object deApply(Object o) {
            return dateFormatter.format((LocalDate) o);
        }
    }

    public static class DateTimeProcess extends BiFunction {
        private final static DateTimeFormatter datetimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        private final static DateTimeFormatter datetimeFormatter_with_zone = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

        @Override
        public Object deApply(Object o) {
            return datetimeFormatter.format((LocalDateTime) o);
        }

        @Override
        public Object apply(Object o) {
            if (o instanceof String) {
                // com.qlangtech.plugins.incr.flink.cdc.valconvert.DateTimeConverter
                String val = (String) o;
                return LocalDateTime.parse(val, val.contains("T") ? datetimeFormatter_with_zone : datetimeFormatter);
            }
            return (LocalDateTime) o;
        }
    }

    private static class NoOpProcess extends BiFunction {
        @Override
        public Object apply(Object o) {
            return o;
        }

        @Override
        public Object deApply(Object o) {
            return o;
        }
    }
}
