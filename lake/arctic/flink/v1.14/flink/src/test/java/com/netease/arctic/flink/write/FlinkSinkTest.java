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

package com.netease.arctic.flink.write;

import com.netease.arctic.flink.FlinkTestBase;
import com.netease.arctic.flink.table.ArcticTableLoader;
import com.netease.arctic.flink.util.DataUtil;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.data.RowData;
import org.apache.iceberg.data.Record;
import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class FlinkSinkTest extends FlinkTestBase {

  @Test
  public void testKeyedSink() throws Exception {
    final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

    env.enableCheckpointing(2000, CheckpointingMode.EXACTLY_ONCE);
    env.getCheckpointConfig()
        .enableExternalizedCheckpoints(
            CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);

    List<Object[]> data = new LinkedList<>();
    data.add(new Object[]{1000004, "a", LocalDateTime.parse("2022-06-17T10:10:11.0")});
    data.add(new Object[]{1000015, "b", LocalDateTime.parse("2022-06-17T10:08:11.0")});
    data.add(new Object[]{1000011, "c", LocalDateTime.parse("2022-06-18T10:10:11.0")});
    data.add(new Object[]{1000014, "d", LocalDateTime.parse("2022-06-17T10:11:11.0")});
    data.add(new Object[]{1000021, "d", LocalDateTime.parse("2022-06-17T16:10:11.0")});
    data.add(new Object[]{1000015, "e", LocalDateTime.parse("2022-06-17T10:10:11.0")});

    DataStream<RowData> input = env.fromElements(data.stream().map(DataUtil::toRowData).toArray(RowData[]::new));

    FlinkSink
        .forRowData(input)
        .table(testKeyedTable)
        .tableLoader(ArcticTableLoader.of(PK_TABLE_ID, catalogBuilder))
        .flinkSchema(FLINK_SCHEMA)
        .build();

    env.execute();

    testKeyedTable.changeTable().refresh();
    List<Record> actual = readKeyedTable(testKeyedTable);

    Set<Record> expected = toRecords(DataUtil.toRowSet(data));
    Assert.assertEquals(expected, new HashSet<>(actual));
  }

  @Test
  public void testUnkeyedSink() throws Exception {
    final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

    env.enableCheckpointing(2000, CheckpointingMode.EXACTLY_ONCE);
    env.getCheckpointConfig()
        .enableExternalizedCheckpoints(
            CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);

    List<Object[]> data = new LinkedList<>();
    data.add(new Object[]{1000004, "a", LocalDateTime.parse("2022-06-17T10:10:11.0")});
    data.add(new Object[]{1000015, "b", LocalDateTime.parse("2022-06-17T10:08:11.0")});
    data.add(new Object[]{1000011, "c", LocalDateTime.parse("2022-06-18T10:10:11.0")});
    data.add(new Object[]{1000014, "d", LocalDateTime.parse("2022-06-17T10:11:11.0")});
    data.add(new Object[]{1000021, "d", LocalDateTime.parse("2022-06-17T16:10:11.0")});
    data.add(new Object[]{1000015, "e", LocalDateTime.parse("2022-06-17T10:10:11.0")});

    DataStream<RowData> input = env.fromElements(data.stream().map(DataUtil::toRowData).toArray(RowData[]::new));

    FlinkSink
        .forRowData(input)
        .table(testTable)
        .tableLoader(ArcticTableLoader.of(TABLE_ID, catalogBuilder))
        .flinkSchema(FLINK_SCHEMA)
        .build();

    env.execute();
    testTable.refresh();
    Set<Record> actual = DataUtil.read(testTable);

    Set<Record> expected = toRecords(DataUtil.toRowSet(data));
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testUnkeyedOverwrite() throws Exception {
    final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

    env.enableCheckpointing(2000, CheckpointingMode.EXACTLY_ONCE);
    env.getCheckpointConfig()
        .enableExternalizedCheckpoints(
            CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);

    List<Object[]> data = new LinkedList<>();
    data.add(new Object[]{1000004, "a", LocalDateTime.parse("2022-06-17T10:10:11.0")});
    data.add(new Object[]{1000015, "b", LocalDateTime.parse("2022-06-17T10:10:11.0")});
    data.add(new Object[]{1000011, "c", LocalDateTime.parse("2022-06-18T10:10:11.0")});
    data.add(new Object[]{1000014, "d", LocalDateTime.parse("2022-06-18T10:10:11.0")});
    data.add(new Object[]{1000021, "d", LocalDateTime.parse("2022-06-18T10:10:11.0")});
    data.add(new Object[]{1000015, "e", LocalDateTime.parse("2022-06-17T10:10:11.0")});

    DataStream<RowData> input = env.fromElements(data.stream().map(DataUtil::toRowData).toArray(RowData[]::new));

    FlinkSink
        .forRowData(input)
        .table(testTable)
        .tableLoader(ArcticTableLoader.of(TABLE_ID, catalogBuilder))
        .flinkSchema(FLINK_SCHEMA)
        .build();
    env.execute();

    data.clear();
    data.add(new Object[]{12, "d", LocalDateTime.parse("2022-06-17T10:10:11.0")});
    data.add(new Object[]{11, "a", LocalDateTime.parse("2022-06-17T10:10:11.0")});
    data.add(new Object[]{15, "c", LocalDateTime.parse("2022-06-18T10:10:11.0")});
    data.add(new Object[]{21, "k", LocalDateTime.parse("2022-06-17T10:10:11.0")});
    data.add(new Object[]{91, "l", LocalDateTime.parse("2022-06-18T10:10:11.0")});
    data.add(new Object[]{74, "m", LocalDateTime.parse("2022-06-17T10:10:11.0")});

    DataStream<RowData> overwrite = env.fromElements(data.stream().map(DataUtil::toRowData).toArray(RowData[]::new));

    FlinkSink
        .forRowData(overwrite)
        .table(testTable)
        .tableLoader(ArcticTableLoader.of(TABLE_ID, catalogBuilder))
        .overwrite(true)
        .flinkSchema(FLINK_SCHEMA)
        .build();

    env.execute();
    testTable.refresh();
    Set<Record> actual = DataUtil.read(testTable);

    Set<Record> expected = toRecords(DataUtil.toRowSet(data));
    Assert.assertEquals(expected, actual);
  }
}
