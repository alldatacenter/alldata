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

import org.apache.hadoop.fs.Path;

import java.util.List;

/**
 * Interface to define a partition. Partition could be simple,
 * which represents a basic unit for partition, determined by
 * the underlying storage plugin. On file system, a simple partition
 * represents a file. Partition could be composite, consisting of
 * other partitions. On file system storage plugin, a composite
 * partition corresponds to a directory.
 *
 * Simple partition location keeps track the string representation of
 * partition and also stores the value of the individual partition keys
 * for this partition. Composite partition location keeps track the common
 * partition keys, but does not keep track the the string representation of
 * partition and leave it to each individual simple partition it consists of.
 */
public interface PartitionLocation {
  /**
   * Returns the value of the 'index' partition column
   */
  String getPartitionValue(int index);

  /**
   * Returns the path of this partition.
   * Only a non-composite partition supports this.
   */
  Path getEntirePartitionLocation();

  /**
   * Returns the list of the non-composite partitions that this partition consists of.
   */
  List<SimplePartitionLocation> getPartitionLocationRecursive();

  /**
   * Returns if this is a simple or composite partition.
   */
  boolean isCompositePartition();

  /**
   * Returns the path string of directory names only for composite partition
   */
  Path getCompositePartitionPath();

}
