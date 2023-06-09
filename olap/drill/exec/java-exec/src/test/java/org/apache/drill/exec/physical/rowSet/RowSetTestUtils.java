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
package org.apache.drill.exec.physical.rowSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.record.metadata.TupleMetadata;

public class RowSetTestUtils {

  private RowSetTestUtils() { }

  public static List<SchemaPath> projectList(String... names) {
    List<SchemaPath> selected = new ArrayList<>();
    for (String name : names) {
      if (name.equals(SchemaPath.DYNAMIC_STAR) || name.equals("*")) {
        selected.add(SchemaPath.STAR_COLUMN);
      } else {
        selected.add(SchemaPath.parseFromString(name));
      }
    }
    return selected;
  }

  public static List<SchemaPath> projectList(List<String> names) {
    String nameArray[] = new String[names.size()];
    names.toArray(nameArray);
    return projectList(nameArray);
  }

  public static List<SchemaPath> projectCols(SchemaPath... cols) {
    List<SchemaPath> selected = new ArrayList<>();
    for (SchemaPath col: cols) {
      selected.add(col);
    }
    return selected;
  }

  public static List<SchemaPath> projectAll() {
    return Arrays.asList(
        new SchemaPath[] {SchemaPath.STAR_COLUMN});
  }

  public static List<SchemaPath> projectNone() {
    return Collections.emptyList();
  }

  @SafeVarargs
  public static List<SchemaPath> concat(List<SchemaPath>... parts) {
    List<SchemaPath> selected = new ArrayList<>();
    for (List<SchemaPath> part : parts) {
      selected.addAll(part);
    }
    return selected;
  }

  public static RowSetWriter makeWriter(BufferAllocator allocator, TupleMetadata outputSchema) {
    return makeWriter(allocator, outputSchema, 256);
  }

  public static RowSetWriter makeWriter(BufferAllocator allocator, TupleMetadata outputSchema, int size) {
    DirectRowSet rowSet = DirectRowSet.fromSchema(allocator, outputSchema);
    return rowSet.writer(size);
  }
}
