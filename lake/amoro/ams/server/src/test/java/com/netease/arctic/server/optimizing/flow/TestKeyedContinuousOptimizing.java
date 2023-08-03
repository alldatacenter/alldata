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

package com.netease.arctic.server.optimizing.flow;

import com.netease.arctic.BasicTableTestHelper;
import com.netease.arctic.TableTestHelper;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.catalog.BasicCatalogTestHelper;
import com.netease.arctic.catalog.CatalogTestHelper;
import com.netease.arctic.catalog.TableTestBase;
import com.netease.arctic.hive.TestHMS;
import com.netease.arctic.hive.catalog.HiveCatalogTestHelper;
import com.netease.arctic.hive.catalog.HiveTableTestHelper;
import com.netease.arctic.server.optimizing.flow.checker.DataConcurrencyChecker;
import com.netease.arctic.server.optimizing.flow.checker.FullOptimizingMove2HiveChecker;
import com.netease.arctic.server.optimizing.flow.checker.FullOptimizingWrite2HiveChecker;
import com.netease.arctic.server.optimizing.flow.checker.MajorOptimizingChecker;
import com.netease.arctic.server.optimizing.flow.checker.MinorOptimizingCheck;
import com.netease.arctic.server.optimizing.flow.checker.OptimizingCountChecker;
import com.netease.arctic.server.optimizing.flow.view.KeyedTableDataView;
import com.netease.arctic.table.ArcticTable;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.List;

import static com.netease.arctic.table.TableProperties.SELF_OPTIMIZING_FULL_REWRITE_ALL_FILES;
import static com.netease.arctic.table.TableProperties.SELF_OPTIMIZING_FULL_TRIGGER_INTERVAL;

@RunWith(Parameterized.class)
public class TestKeyedContinuousOptimizing extends TableTestBase {

  @ClassRule
  public static TestHMS TEST_HMS = new TestHMS();

  public TestKeyedContinuousOptimizing(CatalogTestHelper catalogTestHelper, TableTestHelper tableTestHelper) {
    super(catalogTestHelper, tableTestHelper);
  }

  @Parameterized.Parameters(name = "{1}.{2}")
  public static Object[] parameters() {
    return new Object[][] {
        {
            new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
            new BasicTableTestHelper(true, true)
        },
        {
            new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
            new BasicTableTestHelper(true, false)
        },
        {
            new BasicCatalogTestHelper(TableFormat.ICEBERG),
            new BasicTableTestHelper(true, false)
        },
        {
            new BasicCatalogTestHelper(TableFormat.ICEBERG),
            new BasicTableTestHelper(true, false)
        },
        {
            new HiveCatalogTestHelper(TableFormat.MIXED_HIVE, TEST_HMS.getHiveConf()),
            new HiveTableTestHelper(true, true)
        },
        {
            new HiveCatalogTestHelper(TableFormat.MIXED_HIVE, TEST_HMS.getHiveConf()),
            new BasicTableTestHelper(true, false)
        }
    };
  }

  @Test
  public void run() throws Exception {
    ArcticTable table = getArcticTable();

    int partitionCount = 2;
    int primaryUpperBound = 30000;

    long writeTargetFileSize = 1024 * 12;
    long selfTargetFileSize = table.format() == TableFormat.ICEBERG ? 1024 * 384 : 1024 * 128;
    int minorTriggerCount = table.format() == TableFormat.ICEBERG ? 3 : 4;
    int availableCore = 10;

    int cycle = 5;
    int recordCountOnceWrite = 2500;

    //close full optimize
    table.updateProperties().set(SELF_OPTIMIZING_FULL_TRIGGER_INTERVAL, "-1").commit();
    //Need move file to hive scene
    table.updateProperties().set(SELF_OPTIMIZING_FULL_REWRITE_ALL_FILES, "false").commit();

    KeyedTableDataView view = new KeyedTableDataView(table, tableTestHelper().primaryKeySpec().getPkSchema(),
        partitionCount, primaryUpperBound, writeTargetFileSize, null);

    //init checker
    DataConcurrencyChecker dataConcurrencyChecker = new DataConcurrencyChecker(view);
    OptimizingCountChecker optimizingCountChecker = new OptimizingCountChecker(0);
    FullOptimizingWrite2HiveChecker fullOptimizingWrite2HiveChecker = new FullOptimizingWrite2HiveChecker(view);
    FullOptimizingMove2HiveChecker fullOptimizingMove2HiveChecker = new FullOptimizingMove2HiveChecker(view);
    MinorOptimizingCheck minorOptimizingCheck = new MinorOptimizingCheck();
    MajorOptimizingChecker majorOptimizingChecker = new MajorOptimizingChecker();

    CompleteOptimizingFlow.Builder builder = CompleteOptimizingFlow
        .builder(table, availableCore)
        .setTargetSize(selfTargetFileSize)
        .setFragmentRatio(null)
        .setDuplicateRatio(null)
        .setMinorTriggerFileCount(minorTriggerCount)
        .addChecker(dataConcurrencyChecker)
        .addChecker(optimizingCountChecker)
        .addChecker(minorOptimizingCheck)
        .addChecker(majorOptimizingChecker);

    if (table.format() == TableFormat.MIXED_HIVE) {
      builder
          .addChecker(fullOptimizingWrite2HiveChecker)
          .addChecker(fullOptimizingMove2HiveChecker);
    }

    CompleteOptimizingFlow optimizingFlow = builder.build();

    //full optimizing need move file to hive from change
    view.append(recordCountOnceWrite);
    mustFullCycle(table, optimizingFlow::optimize);

    view.append(recordCountOnceWrite);
    optimizingFlow.optimize();

    //full optimizing need move file to hive from change and base
    view.append(recordCountOnceWrite);
    mustFullCycle(table, optimizingFlow::optimize);

    while (cycle-- > 0) {
      view.onlyDelete(recordCountOnceWrite);
      optimizingFlow.optimize();

      view.cdc(recordCountOnceWrite);
      optimizingFlow.optimize();

      view.upsert(recordCountOnceWrite);
      if (cycle % 2 == 0) {
        mustFullCycle(table, optimizingFlow::optimize);
      } else {
        optimizingFlow.optimize();
      }
    }

    List<CompleteOptimizingFlow.Checker> checkers = optimizingFlow.unTriggerChecker();
    if (checkers.size() != 0) {
      throw new IllegalStateException("Some checkers are not triggered:" + checkers);
    }
  }

  private static void mustFullCycle(ArcticTable table, RunnableWithException runnable) throws Exception {
    table.updateProperties().set(SELF_OPTIMIZING_FULL_TRIGGER_INTERVAL, "1").commit();
    runnable.run();
    table.updateProperties().set(SELF_OPTIMIZING_FULL_TRIGGER_INTERVAL, "-1").commit();
  }

  public interface RunnableWithException {
    void run() throws Exception;
  }
}
