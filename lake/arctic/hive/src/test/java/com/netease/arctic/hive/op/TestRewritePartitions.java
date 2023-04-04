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

package com.netease.arctic.hive.op;

import com.netease.arctic.hive.HiveTableTestBase;
import com.netease.arctic.hive.MockDataFileBuilder;
import com.netease.arctic.hive.exceptions.CannotAlterHiveLocationException;
import com.netease.arctic.op.RewritePartitions;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.UnkeyedTable;
import com.netease.arctic.utils.TableFileUtils;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.ReplacePartitions;
import org.apache.iceberg.Transaction;
import org.apache.iceberg.UpdateProperties;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.apache.thrift.TException;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TestRewritePartitions extends HiveTableTestBase {

  @Test
  public void testUnKeyedTableRewritePartitions() throws TException {
    UnkeyedTable table = testHiveTable;
    Map<String, String> partitionAndLocations = Maps.newHashMap();

    List<Map.Entry<String, String>> overwriteFiles = Lists.newArrayList(
        Maps.immutableEntry("name=aaa", "/test_path/partition1/data-a1.parquet"),
        Maps.immutableEntry("name=aaa", "/test_path/partition1/data-a2.parquet"),
        Maps.immutableEntry("name=bbb", "/test_path/partition2/data-a2.parquet")
    );
    MockDataFileBuilder dataFileBuilder = new MockDataFileBuilder(table, hms.getClient());
    List<DataFile> initDataFiles = dataFileBuilder.buildList(overwriteFiles);

    ReplacePartitions replacePartitions = table.newReplacePartitions();
    initDataFiles.forEach(replacePartitions::addFile);
    replacePartitions.commit();

    applyRewritePartitions(partitionAndLocations, overwriteFiles);
    //assert hive partition equal with expect.
    assertHivePartitionLocations(partitionAndLocations, table);

    // =================== overwrite table ========================
    overwriteFiles = Lists.newArrayList(
        Maps.immutableEntry("name=aaa", "/test_path/partition3/data-a3.parquet"),
        Maps.immutableEntry("name=ccc", "/test_path/partition4/data-c.parquet")
    );

    List<DataFile> overwriteDataFiles = dataFileBuilder.buildList(overwriteFiles);
    replacePartitions = table.newReplacePartitions();
    overwriteDataFiles.forEach(replacePartitions::addFile);
    replacePartitions.commit();

    applyRewritePartitions(partitionAndLocations, overwriteFiles);
    //assert hive partition equal with expect.
    assertHivePartitionLocations(partitionAndLocations, table);
  }

  @Test
  public void testKeyedTableRewritePartitions() throws Exception {
    KeyedTable table = testKeyedHiveTable;
    // ====================== init table partition by overwrite =====================
    Map<String, String> partitionAndLocations = Maps.newHashMap();

    List<Map.Entry<String, String>> overwriteFiles = Lists.newArrayList(
        Maps.immutableEntry("name=aaa", "/test_path/partition1/data-a1.parquet"),
        Maps.immutableEntry("name=aaa", "/test_path/partition1/data-a2.parquet"),
        Maps.immutableEntry("name=bbb", "/test_path/partition2/data-a2.parquet")
    );
    MockDataFileBuilder dataFileBuilder = new MockDataFileBuilder(table, hms.getClient());
    List<DataFile> initDataFiles = dataFileBuilder.buildList(overwriteFiles);

    RewritePartitions rewritePartitions = table.newRewritePartitions();
    rewritePartitions.updateOptimizedSequenceDynamically(table.beginTransaction(""));
    initDataFiles.forEach(rewritePartitions::addDataFile);
    rewritePartitions.commit();

    applyRewritePartitions(partitionAndLocations, overwriteFiles);
    //assert hive partition equal with expect.
    assertHivePartitionLocations(partitionAndLocations, table);

    // =================== overwrite table ========================
    overwriteFiles = Lists.newArrayList(
        Maps.immutableEntry("name=aaa", "/test_path/partition3/data-a3.parquet"),
        Maps.immutableEntry("name=ccc", "/test_path/partition4/data-c.parquet")
    );

    List<DataFile> overwriteDataFiles = dataFileBuilder.buildList(overwriteFiles);
    rewritePartitions = table.newRewritePartitions();
    rewritePartitions.updateOptimizedSequenceDynamically(table.beginTransaction(""));
    overwriteDataFiles.forEach(rewritePartitions::addDataFile);
    rewritePartitions.commit();

    applyRewritePartitions(partitionAndLocations, overwriteFiles);
    //assert hive partition equal with expect.
    assertHivePartitionLocations(partitionAndLocations, table);
  }

  @Test
  public void testRewritePartitionInTransaction() throws Exception {
    UnkeyedTable table = testHiveTable;
    Map<String, String> partitionAndLocations = Maps.newHashMap();

    List<Map.Entry<String, String>> overwriteFiles = Lists.newArrayList(
        Maps.immutableEntry("name=aaa", "/test_path/partition1/data-a1.parquet"),
        Maps.immutableEntry("name=aaa", "/test_path/partition1/data-a2.parquet"),
        Maps.immutableEntry("name=bbb", "/test_path/partition2/data-a2.parquet")
    );
    MockDataFileBuilder dataFileBuilder = new MockDataFileBuilder(table, hms.getClient());
    List<DataFile> initDataFiles = dataFileBuilder.buildList(overwriteFiles);

    Transaction tx = table.newTransaction();
    ReplacePartitions replacePartitions = tx.newReplacePartitions();
    initDataFiles.forEach(replacePartitions::addFile);
    replacePartitions.commit();

    UpdateProperties updateProperties = tx.updateProperties();
    updateProperties.set("test-rewrite-partition", "test-rewrite-partition-value");
    updateProperties.commit();

    table.refresh();

    assertHivePartitionLocations(partitionAndLocations, table);
    Assert.assertFalse(table.properties().containsKey("test-rewrite-partition"));

    tx.commitTransaction();
    applyRewritePartitions(partitionAndLocations, overwriteFiles);

    assertHivePartitionLocations(partitionAndLocations, table);
    Assert.assertTrue(table.properties().containsKey("test-rewrite-partition"));
    Assert.assertEquals("test-rewrite-partition-value", table.properties().get("test-rewrite-partition"));
  }

  /**
   * failed then add file of same partition with different location
   */
  @Test
  public void testExceptionAddFileWithDifferentLocation() throws TException {
    UnkeyedTable table = testHiveTable;

    List<Map.Entry<String, String>> overwriteFiles = Lists.newArrayList(
        Maps.immutableEntry("name=aaa", "/test_path/partition1/data-a1.parquet"),
        Maps.immutableEntry("name=aaa", "/test_path/partition2/data-a2.parquet"),
        Maps.immutableEntry("name=bbb", "/test_path/partition2/data-a2.parquet")
    );
    MockDataFileBuilder dataFileBuilder = new MockDataFileBuilder(table, hms.getClient());
    List<DataFile> initDataFiles = dataFileBuilder.buildList(overwriteFiles);

    ReplacePartitions replacePartitions = table.newReplacePartitions();
    initDataFiles.forEach(replacePartitions::addFile);

    Assert.assertThrows(CannotAlterHiveLocationException.class, replacePartitions::commit);
  }
  private void applyRewritePartitions(
      Map<String, String> partitionLocations,
      List<Map.Entry<String, String>> overwriteFiles) {
    overwriteFiles.forEach(kv -> {
      String partLocation = TableFileUtils.getFileDir(kv.getValue());
      partitionLocations.put(kv.getKey(), partLocation);
    });
  }
}
