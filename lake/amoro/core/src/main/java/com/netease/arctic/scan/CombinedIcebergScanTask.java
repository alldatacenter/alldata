/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.scan;

import com.netease.arctic.data.IcebergDataFile;
import com.netease.arctic.data.IcebergDeleteFile;
import com.netease.arctic.io.reader.GenericIcebergDataReader;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Be used in {@link GenericIcebergDataReader} to read iceberg for optimizing
 */
public class CombinedIcebergScanTask {

  private final IcebergDataFile[] dataFiles;

  private final IcebergDeleteFile[] deleteFiles;

  private final PartitionSpec partitionSpec;

  private final StructLike partitionData;

  public CombinedIcebergScanTask(
      IcebergDataFile[] dataFiles,
      IcebergDeleteFile[] deleteFiles,
      PartitionSpec partitionSpec, StructLike partitionData) {
    this.dataFiles = dataFiles == null ? new IcebergDataFile[0] : dataFiles;
    this.deleteFiles = deleteFiles == null ? new IcebergDeleteFile[0] : deleteFiles;
    this.partitionSpec = partitionSpec;
    this.partitionData = partitionData;
  }

  public List<IcebergDataFile> getDataFiles() {
    return ImmutableList.copyOf(dataFiles);
  }

  public List<IcebergDeleteFile> getDeleteFiles() {
    return ImmutableList.copyOf(deleteFiles);
  }

  public PartitionSpec getPartitionSpec() {
    return partitionSpec;
  }

  public StructLike getPartitionData() {
    return partitionData;
  }
}
