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

package com.netease.arctic.server.optimizing.scan;

import com.netease.arctic.BasicTableTestHelper;
import com.netease.arctic.TableTestHelper;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.catalog.BasicCatalogTestHelper;
import com.netease.arctic.catalog.CatalogTestHelper;
import com.netease.arctic.io.DataTestHelpers;
import com.netease.arctic.server.optimizing.OptimizingTestHelpers;
import com.netease.arctic.server.utils.IcebergTableUtil;
import com.netease.arctic.table.UnkeyedTable;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RunWith(Parameterized.class)
public class TestUnkeyedTableFileScanHelper extends TableFileScanHelperTestBase {
  public TestUnkeyedTableFileScanHelper(CatalogTestHelper catalogTestHelper,
                                        TableTestHelper tableTestHelper) {
    super(catalogTestHelper, tableTestHelper);
  }

  @Parameterized.Parameters(name = "{0}, {1}")
  public static Object[][] parameters() {
    return new Object[][] {
        {new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
            new BasicTableTestHelper(false, true)},
        {new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
            new BasicTableTestHelper(false, false)}};
  }

  @Test
  public void testScanEmpty() {
    List<TableFileScanHelper.FileScanResult> scan = buildFileScanHelper().scan();
    assertScanResult(scan, 0);
  }

  @Test
  public void testScanEmptySnapshot() {
    OptimizingTestHelpers.appendBase(getArcticTable(),
        tableTestHelper().writeBaseStore(getArcticTable(), 0L, Collections.emptyList(), false));

    List<TableFileScanHelper.FileScanResult> scan = buildFileScanHelper().scan();
    assertScanResult(scan, 0);
  }

  @Test
  public void testScan() {
    ArrayList<Record> newRecords = Lists.newArrayList(
        tableTestHelper().generateTestRecord(1, "111", 0, "2022-01-01T12:00:00"),
        tableTestHelper().generateTestRecord(2, "222", 0, "2022-01-01T12:00:00"),
        tableTestHelper().generateTestRecord(3, "333", 0, "2022-01-02T12:00:00"),
        tableTestHelper().generateTestRecord(4, "444", 0, "2022-01-02T12:00:00")
    );
    OptimizingTestHelpers.appendBase(getArcticTable(),
        tableTestHelper().writeBaseStore(getArcticTable(), 0L, newRecords, false));
    OptimizingTestHelpers.appendBase(getArcticTable(),
        tableTestHelper().writeBaseStore(getArcticTable(), 0L, newRecords, false));

    List<TableFileScanHelper.FileScanResult> scan = buildFileScanHelper().scan();

    if (isPartitionedTable()) {
      assertScanResult(scan, 4, null, 0);
    } else {
      assertScanResult(scan, 2, null, 0);
    }

    // test partition filter
    scan = buildFileScanHelper().withPartitionFilter(
        partition -> getPartition().equals(partition)).scan();
    if (isPartitionedTable()) {
      assertScanResult(scan, 2, null, 0);
    } else {
      assertScanResult(scan, 2, null, 0);
    }
  }

  @Test
  public void testScanWithPosDelete() {
    ArrayList<Record> newRecords = Lists.newArrayList(
        tableTestHelper().generateTestRecord(1, "111", 0, "2022-01-01T12:00:00"),
        tableTestHelper().generateTestRecord(2, "222", 0, "2022-01-01T12:00:00"),
        tableTestHelper().generateTestRecord(3, "333", 0, "2022-01-01T12:00:00"),
        tableTestHelper().generateTestRecord(4, "444", 0, "2022-01-01T12:00:00")
    );
    List<DataFile> dataFiles = OptimizingTestHelpers.appendBase(getArcticTable(),
        tableTestHelper().writeBaseStore(getArcticTable(), 0L, newRecords, false));
    List<DeleteFile> posDeleteFiles = Lists.newArrayList();
    for (DataFile dataFile : dataFiles) {
      posDeleteFiles.addAll(
          DataTestHelpers.writeBaseStorePosDelete(getArcticTable(), 0L, dataFile,
              Collections.singletonList(0L)));
    }
    OptimizingTestHelpers.appendBasePosDelete(getArcticTable(), posDeleteFiles);

    List<TableFileScanHelper.FileScanResult> scan = buildFileScanHelper().scan();

    assertScanResult(scan, 1, null, 1);
  }

  @Override
  protected UnkeyedTable getArcticTable() {
    return super.getArcticTable().asUnkeyedTable();
  }

  @Override
  protected TableFileScanHelper buildFileScanHelper() {
    long baseSnapshotId = IcebergTableUtil.getSnapshotId(getArcticTable(), true);
    return new UnkeyedTableFileScanHelper(getArcticTable(), baseSnapshotId);
  }
}
