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

package com.bytedance.bitsail.core.reader;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.flink.core.legacy.connector.InputFormatPlugin;

import org.apache.flink.api.common.io.statistics.BaseStatistics;
import org.apache.flink.core.io.InputSplit;
import org.apache.flink.core.io.InputSplitAssigner;
import org.apache.flink.types.Row;

import java.io.IOException;

public class MockInputFormatPlugin extends InputFormatPlugin {
  @Override
  public Row buildRow(Row reuse, String mandatoryEncoding) throws BitSailException {
    return null;
  }

  @Override
  public boolean isSplitEnd() throws IOException {
    return false;
  }

  @Override
  public InputSplit[] createSplits(int minNumSplits) throws IOException {
    return new InputSplit[0];
  }

  @Override
  public void initPlugin() throws Exception {

  }

  @Override
  public String getType() {
    return MockInputFormatPlugin.class.getSimpleName();
  }

  @Override
  public BaseStatistics getStatistics(BaseStatistics baseStatistics) throws IOException {
    return null;
  }

  @Override
  public InputSplitAssigner getInputSplitAssigner(InputSplit[] inputSplits) {
    return null;
  }

  @Override
  public void open(InputSplit inputSplit) throws IOException {

  }

  @Override
  public Object nextRecord(Object o) throws IOException {
    return null;
  }

  @Override
  public void close() throws IOException {

  }
}
