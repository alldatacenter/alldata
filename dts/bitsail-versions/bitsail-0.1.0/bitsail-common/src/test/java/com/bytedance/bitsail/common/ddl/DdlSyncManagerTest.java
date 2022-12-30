/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.common.ddl;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.ddl.external.FakeSinkEngineConnector;
import com.bytedance.bitsail.common.ddl.external.FakeSourceEngineConnector;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.common.option.ReaderOptions;
import com.bytedance.bitsail.common.option.WriterOptions;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * Created 2022/8/23
 */
public class DdlSyncManagerTest {
  private BitSailConfiguration commonConfiguration;
  private BitSailConfiguration readerConfiguration;
  private BitSailConfiguration writerConfiguration;

  private FakeSourceEngineConnector sourceEngineConnector;
  private FakeSinkEngineConnector sinkEngineConnector;

  private DdlSyncManager syncManager;

  @Before
  public void before() {
    commonConfiguration = BitSailConfiguration.newDefault();
    readerConfiguration = BitSailConfiguration.newDefault();
    writerConfiguration = BitSailConfiguration.newDefault();
  }

  @Test
  public void testColumnAlignment() throws Exception {
    List<ColumnInfo> readerColumns = Lists.newArrayList();
    List<ColumnInfo> writerColumns = Lists.newArrayList();

    readerColumns.add(ColumnInfo.builder()
        .name("name")
        .type("string")
        .build());

    readerColumns.add(ColumnInfo.builder()
        .name("id")
        .type("int")
        .build());

    writerColumns.add(ColumnInfo.builder()
        .name("name")
        .type("string")
        .build());

    commonConfiguration.set(CommonOptions.COLUMN_ALIGN_STRATEGY, SchemaColumnAligner.ColumnAlignmentStrategy.intersect.name());
    commonConfiguration.set(CommonOptions.SYNC_DDL, true);
    sourceEngineConnector = new FakeSourceEngineConnector(readerColumns, commonConfiguration, readerConfiguration);
    sinkEngineConnector = new FakeSinkEngineConnector(writerColumns, commonConfiguration, readerConfiguration);

    syncManager = new DdlSyncManager(sourceEngineConnector, sinkEngineConnector, commonConfiguration, readerConfiguration, writerConfiguration);
    syncManager.doColumnAlignment(false);

    List<ColumnInfo> finalReaderColumns = readerConfiguration.get(ReaderOptions.BaseReaderOptions.COLUMNS);
    List<ColumnInfo> finalWriterColumns = writerConfiguration.get(WriterOptions.BaseWriterOptions.COLUMNS);

    Assert.assertEquals(finalReaderColumns.size(), finalWriterColumns.size());
  }

  @Test
  public void testColumnAlignmentSourceOnly() throws Exception {
    List<ColumnInfo> readerColumns = Lists.newArrayList();
    List<ColumnInfo> writerColumns = Lists.newArrayList();

    readerColumns.add(ColumnInfo.builder()
        .name("name")
        .type("string")
        .build());

    readerColumns.add(ColumnInfo.builder()
        .name("id")
        .type("int")
        .build());

    writerColumns.add(ColumnInfo.builder()
        .name("name")
        .type("string")
        .build());

    commonConfiguration.set(CommonOptions.COLUMN_ALIGN_STRATEGY, SchemaColumnAligner.ColumnAlignmentStrategy.source_only.name());
    commonConfiguration.set(CommonOptions.SYNC_DDL, true);

    sourceEngineConnector = new FakeSourceEngineConnector(readerColumns, commonConfiguration, readerConfiguration);
    sinkEngineConnector = new FakeSinkEngineConnector(writerColumns, commonConfiguration, readerConfiguration);

    syncManager = new DdlSyncManager(sourceEngineConnector, sinkEngineConnector, commonConfiguration, readerConfiguration, writerConfiguration);
    syncManager.doColumnAlignment(false);

    List<ColumnInfo> finalReaderColumns = readerConfiguration.get(ReaderOptions.BaseReaderOptions.COLUMNS);
    List<ColumnInfo> finalWriterColumns = writerConfiguration.get(WriterOptions.BaseWriterOptions.COLUMNS);

    Assert.assertEquals(finalReaderColumns.size(), finalWriterColumns.size());
    List<ColumnInfo> addedColumns = sinkEngineConnector.getAddedColumns();
    Assert.assertEquals(addedColumns.size(), 1);
  }

  @Test
  public void testColumnAlignmentUpdate() throws Exception {
    List<ColumnInfo> readerColumns = Lists.newArrayList();
    List<ColumnInfo> writerColumns = Lists.newArrayList();

    readerColumns.add(ColumnInfo.builder()
        .name("name")
        .type("int")
        .build());

    readerColumns.add(ColumnInfo.builder()
        .name("id")
        .type("int")
        .build());

    writerColumns.add(ColumnInfo.builder()
        .name("name")
        .type("string")
        .build());

    commonConfiguration.set(CommonOptions.COLUMN_ALIGN_STRATEGY, SchemaColumnAligner.ColumnAlignmentStrategy.source_only.name());
    commonConfiguration.set(CommonOptions.SYNC_DDL, true);

    sourceEngineConnector = new FakeSourceEngineConnector(readerColumns, commonConfiguration, readerConfiguration);
    sinkEngineConnector = new FakeSinkEngineConnector(writerColumns, commonConfiguration, readerConfiguration);

    syncManager = new DdlSyncManager(sourceEngineConnector, sinkEngineConnector, commonConfiguration, readerConfiguration, writerConfiguration);
    syncManager.doColumnAlignment(false);

    List<ColumnInfo> finalReaderColumns = readerConfiguration.get(ReaderOptions.BaseReaderOptions.COLUMNS);
    List<ColumnInfo> finalWriterColumns = writerConfiguration.get(WriterOptions.BaseWriterOptions.COLUMNS);

    Assert.assertEquals(finalReaderColumns.size(), finalWriterColumns.size());
    List<ColumnInfo> addedColumns = sinkEngineConnector.getAddedColumns();
    Assert.assertEquals(addedColumns.size(), 1);
    Assert.assertEquals(sinkEngineConnector.getUpdatedColumns().size(), 1);
  }
}