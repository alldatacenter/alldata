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

package com.netease.arctic.flink.write.hidden;

import com.netease.arctic.flink.shuffle.LogRecordV1;
import com.netease.arctic.flink.shuffle.ShuffleHelper;
import com.netease.arctic.log.LogData;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.table.data.RowData;
import org.apache.iceberg.Schema;

import java.util.Properties;

import static com.netease.arctic.flink.shuffle.RowKindUtil.transformFromFlinkRowKind;

/**
 * This is a hidden log writer.
 */
public class HiddenLogWriter extends AbstractHiddenLogWriter {
  private static final long serialVersionUID = 1L;

  public HiddenLogWriter(
      Schema schema,
      Properties producerConfig,
      String topic,
      LogMsgFactory<RowData> factory,
      LogData.FieldGetterFactory<RowData> fieldGetterFactory,
      byte[] jobId,
      ShuffleHelper helper) {
    super(schema, producerConfig, topic, factory, fieldGetterFactory, jobId, helper);
  }

  @Override
  public void endInput() throws Exception {
    producer.flush();
  }

  @Override
  public void processElement(StreamRecord<RowData> element) throws Exception {
    // check send flip successfully or not
    super.processElement(element);

    // continue process element
    RowData rowData = element.getValue();
    LogData<RowData> logData = new LogRecordV1(
        logVersion,
        jobIdentify,
        epicNo,
        false,
        transformFromFlinkRowKind(rowData.getRowKind()),
        rowData
    );
    producer.send(logData);
    output.collect(new StreamRecord<>(rowData));
  }
}
