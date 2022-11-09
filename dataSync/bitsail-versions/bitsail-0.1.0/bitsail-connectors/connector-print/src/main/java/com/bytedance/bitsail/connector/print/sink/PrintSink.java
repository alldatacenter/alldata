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

package com.bytedance.bitsail.connector.print.sink;

import com.bytedance.bitsail.base.connector.writer.v1.Sink;
import com.bytedance.bitsail.base.connector.writer.v1.Writer;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.option.WriterOptions;
import com.bytedance.bitsail.common.row.Row;
import com.bytedance.bitsail.common.type.BitSailTypeInfoConverter;
import com.bytedance.bitsail.common.type.TypeInfoConverter;
import com.bytedance.bitsail.connector.print.sink.option.PrintWriterOptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class PrintSink implements Sink<Row, String, Integer> {
  private static final Logger LOG = LoggerFactory.getLogger(PrintSink.class);

  private List<String> fieldNames;
  private int batchSize;

  @Override
  public String getWriterName() {
    return "print";
  }

  @Override
  public void configure(BitSailConfiguration commonConfiguration, BitSailConfiguration writerConfiguration) {
    this.batchSize = writerConfiguration.get(PrintWriterOptions.BATCH_SIZE);
    this.fieldNames = writerConfiguration.get(WriterOptions.BaseWriterOptions.COLUMNS)
        .stream()
        .map(ColumnInfo::getName)
        .collect(Collectors.toList());
  }

  @Override
  public Writer<Row, String, Integer> createWriter(Writer.Context<Integer> context) {
    return new PrintWriter(batchSize, fieldNames);
  }

  @Override
  public TypeInfoConverter createTypeInfoConverter() {
    return new BitSailTypeInfoConverter();
  }
}
