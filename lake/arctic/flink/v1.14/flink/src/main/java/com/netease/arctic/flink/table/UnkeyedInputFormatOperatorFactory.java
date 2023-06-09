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

package com.netease.arctic.flink.table;

import com.netease.arctic.flink.interceptor.ProxyFactory;
import com.netease.arctic.flink.util.IcebergClassUtil;
import org.apache.flink.api.common.operators.MailboxExecutor;
import org.apache.flink.streaming.api.operators.AbstractStreamOperatorFactory;
import org.apache.flink.streaming.api.operators.OneInputStreamOperatorFactory;
import org.apache.flink.streaming.api.operators.StreamOperator;
import org.apache.flink.streaming.api.operators.StreamOperatorParameters;
import org.apache.flink.streaming.api.operators.YieldingOperatorFactory;
import org.apache.flink.table.data.RowData;
import org.apache.iceberg.flink.source.FlinkInputFormat;
import org.apache.iceberg.flink.source.FlinkInputSplit;
import org.apache.iceberg.flink.source.StreamingReaderOperator;

public class UnkeyedInputFormatOperatorFactory extends AbstractStreamOperatorFactory<RowData>
    implements YieldingOperatorFactory<RowData>, OneInputStreamOperatorFactory<FlinkInputSplit, RowData> {

  private final ProxyFactory<FlinkInputFormat> factory;

  private transient MailboxExecutor mailboxExecutor;

  public UnkeyedInputFormatOperatorFactory(ProxyFactory<FlinkInputFormat> factory) {
    this.factory = factory;
  }

  @Override
  public void setMailboxExecutor(MailboxExecutor mailboxExecutor) {
    this.mailboxExecutor = mailboxExecutor;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <O extends StreamOperator<RowData>> O createStreamOperator(StreamOperatorParameters<RowData> parameters) {
    StreamingReaderOperator operator = IcebergClassUtil.newStreamingReaderOperator(factory.getInstance(),
        processingTimeService, mailboxExecutor);
    operator.setup(parameters.getContainingTask(), parameters.getStreamConfig(), parameters.getOutput());
    return (O) operator;
  }

  @Override
  public Class<? extends StreamOperator> getStreamOperatorClass(ClassLoader classLoader) {
    return StreamingReaderOperator.class;
  }
}