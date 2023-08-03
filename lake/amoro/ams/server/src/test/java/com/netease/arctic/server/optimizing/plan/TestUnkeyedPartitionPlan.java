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

package com.netease.arctic.server.optimizing.plan;

import com.netease.arctic.BasicTableTestHelper;
import com.netease.arctic.TableTestHelper;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.catalog.BasicCatalogTestHelper;
import com.netease.arctic.catalog.CatalogTestHelper;
import com.netease.arctic.server.optimizing.scan.TableFileScanHelper;
import com.netease.arctic.server.optimizing.scan.UnkeyedTableFileScanHelper;
import com.netease.arctic.server.utils.IcebergTableUtil;
import com.netease.arctic.table.UnkeyedTable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class TestUnkeyedPartitionPlan extends MixedTablePlanTestBase {

  public TestUnkeyedPartitionPlan(CatalogTestHelper catalogTestHelper,
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
  public void testFragmentFiles() {
    testFragmentFilesBase();
  }

  @Test
  public void testSegmentFiles() {
    testSegmentFilesBase();
  }

  @Test
  public void testWithDeleteFiles() {
    testWithDeleteFilesBase();
  }

  @Test
  public void testOnlyOneFragmentFiles() {
    testOnlyOneFragmentFileBase();
  }

  @Override
  protected AbstractPartitionPlan getPartitionPlan() {
    return new MixedIcebergPartitionPlan(getTableRuntime(), getArcticTable(), getPartition(),
        System.currentTimeMillis());
  }

  @Override
  protected TableFileScanHelper getTableFileScanHelper() {
    long baseSnapshotId = IcebergTableUtil.getSnapshotId(getArcticTable(), true);
    return new UnkeyedTableFileScanHelper(getArcticTable(), baseSnapshotId);
  }

  @Override
  protected UnkeyedTable getArcticTable() {
    return super.getArcticTable().asUnkeyedTable();
  }
}
