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
package org.apache.drill.exec.store.mapr.db.json;

import java.util.Stack;

import org.apache.drill.common.expression.PathSegment;
import org.apache.drill.common.expression.SchemaPath;
import org.ojai.FieldPath;
import org.ojai.FieldSegment;

public class FieldPathHelper {

  /**
   * Returns {@link SchemaPath} equivalent of the specified {@link FieldPath}.
   */
  public static SchemaPath fieldPath2SchemaPath(FieldPath fieldPath) {
    Stack<FieldSegment> fieldSegments = new Stack<FieldSegment>();
    FieldSegment seg = fieldPath.getRootSegment();
    while (seg != null) {
      fieldSegments.push(seg);
      seg = seg.getChild();
    }

    PathSegment child = null;
    while (!fieldSegments.isEmpty()) {
      seg = fieldSegments.pop();
      if (seg.isNamed()) {
        child = new PathSegment.NameSegment(((FieldSegment.NameSegment)seg).getName(), child);
      } else {
        child = new PathSegment.ArraySegment(((FieldSegment.IndexSegment)seg).getIndex(), child);
      }
    }
    return new SchemaPath((PathSegment.NameSegment)child);
  }

  /**
   * Returns {@link FieldPath} equivalent of the specified {@link SchemaPath}.
   */
  public static FieldPath schemaPath2FieldPath(SchemaPath column) {
    Stack<PathSegment> pathSegments = new Stack<PathSegment>();
    PathSegment seg = column.getRootSegment();
    while (seg != null) {
      pathSegments.push(seg);
      seg = seg.getChild();
    }

    FieldSegment child = null;
    while (!pathSegments.isEmpty()) {
      seg = pathSegments.pop();
      if (seg.isNamed()) {
        child = new FieldSegment.NameSegment(((PathSegment.NameSegment)seg).getPath(), child, false);
      } else {
        child = new FieldSegment.IndexSegment(((PathSegment.ArraySegment)seg).getIndex(), child);
      }
    }
    return new FieldPath((FieldSegment.NameSegment) child);
  }

}
