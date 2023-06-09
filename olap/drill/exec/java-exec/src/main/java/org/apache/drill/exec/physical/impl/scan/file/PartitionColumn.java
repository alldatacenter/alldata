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
package org.apache.drill.exec.physical.impl.scan.file;

import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.impl.scan.project.VectorSource;

/**
 * Represents a partition column (usually dir0, dir1, ...). This is an implicit
 * column that has a variable part: the partition index.
 */
public class PartitionColumn extends MetadataColumn {

  protected final int partition;

  public PartitionColumn(String name, int partition) {
    super(name, dataType(), null, null, 0);
    this.partition = partition;
  }

  public PartitionColumn(String name, int partition,
      FileMetadata fileInfo, VectorSource source, int sourceIndex) {
    super(name, dataType(), fileInfo.partition(partition), source, sourceIndex);
    this.partition = partition;
  }

  public int partition() { return partition; }

  @Override
  public MetadataColumn resolve(FileMetadata fileInfo, VectorSource source, int sourceIndex) {
    return new PartitionColumn(name(), partition, fileInfo, source, sourceIndex);
  }

  public static MajorType dataType() {
    return MajorType.newBuilder()
          .setMinorType(MinorType.VARCHAR)
          .setMode(DataMode.OPTIONAL)
          .build();
  }
}
