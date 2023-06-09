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

package com.netease.arctic.flink.read.hybrid.enumerator;

import com.netease.arctic.flink.FlinkTestBase;
import com.netease.arctic.table.KeyedTable;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;
import org.apache.flink.table.data.TimestampData;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.types.RowKind;
import org.apache.iceberg.flink.FlinkSchemaUtil;
import org.apache.iceberg.io.TaskWriter;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ContinuousSplitPlannerImplTest extends FlinkTestBase {
  private static final Logger LOG = LoggerFactory.getLogger(ContinuousSplitPlannerImplTest.class);
  protected static final RowType ROW_TYPE = FlinkSchemaUtil.convert(TABLE_SCHEMA);

  protected static final LocalDateTime ldt =
      LocalDateTime.of(
          LocalDate.of(2022, 1, 1),
          LocalTime.of(0, 0, 0, 0));

  @Before
  public void init() throws IOException {
    //write base
    {
      TaskWriter<RowData> taskWriter = createTaskWriter(true);
      List<RowData> baseData = new ArrayList<RowData>() {{
        add(GenericRowData.ofKind(
            RowKind.INSERT, 1, StringData.fromString("john"), TimestampData.fromLocalDateTime(ldt)));
        add(GenericRowData.ofKind(
            RowKind.INSERT, 2, StringData.fromString("lily"), TimestampData.fromLocalDateTime(ldt)));
        add(GenericRowData.ofKind(
            RowKind.INSERT, 3, StringData.fromString("jake"), TimestampData.fromLocalDateTime(ldt.plusDays(1))));
        add(GenericRowData.ofKind(
            RowKind.INSERT, 4, StringData.fromString("sam"), TimestampData.fromLocalDateTime(ldt.plusDays(1))));
      }};
      for (RowData record : baseData) {
        taskWriter.write(record);
      }
      commit(testKeyedTable, taskWriter.complete(), true);
    }

    //write change insert
    {
      TaskWriter<RowData> taskWriter = createTaskWriter(false);
      List<RowData> insert = new ArrayList<RowData>() {{
        add(GenericRowData.ofKind(
            RowKind.INSERT, 5, StringData.fromString("mary"), TimestampData.fromLocalDateTime(ldt)));
        add(GenericRowData.ofKind(
            RowKind.INSERT, 6, StringData.fromString("mack"), TimestampData.fromLocalDateTime(ldt)));
      }};
      for (RowData record : insert) {
        taskWriter.write(record);
      }
      commit(testKeyedTable, taskWriter.complete(), true);
    }

    //write change delete
    {
      TaskWriter<RowData> taskWriter = createTaskWriter(false);
      List<RowData> update = new ArrayList<RowData>() {{
        add(GenericRowData.ofKind(
            RowKind.DELETE, 5, StringData.fromString("mary"), TimestampData.fromLocalDateTime(ldt)));
        add(GenericRowData.ofKind(
            RowKind.INSERT, 5, StringData.fromString("lind"), TimestampData.fromLocalDateTime(ldt)));
      }};

      for (RowData record : update) {
        taskWriter.write(record);
      }
      commit(testKeyedTable, taskWriter.complete(), false);
    }
  }

  protected TaskWriter<RowData> createTaskWriter(boolean base) {
    return createKeyedTaskWriter(testKeyedTable, ROW_TYPE, base);
  }

  protected TaskWriter<RowData> createTaskWriter(KeyedTable keyedTable, boolean base) {
    return createKeyedTaskWriter(keyedTable, ROW_TYPE, base);
  }
}