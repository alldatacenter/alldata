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

import org.apache.calcite.rex.RexNode;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.base.IndexGroupScan;

/**
 *  Interface used to describe an index collection
 */
public interface IndexCollection extends Iterable<IndexDescriptor> {
  /**
   * Types of an index collections: NATIVE_SECONDARY_INDEX_COLLECTION, EXTERNAL_SECONDARY_INDEX_COLLECTION
   */
  public static enum IndexCollectionType {
    NATIVE_SECONDARY_INDEX_COLLECTION,
    EXTERNAL_SECONDARY_INDEX_COLLECTION
  };

  /**
   * Add a new index to the collection. Return True if index was successfully added; False otherwise
   */
  public boolean addIndex(IndexDescriptor index);

  /**
   * Remove an index (identified by table name and index name) from the collection.
   * Return True if index was successfully removed; False otherwise
   */
  public boolean removeIndex(IndexDescriptor index);

  /**
   * Clears all entries from this index collection
   */
  public void clearAll();

  /**
   * Get the type of this index based on {@link IndexCollectionType}
   * @return one of the values in {@link IndexCollectionType}
   */
  public IndexCollectionType getIndexCollectionType();

  /**
   * Whether or not this index collection supports index selection (selecting an
   * appropriate index out of multiple candidates). Typically, external index collections
   * such as Elasticsearch already have this capability while native secondary index collection
   * may not have - in such cases, Drill needs to do the index selection.
   */
  public boolean supportsIndexSelection();

  /**
   * Get the estimated row count for a single index condition
   * @param indexCondition The index condition (e.g index_col1 < 10 AND index_col2 = 'abc')
   * @return The estimated row count
   */
  public double getRows(RexNode indexCondition);

  /**
   * Whether or not the index supports getting row count statistics
   * @return True if index supports getting row count, False otherwise
   */
  public boolean supportsRowCountStats();

  /**
   * Whether or not the index supports full-text search (to allow pushing down such filters)
   * @return True if index supports full-text search, False otherwise
   */
  public boolean supportsFullTextSearch();

  /**
   * If this IndexCollection exposes a single GroupScan, return the GroupScan instance. For external indexes
   * such as Elasticsearch, we may have a single GroupScan representing all the indexes contained
   * within that collection.  On the other hand, for native indexes, each separate index would
   * have its own GroupScan.
   * @return GroupScan for this IndexCollection if available, otherwise null
   */
  public IndexGroupScan getGroupScan();

  /**
   * Check if the field name is the leading key of any of the indexes in this collection
   * @param path
   * @return True if an appropriate index is found, False otherwise
   */
  public boolean isColumnIndexed(SchemaPath path);

}
