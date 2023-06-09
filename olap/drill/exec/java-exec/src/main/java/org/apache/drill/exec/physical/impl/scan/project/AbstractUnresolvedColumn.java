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
package org.apache.drill.exec.physical.impl.scan.project;

import org.apache.drill.exec.physical.resultSet.project.RequestedColumn;
import org.apache.drill.exec.record.metadata.ColumnMetadata;

/**
 * Represents a projected column that has not yet been bound to a
 * table column, special column or a null column. Once bound, this
 * column projection is replaced with the detailed binding.
 * <p>
 * Occurs in a scan-level projection to identify columns needed in
 * the output batch. Once we see reader data, we create a
 * {@link ResolvedColumn} to replace this unresolved form. The
 * resolved form identifies how to map data from its source (reader,
 * null column builder, etc.) to the output batch. Thus the columns
 * here are placeholders to be rewritten once more data is available.
 */
public abstract class AbstractUnresolvedColumn implements ColumnProjection {

  /**
   * Represents an unresolved table column to be provided by the
   * reader (or filled in with nulls.) May be associated with
   * a provided schema column.
   */
  public static class UnresolvedColumn extends AbstractUnresolvedColumn {

    private final ColumnMetadata colDefn;

    public UnresolvedColumn(RequestedColumn inCol) {
      this(inCol, null);
    }

    public UnresolvedColumn(RequestedColumn inCol, ColumnMetadata colDefn) {
      super(inCol);
      this.colDefn = colDefn;
    }

    public ColumnMetadata metadata() { return colDefn; }

    @Override
    public String name() { return colDefn == null ? super.name() : colDefn.name(); }
 }

  public static class UnresolvedWildcardColumn extends AbstractUnresolvedColumn {

    public UnresolvedWildcardColumn(RequestedColumn inCol) {
      super(inCol);
    }
  }

  /**
   * The original physical plan column to which this output column
   * maps. In some cases, multiple output columns map map the to the
   * same "input" (to the projection process) column.
   */
  protected final RequestedColumn inCol;

  public AbstractUnresolvedColumn(RequestedColumn inCol) {
    this.inCol = inCol;
  }

  @Override
  public String name() { return inCol.name(); }

  public RequestedColumn element() { return inCol; }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf
      .append("[")
      .append(getClass().getSimpleName())
      .append(" type=")
      .append(getClass().getSimpleName());
    if (inCol != null) {
      buf
        .append(", incol=")
        .append(inCol.toString());
    }
    return buf.append("]").toString();
  }
}