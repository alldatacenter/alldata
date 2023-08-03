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

package com.netease.arctic.trace;

import com.google.common.collect.Lists;
import com.netease.arctic.BasicTableTestHelper;
import com.netease.arctic.DataFileTestHelpers;
import com.netease.arctic.ams.api.CommitMetaProducer;
import com.netease.arctic.ams.api.Constants;
import com.netease.arctic.ams.api.DataFile;
import com.netease.arctic.ams.api.TableChange;
import com.netease.arctic.ams.api.TableCommitMeta;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.catalog.BasicCatalogTestHelper;
import com.netease.arctic.catalog.TableTestBase;
import com.netease.arctic.data.DataFileType;
import com.netease.arctic.io.writer.GenericTaskWriters;
import com.netease.arctic.io.writer.SortedPosDeleteWriter;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.UnkeyedTable;
import org.apache.iceberg.DataOperations;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.DeleteFiles;
import org.apache.iceberg.OverwriteFiles;
import org.apache.iceberg.RowDelta;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.Transaction;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.apache.iceberg.relocated.com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Table trace is disabled for version 0.5.0
 */
@Ignore
@RunWith(Parameterized.class)
public class TestTableTracer extends TableTestBase {

  private final boolean onBaseTable;

  private UnkeyedTable operationTable;

  public TestTableTracer(
      boolean keyedTable,
      boolean onBaseTable,
      boolean partitionedTable) {
    super(new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
        new BasicTableTestHelper(keyedTable, partitionedTable));
    this.onBaseTable = onBaseTable;
  }

  @Parameterized.Parameters(name = "keyedTable = {0}, onBaseTable = {1}, partitionedTable = {2}")
  public static Object[][] parameters() {
    return new Object[][] {{true, true, true}, {true, true, false}, {true, false, true}, {true, false, false},
                           {false, true, true}, {false, true, false}};
  }

  private UnkeyedTable getOperationTable() {
    if (operationTable == null) {
      ArcticTable arcticTable = getArcticTable();
      if (isKeyedTable()) {
        if (onBaseTable) {
          operationTable = arcticTable.asKeyedTable().baseTable();
        } else {
          operationTable = arcticTable.asKeyedTable().changeTable();
        }
      } else {
        if (onBaseTable) {
          operationTable = arcticTable.asUnkeyedTable();
        } else {
          throw new IllegalArgumentException("Unkeyed table do not have change store");
        }
      }
    }
    return operationTable;
  }

  private org.apache.iceberg.DataFile getDataFile(int number) {
    if (isPartitionedTable()) {
      return DataFileTestHelpers.getFile(number, "op_time_day=2022-08-30");
    } else {
      return DataFileTestHelpers.getFile(number);
    }
  }

  @Before
  public void clearCommitMeta() {
    getAmsHandler().getTableCommitMetas().remove(getOperationTable().id().buildTableIdentifier());
  }

  @Test
  public void testTraceAppendFiles() {
    UnkeyedTable operationTable = getOperationTable();
    operationTable.newAppend()
        .appendFile(getDataFile(1))
        .appendFile(getDataFile(2))
        .commit();

    List<TableCommitMeta> tableCommitMetas = getAmsHandler().getTableCommitMetas().get(
        operationTable.id().buildTableIdentifier());
    Assert.assertEquals(0, tableCommitMetas.size());
    TableCommitMeta commitMeta = tableCommitMetas.get(0);
    validateCommitMeta(commitMeta, DataOperations.APPEND, new org.apache.iceberg.DataFile[] {
        getDataFile(1), getDataFile(2)}, new org.apache.iceberg.DataFile[] {});
  }

  @Test
  public void testTraceAppendFilesInTx() {
    UnkeyedTable operationTable = getOperationTable();
    Transaction transaction = operationTable.newTransaction();
    transaction.newAppend()
        .appendFile(getDataFile(1))
        .appendFile(getDataFile(2))
        .commit();

    Assert.assertFalse(getAmsHandler().getTableCommitMetas().containsKey(operationTable.id().buildTableIdentifier()));

    transaction.commitTransaction();

    List<TableCommitMeta> tableCommitMetas =
        getAmsHandler().getTableCommitMetas().get(operationTable.id().buildTableIdentifier());
    Assert.assertEquals(1, tableCommitMetas.size());
    TableCommitMeta commitMeta = tableCommitMetas.get(0);
    validateCommitMeta(commitMeta, DataOperations.APPEND,
        new org.apache.iceberg.DataFile[] {getDataFile(1), getDataFile(2)},
        new org.apache.iceberg.DataFile[] {});
  }

