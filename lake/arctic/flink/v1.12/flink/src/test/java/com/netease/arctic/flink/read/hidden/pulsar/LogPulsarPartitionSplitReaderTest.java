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

package com.netease.arctic.flink.read.hidden.pulsar;

import com.netease.arctic.flink.read.source.log.LogSourceHelper;
import com.netease.arctic.flink.read.source.log.pulsar.LogPulsarOrderedPartitionSplitReader;
import com.netease.arctic.flink.read.source.log.pulsar.LogRecordPulsarWithRetractInfo;
import com.netease.arctic.flink.util.pulsar.LogPulsarHelper;
import com.netease.arctic.flink.util.pulsar.PulsarTestEnvironment;
import com.netease.arctic.flink.util.pulsar.runtime.PulsarRuntime;
import org.apache.flink.api.connector.source.Boundedness;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.base.source.reader.RecordsWithSplitIds;
import org.apache.flink.connector.base.source.reader.splitreader.SplitsAddition;
import org.apache.flink.connector.pulsar.common.config.PulsarConfigBuilder;
import org.apache.flink.connector.pulsar.source.config.SourceConfiguration;
import org.apache.flink.connector.pulsar.source.enumerator.cursor.StopCursor;
import org.apache.flink.connector.pulsar.source.enumerator.topic.TopicPartition;
import org.apache.flink.connector.pulsar.source.reader.message.PulsarMessage;
import org.apache.flink.connector.pulsar.source.split.PulsarPartitionSplit;
import org.apache.flink.table.data.RowData;
import org.apache.pulsar.client.admin.PulsarAdmin;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.PulsarClient;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static com.netease.arctic.flink.write.hidden.BaseLogTest.userSchema;
import static com.netease.arctic.flink.write.hidden.HiddenLogOperatorsTest.DATA_INDEX;
import static java.util.Collections.singletonList;
import static org.apache.flink.connector.pulsar.common.config.PulsarOptions.PULSAR_ADMIN_URL;
import static org.apache.flink.connector.pulsar.common.config.PulsarOptions.PULSAR_SERVICE_URL;
import static org.apache.flink.connector.pulsar.source.PulsarSourceOptions.PULSAR_MAX_FETCH_TIME;
import static org.apache.flink.connector.pulsar.source.PulsarSourceOptions.PULSAR_SUBSCRIPTION_NAME;
import static org.apache.flink.connector.pulsar.source.config.PulsarSourceConfigUtils.SOURCE_CONFIG_VALIDATOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class LogPulsarPartitionSplitReaderTest {

  private static final Logger LOG = LoggerFactory.getLogger(LogPulsarPartitionSplitReaderTest.class);
  @ClassRule
  public static PulsarTestEnvironment environment = new PulsarTestEnvironment(PulsarRuntime.container());
  public static final String TOPIC = "splitReaderTest";
  public LogPulsarHelper logPulsarHelper;

  @Before
  public void initData() {
    logPulsarHelper = new LogPulsarHelper(environment);
    // |0 1 2 3 4 5 6 7 8 9 Flip 10 11 12 13 14| 15 16 17 18 19
    logPulsarHelper.write(TOPIC, 0);
  }

  @Test
  public void testHandleSplitChangesAndFetch() {
    LogPulsarOrderedPartitionSplitReader reader = createReader(null, false);
    handleSplit(reader, TOPIC, 0, MessageId.earliest);
    fetchedMessages(reader, 20, true);
  }

  protected List<PulsarMessage<RowData>> fetchedMessages(
      LogPulsarOrderedPartitionSplitReader splitReader, int expectedCount, boolean verify) {
    return fetchedMessages(
        splitReader, expectedCount, verify, Boundedness.CONTINUOUS_UNBOUNDED);
  }

  private List<PulsarMessage<RowData>> fetchedMessages(
      LogPulsarOrderedPartitionSplitReader splitReader,
      int expectedCount,
      boolean verify,
      Boundedness boundedness) {
    List<PulsarMessage<RowData>> messages = new ArrayList<>(expectedCount);
    List<String> finishedSplits = new ArrayList<>();
    for (int i = 0; i < 3; ) {
      try {
        RecordsWithSplitIds<PulsarMessage<RowData>> recordsBySplitIds = splitReader.fetch();
        if (recordsBySplitIds.nextSplit() != null) {
          // Collect the records in this split.
          PulsarMessage<RowData> record;
          while ((record = recordsBySplitIds.nextRecordFromSplit()) != null) {
            LOG.info("read msg: {}, msgId: {}, idx: {}",
                ((LogRecordPulsarWithRetractInfo) record).getValueToBeSent(), record.getId(), messages.size());
            messages.add(record);
          }
          finishedSplits.addAll(recordsBySplitIds.finishedSplits());
        } else {
          i++;
        }
      } catch (IOException e) {
        i++;
      }
      sleepUninterruptibly(1, TimeUnit.SECONDS);
    }
    if (verify) {
      assertThat(messages).as("We should fetch the expected size").hasSize(expectedCount);
      if (boundedness == Boundedness.CONTINUOUS_UNBOUNDED) {
        assertThat(finishedSplits).as("Split should not be marked as finished").isEmpty();
      } else {
        assertThat(finishedSplits).as("Split should be marked as finished").hasSize(1);
      }
      for (int i = 0; i < messages.size(); i++) {
        verifyMsg(((LogRecordPulsarWithRetractInfo<RowData>) messages.get(i)), i);
      }
    }

    return messages;
  }

  private void verifyMsg(LogRecordPulsarWithRetractInfo<RowData> msg, int index) {
    if (msg.isRetracting()) {

    } else {
      assertEquals(index, msg.getValueToBeSent().getInt(DATA_INDEX));
    }
  }

  protected void handleSplit(
      LogPulsarOrderedPartitionSplitReader reader, String topicName, int partitionId) {
    handleSplit(reader, topicName, partitionId, null);
  }

  protected void handleSplit(
      LogPulsarOrderedPartitionSplitReader reader,
      String topicName,
      int partitionId,
      MessageId startPosition) {
    TopicPartition partition = new TopicPartition(topicName, partitionId);
    PulsarPartitionSplit split =
        new PulsarPartitionSplit(partition, StopCursor.never(), startPosition, null);
    SplitsAddition<PulsarPartitionSplit> addition = new SplitsAddition<>(singletonList(split));
    reader.handleSplitsChanges(addition);
  }

  private LogPulsarOrderedPartitionSplitReader createReader(Configuration conf, boolean logRetractionEnable) {
    PulsarClient pulsarClient = logPulsarHelper.op().client();
    PulsarAdmin pulsarAdmin = logPulsarHelper.op().admin();
    PulsarConfigBuilder configBuilder = new PulsarConfigBuilder();

    if (conf != null) {
      configBuilder.set(conf);
    }
    configBuilder.set(PULSAR_SERVICE_URL, logPulsarHelper.op().serviceUrl());
    configBuilder.set(PULSAR_ADMIN_URL, logPulsarHelper.op().adminUrl());
    configBuilder.set(PULSAR_SUBSCRIPTION_NAME, "test-split-reader");
    configBuilder.set(PULSAR_MAX_FETCH_TIME, Duration.ofSeconds(1).toMillis());

    SourceConfiguration sourceConfiguration =
        configBuilder.build(SOURCE_CONFIG_VALIDATOR, SourceConfiguration::new);

    String logConsumerChangelogMode = "all-kinds";

    LogSourceHelper logReadHelper = logRetractionEnable ? new LogSourceHelper() : null;
    return new LogPulsarOrderedPartitionSplitReader(
        pulsarClient,
        pulsarAdmin,
        sourceConfiguration,
        null,
        userSchema,
        false,
        logReadHelper,
        logConsumerChangelogMode);
  }

}
