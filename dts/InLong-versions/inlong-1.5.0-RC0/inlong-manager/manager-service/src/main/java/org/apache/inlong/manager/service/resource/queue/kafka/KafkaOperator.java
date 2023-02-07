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

package org.apache.inlong.manager.service.resource.queue.kafka;

import org.apache.inlong.manager.pojo.cluster.kafka.KafkaClusterInfo;
import org.apache.inlong.manager.pojo.group.kafka.InlongKafkaInfo;
import org.apache.inlong.manager.service.cluster.InlongClusterServiceImpl;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * kafka operator, supports creating topics and creating subscription.
 */
@Service
public class KafkaOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(InlongClusterServiceImpl.class);

    /**
     * Create Kafka topic inlongKafkaInfo
     */
    public void createTopic(InlongKafkaInfo inlongKafkaInfo, KafkaClusterInfo kafkaClusterInfo, String topicName)
            throws InterruptedException, ExecutionException {
        AdminClient adminClient = KafkaUtils.getAdminClient(kafkaClusterInfo);
        NewTopic topic = new NewTopic(topicName,
                inlongKafkaInfo.getNumPartitions(),
                inlongKafkaInfo.getReplicationFactor());
        // Topic will be returned if it exists, and created if it does not exist
        if (topicIsExists(kafkaClusterInfo, topicName)) {
            LOGGER.warn("kafka topic={} already exists", topicName);
            return;
        }
        CreateTopicsResult result = adminClient.createTopics(Collections.singletonList(topic));
        // To prevent the client from disconnecting too quickly and causing the Topic to not be created successfully
        Thread.sleep(500);
        LOGGER.info("success to create kafka topic={}, with={} numPartitions",
                topicName,
                result.numPartitions(topicName).get());
    }

    /**
     * Force delete Kafka topic
     */
    public void forceDeleteTopic(KafkaClusterInfo kafkaClusterInfo, String topicName) {
        AdminClient adminClient = KafkaUtils.getAdminClient(kafkaClusterInfo);
        DeleteTopicsResult result = adminClient.deleteTopics(Collections.singletonList(topicName));
        LOGGER.info("success to delete topic={}", topicName);
    }

    public boolean topicIsExists(KafkaClusterInfo kafkaClusterInfo, String topic)
            throws ExecutionException, InterruptedException {
        AdminClient adminClient = KafkaUtils.getAdminClient(kafkaClusterInfo);
        Set<String> topicList = adminClient.listTopics().names().get();
        return topicList.contains(topic);
    }

}
