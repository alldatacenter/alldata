/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.utils;

import com.netease.arctic.data.DataTreeNode;
import com.netease.arctic.data.PrimaryKeyData;
import com.netease.arctic.table.PrimaryKeySpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.util.Filter;

import java.util.Set;
import java.util.function.Function;

/**
 * Util to Filter data belong to specific {@link DataTreeNode}s.
 *
 * @param <T> to indicate the record data type
 */
public class NodeFilter<T> extends Filter<T> {
  // record not in nodes will be ignored, null means no record will be ignored.
  private final Set<DataTreeNode> sourceNodes;
  private final PrimaryKeyData primaryKey;
  private final Function<T, StructLike> asStructLike;

  public NodeFilter(Set<DataTreeNode> sourceNodes, Schema schema, PrimaryKeySpec primaryKeySpec,
                    Function<T, StructLike> asStructLike) {
    this.sourceNodes = sourceNodes;
    this.primaryKey = new PrimaryKeyData(primaryKeySpec, schema);
    this.asStructLike = asStructLike;
  }

  @Override
  protected boolean shouldKeep(T record) {
    if (sourceNodes == null) {
      return true;
    }

    if (sourceNodes.isEmpty()) {
      return false;
    }

    primaryKey.primaryKey(asStructLike.apply(record));
    return sourceNodes.stream().anyMatch(node -> (primaryKey.hashCode() & node.mask()) == node.index());
  }
}
