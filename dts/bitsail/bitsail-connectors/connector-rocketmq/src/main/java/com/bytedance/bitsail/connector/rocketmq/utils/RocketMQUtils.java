/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.bytedance.bitsail.connector.rocketmq.utils;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.connector.rocketmq.option.RocketMQSourceOptions;

import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.consumer.DefaultLitePullConsumer;

public class RocketMQUtils {

  public static DefaultLitePullConsumer prepareRocketMQConsumer(BitSailConfiguration rocketmqConfiguration,
                                                                String instanceName) {
    String cluster = rocketmqConfiguration.get(RocketMQSourceOptions.CLUSTER);
    String consumerGroup = rocketmqConfiguration.get(RocketMQSourceOptions.CONSUMER_GROUP);
    String accessKey = rocketmqConfiguration.get(RocketMQSourceOptions.ACCESS_KEY);
    String secretKey = rocketmqConfiguration.get(RocketMQSourceOptions.SECRET_KEY);

    DefaultLitePullConsumer consumer;
    if (StringUtils.isNotEmpty(accessKey) && StringUtils.isNotEmpty(secretKey)) {
      AclClientRPCHook aclClientRPCHook = new AclClientRPCHook(
          new SessionCredentials(accessKey, secretKey));
      consumer = new DefaultLitePullConsumer(aclClientRPCHook);
    } else {
      consumer = new DefaultLitePullConsumer();
    }
    consumer.setConsumerGroup(consumerGroup);
    consumer.setNamesrvAddr(cluster);
    consumer.setInstanceName(instanceName);

    return consumer;
  }

}


