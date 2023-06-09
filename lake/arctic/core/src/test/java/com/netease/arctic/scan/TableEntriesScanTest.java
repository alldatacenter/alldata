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

package com.netease.arctic.scan;

import com.netease.arctic.IcebergFileEntry;
import com.netease.arctic.data.DataFileType;
import com.netease.arctic.data.file.FileNameGenerator;
import com.netease.arctic.io.TableDataTestBase;
import org.apache.iceberg.ContentFile;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.FileContent;
import org.apache.iceberg.Table;
import org.apache.iceberg.expressions.Expressions;
import org.junit.Assert;
import org.junit.Test;

public class TableEntriesScanTest extends TableDataTestBase {

  @Test
  public void testScanDataEntries() {
    // change table commit 2 insert files, then commit 1 delete file
    Table changeTable = getArcticTable().asKeyedTable().changeTable();
    TableEntriesScan dataFileScan = TableEntriesScan.builder(changeTable)
        .includeFileContent(FileContent.DATA)
        .build();
    long currentSnapshotId = changeTable.currentSnapshot().snapshotId();
    int cnt = 0;
    for (IcebergFileEntry entry : dataFileScan.entries()) {
      cnt++;
      DataFile file = (DataFile) entry.getFile();
      DataFileType dataFileType = FileNameGenerator.parseFileTypeForChange(file.path().toString());
      if (dataFileType == DataFileType.INSERT_FILE) {
        Assert.assertEquals(1, entry.getSequenceNumber());
      } else if (dataFileType == DataFileType.EQ_DELETE_FILE) {
        Assert.assertEquals(2, entry.getSequenceNumber());
        Assert.assertEquals(currentSnapshotId, (long) entry.getSnapshotId());
      }
      Assert.assertEquals(FileContent.DATA, file.content());
    }
    Assert.assertEquals(3, cnt);
  }

  @Test
  public void testScanDeleteEntries() {
    // base table commit 4 insert files, then commit 1 pos-delete file
    Table baseTable = getArcticTable().asKeyedTable().baseTable();
    TableEntriesScan deleteFileScan = TableEntriesScan.builder(baseTable)
        .includeFileContent(FileContent.POSITION_DELETES)
        .build();
    long currentSnapshotId = baseTable.currentSnapshot().snapshotId();
    int cnt = 0;
    for (IcebergFileEntry entry : deleteFileScan.entries()) {
      cnt++;
      DeleteFile file = (DeleteFile) entry.getFile();
      Assert.assertEquals(2, entry.getSequenceNumber());
      Assert.assertEquals(currentSnapshotId, (long) entry.getSnapshotId());
      Assert.assertEquals(FileContent.POSITION_DELETES, file.content());
    }
    Assert.assertEquals(1, cnt);
  }

  @Test
  public void testScanAllEntries() {
    // base table commit 4 insert files, then commit 1 pos-delete file
    Table baseTable = getArcticTable().asKeyedTable().baseTable();
    TableEntriesScan deleteFileScan = TableEntriesScan.builder(baseTable)
        .includeFileContent(FileContent.POSITION_DELETES, FileContent.DATA, FileContent.EQUALITY_DELETES)
        .build();
    long currentSnapshotId = baseTable.currentSnapshot().snapshotId();
    int cnt = 0;
    for (IcebergFileEntry entry : deleteFileScan.entries()) {
      cnt++;
      ContentFile<?> file = entry.getFile();
      if (file.content() == FileContent.DATA) {
        Assert.assertEquals(1, entry.getSequenceNumber());
      } else {
        Assert.assertEquals(2, entry.getSequenceNumber());
        Assert.assertEquals(currentSnapshotId, (long) entry.getSnapshotId());
        Assert.assertEquals(FileContent.POSITION_DELETES, file.content());
      }
    }
    Assert.assertEquals(5, cnt);
  }

  @Test
  public void testScanEntriesWithFilter() {
    // change table commit 2 insert files, then commit 1 delete file
    Table changeTable = getArcticTable().asKeyedTable().changeTable();
    TableEntriesScan dataFileScan = TableEntriesScan.builder(changeTable)
        .includeFileContent(FileContent.DATA)
        .withDataFilter(Expressions.equal("id", 5))
        .build();
    long currentSnapshotId = changeTable.currentSnapshot().snapshotId();
    int cnt = 0;
    for (IcebergFileEntry entry : dataFileScan.entries()) {
      cnt++;
      DataFile file = (DataFile) entry.getFile();
      DataFileType dataFileType = FileNameGenerator.parseFileTypeForChange(file.path().toString());
      if (dataFileType == DataFileType.INSERT_FILE) {
        Assert.assertEquals(1, entry.getSequenceNumber());
      } else if (dataFileType == DataFileType.EQ_DELETE_FILE) {
        Assert.assertEquals(2, entry.getSequenceNumber());
        Assert.assertEquals(currentSnapshotId, (long) entry.getSnapshotId());
      }
      Assert.assertEquals(FileContent.DATA, file.content());
    }
    Assert.assertEquals(2, cnt);
  }
}