  @Test
  public void testTraceAppendFilesByOptimize() {
    UnkeyedTable operationTable = getOperationTable();
    operationTable.newAppend()
        .appendFile(getDataFile(1))
        .appendFile(getDataFile(2))
        .set(SnapshotSummary.SNAPSHOT_PRODUCER, CommitMetaProducer.OPTIMIZE.name())
        .commit();

    List<TableCommitMeta> tableCommitMetas = getAmsHandler().getTableCommitMetas()
        .get(operationTable.id().buildTableIdentifier());
    Assert.assertEquals(1, tableCommitMetas.size());
    TableCommitMeta commitMeta = tableCommitMetas.get(0);
    Assert.assertSame(commitMeta.getCommitMetaProducer(), CommitMetaProducer.OPTIMIZE);
    validateCommitMeta(commitMeta, DataOperations.APPEND,
        new org.apache.iceberg.DataFile[] {getDataFile(1), getDataFile(2)},
        new org.apache.iceberg.DataFile[] {});
  }

  @Test
  public void testTraceFastAppend() {
    UnkeyedTable operationTable = getOperationTable();
    operationTable.newFastAppend()
        .appendFile(getDataFile(1))
        .appendFile(getDataFile(2))
        .commit();

    List<TableCommitMeta> tableCommitMetas = getAmsHandler().getTableCommitMetas()
        .get(operationTable.id().buildTableIdentifier());
    Assert.assertEquals(1, tableCommitMetas.size());
    TableCommitMeta commitMeta = tableCommitMetas.get(0);
    validateCommitMeta(commitMeta, DataOperations.APPEND,
        new org.apache.iceberg.DataFile[] {getDataFile(1), getDataFile(2)},
        new org.apache.iceberg.DataFile[] {});
  }

  @Test
  public void testTraceAppendNoneFiles() {
    UnkeyedTable operationTable = getOperationTable();
    operationTable.newAppend().commit();
    List<TableCommitMeta> appendTableCommitMetas =
        getAmsHandler().getTableCommitMetas().get(operationTable.id().buildTableIdentifier());
    Assert.assertEquals(1, appendTableCommitMetas.size());
    Assert.assertNotNull(appendTableCommitMetas.get(0).getChanges());

    Transaction overwriteTransaction = operationTable.newTransaction();
    overwriteTransaction.newOverwrite()
        .commit();
    overwriteTransaction.commitTransaction();
    List<TableCommitMeta> overwriteTableCommitMetas =
        getAmsHandler().getTableCommitMetas().get(operationTable.id().buildTableIdentifier());
    Assert.assertEquals(2, overwriteTableCommitMetas.size());
    Assert.assertEquals(1, overwriteTableCommitMetas.get(1).getChanges().size());

    Transaction rewriteTransaction = operationTable.newTransaction();
    rewriteTransaction.newRewrite()
        .commit();
    rewriteTransaction.commitTransaction();
    List<TableCommitMeta> rewriteTableCommitMetas =
        getAmsHandler().getTableCommitMetas().get(operationTable.id().buildTableIdentifier());
    Assert.assertEquals(3, rewriteTableCommitMetas.size());
    Assert.assertEquals(1, rewriteTableCommitMetas.get(2).getChanges().size());

    getArcticTable().updateSchema().commit();
    List<TableCommitMeta> updateSchemaCommitMetas =
        getAmsHandler().getTableCommitMetas().get(operationTable.id().buildTableIdentifier());
    Assert.assertEquals(3, updateSchemaCommitMetas.size());

    getArcticTable().updateProperties().commit();
    List<TableCommitMeta> updatePropertiesCommitMetas =
        getAmsHandler().getTableCommitMetas().get(operationTable.id().buildTableIdentifier());
    Assert.assertEquals(4, updatePropertiesCommitMetas.size());
    Assert.assertNull(rewriteTableCommitMetas.get(3).getChanges());
  }

