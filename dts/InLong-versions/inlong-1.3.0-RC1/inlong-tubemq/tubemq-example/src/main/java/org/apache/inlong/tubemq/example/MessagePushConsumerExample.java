/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.example;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.Executor;
import org.apache.inlong.tubemq.client.common.PeerInfo;
import org.apache.inlong.tubemq.client.config.ConsumerConfig;
import org.apache.inlong.tubemq.client.consumer.ConsumePosition;
import org.apache.inlong.tubemq.client.consumer.MessageListener;
import org.apache.inlong.tubemq.client.consumer.PushMessageConsumer;
import org.apache.inlong.tubemq.client.factory.MessageSessionFactory;
import org.apache.inlong.tubemq.client.factory.TubeSingleSessionFactory;
import org.apache.inlong.tubemq.corebase.Message;
import org.apache.inlong.tubemq.corebase.utils.MixedUtils;

/**
 * This demo shows how to consume messages sequentially in Push mode.
 *
 *Consumer supports subscribe multiple topics in one consume group. Message from subscription
 * sent back to business logic via callback {@link MessageListener}. It is highly recommended NOT
 * to perform any blocking operation inside the callback.
 *
 *As for consumption control of {@link PushMessageConsumer}, business logic is able to monitor
 * current state and adjust consumption by
 *
 *<ul>
 *     <li>call {@link PushMessageConsumer#pauseConsume()} to pause consumption when high water mark exceeded.</li>
 *     <li>call {@link PushMessageConsumer#resumeConsume()} to resume consumption</li>
 * </ul>
 */
public final class MessagePushConsumerExample {

    private static final MsgSendReceiveStats msgRcvStats =
            new MsgSendReceiveStats(false);
    private static MessageSessionFactory sessionFactory;
    private static final Map<String, PushMessageConsumer> consumerMap = new HashMap<>();

    /**
     * Consume messages in Push mode through a single-session factory instance.
     *
     * @param args   Startup parameter array, including the following parts:
     *               The 1st parameter masterServers is the master address(es) to connect to,
     *                       format is master1_ip:port[,master2_ip:port];
     *               The 2nd parameter subTopicAndFiterItems the topic(s) (and filter condition set) to consume on,
     *                       format is topic_1[[:filterCond_1.1[;filterCond_1.2]][,topic_2]];
     *               The 3rd parameter groupName is the name of consumer group;
     *               The 4th parameter clientCount is the amount of consumer;
     *               The 5th  parameter fetchThreadCnt is the count of fetch thread.
     */
    public static void main(String[] args) throws Throwable {
        // 1. get and initial parameters
        final String masterServers = args[0];
        final String subTopicAndFiterItems = args[1];
        final String groupName = args[2];
        int clientCount = Integer.parseInt(args[3]);
        if (clientCount <= 0) {
            clientCount = 1;
        }
        int paraFetchThreadCnt = -1;
        if (args.length > 5) {
            paraFetchThreadCnt = Integer.parseInt(args[4]);
        }
        final int fetchThreadCnt = paraFetchThreadCnt;
        final Map<String, TreeSet<String>> topicAndFiltersMap =
                MixedUtils.parseTopicParam(subTopicAndFiterItems);

        // 2. initial configure and build session factory object
        ConsumerConfig consumerConfig =
                new ConsumerConfig(masterServers, groupName);
        // 2.1. set consume from latest position if the consumer group is first consume
        consumerConfig.setConsumePosition(ConsumePosition.CONSUMER_FROM_LATEST_OFFSET);
        // 2.2. set the fetch thread count of push consumer
        if (fetchThreadCnt > 0) {
            consumerConfig.setPushFetchThreadCnt(fetchThreadCnt);
        }
        // 2.3. build session factory object
        sessionFactory = new TubeSingleSessionFactory(consumerConfig);

        // 3. build and start consumer object
        for (int i = 0; i < clientCount; i++) {
            // 3.1. build and start consumer object
            PushMessageConsumer consumer =
                    sessionFactory.createPushConsumer(consumerConfig);
            // 3.2. set subscribed topic and Listener
            for (Map.Entry<String, TreeSet<String>> entry : topicAndFiltersMap.entrySet()) {
                MessageListener messageListener = new DefaultMessageListener(entry.getKey());
                consumer.subscribe(entry.getKey(), entry.getValue(), messageListener);
            }
            // 3.3 start consumer
            consumer.completeSubscribe();
            // 3.4 store consumer object
            consumerMap.put(consumer.getConsumerId(), consumer);
        }

        // 4. initial and statistic thread
        Thread statisticThread =
                new Thread(msgRcvStats, "Receive Statistic Thread");
        statisticThread.start();

        // 5. Resource cleanup when exiting the service
        //
        // 5.1 shutdown consumers
        // for (PushMessageConsumer consumer : consumerMap.values()) {
        //     consumer.shutdown();
        // }
        // 5.2 shutdown session factory
        // sessionFactory.shutdown();
        // 5.3 shutdown statistic thread
        // msgRecvStats.stopStats();
    }

    // Message callback processing class.
    // After the SDK receives the message, it will pass the message back to the business layer
    // for message processing by calling the receiveMessages() API of this class
    public static class DefaultMessageListener implements MessageListener {

        private final String topic;

        public DefaultMessageListener(String topic) {
            this.topic = topic;
        }

        @Override
        public void receiveMessages(PeerInfo peerInfo, List<Message> messages) {
            if (messages != null && !messages.isEmpty()) {
                msgRcvStats.addMsgCount(this.topic, messages.size());
            }
        }

        @Override
        public Executor getExecutor() {
            return null;
        }

        @Override
        public void stop() {
        }
    }
}
