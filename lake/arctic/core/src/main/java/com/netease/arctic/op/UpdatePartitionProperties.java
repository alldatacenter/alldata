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

package com.netease.arctic.op;

import org.apache.iceberg.PendingUpdate;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.util.StructLikeMap;

import java.util.Map;

/**
 * API for updating partition properties.
 * <p>
 * Apply returns the updated partition properties as a map for validation.
 * <p>
 * When committing, these changes will be applied to the current table metadata. Commit conflicts
 * will be resolved by applying the pending changes to the new table metadata.
 */
public interface UpdatePartitionProperties extends PendingUpdate<StructLikeMap<Map<String, String>>> {

  /**
   * Add a key/value property to the partition.
   *
   * @param partitionData partition to add property
   * @param key a String key
   * @param value a String value
   * @return this for method chaining
   * @throws NullPointerException If either the key or value is null
   */
  UpdatePartitionProperties set(StructLike partitionData, String key, String value);

  /**
   * Remove the given property key from the partition.
   *
   * @param partitionData partition to remove property
   * @param key a String key
   * @return this for method chaining
   * @throws NullPointerException If the key is null
   */
  UpdatePartitionProperties remove(StructLike partitionData, String key);

}
