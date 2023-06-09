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

import com.netease.arctic.flink.FlinkTestBase;
import com.netease.arctic.flink.table.ArcticTableLoader;
import com.netease.arctic.flink.util.DataUtil;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.netease.arctic.flink.write.ArcticFileWriterTest.TARGET_FILE_SIZE;
import static com.netease.arctic.flink.write.ArcticFileWriterTest.createUnkeyedTaskWriter;

public class FlinkSourceTest extends FlinkTestBase {

  protected static final FileFormat fileFormat = FileFormat.valueOf("parquet".toUpperCase(Locale.ENGLISH));

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
    data.add(new Object[]{1000004, "a", LocalDateTime.parse("2022-06-18T10:10:11.0")});
    data.add(new Object[]{1000015, "b", LocalDateTime.parse("2022-06-18T10:10:11.0")});
    data.add(new Object[]{1000011, "c", LocalDateTime.parse("2022-06-18T10:10:11.0")});
    data.add(new Object[]{1000014, "d", LocalDateTime.parse("2022-06-18T10:10:11.0")});
    data.add(new Object[]{1000021, "d", LocalDateTime.parse("2022-06-18T10:10:11.0")});
    data.add(new Object[]{1000015, "e", LocalDateTime.parse("2022-06-18T10:10:11.0")});

    Collection<RowData> expectedRecords = DataUtil.toRowData(data);
    write(data, testTable, FLINK_ROW_TYPE);

    final CloseableIterator<RowData> resultIterator = com.netease.arctic.flink.table.FlinkSource.forRowData()
        .env(env)
        .project(FLINK_SCHEMA)
        .tableLoader(ArcticTableLoader.of(TABLE_ID, catalogBuilder))
        .flinkConf(conf)
        .properties(new HashMap<String, String>() {{
          put("streaming", "false");
        }})
        .build().executeAndCollect();

    Set<RowData> rowData = new HashSet<>();
    resultIterator.forEachRemaining(o ->
        rowData.add(GenericRowData.of(o.getInt(0), o.getString(1), o.getTimestamp(2, 6))));

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
    data.add(new Object[]{1000004, "a", LocalDateTime.parse("2022-06-18T10:10:11.0")});
    data.add(new Object[]{1000015, "b", LocalDateTime.parse("2022-06-18T10:10:11.0")});
    data.add(new Object[]{1000011, "c", LocalDateTime.parse("2022-06-18T10:10:11.0")});
    data.add(new Object[]{1000014, "d", LocalDateTime.parse("2022-06-18T10:10:11.0")});
    data.add(new Object[]{1000021, "d", LocalDateTime.parse("2022-06-18T10:10:11.0")});
    data.add(new Object[]{1000015, "e", LocalDateTime.parse("2022-06-18T10:10:11.0")});

    Collection<RowData> expectedRecords = DataUtil.toRowData(data);
    write(data, testTable, FLINK_ROW_TYPE);

    DataStream<RowData> ds = com.netease.arctic.flink.table.FlinkSource.forRowData()
        .env(env)
        .project(FLINK_SCHEMA)
        .tableLoader(ArcticTableLoader.of(TABLE_ID, catalogBuilder))
        .flinkConf(conf)
        .build();

    ClientAndIterator<RowData> clientAndIterator =
        DataStreamUtils.collectWithClient(ds, this.getClass().getName());

    JobClient jobClient = clientAndIterator.client;
    CloseableIterator<RowData> iterator = clientAndIterator.iterator;

    Set<RowData> rowData = new HashSet<>();
    while (iterator.hasNext()) {
      RowData o = iterator.next();
      rowData.add(GenericRowData.of(o.getInt(0), o.getString(1), o.getTimestamp(2, 6)));
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
    final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment(conf);

    env.enableCheckpointing(2000, CheckpointingMode.EXACTLY_ONCE);
    env.setParallelism(1);
    env.getCheckpointConfig()
        .enableExternalizedCheckpoints(
            CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);

    List<Object[]> s1 = new LinkedList<>();
    s1.add(new Object[]{1000004, "a", LocalDateTime.parse("2022-06-18T10:10:11.0")});
    s1.add(new Object[]{1000015, "b", LocalDateTime.parse("2022-06-18T10:10:11.0")});
    s1.add(new Object[]{1000011, "c", LocalDateTime.parse("2022-06-18T10:10:11.0")});
    s1.add(new Object[]{1000014, "d", LocalDateTime.parse("2022-06-18T10:10:11.0")});
    s1.add(new Object[]{1000021, "d", LocalDateTime.parse("2022-06-18T10:10:11.0")});
    s1.add(new Object[]{1000015, "e", LocalDateTime.parse("2022-06-18T10:10:11.0")});

    write(s1, testTable, FLINK_ROW_TYPE);

    List<Object[]> s2 = new LinkedList<>();
    s2.add(new Object[]{12, "ac", LocalDateTime.parse("2022-06-18T10:10:11.0")});
    s2.add(new Object[]{52, "ad", LocalDateTime.parse("2022-06-19T10:10:11.0")});
    s2.add(new Object[]{15, "ad", LocalDateTime.parse("2022-06-19T10:10:11.0")});
    s2.add(new Object[]{26, "ae", LocalDateTime.parse("2022-06-19T10:10:11.0")});

    Collection<RowData> expectedRecords = DataUtil.toRowData(s2);
    write(s2, testTable, FLINK_ROW_TYPE);

    testTable.refresh();
    Snapshot s = testTable.snapshots().iterator().next();

    DataStream<RowData> ds = com.netease.arctic.flink.table.FlinkSource.forRowData()
        .env(env)
        .project(FLINK_SCHEMA)
        .tableLoader(ArcticTableLoader.of(TABLE_ID, catalogBuilder))
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
      rowData.add(GenericRowData.of(o.getInt(0), o.getString(1), o.getTimestamp(2, 6)));
      if (rowData.size() == expectedRecords.size()) {
        break;
      }
    }
    jobClient.cancel();

    Assert.assertEquals(new HashSet<>(expectedRecords), rowData);

    CloseableIterator<RowData> resultIterator = com.netease.arctic.flink.table.FlinkSource.forRowData()
        .env(env)
        .project(FLINK_SCHEMA)
        .tableLoader(ArcticTableLoader.of(TABLE_ID, catalogBuilder))
        .flinkConf(conf)
        .properties(new HashMap<String, String>() {{
          put("streaming", "false");
          put("snapshot-id", String.valueOf(s.snapshotId()));
        }})
        .build().executeAndCollect();

    rowData.clear();
    resultIterator.forEachRemaining(o ->
        rowData.add(GenericRowData.of(o.getInt(0), o.getString(1), o.getTimestamp(2, 6))));

    expectedRecords = DataUtil.toRowData(s1);
    Assert.assertEquals(new HashSet<>(expectedRecords), rowData);
  }

}
