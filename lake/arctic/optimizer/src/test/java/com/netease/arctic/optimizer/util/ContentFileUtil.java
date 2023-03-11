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

package com.netease.arctic.optimizer.util;

import com.netease.arctic.ams.api.DataFileInfo;
import com.netease.arctic.data.DataFileType;
import com.netease.arctic.data.file.ContentFileWithSequence;
import com.netease.arctic.data.file.WrapFileWithSequenceNumberHelper;
import org.apache.iceberg.ContentFile;
import org.apache.iceberg.DataFiles;
import org.apache.iceberg.FileMetadata;
import org.apache.iceberg.PartitionSpec;

public class ContentFileUtil {

  public static ContentFileWithSequence<?> buildContentFile(DataFileInfo dataFileInfo,
                                                PartitionSpec partitionSpec,
                                                String fileFormat) {
    ContentFile<?> contentFile;
    if (DataFileType.POS_DELETE_FILE == DataFileType.valueOf(dataFileInfo.getType())) {
      if (partitionSpec.isUnpartitioned()) {
        contentFile = FileMetadata.deleteFileBuilder(partitionSpec)
            .ofPositionDeletes()
            .withPath(dataFileInfo.getPath())
            .withFormat(fileFormat)
            .withFileSizeInBytes(dataFileInfo.getSize())
            .withRecordCount(dataFileInfo.getRecordCount())
            .build();
      } else {
        contentFile = FileMetadata.deleteFileBuilder(partitionSpec)
            .ofPositionDeletes()
            .withPath(dataFileInfo.getPath())
            .withFormat(fileFormat)
            .withFileSizeInBytes(dataFileInfo.getSize())
            .withRecordCount(dataFileInfo.getRecordCount())
            .withPartitionPath(dataFileInfo.getPartition())
            .build();
      }
    } else {
      if (partitionSpec.isUnpartitioned()) {
        contentFile = DataFiles.builder(partitionSpec)
            .withPath(dataFileInfo.getPath())
            .withFormat(fileFormat)
            .withFileSizeInBytes(dataFileInfo.getSize())
            .withRecordCount(dataFileInfo.getRecordCount())
            .build();
      } else {
        contentFile = DataFiles.builder(partitionSpec)
            .withPath(dataFileInfo.getPath())
            .withFormat(fileFormat)
            .withFileSizeInBytes(dataFileInfo.getSize())
            .withRecordCount(dataFileInfo.getRecordCount())
            .withPartitionPath(dataFileInfo.getPartition())
            .build();
      }
    }

    return WrapFileWithSequenceNumberHelper.wrap(contentFile, dataFileInfo.sequence);
  }
}
