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

import com.bytedance.bitsail.common.util.Preconditions;
import com.bytedance.bitsail.connector.legacy.rocketmq.config.RocketMQSinkConfig;

import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RocketMQProducer implements Serializable {
  private static final Logger LOG = LoggerFactory.getLogger(RocketMQProducer.class);

  private static final long serialVersionUID = 1L;

  /* necessary properties for @{link DefaultMQProducer} */
  private final String nameServerAddress;
  private final String producerGroup;

  /* flush settings */
  private final int batchSize;
  private final boolean batchFlushEnable;
  private final boolean logFailuresOnly;
  private final boolean enableSyncSend;

  /* acl control */
  private final String accessKey;
  private final String secretKey;

  /* props for producer */
  private final int failureRetryTimes;
  private final int sendMsgTimeout;
  private final int maxMessageSize;

  /* messages and producer */
  private final List<Message> messageList;
  private transient DefaultMQProducer producer;

  private transient SendCallback callBack;
  private transient volatile Exception flushException;

  @Setter
  private boolean enableQueueSelector;
  private transient HashQueueSelector queueSelector;

  public RocketMQProducer(RocketMQSinkConfig sinkConfig) {
    this.nameServerAddress = sinkConfig.getNameServerAddress();
    this.producerGroup = sinkConfig.getProducerGroup();
    this.accessKey = sinkConfig.getAccessKey();
    this.secretKey = sinkConfig.getSecretKey();
    this.failureRetryTimes = sinkConfig.getFailureRetryTimes();
    this.sendMsgTimeout = sinkConfig.getSendMsgTimeout();
    this.maxMessageSize = sinkConfig.getMaxMessageSize();

    this.batchSize = sinkConfig.getBatchSize();
    this.batchFlushEnable = sinkConfig.isEnableBatchFlush();
    this.logFailuresOnly = sinkConfig.isLogFailuresOnly();
    this.enableSyncSend = sinkConfig.isEnableSyncSend();

    this.enableQueueSelector = false;
    this.messageList = new ArrayList<>();
  }

  /**
   * initialize message list, open rocketmq producer
   */
  public void open() {
    // step1: initialize callback and queue-selector
    if (logFailuresOnly) {
      callBack = new SendCallback() {
        @Override
        public void onSuccess(SendResult sendResult) {}

        @Override
        public void onException(Throwable throwable) {
          LOG.error("Error while sending record to rocketmq: " + throwable.getMessage(), throwable);
        }
      };
    } else {
      callBack = new SendCallback() {
        @Override
        public void onSuccess(SendResult result) {}

        @Override
        public void onException(Throwable e) {
          if (flushException == null) {
            flushException = new IOException(e.getMessage(), e);
          }
        }
      };
    }

    if (enableQueueSelector) {
      this.queueSelector = new HashQueueSelector();
    }

    // step2: open a producer
    if (!StringUtils.isEmpty(accessKey) && !StringUtils.isEmpty(secretKey)) {
      AclClientRPCHook rpcHook = new AclClientRPCHook(new SessionCredentials(accessKey, secretKey));
      this.producer = new DefaultMQProducer(producerGroup, rpcHook);
    } else {
      this.producer = new DefaultMQProducer(producerGroup);
    }

    producer.setNamesrvAddr(nameServerAddress);
    producer.setRetryTimesWhenSendFailed(failureRetryTimes);
    producer.setSendMsgTimeout(sendMsgTimeout);
    producer.setMaxMessageSize(maxMessageSize);

    try {
      producer.start();
    } catch (MQClientException e) {
      LOG.error("Failed to start rocketmq producer.", e);
      throw new RuntimeException(e);
    }
  }

  public void validateParams() {
    if (enableQueueSelector && batchFlushEnable) {
      LOG.warn("Queue selector can not be used int batch flush mode!");
    }
    Preconditions.checkNotNull(nameServerAddress);
    Preconditions.checkNotNull(producerGroup);
  }

  /**
   * send row to rocketmq
   */
  public void send(Message message, String partitionKeys) throws Exception {
    checkErroneous();
    if (batchFlushEnable) {
      synchronized (messageList) {
        messageList.add(message);
        if (messageList.size() >= batchSize) {
          flushMessages(enableSyncSend);
        }
      }
    } else {
      sendSingleMessage(message, partitionKeys, enableSyncSend);
    }
  }

  /**
   * flush message list to rocketmq producer
   */
  private void flushMessages(boolean syncSend) {
    if (messageList.isEmpty()) {
      return;
    }

    try {
      sendMessageBatch(messageList, syncSend);
    } catch (Exception e) {
      LOG.error("Failed to flush.", e);
      throw new RuntimeException("Rocketmq flush exception", e);
    }
    messageList.clear();
  }

  /**
   * close producer
   */
  public void close() throws Exception {
    LOG.info("Closing RocketMQProducer.");

    if (producer != null) {
      try {
        flushMessages(true);
      } catch (Exception e) {
        LOG.error("Failed to send last batch of message.", e);
        throw new RuntimeException("Failed to send last batch of message.", e);
      }

      producer.shutdown();
    }

    checkErroneous();
  }

  private void checkErroneous() throws IOException {
    Exception e = flushException;
    if (e != null) {
      // prevent double throwing
      flushException = null;
      throw new IOException("Failed to send data to rocketmq: " + e.getMessage(), e);
    }
  }

  /**
   * Send single message.
   */
  private void sendSingleMessage(Message message, String partitionKeys, boolean enableSyncSend) throws Exception {
    if (enableSyncSend) {
      SendResult result;
      if (enableQueueSelector) {
        result = producer.send(message, queueSelector, partitionKeys);
      } else {
        result = producer.send(message);
      }
      if (result.getSendStatus() != SendStatus.SEND_OK) {
        throw new RemotingException(result.toString());
      }
    } else {
      if (enableQueueSelector) {
        producer.send(message, queueSelector, partitionKeys, callBack);
      } else {
        producer.send(message, callBack);
      }
    }
  }

  /**
   * Send multi message.
   */
  private void sendMessageBatch(List<Message> messages, boolean syncSend) throws Exception {
    if (syncSend) {
      SendResult result = producer.send(messages);
      if (result.getSendStatus() != SendStatus.SEND_OK) {
        throw new RemotingException(result.toString());
      }
    } else {
      producer.send(messages, callBack);
    }
  }
}
