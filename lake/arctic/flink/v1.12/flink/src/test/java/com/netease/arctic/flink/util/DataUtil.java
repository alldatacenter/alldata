/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.flink.util;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.flink.table.api.ApiExpression;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;
import org.apache.flink.table.data.TimestampData;
import org.apache.flink.types.Row;
import org.apache.flink.types.RowKind;
import org.apache.flink.util.CollectionUtil;
import org.apache.iceberg.Table;
import org.apache.iceberg.data.IcebergGenerics;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.io.CloseableIterable;
import org.junit.Assert;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.flink.table.api.Expressions.row;

public class DataUtil {

  public static List<ApiExpression> toRows(Collection<Object[]> data) {
    return data.stream().map(i -> {
      int size = i.length;
      return size == 1 ? row(i[0]) : row(i[0], ArrayUtils.subarray(i, 1, size));
    }).collect(Collectors.toList());
  }

  public static Set<Row> toRowSet(Collection<Object[]> data) {
    return data.stream().map(r ->
        r[0] instanceof RowKind ? Row.ofKind((RowKind) r[0], ArrayUtils.subarray(r, 1, r.length)) :
            Row.of(r)).collect(Collectors.toSet());
  }

  public static List<Row> toRowList(Collection<Object[]> data) {
    return data.stream().map(r ->
        r[0] instanceof RowKind ? Row.ofKind((RowKind) r[0], ArrayUtils.subarray(r, 1, r.length)) :
            Row.of(r)).collect(Collectors.toList());
  }

  public static void assertEqual(Collection<Object[]> expected, Collection<Object[]> actual) {
    Assert.assertEquals(CollectionUtil.isNullOrEmpty(expected), CollectionUtil.isNullOrEmpty(actual));
    if (expected == null) {
      return;
    }
    Assert.assertEquals(expected.size(), actual.size());
    for (Iterator<Object[]> i1 = expected.iterator(), i2 = actual.iterator(); i1.hasNext(); ) {
      Object[] actualRow = i2.next();
      System.out.println(ArrayUtils.toString(actualRow));
      Assert.assertArrayEquals(i1.next(), actualRow);
    }
  }

  private static Object[] convertData(Object... values) {
    Object[] row = new Object[values.length];
    for (int i = 0; i < values.length; i++) {
      if (values[i] instanceof String) {
        row[i] = StringData.fromString((String) values[i]);
      } else if (values[i] instanceof LocalDateTime) {
        row[i] = TimestampData.fromLocalDateTime(((LocalDateTime) values[i]));
      } else if (values[i] instanceof Instant) {
        row[i] = TimestampData.fromInstant((Instant) values[i]);
      } else {
        row[i] = values[i];
      }
    }
    return row;
  }

  public static Collection<RowData> toRowData(List<Object[]> data) {
    return data.stream().map(d ->
        d[0] instanceof RowKind ?
            toRowDataWithKind((RowKind) d[0], ArrayUtils.subarray(d, 1, d.length)) :
            toRowData(d)
    ).collect(Collectors.toList());
  }

  public static RowData toRowData(Object... values) {
    return GenericRowData.of(convertData(values));
  }

  public static RowData toRowDataWithKind(RowKind rowKind, Object... values) {
    return GenericRowData.ofKind(rowKind, convertData(values));
  }

  public static Set<Record> read(Table table) {
    table.refresh();

    Set<Record> records = new HashSet<>();

    try (CloseableIterable<Record> iterable = IcebergGenerics.read(table).build()) {
      for (Record record : iterable) {
        records.add(record);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return records;
  }

  public static Map<Object, List<Row>> groupByPrimaryKey(List<Row> rowList, int pkIdx) {
    Map<Object, List<Row>> result = new HashMap<>();
    for (Row row : rowList) {
      Object pk = row.getField(pkIdx);
      List<Row> list = result.getOrDefault(pk, new LinkedList<>());
      list.add(row);
      result.put(pk, list);
    }
    return result;
  }

}
