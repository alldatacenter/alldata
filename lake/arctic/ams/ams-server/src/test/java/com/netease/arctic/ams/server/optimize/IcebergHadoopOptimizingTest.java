/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *  *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.ams.server.optimize;

import com.netease.arctic.ams.api.OptimizeType;
import com.netease.arctic.ams.server.model.OptimizeHistory;
import com.netease.arctic.table.TableIdentifier;
import com.netease.arctic.table.TableProperties;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.Table;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;

import java.io.IOException;

import static com.netease.arctic.TableTestBase.partitionData;

public class IcebergHadoopOptimizingTest extends AbstractOptimizingTest {

  private final TableIdentifier tb;
  private final Table table;
  private final long startId;

  public IcebergHadoopOptimizingTest(TableIdentifier tb, Table table, long optimizeHistoryStartId) {
    this.tb = tb;
    this.table = table;
    this.startId = optimizeHistoryStartId;
  }

  public void testIcebergTableOptimizing() throws IOException {
    int offset = 1;
    StructLike partitionData = partitionData(table.schema(), table.spec(), quickDateWithZone(3));

    // Step 1: insert 2 data file and Minor Optimize
    insertDataFile(table, Lists.newArrayList(
        newRecord(1, "aaa", quickDateWithZone(3)),
        newRecord(2, "bbb", quickDateWithZone(3)),
        newRecord(3, "aaa", quickDateWithZone(3)),
        newRecord(4, "bbb", quickDateWithZone(3))
    ), partitionData);

    insertDataFile(table, Lists.newArrayList(
        newRecord(5, "ccc", quickDateWithZone(3)),
        newRecord(6, "ddd", quickDateWithZone(3))
    ), partitionData);

    // wait Minor Optimize result
    OptimizeHistory optimizeHistory = waitOptimizeResult(tb, startId + offset++);
    assertOptimizeHistory(optimizeHistory, OptimizeType.Minor, 2, 1);
    assertIds(readRecords(table), 1, 2, 3, 4, 5, 6);

    // Step 2: insert delete file and Minor Optimize
    insertEqDeleteFiles(table, Lists.newArrayList(
        newRecord(1, "aaa", quickDateWithZone(3))
    ), partitionData);

    // wait Minor Optimize result
    optimizeHistory = waitOptimizeResult(tb, startId + offset++);
    assertOptimizeHistory(optimizeHistory, OptimizeType.Minor, 2, 1);
    assertIds(readRecords(table), 2, 3, 4, 5, 6);

    // Step 3: insert 2 delete file and Minor Optimize(big file)
    long dataFileSize = getDataFileSize(table);
    updateProperties(table, TableProperties.SELF_OPTIMIZING_FRAGMENT_RATIO,
        TableProperties.SELF_OPTIMIZING_TARGET_SIZE_DEFAULT / (dataFileSize - 100) + "");

    insertEqDeleteFiles(table, Lists.newArrayList(
        newRecord(2, "aaa", quickDateWithZone(3))
    ), partitionData);

    insertEqDeleteFiles(table, Lists.newArrayList(
        newRecord(3, "aaa", quickDateWithZone(3))
    ), partitionData);

    // wait Minor Optimize result
    optimizeHistory = waitOptimizeResult(tb, startId + offset++);
    assertOptimizeHistory(optimizeHistory, OptimizeType.Minor, 3, 1);
    assertIds(readRecords(table), 4, 5, 6);
    updateProperties(table, TableProperties.SELF_OPTIMIZING_MINOR_TRIGGER_FILE_CNT, "10");

    // Step 4: insert 1 delete and full optimize
    // insertEqDeleteFiles(table, Lists.newArrayList(
    //     newRecord(4, "bbb", quickDateWithZone(3))
    // ));
    rowDelta(table, Lists.newArrayList(
        newRecord(7, "aaa", quickDateWithZone(3)),
        newRecord(8, "aaa", quickDateWithZone(3))
    ), Lists.newArrayList(
        newRecord(4, "aaa", quickDateWithZone(3))
    ), partitionData);
    updateProperties(table, TableProperties.SELF_OPTIMIZING_MAJOR_TRIGGER_DUPLICATE_RATIO, "0");

    // wait FullMajor Optimize result
    optimizeHistory = waitOptimizeResult(tb, startId + offset++);
    assertOptimizeHistory(optimizeHistory, OptimizeType.FullMajor, 4, 1);

    assertIds(readRecords(table), 5, 6, 7, 8);
    assertOptimizeHangUp(tb, startId + offset);
  }

