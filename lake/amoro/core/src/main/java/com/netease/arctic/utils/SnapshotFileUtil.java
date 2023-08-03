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

package com.netease.arctic.utils;

import com.netease.arctic.table.ArcticTable;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SnapshotFileUtil {
  private static final Logger LOG = LoggerFactory.getLogger(SnapshotFileUtil.class);

  public static void getSnapshotFiles(
      ArcticTable table, String innerTable, Snapshot snapshot,
      List<com.netease.arctic.ams.api.DataFile> addFiles,
      List<com.netease.arctic.ams.api.DataFile> deleteFiles) {
    Preconditions.checkNotNull(addFiles, "Add files to delete can not be null");
    Preconditions.checkNotNull(deleteFiles, "Delete files to delete can not be null");

    for (DataFile file : snapshot.addedDataFiles(table.io())) {
      addFiles.add(ConvertStructUtil.convertToAmsDatafile(file, table, innerTable));
    }
    for (DataFile file : snapshot.removedDataFiles(table.io())) {
      deleteFiles.add(ConvertStructUtil.convertToAmsDatafile(file, table, innerTable));
    }
    for (DeleteFile file : snapshot.addedDeleteFiles(table.io())) {
      addFiles.add(ConvertStructUtil.convertToAmsDatafile(file, table, innerTable));
    }
    for (DeleteFile file : snapshot.removedDeleteFiles(table.io())) {
      deleteFiles.add(ConvertStructUtil.convertToAmsDatafile(file, table, innerTable));
    }

    LOG.debug("{} snapshot get {} add files count and {} delete file count.",
        snapshot.snapshotId(), addFiles.size(), deleteFiles.size());
  }
}
