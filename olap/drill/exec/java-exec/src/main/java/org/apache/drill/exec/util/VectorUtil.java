/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.util;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.util.DrillStringUtils;
import org.apache.drill.exec.expr.fn.impl.DateUtility;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.vector.AllocationHelper;
import org.apache.drill.exec.vector.ValueVector;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.drill.shaded.guava.com.google.common.base.Joiner;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

public class VectorUtil {
  private static final Logger logger = LoggerFactory.getLogger(VectorUtil.class);
  public static final int DEFAULT_COLUMN_WIDTH = 15;

  public static void logVectorAccessibleContent(VectorAccessible va, final String delimiter) {
    final StringBuilder sb = new StringBuilder();
    int rows = va.getRecordCount();
    sb.append(rows).append(" row(s):\n");
    List<String> columns = Lists.newArrayList();
    for (VectorWrapper<?> vw : va) {
      columns.add(formatFieldSchema(vw.getValueVector().getField()));
    }

    int width = columns.size();
    for (String column : columns) {
      sb.append(column).append(column.equals(columns.get(width - 1)) ? "\n" : delimiter);
    }
    for (int row = 0; row < rows; row++) {
      int columnCounter = 0;
      for (VectorWrapper<?> vw : va) {
        boolean lastColumn = columnCounter == width - 1;
        Object o;
        try{
          o = vw.getValueVector().getAccessor().getObject(row);
        } catch (Exception e) {
          throw new RuntimeException("failure while trying to read column " + vw.getField().getName());
        }
        if (o == null) {
          //null value
          String value = "null";
          sb.append(value).append(lastColumn ? "\n" : delimiter);
        }
        else if (o instanceof byte[]) {
          String value = new String((byte[]) o);
          sb.append(value).append(lastColumn ? "\n" : delimiter);
        } else {
          String value = o.toString();
          sb.append(value).append(lastColumn ? "\n" : delimiter);
        }
        columnCounter++;
      }
    }

    for (VectorWrapper<?> vw : va) {
      vw.clear();
    }

    logger.info(sb.toString());
  }

  public static String formatFieldSchema(MaterializedField field) {
    String colName = field.getName() + "<" + field.getType().getMinorType() + "(" + field.getType().getMode() + ")" + ">";
    if (field.getType().getMinorType() == MinorType.MAP) {
      colName += expandMapSchema(field);
    }
    return colName;
  }

  public static void appendVectorAccessibleContent(VectorAccessible va, StringBuilder formattedResults,
      final String delimiter, boolean includeHeader) {
    if (includeHeader) {
      List<String> columns = Lists.newArrayList();
      for (VectorWrapper<?> vw : va) {
        columns.add(vw.getValueVector().getField().getName());
      }

      formattedResults.append(Joiner.on(delimiter).join(columns));
      formattedResults.append("\n");
    }

    int rows = va.getRecordCount();
    for (int row = 0; row < rows; row++) {
      List<String> rowValues = Lists.newArrayList();
      for (VectorWrapper<?> vw : va) {
        Object o = vw.getValueVector().getAccessor().getObject(row);
        if (o == null) {
          rowValues.add("null");
        } else if (o instanceof byte[]) {
          rowValues.add(new String((byte[]) o));
        } else if (o instanceof DateTime) {
          // TODO(DRILL-3882) - remove this once the datetime is not returned in an
          // object needlessly holding a timezone
          rowValues.add(DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS").print((DateTime) o));
        } else if (o instanceof LocalDateTime) {
          rowValues.add(DateUtility.formatTimeStamp.format((LocalDateTime) o));
        } else {
          rowValues.add(o.toString());
        }
      }
      formattedResults.append(Joiner.on(delimiter).join(rowValues));
      formattedResults.append("\n");
    }

    for (VectorWrapper<?> vw : va) {
      vw.clear();
    }
  }

  public static void logVectorAccessibleContent(VectorAccessible va) {
    logVectorAccessibleContent(va, DEFAULT_COLUMN_WIDTH);
  }

  public static void logVectorAccessibleContent(VectorAccessible va, int columnWidth) {
    logVectorAccessibleContent(va, new int[]{ columnWidth });
  }

  public static void logVectorAccessibleContent(VectorAccessible va, int[] columnWidths) {
    final StringBuilder sb = new StringBuilder();
    int width = 0;
    int columnIndex = 0;
    List<String> columns = Lists.newArrayList();
    List<String> formats = Lists.newArrayList();
    for (VectorWrapper<?> vw : va) {
      int columnWidth = getColumnWidth(columnWidths, columnIndex);
      width += columnWidth + 2;
      formats.add("| %-" + columnWidth + "s");
      columnIndex++;
      columns.add(formatFieldSchema(vw.getValueVector().getField()));
    }

    int rows = va.getRecordCount();
    sb.append(rows).append(" row(s):\n");
    for (int row = 0; row < rows; row++) {
      // header, every 50 rows.
      if (row%50 == 0) {
        sb.append(StringUtils.repeat("-", width + 1)).append('\n');
        columnIndex = 0;
        for (String column : columns) {
          int columnWidth = getColumnWidth(columnWidths, columnIndex);
          sb.append(String.format(formats.get(columnIndex), column.length() <= columnWidth ? column : column.substring(0, columnWidth - 1)));
          columnIndex++;
        }

        sb.append("|\n");
        sb.append(StringUtils.repeat("-", width + 1)).append('\n');
      }
      // column values
      columnIndex = 0;
      for (VectorWrapper<?> vw : va) {
        int columnWidth = getColumnWidth(columnWidths, columnIndex);
        Object o = vw.getValueVector().getAccessor().getObject(row);
        String cellString;
        if (o instanceof byte[]) {
          cellString = DrillStringUtils.toBinaryString((byte[]) o);
        } else {
          cellString = DrillStringUtils.escapeNewLines(String.valueOf(o));
        }
        sb.append(String.format(formats.get(columnIndex), cellString.length() <= columnWidth ? cellString : cellString.substring(0, columnWidth - 1)));
        columnIndex++;
      }
      sb.append("|\n");
    }
    if (rows > 0) {
      sb.append(StringUtils.repeat("-", width + 1)).append('\n');
    }

    for (VectorWrapper<?> vw : va) {
      vw.clear();
    }

    logger.info(sb.toString());
  }

  private static String expandMapSchema(MaterializedField mapField) {
    StringBuilder buf = new StringBuilder();
    buf.append("{");
    String sep = "";
    for (MaterializedField field : mapField.getChildren()) {
      buf.append(sep);
      sep = ",";
      buf.append(formatFieldSchema(field));
    }
    buf.append("}");
    return buf.toString();
  }

  public static void allocateVectors(Iterable<ValueVector> valueVectors, int count) {
    for (final ValueVector v : valueVectors) {
      AllocationHelper.allocateNew(v, count);
    }
  }

  public static void setValueCount(Iterable<ValueVector> valueVectors, int count) {
    for (final ValueVector v : valueVectors) {
      v.getMutator().setValueCount(count);
    }
  }

  private static int getColumnWidth(int[] columnWidths, int columnIndex) {
    return (columnWidths == null) ? DEFAULT_COLUMN_WIDTH
        : (columnWidths.length > columnIndex) ? columnWidths[columnIndex] : columnWidths[0];
  }
}