  public void testV1IcebergTableOptimizing() throws IOException {
    int offset = 1;
    StructLike partitionData = partitionData(table.schema(), table.spec(), quickDateWithZone(3));

    // Step 1: insert 2 data file and Minor Optimize
    insertDataFile(table, Lists.newArrayList(
        newRecord(1, "aaa", quickDateWithZone(3)),
        newRecord(2, "bbb", quickDateWithZone(3)),
        newRecord(3, "aaa", quickDateWithZone(3)),
        newRecord(4, "bbb", quickDateWithZone(3))
    ), partitionData);

    insertDataFile(table, Lists.newArrayList(
        newRecord(5, "ccc", quickDateWithZone(3)),
        newRecord(6, "ddd", quickDateWithZone(3))
    ), partitionData);

    // wait Minor Optimize result
    OptimizeHistory optimizeHistory = waitOptimizeResult(tb, startId + offset++);
    assertOptimizeHistory(optimizeHistory, OptimizeType.Minor, 2, 1);
    assertIds(readRecords(table), 1, 2, 3, 4, 5, 6);

    // Step 1: insert 2 data file and Minor Optimize
    insertDataFile(table, Lists.newArrayList(
        newRecord(7, "ccc", quickDateWithZone(3)),
        newRecord(8, "ddd", quickDateWithZone(3))
    ), partitionData);

    // wait Minor Optimize result
    optimizeHistory = waitOptimizeResult(tb, startId + offset++);
    assertOptimizeHistory(optimizeHistory, OptimizeType.Minor, 2, 1);
    assertIds(readRecords(table), 1, 2, 3, 4, 5, 6, 7, 8);
    assertOptimizeHangUp(tb, startId + offset);
  }

  public void testPartitionIcebergTableOptimizing() throws IOException {
    int offset = 1;
    StructLike partitionData = partitionData(table.schema(), table.spec(), quickDateWithZone(3));

    // Step 1: insert 2 data file and Minor Optimize
    insertDataFile(table, Lists.newArrayList(
        newRecord(1, "aaa", quickDateWithZone(3)),
        newRecord(2, "bbb", quickDateWithZone(3)),
        newRecord(3, "aaa", quickDateWithZone(3)),
        newRecord(4, "bbb", quickDateWithZone(3))
    ), partitionData);

    insertDataFile(table, Lists.newArrayList(
        newRecord(5, "ccc", quickDateWithZone(3)),
        newRecord(6, "ddd", quickDateWithZone(3))
    ), partitionData);

    // wait Minor Optimize result
    OptimizeHistory optimizeHistory = waitOptimizeResult(tb, startId + offset++);
    assertOptimizeHistory(optimizeHistory, OptimizeType.Minor, 2, 1);
    assertIds(readRecords(table), 1, 2, 3, 4, 5, 6);

    // Step 2: insert delete file and Minor Optimize
    insertEqDeleteFiles(table, Lists.newArrayList(
        newRecord(1, "aaa", quickDateWithZone(3))
    ), partitionData);

    // wait Minor Optimize result
    optimizeHistory = waitOptimizeResult(tb, startId + offset++);
    assertOptimizeHistory(optimizeHistory, OptimizeType.Minor, 2, 1);
    assertIds(readRecords(table), 2, 3, 4, 5, 6);

    // Step 3: insert 2 delete file and Minor Optimize(big file)
    long dataFileSize = getDataFileSize(table);
    updateProperties(table, TableProperties.SELF_OPTIMIZING_FRAGMENT_RATIO,
        TableProperties.SELF_OPTIMIZING_TARGET_SIZE_DEFAULT / (dataFileSize - 100) + "");

    insertEqDeleteFiles(table, Lists.newArrayList(
        newRecord(2, "aaa", quickDateWithZone(3))
    ), partitionData);

    insertEqDeleteFiles(table, Lists.newArrayList(
        newRecord(3, "aaa", quickDateWithZone(3))
    ), partitionData);

    // wait Minor Optimize result
    optimizeHistory = waitOptimizeResult(tb, startId + offset++);
    assertOptimizeHistory(optimizeHistory, OptimizeType.Minor, 3, 1);
    assertIds(readRecords(table), 4, 5, 6);
    updateProperties(table, TableProperties.SELF_OPTIMIZING_MINOR_TRIGGER_FILE_CNT, "10");

    // Step 4: insert 1 delete and full optimize
    // insertEqDeleteFiles(table, Lists.newArrayList(
    //     newRecord(4, "bbb", quickDateWithZone(3))
    // ));
    rowDelta(table, Lists.newArrayList(
        newRecord(7, "aaa", quickDateWithZone(3)),
        newRecord(8, "aaa", quickDateWithZone(3))
    ), Lists.newArrayList(
        newRecord(4, "aaa", quickDateWithZone(3))
    ), partitionData);
    updateProperties(table, TableProperties.SELF_OPTIMIZING_MAJOR_TRIGGER_DUPLICATE_RATIO, "0");

    // wait FullMajor Optimize result
    optimizeHistory = waitOptimizeResult(tb, startId + offset++);
    assertOptimizeHistory(optimizeHistory, OptimizeType.FullMajor, 4, 1);

    assertIds(readRecords(table), 5, 6, 7, 8);

    assertOptimizeHangUp(tb, startId + offset);
  }

