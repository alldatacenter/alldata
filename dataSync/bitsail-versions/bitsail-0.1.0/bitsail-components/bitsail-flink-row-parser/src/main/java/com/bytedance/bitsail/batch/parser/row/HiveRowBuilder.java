/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.batch.parser.row;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.column.BooleanColumn;
import com.bytedance.bitsail.common.column.BytesColumn;
import com.bytedance.bitsail.common.column.Column;
import com.bytedance.bitsail.common.column.ColumnCast;
import com.bytedance.bitsail.common.column.DateColumn;
import com.bytedance.bitsail.common.column.DoubleColumn;
import com.bytedance.bitsail.common.column.ListColumn;
import com.bytedance.bitsail.common.column.LongColumn;
import com.bytedance.bitsail.common.column.MapColumn;
import com.bytedance.bitsail.common.column.StringColumn;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.component.format.api.RowBuilder;
import com.bytedance.bitsail.flink.core.typeinfo.ListColumnTypeInfo;
import com.bytedance.bitsail.flink.core.typeinfo.MapColumnTypeInfo;

import com.bytedance.bitsail.shaded.hive.client.HiveMetaClientUtil;

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.types.Row;
import org.apache.hadoop.hive.serde2.io.TimestampWritable;
import org.apache.hadoop.io.ArrayWritable;
import org.apache.hadoop.io.BooleanWritable;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Writable;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
public class HiveRowBuilder implements RowBuilder {
  private Map<String, Integer> columnMapping;

  public HiveRowBuilder(Map<String, Integer> columnMapping) {
    this.columnMapping = columnMapping;
  }

  /**
   * construct hive parquet row
   *
   * @param reuse
   */
  @Override
  public void build(Object objectValue, Row reuse, String mandatoryEncoding, RowTypeInfo rowTypeInfo) throws BitSailException {
    String columnName = "";
    try {
      Writable[] vals = ((ArrayWritable) objectValue).get();

      for (int i = 0; i < reuse.getArity(); i++) {
        TypeInformation typeInfo = rowTypeInfo.getTypeAt(i);
        columnName = rowTypeInfo.getFieldNames()[i];
        Writable w = vals[columnMapping.get(columnName.toUpperCase())];

        Column column = createColumn(typeInfo, w);
        reuse.setField(i, column);
      }
    } catch (BitSailException e) {
      throw BitSailException.asBitSailException(e.getErrorCode(), String.format("column[%s] %s", columnName, e.getErrorMessage()));
    } catch (ClassCastException e) {
      throw new ClassCastException(String.format("Columns mapping warning: cast column[%s] type error,please check column mapping. %s", columnName, e.getMessage()));
    }
  }

  protected Column createColumn(TypeInformation typeInformation, Writable w) {
    Class columnTypeClass = typeInformation.getTypeClass();
    //List
    if (columnTypeClass == List.class) {
      return createListColumn(typeInformation, w);
    } else if (columnTypeClass == Map.class) {
      //Map
      return createMapColumn(typeInformation, w);
    } else {
      return createBasicColumn(typeInformation, w);
    }
  }

  protected Column createBasicColumn(TypeInformation typeInformation, Writable w) {
    Class columnTypeClass = typeInformation.getTypeClass();

    if (columnTypeClass == StringColumn.class) {
      return new StringColumn(w == null ? null : w.toString());
    } else if (columnTypeClass == BooleanColumn.class) {
      return new BooleanColumn(w != null && ((BooleanWritable) w).get());
    } else if (columnTypeClass == BytesColumn.class) {
      return new BytesColumn(w == null ? null : ((BytesWritable) w).getBytes());
    } else if (columnTypeClass == LongColumn.class) {
      return w == null ? new LongColumn((Long) null) : getLongColumnFromWritable(w);
    } else if (columnTypeClass == DateColumn.class) {
      return w == null ? new DateColumn((Date) null) : getDateColumnFromWritable(w.toString());
    } else if (columnTypeClass == DoubleColumn.class) {
      return w == null ? new DoubleColumn((Double) null) : getDoubleColumnFromWritable(w);
    } else {
      throw BitSailException.asBitSailException(CommonErrorCode.UNSUPPORTED_COLUMN_TYPE, columnTypeClass + " is not supported yet!");
    }
  }

  protected ListColumn<Column> createListColumn(TypeInformation typeInfo, Writable w) {
    final ListColumnTypeInfo listTypeInfo = (ListColumnTypeInfo) typeInfo;
    TypeInformation<?> valueTypeInfo = listTypeInfo.getElementTypeInfo();

    ListColumn<Column> lc = new ListColumn<Column>(Column.class);
    if (w != null) {
      ArrayWritable val = (ArrayWritable) w;
      ArrayWritable value = getArrayWritable((ArrayWritable) w);
      for (int j = 0; j < value.get().length; j++) {
        Column temp = createColumn(valueTypeInfo, value.get()[j]);
        lc.add(temp);
      }
    }
    return lc;
  }

  protected MapColumn<Column, Column> createMapColumn(TypeInformation typeInfo, Writable w) {
    final MapColumnTypeInfo mapTypeInfo = (MapColumnTypeInfo) typeInfo;
    TypeInformation<?> keyTypeInfo = mapTypeInfo.getKeyTypeInfo();
    TypeInformation<?> valueTypeInfo = mapTypeInfo.getValueTypeInfo();

    MapColumn<Column, Column> mc = new MapColumn<Column, Column>(Column.class, Column.class);
    if (w != null) {
      ArrayWritable val = getArrayWritable((ArrayWritable) w);
      for (Writable o : val.get()) {
        ArrayWritable t = (ArrayWritable) o;
        Column keyColumn = createColumn(keyTypeInfo, t.get()[0]);
        Column valueColumn = createColumn(valueTypeInfo, t.get()[1]);
        mc.put(keyColumn, valueColumn);
      }
    }
    return mc;
  }

  protected ArrayWritable getArrayWritable(ArrayWritable w) {
    return HiveMetaClientUtil.getHiveShim().getComplexValueFromArrayWritable(w);
  }

  /**
   * @param w
   * @return
   */
  @SuppressWarnings("checkstyle:MagicNumber")
  private LongColumn getLongColumnFromWritable(Writable w) {
    if (w instanceof LongWritable) {
      return new LongColumn(((LongWritable) w).get());
    } else if (w instanceof IntWritable) {
      return new LongColumn(((IntWritable) w).get());
    } else if (w instanceof TimestampWritable) {
      return new LongColumn(((TimestampWritable) w).getTimestamp().getTime() / 1000);
    } else {
      return new LongColumn(w.toString());
    }
  }

  private DoubleColumn getDoubleColumnFromWritable(Writable w) {
    if (w instanceof DoubleWritable) {
      return new DoubleColumn(((DoubleWritable) w).get());
    } else if (w instanceof IntWritable) {
      return new DoubleColumn(((IntWritable) w).get());
    } else {
      return new DoubleColumn(w.toString());
    }
  }

  private DateColumn getDateColumnFromWritable(String timeStr) {
    try {
      StringColumn strColumn = new StringColumn(timeStr);
      return new DateColumn(ColumnCast.string2Date(strColumn).getTime());
    } catch (Exception e) {
      throw BitSailException.asBitSailException(
          CommonErrorCode.CONVERT_NOT_SUPPORT,
          String.format("String[%s] can't convert to Date.", timeStr));
    }
  }
}
