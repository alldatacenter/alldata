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

package com.netease.arctic.flink.read;

import com.netease.arctic.BasicTableTestHelper;
import com.netease.arctic.TableTestHelper;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.catalog.BasicCatalogTestHelper;
import com.netease.arctic.flink.FlinkTestBase;
import com.netease.arctic.flink.table.ArcticTableLoader;
import com.netease.arctic.flink.util.DataUtil;
import com.netease.arctic.table.UnkeyedTable;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.execution.JobClient;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamUtils;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.operators.collect.ClientAndIterator;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.util.CloseableIterator;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.FileFormat;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.Table;
import org.apache.iceberg.io.TaskWriter;
import org.apache.iceberg.io.WriteResult;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.netease.arctic.flink.write.TestArcticFileWriter.TARGET_FILE_SIZE;
import static com.netease.arctic.flink.write.TestArcticFileWriter.createUnkeyedTaskWriter;

public class TestFlinkSource extends FlinkTestBase {

  protected static final FileFormat fileFormat = FileFormat.valueOf("parquet".toUpperCase(Locale.ENGLISH));

  public TestFlinkSource() {
    super(new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
      new BasicTableTestHelper(false, true));
  }

  protected static void commit(WriteResult result, Table table) {
    AppendFiles append = table.newAppend();
    Arrays.stream(result.dataFiles()).forEach(append::appendFile);
    append.commit();
  }

