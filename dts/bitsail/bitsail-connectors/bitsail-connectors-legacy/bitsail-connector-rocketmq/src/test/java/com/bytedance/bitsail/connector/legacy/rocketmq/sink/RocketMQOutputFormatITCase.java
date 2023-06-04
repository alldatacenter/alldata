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

package com.bytedance.bitsail.connector.legacy.rocketmq.sink;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.connector.legacy.fake.option.FakeReaderOptions;
import com.bytedance.bitsail.connector.legacy.rocketmq.option.RocketMQWriterOptions;
import com.bytedance.bitsail.test.connector.test.EmbeddedFlinkCluster;
import com.bytedance.bitsail.test.connector.test.utils.JobConfUtils;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Need to start rocket service first.
 */
@Ignore
public class RocketMQOutputFormatITCase {
  private static final Logger LOG = LoggerFactory.getLogger(RocketMQOutputFormatITCase.class);

  private static final int TOTAL_SEND_COUNT = 300;

  private static final String NAME_SERVER_ADDRESS = "127.0.0.1:9876";
  private static final String TOPIC_NAME = "test_topic";
  private static final String PRODUCER_GROUP = "test_producer_group";
  private static final String TAG = "itcase_test";

  @Test
  public void testFakeToRocketMQ() throws Exception {
    BitSailConfiguration configuration = JobConfUtils.fromClasspath("fake_to_rocketmq.json");
    updateConfiguration(configuration);
    EmbeddedFlinkCluster.submitJob(configuration);

    Queue<MessageExt> messages = consumeTopic();
    Assert.assertEquals(TOTAL_SEND_COUNT, messages.size());
  }

  protected void updateConfiguration(BitSailConfiguration jobConfiguration) {
    jobConfiguration.set(FakeReaderOptions.TOTAL_COUNT, TOTAL_SEND_COUNT);
    jobConfiguration.set(RocketMQWriterOptions.NAME_SERVER_ADDRESS, NAME_SERVER_ADDRESS);
    jobConfiguration.set(RocketMQWriterOptions.TOPIC, TOPIC_NAME);
    jobConfiguration.set(RocketMQWriterOptions.PRODUCER_GROUP, PRODUCER_GROUP);
    jobConfiguration.set(RocketMQWriterOptions.TAG, TAG);
    jobConfiguration.set(RocketMQWriterOptions.PARTITION_FIELDS, "id");
    jobConfiguration.set(RocketMQWriterOptions.ENABLE_BATCH_FLUSH, false);
  }

  private Queue<MessageExt> consumeTopic() throws Exception {
    DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("test_consumer");
    consumer.setNamesrvAddr(NAME_SERVER_ADDRESS);
    consumer.subscribe(TOPIC_NAME, TAG);
    consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);

    Queue<MessageExt> messageQ = new ConcurrentLinkedQueue<>();

    consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
      messageQ.addAll(msgs);
      return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    });
    try {
      consumer.start();
    } catch (MQClientException e) {
      LOG.info("send message failed. {}", e.toString());
    }
    Thread.sleep(5000);
    consumer.shutdown();

    for (MessageExt msg : messageQ) {
      LOG.debug(new String(msg.getBody()));
    }

    LOG.info("Total get {} messages.", messageQ.size());
    return messageQ;
  }
}
