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

package com.netease.arctic.optimizer.operator.executor;

import com.netease.arctic.ams.api.DataFileInfo;
import com.netease.arctic.hive.HiveTableTestBase;
import org.apache.iceberg.Schema;
import org.apache.iceberg.data.GenericRecord;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableList;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public class TestSupportHiveMajorOptimizeBase extends HiveTableTestBase implements TestOptimizeBase {
  protected List<DataFileInfo> baseDataFilesInfo = new ArrayList<>();
  protected List<DataFileInfo> posDeleteFilesInfo = new ArrayList<>();

  @Before
  public void initDataFileInfo() {
    baseDataFilesInfo = new ArrayList<>();
    posDeleteFilesInfo = new ArrayList<>();
  }

  @After
  public void clearDataFileInfo() {
    baseDataFilesInfo.clear();
    posDeleteFilesInfo.clear();
  }

  public List<Record> baseRecords(int start, int length, Schema tableSchema) {
    GenericRecord record = GenericRecord.create(tableSchema);

    ImmutableList.Builder<Record> builder = ImmutableList.builder();
    for (int i = start; i < start + length; i++) {
      builder.add(record.copy(ImmutableMap.of(COLUMN_NAME_ID, i,
          COLUMN_NAME_OP_TIME, LocalDateTime.of(2022, 1, 1, 12, 0, 0),
          COLUMN_NAME_OP_TIME_WITH_ZONE, LocalDateTime.of(2022, 1, i % 2 + 1, 12, 0, 0).atOffset(ZoneOffset.UTC),
          COLUMN_NAME_D, new BigDecimal(i), COLUMN_NAME_NAME, "name")));
    }

    return builder.build();
  }
}
