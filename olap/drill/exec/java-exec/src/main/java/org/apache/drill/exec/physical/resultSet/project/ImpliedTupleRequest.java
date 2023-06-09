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

import java.util.ArrayList;
import java.util.List;

import org.apache.drill.common.exceptions.CustomErrorContext;
import org.apache.drill.exec.record.metadata.ColumnMetadata;

/**
 * Represents a wildcard: SELECT * when used at the root tuple.
 * When used with maps, means selection of all map columns, either
 * implicitly, or because the map itself is selected.
 */
public class ImpliedTupleRequest implements RequestedTuple {

  public static final RequestedTuple ALL_MEMBERS =
      new ImpliedTupleRequest(true);
  public static final RequestedTuple NO_MEMBERS =
      new ImpliedTupleRequest(false);
  public static final List<RequestedColumn> EMPTY_COLS = new ArrayList<>();

  private final boolean allProjected;

  public ImpliedTupleRequest(boolean allProjected) {
    this.allProjected = allProjected;
  }

  @Override
  public RequestedTuple mapProjection(String colName) {
    return allProjected ? ALL_MEMBERS : NO_MEMBERS;
  }

  @Override
  public int size() { return 0; }

  @Override
  public RequestedColumn get(int i) { return null; }

  @Override
  public RequestedColumn get(String colName) { return null; }

  @Override
  public List<RequestedColumn> projections() { return EMPTY_COLS; }

  @Override
  public void buildName(StringBuilder buf) { }

  @Override
  public TupleProjectionType type() {
    return allProjected ? TupleProjectionType.ALL : TupleProjectionType.NONE;
  }

  @Override
  public boolean isProjected(String colName) {
    return allProjected;
  }

  @Override
  public boolean isProjected(ColumnMetadata columnSchema) {
    return allProjected ? !Projections.excludeFromWildcard(columnSchema) : false;
  }

  @Override
  public boolean enforceProjection(ColumnMetadata columnSchema,
      CustomErrorContext errorContext) {
    return isProjected(columnSchema);
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder()
        .append("{");
    if (allProjected) {
      buf.append("*");
    }
    return buf.append("}").toString();
  }

  @Override
  public boolean isEmpty() { return !allProjected; }
}