  @Test
  public void testTraceFastAppendInTx() {
    UnkeyedTable operationTable = getOperationTable();
    Transaction transaction = operationTable.newTransaction();
    transaction.newFastAppend()
        .appendFile(getDataFile(1))
        .appendFile(getDataFile(2))
        .commit();

    Assert.assertFalse(getAmsHandler().getTableCommitMetas().containsKey(operationTable.id().buildTableIdentifier()));

    transaction.commitTransaction();
    List<TableCommitMeta> tableCommitMetas = getAmsHandler().getTableCommitMetas()
        .get(operationTable.id().buildTableIdentifier());
    Assert.assertEquals(1, tableCommitMetas.size());
    TableCommitMeta commitMeta = tableCommitMetas.get(0);
    validateCommitMeta(commitMeta, DataOperations.APPEND,
        new org.apache.iceberg.DataFile[] {getDataFile(1), getDataFile(2)},
        new org.apache.iceberg.DataFile[] {});
  }

  @Test
  public void testTraceFastAppendByOptimize() {
    UnkeyedTable operationTable = getOperationTable();
    operationTable.newFastAppend()
        .appendFile(getDataFile(1))
        .appendFile(getDataFile(2))
        .set(SnapshotSummary.SNAPSHOT_PRODUCER, CommitMetaProducer.OPTIMIZE.name())
        .commit();

    List<TableCommitMeta> tableCommitMetas = getAmsHandler().getTableCommitMetas()
        .get(operationTable.id().buildTableIdentifier());
    Assert.assertEquals(1, tableCommitMetas.size());
    TableCommitMeta commitMeta = tableCommitMetas.get(0);
    Assert.assertSame(commitMeta.getCommitMetaProducer(), CommitMetaProducer.OPTIMIZE);
    validateCommitMeta(commitMeta, DataOperations.APPEND,
        new org.apache.iceberg.DataFile[] {getDataFile(1), getDataFile(2)},
        new org.apache.iceberg.DataFile[] {});
  }

  @Test
  public void testTraceOverwrite() {
    UnkeyedTable operationTable = getOperationTable();
    operationTable.newFastAppend()
        .appendFile(getDataFile(1))
        .appendFile(getDataFile(2))
        .commit();

    operationTable.newOverwrite()
        .deleteFile(getDataFile(1))
        .deleteFile(getDataFile(2))
        .addFile(getDataFile(3))
        .commit();

    List<TableCommitMeta> tableCommitMetas = getAmsHandler().getTableCommitMetas()
        .get(operationTable.id().buildTableIdentifier());
    Assert.assertEquals(2, tableCommitMetas.size());
    TableCommitMeta commitMeta = tableCommitMetas.get(1);
    validateCommitMeta(commitMeta, DataOperations.OVERWRITE, new org.apache.iceberg.DataFile[] {getDataFile(3)},
        new org.apache.iceberg.DataFile[] {getDataFile(1), getDataFile(2)});
  }

  @Test
  public void testTraceOverwriteInTx() {
    UnkeyedTable operationTable = getOperationTable();
    operationTable.newFastAppend()
        .appendFile(getDataFile(1))
        .appendFile(getDataFile(2))
        .commit();

    Transaction transaction = operationTable.newTransaction();
    transaction.newOverwrite()
        .deleteFile(getDataFile(1))
        .deleteFile(getDataFile(2))
        .addFile(getDataFile(3))
        .commit();

    Assert.assertEquals(1, getAmsHandler().getTableCommitMetas()
        .get(operationTable.id().buildTableIdentifier()).size());

    transaction.commitTransaction();
    List<TableCommitMeta> tableCommitMetas = getAmsHandler().getTableCommitMetas()
        .get(operationTable.id().buildTableIdentifier());
    Assert.assertEquals(2, tableCommitMetas.size());
    TableCommitMeta commitMeta = tableCommitMetas.get(1);
    validateCommitMeta(commitMeta, DataOperations.OVERWRITE, new org.apache.iceberg.DataFile[] {getDataFile(3)},
        new org.apache.iceberg.DataFile[] {getDataFile(1), getDataFile(2)});
  }

