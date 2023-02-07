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

import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

import org.apache.calcite.rex.RexNode;
import org.apache.drill.common.expression.SchemaPath;

/**
 * Abstract base class for Index collection (collection of Index descriptors)
 *
 */
public abstract class AbstractIndexCollection implements IndexCollection, Iterable<IndexDescriptor> {

  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AbstractIndexCollection.class);
  /**
   * A set of indexes for a particular table
   */
  @JsonProperty
  protected List<IndexDescriptor> indexes;

  public AbstractIndexCollection() {
    indexes = Lists.newArrayList();
  }

  @Override
  public boolean addIndex(IndexDescriptor index) {
    return indexes.add(index);
  }

  @Override
  public boolean removeIndex(IndexDescriptor index) {
    return indexes.remove(index);
  }

  @Override
  public void clearAll() {
    indexes.clear();
  }

  @Override
  public boolean supportsIndexSelection() {
    return false;
  }

  @Override
  public double getRows(RexNode indexCondition) {
    throw new UnsupportedOperationException("getRows() not supported for this index collection.");
  }

  @Override
  public boolean supportsRowCountStats() {
    return false;
  }

  @Override
  public boolean supportsFullTextSearch() {
    return false;
  }

  @Override
  public boolean isColumnIndexed(SchemaPath path) {
    for (IndexDescriptor index : indexes) {
      if (index.getIndexColumnOrdinal(path) >= 0) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Iterator<IndexDescriptor> iterator() {
    return indexes.iterator();
  }

}
