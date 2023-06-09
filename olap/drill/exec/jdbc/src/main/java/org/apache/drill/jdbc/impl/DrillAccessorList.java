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
package org.apache.drill.jdbc.impl;

import java.sql.SQLException;

import org.apache.calcite.avatica.util.Cursor.Accessor;
import org.apache.drill.exec.expr.TypeHelper;
import org.apache.drill.exec.record.RecordBatchLoader;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.accessor.BoundCheckingAccessor;
import org.apache.drill.exec.vector.accessor.SqlAccessor;
import org.apache.drill.jdbc.JdbcApiSqlException;


class DrillAccessorList extends BasicList<Accessor> {

  @SuppressWarnings("unused")
  private static final org.slf4j.Logger logger =
      org.slf4j.LoggerFactory.getLogger(DrillAccessorList.class);
  /** "None" value for rowLastColumnOffset. */
  // (Not -1, since -1 can result from 0 (bad 1-based index) minus 1 (offset
  // from 1-based to 0-based indexing.)
  private static final int NULL_LAST_COLUMN_INDEX = -2;

  private AvaticaDrillSqlAccessor[] accessors = new AvaticaDrillSqlAccessor[0];

  /** Zero-based offset of last column referenced in current row.
   *  For {@link #wasNull()}. */
  private int rowLastColumnOffset = NULL_LAST_COLUMN_INDEX;


  /**
   * Resets last-column-referenced information for {@link #wasNull}.
   * Must be called whenever row is advanced (when {@link ResultSet#next()}
   * is called).
   */
  void clearLastColumnIndexedInRow() {
    rowLastColumnOffset = NULL_LAST_COLUMN_INDEX;
  }

  void generateAccessors(DrillCursor cursor, RecordBatchLoader currentBatch) {
    int cnt = currentBatch.getSchema().getFieldCount();
    accessors = new AvaticaDrillSqlAccessor[cnt];
    for(int i =0; i < cnt; i++){
      final ValueVector vector = currentBatch.getValueAccessorById(null, i).getValueVector();
      final SqlAccessor acc =
          new TypeConvertingSqlAccessor(
              new BoundCheckingAccessor(vector, TypeHelper.getSqlAccessor(vector))
              );
      accessors[i] = new AvaticaDrillSqlAccessor(acc, cursor);
    }
    clearLastColumnIndexedInRow();
  }

  /**
   * @param  accessorOffset  0-based index of accessor array (not 1-based SQL
   *           column index/ordinal value)
   */
  @Override
  public AvaticaDrillSqlAccessor get(final int accessorOffset) {
    final AvaticaDrillSqlAccessor accessor = accessors[accessorOffset];
    // Update lastColumnIndexedInRow after indexing accessors to not touch
    // lastColumnIndexedInRow in case of out-of-bounds exception.
    rowLastColumnOffset = accessorOffset;
    return accessor;
  }

  boolean wasNull() throws SQLException{
    if (NULL_LAST_COLUMN_INDEX == rowLastColumnOffset) {
      throw new JdbcApiSqlException(
          "ResultSet.wasNull() called without a preceding call to a column"
          + " getter method since the last call to ResultSet.next()");
    }
    return accessors[rowLastColumnOffset].wasNull();
  }

  @Override
  public int size() {
    return accessors.length;
  }

}
