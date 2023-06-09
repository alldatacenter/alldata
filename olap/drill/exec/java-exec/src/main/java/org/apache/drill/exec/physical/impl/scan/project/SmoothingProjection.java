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

import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.exec.physical.impl.scan.project.SchemaSmoother.IncompatibleSchemaException;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.metadata.TupleMetadata;

/**
 * Resolve a table schema against the prior schema. This works only if the
 * types match and if all columns in the table schema already appear in the
 * prior schema.
 * <p>
 * Consider this an experimental mechanism. The hope was that, with clever
 * techniques, we could "smooth over" some of the issues that cause schema
 * change events in Drill. As it turned out, however, creating this mechanism
 * revealed that it is not possible, even in theory, to handle most schema
 * changes because of the time dimension:
 * <ul>
 * <li>An even in a later batch may provide information that would have
 * caused us to make a different decision in an earlier batch. For example,
 * we are asked for column `foo`, did not see such a column in the first
 * batch, block or file, guessed some type, and later saw that the column
 * was of a different type. We can't "time travel" to tell our earlier
 * selves, nor, when we make the initial type decision, can we jump to
 * the future to see what type we'll discover.</li>
 * <li>Readers in this fragment may see column `foo` but readers in
 * another fragment read files/blocks that don't have that column. The
 * two readers cannot communicate to agree on a type.</li>
 * </ul>
 * <p>
 * What this mechanism can do is make decisions based on history: when a
 * column appears, we can adjust its type a bit to try to avoid an
 * unnecessary change. For example, if a prior file in this scan saw
 * `foo` as nullable Varchar, but the present file has the column as
 * requied Varchar, we can use the more general nullable form. But,
 * again, the "can't predict the future" bites us: we can handle a
 * nullable-to-required column change, but not visa-versa.
 * <p>
 * What this mechanism will tell the careful reader is that the only
 * general solution to the schema-change problem is to now the full
 * schema up front: for the planner to be told the schema and to
 * communicate that schema to all readers so that all readers agree
 * on the final schema.
 * <p>
 * When that is done, the techniques shown here can be used to adjust
 * any per-file variation of schema to match the up-front schema.
 */

public class SmoothingProjection extends ReaderLevelProjection {

  protected final List<MaterializedField> rewrittenFields = new ArrayList<>();

  public SmoothingProjection(ScanLevelProjection scanProj,
      TupleMetadata tableSchema,
      ResolvedTuple priorSchema,
      ResolvedTuple outputTuple,
      List<ReaderProjectionResolver> resolvers) throws IncompatibleSchemaException {

    super(resolvers);

    for (ResolvedColumn priorCol : priorSchema.columns()) {
      if (priorCol instanceof ResolvedTableColumn ||
        priorCol instanceof  ResolvedNullColumn) {
        // This is a regular column known to this base framework.
        resolveColumn(outputTuple, priorCol, tableSchema);
      } else {
        // The column is one known to an add-on mechanism.
        resolveSpecial(outputTuple, priorCol, tableSchema);
      }
    }

    // Check if all table columns were matched. Since names are unique,
    // each column can be matched at most once. If the number of matches is
    // less than the total number of columns, then some columns were not
    // matched and we must start over.

    if (rewrittenFields.size() < tableSchema.size()) {
      throw new IncompatibleSchemaException();
    }
  }

  /**
   * Resolve a prior column against the current table schema. Resolves to
   * a table column, a null column, or throws an exception if the
   * schemas are incompatible
   *
   * @param priorCol a column from the prior schema
   * @throws IncompatibleSchemaException if the prior column exists in
   * the current table schema, but with an incompatible type
   */

  private void resolveColumn(ResolvedTuple outputTuple,
      ResolvedColumn priorCol, TupleMetadata tableSchema) throws IncompatibleSchemaException {
    int tableColIndex = tableSchema.index(priorCol.name());
    if (tableColIndex == -1) {
      resolveNullColumn(outputTuple, priorCol);
      return;
    }
    MaterializedField tableCol = tableSchema.column(tableColIndex);
    MaterializedField priorField = priorCol.schema();
    if (! tableCol.isPromotableTo(priorField, false)) {
      throw new IncompatibleSchemaException();
    }
    outputTuple.add(
        new ResolvedTableColumn(priorCol.name(), priorField, outputTuple, tableColIndex));
    rewrittenFields.add(priorField);
  }

  /**
   * A prior schema column does not exist in the present table column schema.
   * Create a null column with the same type as the prior column, as long as
   * the prior column was not required.
   *
   * @param priorCol the prior column to project to a null column
   * @throws IncompatibleSchemaException if the prior column was required
   * and thus cannot be null-filled
   */

  private void resolveNullColumn(ResolvedTuple outputTuple,
      ResolvedColumn priorCol) throws IncompatibleSchemaException {
    if (priorCol.schema().getType().getMode() == DataMode.REQUIRED) {
      throw new IncompatibleSchemaException();
    }
    outputTuple.add(outputTuple.nullBuilder().add(priorCol.name(),
        priorCol.schema().getType()));
  }

  public List<MaterializedField> revisedTableSchema() { return rewrittenFields; }
}
