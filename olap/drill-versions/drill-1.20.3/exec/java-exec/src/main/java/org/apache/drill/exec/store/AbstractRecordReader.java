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
package org.apache.drill.exec.store;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.drill.common.expression.PathSegment;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.util.Utilities;
import org.apache.drill.exec.vector.ValueVector;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

public abstract class AbstractRecordReader implements RecordReader {
  private static final Logger logger = LoggerFactory.getLogger(AbstractRecordReader.class);

  // For text reader, the default columns to read is "columns[0]".
  protected static final List<SchemaPath> DEFAULT_TEXT_COLS_TO_READ = ImmutableList.of(new SchemaPath(new PathSegment.NameSegment("columns", new PathSegment.ArraySegment(0))));

  private Collection<SchemaPath> columns;
  private boolean isStarQuery;
  private boolean isSkipQuery;

  @Override
  public String toString() {
    return super.toString()
        + "[columns = " + columns
        + ", isStarQuery = " + isStarQuery
        + ", isSkipQuery = " + isSkipQuery + "]";
  }

  /**
   *
   * @param projected : The column list to be returned from this RecordReader.
   *                  1) empty column list: this is for skipAll query. It's up to each storage-plugin to
   *                  choose different policy of handling skipAll query. By default, it will use * column.
   *                  2) NULL : is NOT allowed. It requires the planner's rule, or GroupScan or ScanBatchCreator to handle NULL.
   */
  protected final void setColumns(Collection<SchemaPath> projected) {
    Preconditions.checkNotNull(projected, Utilities.COL_NULL_ERROR);
    isSkipQuery = projected.isEmpty();
    Collection<SchemaPath> columnsToRead = projected;

    // If no column is required (SkipQuery), by default it will use DEFAULT_COLS_TO_READ .
    // Handling SkipQuery is storage-plugin specific : JSON, text reader, parquet will override, in order to
    // improve query performance.
    if (projected.isEmpty()) {
      columnsToRead = getDefaultColumnsToRead();
    }

    isStarQuery = Utilities.isStarQuery(columnsToRead);
    columns = transformColumns(columnsToRead);

    logger.debug("columns to read : {}", columns);
  }

  protected Collection<SchemaPath> getColumns() {
    return columns;
  }

  protected Collection<SchemaPath> transformColumns(Collection<SchemaPath> projected) {
    return projected;
  }

  protected boolean isStarQuery() {
    return isStarQuery;
  }

  /**
   * Returns true if reader should skip all of the columns, reporting number of records only. Handling of a skip query
   * is storage plugin-specific.
   */
  protected boolean isSkipQuery() {
    return isSkipQuery;
  }

  @Override
  public void allocate(Map<String, ValueVector> vectorMap) throws OutOfMemoryException {
    for (final ValueVector v : vectorMap.values()) {
      v.allocateNew();
    }
  }

  @Override
  public boolean hasNext() {
    return false;
  }

  protected List<SchemaPath> getDefaultColumnsToRead() {
    return GroupScan.ALL_COLUMNS;
  }
}
