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

package com.netease.arctic.server.table.executor;

import com.netease.arctic.TableTestHelper;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.catalog.CatalogTestHelper;
import com.netease.arctic.hive.TestHMS;
import com.netease.arctic.hive.catalog.HiveCatalogTestHelper;
import com.netease.arctic.hive.catalog.HiveTableTestHelper;
import com.netease.arctic.hive.table.SupportHive;
import org.apache.iceberg.io.OutputFile;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;

import static com.netease.arctic.server.table.executor.OrphanFilesCleaningExecutor.DATA_FOLDER_NAME;

@RunWith(Parameterized.class)
public class TestOrphanFileCleanHive extends TestOrphanFileClean {

  @ClassRule
  public static TestHMS TEST_HMS = new TestHMS();

  @Parameterized.Parameters(name = "{0}, {1}")
  public static Object[] parameters() {
    return new Object[][]{
        {new HiveCatalogTestHelper(TableFormat.MIXED_HIVE, TEST_HMS.getHiveConf()),
            new HiveTableTestHelper(true, true)},
        {new HiveCatalogTestHelper(TableFormat.MIXED_HIVE, TEST_HMS.getHiveConf()),
            new HiveTableTestHelper(true, false)},
        {new HiveCatalogTestHelper(TableFormat.MIXED_HIVE, TEST_HMS.getHiveConf()),
            new HiveTableTestHelper(false, true)},
        {new HiveCatalogTestHelper(TableFormat.MIXED_HIVE, TEST_HMS.getHiveConf()),
            new HiveTableTestHelper(false, false)}};
  }

  public TestOrphanFileCleanHive(CatalogTestHelper catalogTestHelper, TableTestHelper tableTestHelper) {
    super(catalogTestHelper, tableTestHelper);
  }

  @Test
  public void hiveLocationOrphanDataFileClean() throws IOException {
    String hiveOrphanFilePath = ((SupportHive) getArcticTable()).hiveLocation() +
        File.separator + DATA_FOLDER_NAME + File.separator + "orphan.parquet";
    OutputFile changeOrphanDataFile = getArcticTable().io().newOutputFile(hiveOrphanFilePath);
    changeOrphanDataFile.createOrOverwrite().close();
    Assert.assertTrue(getArcticTable().io().exists(hiveOrphanFilePath));
    OrphanFilesCleaningExecutor.cleanContentFiles(getArcticTable(), System.currentTimeMillis());
    Assert.assertTrue(getArcticTable().io().exists(hiveOrphanFilePath));
  }


}
