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

import org.apache.flink.table.data.RowData;
import org.apache.flink.table.runtime.functions.SqlDateTimeUtils;

import javax.annotation.Nullable;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-10-25 12:06
 **/
public class RowFieldGetterFactory {

    public static RowData.FieldGetter intGetter(String colName, int colIndex) {
        return new IntGetter(colName, colIndex);
    }

    public static RowData.FieldGetter smallIntGetter(String colName, int colIndex) {
        return new SmallIntGetter(colName, colIndex);
    }

    public static class StringGetter extends BasicGetter {
        public StringGetter(String colName, int colIndex) {
            super(colName, colIndex);
        }

        @Override
        protected Object getObject(RowData rowData) {
            return String.valueOf(rowData.getString(colIndex));
        }
    }

    public static class BlobGetter extends BasicGetter {
        public BlobGetter(String colName, int colIndex) {
            super(colName, colIndex);
        }

        @Override
        protected Object getObject(RowData rowData) {
            return rowData.getBinary(colIndex);
        }
    }

    public static class BoolGetter extends BasicGetter {
        public BoolGetter(String colName, int colIndex) {
            super(colName, colIndex);
        }

        @Override
        public Object getObject(RowData rowData) {
            return rowData.getBoolean(colIndex);
        }
    }

    public static class DateGetter extends BasicGetter {

        public DateGetter(String colName, int colIndex) {
            super(colName, colIndex);
        }

        @Override
        public Object getObject(RowData rowData) {
            return Date.valueOf(LocalDate.ofEpochDay(rowData.getInt(colIndex)));
        }
    }

    public static class TimestampGetter extends BasicGetter {

        public TimestampGetter(String colName, int colIndex) {
            super(colName, colIndex);
        }

        @Override
        public Object getObject(RowData rowData) {
            return rowData.getTimestamp(colIndex, -1).toTimestamp();
        }
    }


    public static class DoubleGetter extends BasicGetter {
        public DoubleGetter(String colName, int colIndex) {
            super(colName, colIndex);
        }

        @Override
        public Object getObject(RowData rowData) {
            return rowData.getDouble(colIndex);
        }
    }

    public static class DecimalGetter extends BasicGetter {
        public DecimalGetter(String colName, int colIndex) {
            super(colName, colIndex);
        }

        @Override
        public Object getObject(RowData rowData) {
            return rowData.getDecimal(colIndex, -1, -1);
        }
    }


    public static class BigIntGetter extends BasicGetter {
        public BigIntGetter(String colName, int colIndex) {
            super(colName, colIndex);
        }

        @Override
        public Object getObject(RowData rowData) {
            return rowData.getLong(colIndex);
        }
    }

    public static class TimeGetter extends BasicGetter {
        public TimeGetter(String colName, int colIndex) {
            super(colName, colIndex);
        }

        @Nullable
        @Override
        public Object getObject(RowData rowData) {
            return Time.valueOf(SqlDateTimeUtils.unixTimeToLocalTime((rowData.getInt(colIndex))));
        }
    }


    public static class FloatGetter extends BasicGetter {
        public FloatGetter(String colName, int colIndex) {
            super(colName, colIndex);
        }

        @Override
        public Object getObject(RowData rowData) {
            return rowData.getFloat(colIndex);
        }
    }


    public static class ByteGetter extends BasicGetter {
        public ByteGetter(String colName, int colIndex) {
            super(colName, colIndex);
        }

        @Override
        public Object getObject(RowData rowData) {
            return rowData.getByte(colIndex);
        }
    }

    static class SmallIntGetter extends BasicGetter {
        public SmallIntGetter(String colName, int colIndex) {
            super(colName, colIndex);
        }

        @Override
        public Object getObject(RowData rowData) {
            return rowData.getShort(colIndex);
        }
    }

    static class IntGetter extends BasicGetter {

        public IntGetter(String colName, int colIndex) {
            super(colName, colIndex);
        }

        @Override
        public Object getObject(RowData rowData) {
            return rowData.getInt(colIndex);
        }
    }

    static abstract class BasicGetter implements RowData.FieldGetter {
        final int colIndex;
        final String colName;

        public BasicGetter(String colName, int colIndex) {
            this.colIndex = colIndex;
            this.colName = colName;
        }

        @Override
        public final Object getFieldOrNull(RowData rowData) {
            return getVal(rowData);
        }

        private Object getVal(RowData rowData) {
            try {
                return getObject(rowData);
            } catch (ClassCastException e) {
                throw new RuntimeException("colIdx:" + this.colIndex + ",colName:" + this.colName, e);
            }
        }

        protected abstract Object getObject(RowData rowData);
    }
}