  @Test
  public void testTraceOverwriteByOptimize() {
    UnkeyedTable operationTable = getOperationTable();
    operationTable.newFastAppend()
        .appendFile(getDataFile(1))
        .appendFile(getDataFile(2))
        .commit();

    operationTable.newOverwrite()
        .deleteFile(getDataFile(1))
        .deleteFile(getDataFile(2))
        .addFile(getDataFile(3))
        .set(SnapshotSummary.SNAPSHOT_PRODUCER, CommitMetaProducer.OPTIMIZE.name())
        .commit();

    List<TableCommitMeta> tableCommitMetas = getAmsHandler().getTableCommitMetas()
        .get(operationTable.id().buildTableIdentifier());
    Assert.assertEquals(2, tableCommitMetas.size());
    TableCommitMeta commitMeta = tableCommitMetas.get(1);
    Assert.assertSame(commitMeta.getCommitMetaProducer(), CommitMetaProducer.OPTIMIZE);
    validateCommitMeta(commitMeta, DataOperations.OVERWRITE, new org.apache.iceberg.DataFile[] {getDataFile(3)},
        new org.apache.iceberg.DataFile[] {getDataFile(1), getDataFile(2)});
  }

  @Test
  public void testTraceRewrite() {
    UnkeyedTable operationTable = getOperationTable();
    operationTable.newFastAppend()
        .appendFile(getDataFile(1))
        .appendFile(getDataFile(2))
        .commit();

    operationTable.newRewrite()
        .rewriteFiles(
            Sets.newHashSet(getDataFile(1), getDataFile(2)),
            Sets.newHashSet(getDataFile(3)))
        .commit();

    List<TableCommitMeta> tableCommitMetas = getAmsHandler().getTableCommitMetas()
        .get(operationTable.id().buildTableIdentifier());
    Assert.assertEquals(2, tableCommitMetas.size());
    TableCommitMeta commitMeta = tableCommitMetas.get(1);
    validateCommitMeta(commitMeta, DataOperations.REPLACE, new org.apache.iceberg.DataFile[] {getDataFile(3)},
        new org.apache.iceberg.DataFile[] {getDataFile(1), getDataFile(2)});
  }

  @Test
  public void testTraceRewriteInTx() {
    UnkeyedTable operationTable = getOperationTable();
    operationTable.newFastAppend()
        .appendFile(getDataFile(1))
        .appendFile(getDataFile(2))
        .commit();

    Transaction transaction = operationTable.newTransaction();
    transaction.newRewrite()
        .rewriteFiles(
            Sets.newHashSet(getDataFile(1), getDataFile(2)),
            Sets.newHashSet(getDataFile(3)))
        .commit();

    Assert.assertEquals(1, getAmsHandler().getTableCommitMetas()
        .get(operationTable.id().buildTableIdentifier()).size());

    transaction.commitTransaction();
    List<TableCommitMeta> tableCommitMetas = getAmsHandler().getTableCommitMetas()
        .get(operationTable.id().buildTableIdentifier());
    Assert.assertEquals(2, tableCommitMetas.size());
    TableCommitMeta commitMeta = tableCommitMetas.get(1);
    validateCommitMeta(commitMeta, DataOperations.REPLACE, new org.apache.iceberg.DataFile[] {getDataFile(3)},
        new org.apache.iceberg.DataFile[] {getDataFile(1), getDataFile(2)});
  }

  @Test
  public void testTraceRewriteByOptimize() {
    UnkeyedTable operationTable = getOperationTable();
    operationTable.newFastAppend()
        .appendFile(getDataFile(1))
        .appendFile(getDataFile(2))
        .commit();

    operationTable.newRewrite()
        .rewriteFiles(
            Sets.newHashSet(getDataFile(1), getDataFile(2)),
            Sets.newHashSet(getDataFile(3)))
        .set(SnapshotSummary.SNAPSHOT_PRODUCER, CommitMetaProducer.OPTIMIZE.name())
        .commit();

    List<TableCommitMeta> tableCommitMetas = getAmsHandler().getTableCommitMetas()
        .get(operationTable.id().buildTableIdentifier());
    Assert.assertEquals(2, tableCommitMetas.size());
    TableCommitMeta commitMeta = tableCommitMetas.get(1);
    Assert.assertSame(commitMeta.getCommitMetaProducer(), CommitMetaProducer.OPTIMIZE);
    validateCommitMeta(commitMeta, DataOperations.REPLACE, new org.apache.iceberg.DataFile[] {getDataFile(3)},
        new org.apache.iceberg.DataFile[] {getDataFile(1), getDataFile(2)});
  }

