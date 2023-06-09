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

import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.metadata.ColumnMetadata;

/**
 * Column that matches one provided by the table. Provides the data type
 * of that column and information to project from the result set loader
 * output container to the scan output container. (Note that the result
 * set loader container is, itself, a projection from the actual table
 * schema to the desired set of columns; but in the order specified
 * by the table.)
 */

public class ResolvedTableColumn extends ResolvedColumn {

  private final String projectedName;
  private final MaterializedField schema;

  public ResolvedTableColumn(String projectedName,
      MaterializedField schema,
      VectorSource source, int sourceIndex) {
    super(source, sourceIndex);
    this.projectedName = projectedName;
    this.schema = schema;
  }

  public ResolvedTableColumn(ColumnMetadata outputCol,
      VectorSource source, int sourceIndex) {
    super(outputCol, source, sourceIndex);
    this.projectedName = outputCol.name();
    this.schema = outputCol.schema();
  }

  @Override
  public String name() { return projectedName; }

  @Override
  public MaterializedField schema() { return schema; }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf
      .append("[")
      .append(getClass().getSimpleName())
      .append(" name=")
      .append(name())
      .append("]");
    return buf.toString();
  }
}
