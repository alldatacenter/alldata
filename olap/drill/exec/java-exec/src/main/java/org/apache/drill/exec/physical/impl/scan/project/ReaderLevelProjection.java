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

import java.util.ArrayList;
import java.util.List;

import org.apache.drill.exec.record.metadata.TupleMetadata;

/**
 * Computes the full output schema given a table (or batch)
 * schema. Takes the original, unresolved output list from the projection
 * definition, merges it with the file, directory and table schema information,
 * and produces a partially or fully resolved output list.
 * <p>
 * A "resolved" projection list is a list of concrete columns: table
 * columns, nulls, file metadata or partition metadata. An unresolved list
 * has either table column names, but no match, or a wildcard column.
 * <p>
 * The idea is that the projection list moves through stages of resolution
 * depending on which information is available. An "early schema" table
 * provides schema information up front, and so allows fully resolving
 * the projection list on table open. A "late schema" table allows only a
 * partially resolved projection list, with the remainder of resolution
 * happening on the first (or perhaps every) batch.
 * <p>
 * Data source (table) schema can be of two forms:
 * <ul>
 * <li>Early schema: the schema is known before reading data. A JDBC data
 * source is an example, as is a CSV reader for a file with headers.</li>
 * <li>Late schema: the schema is not known until data is read, and is
 * discovered on the fly. Example: JSON, which declares values as maps
 * without an up-front schema.</li>
 * </ul>
 * These two forms give rise to distinct ways of planning the projection.
 * <p>
 * The final result of the projection is a set of "output" columns: a set
 * of columns that, taken together, defines the row (bundle of vectors) that
 * the scan operator produces. Columns are ordered: the order specified here
 * must match the order that columns appear in the result set loader and the
 * vector container so that code can access columns by index as well as name.
 *
 * @see {@link ImplicitColumnExplorer}, the class from which this class
 * evolved
 */
public class ReaderLevelProjection {

  /**
   * Reader-level projection is customizable. Implement this interface, and
   * add an instance to the scan orchestrator, to perform custom mappings
   * from unresolved columns (perhaps of an extension-specified type) to
   * final projected columns. The metadata manager, for example, implements
   * this interface to map metadata columns.
   */
  public interface ReaderProjectionResolver {
    void startResolution();
    boolean resolveColumn(ColumnProjection col, ResolvedTuple tuple,
        TupleMetadata tableSchema);
  }

  protected final List<ReaderProjectionResolver> resolvers;

  protected ReaderLevelProjection(
        List<ReaderProjectionResolver> resolvers) {
    this.resolvers = resolvers == null ? new ArrayList<>() : resolvers;
    for (ReaderProjectionResolver resolver : resolvers) {
      resolver.startResolution();
    }
  }

  protected void resolveSpecial(ResolvedTuple rootOutputTuple, ColumnProjection col,
      TupleMetadata tableSchema) {
    for (ReaderProjectionResolver resolver : resolvers) {
      if (resolver.resolveColumn(col, rootOutputTuple, tableSchema)) {
        return;
      }
    }
    throw new IllegalStateException(
        String.format("No resolver for column `%s` of type %s",
            col.name(), col.getClass().getSimpleName()));
  }
}
