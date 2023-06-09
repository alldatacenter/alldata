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
package org.apache.drill.exec.physical.resultSet.project;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.drill.exec.physical.resultSet.project.RequestedTuple.TupleProjectionType;

/**
 * Represents one level of qualifier for a column. Analogous to
 * a {@code SchemaPath}, but represents the result of coalescing
 * multiple occurrences of the same column.
 */
public class Qualifier implements QualifierContainer {
  /**
   * Marker to indicate that that a) the item is an
   * array, and b) that all indexes are to be projected.
   * Used when seeing both a and a[x].
   */
  private static final Set<Integer> ALL_INDEXES = new HashSet<>();

  private Set<Integer> indexes;
  private RequestedTuple members;
  private Qualifier child;

  @Override
  public Qualifier qualifier() { return child; }

  @Override
  public Qualifier requireQualifier() {
    if (child == null) {
      child = new Qualifier();
    }
    return child;
  }

  public boolean isArray() {
    return indexes != null;
  }

  public boolean hasIndexes() {
    return isArray() && indexes != ALL_INDEXES;
  }

  public boolean hasIndex(int index) {
    return hasIndexes() && indexes.contains(index);
  }

  public int maxIndex() {
    if (! hasIndexes()) {
      return 0;
    }
    int max = 0;
    for (final Integer index : indexes) {
      max = Math.max(max, index);
    }
    return max;
  }

  public boolean[] indexArray() {
    if (! hasIndexes()) {
      return null;
    }
    final int max = maxIndex();
    final boolean map[] = new boolean[max+1];
    for (final Integer index : indexes) {
      map[index] = true;
    }
    return map;
  }

  public boolean isTuple() {
    return members != null || (child != null && child.isTuple());
  }

  public RequestedTuple tuple() {
    if (members != null) {
      return members;
    } if (child != null) {
      return child.tuple();
    } else {
      return null;
    }
  }

  protected void addIndex(int index) {
    if (indexes == null) {
      indexes = new HashSet<>();
    }
    if (indexes != ALL_INDEXES) {
      indexes.add(index);
    }
  }

  protected void projectAllElements() {
    indexes = ALL_INDEXES;
  }

  public int arrayDims() {
    if (!isArray()) {
      return 0;
    } else if (child == null) {
      return 1;
    } else {
      return 1 + child.arrayDims();
    }
  }

  public void projectAllMembers() {
    if (members == null || members.type() != TupleProjectionType.ALL) {
      members = ImpliedTupleRequest.ALL_MEMBERS;
    }
  }

  public RequestedTupleImpl explicitMembers() {
    if (members == null) {
      members = new RequestedTupleImpl();
    }
    if (members.type() == TupleProjectionType.SOME) {
      return (RequestedTupleImpl) members;
    } else {
      return null;
    }
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
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
    if (members != null) {
      buf.append(members.toString());
    }
    return buf.toString();
  }
}
