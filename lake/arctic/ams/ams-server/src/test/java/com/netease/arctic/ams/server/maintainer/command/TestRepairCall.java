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

package com.netease.arctic.ams.server.maintainer.command;

import com.netease.arctic.TableTestHelpers;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.FileScanTask;
import org.apache.iceberg.Table;
import org.apache.iceberg.io.CloseableIterator;
import org.junit.Assert;
import org.junit.Test;

public class TestRepairCall extends CallCommandTestBase {

  @Test
  public void testRepairFileLoseThroughSyncMetadata() {
    Context context = new Context();
    String removeFile = removeFile();
    getArcticTable().io().deleteFile(removeFile);
    Assert.assertTrue(analyze(context).contains(removeFile));

    repair(context, RepairWay.SYNC_METADATA, null);

    Assert.assertFalse(fileExists(removeFile, getArcticTable().asKeyedTable().changeTable()));
  }

  @Test
  public void testRepairFileLoseThroughRollback() {
    Context context = new Context();
    String removeFile = removeFile();
    Assert.assertTrue(analyze(context).contains(removeFile));

    TableAnalyzeResult tableAnalyzeResult = context.getTableAvailableResult(TableTestHelpers.TEST_TABLE_ID);

    repair(context, RepairWay.ROLLBACK, tableAnalyzeResult.getRollbackList().get(0).snapshotId());

    Assert.assertFalse(fileExists(removeFile, getArcticTable().asKeyedTable().changeTable()));
  }

  @Test
  public void testRepairManifestLoseThroughSyncMetadata() {
    Context context = new Context();
    String removeManifest = removeManifest();
    Assert.assertTrue(analyze(context).contains(removeManifest));

    repair(context, RepairWay.SYNC_METADATA, null);

    Assert.assertFalse(manifestExists(removeManifest, getArcticTable().asKeyedTable().changeTable()));
  }

  @Test
  public void testRepairManifestLoseThroughRollback() {
    Context context = new Context();
    String removeManifest = removeManifest();
    Assert.assertTrue(analyze(context).contains(removeManifest));

    TableAnalyzeResult tableAnalyzeResult = context.getTableAvailableResult(TableTestHelpers.TEST_TABLE_ID);
    repair(context, RepairWay.ROLLBACK, tableAnalyzeResult.getRollbackList().get(0).snapshotId());

    Assert.assertFalse(manifestExists(removeManifest, getArcticTable().asKeyedTable().changeTable()));
  }

  @Test
  public void testRepairManifestListLoseThroughRollback() {
    Context context = new Context();
    String removeManifestList = removeManifestList();
    Assert.assertTrue(analyze(context).contains(removeManifestList));

    TableAnalyzeResult tableAnalyzeResult = context.getTableAvailableResult(TableTestHelpers.TEST_TABLE_ID);
    repair(context, RepairWay.ROLLBACK, tableAnalyzeResult.getRollbackList().get(0).snapshotId());
    getArcticTable().refresh();
    Assert.assertFalse(getArcticTable().asKeyedTable().changeTable().currentSnapshot().manifestListLocation().equalsIgnoreCase(removeManifestList));
  }

  @Test
  public void testRepairMetadataLoseThroughRollback() {
    Context context = new Context();
    int version = removeMetadata();
    Assert.assertTrue(analyze(context).contains(String.valueOf(version)));

    TableAnalyzeResult tableAnalyzeResult = context.getTableAvailableResult(TableTestHelpers.TEST_TABLE_ID);
    repair(context, RepairWay.ROLLBACK_OR_DROP_TABLE, null);
    getCatalog().loadTable(TableTestHelpers.TEST_TABLE_ID);
  }

  private String analyze(Context context) {
    return callFactory.generateAnalyzeCall(TableTestHelpers.TEST_TABLE_ID.toString()).call(context);
  }

  private String repair(Context context, RepairWay repairWay, Long option) {
    return callFactory.generateRepairCall(TableTestHelpers.TEST_TABLE_ID.toString(), repairWay, option).call(context);
  }

  private boolean fileExists(String path, Table table) {
    table.refresh();
    CloseableIterator<FileScanTask> iterator = table.newScan().planFiles().iterator();
    while (iterator.hasNext()) {
      FileScanTask next = iterator.next();
      if (next.file().path().toString().equals(path)) {
        return true;
      }
      for (DeleteFile deleteFile: next.deletes()) {
        if (deleteFile.path().toString().equals(path)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean manifestExists(String path, Table table) {
    table.refresh();
    return table.currentSnapshot().allManifests()
        .stream().filter(s -> s.path().equals(path)).count() != 0;
  }
}
