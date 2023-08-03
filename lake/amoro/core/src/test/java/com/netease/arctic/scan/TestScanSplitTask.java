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

import com.netease.arctic.io.DataTestHelpers;
import com.netease.arctic.io.TableDataTestBase;
import com.netease.arctic.io.writer.GenericChangeTaskWriter;
import com.netease.arctic.io.writer.GenericTaskWriters;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.io.WriteResult;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

public class TestScanSplitTask extends TableDataTestBase {
  @Test
  public void testSplitTaskByDeleteRatio() throws IOException {
    writeInsertFileIntoBaseStore();
    writeInsertFileIntoBaseStore();
    {
      CloseableIterable<CombinedScanTask> combinedScanTasks =
          getArcticTable().asKeyedTable().newScan().planTasks();
      for (CombinedScanTask combinedScanTask : combinedScanTasks) {
        Assert.assertEquals(combinedScanTask.tasks().size(), 7);
      }
    }
    {
      CloseableIterable<CombinedScanTask> combinedScanTasks =
          getArcticTable().asKeyedTable().newScan().enableSplitTaskByDeleteRatio(0.04).planTasks();
      for (CombinedScanTask combinedScanTask : combinedScanTasks) {
        // If enableSplitTaskByDeleteRatio is turned on, tasks with delete content below the threshold will be split
        // as much as possible. The above normal plan has 7 tasks, but two of them contain two files each and no
        // delete content. Therefore, after enableSplitTaskByDeleteRatio is turned on, two additional tasks will be
        // split out.
        Assert.assertEquals(combinedScanTask.tasks().size(), 9);
      }
    }
  }

  private void writeInsertFileIntoBaseStore() throws IOException {
    ImmutableList.Builder<Record> builder = ImmutableList.builder();
    builder.add(DataTestHelpers.createRecord(7, "mary", 0, "2022-01-01T12:00:00"));
    builder.add(DataTestHelpers.createRecord(8, "mack", 0, "2022-01-01T12:00:00"));
    ImmutableList<Record> records = builder.build();

    GenericChangeTaskWriter writer = GenericTaskWriters.builderFor(getArcticTable().asKeyedTable())
        .withTransactionId(5L).buildChangeWriter();
    for (Record record : records) {
      writer.write(record);
    }
    WriteResult result = writer.complete();
    AppendFiles baseAppend = getArcticTable().asKeyedTable().baseTable().newAppend();
    Arrays.stream(result.dataFiles()).forEach(baseAppend::appendFile);
    baseAppend.commit();
  }
}