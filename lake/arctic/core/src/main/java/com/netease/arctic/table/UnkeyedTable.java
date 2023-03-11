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

package com.netease.arctic.table;

import com.netease.arctic.op.UpdatePartitionProperties;
import org.apache.iceberg.Table;
import org.apache.iceberg.Transaction;
import org.apache.iceberg.util.StructLikeMap;

import java.util.Map;

/**
 * Represents an arctic table without keys supported, the same as an {@link Table}
 */
public interface UnkeyedTable extends ArcticTable, Table {

  /**
   * Returns the partition properties map.
   */
  StructLikeMap<Map<String, String>> partitionProperty();

  /**
   * Create a new {@link UpdatePartitionProperties} to update partition properties and commit the changes.
   *
   * @param transaction the transaction to update partition properties
   * @return a new {@link UpdatePartitionProperties}
   */
  UpdatePartitionProperties updatePartitionProperties(Transaction transaction);

  @Override
  default String name() {
    return toString();
  }

  @Override
  default boolean isUnkeyedTable() {
    return true;
  }

  @Override
  default UnkeyedTable asUnkeyedTable() {
    return this;
  }
}
