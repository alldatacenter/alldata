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

import org.apache.drill.exec.store.ColumnExplorer;
import org.apache.hadoop.fs.Path;

/**
 * Defines a single partition in a DFS table.
 */
public class DFSFilePartitionLocation extends SimplePartitionLocation {
  private final String[] dirs;
  private final Path file;

  public DFSFilePartitionLocation(int max, Path selectionRoot, Path file, boolean hasDirsOnly) {
    this.file = file;
    this.dirs = new String[max];

    String[] dirs = ColumnExplorer.parsePartitions(this.file, selectionRoot, hasDirsOnly);
    System.arraycopy(dirs, 0, this.dirs, 0, Math.min(max, dirs.length));
  }

  /**
   * Returns the value for a give partition key
   * @param index - Index of the partition key whose value is to be returned
   * @return The value for a partition.
   */
  @Override
  public String getPartitionValue(int index) {
    assert index < dirs.length;
    return dirs[index];
  }

  /**
   * Return the full location of this partition
   * @return The partition location.
   */
  @Override
  public Path getEntirePartitionLocation() {
    return file;
  }

  public String[] getDirs() {
    return dirs;
  }
}
