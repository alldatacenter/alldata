/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.audit.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class MessageQueueConfig {

    @Value("${audit.pulsar.server.url:}")
    private String pulsarServerUrl;

    @Value("${audit.pulsar.topic:}")
    private String pulsarTopic;

    @Value("${audit.pulsar.consumer.sub.name:}")
    private String pulsarConsumerSubName;

    @Value("${audit.pulsar.consumer.enable.retry:false}")
    private boolean pulsarConsumerEnableRetry = false;

    @Value("${audit.pulsar.token:}")
    private String pulsarToken;

    @Value("${audit.pulsar.enable.auth:false}")
    private boolean pulsarEnableAuth;

    @Value("${audit.pulsar.consumer.receive.queue.size:1000}")
    private int consumerReceiveQueueSize = 1000;

    @Value("${audit.pulsar.client.operation.timeout.second:30}")
    private int clientOperationTimeoutSecond = 30;

    @Value("${audit.pulsar.client.concurrent.consumer.num:1}")
    private int concurrentConsumerNum = 1;

    @Value("${audit.tube.masterlist:}")
    private String tubeMasterList;

    @Value("${audit.tube.topic:}")
    private String tubeTopic;

    @Value("${audit.tube.consumer.group.name:}")
    private String tubeConsumerGroupName;

    @Value("${audit.tube.consumer.thread.num:4}")
    private int tubeThreadNum;

    @Value("${audit.kafka.server.url:}")
    private String kafkaServerUrl;

    @Value("${audit.kafka.topic:}")
    private String kafkaTopic;

    @Value("${audit.kafka.consumer.name:}")
    private String kafkaConsumerName;

    @Value("${audit.kafka.group.id:audit-consumer-group}")
    private String kafkaGroupId;

    @Value("${audit.kafka.enable.auto.commit:true}")
    private String enableAutoCommit;

    @Value("${audit.kafka.auto.commit.interval.ms:1000}")
    private String autoCommitIntervalMs;

    @Value("${audit.kafka.fetch.wait.ms:100}")
    private long fetchWaitMs = 100;

    @Value("${audit.kafka.auto.offset.reset:earliest}")
    private String autoOffsetReset;

    @Value("${audit.config.proxy.type:pulsar}")
    private String mqType;

    public boolean isPulsar() {
        return mqType.trim().equalsIgnoreCase("pulsar");
    }

    public boolean isTube() {
        return mqType.trim().equalsIgnoreCase("tube");
    }

    public boolean isKafka() {
        return mqType.trim().equalsIgnoreCase("kafka");
    }

}
