/*
 * Copyright 2022 ByteDance and/or its affiliates
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.core.writer;

import com.bytedance.bitsail.base.connector.writer.DataWriterDAGBuilder;
import com.bytedance.bitsail.base.execution.ExecutionEnviron;
import com.bytedance.bitsail.base.execution.Mode;
import com.bytedance.bitsail.base.packages.PackageManager;
import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.common.option.WriterOptions;
import com.bytedance.bitsail.flink.core.execution.FlinkExecutionEnviron;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class DataWriterBuilderFactoryTest {
  private BitSailConfiguration dagBuilderConf;
  private BitSailConfiguration legacyPluginConf;

  private BitSailConfiguration unsupportedClassConf;
  private PackageManager packageManager;

  @Before
  public void init() {
    dagBuilderConf = BitSailConfiguration.newDefault();
    dagBuilderConf.set(CommonOptions.ENABLE_DYNAMIC_LOADER, false);
    dagBuilderConf.set(WriterOptions.WRITER_CLASS, MockDataWriterDAGBuilder.class.getName());

    ExecutionEnviron executionEnviron = Mockito.mock(FlinkExecutionEnviron.class);

    packageManager = PackageManager.getInstance(executionEnviron, dagBuilderConf);

    legacyPluginConf = BitSailConfiguration.newDefault();
    legacyPluginConf.set(CommonOptions.ENABLE_DYNAMIC_LOADER, false);
    legacyPluginConf.set(WriterOptions.WRITER_CLASS, MockOutputFormatPlugin.class.getName());

    unsupportedClassConf = BitSailConfiguration.newDefault();
    unsupportedClassConf.set(CommonOptions.ENABLE_DYNAMIC_LOADER, false);
    unsupportedClassConf.set(WriterOptions.WRITER_CLASS, DataWriterBuilderFactoryTest.class.getName());
  }

  @Test
  public void testGetDataReaderDAGBuilder() throws Exception {

    DataWriterDAGBuilder dataWriterDAGBuilder = DataWriterBuilderFactory.getDataWriterDAGBuilder(
        Mode.BATCH, dagBuilderConf, packageManager);
    assertEquals(dataWriterDAGBuilder.getWriterName(), MockDataWriterDAGBuilder.class.getSimpleName());
  }

  @Test
  public void testGetInputFormatPlugin() throws Exception {
    DataWriterDAGBuilder dataWriterDAGBuilder = DataWriterBuilderFactory.getDataWriterDAGBuilder(
        Mode.BATCH, legacyPluginConf, packageManager);
    assertEquals(dataWriterDAGBuilder.getWriterName(), MockOutputFormatPlugin.class.getSimpleName());
  }

  @Test
  public void testGetDataReaderDAGBuilderList() {
    List<DataWriterDAGBuilder> dataWriterDAGBuilderList = DataWriterBuilderFactory.getDataWriterDAGBuilderList(
        Mode.BATCH, ImmutableList.of(dagBuilderConf, legacyPluginConf), packageManager);
    assertEquals(dataWriterDAGBuilderList.size(), 2);
    assertEquals(dataWriterDAGBuilderList.get(0).getWriterName(), MockDataWriterDAGBuilder.class.getSimpleName());
    assertEquals(dataWriterDAGBuilderList.get(1).getWriterName(), MockOutputFormatPlugin.class.getSimpleName());
  }

  @Test(expected = BitSailException.class)
  public void testUnsupportedReaderClass() throws Exception {
    DataWriterBuilderFactory.getDataWriterDAGBuilder(
        Mode.BATCH, unsupportedClassConf, packageManager);
  }

}
