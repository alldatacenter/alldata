/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.kafka;

import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.drill.categories.KafkaStorageTest;
import org.apache.drill.categories.SlowTest;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.proto.UserBitShared.DrillPBError.ErrorType;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category({KafkaStorageTest.class, SlowTest.class})
public class MessageIteratorTest extends KafkaTestBase {

  private KafkaConsumer<byte[], byte[]> kafkaConsumer;
  private KafkaPartitionScanSpec subScanSpec;

  @Before
  public void setUp() {
    Properties consumerProps = storagePluginConfig.getKafkaConsumerProps();
    consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
    consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
    consumerProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "4");
    kafkaConsumer = new KafkaConsumer<>(consumerProps);
    subScanSpec = new KafkaPartitionScanSpec(TestQueryConstants.JSON_TOPIC, 0, 0, TestKafkaSuit.NUM_JSON_MSG);
  }

  @After
  public void cleanUp() {
    if (kafkaConsumer != null) {
      kafkaConsumer.close();
      kafkaConsumer = null;
    }
  }

  @Test
  public void testWhenPollTimeOutIsTooLess() {
    MessageIterator iterator = new MessageIterator(kafkaConsumer, subScanSpec, 1);
    try {
      iterator.hasNext();
      Assert.fail("Test passed even though there are no message fetched.");
    } catch (UserException ue) {
      Assert.assertEquals(ErrorType.DATA_READ, ue.getErrorType());
      Assert.assertTrue(ue.getMessage().contains(
          "DATA_READ ERROR: Failed to fetch messages within 1 milliseconds. Consider increasing the value of the property: store.kafka.poll.timeout"));
    }
  }

  @Test
  public void testShouldReturnTrueAsKafkaHasMessages() {
    MessageIterator iterator = new MessageIterator(kafkaConsumer, subScanSpec, TimeUnit.SECONDS.toMillis(1));
    Assert.assertTrue("Message iterator returned false though there are messages in Kafka", iterator.hasNext());
  }

  @Test
  public void testShouldReturnMessage1() {
    MessageIterator iterator = new MessageIterator(kafkaConsumer, subScanSpec, TimeUnit.SECONDS.toMillis(1));
    // Calling hasNext makes only one poll to Kafka which fetches only 4 messages.
    // so fifth operation on iterator is expected to fail.
    iterator.hasNext();
    Assert.assertNotNull(iterator.next());
    Assert.assertNotNull(iterator.next());
    Assert.assertNotNull(iterator.next());
    Assert.assertNotNull(iterator.next());
    try {
      iterator.next();
      Assert.fail("Kafka fetched more messages than configured.");
    } catch (NoSuchElementException nse) {
      // Expected
    }
  }

  @Test
  public void testShouldReturnMessage2() {
    MessageIterator iterator = new MessageIterator(kafkaConsumer, subScanSpec, TimeUnit.SECONDS.toMillis(1));
    int messageCount = 0;
    while (iterator.hasNext()) {
      ConsumerRecord<byte[], byte[]> consumerRecord = iterator.next();
      Assert.assertNotNull(consumerRecord);
      ++messageCount;
    }
    Assert.assertEquals(TestKafkaSuit.NUM_JSON_MSG, messageCount);
  }
}
