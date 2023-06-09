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

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.physical.impl.scan.project.AbstractUnresolvedColumn.UnresolvedColumn;
import org.apache.drill.exec.physical.impl.scan.project.ColumnProjection;
import org.apache.drill.exec.physical.impl.scan.project.ScanLevelProjection;
import org.apache.drill.exec.physical.impl.scan.project.ScanLevelProjection.ScanProjectionParser;
import org.apache.drill.exec.physical.resultSet.project.RequestedColumn;
import org.apache.drill.exec.physical.resultSet.project.RequestedColumnImpl;
import org.apache.drill.exec.store.easy.text.TextFormatPlugin;
import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses the `columns` array. Doing so is surprisingly complex.
 * <ul>
 * <li>Depending on what is known about the input file, the `columns`
 * array may be required or optional.</li>
 * <li>If the columns array is required, then the wildcard (`*`)
 * expands to `columns`.</li>
 * <li>If the columns array appears, then no other table columns
 * can appear.</li>
 * <li>Both 'columns' and the wildcard can appear for queries such
 * as:<code><pre>
 * select * from dfs.`multilevel/csv`
 * where columns[1] < 1000</pre>
 * </code></li>
 * <li>The query can select specific elements such as `columns`[2].
 * In this case, only array elements can appear, not the unindexed
 * `columns` column.</li>
 * <li>If is possible for `columns` to appear twice. In this case,
 * the project operator will make a copy.</li>
 * </ul>
 * <p>
 * To handle these cases, the general rule is: allow any number
 * of wildcard or `columns` appearances in the input projection, but
 * collapse them all down to a single occurrence of `columns` in the
 * output projection. (Upstream code will prevent `columns` from
 * appearing twice in its non-indexed form.)
 * <p>
 * It falls to this parser to detect a not-uncommon user error, a
 * query such as the following:<pre><code>
 * SELECT max(columns[1]) AS col1
 * FROM cp.`textinput/input1.csv`
 * WHERE col1 IS NOT NULL
 * </code></pre>
 * In standard SQL, column aliases are not allowed in the WHERE
 * clause. So, Drill will push two columns down to the scan operator:
 * `columns`[1] and `col1`. This parser will detect the "extra"
 * columns and must provide a message that helps the user identify
 * the likely original problem.
 */
public class ColumnsArrayParser implements ScanProjectionParser {
  private static final Logger logger = LoggerFactory.getLogger(ColumnsArrayParser.class);

  // Config

  /**
   * True if the project list must include either the columns[] array
   * or the wildcard.
   */
  private final boolean requireColumnsArray;

  /**
   * True if the project list can include columns other than/in addition to
   * the columns[] array. Handy if the plugin provides special columns such
   * as the log regex plugin.
   */
  private final boolean allowOtherCols;

  // Internals

  private ScanLevelProjection builder;

  // Output

  private UnresolvedColumnsArrayColumn columnsArrayCol;

  public ColumnsArrayParser(boolean requireColumnsArray, boolean allowOtherCols) {
    this.requireColumnsArray = requireColumnsArray;
    this.allowOtherCols = allowOtherCols;
  }

  @VisibleForTesting
  public ColumnsArrayParser(boolean requireColumnsArray) {
    this(requireColumnsArray, false);
  }

  @Override
  public void bind(ScanLevelProjection builder) {
    this.builder = builder;
  }

  @Override
  public boolean parse(RequestedColumn inCol) {
    if (!requireColumnsArray && !allowOtherCols) {

      // If we do not require the columns array, then we presume that
      // the reader does not provide arrays, so any use of the columns[x]
      // column is likely an error. We rely on the plugin's own error
      // context to fill in information that would explain the issue
      // in the context of that plugin.

      if (inCol.isArray()) {
        throw UserException
            .validationError()
            .message("Unexpected `columns`[x]; columns array not enabled")
            .addContext(builder.context())
            .build(logger);
      }
      return false;
    }
    if (inCol.isWildcard()) {
      createColumnsCol(
          new RequestedColumnImpl(builder.rootProjection(), ColumnsScanFramework.COLUMNS_COL));
      return true;
    }
    if (!inCol.nameEquals(ColumnsScanFramework.COLUMNS_COL)) {
      return false;
    }

    // The columns column cannot be a map. That is, the following is
    // not allowed: columns.foo.

    if (inCol.isTuple() && !allowOtherCols) {
      throw UserException
        .validationError()
        .message("Column `%s` has map elements, but must be an array", inCol.name())
        .addContext(builder.context())
        .build(logger);
    }

    if (inCol.isArray()) {
      int maxIndex = inCol.maxIndex();
      if (maxIndex > TextFormatPlugin.MAXIMUM_NUMBER_COLUMNS) {
        throw UserException
          .validationError()
          .message("`columns`[%d] index out of bounds, max supported size is %d",
              maxIndex, TextFormatPlugin.MAXIMUM_NUMBER_COLUMNS)
          .addContext("Column:", inCol.name())
          .addContext("Maximum index:", TextFormatPlugin.MAXIMUM_NUMBER_COLUMNS)
          .addContext("Actual index:", maxIndex)
          .addContext(builder.context())
          .build(logger);
      }
    }
    createColumnsCol(inCol);
    return true;
  }

  private void createColumnsCol(RequestedColumn inCol) {

    // Special `columns` array column. Allow multiple, but
    // project only one.
    if (columnsArrayCol == null) {
      columnsArrayCol = new UnresolvedColumnsArrayColumn(inCol);
      builder.addTableColumn(columnsArrayCol);
    }
  }

  @Override
  public void validate() { }

  @Override
  public void validateColumn(ColumnProjection col) {
    if (col instanceof UnresolvedColumn) {
      if (columnsArrayCol != null) {
        throw UserException
          .validationError()
          .message("Cannot select columns[] and other table columns. "+
              "Column alias incorrectly used in the WHERE clause?")
          .addContext("Column name", col.name())
          .addContext(builder.context())
          .build(logger);
      }
      if (requireColumnsArray && !allowOtherCols) {
        throw UserException
          .validationError()
          .message("Only `columns` column is allowed. Found: " + col.name())
          .addContext(builder.context())
          .build(logger);
      }
    }
  }

  @Override
  public void build() { }

  public UnresolvedColumnsArrayColumn columnsArrayCol() { return columnsArrayCol; }
}
