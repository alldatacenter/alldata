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

package com.netease.arctic.flink.util.pulsar;

import com.netease.arctic.flink.shuffle.LogRecordV1;
import com.netease.arctic.flink.util.pulsar.runtime.PulsarRuntimeOperator;
import com.netease.arctic.log.FormatVersion;
import com.netease.arctic.log.LogData;
import com.netease.arctic.log.LogDataJsonDeserialization;
import com.netease.arctic.log.LogDataJsonSerialization;
import com.netease.arctic.utils.IdGenerator;
import org.apache.flink.table.data.RowData;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static com.netease.arctic.flink.shuffle.RowKindUtil.transformFromFlinkRowKind;
import static com.netease.arctic.flink.write.hidden.BaseLogTest.createLogDataDeserialization;
import static com.netease.arctic.flink.write.hidden.BaseLogTest.userSchema;
import static com.netease.arctic.flink.write.hidden.HiddenLogOperatorsTest.createRowData;

public class LogPulsarHelper {

  private static final Logger LOG = LoggerFactory.getLogger(LogPulsarHelper.class);

  private static byte[] jobId;
  private PulsarTestEnvironment environment;

  public LogPulsarHelper(PulsarTestEnvironment environment) {
    this.environment = environment;
    jobId = IdGenerator.generateUpstreamId();
  }

  public PulsarRuntimeOperator op() {
    return environment.operator();
  }

  private static byte[] createLogData(int i, int epicNo, boolean flip,
                                      LogDataJsonSerialization<RowData> serialization) {
    RowData rowData = createRowData(i);
    LogData<RowData> logData = new LogRecordV1(
        FormatVersion.FORMAT_VERSION_V1,
        jobId,
        epicNo,
        flip,
        transformFromFlinkRowKind(rowData.getRowKind()),
        rowData
    );
    return serialization.serialize(logData);
  }

  public List<LogData<RowData>> write(String topic, int offset) {
    List<byte[]> data = new ArrayList<>(20 + offset);

    LogDataJsonSerialization<RowData> serialization =
        new LogDataJsonSerialization<>(userSchema, LogRecordV1.fieldGetterFactory);
    for (int j = 0; j < offset; j++) {
      data.add(createLogData(0, 1, false, serialization));
    }

    int i = offset, batch = 5;
    // 0-4 + offset success
    for (; i < offset + batch; i++) {
      data.add(createLogData(i, 1, false, serialization));
    }

    // 5-9 + offset fail
    for (; i < offset + batch * 2; i++) {
      data.add(createLogData(i, 2, false, serialization));
    }

    data.add(createLogData(i, 1, true, serialization));

    // 10-14 + offset success
    for (; i < offset + batch * 3; i++) {
      data.add(createLogData(i, 2, false, serialization));
    }

    for (; i < offset + batch * 4; i++) {
      data.add(createLogData(i, 3, false, serialization));
    }
    op().setupTopic(topic, Schema.BYTES, data);
    return printDataInTopic(topic);
  }

  public List<LogData<RowData>> printDataInTopic(String topic) {
    List<Message<byte[]>> consumerRecords = op().receiveAllMessages(topic, Schema.BYTES, Duration.ofSeconds(10));
    List<LogData<RowData>> actual = new ArrayList<>(consumerRecords.size());

    LOG.info("data in topic: {}", topic);
    LogDataJsonDeserialization<RowData> deserialization = createLogDataDeserialization();
    consumerRecords.forEach(consumerRecord -> {
      try {
        LogData<RowData> logData = deserialization.deserialize(consumerRecord.getData());
        LOG.info("data in pulsar: {}, msgId: {}", logData, consumerRecord.getMessageId());
        actual.add(logData);
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
    return actual;
  }

}
