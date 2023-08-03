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
import com.netease.arctic.io.TableDataTestBase;
import com.netease.arctic.io.writer.GenericBaseTaskWriter;
import com.netease.arctic.io.writer.GenericTaskWriters;
import com.netease.arctic.utils.ManifestEntryFields;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.DeleteFiles;
import org.apache.iceberg.FileContent;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.Table;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.expressions.Expressions;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.io.WriteResult;
import org.apache.iceberg.relocated.com.google.common.collect.Iterables;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestTableEntriesScan extends TableDataTestBase {

  @Test
  public void testScanEntriesForDataFile() {
    // change table commit 2 insert files, then commit 1 delete file
    Table changeTable = getArcticTable().asKeyedTable().changeTable();
    Map<String, Entry> expectedEntries = getExpectedCurrentEntries(changeTable);
    TableEntriesScan dataFileScan = TableEntriesScan.builder(changeTable)
        .includeFileContent(FileContent.DATA)
        .build();
    int cnt = 0;
    for (IcebergFileEntry entry : dataFileScan.entries()) {
      cnt++;
      assertEntry(expectedEntries, entry);
    }
    Assert.assertEquals(3, cnt);
  }

  @Test
  public void testScanEntriesForPosDeleteFiles() {
    // base table commit 4 insert files, then commit 1 pos-delete file
    Table baseTable = getArcticTable().asKeyedTable().baseTable();
    TableEntriesScan deleteFileScan = TableEntriesScan.builder(baseTable)
        .includeFileContent(FileContent.POSITION_DELETES)
        .build();
    Map<String, Entry> expectedEntries = getExpectedCurrentEntries(baseTable);
    int cnt = 0;
    for (IcebergFileEntry entry : deleteFileScan.entries()) {
      cnt++;
      assertEntry(expectedEntries, entry);
    }
    Assert.assertEquals(1, cnt);
  }

  @Test
  public void testScanAllEntries() throws IOException {
    // base table commit 4 insert files, then commit 1 pos-delete file
    Table baseTable = getArcticTable().asKeyedTable().baseTable();
    Snapshot snapshot1 = baseTable.currentSnapshot();
    Map<String, Entry> expectedEntries1 = getExpectedCurrentEntries(baseTable);

    // add and delete 4 files
    List<DataFile> dataFiles = writeIntoBase();
    DeleteFiles deleteFiles = baseTable.newDelete();
    dataFiles.forEach(deleteFiles::deleteFile);
    deleteFiles.commit();

    Map<String, Entry> expectedEntries2 = getExpectedCurrentEntries(baseTable);

    // using snapshot
    TableEntriesScan entryScan1 = TableEntriesScan.builder(baseTable)
        .includeFileContent(FileContent.POSITION_DELETES, FileContent.DATA, FileContent.EQUALITY_DELETES)
        .useSnapshot(snapshot1.snapshotId())
        .withAliveEntry(false)
        .build();
    int cnt = 0;
    for (IcebergFileEntry entry : entryScan1.entries()) {
      cnt++;
      assertEntry(expectedEntries1, entry);
    }
    Assert.assertEquals(5, cnt);

    // current
    TableEntriesScan entryScan2 = TableEntriesScan.builder(baseTable)
        .includeFileContent(FileContent.POSITION_DELETES, FileContent.DATA, FileContent.EQUALITY_DELETES)
        .withAliveEntry(false)
        .build();
    cnt = 0;
    for (IcebergFileEntry entry : entryScan2.entries()) {
      cnt++;
      assertEntry(expectedEntries2, entry);
    }
    Assert.assertEquals(9, cnt);

    // all entries (in all snapshots)
    TableEntriesScan entryScan3 = TableEntriesScan.builder(baseTable)
        .includeFileContent(FileContent.POSITION_DELETES, FileContent.DATA, FileContent.EQUALITY_DELETES)
        .withAliveEntry(false)
        .allEntries()
        .build();
    Assert.assertEquals(13, Iterables.size(entryScan3.entries()));
  }

  @Test
  public void testScanEntriesWithFilter() {
    // change table commit 2 insert files, then commit 1 delete file
    Table changeTable = getArcticTable().asKeyedTable().changeTable();
    TableEntriesScan dataFileScan = TableEntriesScan.builder(changeTable)
        .includeFileContent(FileContent.DATA)
        .withDataFilter(Expressions.equal("id", 5))
        .build();
    Map<String, Entry> expectedEntries = getExpectedCurrentEntries(changeTable);
    int cnt = 0;
    for (IcebergFileEntry entry : dataFileScan.entries()) {
      cnt++;
      assertEntry(expectedEntries, entry);
    }
    Assert.assertEquals(2, cnt);
  }

  @Test
  public void testScanEntriesFromSequence() throws IOException {
    // change table commit 2 insert files, then commit 1 delete file
    Table changeTable = getArcticTable().asKeyedTable().changeTable();
    TableEntriesScan.Builder builder = TableEntriesScan.builder(changeTable)
        .includeFileContent(FileContent.DATA)
        .fromSequence(2L);
    Map<String, Entry> expectedEntries = getExpectedCurrentEntries(changeTable);
    int cnt = 0;

    try (CloseableIterable<IcebergFileEntry> entries = builder.build().entries()) {
      for (IcebergFileEntry entry : entries) {
        cnt++;
        assertEntry(expectedEntries, entry);
      }
    }
    Assert.assertEquals(1, cnt);


    // base table commit 4 insert files, then commit 1 pos-delete file
    Table baseTable = getArcticTable().asKeyedTable().baseTable();
    builder = TableEntriesScan.builder(baseTable)
        .includeFileContent(FileContent.POSITION_DELETES)
        .fromSequence(2L);
    expectedEntries = getExpectedCurrentEntries(baseTable);
    cnt = 0;

    try (CloseableIterable<IcebergFileEntry> entries = builder.build().entries()) {
      for (IcebergFileEntry entry : entries) {
        cnt++;
        assertEntry(expectedEntries, entry);
      }
    }
    Assert.assertEquals(1, cnt);
  }

  private List<DataFile> writeIntoBase() throws IOException {
    long transactionId = getArcticTable().asKeyedTable().beginTransaction("");
    GenericBaseTaskWriter writer = GenericTaskWriters.builderFor(getArcticTable().asKeyedTable())
        .withTransactionId(transactionId).buildBaseWriter();

    for (Record record : baseRecords(allRecords)) {
      writer.write(record);
    }
    WriteResult result = writer.complete();
    AppendFiles baseAppend = getArcticTable().asKeyedTable().baseTable().newAppend();
    Arrays.stream(result.dataFiles()).forEach(baseAppend::appendFile);
    baseAppend.commit();
    return Arrays.asList(result.dataFiles());
  }

  private void assertEntry(Map<String, Entry> expected, IcebergFileEntry entry) {
    Entry expectEntry = expected.get(entry.getFile().path().toString());
    Assert.assertNotNull(expectEntry);
    Assert.assertEquals("fail file " + entry, expectEntry.getSequenceNumber(), entry.getSequenceNumber());
    Assert.assertEquals("fail file " + entry, expectEntry.getSnapshotId(), entry.getSnapshotId());
    Assert.assertEquals("fail file " + entry, expectEntry.getFileContent(), entry.getFile().content());
    if (expectEntry.isAliveEntry()) {
      assertAliveEntry(entry);
    } else {
      assertDeletedEntry(entry);
    }
  }

  private Map<String, Entry> getExpectedCurrentEntries(Table table) {
    Iterable<Snapshot> snapshots = table.snapshots();
    Map<String, Entry> result = new HashMap<>();
    for (Snapshot snapshot : snapshots) {
      long snapshotId = snapshot.snapshotId();
      long sequenceNumber = snapshot.sequenceNumber();
      for (DataFile addFile : snapshot.addedDataFiles(table.io())) {
        result.put(addFile.path().toString(), new Entry(snapshotId, sequenceNumber, addFile.content(), true));
      }
      for (DeleteFile addFile : snapshot.addedDeleteFiles(table.io())) {
        result.put(addFile.path().toString(), new Entry(snapshotId, sequenceNumber, addFile.content(), true));
      }
      for (DataFile removedFile : snapshot.removedDataFiles(table.io())) {
        Entry entry = result.get(removedFile.path().toString());
        // sequence for removed file is the sequence it added
        result.put(removedFile.path().toString(),
            new Entry(snapshotId, entry.getSequenceNumber(), removedFile.content(), false));
      }
      for (DeleteFile removedFile : snapshot.removedDeleteFiles(table.io())) {
        Entry entry = result.get(removedFile.path().toString());
        // sequence for removed file is the sequence it added
        result.put(removedFile.path().toString(),
            new Entry(snapshotId, entry.getSequenceNumber(), removedFile.content(), false));
      }
    }
    return result;
  }

  private static class Entry {
    private final Long snapshotId;
    private final Long sequenceNumber;
    private final boolean aliveEntry;
    private final FileContent fileContent;

    public Entry(Long snapshotId, Long sequenceNumber, FileContent fileContent, boolean aliveEntry) {
      this.snapshotId = snapshotId;
      this.sequenceNumber = sequenceNumber;
      this.fileContent = fileContent;
      this.aliveEntry = aliveEntry;
    }

    public Long getSnapshotId() {
      return snapshotId;
    }

    public Long getSequenceNumber() {
      return sequenceNumber;
    }

    public boolean isAliveEntry() {
      return aliveEntry;
    }

    public FileContent getFileContent() {
      return fileContent;
    }
  }

  private void assertAliveEntry(IcebergFileEntry entry) {
    Assert.assertTrue("fail file " + entry, entry.getStatus() == ManifestEntryFields.Status.ADDED ||
        entry.getStatus() == ManifestEntryFields.Status.EXISTING);
  }

  private void assertDeletedEntry(IcebergFileEntry entry) {
    Assert.assertSame("fail file " + entry, entry.getStatus(), ManifestEntryFields.Status.DELETED);
  }
}