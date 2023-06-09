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
package org.apache.drill.exec.planner;

import org.apache.calcite.rel.core.TableScan;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.store.dfs.MetadataContext;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.hadoop.fs.Path;

import java.util.BitSet;
import java.util.List;
import java.util.Map;

/**
 * Interface used to describe partitions. Currently used by file system based
 * partitions and hive partitions
 */
public interface PartitionDescriptor extends Iterable<List<PartitionLocation>> {

  int PARTITION_BATCH_SIZE = Character.MAX_VALUE;

  /**
   * Get the hierarchy index of the given partition
   * For eg: if we have the partition laid out as follows
   * 1997/q1/jan
   * then getPartitionHierarchyIndex("jan") => 2
   *
   * @param partitionName Partition name
   * @return the index of specified partition name in the hierarchy
   */
  int getPartitionHierarchyIndex(String partitionName);

  /**
   * Given a column name return boolean to indicate if its a partition column or not
   *
   * @param name of Partition
   * @return true, if this is the partition name and vise versa.
   */
  boolean isPartitionName(String name);

  /**
   * Check to see if the name is a partition name.
   *
   * @param name The field name you want to compare to partition names.
   * @return Return index if valid, otherwise return null;
   */
  Integer getIdIfValid(String name);

  /**
   * Maximum level of partition nesting/ hierarchy supported
   *
   * @return maximum supported level number of partition hierarchy
   */
  int getMaxHierarchyLevel();

  /**
   * Creates an in memory representation of all the partitions. For each level of partitioning we
   * will create a value vector which this method will populate for all the partitions with the values of the
   * partitioning key
   * @param vectors - Array of vectors in the container that need to be populated
   * @param partitions - List of all the partitions that exist in the table
   * @param partitionColumnBitSet - Partition columns selected in the query
   * @param fieldNameMap - Maps field ordinal to the field name
   */
  void populatePartitionVectors(ValueVector[] vectors, List<PartitionLocation> partitions,
                                BitSet partitionColumnBitSet, Map<Integer, String> fieldNameMap);

  /**
   * Method returns the Major type associated with the given column
   * @param column - column whose type should be determined
   * @param plannerSettings
   */
  TypeProtos.MajorType getVectorType(SchemaPath column, PlannerSettings plannerSettings);

  /**
   * Create a new TableScan rel node, given the lists of new partitions or new files to SCAN.
   * @param newPartitions
   * @param wasAllPartitionsPruned
   * @throws Exception
   */
  TableScan createTableScan(List<PartitionLocation> newPartitions,
      boolean wasAllPartitionsPruned) throws Exception;

  /**
   * Create a new TableScan rel node, given the lists of new partitions or new files to scan and a path
   * to a metadata cache file
   * @param newPartitions
   * @param cacheFileRoot
   * @param wasAllPartitionsPruned
   * @param metaContext
   * @throws Exception
   */
  TableScan createTableScan(List<PartitionLocation> newPartitions, Path cacheFileRoot,
      boolean wasAllPartitionsPruned, MetadataContext metaContext) throws Exception;

  boolean supportsMetadataCachePruning();

  Path getBaseTableLocation();
}