  @Test
  public void testMultipleOperationInTx() {
    UnkeyedTable operationTable = getOperationTable();
    Transaction transaction = operationTable.newTransaction();
    transaction.newAppend()
        .appendFile(getDataFile(1))
        .appendFile(getDataFile(2))
        .commit();

    transaction.newOverwrite()
        .deleteFile(getDataFile(1))
        .addFile(getDataFile(3))
        .commit();

    Assert.assertFalse(getAmsHandler().getTableCommitMetas().containsKey(operationTable.id().buildTableIdentifier()));

    transaction.commitTransaction();

    List<Snapshot> snapshots = Lists.newArrayList(operationTable.snapshots());
    Assert.assertEquals(2, snapshots.size());
    List<TableCommitMeta> tableCommitMetas = getAmsHandler().getTableCommitMetas()
        .get(operationTable.id().buildTableIdentifier());
    Assert.assertEquals(1, tableCommitMetas.size());
    TableCommitMeta commitMeta = tableCommitMetas.get(0);
    Assert.assertEquals(2, commitMeta.getChanges().size());
    validateTableChange(snapshots.get(0), commitMeta.getChanges().get(0),
        new org.apache.iceberg.DataFile[] {getDataFile(1), getDataFile(2)},
        new org.apache.iceberg.DataFile[] {});
    validateTableChange(snapshots.get(1), commitMeta.getChanges().get(1),
        new org.apache.iceberg.DataFile[] {getDataFile(3)},
        new org.apache.iceberg.DataFile[] {getDataFile(1)});
  }

  @Test
  public void testMultipleOperationInTxByOptimize() {
    UnkeyedTable operationTable = getOperationTable();
    Transaction transaction = operationTable.newTransaction();
    transaction.newAppend()
        .appendFile(getDataFile(1))
        .appendFile(getDataFile(2))
        .set(SnapshotSummary.SNAPSHOT_PRODUCER, CommitMetaProducer.OPTIMIZE.name())
        .commit();

    transaction.newOverwrite()
        .deleteFile(getDataFile(1))
        .addFile(getDataFile(3))
        .set(SnapshotSummary.SNAPSHOT_PRODUCER, CommitMetaProducer.OPTIMIZE.name())
        .commit();

    Assert.assertFalse(getAmsHandler().getTableCommitMetas().containsKey(operationTable.id().buildTableIdentifier()));

    transaction.commitTransaction();

    List<Snapshot> snapshots = Lists.newArrayList(operationTable.snapshots());
    Assert.assertEquals(2, snapshots.size());
    List<TableCommitMeta> tableCommitMetas = getAmsHandler().getTableCommitMetas()
        .get(operationTable.id().buildTableIdentifier());
    Assert.assertEquals(1, tableCommitMetas.size());
    TableCommitMeta commitMeta = tableCommitMetas.get(0);
    Assert.assertSame(commitMeta.getCommitMetaProducer(), CommitMetaProducer.OPTIMIZE);
    Assert.assertEquals(2, commitMeta.getChanges().size());
    validateTableChange(snapshots.get(0), commitMeta.getChanges().get(0),
        new org.apache.iceberg.DataFile[] {getDataFile(1), getDataFile(2)},
        new org.apache.iceberg.DataFile[] {});
    validateTableChange(snapshots.get(1), commitMeta.getChanges().get(1),
        new org.apache.iceberg.DataFile[] {getDataFile(3)},
        new org.apache.iceberg.DataFile[] {getDataFile(1)});
  }