  public void testIcebergTableFullOptimize() throws IOException {
    long offset = 1;
    StructLike partitionData = partitionData(table.schema(), table.spec(), quickDateWithZone(3));

    updateProperties(table, TableProperties.SELF_OPTIMIZING_MINOR_TRIGGER_FILE_CNT, "100");

    insertDataFile(table, Lists.newArrayList(
        newRecord(1, "aaa", quickDateWithZone(3)),
        newRecord(2, "aaa", quickDateWithZone(3)),
        newRecord(3, "aaa", quickDateWithZone(3)),
        newRecord(4, "aaa", quickDateWithZone(3))
    ), partitionData);

    rowDelta(table, Lists.newArrayList(
        newRecord(1, "aaa", quickDateWithZone(3)),
        newRecord(2, "aaa", quickDateWithZone(3)),
        newRecord(3, "aaa", quickDateWithZone(3)),
        newRecord(4, "aaa", quickDateWithZone(3))
    ), Lists.newArrayList(
        newRecord(1, "aaa", quickDateWithZone(3)),
        newRecord(2, "aaa", quickDateWithZone(3)),
        newRecord(3, "aaa", quickDateWithZone(3)),
        newRecord(4, "aaa", quickDateWithZone(3))
    ), partitionData);

    rowDelta(table, Lists.newArrayList(
        newRecord(1, "aaa", quickDateWithZone(3)),
        newRecord(2, "aaa", quickDateWithZone(3)),
        newRecord(3, "aaa", quickDateWithZone(3)),
        newRecord(4, "aaa", quickDateWithZone(3))
    ), Lists.newArrayList(
        newRecord(1, "aaa", quickDateWithZone(3)),
        newRecord(2, "aaa", quickDateWithZone(3)),
        newRecord(3, "aaa", quickDateWithZone(3)),
        newRecord(4, "aaa", quickDateWithZone(3))
    ), partitionData);

    rowDeltaWithPos(table, Lists.newArrayList(
        newRecord(3, "aaa", quickDateWithZone(3)),
        newRecord(4, "aaa", quickDateWithZone(3))
    ), Lists.newArrayList(
        newRecord(1, "aaa", quickDateWithZone(3)),
        newRecord(2, "aaa", quickDateWithZone(3)),
        newRecord(3, "aaa", quickDateWithZone(3)),
        newRecord(4, "aaa", quickDateWithZone(3))
    ), partitionData);

    insertDataFile(table, Lists.newArrayList(
        newRecord(5, "eee", quickDateWithZone(3))
    ), partitionData);

    assertIds(readRecords(table), 4, 5);

    updateProperties(table, TableProperties.SELF_OPTIMIZING_MAJOR_TRIGGER_DUPLICATE_RATIO, "0");

    OptimizeHistory optimizeHistory = waitOptimizeResult(tb, startId + offset++);
    assertOptimizeHistory(optimizeHistory, OptimizeType.FullMajor, 9, 1);

    assertIds(readRecords(table), 4, 5);

    assertOptimizeHangUp(tb, startId + offset);
  }

  public void testPartitionIcebergTablePartialOptimizing() throws IOException {
    int offset = 1;
    updateProperties(table, TableProperties.ENABLE_SELF_OPTIMIZING, "false");

    // Step 1: insert 6 data files for two partitions
    StructLike partitionData1 = partitionData(table.schema(), table.spec(), quickDateWithZone(1));
    insertDataFile(table, Lists.newArrayList(
        newRecord(1, "aaa", quickDateWithZone(1))
    ), partitionData1);
    insertDataFile(table, Lists.newArrayList(
        newRecord(2, "bbb", quickDateWithZone(1))
    ), partitionData1);
    StructLike partitionData2 = partitionData(table.schema(), table.spec(), quickDateWithZone(2));
    insertDataFile(table, Lists.newArrayList(
        newRecord(3, "ccc", quickDateWithZone(2))
    ), partitionData2);
    insertDataFile(table, Lists.newArrayList(
        newRecord(4, "ddd", quickDateWithZone(2))
    ), partitionData2);
    StructLike partitionData3 = partitionData(table.schema(), table.spec(), quickDateWithZone(3));
    insertDataFile(table, Lists.newArrayList(
        newRecord(5, "eee", quickDateWithZone(3))
    ), partitionData3);
    insertDataFile(table, Lists.newArrayList(
        newRecord(6, "fff", quickDateWithZone(3))
    ), partitionData3);

    updateProperties(table, TableProperties.SELF_OPTIMIZING_MINOR_TRIGGER_FILE_CNT, "2");
    updateProperties(table, TableProperties.SELF_OPTIMIZING_MAX_FILE_CNT, "4");
    updateProperties(table, TableProperties.ENABLE_SELF_OPTIMIZING, "true");

    // wait Minor Optimize result
    OptimizeHistory optimizeHistory = waitOptimizeResult(tb, startId + offset++);
    assertOptimizeHistory(optimizeHistory, OptimizeType.Minor, 4, 2);
    optimizeHistory = waitOptimizeResult(tb, startId + offset++);
    assertOptimizeHistory(optimizeHistory, OptimizeType.Minor, 2, 1);
    assertIds(readRecords(table), 1, 2, 3, 4, 5, 6);

    assertOptimizeHangUp(tb, startId + offset);
  }

  private Record newRecord(Object... val) {
    return newRecord(table.schema(), val);
  }
}
