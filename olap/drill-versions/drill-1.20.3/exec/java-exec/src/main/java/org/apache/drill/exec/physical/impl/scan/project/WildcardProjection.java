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

import java.util.List;

import org.apache.drill.exec.physical.impl.scan.project.AbstractUnresolvedColumn.UnresolvedWildcardColumn;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.metadata.TupleMetadata;

/**
 * Perform a wildcard projection. In this case, the query wants all
 * columns in the source table, so the table drives the final projection.
 * Since we include only those columns in the table, there is no need
 * to create null columns. Example: SELECT *
 */

public class WildcardProjection extends ReaderLevelProjection {

  public WildcardProjection(ScanLevelProjection scanProj,
      TupleMetadata tableSchema,
      ResolvedTuple rootTuple,
      List<ReaderProjectionResolver> resolvers) {
    super(resolvers);
    for (ColumnProjection col : scanProj.columns()) {
      if (col instanceof UnresolvedWildcardColumn) {
        projectAllColumns(rootTuple, tableSchema);
      } else {
        resolveSpecial(rootTuple, col, tableSchema);
      }
    }
  }

  /**
   * Project all columns from table schema to the output, in table
   * schema order. Since we accept any map columns as-is, no need
   * to do recursive projection.
   */

  private void projectAllColumns(ResolvedTuple rootTuple, TupleMetadata tableSchema) {
    for (int i = 0; i < tableSchema.size(); i++) {
      MaterializedField colSchema = tableSchema.column(i);
      rootTuple.add(
          new ResolvedTableColumn(colSchema.getName(),
              colSchema, rootTuple, i));
    }
  }
}