  @Test
  public void testTracedReplacePartitions() {
    Assume.assumeTrue(isPartitionedTable());
    UnkeyedTable operationTable = getOperationTable();
    operationTable.newFastAppend()
        .appendFile(DataFileTestHelpers.getFile(1, "op_time_day=2022-01-01"))
        .appendFile(DataFileTestHelpers.getFile(2, "op_time_day=2022-01-01"))
        .appendFile(DataFileTestHelpers.getFile(3, "op_time_day=2022-01-02"))
        .commit();

    operationTable.newReplacePartitions()
        .addFile(DataFileTestHelpers.getFile(4, "op_time_day=2022-01-02"))
        .commit();

    List<TableCommitMeta> tableCommitMetas = getAmsHandler().getTableCommitMetas()
        .get(operationTable.id().buildTableIdentifier());
    Assert.assertEquals(2, tableCommitMetas.size());
    TableCommitMeta commitMeta = tableCommitMetas.get(1);
    validateCommitMeta(commitMeta, DataOperations.OVERWRITE,
        new org.apache.iceberg.DataFile[] {DataFileTestHelpers.getFile(4, "op_time_day=2022-01-02")},
        new org.apache.iceberg.DataFile[] {DataFileTestHelpers.getFile(3, "op_time_day=2022-01-02")});
  }

  @Test
  public void testTraceRemovePosDeleteInternal() throws Exception {
    Assume.assumeTrue(isKeyedTable() && onBaseTable);
    getArcticTable().asKeyedTable().baseTable().newAppend().appendFile(getDataFile(1)).commit();

    SortedPosDeleteWriter<Record> writer = GenericTaskWriters.builderFor(getArcticTable().asKeyedTable())
        .withTransactionId(1L).buildBasePosDeleteWriter(2, 1, getDataFile(1).partition());
    writer.delete(getDataFile(1).path(), 1);
    writer.delete(getDataFile(1).path(), 3);
    writer.delete(getDataFile(1).path(), 5);
    List<DeleteFile> result = writer.complete();
    RowDelta rowDelta = getArcticTable().asKeyedTable().baseTable().newRowDelta();
    result.forEach(rowDelta::addDeletes);
    rowDelta.commit();

    getArcticTable().asKeyedTable().baseTable().newAppend().appendFile(getDataFile(3)).commit();
    OverwriteFiles overwriteFiles = getArcticTable().asKeyedTable().baseTable().newOverwrite();
    overwriteFiles.deleteFile(getDataFile(1));
    overwriteFiles.addFile(getDataFile(2));
    overwriteFiles.commit();

    List<TableCommitMeta> tableCommitMetas = getAmsHandler().getTableCommitMetas()
        .get(getArcticTable().id().buildTableIdentifier());
    Assert.assertEquals(4, tableCommitMetas.size());
    TableCommitMeta commitMeta = tableCommitMetas.get(tableCommitMetas.size() - 1);
    Assert.assertEquals(1, commitMeta.getChanges().size());
    TableChange tableChange = commitMeta.getChanges().get(0);
    Assert.assertEquals(2, tableChange.deleteFiles.size());
  }

  @Test
  public void testTraceRemovePosDeleteInternalInTransaction() throws Exception {
    Assume.assumeTrue(isKeyedTable() && onBaseTable);
    getArcticTable().asKeyedTable().baseTable().newAppend().appendFile(getDataFile(1)).commit();

    SortedPosDeleteWriter<Record> writer = GenericTaskWriters.builderFor(getArcticTable().asKeyedTable())
        .withTransactionId(1L).buildBasePosDeleteWriter(2, 1, getDataFile(1).partition());
    writer.delete(getDataFile(1).path(), 1);
    writer.delete(getDataFile(1).path(), 3);
    writer.delete(getDataFile(1).path(), 5);
    List<DeleteFile> result = writer.complete();
    RowDelta rowDelta = getArcticTable().asKeyedTable().baseTable().newRowDelta();
    result.forEach(rowDelta::addDeletes);
    rowDelta.commit();

    getArcticTable().asKeyedTable().baseTable().newAppend().appendFile(getDataFile(3)).commit();
    Transaction transaction = getArcticTable().asKeyedTable().baseTable().newTransaction();
    OverwriteFiles overwriteFiles = transaction.newOverwrite();
    overwriteFiles.deleteFile(getDataFile(1));
    overwriteFiles.addFile(getDataFile(2));
    overwriteFiles.commit();
    DeleteFiles deleteFiles = transaction.newDelete();
    deleteFiles.deleteFile(getDataFile(3));
    deleteFiles.commit();
    transaction.commitTransaction();

    List<TableCommitMeta> tableCommitMetas =
        getAmsHandler().getTableCommitMetas().get(getArcticTable().id().buildTableIdentifier());
    Assert.assertEquals(4, tableCommitMetas.size());
    TableCommitMeta commitMeta = tableCommitMetas.get(tableCommitMetas.size() - 1);
    Assert.assertEquals(2, commitMeta.getChanges().size());
    TableChange tableChange = commitMeta.getChanges().get(0);
    Assert.assertEquals(2, tableChange.deleteFiles.size());
  }

