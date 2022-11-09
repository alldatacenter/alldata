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

package com.bytedance.bitsail.connector.legacy.rocketmq.config;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.connector.legacy.rocketmq.error.RocketMQPluginErrorCode;
import com.bytedance.bitsail.connector.legacy.rocketmq.option.RocketMQWriterOptions;

import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
public class RocketMQSinkConfig implements Serializable {
  private static final long serialVersionUID = 2L;

  private String nameServerAddress;
  private String producerGroup;
  private String topic;
  private String tag;

  private boolean enableBatchFlush;
  private int batchSize;

  private String accessKey;
  private String secretKey;

  private boolean logFailuresOnly;
  private boolean enableSyncSend;

  private int failureRetryTimes;
  private int sendMsgTimeout;
  private int maxMessageSize;

  public RocketMQSinkConfig(BitSailConfiguration outputSliceConfig) {
    this.nameServerAddress = outputSliceConfig.getNecessaryOption(RocketMQWriterOptions.NAME_SERVER_ADDRESS,
        RocketMQPluginErrorCode.REQUIRED_VALUE);
    this.producerGroup = outputSliceConfig.getUnNecessaryOption(RocketMQWriterOptions.PRODUCER_GROUP,
        UUID.randomUUID().toString());
    this.topic = outputSliceConfig.getNecessaryOption(RocketMQWriterOptions.TOPIC,
        RocketMQPluginErrorCode.REQUIRED_VALUE);
    this.tag = outputSliceConfig.get(RocketMQWriterOptions.TAG);

    this.enableBatchFlush = outputSliceConfig.get(RocketMQWriterOptions.ENABLE_BATCH_FLUSH);
    this.batchSize = outputSliceConfig.get(RocketMQWriterOptions.BATCH_SIZE);

    this.accessKey = outputSliceConfig.get(RocketMQWriterOptions.ACCESS_KEY);
    this.secretKey = outputSliceConfig.get(RocketMQWriterOptions.SECRET_KEY);

    this.logFailuresOnly = outputSliceConfig.get(RocketMQWriterOptions.LOG_FAILURES_ONLY);
    this.enableSyncSend = outputSliceConfig.get(RocketMQWriterOptions.ENABLE_SYNC_SEND);

    this.failureRetryTimes = outputSliceConfig.get(RocketMQWriterOptions.SEND_FAILURE_RETRY_TIMES);
    this.sendMsgTimeout = outputSliceConfig.get(RocketMQWriterOptions.SEND_MESSAGE_TIMEOUT);
    this.maxMessageSize = outputSliceConfig.get(RocketMQWriterOptions.MAX_MESSAGE_SIZE);
  }

  @Override
  public String toString() {
    Map<String, String> configMap = new HashMap<>();
    configMap.put("nameServerAddress", this.nameServerAddress);
    configMap.put("producerGroup", this.producerGroup);
    configMap.put("topic", this.topic);
    configMap.put("tag", this.tag);
    configMap.put("batchSize", this.batchSize + "");
    configMap.put("enableBatchFlush", this.enableBatchFlush + "");
    configMap.put("logFailuresOnly", this.logFailuresOnly + "");
    configMap.put("enableSyncSend", this.enableSyncSend + "");
    configMap.put("failureRetryTimes", this.failureRetryTimes + "");
    configMap.put("sendMsgTimeout", this.sendMsgTimeout + "");
    configMap.put("maxMessageSize", this.maxMessageSize + "");
    return configMap.toString();
  }

}

