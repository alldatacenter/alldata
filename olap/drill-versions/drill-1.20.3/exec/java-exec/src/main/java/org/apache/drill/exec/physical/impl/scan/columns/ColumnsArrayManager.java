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
package org.apache.drill.exec.physical.impl.scan.columns;

import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.exec.physical.impl.scan.project.ColumnProjection;
import org.apache.drill.exec.physical.impl.scan.project.ReaderLevelProjection.ReaderProjectionResolver;
import org.apache.drill.exec.physical.impl.scan.project.ResolvedTuple;
import org.apache.drill.exec.physical.impl.scan.project.ScanLevelProjection.ScanProjectionParser;
import org.apache.drill.exec.physical.impl.scan.project.ScanSchemaOrchestrator;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;

/**
 * Handles the special case in which the entire row is returned as a
 * "columns" array. The SELECT list can include:
 * <ul>
 * <li><tt>`columns`</tt>: Use the columns array.</li>
 * <li>Wildcard: Equivalent to <tt>columns</tt>.</li>
 * <li>One or more references to specific array members:
 * <tt>columns[2], columns[4], ...</tt>.</li>
 * </ul>
 * In the first two forms, all columns are loaded into the
 * <tt>columns</tt> array. In the third form, only the columns
 * listed are loaded, the other slots will exist, but will be empty.
 * <p>
 * The <tt>columns</tt> mechanism works in conjunction with a reader that
 * is aware of this model. For example, the text reader can be configured
 * to use the <tt>columns</tt> mechanism, or to pick out columns by name.
 * The reader and this mechanism must be coordinated: configure this
 * mechanism only when the reader itself is configured to use model. That
 * configuration is done outside of this mechanism; it is typically done
 * when setting up the scan operator.
 * <p>
 * The output of this mechanism is a specialized projected column that
 * identifies itself as the <tt>columns</tt> column, and optionally holds
 * the list of selected elements.
 *
 * <h4>Configuration</h4>
 *
 * The mechanism handles two use cases:
 * <ul>
 * <li>The reader uses normal named columns. In this case the scan operator
 * should not configure this mechanism; that will allow a column called
 * <tt>columns</tt> to work like any other column: no special handling.</li>
 * <li>The reader uses a single <tt>columns</tt> array as described below.</li>
 * </ul>
 *
 * The scan operator configures this mechanism and adds it to the
 * {@link ScanSchemaOrchestrator}. The associated parser handles the scan-level
 * projection resolution.
 * <p>
 * The reader produces a schema with a single column, <tt>columns</tt>, of the
 * agreed-upon type (as configured here.)
 * <p>
 * Although the {@link ResultSetLoader} automatically handles column-level
 * projection; it does not handle array-level projection. (Perhaps we might want
 * to add that later.) Instead, the reader is given a pointer to this mechanism
 * from which it can retrieve the desired set of array items, and writes only those
 * items to the array column via the usual vector writer mechanism.
 */

public class ColumnsArrayManager implements ReaderProjectionResolver {

  // Internal

  private final ColumnsArrayParser parser;

  public ColumnsArrayManager(boolean requireColumnsArray, boolean allowOtherCols) {
    parser = new ColumnsArrayParser(requireColumnsArray, allowOtherCols);
  }

  @VisibleForTesting
  public ColumnsArrayManager(boolean requireColumnsArray) {
    this(requireColumnsArray, false);
  }

  public ScanProjectionParser projectionParser() { return parser; }

  public ReaderProjectionResolver resolver() { return this; }

  @Override
  public void startResolution() { }

  @Override
  public boolean resolveColumn(ColumnProjection col, ResolvedTuple outputTuple,
      TupleMetadata tableSchema) {
    if (! (col instanceof UnresolvedColumnsArrayColumn)) {
      return false;
    }

    // Verify that the reader fulfilled its obligation to return just
    // one column of the proper name and type.

    if (hasColumnsArrayColumn() && tableSchema.size() != 1) {
      throw new IllegalStateException("Table schema must have exactly one column.");
    }

    final int tabColIndex = tableSchema.index(ColumnsScanFramework.COLUMNS_COL);
    if (tabColIndex == -1) {
      throw new IllegalStateException("Table schema must include only one column named `" + ColumnsScanFramework.COLUMNS_COL + "`");
    }
    final MaterializedField tableCol = tableSchema.column(tabColIndex);
    if (tableCol.getType().getMode() != DataMode.REPEATED) {
      throw new IllegalStateException("Table schema column `" + ColumnsScanFramework.COLUMNS_COL +
          "` is of mode " + tableCol.getType().getMode() +
          " but expected " + DataMode.REPEATED);
    }

    // Turn the columns array column into a routine table column.

    outputTuple.add(new ResolvedColumnsArrayColumn((UnresolvedColumnsArrayColumn) col,
        tableCol, outputTuple, tabColIndex));
    return true;
  }

  public boolean[] elementProjection() {
    final UnresolvedColumnsArrayColumn columnsArrayCol = parser.columnsArrayCol();
    return columnsArrayCol == null ? null : columnsArrayCol.selectedIndexes();
  }

  public boolean hasColumnsArrayColumn() {
    return parser.columnsArrayCol() != null;
  }
}