  private String getExpectedInnerTable() {
    if (onBaseTable) {
      return Constants.INNER_TABLE_BASE;
    } else {
      return Constants.INNER_TABLE_CHANGE;
    }
  }

  private void validateTableChange(
      Snapshot snapshot, TableChange tableChange,
      org.apache.iceberg.DataFile[] addFiles,
      org.apache.iceberg.DataFile[] deleteFiles) {
    Assert.assertEquals(getExpectedInnerTable(), tableChange.getInnerTable());
    Assert.assertEquals(snapshot.snapshotId(), tableChange.getSnapshotId());
    Assert.assertEquals(snapshot.parentId() == null ? -1 :
        getOperationTable().currentSnapshot().parentId(), tableChange.getParentSnapshotId());

    validateDataFile(tableChange.getAddFiles(), addFiles);
    validateDataFile(tableChange.getDeleteFiles(), deleteFiles);
  }

  private void validateCommitMeta(
      TableCommitMeta commitMeta, String operation,
      org.apache.iceberg.DataFile[] addFiles,
      org.apache.iceberg.DataFile[] deleteFiles) {

    Assert.assertEquals(getArcticTable().id().buildTableIdentifier(), commitMeta.getTableIdentifier());
    Assert.assertEquals(operation, commitMeta.getAction());
    Assert.assertEquals(1, commitMeta.getChanges().size());

    TableChange tableChange = commitMeta.getChanges().get(0);
    Assert.assertEquals(getExpectedInnerTable(), tableChange.getInnerTable());
    Assert.assertEquals(getOperationTable().currentSnapshot().snapshotId(), tableChange.getSnapshotId());
    Assert.assertEquals(getOperationTable().currentSnapshot().parentId() == null ? -1 :
        getOperationTable().currentSnapshot().parentId(), tableChange.getParentSnapshotId());

    validateDataFile(tableChange.getAddFiles(), addFiles);
    validateDataFile(tableChange.getDeleteFiles(), deleteFiles);
  }

  private void validateDataFile(List<DataFile> dataFiles, org.apache.iceberg.DataFile... icebergFiles) {
    Assert.assertEquals(icebergFiles.length, dataFiles.size());
    Map<String, org.apache.iceberg.DataFile> icebergFilesMap = Maps.newHashMap();
    Arrays.stream(icebergFiles).forEach(f -> icebergFilesMap.put(f.path().toString(), f));
    for (DataFile validateFile : dataFiles) {
      org.apache.iceberg.DataFile icebergFile = icebergFilesMap.get(validateFile.getPath());
      Assert.assertEquals(icebergFile.path(), validateFile.getPath());
      Assert.assertEquals(icebergFile.fileSizeInBytes(), validateFile.getFileSize());
      Assert.assertEquals(icebergFile.recordCount(), validateFile.getRecordCount());
      Assert.assertEquals(onBaseTable ? DataFileType.BASE_FILE.name() : DataFileType.INSERT_FILE.name(),
          validateFile.getFileType());
      Assert.assertEquals(0, validateFile.getIndex());
      Assert.assertEquals(0, validateFile.getMask());
      if (isPartitionedTable()) {
        Assert.assertEquals(getArcticTable().spec().specId(), validateFile.getSpecId());
        Assert.assertEquals(1, validateFile.getPartitionSize());
        Assert.assertEquals(
            getArcticTable().spec().fields().get(0).name(),
            validateFile.getPartition().get(0).getName());
        Assert.assertEquals(
            getArcticTable().spec().partitionToPath(icebergFile.partition()),
            getArcticTable().spec().fields().get(0).name() + "=" + validateFile.getPartition().get(0).getValue());
      }
    }
  }
}
