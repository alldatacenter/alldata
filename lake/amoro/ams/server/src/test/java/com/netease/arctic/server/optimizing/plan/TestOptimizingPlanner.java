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
import com.netease.arctic.server.optimizing.OptimizingType;
import com.netease.arctic.server.optimizing.scan.TableFileScanHelper;
import org.apache.iceberg.DataFile;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(Parameterized.class)
public class TestOptimizingPlanner extends TestOptimizingEvaluator {
  public TestOptimizingPlanner(CatalogTestHelper catalogTestHelper,
                               TableTestHelper tableTestHelper) {
    super(catalogTestHelper, tableTestHelper);
  }

  @Parameterized.Parameters(name = "{0}, {1}")
  public static Object[][] parameters() {
    return new Object[][] {
        {new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
            new BasicTableTestHelper(true, true)},
        {new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
            new BasicTableTestHelper(true, false)},
        {new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
            new BasicTableTestHelper(false, true)},
        {new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
            new BasicTableTestHelper(false, false)}};
  }

  @Test
  @Override
  public void testFragmentFiles() {
    super.testFragmentFiles();
    OptimizingPlanner optimizingEvaluator = buildOptimizingEvaluator();
    Assert.assertTrue(optimizingEvaluator.isNecessary());
    List<TaskDescriptor> taskDescriptors = optimizingEvaluator.planTasks();
    Assert.assertEquals(1, taskDescriptors.size());
    List<DataFile> dataFiles =
        scanFiles().stream().map(TableFileScanHelper.FileScanResult::file).collect(Collectors.toList());
    assertTask(taskDescriptors.get(0), dataFiles, Collections.emptyList(), Collections.emptyList(),
        Collections.emptyList());

    openFullOptimizing();
    optimizingEvaluator = buildOptimizingEvaluator();
    Assert.assertTrue(optimizingEvaluator.isNecessary());
    Assert.assertEquals(OptimizingType.FULL, optimizingEvaluator.getOptimizingType());
    taskDescriptors = optimizingEvaluator.planTasks();
    Assert.assertEquals(1, taskDescriptors.size());
    assertTask(taskDescriptors.get(0), dataFiles, Collections.emptyList(), Collections.emptyList(),
        Collections.emptyList());
  }

  @Override
  protected OptimizingPlanner buildOptimizingEvaluator() {
    return new OptimizingPlanner(getTableRuntime(), getArcticTable(), 1);
  }
}
