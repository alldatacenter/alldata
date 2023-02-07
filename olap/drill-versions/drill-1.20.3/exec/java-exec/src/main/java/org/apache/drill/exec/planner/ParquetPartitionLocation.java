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

/*
 * PartitionLocation for the parquet auto partitioning scheme. We just store
 * the location of each partition within this class. Since the partition value
 * is obtained from the file metadata and not from the location string (like in directory based
 * partitioning scheme) we throw UnsupportedOperationException when getPartitionValue() is
 * invoked.
 */
public class ParquetPartitionLocation extends SimplePartitionLocation {
  private final Path file;

  public ParquetPartitionLocation(Path file) {
    this.file = file;
  }

  /**
   * Parquet CTAS auto partitioning scheme does not support getting the partition value
   * based on the location string.
   * @param index
   * @return
   */
  @Override
  public String getPartitionValue(int index) {
    throw new UnsupportedOperationException("Getting partitioning column value from the partition location is not " +
        "supported by parquet auto partitioning scheme");
  }

  /**
   * Get the location of this partition
   * @return String location of the partition
   */
  @Override
  public Path getEntirePartitionLocation() {
    return file;
  }
}
