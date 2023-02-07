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

import java.util.List;

/**
 * Exposes partition information for a particular schema.
 * <p>
 * For a more explanation of the current use of this interface see
 * the documentation in {@link PartitionExplorer}.
 */
public interface SchemaPartitionExplorer {

  /**
   * Get a list of sub-partitions of a particular table and the partitions
   * specified by partition columns and values. Individual storage
   * plugins will assign specific meaning to the parameters and return
   * values.
   * <p>
   * For more info see docs in {@link PartitionExplorer}.
   *
   * @param partitionColumns a list of partitions to match
   * @param partitionValues list of values of each partition (corresponding
   *                        to the partition column list)
   * @return list of sub-partitions, will be empty if a there is no further
   *         level of sub-partitioning below, i.e. hit a leaf partition
   * @throws PartitionNotFoundException when the partition does not exist in
   *          the given workspace
   */
  Iterable<String> getSubPartitions(String table,
                                    List<String> partitionColumns,
                                    List<String> partitionValues) throws PartitionNotFoundException;
}
