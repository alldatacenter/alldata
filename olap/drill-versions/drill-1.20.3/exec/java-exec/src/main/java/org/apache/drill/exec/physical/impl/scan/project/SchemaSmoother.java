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

import org.apache.drill.exec.physical.impl.scan.project.ReaderLevelProjection.ReaderProjectionResolver;
import org.apache.drill.exec.record.metadata.TupleMetadata;

/**
 * Implements a "schema smoothing" algorithm.
 * Schema persistence for the wildcard selection (i.e. SELECT *)
 * <p>
 * Constraints:
 * <ul>
 * <li>Adding columns causes a hard schema change.</li>
 * <li>Removing columns is allowed, uses type from previous
 * schema, as long as previous mode was nullable or repeated.</li>
 * <li>Changing type or mode causes a hard schema change.</li>
 * <li>Changing column order is fine; use order from previous
 * schema.</li>
 * </ul>
 * This can all be boiled down to a simpler rule:
 * <ul>
 * <li>Schema persistence is possible if the output schema
 * from a prior schema can be reused for the current schema.</li>
 * <li>Else, a hard schema change occurs and a new output
 * schema is derived from the new table schema.</li>
 * </ul>
 * The core idea here is to "unresolve" a fully-resolved table schema
 * to produce a new projection list that is the equivalent of using that
 * prior projection list in the SELECT. Then, keep that projection list only
 * if it is compatible with the next table schema, else throw it away and
 * start over from the actual scan projection list.
 * <p>
 * Algorithm:
 * <ul>
 * <li>If partitions are included in the wildcard, and the new
 * file needs more than the current one, create a new schema.</li>
 * <li>Else, treat partitions as select, fill in missing with
 * nulls.</li>
 * <li>From an output schema, construct a new select list
 * specification as though the columns in the current schema were
 * explicitly specified in the SELECT clause.</li>
 * <li>For each new schema column, verify that the column exists
 * in the generated SELECT clause and is of the same type.
 * If not, create a new schema.</li>
 * <li>Use the generated schema to plan a new projection from
 * the new schema to the prior schema.</li>
 * </ul>
 */

public class SchemaSmoother {

  /**
   * Exception thrown if the prior schema is not compatible with the
   * new table schema.
   */

  @SuppressWarnings("serial")
  public static class IncompatibleSchemaException extends Exception { }

  private final ScanLevelProjection scanProj;
  private final List<ReaderProjectionResolver> resolvers;
  private ResolvedTuple priorSchema;
  private int schemaVersion = 0;

  public SchemaSmoother(ScanLevelProjection scanProj,
      List<ReaderProjectionResolver> resolvers) {
    this.scanProj = scanProj;
    this.resolvers = resolvers;
  }

  public ReaderLevelProjection resolve(
      TupleMetadata tableSchema, ResolvedTuple outputTuple) {

    // If a prior schema exists, try resolving the new table using the
    // prior schema. If this works, use the projection. Else, start
    // over with the scan projection.

    if (priorSchema != null) {
      try {
        SmoothingProjection smoother = new SmoothingProjection(scanProj, tableSchema,
            priorSchema, outputTuple, resolvers);
        priorSchema = outputTuple;
        return smoother;
      } catch (IncompatibleSchemaException e) {
        outputTuple.reset();
        // Fall through
      }
    }

    // Can't use the prior schema. Start over with the original scan projection.
    // Type smoothing is provided by the vector cache; but a hard schema change
    // will occur because either a type has changed or a new column has appeared.
    // (Or, this is the first schema.)

    ReaderLevelProjection schemaProj = new WildcardProjection(scanProj,
        tableSchema, outputTuple, resolvers);
    priorSchema = outputTuple;
    schemaVersion++;
    return schemaProj;
  }

  public int schemaVersion() { return schemaVersion; }
}
