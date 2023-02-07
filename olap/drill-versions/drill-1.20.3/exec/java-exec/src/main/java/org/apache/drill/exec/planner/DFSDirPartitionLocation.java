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
/**
 * Class defines a single partition corresponding to a directory in a DFS table.
 */
package org.apache.drill.exec.planner;


import org.apache.drill.exec.util.DrillFileSystemUtil;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.hadoop.fs.Path;

import java.util.Collection;
import java.util.List;

/**
 * Composite partition location corresponds to a directory in the file system.
 * */
public class DFSDirPartitionLocation implements PartitionLocation {
  // Similar to directory / file structures, subPartitions could be either a DFSDirPartitionLocation or DFSFilePartitionLocation
  private final Collection<PartitionLocation> subPartitions;
  private final String[] dirs;

  public DFSDirPartitionLocation(String[] dirs, Collection<PartitionLocation> subPartitions) {
    this.subPartitions = subPartitions;
    this.dirs = dirs;
  }

  @Override
  public String getPartitionValue(int index) {
    assert index < dirs.length;
    return dirs[index];
  }

  @Override
  public Path getEntirePartitionLocation() {
    throw new UnsupportedOperationException("Should not call getEntirePartitionLocation for composite partition location!");
  }

  @Override
  public List<SimplePartitionLocation> getPartitionLocationRecursive() {
    List<SimplePartitionLocation> results = Lists.newArrayList();

    for (final PartitionLocation partitionLocation : subPartitions) {
      results.addAll(partitionLocation.getPartitionLocationRecursive());
    }

    return results;
  }

  @Override
  public boolean isCompositePartition() {
    return true;
  }

  @Override
  public Path getCompositePartitionPath() {
    StringBuilder path = new StringBuilder();
    for (String dir : dirs) {
      if (dir == null) { // get the prefix
        break;
      }
      path.append("/")
          .append(dir);
    }

    return DrillFileSystemUtil.createPathSafe(path.toString());
  }
}
