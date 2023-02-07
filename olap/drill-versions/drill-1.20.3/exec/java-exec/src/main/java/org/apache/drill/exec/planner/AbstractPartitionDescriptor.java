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

import java.util.Iterator;
import java.util.List;

import org.apache.calcite.rel.core.TableScan;
import org.apache.drill.exec.store.dfs.MetadataContext;
import org.apache.hadoop.fs.Path;

/**
 * Abstract base class for file system based partition descriptors and Hive
 * partition descriptors.
 */
public abstract class AbstractPartitionDescriptor implements PartitionDescriptor, Iterable<List<PartitionLocation>> {

  /**
   * A sequence of sublists of partition locations combined into a single super
   * list. The size of each sublist is at most
   * {@link PartitionDescriptor#PARTITION_BATCH_SIZE} For example if the size is
   * 3, the complete list could be: {(a, b, c), {d, e, f), (g, h)}
   */
  protected List<List<PartitionLocation>> locationSuperList;

  /**
   * Indicates if the sublists of the partition locations has been created
   */
  protected boolean sublistsCreated;

  /**
   * Create sublists of the partition locations, each sublist of size
   * at most {@link PartitionDescriptor#PARTITION_BATCH_SIZE}
   */
  protected abstract void createPartitionSublists();

  /**
   * Iterator that traverses over the super list of partition locations and
   * each time returns a single sublist of partition locations.
   */
  @Override
  public Iterator<List<PartitionLocation>> iterator() {
    if (!sublistsCreated) {
      createPartitionSublists();
    }
    return locationSuperList.iterator();
  }

  @Override
  public boolean supportsMetadataCachePruning() {
    return false;
  }

  @Override
  public TableScan createTableScan(List<PartitionLocation> newPartitions, Path cacheFileRoot,
      boolean isAllPruned, MetadataContext metaContext) throws Exception {
    throw new UnsupportedOperationException();
  }
}
