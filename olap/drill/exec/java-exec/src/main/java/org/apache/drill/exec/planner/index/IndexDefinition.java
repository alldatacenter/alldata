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
package org.apache.drill.exec.planner.index;

import org.apache.calcite.rel.RelCollation;
import org.apache.calcite.rel.RelFieldCollation;
import org.apache.calcite.rel.RelFieldCollation.NullDirection;
import org.apache.drill.common.expression.LogicalExpression;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Top level interface used to define an index.
 */
public interface IndexDefinition {
  /**
   * Types of an index: PRIMARY_KEY_INDEX, NATIVE_SECONDARY_INDEX, EXTERNAL_SECONDARY_INDEX
   */
  static enum IndexType {
    PRIMARY_KEY_INDEX,
    NATIVE_SECONDARY_INDEX,
    EXTERNAL_SECONDARY_INDEX
  };

  /**
   * Check to see if the field name is an index column and if so return the ordinal position in the index
   * @param path The field path you want to compare to index column names.
   * @return Return ordinal of the indexed column if valid, otherwise return -1
   */
  int getIndexColumnOrdinal(LogicalExpression path);

  /**
   * Get the name of the index
   */
  String getIndexName();

  /**
   * Check if this index 'covers' all the columns specified in the supplied list of columns
   * @param columns
   * @return True for covering index, False for non-covering
   */
  boolean isCoveringIndex(List<LogicalExpression> columns);

  /**
   * Check if this index have all the columns specified in the supplied list of columns indexed
   * @param columns
   * @return True if all fields are indexed, False for some or all fields is not indexed
   */
  boolean allColumnsIndexed(Collection<LogicalExpression> columns);

  /**
   * Check if this index has some columns specified in the supplied list of columns indexed
   * @param columns
   * @return True if some fields are indexed, False if none of the fields are indexed
   */
  boolean someColumnsIndexed(Collection<LogicalExpression> columns);

  /**
   * Get the list of columns (typically 1 column) that constitute the row key (primary key)
   * @return
   */
  List<LogicalExpression> getRowKeyColumns();

  /**
   * Get the name of the table this index is associated with
   */
  String getTableName();

  /**
   * Get the type of this index based on {@link IndexType}
   * @return one of the values in {@link IndexType}
   */
  IndexType getIndexType();

  /**
   * Get the list of columns in the index key.
   */
  List<LogicalExpression> getIndexColumns();

  /**
   * Get the list of columns that are in the 'included' or 'covered' fields.
   */
  List<LogicalExpression> getNonIndexColumns();

  /**
   * Get the collation property (physical ordering) of the index.
   */
  RelCollation getCollation();

  /**
   * Get a mapping of the LogicalExpresion/SchemaPath to its field collation
   *  - e.g Ascending/Descending, Nulls First/Nulls Last
   */
  Map<LogicalExpression, RelFieldCollation> getCollationMap();

  /**
   * Get the nulls ordering of this index
   * @return True, if nulls first. False otherwise
   */
  NullDirection getNullsOrderingDirection();

}
