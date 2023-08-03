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

package com.netease.arctic.flink.write.hidden.kafka;

import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.flink.shuffle.LogRecordV1;
import com.netease.arctic.flink.util.kafka.KafkaTestBase;
import com.netease.arctic.flink.write.hidden.LogMsgFactory;
import com.netease.arctic.flink.write.hidden.TestBaseLog;
import com.netease.arctic.log.Bytes;
import com.netease.arctic.log.FormatVersion;
import com.netease.arctic.log.LogData;
import com.netease.arctic.log.LogDataJsonDeserialization;
import com.netease.arctic.log.LogDataJsonSerialization;
import com.netease.arctic.utils.IdGenerator;
import org.apache.flink.streaming.connectors.kafka.internals.FlinkKafkaInternalProducer;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.types.RowKind;
import org.apache.flink.util.InstantiationUtil;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Properties;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.netease.arctic.flink.util.kafka.KafkaConfigGenerate.getProperties;
import static com.netease.arctic.flink.util.kafka.KafkaConfigGenerate.getPropertiesWithByteArray;
import static org.apache.kafka.clients.producer.ProducerConfig.TRANSACTIONAL_ID_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class TestHiddenKafkaProducer extends TestBaseLog {
  private static final Logger LOG = LoggerFactory.getLogger(TestHiddenKafkaProducer.class);
  private static final KafkaTestBase kafkaTestBase = new KafkaTestBase();

  @BeforeClass
  public static void prepare() throws Exception {
    kafkaTestBase.prepare();
  }

  @AfterClass
  public static void shutdown() throws Exception {
    kafkaTestBase.shutDownServices();
  }

  @Test
  public void testInitTransactionId() {
    final String topic = "test-init-transactions";
    kafkaTestBase.createTestTopic(topic, 1, 1);
    FlinkKafkaInternalProducer<String, String> reuse = null;
    final String transactionalIdPrefix = UUID.randomUUID().toString();
    try {
      int numTransactions = 20;
      for (int i = 1; i <= numTransactions; i++) {
        Properties properties = getProperties(kafkaTestBase.getProperties());
        properties.put(TRANSACTIONAL_ID_CONFIG, transactionalIdPrefix + i);
        reuse = new FlinkKafkaInternalProducer<>(properties);
        reuse.initTransactions();
        reuse.beginTransaction();
        reuse.send(new ProducerRecord<>(topic, "test-value-" + i));
        if (i % 2 == 0) {
          reuse.commitTransaction();
        } else {
          reuse.flush();
          reuse.abortTransaction();
        }

        int count = kafkaTestBase.countAllRecords(topic);
        LOG.info("consumption = {}", count);
        assertThat(count).isEqualTo(i / 2);
      }
    } catch (Throwable e) {
      LOG.error("error:", e);
      if (reuse != null) {
        reuse.abortTransaction();
      }
    } finally {
      assert reuse != null;
      reuse.close(Duration.ofMillis(1000));
    }
  }

  @Test
  public void testLogProducerSendFlip() throws Exception {
    final String topic = "test-recover-transactions";
    int numPartitions = 3;
    kafkaTestBase.createTopics(numPartitions, topic);
    LogData.FieldGetterFactory<RowData> fieldGetterFactory = LogRecordV1.fieldGetterFactory;
    LogDataJsonSerialization<RowData> logDataJsonSerialization = new LogDataJsonSerialization<>(
        checkNotNull(userSchema),
        checkNotNull(fieldGetterFactory));

    Properties properties = getPropertiesWithByteArray(kafkaTestBase.getProperties());
    LogMsgFactory.Producer<RowData> producer =
        new HiddenKafkaFactory<RowData>().createProducer(
            properties,
            topic,
            logDataJsonSerialization,
            null);
    producer.open(null);

    int recoverNum = 3;
    for (int i = 0; i < recoverNum; i++) {
      producer.sendToAllPartitions(FLIP_LOG);
    }
    producer.close();

    int count = kafkaTestBase.countAllRecords(topic);
    assertThat(count).isEqualTo(numPartitions * recoverNum);
  }

  @Test
  public void testLogDataNullValueSerialize() throws IOException {

    LogDataJsonSerialization<RowData> logDataJsonSerialization =
      new LogDataJsonSerialization<>(userSchemaWithAllDataType, LogRecordV1.fieldGetterFactory);

    GenericRowData rowData = new GenericRowData(17);
    rowData.setRowKind(RowKind.INSERT);
    rowData.setField(0, null);
    rowData.setField(1, null);
    rowData.setField(2, null);
    rowData.setField(3, null);
    rowData.setField(4, null);
    rowData.setField(5, null);
    rowData.setField(6, null);
    rowData.setField(7, null);
    rowData.setField(8, null);
    rowData.setField(9, null);
    rowData.setField(10, null);
    rowData.setField(11, null);
    rowData.setField(12, null);
    rowData.setField(13, null);
    rowData.setField(14, null);
    rowData.setField(15, null);
    rowData.setField(16, null);

    LogData<RowData> logData = new LogRecordV1(
      FormatVersion.FORMAT_VERSION_V1,
      IdGenerator.generateUpstreamId(),
      1L,
      false,
      ChangeAction.INSERT,
      rowData
    );

    byte[] bytes = logDataJsonSerialization.serialize(logData);

    Assert.assertNotNull(bytes);
    String actualJson = new String(Bytes.subByte(bytes, 18, bytes.length - 18));

    String expected = "{\"f_boolean\":null,\"f_int\":null,\"f_date\":null,\"f_long\":null,\"f_time\":null,\"f_float\":null,\"f_double\":null,\"f_timestamp_local\":null,\"f_timestamp_tz\":null,\"f_string\":null,\"f_uuid\":null,\"f_fixed\":null,\"f_binary\":null,\"f_decimal\":null,\"f_list\":null,\"f_map\":null,\"f_struct\":null}";
    assertEquals(expected, actualJson);

    LogDataJsonDeserialization<RowData> logDataDeserialization = createLogDataDeserialization();
    LogData<RowData> result = logDataDeserialization.deserialize(bytes);
    Assert.assertNotNull(result);
  }

  @Test
  public void testLogDataJsonSerializationClassSerialize() throws IOException, ClassNotFoundException {
    LogDataJsonSerialization<RowData> actual =
      new LogDataJsonSerialization<>(userSchema, LogRecordV1.fieldGetterFactory);
    byte[] bytes = InstantiationUtil.serializeObject(actual);
    LogDataJsonSerialization<RowData> result = InstantiationUtil.deserializeObject(bytes, actual.getClass().getClassLoader());
    Assert.assertNotNull(result);
  }
}