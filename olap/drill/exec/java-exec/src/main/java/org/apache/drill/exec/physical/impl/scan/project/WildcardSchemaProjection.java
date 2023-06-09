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

import org.apache.drill.exec.physical.impl.scan.project.AbstractUnresolvedColumn.UnresolvedColumn;
import org.apache.drill.exec.physical.impl.scan.project.ScanLevelProjection.ScanProjectionType;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.TupleMetadata;

/**
 * Perform a wildcard projection with an associated output schema.
 * Matches the reader schema against the output schema. If a column
 * appears, it is projected into the output schema. If not found,
 * then a null column (as defined by the output schema) is projected.
 * <p>
 * Note that we don't go down this path for strict schema: in that
 * case we expanded the columns at the scan level.
 */

public class WildcardSchemaProjection extends ReaderLevelProjection {

  public WildcardSchemaProjection(ScanLevelProjection scanProj,
      TupleMetadata readerSchema,
      ResolvedTuple rootTuple,
      List<ReaderProjectionResolver> resolvers) {
    super(resolvers);

    // Match each column expanded from the output schema against the
    // columns provided by the reader.

    boolean readerProjectionMap[] = new boolean[readerSchema.size()];
    for (ColumnProjection col : scanProj.columns()) {
      if (col instanceof UnresolvedColumn) {

        // Look for a match in the reader schema

        UnresolvedColumn tableCol = (UnresolvedColumn) col;
        ColumnMetadata readerCol = readerSchema.metadata(tableCol.name());
        if (readerCol != null) {

          // Is a match, project this reader column

          int index = readerSchema.index(col.name());
          readerProjectionMap[index] = true;
          rootTuple.add(
              new ResolvedTableColumn(tableCol.metadata(), rootTuple, index));
        } else {

          // No match, project a null column

          rootTuple.add(rootTuple.nullBuilder.add(tableCol.metadata()));
        }
      } else {

        // Not a schema column, handle specially

        resolveSpecial(rootTuple, col, readerSchema);
      }
    }

    // If lenient wildcard projection, add unmatched reader columns.

    if (scanProj.projectionType() == ScanProjectionType.SCHEMA_WILDCARD) {
      for (int i = 0; i < readerProjectionMap.length; i++) {
        if (readerProjectionMap[i]) {
          continue;
        }
        ColumnMetadata readerCol = readerSchema.metadata(i);
        if (! readerCol.booleanProperty(ColumnMetadata.EXCLUDE_FROM_WILDCARD)) {
          rootTuple.add(
              new ResolvedTableColumn(readerCol.name(),
                  readerCol.schema(), rootTuple, i));
        }
      }
    }
  }
}
