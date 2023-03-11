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

package com.netease.arctic.ams.server.utils;

import com.google.common.collect.Maps;
import com.netease.arctic.TableTestBase;
import com.netease.arctic.utils.SequenceNumberFetcher;
import jline.internal.Log;
import org.apache.hadoop.conf.Configuration;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.FileScanTask;
import org.apache.iceberg.OverwriteFiles;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.RewriteFiles;
import org.apache.iceberg.RowDelta;
import org.apache.iceberg.Schema;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.Table;
import org.apache.iceberg.TableProperties;
import org.apache.iceberg.Tables;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.hadoop.HadoopTables;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.relocated.com.google.common.collect.Multimap;
import org.apache.iceberg.relocated.com.google.common.collect.Multimaps;
import org.apache.iceberg.relocated.com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.netease.arctic.TableTestBase.newGenericRecord;
import static com.netease.arctic.TableTestBase.partitionData;
import static com.netease.arctic.TableTestBase.writeEqDeleteFile;
import static com.netease.arctic.TableTestBase.writeNewDataFile;
import static com.netease.arctic.TableTestBase.writePosDeleteFile;

public class SequenceNumberFetcherTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testUnPartitionTable() throws IOException {
    Tables hadoopTables = new HadoopTables(new Configuration());
    Map<String, String> tableProperties = Maps.newHashMap();
    tableProperties.put(TableProperties.FORMAT_VERSION, "2");

