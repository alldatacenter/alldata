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
package org.apache.drill.exec.physical.impl.scan.v3.schema;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.DynamicColumn;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.metadata.TupleSchema;

/**
 * Enhanced form of a dynamic column which records all information from
 * the project list.
 */
public class ProjectedColumn extends DynamicColumn {

  /**
   * Marker to indicate that that a) the item is an
   * array, and b) that all indexes are to be projected.
   * Used when seeing both a and a[x].
   */
  private static final Set<Integer> ALL_INDEXES = new HashSet<>();

  private int refCount = 1;
  private int arrayDims;
  private Set<Integer> indexes;
  private TupleMetadata members;

  public ProjectedColumn(String name) {
    super(name);
  }

  protected void bumpRefCount() { refCount++; }

  public int refCount() { return refCount; }

  public boolean isSimple() {
    return !isArray() && !isMap();
  }

  @Override
  public boolean isMap() {
    return members != null;
  }

  public void projectAllElements() {
    indexes = ALL_INDEXES;
  }

  public void becomeArray(int dims) {
    arrayDims = dims;
    indexes = indexes == null ? new HashSet<>() : indexes;
  }

  public int arrayDims() { return arrayDims; }

  @Override
  public boolean isArray() {
    return arrayDims > 0;
  }

  protected void addIndex(int index) {
    if (indexes == null) {
      indexes = new HashSet<>();
    }
    if (indexes != ALL_INDEXES) {
      indexes.add(index);
    }
  }

  public boolean hasIndexes() {
    return isArray() && indexes != ALL_INDEXES;
  }

  public boolean hasIndex(int index) {
    return hasIndexes() && indexes.contains(index);
  }

  public int maxIndex() {
    if (!hasIndexes()) {
      return 0;
    }
    int max = 0;
    for (final Integer index : indexes) {
      max = Math.max(max, index);
    }
    return max;
  }

  public boolean[] indexes() {
    if (!hasIndexes()) {
      return null;
    }
    final int max = maxIndex();
    final boolean map[] = new boolean[max+1];
    for (final Integer index : indexes) {
      map[index] = true;
    }
    return map;
  }

  public void projectAllMembers() {
    if (members == null) {
      members = new TupleSchema();
    }
    members.setProperty(ScanProjectionParser.PROJECTION_TYPE_PROP, ScanProjectionParser.PROJECT_ALL);
  }

  public TupleMetadata explicitMembers() {
    if (members == null) {
      members = new TupleSchema();
    }
    return members;
  }

  @Override
  public TupleMetadata tupleSchema() { return members; }

  @Override
  protected void appendContents(StringBuilder buf) {
    appendArray(buf);
    if (isMap()) {
      buf.append(" members=").append(members.toString());
    }
  }

  private void appendArray(StringBuilder buf) {
    if (isArray()) {
      buf.append("[");
      if (indexes == ALL_INDEXES) {
        buf.append("*");
      } else {
        List<String> idxs = indexes.stream().sorted().map(i -> Integer.toString(i)).collect(Collectors.toList());
        buf.append(String.join(", ", idxs));
      }
      buf.append("]");
    }
  }

  @Override
  public ColumnMetadata copy() {
    ProjectedColumn copy = new ProjectedColumn(name);
    copy.refCount = refCount;
    copy.arrayDims = arrayDims;
    copy.indexes = indexes; // Indexes are immutable after parsing
    copy.members = members == null ? null : members.copy();
    return copy;
  }

  public String projectString() {
    StringBuilder buf = new StringBuilder()
        .append(name);
    appendArray(buf);
    if (isMap()) {
      buf.append(" {");
      int i = 0;
      for (ColumnMetadata child : members) {
        if (i++ > 0) {
          buf.append(", ");
        }
        if (child instanceof ProjectedColumn) {
          buf.append(((ProjectedColumn) child).projectString());
        } else {
          buf.append(child.toString());
        }
      }
      buf.append("}");
    }
    return buf.toString();
  }
}