  protected static void write(Collection<Object[]> data, Table table, RowType rowType) throws IOException {
    try (TaskWriter<RowData> taskWriter = createUnkeyedTaskWriter(table, TARGET_FILE_SIZE, fileFormat, rowType)) {
      data.forEach(d -> {
        try {
          taskWriter.write(DataUtil.toRowData(d));
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
      taskWriter.close();

      commit(taskWriter.complete(), table);
    }
  }

  @Test
  public void testUnkeyedTableDataStream() throws Exception {
    Configuration conf = new Configuration();
    final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment(conf);

    env.enableCheckpointing(2000, CheckpointingMode.EXACTLY_ONCE);
    env.setParallelism(1);
    env.getCheckpointConfig()
        .enableExternalizedCheckpoints(
            CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);

    List<Object[]> data = new LinkedList<>();
    LocalDateTime localDateTime = LocalDateTime.parse("2022-06-18T10:10:11.0");
    long timestamp = localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
    data.add(new Object[]{1000004, "a", timestamp, localDateTime});
    data.add(new Object[]{1000015, "b", timestamp, localDateTime});
    data.add(new Object[]{1000011, "c", timestamp, localDateTime});
    data.add(new Object[]{1000014, "d", timestamp, localDateTime});
    data.add(new Object[]{1000021, "d", timestamp, localDateTime});
    data.add(new Object[]{1000015, "e", timestamp, localDateTime});

    Collection<RowData> expectedRecords = DataUtil.toRowData(data);
    write(data, getArcticTable().asUnkeyedTable(), FLINK_ROW_TYPE);

    final CloseableIterator<RowData> resultIterator = com.netease.arctic.flink.table.FlinkSource.forRowData()
        .env(env)
        .project(FLINK_SCHEMA)
        .tableLoader(ArcticTableLoader.of(TableTestHelper.TEST_TABLE_ID, catalogBuilder))
        .flinkConf(conf)
        .properties(new HashMap<String, String>() {{
          put("streaming", "false");
        }})
        .build().executeAndCollect();

    Set<RowData> rowData = new HashSet<>();
    resultIterator.forEachRemaining(o ->
        rowData.add(GenericRowData.of(o.getInt(0), o.getString(1), o.getLong(2), o.getTimestamp(3, 6))));

    Assert.assertEquals(new HashSet<>(expectedRecords), rowData);
  }

  @Test
  public void testUnkeyedStreamingRead() throws Exception {
    Configuration conf = new Configuration();
    final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment(conf);

    env.enableCheckpointing(2000, CheckpointingMode.EXACTLY_ONCE);
    env.setParallelism(1);
    env.getCheckpointConfig()
        .enableExternalizedCheckpoints(
            CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);

    List<Object[]> data = new LinkedList<>();
    LocalDateTime localDateTime = LocalDateTime.parse("2022-06-18T10:10:11.0");
    long timestamp = localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
    data.add(new Object[]{1000004, "a", timestamp, localDateTime});
    data.add(new Object[]{1000015, "b", timestamp, localDateTime});
    data.add(new Object[]{1000011, "c", timestamp, localDateTime});
    data.add(new Object[]{1000014, "d", timestamp, localDateTime});
    data.add(new Object[]{1000021, "d", timestamp, localDateTime});
    data.add(new Object[]{1000015, "e", timestamp, localDateTime});

    Collection<RowData> expectedRecords = DataUtil.toRowData(data);
    write(data, getArcticTable().asUnkeyedTable(), FLINK_ROW_TYPE);

    DataStream<RowData> ds = com.netease.arctic.flink.table.FlinkSource.forRowData()
        .env(env)
        .project(FLINK_SCHEMA)
        .tableLoader(ArcticTableLoader.of(TableTestHelper.TEST_TABLE_ID, catalogBuilder))
        .flinkConf(conf)
        .build();

    ClientAndIterator<RowData> clientAndIterator =
        DataStreamUtils.collectWithClient(ds, this.getClass().getName());

    JobClient jobClient = clientAndIterator.client;
    CloseableIterator<RowData> iterator = clientAndIterator.iterator;

    Set<RowData> rowData = new HashSet<>();
    while (iterator.hasNext()) {
      RowData o = iterator.next();
      rowData.add(GenericRowData.of(o.getInt(0), o.getString(1), o.getLong(2), o.getTimestamp(3, 6)));
      if (rowData.size() == expectedRecords.size()) {
        break;
      }
    }
    jobClient.cancel();

    Assert.assertEquals(new HashSet<>(expectedRecords), rowData);
  }

  @Test
  public void testUnkeyedSnapshotRead() throws Exception {
    Configuration conf = new Configuration();
    UnkeyedTable testTable = getArcticTable().asUnkeyedTable();
    final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment(conf);

    env.enableCheckpointing(2000, CheckpointingMode.EXACTLY_ONCE);
    env.setParallelism(1);
    env.getCheckpointConfig()
        .enableExternalizedCheckpoints(
            CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);

    List<Object[]> s1 = new LinkedList<>();
    LocalDateTime localDateTime1 = LocalDateTime.parse("2022-06-18T10:10:11.0");
    long timestamp1 = localDateTime1.toInstant(ZoneOffset.UTC).toEpochMilli();
    s1.add(new Object[]{1000004, "a", timestamp1, localDateTime1});
    s1.add(new Object[]{1000015, "b", timestamp1, localDateTime1});
    s1.add(new Object[]{1000011, "c", timestamp1, localDateTime1});
    s1.add(new Object[]{1000014, "d", timestamp1, localDateTime1});
    s1.add(new Object[]{1000021, "d", timestamp1, localDateTime1});
    s1.add(new Object[]{1000015, "e", timestamp1, localDateTime1});

    write(s1, testTable, FLINK_ROW_TYPE);

    List<Object[]> s2 = new LinkedList<>();
    LocalDateTime localDateTime2 = LocalDateTime.parse("2022-06-19T10:10:11.0");
    long timestamp2 = localDateTime2.toInstant(ZoneOffset.UTC).toEpochMilli();
    s2.add(new Object[]{12, "ac", timestamp2, localDateTime2});
    s2.add(new Object[]{52, "ad", timestamp2, localDateTime2});
    s2.add(new Object[]{15, "ad", timestamp2, localDateTime2});
    s2.add(new Object[]{26, "ae", timestamp2, localDateTime2});

    Collection<RowData> expectedRecords = DataUtil.toRowData(s2);
    write(s2, testTable, FLINK_ROW_TYPE);

    testTable.refresh();
    Snapshot s = testTable.snapshots().iterator().next();

    DataStream<RowData> ds = com.netease.arctic.flink.table.FlinkSource.forRowData()
        .env(env)
        .project(FLINK_SCHEMA)
        .tableLoader(ArcticTableLoader.of(TableTestHelper.TEST_TABLE_ID, catalogBuilder))
        .flinkConf(conf)
        .properties(new HashMap<String, String>() {{
          put("streaming", "true");
          put("start-snapshot-id", String.valueOf(s.snapshotId()));
        }})
        .build();

    ClientAndIterator<RowData> clientAndIterator =
        DataStreamUtils.collectWithClient(ds, this.getClass().getName());

    JobClient jobClient = clientAndIterator.client;
    CloseableIterator<RowData> iterator = clientAndIterator.iterator;

    Set<RowData> rowData = new HashSet<>();
    while (iterator.hasNext()) {
      RowData o = iterator.next();
      rowData.add(GenericRowData.of(o.getInt(0), o.getString(1), o.getLong(2), o.getTimestamp(3, 6)));
      if (rowData.size() == expectedRecords.size()) {
        break;
      }
    }
    jobClient.cancel();

    Assert.assertEquals(new HashSet<>(expectedRecords), rowData);

    CloseableIterator<RowData> resultIterator = com.netease.arctic.flink.table.FlinkSource.forRowData()
        .env(env)
        .project(FLINK_SCHEMA)
        .tableLoader(ArcticTableLoader.of(TableTestHelper.TEST_TABLE_ID, catalogBuilder))
        .flinkConf(conf)
        .properties(new HashMap<String, String>() {{
          put("streaming", "false");
          put("snapshot-id", String.valueOf(s.snapshotId()));
        }})
        .build().executeAndCollect();

    rowData.clear();
    resultIterator.forEachRemaining(o ->
        rowData.add(GenericRowData.of(o.getInt(0), o.getString(1), o.getLong(2), o.getTimestamp(3, 6))));

    expectedRecords = DataUtil.toRowData(s1);
    Assert.assertEquals(new HashSet<>(expectedRecords), rowData);
  }

}
