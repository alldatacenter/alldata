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

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.record.selection.SelectionVector2;
import org.apache.drill.exec.record.selection.SelectionVector4;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

/**
 * Tool for printing the content of record batches to screen. Used for debugging.
 *
 * @See {@link org.apache.drill.exec.physical.rowSet.RowSetFormatter RowSetFormatter}
 */

public class BatchPrinter {
  public static void printHyperBatch(VectorAccessible batch, SelectionVector4 sv4) {
    List<String> columns = Lists.newArrayList();
    for (VectorWrapper<?> vw : batch) {
      columns.add(vw.getValueVectors()[0].getField().getName());
    }
    int width = columns.size();
    for (int j = 0; j < sv4.getCount(); j++) {
      if (j % 50 == 0) {
        System.out.println(StringUtils.repeat("-", width * 17 + 1));
        for (String column : columns) {
          System.out.printf("| %-15s", width <= 15 ? column : column.substring(0, 14));
        }
        System.out.printf("|\n");
        System.out.println(StringUtils.repeat("-", width * 17 + 1));
      }
      for (VectorWrapper<?> vw : batch) {
        Object o = vw.getValueVectors()[sv4.get(j) >>> 16].getAccessor().getObject(sv4.get(j) & 65535);
        String value;
        if (o == null) {
          value = "null";
        } else if (o instanceof byte[]) {
          value = new String((byte[]) o);
        } else {
          value = o.toString();
        }
        System.out.printf("| %-15s", value.length() <= 15 ? value : value.substring(0, 14));
      }
      System.out.printf("|\n");
    }
    System.out.printf("|\n");
  }

  public static void printBatch(VectorAccessible batch) {
    List<String> columns = Lists.newArrayList();
    List<ValueVector> vectors = Lists.newArrayList();
    for (VectorWrapper<?> vw : batch) {
      columns.add(vw.getValueVector().getField().getName());
      vectors.add(vw.getValueVector());
    }
    int width = columns.size();
    int rows = vectors.get(0).getMetadata().getValueCount();
    for (int row = 0; row < rows; row++) {
      if (row % 50 == 0) {
        System.out.println(StringUtils.repeat("-", width * 17 + 1));
        for (String column : columns) {
          System.out.printf("| %-15s", width <= 15 ? column : column.substring(0, 14));
        }
        System.out.printf("|\n");
        System.out.println(StringUtils.repeat("-", width * 17 + 1));
      }
      for (ValueVector vv : vectors) {
        Object o = vv.getAccessor().getObject(row);
        String value;
        if (o == null) {
          value = "null";
        } else if (o instanceof byte[]) {
          value = new String((byte[]) o);
        } else {
          value = o.toString();
        }
        System.out.printf("| %-15s", value.length() <= 15 ? value : value.substring(0, 14));
      }
      System.out.printf("|\n");
    }
  }

  public static void printBatch(VectorAccessible batch, SelectionVector2 sv2) {
    List<String> columns = Lists.newArrayList();
    List<ValueVector> vectors = Lists.newArrayList();
    for (VectorWrapper<?> vw : batch) {
      columns.add(vw.getValueVector().getField().getName());
      vectors.add(vw.getValueVector());
    }
    int width = columns.size();
    int rows = vectors.get(0).getMetadata().getValueCount();
    for (int i = 0; i < rows; i++) {
      if (i % 50 == 0) {
        System.out.println(StringUtils.repeat("-", width * 17 + 1));
        for (String column : columns) {
          System.out.printf("| %-15s", width <= 15 ? column : column.substring(0, 14));
        }
        System.out.printf("|\n");
        System.out.println(StringUtils.repeat("-", width * 17 + 1));
      }
      int row = sv2.getIndex(i);
      for (ValueVector vv : vectors) {
        Object o = vv.getAccessor().getObject(row);
        String value;
        if (o == null) {
          value = "null";
        } else if (o instanceof byte[]) {
          value = new String((byte[]) o);
        } else {
          value = o.toString();
        }
        System.out.printf("| %-15s", value.length() <= 15 ? value : value.substring(0, 14));
      }
      System.out.printf("|\n");
    }
  }
}