    String path = tempFolder.getRoot().getPath();
    Log.info(path);
    Table table = hadoopTables.create(TableTestBase.TABLE_SCHEMA, PartitionSpec.unpartitioned(), tableProperties,
        path + "/test/table1");
    testTable(table);
  }

  @Test
  public void testPartitionTable() throws IOException {
    Tables hadoopTables = new HadoopTables(new Configuration());
    Map<String, String> tableProperties = Maps.newHashMap();
    tableProperties.put(TableProperties.FORMAT_VERSION, "2");

    String path = tempFolder.getRoot().getPath();
    Log.info(path);
    Table table = hadoopTables.create(TableTestBase.TABLE_SCHEMA, TableTestBase.SPEC, tableProperties,
        path + "/test/table2");
    testTable(table);
  }

  @Test
  public void testV1IcebergTable() throws IOException {
    Tables hadoopTables = new HadoopTables(new Configuration());
    Map<String, String> tableProperties = Maps.newHashMap();
    tableProperties.put(TableProperties.FORMAT_VERSION, "1");

    String path = tempFolder.getRoot().getPath();
    Log.info(path);
    Table table = hadoopTables.create(TableTestBase.TABLE_SCHEMA, TableTestBase.SPEC, tableProperties,
        path + "/test/table3");
    Map<String, Long> checkedDeletes = Maps.newHashMap();
    Map<String, Long> checkedDataFiles = Maps.newHashMap();
    StructLike partitionData = partitionData(table.schema(), table.spec(), getOpTime());

    List<DataFile> dataFiles1 = insertDataFiles(table, 10);
    checkNewFileSequenceNumber(table, checkedDeletes, checkedDataFiles, 0);

    List<DataFile> dataFiles2 = insertDataFiles(table, 10);
    checkNewFileSequenceNumber(table, checkedDeletes, checkedDataFiles, 0);

    List<DataFile> dataFiles3 = overwriteDataFiles(table, dataFiles1,
        Collections.singletonList(writeNewDataFile(table, records(0, 10, table.schema()),
            partitionData)));
    checkNewFileSequenceNumber(table, checkedDeletes, checkedDataFiles, 0);


    List<DataFile> dataFiles4 = rewriteFiles(table, dataFiles2,
        Collections.singletonList(writeNewDataFile(table, records(0, 10, table.schema()), partitionData)),
        4);
    checkNewFileSequenceNumber(table, checkedDeletes, checkedDataFiles, 0);

  }

  private void testTable(Table table) throws IOException {
    Map<String, Long> checkedDeletes = Maps.newHashMap();
    Map<String, Long> checkedDataFiles = Maps.newHashMap();
    StructLike partitionData = partitionData(table.schema(), table.spec(), getOpTime());

    List<DataFile> dataFiles1 = insertDataFiles(table, 10);
    checkNewFileSequenceNumber(table, checkedDeletes, checkedDataFiles, 1);

    List<DeleteFile> deleteFiles1 = insertEqDeleteFiles(table, 1);
    checkNewFileSequenceNumber(table, checkedDeletes, checkedDataFiles, 2);

    List<DeleteFile> deleteFiles2 = insertPosDeleteFiles(table, dataFiles1);
    checkNewFileSequenceNumber(table, checkedDeletes, checkedDataFiles, 3);

    List<DataFile> dataFiles2 = insertDataFiles(table, 10);
    checkNewFileSequenceNumber(table, checkedDeletes, checkedDataFiles, 4);

    List<DataFile> dataFiles3 = overwriteDataFiles(table, dataFiles1,
        Collections.singletonList(writeNewDataFile(table, records(0, 10, table.schema()),
            partitionData)));
    checkNewFileSequenceNumber(table, checkedDeletes, checkedDataFiles, 5);

    List<DataFile> dataFiles4 = rewriteFiles(table, dataFiles2,
        Collections.singletonList(writeNewDataFile(table, records(0, 10, table.schema()), partitionData)),
        4);
    checkNewFileSequenceNumber(table, checkedDeletes, checkedDataFiles, 4);

    List<DeleteFile> deleteFiles3 = insertEqDeleteFiles(table, 1);
    checkNewFileSequenceNumber(table, checkedDeletes, checkedDataFiles, 7);

    List<DeleteFile> deleteFiles4 = insertPosDeleteFiles(table, dataFiles4);
    checkNewFileSequenceNumber(table, checkedDeletes, checkedDataFiles, 8);

    Set<DeleteFile> currentAllDeleteFiles = getCurrentAllDeleteFiles(table);
    Assert.assertEquals(2, currentAllDeleteFiles.size());
    Multimap<String, Long> file2Positions = Multimaps.newListMultimap(Maps.newHashMap(), Lists::newArrayList);
    for (DataFile dataFile : dataFiles3) {
      file2Positions.put(dataFile.path().toString(), 0L);
    }
    rewriteFiles(table, currentAllDeleteFiles,
        Collections.singletonList(writePosDeleteFile(table, file2Positions, partitionData)));
    checkNewFileSequenceNumber(table, checkedDeletes, checkedDataFiles, 9);

    insertDataFiles(table, 0);
    checkNewFileSequenceNumber(table, checkedDeletes, checkedDataFiles, 10);
  }

  private void checkNewFileSequenceNumber(Table table, Map<String, Long> checkedDeletes,
                                          Map<String, Long> checkedDataFiles,
                                          long expectSequence) {
    SequenceNumberFetcher sequenceNumberFetcher;
    sequenceNumberFetcher = SequenceNumberFetcher.with(table, table.currentSnapshot().snapshotId());
    for (FileScanTask fileScanTask : table.newScan().planFiles()) {
      String path = fileScanTask.file().path().toString();
      long sequenceNumber = sequenceNumberFetcher.sequenceNumberOf(path);
      if (checkedDataFiles.containsKey(path)) {
        Assert.assertEquals((long) checkedDataFiles.get(path), sequenceNumber);
      } else {
        checkedDataFiles.put(path, sequenceNumber);
        Assert.assertEquals(expectSequence, sequenceNumber);
      }
      List<DeleteFile> deletes = fileScanTask.deletes();
      for (DeleteFile delete : deletes) {
        path = delete.path().toString();
        sequenceNumber = sequenceNumberFetcher.sequenceNumberOf(path);
        if (checkedDeletes.containsKey(path)) {
          Assert.assertEquals((long) checkedDeletes.get(path), sequenceNumber);
        } else {
          checkedDeletes.put(path, sequenceNumber);
          Assert.assertEquals(expectSequence, sequenceNumber);
        }
      }
    }
  }

  private Set<DeleteFile> getCurrentAllDeleteFiles(Table table) {
    Set<DeleteFile> results = Sets.newHashSet();
    CloseableIterable<FileScanTask> fileScanTasks = table.newScan().planFiles();
    for (FileScanTask fileScanTask : fileScanTasks) {
      results.addAll(fileScanTask.deletes());
    }
    return results;
  }

  private static List<DataFile> insertDataFiles(Table table, int length) throws IOException {
    StructLike partitionData = partitionData(table.schema(), table.spec(), getOpTime());
    DataFile result = writeNewDataFile(table, records(0, length, table.schema()), partitionData);

    AppendFiles baseAppend = table.newAppend();
    baseAppend.appendFile(result);
    baseAppend.commit();

    return Collections.singletonList(result);
  }

  private List<DataFile> overwriteDataFiles(Table table, List<DataFile> toDeleteDataFiles,
                                            List<DataFile> newDataFiles) {
    OverwriteFiles overwrite = table.newOverwrite();
    toDeleteDataFiles.forEach(overwrite::deleteFile);
    newDataFiles.forEach(overwrite::addFile);
    overwrite.commit();

    return newDataFiles;
  }

  private List<DataFile> rewriteFiles(Table table, List<DataFile> toDeleteDataFiles, List<DataFile> newDataFiles,
                                      long sequence) {
    RewriteFiles rewriteFiles = table.newRewrite();
    rewriteFiles.rewriteFiles(Sets.newHashSet(toDeleteDataFiles), Sets.newHashSet(newDataFiles), sequence);
    rewriteFiles.validateFromSnapshot(table.currentSnapshot().snapshotId());
    rewriteFiles.commit();
    return newDataFiles;
  }

  private List<DeleteFile> rewriteFiles(Table table, Set<DeleteFile> toDeleteFiles,
                                        List<DeleteFile> newFiles) {
    RewriteFiles rewriteFiles = table.newRewrite();
    rewriteFiles.rewriteFiles(Collections.emptySet(), Sets.newHashSet(toDeleteFiles), Collections.emptySet(),
        Sets.newHashSet(newFiles));
    rewriteFiles.validateFromSnapshot(table.currentSnapshot().snapshotId());
    rewriteFiles.commit();
    return newFiles;
  }

  private List<DeleteFile> insertEqDeleteFiles(Table table, int length) throws IOException {
    StructLike partitionData = partitionData(table.schema(), table.spec(), getOpTime());
    DeleteFile result = writeEqDeleteFile(table, records(0, length, table.schema()), partitionData);

    RowDelta rowDelta = table.newRowDelta();
    rowDelta.addDeletes(result);
    rowDelta.commit();
    return Collections.singletonList(result);
  }

  private List<DeleteFile> insertPosDeleteFiles(Table table, List<DataFile> dataFiles) throws IOException {
    Multimap<String, Long> file2Positions = Multimaps.newListMultimap(Maps.newHashMap(), Lists::newArrayList);
    for (DataFile dataFile : dataFiles) {
      file2Positions.put(dataFile.path().toString(), 0L);
    }
    StructLike partitionData = partitionData(table.schema(), table.spec(), getOpTime());
    DeleteFile result = writePosDeleteFile(table, file2Positions, partitionData);
    RowDelta rowDelta = table.newRowDelta();
    rowDelta.addDeletes(result);
    rowDelta.commit();
    return Collections.singletonList(result);
  }

  private static List<Record> records(int start, int length, Schema tableSchema) {
    List<Record> records = Lists.newArrayList();
    for (int i = start; i < start + length; i++) {
      records.add(newGenericRecord(tableSchema, i, "name", getOpTime()));
    }
    return records;
  }

  private static LocalDateTime getOpTime() {
    return LocalDateTime.of(2022, 1, 1, 12, 0, 0);
  }
}