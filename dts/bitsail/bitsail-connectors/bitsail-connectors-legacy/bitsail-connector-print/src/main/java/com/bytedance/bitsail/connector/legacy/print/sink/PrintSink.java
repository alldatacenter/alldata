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

package com.bytedance.bitsail.connector.legacy.print.sink;

import com.bytedance.bitsail.flink.core.legacy.connector.OutputFormatPlugin;

import org.apache.flink.api.common.functions.util.PrintSinkOutputWriter;
import org.apache.flink.types.Row;

import java.io.IOException;

public class PrintSink extends OutputFormatPlugin<Row> {
  private PrintSinkOutputWriter<Row> writer;

  @Override
  public void open(int taskNumber, int numTasks) throws IOException {
    super.open(taskNumber, numTasks);
    writer.open(taskNumber, numTasks);

  }

  @Override
  public void writeRecordInternal(Row record) throws Exception {
    writer.write(record);
  }

  @Override
  public int getMaxParallelism() {
    return Integer.MAX_VALUE;
  }

  @Override
  public void initPlugin() throws Exception {
    writer = new PrintSinkOutputWriter<Row>(false);

  }

  @Override
  public String getType() {
    return "print-sink";
  }

  @Override
  public void tryCleanupOnError() throws Exception {

  }
}
